package com.Azelmods.App.data.ai.agents

import android.util.Log
import com.Azelmods.App.data.security.tor.TorService
import com.Azelmods.App.data.security.tor.TorState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Resultado de una herramienta: lo que verá el agente y si funcionó. */
data class ToolOutcome(val output: String, val ok: Boolean)

/**
 * Ejecuta las herramientas de los agentes.
 *
 * Todas las salidas se recortan a [MAX_OUTPUT_CHARS]: una página entera no cabe
 * en el contexto de un modelo y lo que se añade de más solo encarece y ralentiza.
 *
 * Las peticiones de red pasan por el ProxySelector global, así que con el modo
 * anónimo activo (Tor) también las herramientas de los agentes salen por Tor.
 */
@Singleton
class AgentToolbox @Inject constructor(
    private val torService: TorService
) {
    companion object {
        private const val TAG = "AgentToolbox"
        const val MAX_OUTPUT_CHARS = 3_500
        private const val USER_AGENT = "NexusChat/6 (Android; agentes de IA)"
        private const val MAX_PAGE_BYTES = 1_500_000L
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .callTimeout(40, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private fun torActive(): Boolean = torService.torState.value is TorState.Connected

    suspend fun run(tool: AgentToolId, input: String): ToolOutcome = withContext(Dispatchers.IO) {
        try {
            val result = when (tool) {
                AgentToolId.CALCULATOR -> calculate(input)
                AgentToolId.DATETIME -> ToolOutcome(currentDateTime(), true)
                AgentToolId.WIKIPEDIA -> wikipedia(input)
                AgentToolId.WEB_SEARCH -> webSearch(input)
                AgentToolId.READ_URL -> readUrl(input)
            }
            result.copy(output = result.output.take(MAX_OUTPUT_CHARS))
        } catch (e: Exception) {
            Log.w(TAG, "Herramienta ${tool.id} falló: ${e.message}")
            ToolOutcome("La herramienta falló: ${e.message ?: e.javaClass.simpleName}", false)
        }
    }

    // ── Calculadora ──────────────────────────────────────────────────────

    private fun calculate(input: String): ToolOutcome = try {
        val value = SafeCalculator.evaluate(input)
        ToolOutcome("${input.trim()} = ${SafeCalculator.format(value)}", true)
    } catch (e: SafeCalculator.CalculatorException) {
        ToolOutcome("No se pudo calcular: ${e.message}", false)
    }

    private fun currentDateTime(): String {
        val now = Date()
        val fmt = SimpleDateFormat("EEEE d 'de' MMMM 'de' yyyy, HH:mm (zzzz)", Locale.forLanguageTag("es"))
        return "Ahora es ${fmt.format(now)}. Fecha ISO: ${SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(now)}."
    }

    // ── Wikipedia ────────────────────────────────────────────────────────

    private fun wikipediaLang(): String =
        Locale.getDefault().language.takeIf { it.length == 2 } ?: "es"

    private fun wikipedia(query: String): ToolOutcome {
        val q = query.trim()
        if (q.isEmpty()) return ToolOutcome("Indica un tema para buscar en Wikipedia.", false)

        val langs = listOf(wikipediaLang(), "en").distinct()
        for (lang in langs) {
            val title = wikiSearchTitles(lang, q, limit = 1).firstOrNull() ?: continue
            val summary = wikiSummary(lang, title) ?: continue
            return ToolOutcome(summary, true)
        }
        return ToolOutcome("Wikipedia no tiene un artículo sobre «$q».", false)
    }

    private fun wikiSearchTitles(lang: String, query: String, limit: Int): List<String> {
        val url = "https://$lang.wikipedia.org/w/api.php?action=query&list=search&format=json&utf8=1" +
            "&srlimit=$limit&srsearch=${enc(query)}"
        val body = get(url) ?: return emptyList()
        val results = JSONObject(body).optJSONObject("query")?.optJSONArray("search") ?: return emptyList()
        return (0 until results.length()).mapNotNull { results.optJSONObject(it)?.optString("title")?.takeIf { t -> t.isNotBlank() } }
    }

    private fun wikiSummary(lang: String, title: String): String? {
        val url = "https://$lang.wikipedia.org/api/rest_v1/page/summary/${enc(title.replace(' ', '_'))}"
        val body = get(url) ?: return null
        val json = JSONObject(body)
        val extract = json.optString("extract").takeIf { it.isNotBlank() } ?: return null
        val page = json.optJSONObject("content_urls")?.optJSONObject("desktop")?.optString("page").orEmpty()
        return buildString {
            append("Wikipedia — ").append(json.optString("title", title)).append('\n')
            append(extract)
            if (page.isNotBlank()) append("\nFuente: ").append(page)
        }
    }

    // ── Búsqueda web ─────────────────────────────────────────────────────

    /**
     * Búsqueda sin clave de API: resultados de DuckDuckGo (versión HTML ligera)
     * y, si no hay o el servicio no responde, búsqueda de Wikipedia con extractos.
     */
    private fun webSearch(query: String): ToolOutcome {
        val q = query.trim()
        if (q.isEmpty()) return ToolOutcome("La búsqueda está vacía.", false)

        val ddg = runCatching { duckDuckGo(q) }.getOrElse {
            Log.w(TAG, "DuckDuckGo no respondió: ${it.message}")
            emptyList()
        }
        if (ddg.isNotEmpty()) {
            return ToolOutcome(
                "Resultados para «$q»:\n" + ddg.take(6).mapIndexed { i, r ->
                    "${i + 1}. ${r.title}\n   ${r.snippet}\n   ${r.url}"
                }.joinToString("\n"),
                true
            )
        }

        // Plan B: Wikipedia, que casi nunca falla y siempre trae fuentes.
        val lang = wikiSearchLangWithResults(q)
        if (lang != null) {
            val (l, titles) = lang
            val parts = titles.take(3).mapNotNull { wikiSummary(l, it) }
            if (parts.isNotEmpty()) {
                return ToolOutcome("Resultados (Wikipedia) para «$q»:\n" + parts.joinToString("\n\n"), true)
            }
        }
        return ToolOutcome("No se encontraron resultados para «$q». Prueba con otras palabras.", false)
    }

    private fun wikiSearchLangWithResults(q: String): Pair<String, List<String>>? {
        for (lang in listOf(wikipediaLang(), "en").distinct()) {
            val titles = runCatching { wikiSearchTitles(lang, q, limit = 3) }.getOrDefault(emptyList())
            if (titles.isNotEmpty()) return lang to titles
        }
        return null
    }

    private data class SearchResult(val title: String, val snippet: String, val url: String)

    private fun duckDuckGo(query: String): List<SearchResult> {
        val html = get("https://html.duckduckgo.com/html/?q=${enc(query)}&kl=es-es") ?: return emptyList()
        val linkRegex = Regex(
            "<a[^>]+class=\"result__a\"[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>",
            RegexOption.DOT_MATCHES_ALL
        )
        val snippetRegex = Regex(
            "<a[^>]+class=\"result__snippet\"[^>]*>(.*?)</a>",
            RegexOption.DOT_MATCHES_ALL
        )
        val links = linkRegex.findAll(html).toList()
        val snippets = snippetRegex.findAll(html).map { HtmlText.toPlainText(it.groupValues[1]) }.toList()
        return links.mapIndexedNotNull { i, m ->
            val url = resolveDuckDuckGoUrl(m.groupValues[1]) ?: return@mapIndexedNotNull null
            SearchResult(
                title = HtmlText.toPlainText(m.groupValues[2]).take(160),
                snippet = snippets.getOrNull(i).orEmpty().take(300),
                url = url
            )
        }.filter { it.title.isNotBlank() && !it.url.contains("duckduckgo.com/y.js") }
    }

    /** Los enlaces de DuckDuckGo pasan por un redirector: se extrae el destino. */
    private fun resolveDuckDuckGoUrl(href: String): String? {
        val decoded = href.replace("&amp;", "&")
        val uddg = Regex("[?&]uddg=([^&]+)").find(decoded)?.groupValues?.get(1)
        val target = if (uddg != null) URLDecoder.decode(uddg, "UTF-8") else decoded
        val absolute = if (target.startsWith("//")) "https:$target" else target
        return absolute.takeIf { it.startsWith("http") }
    }

    // ── Leer una página ──────────────────────────────────────────────────

    private fun readUrl(input: String): ToolOutcome {
        val url = input.trim().trim('<', '>', '"', '\'')
        when (val verdict = UrlGuard.check(url, torActive())) {
            is UrlGuard.Verdict.Blocked -> return ToolOutcome(verdict.reason, false)
            UrlGuard.Verdict.Allowed -> Unit
        }
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/html,text/plain,application/json;q=0.9,*/*;q=0.5")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ToolOutcome("La página respondió con error ${response.code}.", false)
            val type = response.header("Content-Type").orEmpty()
            if (!type.contains("text") && !type.contains("json") && !type.contains("xml")) {
                return ToolOutcome("La URL no es una página de texto ($type).", false)
            }
            // Como mucho 1,5 MB: no se descarga un fichero enorme para leer 3.500 caracteres.
            val raw = response.body?.source()?.let { src ->
                src.request(MAX_PAGE_BYTES)
                src.buffer.readUtf8(minOf(src.buffer.size, MAX_PAGE_BYTES))
            }.orEmpty()
            val text = if (type.contains("html")) HtmlText.toPlainText(raw) else raw
            if (text.isBlank()) return ToolOutcome("La página no tiene texto legible.", false)
            val title = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(raw)?.groupValues?.get(1)?.let { HtmlText.toPlainText(it) }
            return ToolOutcome(buildString {
                if (!title.isNullOrBlank()) append("Título: ").append(title).append('\n')
                append("URL: ").append(url).append('\n')
                append(text)
            }, true)
        }
    }

    private fun get(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "GET $url → ${response.code}")
                return null
            }
            return response.body?.string()
        }
    }

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
}

/**
 * Qué URLs puede abrir un agente.
 *
 * El texto que lee un agente puede venir de una web hostil que intente
 * "ordenarle" visitar direcciones internas (inyección de instrucciones). Por eso
 * se bloquea la red local del dispositivo —router, impresoras, servicios en
 * localhost— y cualquier esquema que no sea http/https.
 */
object UrlGuard {
    sealed class Verdict {
        object Allowed : Verdict()
        data class Blocked(val reason: String) : Verdict()
    }

    fun check(url: String, torActive: Boolean): Verdict {
        val parsed = url.toHttpUrlOrNull()
            ?: return Verdict.Blocked("URL no válida. Debe empezar por http:// o https://")
        val host = parsed.host.lowercase()

        if (host.endsWith(".onion")) {
            return if (torActive) Verdict.Allowed
            else Verdict.Blocked("Las direcciones .onion solo se pueden abrir con el modo anónimo (Tor) activo.")
        }
        if (host == "localhost" || host.endsWith(".local") || host.endsWith(".internal") || host.endsWith(".lan")) {
            return Verdict.Blocked("Por seguridad, los agentes no acceden a la red local.")
        }
        if (isPrivateIp(host)) {
            return Verdict.Blocked("Por seguridad, los agentes no acceden a direcciones IP privadas.")
        }
        return Verdict.Allowed
    }

    private fun isPrivateIp(host: String): Boolean {
        val h = host.trim('[', ']')
        if (h == "::1" || h.startsWith("fc") || h.startsWith("fd") || h.startsWith("fe80")) return h.contains(':')
        val parts = h.split('.')
        if (parts.size != 4) return false
        val octets = parts.map { it.toIntOrNull() ?: return false }
        val (a, b) = octets[0] to octets[1]
        return a == 10 || a == 127 || a == 0 ||
            (a == 169 && b == 254) ||
            (a == 172 && b in 16..31) ||
            (a == 192 && b == 168) ||
            (a == 100 && b in 64..127)
    }
}

/** Texto legible a partir de HTML, sin depender de Android (testeable en JVM). */
object HtmlText {
    private val dropBlocks = Regex(
        "<(script|style|noscript|svg|head|nav|footer|form|iframe)[^>]*>.*?</\\1>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val comments = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val blockBreaks = Regex("</?(p|div|br|li|h[1-6]|tr|section|article|ul|ol|table)[^>]*>", RegexOption.IGNORE_CASE)
    private val tags = Regex("<[^>]+>")
    private val numericEntity = Regex("&#(x?[0-9a-fA-F]+);")

    fun toPlainText(html: String): String {
        var s = html
        s = comments.replace(s, " ")
        s = dropBlocks.replace(s, " ")
        s = blockBreaks.replace(s, "\n")
        s = tags.replace(s, " ")
        s = decodeEntities(s)
        return s.lines()
            .map { it.replace(Regex("[ \\t\\u00A0]+"), " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }

    fun decodeEntities(s: String): String {
        var out = s
            .replace("&nbsp;", " ")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
        out = numericEntity.replace(out) { m ->
            val v = m.groupValues[1]
            val code = if (v.startsWith("x") || v.startsWith("X")) v.drop(1).toIntOrNull(16) else v.toIntOrNull()
            code?.takeIf { it in 1..0x10FFFF }?.let { String(Character.toChars(it)) } ?: m.value
        }
        // &amp; al final para no decodificar dos veces "&amp;lt;".
        return out.replace("&amp;", "&")
    }
}

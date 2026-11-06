package com.Azelmods.App.data.ai.agents

import android.util.Log
import com.Azelmods.App.data.api.AzelAIApiService
import com.Azelmods.App.data.security.tor.TorService
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.ui.screens.editor.CodeFile
import com.Azelmods.App.ui.screens.editor.CodeLanguages
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Resultado de una herramienta: lo que verá el agente y si funcionó. */
data class ToolOutcome(val output: String, val ok: Boolean)

/**
 * Ejecuta las herramientas de los agentes.
 *
 * Las salidas se recortan: una página entera no cabe en el contexto de un modelo y lo que
 * sobra solo encarece y ralentiza. Las páginas largas se leen por partes.
 *
 * Las peticiones de red pasan por el ProxySelector global, así que con el modo anónimo
 * activo (Tor) también las herramientas de los agentes salen por Tor.
 */
@Singleton
class AgentToolbox @Inject constructor(
    private val torService: TorService,
    private val jsSandbox: JsSandbox,
    private val api: AzelAIApiService,
    private val exporter: AgentFileExporter
) {
    companion object {
        private const val TAG = "AgentToolbox"
        const val MAX_OUTPUT_CHARS = 6_000
        private const val PAGE_CHARS = 9_000
        private const val USER_AGENT = "NexusChat/7 (Android; agentes de IA)"
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0 Mobile Safari/537.36"
        private const val MAX_PAGE_BYTES = 3_000_000L
        /** Con menos texto que esto, la página casi seguro se dibuja con JavaScript. */
        private const val MIN_USEFUL_TEXT = 250
        private const val MAX_FILE_CHARS = 1_000_000
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .callTimeout(40, TimeUnit.SECONDS)
            .followRedirects(true)
            // También en cada redirección: una web pública podría redirigir a la red local.
            .addNetworkInterceptor { chain ->
                val verdict = UrlGuard.check(chain.request().url.toString(), torActive())
                if (verdict is UrlGuard.Verdict.Blocked) throw IOException(verdict.reason)
                chain.proceed(chain.request())
            }
            .build()
    }

    /** Un buscador lento no debe retrasar a los demás. */
    private val searchClient: OkHttpClient by lazy {
        client.newBuilder().callTimeout(15, TimeUnit.SECONDS).build()
    }

    private fun torActive(): Boolean = torService.torState.value is TorState.Connected

    suspend fun run(tool: AgentToolId, input: String): ToolOutcome = withContext(Dispatchers.IO) {
        try {
            val result = when (tool) {
                AgentToolId.WEB_SEARCH -> webSearch(input)
                AgentToolId.NEWS_SEARCH -> newsSearch(input)
                AgentToolId.READ_URL -> readUrl(input)
                AgentToolId.PROFILE_LOOKUP -> profileLookup(input)
                AgentToolId.GITHUB -> github(input)
                AgentToolId.STACKOVERFLOW -> stackOverflow(input)
                AgentToolId.PACKAGE_INFO -> packageInfo(input)
                AgentToolId.WIKIPEDIA -> wikipedia(input)
                AgentToolId.WEATHER -> weather(input)
                AgentToolId.CURRENCY -> currency(input)
                AgentToolId.CALCULATOR -> calculate(input)
                AgentToolId.DATETIME -> ToolOutcome(currentDateTime(), true)
                AgentToolId.RUN_JS -> jsSandbox.run(stripFence(input))
                AgentToolId.RUN_CODE -> runCode(input)
                AgentToolId.SAVE_FILE -> saveFile(input)
                AgentToolId.EXPORT_FILE -> exportFile(input)
                AgentToolId.READ_FILE -> readFile(input)
                AgentToolId.LIST_FILES -> listFiles()
            }
            val limit = when (tool) {
                AgentToolId.READ_URL, AgentToolId.READ_FILE, AgentToolId.GITHUB -> PAGE_CHARS + 400
                else -> MAX_OUTPUT_CHARS
            }
            result.copy(output = result.output.take(limit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Herramienta ${tool.id} falló: ${e.message}")
            ToolOutcome("La herramienta falló: ${e.message ?: e.javaClass.simpleName}", false)
        }
    }

    // ── Calculadora y fecha ──────────────────────────────────────────────

    private fun calculate(input: String): ToolOutcome = try {
        val value = SafeCalculator.evaluate(input)
        ToolOutcome("${input.trim()} = ${SafeCalculator.format(value)}", true)
    } catch (e: SafeCalculator.CalculatorException) {
        ToolOutcome("No se pudo calcular: ${e.message}", false)
    }

    fun currentDateTime(): String {
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
     * Pregunta a varios buscadores a la vez y une lo que devuelven.
     *
     * Un solo buscador no basta: ante un nombre poco común, DuckDuckGo devuelve páginas que
     * no tienen nada que ver y el agente concluía que eso "no existe". Por eso se comprueba
     * si cada resultado contiene literalmente lo buscado y se le dice al agente la verdad:
     * dónde buscó y qué no encontró, que no es lo mismo que demostrar que no existe.
     */
    private suspend fun webSearch(query: String): ToolOutcome = coroutineScope {
        val q = query.trim()
        if (q.isEmpty()) return@coroutineScope ToolOutcome("La búsqueda está vacía.", false)

        val terms = SearchRanking.exactTerms(q)
        val quoted = if (terms.isNotEmpty() && '"' !in q) terms.joinToString(" ") { "\"$it\"" } else null

        val google = async { runCatching { api.googleSearch(q) }.getOrNull() }
        val engines = buildList {
            add(async { engine("DuckDuckGo") { SearchParsers.duckDuckGo(html("https://html.duckduckgo.com/html/?q=${enc(q)}")) } })
            add(async { engine("Bing") { SearchParsers.bing(html("https://www.bing.com/search?q=${enc(q)}&setlang=es")) } })
            add(async { engine("Yahoo") { SearchParsers.yahoo(html("https://search.yahoo.com/search?p=${enc(q)}")) } })
            if (quoted != null) {
                add(async { engine("DuckDuckGo") { SearchParsers.duckDuckGo(html("https://html.duckduckgo.com/html/?q=${enc(quoted)}")) } })
                add(async { engine("Bing") { SearchParsers.bing(html("https://www.bing.com/search?q=${enc(quoted)}&setlang=es")) } })
            }
        }.awaitAll()

        val results = linkedMapOf<String, List<RawHit>>()
        engines.forEach { (name, hits) -> if (hits != null) results[name] = results[name].orEmpty() + hits }
        val merged = SearchRanking.merge(q, results)
        val googleResult = google.await()

        val consulted = buildList {
            add(if (googleResult != null) "Google (vía Gemini)" else "Google: no disponible sin clave de Gemini")
            engines.map { it.first }.distinct().forEach { name ->
                val answered = results[name]
                add(if (answered == null) "$name: no respondió" else "$name: ${answered.size}")
            }
        }

        val text = buildString {
            append("Búsqueda: «").append(q).append("»\n")
            append("Consultados: ").append(consulted.joinToString(" · ")).append("\n\n")
            if (googleResult != null) {
                append("Google responde:\n").append(googleResult.summary.take(1_800)).append('\n')
                googleResult.sources.take(6).forEach { (title, url) -> append("- ").append(title).append(": ").append(url).append('\n') }
                append('\n')
            }
            merged.take(10).forEachIndexed { i, hit ->
                append(i + 1).append(". ")
                if (hit.exact) append("[coincidencia exacta] ")
                append(hit.title).append('\n')
                if (hit.snippet.isNotBlank()) append("   ").append(hit.snippet).append('\n')
                append("   ").append(hit.url).append('\n')
            }
            if (terms.isNotEmpty() && merged.none { it.exact } && googleResult == null) {
                append("\nAVISO: ningún resultado contiene literalmente ")
                append(terms.joinToString(" ni ") { "«$it»" })
                append(". Lo de arriba son páginas parecidas, no sobre esto. Significa que estos buscadores no lo ")
                append("muestran; NO demuestra que no exista. Si es un nombre de usuario usa profile_lookup, y dile al ")
                append("usuario exactamente dónde buscaste.\n")
            }
        }
        val found = merged.isNotEmpty() || googleResult != null
        ToolOutcome(
            if (found) text else "No hubo resultados para «$q» en ningún buscador (${consulted.joinToString(" · ")}). Prueba con otras palabras.",
            found
        )
    }

    /** null = el buscador falló, que no es lo mismo que no encontrar nada. */
    private fun engine(name: String, search: () -> List<RawHit>): Pair<String, List<RawHit>?> =
        name to runCatching(search).onFailure { Log.w(TAG, "$name no respondió: ${it.message}") }.getOrNull()

    private fun html(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept-Language", "es,en;q=0.8")
            .build()
        searchClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    private fun newsSearch(query: String): ToolOutcome {
        val q = query.trim()
        if (q.isEmpty()) return ToolOutcome("Indica un tema.", false)
        val xml = html("https://news.google.com/rss/search?q=${enc(q)}&hl=es-419&gl=CO&ceid=CO:es-419")
        val items = SearchParsers.newsRss(xml)
        if (items.isEmpty()) return ToolOutcome("Google Noticias no tiene noticias sobre «$q».", false)
        return ToolOutcome(
            "Noticias sobre «$q»:\n" + items.take(10).mapIndexed { i, item ->
                "${i + 1}. ${item["title"]} (${item["source"]}, ${item["date"]})\n   ${item["link"]}"
            }.joinToString("\n"),
            true
        )
    }

    // ── Leer una página ──────────────────────────────────────────────────

    private val pageParam = Regex("\\s+(?:pagina|página|page)\\s*=\\s*(\\d+)\\s*$", RegexOption.IGNORE_CASE)

    private data class Page(val title: String?, val text: String)

    private fun readUrl(input: String): ToolOutcome {
        val trimmed = input.trim()
        val pageMatch = pageParam.find(trimmed)
        val pageNumber = pageMatch?.groupValues?.get(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val url = (pageMatch?.let { trimmed.removeRange(it.range) } ?: trimmed).trim().trim('<', '>', '"', '\'')

        val verdict = UrlGuard.check(url, torActive())
        if (verdict is UrlGuard.Verdict.Blocked) return ToolOutcome(verdict.reason, false)

        val direct = runCatching { fetchPage(url) }
        var page = direct.getOrNull()
        var viaReader = false
        // Webs hechas con JavaScript, que bloquean bots, o PDF: un lector externo las abre.
        // Las .onion nunca salen a un tercero.
        if ((page == null || page.text.length < MIN_USEFUL_TEXT) && !url.contains(".onion")) {
            val read = runCatching { fetchWithReader(url) }.getOrNull()
            if (read != null && read.text.length > (page?.text?.length ?: 0)) {
                page = read
                viaReader = true
            }
        }
        if (page == null) {
            return ToolOutcome("No se pudo abrir la página: ${direct.exceptionOrNull()?.message ?: "sin respuesta"}.", false)
        }
        if (page.text.isBlank()) return ToolOutcome("La página no tiene texto legible.", false)

        val totalPages = (page.text.length + PAGE_CHARS - 1) / PAGE_CHARS
        if (pageNumber > totalPages) return ToolOutcome("La página solo tiene $totalPages partes.", false)
        val chunk = page.text.drop((pageNumber - 1) * PAGE_CHARS).take(PAGE_CHARS)
        return ToolOutcome(buildString {
            if (!page.title.isNullOrBlank()) append("Título: ").append(page.title).append('\n')
            append("URL: ").append(url).append('\n')
            if (viaReader) append("(Leída con un lector externo porque la web no se puede abrir directamente.)\n")
            if (totalPages > 1) {
                append("Parte ").append(pageNumber).append(" de ").append(totalPages)
                if (pageNumber < totalPages) append(". Para seguir: ").append(url).append(" pagina=").append(pageNumber + 1)
                append('\n')
            }
            append(chunk)
        }, true)
    }

    private fun fetchPage(url: String): Page {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", BROWSER_USER_AGENT)
            .header("Accept", "text/html,text/plain,application/json;q=0.9,*/*;q=0.5")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("La página respondió con error ${response.code}")
            val type = response.header("Content-Type").orEmpty()
            if (!type.contains("text") && !type.contains("json") && !type.contains("xml")) {
                throw IOException("La URL no es una página de texto ($type)")
            }
            // No se descarga un archivo enorme para leer unos pocos miles de caracteres.
            val raw = response.body?.source()?.let { src ->
                src.request(MAX_PAGE_BYTES)
                src.buffer.readUtf8(minOf(src.buffer.size, MAX_PAGE_BYTES))
            }.orEmpty()
            val text = if (type.contains("html")) HtmlText.toPlainText(raw) else raw
            val title = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
                .find(raw)?.groupValues?.get(1)?.let { HtmlText.toPlainText(it) }
            return Page(title, text)
        }
    }

    private fun fetchWithReader(url: String): Page {
        val request = Request.Builder()
            .url("https://r.jina.ai/$url")
            .header("User-Agent", USER_AGENT)
            .header("Accept", "text/plain")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("El lector respondió ${response.code}")
            return Page(null, response.body?.string().orEmpty())
        }
    }

    // ── Perfiles públicos ────────────────────────────────────────────────

    private val validUsername = Regex("^[A-Za-z0-9_.-]{1,64}$")

    /** Cada sitio dice de una forma distinta si un perfil existe; aquí solo cuenta lo comprobado. */
    private suspend fun profileLookup(input: String): ToolOutcome = coroutineScope {
        val user = input.trim().removePrefix("@")
        if (!validUsername.matches(user)) {
            return@coroutineScope ToolOutcome("«$input» no parece un nombre de usuario.", false)
        }
        val u = enc(user)
        val checks: List<Pair<String, () -> String>> = listOf(
            "GitHub" to {
                val (code, body) = status("https://api.github.com/users/$u", USER_AGENT)
                when (code) {
                    200 -> JSONObject(body).let { json ->
                        "EXISTE: ${json.optString("html_url")} — ${json.optString("name")} · " +
                            "${json.optInt("public_repos")} repositorios · ${json.optString("bio")}"
                    }
                    404 -> "no existe"
                    else -> unverified(code)
                }
            },
            "GitLab" to {
                val (code, body) = status("https://gitlab.com/api/v4/users?username=$u", USER_AGENT)
                when {
                    code != 200 -> unverified(code)
                    JSONArray(body).length() > 0 -> "EXISTE: https://gitlab.com/$user"
                    else -> "no existe"
                }
            },
            "YouTube" to {
                when (val code = status("https://www.youtube.com/@$u", BROWSER_USER_AGENT).first) {
                    200 -> "EXISTE: https://www.youtube.com/@$user"
                    404 -> "no existe"
                    else -> unverified(code)
                }
            },
            "X (Twitter)" to {
                val target = enc("https://twitter.com/$user")
                when (val code = status("https://publish.twitter.com/oembed?url=$target", BROWSER_USER_AGENT).first) {
                    200 -> "EXISTE: https://x.com/$user"
                    404 -> "no existe"
                    else -> unverified(code)
                }
            },
            "Reddit" to {
                val (code, body) = status("https://www.reddit.com/user/$u/about.json", USER_AGENT)
                when {
                    code == 200 && body.contains("\"name\"") -> "EXISTE: https://www.reddit.com/user/$user"
                    code == 404 -> "no existe"
                    else -> unverified(code)
                }
            },
            "Telegram" to {
                val (code, body) = status("https://t.me/$u", BROWSER_USER_AGENT)
                when {
                    code != 200 -> unverified(code)
                    body.contains("tgme_page_title") -> "EXISTE: https://t.me/$user"
                    else -> "no existe"
                }
            },
            "TikTok" to {
                val (code, body) = status("https://www.tiktok.com/@$u", BROWSER_USER_AGENT)
                when {
                    code != 200 -> unverified(code)
                    body.contains("\"uniqueId\":\"$user\"", ignoreCase = true) -> "EXISTE: https://www.tiktok.com/@$user"
                    Regex("\"statusCode\":(10221|10202)").containsMatchIn(body) -> "no existe"
                    else -> "no se pudo comprobar"
                }
            },
            "Hacker News" to {
                val (code, body) = status("https://hacker-news.firebaseio.com/v0/user/$u.json", USER_AGENT)
                when {
                    code != 200 -> unverified(code)
                    body.trim() == "null" -> "no existe"
                    else -> "EXISTE: https://news.ycombinator.com/user?id=$user"
                }
            }
        )
        val lines = checks.map { (site, check) ->
            async { "$site: ${runCatching(check).getOrElse { "no se pudo comprobar" }}" }
        }.awaitAll()
        ToolOutcome(
            "Perfil «$user» en sitios públicos:\n" + lines.joinToString("\n") +
                "\nInstagram y Facebook exigen iniciar sesión y no se pueden comprobar así.",
            true
        )
    }

    private fun unverified(code: Int) = "no se pudo comprobar (el sitio respondió $code)"

    private fun status(url: String, userAgent: String): Pair<Int, String> {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept-Language", "es,en;q=0.8")
            .build()
        searchClient.newCall(request).execute().use { response ->
            return response.code to response.body?.string().orEmpty()
        }
    }

    // ── Programación ─────────────────────────────────────────────────────

    private fun github(input: String): ToolOutcome {
        val q = input.trim()
        if (q.isEmpty()) return ToolOutcome("Indica qué buscar en GitHub.", false)
        return when {
            q.startsWith("repo:") -> githubRepo(q.removePrefix("repo:").trim())
            q.startsWith("file:") -> githubFile(q.removePrefix("file:").trim())
            else -> githubSearch(q)
        }
    }

    private fun githubSearch(query: String): ToolOutcome {
        val (code, body) = status("https://api.github.com/search/repositories?q=${enc(query)}&per_page=6", USER_AGENT)
        if (code == 403 || code == 429) return ToolOutcome("GitHub limita las búsquedas sin cuenta; inténtalo en un rato.", false)
        if (code != 200) return ToolOutcome("GitHub respondió $code.", false)
        val items = JSONObject(body).optJSONArray("items") ?: JSONArray()
        val users = runCatching {
            val (userCode, userBody) = status("https://api.github.com/search/users?q=${enc(query)}&per_page=3", USER_AGENT)
            if (userCode == 200) JSONObject(userBody).optJSONArray("items") else null
        }.getOrNull() ?: JSONArray()
        if (items.length() == 0 && users.length() == 0) return ToolOutcome("GitHub no tiene nada sobre «$query».", false)
        return ToolOutcome(buildString {
            append("Repositorios en GitHub para «").append(query).append("»:\n")
            for (i in 0 until items.length()) {
                val repo = items.getJSONObject(i)
                append("- ").append(repo.optString("full_name")).append(" (★").append(repo.optInt("stargazers_count"))
                append(", ").append(repo.optString("language")).append(") ").append(repo.optString("description"))
                append("\n  ").append(repo.optString("html_url")).append('\n')
            }
            if (users.length() > 0) {
                append("Perfiles: ")
                append((0 until users.length()).joinToString { users.getJSONObject(it).optString("html_url") })
            }
        }, true)
    }

    private fun githubRepo(repo: String): ToolOutcome {
        val (code, body) = status("https://api.github.com/repos/$repo", USER_AGENT)
        if (code == 404) return ToolOutcome("El repositorio $repo no existe o es privado.", false)
        if (code != 200) return ToolOutcome("GitHub respondió $code.", false)
        val json = JSONObject(body)
        val branch = json.optString("default_branch", "main")
        val files = runCatching {
            val (_, tree) = status("https://api.github.com/repos/$repo/contents", USER_AGENT)
            val array = JSONArray(tree)
            (0 until array.length()).joinToString(", ") { array.getJSONObject(it).optString("path") }
        }.getOrDefault("")
        val readme = runCatching { fetchPage("https://raw.githubusercontent.com/$repo/$branch/README.md").text }.getOrDefault("")
        return ToolOutcome(buildString {
            append(json.optString("full_name")).append(" — ").append(json.optString("description")).append('\n')
            append("★").append(json.optInt("stargazers_count")).append(" · ").append(json.optString("language"))
            append(" · actualizado ").append(json.optString("pushed_at")).append('\n')
            append(json.optString("html_url")).append('\n')
            if (files.isNotBlank()) append("Archivos: ").append(files).append('\n')
            if (readme.isNotBlank()) append("\nREADME:\n").append(readme.take(PAGE_CHARS - 1_000))
        }, true)
    }

    private fun githubFile(path: String): ToolOutcome {
        val parts = path.split('/', limit = 3)
        if (parts.size < 3) return ToolOutcome("Formato: file:dueño/nombre/ruta/al/archivo", false)
        val (code, body) = status("https://raw.githubusercontent.com/${parts[0]}/${parts[1]}/HEAD/${parts[2]}", USER_AGENT)
        if (code != 200) return ToolOutcome("No se pudo leer el archivo (GitHub respondió $code).", false)
        return ToolOutcome(body, true)
    }

    private fun stackOverflow(input: String): ToolOutcome {
        val q = input.trim()
        if (q.isEmpty()) return ToolOutcome("Indica el problema.", false)
        val (code, body) = status(
            "https://api.stackexchange.com/2.3/search/advanced?order=desc&sort=relevance&site=stackoverflow&pagesize=6&q=${enc(q)}",
            USER_AGENT
        )
        if (code != 200) return ToolOutcome("Stack Overflow respondió $code.", false)
        val items = JSONObject(body).optJSONArray("items") ?: JSONArray()
        if (items.length() == 0) return ToolOutcome("No hay preguntas sobre «$q».", false)
        return ToolOutcome(
            (0 until items.length()).joinToString("\n") { i ->
                val item = items.getJSONObject(i)
                val state = if (item.optBoolean("is_answered")) "resuelta" else "sin resolver"
                "- ${HtmlText.decodeEntities(item.optString("title"))} ($state, ${item.optInt("score")} puntos)\n  ${item.optString("link")}"
            },
            true
        )
    }

    private fun packageInfo(input: String): ToolOutcome {
        val spec = input.trim()
        val registry = spec.substringBefore(':', "npm").lowercase()
        val name = spec.substringAfter(':', spec).trim()
        if (name.isEmpty()) return ToolOutcome("Formato: npm:react, pypi:requests, crates:serde o maven:grupo:artefacto", false)
        return when (registry) {
            "npm" -> {
                val (code, body) = status("https://registry.npmjs.org/${name.replace("/", "%2F")}/latest", USER_AGENT)
                if (code != 200) return ToolOutcome("npm no tiene el paquete «$name».", false)
                val json = JSONObject(body)
                ToolOutcome("npm ${json.optString("name")} ${json.optString("version")}: ${json.optString("description")}\nInstalar: npm install $name", true)
            }
            "pypi" -> {
                val (code, body) = status("https://pypi.org/pypi/${enc(name)}/json", USER_AGENT)
                if (code != 200) return ToolOutcome("PyPI no tiene el paquete «$name».", false)
                val info = JSONObject(body).getJSONObject("info")
                ToolOutcome("PyPI ${info.optString("name")} ${info.optString("version")}: ${info.optString("summary")}\nInstalar: pip install $name", true)
            }
            "crates" -> {
                val (code, body) = status("https://crates.io/api/v1/crates/${enc(name)}", USER_AGENT)
                if (code != 200) return ToolOutcome("crates.io no tiene «$name».", false)
                val crate = JSONObject(body).getJSONObject("crate")
                ToolOutcome("crates.io ${crate.optString("name")} ${crate.optString("max_stable_version")}: ${crate.optString("description")}", true)
            }
            "maven" -> {
                val group = name.substringBefore(':')
                val artifact = name.substringAfter(':', "")
                val query = enc("g:\"$group\" AND a:\"$artifact\"")
                val (code, body) = status("https://search.maven.org/solrsearch/select?q=$query&rows=1&wt=json", USER_AGENT)
                if (code != 200) return ToolOutcome("Maven Central respondió $code.", false)
                val docs = JSONObject(body).getJSONObject("response").optJSONArray("docs")
                val doc = docs?.optJSONObject(0) ?: return ToolOutcome("Maven Central no tiene «$name».", false)
                ToolOutcome("Maven $group:$artifact versión ${doc.optString("latestVersion")}", true)
            }
            else -> ToolOutcome("Registro desconocido «$registry». Usa npm, pypi, crates o maven.", false)
        }
    }

    // ── Ejecutar código en un servidor externo ───────────────────────────

    private val wandboxLanguages = mapOf(
        "python" to "Python", "python3" to "Python", "py" to "Python",
        "javascript" to "JavaScript", "js" to "JavaScript", "node" to "JavaScript",
        "typescript" to "TypeScript", "ts" to "TypeScript",
        "c" to "C", "cpp" to "C++", "c++" to "C++",
        "java" to "Java", "rust" to "Rust", "rs" to "Rust", "go" to "Go", "golang" to "Go",
        "php" to "PHP", "ruby" to "Ruby", "rb" to "Ruby",
        "bash" to "Bash script", "sh" to "Bash script", "shell" to "Bash script",
        "csharp" to "C#", "c#" to "C#", "cs" to "C#",
        "lua" to "Lua", "perl" to "Perl", "swift" to "Swift", "sql" to "SQL", "r" to "R"
    )

    @Volatile private var compilers: Map<String, String> = emptyMap()

    private fun compilerFor(language: String): String? {
        if (compilers.isEmpty()) {
            val (code, body) = status("https://wandbox.org/api/list.json", USER_AGENT)
            if (code != 200) throw IOException("El servicio de ejecución respondió $code")
            val list = JSONArray(body)
            val found = linkedMapOf<String, String>()
            for (i in 0 until list.length()) {
                val item = list.getJSONObject(i)
                val name = item.optString("name")
                // Las versiones "head" son compilaciones de desarrollo, a veces rotas.
                if (!name.contains("head")) found.putIfAbsent(item.optString("language"), name)
            }
            compilers = found
        }
        return compilers[language]
    }

    private fun runCode(input: String): ToolOutcome {
        val fenced = Regex("^```([\\w+#-]*)\\s*\\n", RegexOption.MULTILINE).find(input.trimStart())
        val text = input.trim()
        val header = text.substringBefore('\n').trim().trim('`').lowercase()
        val fenceLanguage = fenced?.groupValues?.get(1)?.lowercase().orEmpty()
        val alias = header.takeIf { it in wandboxLanguages } ?: fenceLanguage.takeIf { it in wandboxLanguages }
            ?: return ToolOutcome(
                "Indica el lenguaje en la primera línea: ${wandboxLanguages.keys.joinToString()}. " +
                    "Kotlin no está disponible aquí; para probar lógica puedes usar Java o JavaScript.",
                false
            )
        val code = stripFence(if (header in wandboxLanguages) text.substringAfter('\n', "") else text)
        if (code.isBlank()) return ToolOutcome("No hay código que ejecutar.", false)
        val language = wandboxLanguages.getValue(alias)
        val compiler = compilerFor(language) ?: return ToolOutcome("$language no está disponible ahora mismo.", false)

        val payload = JSONObject().put("compiler", compiler).put("code", code).toString()
        val request = Request.Builder()
            .url("https://wandbox.org/api/compile.json")
            .header("User-Agent", USER_AGENT)
            .post(payload.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return ToolOutcome("El servicio de ejecución respondió ${response.code}.", false)
            val json = JSONObject(response.body?.string().orEmpty())
            val status = json.optString("status")
            val signal = json.optString("signal")
            val output = buildString {
                append(language).append(" (").append(compiler).append(")\n")
                json.optString("compiler_error").takeIf { it.isNotBlank() }?.let { append("Errores de compilación:\n").append(it).append('\n') }
                json.optString("program_output").takeIf { it.isNotBlank() }?.let { append("Salida:\n").append(it).append('\n') }
                json.optString("program_error").takeIf { it.isNotBlank() }?.let { append("Errores:\n").append(it).append('\n') }
                if (signal.isNotBlank()) append("Terminado por la señal ").append(signal).append('\n')
                append("Código de salida: ").append(status.ifBlank { "desconocido" })
            }
            return ToolOutcome(output, status == "0")
        }
    }

    // ── Archivos del Editor de código ────────────────────────────────────

    /** Lo que los agentes han creado desde que se abrió la app: solo eso pueden sobrescribir. */
    private val agentFileIds = ConcurrentHashMap<String, String>()

    private fun filesRef() = FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
        uid to FirebaseDatabase.getInstance().getReference("codeFiles/$uid")
    }

    private suspend fun loadFiles(): List<CodeFile>? {
        val (_, ref) = filesRef() ?: return null
        return ref.get().await().children.mapNotNull { it.getValue(CodeFile::class.java) }
    }

    /**
     * Guarda en el Editor. Un agente lee páginas de internet, que podrían intentar convencerlo
     * de pisar un archivo del usuario: por eso nunca sobrescribe lo que él no creó, sino que
     * guarda una copia con otro nombre.
     */
    private suspend fun saveFile(input: String): ToolOutcome {
        val (uid, ref) = filesRef() ?: return ToolOutcome("Inicia sesión para guardar archivos.", false)
        val text = input.trim()
        val rawName = text.substringBefore('\n').trim().trim('`', '"', '\'').substringAfterLast('/')
        val content = stripFence(text.substringAfter('\n', ""))
        if (rawName.isEmpty() || !rawName.contains('.') || rawName.length > 120) {
            return ToolOutcome("La primera línea debe ser el nombre del archivo con extensión, p. ej. app.py", false)
        }
        if (content.isBlank()) return ToolOutcome("El archivo está vacío: pon el contenido debajo del nombre.", false)
        if (content.length > MAX_FILE_CHARS) return ToolOutcome("El archivo es demasiado grande.", false)

        val existing = ref.get().await().children.mapNotNull { it.getValue(CodeFile::class.java) }
        val ownId = agentFileIds[rawName.lowercase()]
        val name = if (ownId != null) rawName else uniqueName(rawName, existing.map { it.name })
        val id = ownId ?: ref.push().key ?: "file_${System.currentTimeMillis()}"
        val file = CodeFile(
            id = id,
            name = name,
            language = CodeLanguages.fromFileName(name),
            content = content,
            userId = uid,
            timestamp = System.currentTimeMillis(),
            size = content.length.toLong()
        )
        ref.child(id).setValue(file).await()
        agentFileIds[rawName.lowercase()] = id
        agentFileIds[name.lowercase()] = id
        return ToolOutcome("Guardado «$name» en el Editor de código (${content.lines().size} líneas).", true)
    }

    private suspend fun exportFile(input: String): ToolOutcome {
        val text = input.trim()
        val name = text.substringBefore('\n').trim().trim('`', '"', '\'').substringAfterLast('/')
        val content = stripFence(text.substringAfter('\n', ""))
        if (name.isEmpty() || !name.contains('.') || name.length > 120) {
            return ToolOutcome("La primera línea debe ser el nombre del archivo con extensión, p. ej. informe.pdf", false)
        }
        if (content.isBlank()) return ToolOutcome("El documento está vacío: pon el contenido debajo del nombre.", false)
        return exporter.export(name, content)
    }

    private fun uniqueName(name: String, taken: List<String>): String {
        val used = taken.map { it.lowercase() }.toSet()
        if (name.lowercase() !in used) return name
        val base = name.substringBeforeLast('.')
        val extension = name.substringAfterLast('.')
        var n = 2
        while ("$base-$n.$extension".lowercase() in used) n++
        return "$base-$n.$extension"
    }

    private suspend fun readFile(input: String): ToolOutcome {
        val name = input.trim().trim('`', '"', '\'')
        val files = loadFiles() ?: return ToolOutcome("Inicia sesión para leer archivos.", false)
        val file = files.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?: return ToolOutcome("No hay ningún archivo llamado «$name». Usa list_files para verlos.", false)
        return ToolOutcome("${file.name}:\n${file.content}", true)
    }

    private suspend fun listFiles(): ToolOutcome {
        val files = loadFiles() ?: return ToolOutcome("Inicia sesión para ver tus archivos.", false)
        if (files.isEmpty()) return ToolOutcome("El Editor de código está vacío.", true)
        return ToolOutcome(files.sortedByDescending { it.timestamp }.joinToString("\n") { "- ${it.name} (${it.size} caracteres)" }, true)
    }

    /** Quita las vallas ``` que rodean a un bloque de código, si las hay. */
    private fun stripFence(text: String): String {
        val trimmed = text.trim()
        if (!trimmed.startsWith("```")) return text.trimEnd()
        return trimmed.substringAfter('\n', "").removeSuffix("```").trimEnd()
    }

    // ── Clima y divisas ──────────────────────────────────────────────────

    private fun weather(input: String): ToolOutcome {
        val city = input.trim()
        if (city.isEmpty()) return ToolOutcome("Indica la ciudad.", false)
        val geo = get("https://geocoding-api.open-meteo.com/v1/search?name=${enc(city)}&count=1&language=es")
            ?: return ToolOutcome("No se pudo buscar la ciudad.", false)
        val place = JSONObject(geo).optJSONArray("results")?.optJSONObject(0)
            ?: return ToolOutcome("No encuentro la ciudad «$city».", false)
        val body = get(
            "https://api.open-meteo.com/v1/forecast?latitude=${place.optDouble("latitude")}&longitude=${place.optDouble("longitude")}" +
                "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m" +
                "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max&forecast_days=3&timezone=auto"
        ) ?: return ToolOutcome("El servicio del clima no respondió.", false)
        val json = JSONObject(body)
        val now = json.getJSONObject("current")
        val daily = json.getJSONObject("daily")
        return ToolOutcome(buildString {
            append("Clima en ").append(place.optString("name")).append(", ").append(place.optString("country")).append(":\n")
            append("Ahora: ").append(now.optDouble("temperature_2m")).append(" °C, ")
            append(weatherText(now.optInt("weather_code"))).append(", humedad ").append(now.optInt("relative_humidity_2m"))
            append(" %, viento ").append(now.optDouble("wind_speed_10m")).append(" km/h\n")
            val days = daily.getJSONArray("time")
            for (i in 0 until days.length()) {
                append(days.getString(i)).append(": ").append(daily.getJSONArray("temperature_2m_min").optDouble(i))
                append("–").append(daily.getJSONArray("temperature_2m_max").optDouble(i)).append(" °C, lluvia ")
                append(daily.getJSONArray("precipitation_probability_max").optInt(i)).append(" %\n")
            }
        }, true)
    }

    private fun weatherText(code: Int): String = when (code) {
        0 -> "despejado"
        1, 2 -> "poco nuboso"
        3 -> "nublado"
        45, 48 -> "niebla"
        in 51..57 -> "llovizna"
        in 61..67, in 80..82 -> "lluvia"
        in 71..77, 85, 86 -> "nieve"
        in 95..99 -> "tormenta"
        else -> "variable"
    }

    private val currencyPattern = Regex("^\\s*([\\d.,]+)?\\s*([A-Za-z]{3})\\s*(?:a|to|en|->|=)?\\s*([A-Za-z]{3})\\s*$")

    private fun currency(input: String): ToolOutcome {
        val match = currencyPattern.find(input) ?: return ToolOutcome("Formato: 100 USD a COP", false)
        val amount = match.groupValues[1].replace(",", ".").toDoubleOrNull() ?: 1.0
        val from = match.groupValues[2].uppercase()
        val to = match.groupValues[3].uppercase()
        val body = get("https://open.er-api.com/v6/latest/$from") ?: return ToolOutcome("El servicio de divisas no respondió.", false)
        val json = JSONObject(body)
        val rate = json.optJSONObject("rates")?.takeIf { it.has(to) }?.optDouble(to)
            ?: return ToolOutcome("No hay cambio de $from a $to.", false)
        return ToolOutcome(
            "$amount $from = ${String.format(Locale.ROOT, "%.2f", amount * rate)} $to " +
                "(1 $from = $rate $to, actualizado ${json.optString("time_last_update_utc")})",
            true
        )
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

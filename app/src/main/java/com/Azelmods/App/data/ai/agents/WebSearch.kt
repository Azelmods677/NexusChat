package com.Azelmods.App.data.ai.agents

import java.net.URLDecoder
import java.util.Base64

/** Un resultado tal como lo devuelve un buscador. */
data class RawHit(val title: String, val url: String, val snippet: String)

/** Un resultado ya unido entre buscadores. */
data class SearchHit(
    val title: String,
    val url: String,
    val snippet: String,
    val engines: Set<String>,
    /** Contiene literalmente lo más distintivo de la consulta. */
    val exact: Boolean,
    /** Cuántas palabras de la consulta aparecen en él. */
    val overlap: Int
)

/**
 * Lee las páginas de resultados de cada buscador. Son funciones puras sobre HTML real, sin
 * red, para poder comprobarlas con pruebas cuando un buscador cambie su formato.
 */
object SearchParsers {

    private val ddgLink = Regex("<a[^>]+class=\"result__a\"[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
    private val ddgSnippet = Regex("<a[^>]+class=\"result__snippet\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)

    fun duckDuckGo(html: String): List<RawHit> {
        val snippets = ddgSnippet.findAll(html).map { HtmlText.toPlainText(it.groupValues[1]) }.toList()
        return ddgLink.findAll(html).mapIndexedNotNull { i, m ->
            val url = ddgTarget(m.groupValues[1]) ?: return@mapIndexedNotNull null
            RawHit(HtmlText.toPlainText(m.groupValues[2]), url, snippets.getOrNull(i).orEmpty())
        }.filter { it.title.isNotBlank() && !it.url.contains("duckduckgo.com/y.js") }.toList()
    }

    /** Los enlaces de DuckDuckGo pasan por un redirector: se extrae el destino. */
    private fun ddgTarget(href: String): String? {
        val decoded = href.replace("&amp;", "&")
        val uddg = Regex("[?&]uddg=([^&]+)").find(decoded)?.groupValues?.get(1)
        val target = if (uddg != null) URLDecoder.decode(uddg, "UTF-8") else decoded
        val absolute = if (target.startsWith("//")) "https:$target" else target
        return absolute.takeIf { it.startsWith("http") }
    }

    private val bingLink = Regex("<div class=\"b_algoheader\"[^>]*>\\s*<a[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
    private val bingTitleLink = Regex("<h2[^>]*>\\s*<a[^>]+href=\"([^\"]+)\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
    private val bingSnippet = Regex("<div class=\"b_caption\"[^>]*>.*?<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)

    fun bing(html: String): List<RawHit> =
        html.split("<li class=\"b_algo\"").drop(1).mapNotNull { block ->
            val link = bingLink.find(block) ?: bingTitleLink.find(block) ?: return@mapNotNull null
            val url = bingTarget(link.groupValues[1]) ?: return@mapNotNull null
            RawHit(
                HtmlText.toPlainText(link.groupValues[2]),
                url,
                bingSnippet.find(block)?.groupValues?.get(1)?.let { HtmlText.toPlainText(it) }.orEmpty()
            )
        }.filter { it.title.isNotBlank() }

    /** Bing a veces envuelve el destino en base64 dentro de `u=a1…`. */
    private fun bingTarget(href: String): String? {
        val decoded = href.replace("&amp;", "&")
        if (!decoded.contains("bing.com/ck/")) return decoded.takeIf { it.startsWith("http") }
        val encoded = Regex("[?&]u=a1([^&]+)").find(decoded)?.groupValues?.get(1) ?: return null
        return runCatching {
            String(Base64.getUrlDecoder().decode(encoded), Charsets.UTF_8)
        }.getOrNull()?.takeIf { it.startsWith("http") }
    }

    private val yahooLink = Regex("<a[^>]+class=\"s-title[^\"]*\"[^>]*>", RegexOption.DOT_MATCHES_ALL)
    private val yahooHref = Regex("href=\"([^\"]+)\"")
    private val yahooLabel = Regex("aria-label=\"([^\"]*)\"")
    private val yahooSnippet = Regex("<p class=\"s-desc[^\"]*\"[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL)

    fun yahoo(html: String): List<RawHit> {
        val snippets = yahooSnippet.findAll(html).map { HtmlText.toPlainText(it.groupValues[1]) }.toList()
        return yahooLink.findAll(html).mapIndexedNotNull { i, m ->
            val tag = m.value
            val url = yahooHref.find(tag)?.groupValues?.get(1)?.let(::yahooTarget) ?: return@mapIndexedNotNull null
            val title = yahooLabel.find(tag)?.groupValues?.get(1)?.let(HtmlText::decodeEntities).orEmpty()
            RawHit(title, url, snippets.getOrNull(i).orEmpty())
        }.filter { it.title.isNotBlank() }.toList()
    }

    private fun yahooTarget(href: String): String? {
        val ru = Regex("/RU=([^/]+)/").find(href)?.groupValues?.get(1)
        val target = if (ru != null) URLDecoder.decode(ru, "UTF-8") else href
        return target.takeIf { it.startsWith("http") }
    }

    private val rssItem = Regex("<item>(.*?)</item>", RegexOption.DOT_MATCHES_ALL)

    /** Noticias en RSS: titular, medio, fecha y enlace. */
    fun newsRss(xml: String): List<Map<String, String>> = rssItem.findAll(xml).map { item ->
        fun tag(name: String) = Regex("<$name[^>]*>(.*?)</$name>", RegexOption.DOT_MATCHES_ALL)
            .find(item.groupValues[1])?.groupValues?.get(1)
            ?.removePrefix("<![CDATA[")?.removeSuffix("]]>")
            ?.let { HtmlText.decodeEntities(it).trim() }.orEmpty()
        mapOf("title" to tag("title"), "link" to tag("link"), "date" to tag("pubDate"), "source" to tag("source"))
    }.toList()
}

/** Une y ordena lo que devuelven los buscadores. */
object SearchRanking {

    private val quoted = Regex("\"([^\"]+)\"")
    private val tokenSplit = Regex("[^\\p{L}\\p{N}_.-]+")
    private val siteOperator = Regex("\\bsite:\\S+")
    private val stopWords = setOf(
        "de", "la", "el", "los", "las", "un", "una", "y", "o", "en", "que", "por", "para", "con", "del", "al",
        "the", "of", "and", "or", "in", "to", "a", "is", "for", "on"
    )

    /**
     * Lo que un resultado tiene que contener para ser de verdad sobre la búsqueda: las frases
     * entre comillas y las palabras "raras" (nombres de usuario, códigos). Sin esto, buscar un
     * nombre poco común devuelve páginas parecidas y el agente concluía que no existe.
     */
    fun exactTerms(query: String): List<String> {
        val phrases = quoted.findAll(query).map { it.groupValues[1].trim() }.filter { it.isNotEmpty() }.toList()
        val rare = queryWords(query).filter { word ->
            word.length >= 5 && word.any { it.isDigit() } && word.any { it.isLetter() }
        }
        return (phrases + rare).distinctBy { it.lowercase() }
    }

    fun queryWords(query: String): List<String> =
        siteOperator.replace(query.lowercase(), " ")
            .replace("\"", " ")
            .split(tokenSplit)
            .map { it.trim('.', '-') }
            .filter { it.length > 1 && it !in stopWords }
            .distinct()

    /** @param results buscador → resultados en su orden original. */
    fun merge(query: String, results: Map<String, List<RawHit>>): List<SearchHit> {
        val terms = exactTerms(query).map { it.lowercase() }
        val words = queryWords(query)

        class Entry(val hit: RawHit, val bestRank: Int) {
            val engines = linkedSetOf<String>()
            var snippet = hit.snippet
        }

        val byUrl = linkedMapOf<String, Entry>()
        results.forEach { (engine, hits) ->
            hits.forEachIndexed { rank, hit ->
                val entry = byUrl.getOrPut(normalizeUrl(hit.url)) { Entry(hit, rank) }
                entry.engines += engine
                if (entry.snippet.length < hit.snippet.length) entry.snippet = hit.snippet
            }
        }

        class Ranked(val hit: SearchHit, val rank: Int)
        val merged = byUrl.values.map { entry ->
            val haystack = "${entry.hit.title} ${entry.hit.url} ${entry.snippet}".lowercase()
            Ranked(
                SearchHit(
                    title = entry.hit.title.take(160),
                    url = entry.hit.url,
                    snippet = entry.snippet.take(320),
                    engines = entry.engines,
                    exact = terms.isNotEmpty() && terms.all { it in haystack },
                    overlap = words.count { it in haystack }
                ),
                entry.bestRank
            )
        }.sortedWith(
            compareByDescending<Ranked> { it.hit.exact }
                .thenByDescending { it.hit.overlap }
                .thenByDescending { it.hit.engines.size }
                .thenBy { it.rank }
        ).map { it.hit }

        // Lo que no comparte ni una palabra con la consulta es ruido del buscador.
        val relevant = merged.filter { it.overlap > 0 }
        return relevant.ifEmpty { merged }
    }

    private fun normalizeUrl(url: String): String =
        url.lowercase()
            .substringBefore('#')
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .trimEnd('/')
}

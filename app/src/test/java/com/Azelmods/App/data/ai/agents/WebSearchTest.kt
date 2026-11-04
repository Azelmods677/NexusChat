package com.Azelmods.App.data.ai.agents

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** El HTML de estas pruebas está copiado de respuestas reales de cada buscador. */
class WebSearchTest : StringSpec({

    "DuckDuckGo: destino real tras su redirector" {
        val html = """
            <a rel="nofollow" class="result__a" href="//duckduckgo.com/l/?uddg=https%3A%2F%2Fgithub.com%2Fexample&amp;rut=abc">Example <b>repo</b></a>
            <a class="result__snippet" href="x">Un repositorio de ejemplo</a>
        """.trimIndent()
        val hits = SearchParsers.duckDuckGo(html)
        hits shouldHaveSize 1
        hits[0].url shouldBe "https://github.com/example"
        hits[0].title shouldBe "Example repo"
        hits[0].snippet shouldBe "Un repositorio de ejemplo"
    }

    "Bing: título dentro del encabezado y resumen en el pie" {
        val html = """
            <li class="b_algo" data-id iid=SERP.5265><link rel="stylesheet" href="/rp/a.css"/><div class="b_tpcn">
            <a class="tilk" href="https://www.daytrading.com/metatrader-5"><div class="tptt">daytrading.com</div></a></div>
            <div class="b_algoheader"><a href="https://www.daytrading.com/metatrader-5" h="ID=SERP,5093.2"><h2><strong>Best MetaTrader 5</strong> Brokers</h2></a></div>
            <div class="b_caption"><p class="b_lineclamp3"><span>2 de mar</span> Launched in 2010</p></div></li>
        """.trimIndent()
        val hits = SearchParsers.bing(html)
        hits shouldHaveSize 1
        hits[0].url shouldBe "https://www.daytrading.com/metatrader-5"
        hits[0].title shouldBe "Best MetaTrader 5 Brokers"
    }

    "Yahoo: destino dentro de RU= y título en aria-label" {
        val html = """
            <h3 class="title d-ib mt-42"><a class="s-title fz-m" data-yga="{}" referrerpolicy="origin" href="https://r.search.yahoo.com/_ylt=A2;_ylu=Y2/RV=2/RE=1/RO=10/RU=https%3a%2f%2f2077mods.com%2fmods/RK=2/RS=Uu-" aria-label="Cyberpunk 2077 Mods | 2077MODS Catalog"><span>2077mods.com</span>Cyberpunk</a></h3>
            <div class="compText"><p class="s-desc lh-20"> Browse Cyberpunk 2077 mods</p></div>
        """.trimIndent()
        val hits = SearchParsers.yahoo(html)
        hits shouldHaveSize 1
        hits[0].url shouldBe "https://2077mods.com/mods"
        hits[0].title shouldBe "Cyberpunk 2077 Mods | 2077MODS Catalog"
        hits[0].snippet shouldBe "Browse Cyberpunk 2077 mods"
    }

    "noticias en RSS" {
        val xml = "<rss><channel><item><title>Titular &amp; más</title><link>https://n.com/1</link>" +
            "<pubDate>Mon, 02 Nov 2026</pubDate><source url=\"https://n.com\">Medio</source></item></channel></rss>"
        val items = SearchParsers.newsRss(xml)
        items shouldHaveSize 1
        items[0]["title"] shouldBe "Titular & más"
        items[0]["source"] shouldBe "Medio"
    }

    "un nombre de usuario es un término que debe aparecer literalmente" {
        SearchRanking.exactTerms("azelmodsxc677") shouldContain "azelmodsxc677"
        SearchRanking.exactTerms("\"frase exacta\" otra cosa") shouldContain "frase exacta"
        SearchRanking.exactTerms("mejores móviles baratos") shouldBe emptyList()
    }

    "lo que contiene el término exacto va primero y el ruido se descarta" {
        val merged = SearchRanking.merge(
            "azelmodsxc677",
            mapOf(
                "A" to listOf(
                    RawHit("Cleopatra 1963", "https://yandex.com/video", "película"),
                    RawHit("Perfil de azelmodsxc677", "https://site.com/u/azelmodsxc677", "su perfil")
                )
            )
        )
        merged shouldHaveSize 1
        merged[0].exact shouldBe true
        merged[0].url shouldBe "https://site.com/u/azelmodsxc677"
    }

    "la misma página en dos buscadores se une y suma" {
        val merged = SearchRanking.merge(
            "kotlin coroutines guia",
            mapOf(
                "A" to listOf(RawHit("Kotlin coroutines", "https://www.kotlinlang.org/docs/", "guía")),
                "B" to listOf(RawHit("Kotlin coroutines", "https://kotlinlang.org/docs", "guía más larga sobre coroutines"))
            )
        )
        merged shouldHaveSize 1
        merged[0].engines shouldBe setOf("A", "B")
        merged[0].snippet shouldBe "guía más larga sobre coroutines"
    }

    // ── Protocolo ────────────────────────────────────────────────────────

    "la respuesta final en Markdown no se toca aunque traiga código con llaves" {
        val answer = "```kotlin\nfun main() {\n    println(\"hola\")\n}\n```\n\nEsto imprime hola."
        val decision = AgentProtocol.parseDecision(answer)
        decision.shouldBeInstanceOf<AgentDecision.Final>()
        decision.answer shouldBe answer
    }

    "un ejemplo de JSON dentro de una explicación larga no es una llamada" {
        val answer = "Para llamar a esta API tienes que enviar un cuerpo con la acción que quieres ejecutar. " +
            "Debe llevar los dos campos obligatorios, y el servidor responderá con el resultado de la operación:\n\n" +
            "```json\n{\"action\":\"web_search\",\"input\":\"hola\"}\n```"
        val decision = AgentProtocol.parseDecision(answer)
        decision.shouldBeInstanceOf<AgentDecision.Final>()
        decision.answer shouldBe answer
    }

    "el código puede ir en un bloque tras el JSON, sin escapar" {
        val raw = "{\"thought\":\"lo pruebo\",\"action\":\"run_code\",\"input\":\"python\"}\n" +
            "```python\nprint(\"hola\")\nprint(1 + 1)\n```"
        val decision = AgentProtocol.parseDecision(raw)
        decision.shouldBeInstanceOf<AgentDecision.UseTool>()
        decision.tool shouldBe "run_code"
        decision.input shouldBe "python\nprint(\"hola\")\nprint(1 + 1)"
    }

    "varias herramientas a la vez" {
        val raw = """{"thought":"leo dos","actions":[{"tool":"read_url","input":"https://a.com"},{"tool":"read_url","input":"https://b.com"}]}"""
        val decision = AgentProtocol.parseDecision(raw)
        decision.shouldBeInstanceOf<AgentDecision.UseTool>()
        decision.calls.map { it.input } shouldBe listOf("https://a.com", "https://b.com")
    }

    "los nombres inventados por los modelos llevan a la herramienta real" {
        AgentToolId.fromId("google_search") shouldBe AgentToolId.WEB_SEARCH
        AgentToolId.fromId("python") shouldBe AgentToolId.RUN_CODE
        AgentToolId.fromId("write_file") shouldBe AgentToolId.SAVE_FILE
        AgentToolId.fromId("no_existe") shouldBe null
    }
})

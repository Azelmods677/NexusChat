package com.Azelmods.App.data.ai.agents

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * El parser recibe lo que escriben modelos muy distintos (Gemini, GPT, DeepSeek,
 * modelos locales pequeños). Estos casos son los formatos reales con los que
 * responden, incluidos los "mal hechos".
 */
class AgentProtocolTest : StringSpec({

    "una llamada a herramienta en JSON limpio" {
        val d = AgentProtocol.parseDecision(
            """{"thought":"Necesito datos actuales","action":"web_search","input":"precio bitcoin hoy"}"""
        )
        d.shouldBeInstanceOf<AgentDecision.UseTool>()
        d.tool shouldBe "web_search"
        d.input shouldBe "precio bitcoin hoy"
        d.thought shouldBe "Necesito datos actuales"
    }

    "la respuesta final dentro de un bloque ```json con texto alrededor" {
        val raw = """
            Claro, aquí tienes:
            ```json
            {"thought":"Ya lo sé","final":"# Título\n\n- punto 1\n- punto 2"}
            ```
        """.trimIndent()
        val d = AgentProtocol.parseDecision(raw)
        d.shouldBeInstanceOf<AgentDecision.Final>()
        d.answer shouldBe "# Título\n\n- punto 1\n- punto 2"
    }

    "llaves y comillas escapadas dentro de las cadenas no rompen el JSON" {
        val d = AgentProtocol.parseDecision(
            """{"thought":"código","final":"fun f() { println(\"hola {mundo}\") }"}"""
        )
        d.shouldBeInstanceOf<AgentDecision.Final>()
        d.answer shouldBe "fun f() { println(\"hola {mundo}\") }"
    }

    "saltos de línea sin escapar dentro de la cadena (fallo típico de modelos pequeños)" {
        val raw = "{\"thought\":\"listo\",\"final\":\"línea 1\nlínea 2\"}"
        val d = AgentProtocol.parseDecision(raw)
        d.shouldBeInstanceOf<AgentDecision.Final>()
        d.answer shouldBe "línea 1\nlínea 2"
    }

    "prosa sin JSON se toma como respuesta final" {
        val d = AgentProtocol.parseDecision("La capital de Francia es París.")
        d.shouldBeInstanceOf<AgentDecision.Final>()
        d.answer shouldBe "La capital de Francia es París."
    }

    "action final o none no se trata como herramienta" {
        val d = AgentProtocol.parseDecision("""{"thought":"x","action":"final","final":"respuesta"}""")
        d.shouldBeInstanceOf<AgentDecision.Final>()
        d.answer shouldBe "respuesta"
    }

    "input como objeto se aplana a su valor" {
        val d = AgentProtocol.parseDecision("""{"action":"calculator","input":{"expression":"2+2"}}""")
        d.shouldBeInstanceOf<AgentDecision.UseTool>()
        d.input shouldBe "2+2"
    }

    "plan del coordinador con ids válidos" {
        val raw = """{"thought":"t","plan":[
            {"agent":"investigador","task":"Buscar precios"},
            {"agent":"analista","task":"Comparar"},
            {"agent":"desconocido","task":"Nada"}
        ]}"""
        val plan = AgentProtocol.parsePlan(raw, setOf("investigador", "analista", "redactor"))
        plan shouldHaveSize 2
        plan[0] shouldBe PlanStep("investigador", "Buscar precios")
        plan[1].agentId shouldBe "analista"
    }

    "plan como array suelto y recortado al máximo de pasos" {
        val raw = """[{"agent":"redactor","task":"a"},{"agent":"redactor","task":"b"},{"agent":"redactor","task":"c"}]"""
        AgentProtocol.parsePlan(raw, setOf("redactor"), maxSteps = 2) shouldHaveSize 2
    }

    "plan ilegible devuelve lista vacía (se usa el orden por defecto)" {
        AgentProtocol.parsePlan("no sé hacer planes", setOf("redactor")).shouldBeEmpty()
    }

    "extractThought lee el pensamiento del plan" {
        AgentProtocol.extractThought("""{"thought":"Primero datos","plan":[]}""") shouldBe "Primero datos"
    }
})

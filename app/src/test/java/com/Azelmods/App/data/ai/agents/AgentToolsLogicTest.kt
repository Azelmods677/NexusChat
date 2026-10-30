package com.Azelmods.App.data.ai.agents

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.types.shouldBeInstanceOf

class AgentToolsLogicTest : StringSpec({

    // ── Calculadora ──────────────────────────────────────────────────────

    "precedencia, paréntesis y potencias" {
        SafeCalculator.evaluate("2 + 3 * 4") shouldBe 14.0
        SafeCalculator.evaluate("(2 + 3) * 4") shouldBe 20.0
        SafeCalculator.evaluate("2 ^ 3 ^ 2") shouldBe 512.0 // asociativa por la derecha
        SafeCalculator.evaluate("-2 ^ 2") shouldBe 4.0
    }

    "porcentajes, coma decimal y funciones" {
        SafeCalculator.evaluate("80 * 15%") shouldBe (12.0 plusOrMinus 1e-9)
        SafeCalculator.evaluate("1,5 * 2") shouldBe 3.0
        SafeCalculator.evaluate("sqrt(16) + abs(-3)") shouldBe 7.0
        SafeCalculator.evaluate("2 × 3 ÷ 4") shouldBe 1.5
    }

    "cuota de un préstamo (fórmula real que usaría un agente)" {
        val cuota = SafeCalculator.evaluate("12000 * (0.07/12) / (1 - (1 + 0.07/12)^(-48))")
        cuota shouldBe (287.36 plusOrMinus 0.01)
    }

    "errores claros en vez de resultados inventados" {
        shouldThrow<SafeCalculator.CalculatorException> { SafeCalculator.evaluate("1 / 0") }
        shouldThrow<SafeCalculator.CalculatorException> { SafeCalculator.evaluate("2 +") }
        shouldThrow<SafeCalculator.CalculatorException> { SafeCalculator.evaluate("(1 + 2") }
        shouldThrow<SafeCalculator.CalculatorException> { SafeCalculator.evaluate("System.exit(0)") }
    }

    "formato legible" {
        SafeCalculator.format(14.0) shouldBe "14"
        SafeCalculator.format(0.1 + 0.2) shouldBe "0.3"
    }

    // ── URLs permitidas ──────────────────────────────────────────────────

    "webs públicas permitidas" {
        UrlGuard.check("https://es.wikipedia.org/wiki/Tor", torActive = false) shouldBe UrlGuard.Verdict.Allowed
    }

    "red local e IPs privadas bloqueadas" {
        listOf(
            "http://localhost:8080",
            "http://127.0.0.1/admin",
            "http://192.168.1.1",
            "http://10.0.0.5",
            "http://172.20.1.1",
            "http://impresora.local"
        ).forEach { url ->
            UrlGuard.check(url, torActive = true).shouldBeInstanceOf<UrlGuard.Verdict.Blocked>()
        }
    }

    "esquemas que no son http(s) bloqueados" {
        UrlGuard.check("file:///sdcard/secreto.txt", torActive = false).shouldBeInstanceOf<UrlGuard.Verdict.Blocked>()
        UrlGuard.check("javascript:alert(1)", torActive = false).shouldBeInstanceOf<UrlGuard.Verdict.Blocked>()
    }

    ".onion solo con Tor activo" {
        val onion = "http://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion/"
        UrlGuard.check(onion, torActive = false).shouldBeInstanceOf<UrlGuard.Verdict.Blocked>()
        UrlGuard.check(onion, torActive = true) shouldBe UrlGuard.Verdict.Allowed
    }

    // ── HTML a texto ─────────────────────────────────────────────────────

    "el HTML se convierte en texto legible sin scripts ni estilos" {
        val html = """
            <html><head><title>T</title><style>body{color:red}</style></head>
            <body><script>alert('x')</script>
            <h1>Hola &amp; adiós</h1><p>Uno&nbsp;dos</p><p>Tres &#8364; &#x41;</p>
            </body></html>
        """.trimIndent()
        val text = HtmlText.toPlainText(html)
        text shouldBe "Hola & adiós\nUno dos\nTres € A"
        text shouldNotContain "alert"
        text shouldNotContain "color:red"
    }

    "las entidades no se decodifican dos veces" {
        HtmlText.decodeEntities("&amp;lt;b&amp;gt;") shouldBe "&lt;b&gt;"
    }
})

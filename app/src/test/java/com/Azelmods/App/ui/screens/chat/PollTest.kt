package com.Azelmods.App.ui.screens.chat

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class PollTest : StringSpec({
    "una encuesta se recupera igual tras viajar como texto" {
        val poll = Poll("¿Vamos al cine? 🎬", listOf("Sí", "No, \"mejor\" otro día"))
        decodePoll(encodePoll(poll)) shouldBe poll
    }

    "un mensaje normal no se toma por encuesta" {
        decodePoll("hola") shouldBe null
        decodePoll("{\"q\":\"parece\",\"o\":[\"json\"]}") shouldBe null
    }

    "un contenido dañado no rompe el chat" {
        decodePoll(encodePoll(Poll("a", listOf("b", "c"))).dropLast(3)) shouldBe null
    }
})

package com.Azelmods.App.data.security.secret

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SecretRatchetTest : StringSpec({
    val alice = SecretRatchet.newKeyPair()
    val bob = SecretRatchet.newKeyPair()
    val rootA = SecretRatchet.sharedRoot(alice.private.encoded, bob.public.encoded, "chat1")
    val rootB = SecretRatchet.sharedRoot(bob.private.encoded, alice.public.encoded, "chat1")

    "los dos teléfonos llegan a la misma raíz" {
        rootA.toList() shouldBe rootB.toList()
    }

    "otro chat con las mismas claves tiene otra raíz" {
        SecretRatchet.sharedRoot(alice.private.encoded, bob.public.encoded, "chat2").toList() shouldNotBe rootA.toList()
    }

    "un mensaje cifrado por uno lo abre el otro" {
        val step = SecretRatchet.advance(SecretRatchet.chainFor(rootA, "alice"))
        val sealed = SecretRatchet.seal(step.messageKey, "hola ñandú 😀", "chat1|alice|0")
        val received = SecretRatchet.advance(SecretRatchet.chainFor(rootB, "alice"))
        SecretRatchet.open(received.messageKey, sealed, "chat1|alice|0") shouldBe "hola ñandú 😀"
    }

    "cada mensaje usa una clave distinta y la cadena avanza" {
        val first = SecretRatchet.advance(SecretRatchet.chainFor(rootA, "alice"))
        val second = SecretRatchet.advance(first.nextChain)
        first.messageKey.toList() shouldNotBe second.messageKey.toList()
    }

    "cada participante tiene su propia cadena" {
        SecretRatchet.chainFor(rootA, "alice").toList() shouldNotBe SecretRatchet.chainFor(rootA, "bob").toList()
    }

    "un mensaje alterado se rechaza" {
        val step = SecretRatchet.advance(SecretRatchet.chainFor(rootA, "alice"))
        val sealed = SecretRatchet.seal(step.messageKey, "texto", "ctx")
        sealed[sealed.size - 1] = (sealed[sealed.size - 1] + 1).toByte()
        shouldThrowAny { SecretRatchet.open(step.messageKey, sealed, "ctx") }
    }

    "un mensaje movido a otro contexto se rechaza" {
        val step = SecretRatchet.advance(SecretRatchet.chainFor(rootA, "alice"))
        val sealed = SecretRatchet.seal(step.messageKey, "texto", "chat1|alice|0")
        shouldThrowAny { SecretRatchet.open(step.messageKey, sealed, "chat1|alice|1") }
    }
})

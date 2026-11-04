package com.Azelmods.App.ui.components

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe

class ProfileColorsTest : StringSpec({
    "los colores ya publicados no cambian de posición" {
        // Cada cuenta guarda el índice: moverlos le cambiaría el color a quien ya eligió.
        ProfileColors.names.take(12) shouldBe listOf(
            "Jade", "Océano", "Aurora", "Coral", "Ámbar", "Fuego", "Bosque", "Índigo",
            "Neón", "Sakura", "Oro", "Grafito"
        )
    }

    "los colores Premium originales siguen siendo Premium" {
        (8..11).forEach { it shouldBeIn ProfileColors.premiumIndices }
    }

    "un color se ofrece una sola vez y en una sola sección" {
        (ProfileColors.freeIndices + ProfileColors.premiumIndices).sorted() shouldBe ProfileColors.palettes.indices.toList()
        ProfileColors.names.toSet().size shouldBe ProfileColors.names.size
    }

    "quien no eligió recibe uno de los ocho originales" {
        listOf("", "a", "uid-123", "otro-usuario", "ÑandÚ").forEach { seed ->
            ProfileColors.indexFor(seed) shouldBeIn (0..7).toList()
        }
    }

    "un índice desconocido no rompe la pantalla" {
        ProfileColors.indexFor("uid", chosen = 9_999) shouldBeIn (0..7).toList()
        ProfileColors.indexFor("uid", chosen = 39) shouldBe 39
    }
})

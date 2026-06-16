package com.Azelmods.App.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Los 15 temas de color de NexusChat.
 * Cada tema define primary + variantes automáticamente.
 */
object NexusColorSchemes {

    // ── Función helper para generar variantes ──────────────────────
    private fun buildScheme(
        primaryHex: Long,
        name: String
    ): Pair<String, NexusColors> {
        val primary = Color(primaryHex)
        return name to NexusColors(
            primary          = primary,
            primaryLight     = primary.copy(alpha = 0.7f),
            primaryDark      = primary.copy(alpha = 0.4f),
            primaryContainer = primary.copy(alpha = 0.15f),
            bubbleSent       = primary
        )
    }

    // ── Los 15 temas ──────────────────────────────────────────────
    val Purple   = buildScheme(0xFF7C6FE0, "Nexus Purple").second
    val Cyan     = buildScheme(0xFF00D4FF, "Ocean Cyan").second
    val Pink     = buildScheme(0xFFFF6B9D, "Neon Pink").second
    val Green    = buildScheme(0xFF00E676, "Matrix Green").second
    val Gold     = buildScheme(0xFFFFD700, "Royal Gold").second
    val Orange   = buildScheme(0xFFFF8C00, "Solar Orange").second
    val Red      = buildScheme(0xFFFF3D57, "Crimson Red").second
    val Teal     = buildScheme(0xFF1DE9B6, "Deep Teal").second
    val Indigo   = buildScheme(0xFF3D5AFE, "Electric Indigo").second
    val Rose     = buildScheme(0xFFFF4081, "Rose Quartz").second
    val Amber    = buildScheme(0xFFFFAB00, "Amber Glow").second
    val Emerald  = buildScheme(0xFF00BFA5, "Emerald City").second
    val Lavender = buildScheme(0xFFB39DDB, "Soft Lavender").second
    val Ice      = buildScheme(0xFF80D8FF, "Arctic Ice").second
    val Sunset   = buildScheme(0xFFFF6E40, "Sunset Blaze").second

    // Mapa de ID → scheme (para guardar en DataStore)
    val all: Map<String, NexusColors> = mapOf(
        "purple"   to Purple,
        "cyan"     to Cyan,
        "pink"     to Pink,
        "green"    to Green,
        "gold"     to Gold,
        "orange"   to Orange,
        "red"      to Red,
        "teal"     to Teal,
        "indigo"   to Indigo,
        "rose"     to Rose,
        "amber"    to Amber,
        "emerald"  to Emerald,
        "lavender" to Lavender,
        "ice"      to Ice,
        "sunset"   to Sunset
    )

    fun fromId(id: String): NexusColors = all[id] ?: Purple
}

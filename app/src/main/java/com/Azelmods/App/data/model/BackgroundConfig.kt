package com.Azelmods.App.data.model

/**
 * Background configuration for app-wide or per-chat backgrounds
 */
data class BackgroundConfig(
    val type: BackgroundType = BackgroundType.NONE,
    val colorHex: String? = null,
    val imageUri: String? = null,
    val videoUri: String? = null,
    val gradientColors: List<String> = emptyList(),
    val gradientAngle: Int = 135, // degrees
    val blurRadius: Float = 10f, // dp
    val overlayAlpha: Float = 0.4f // 0.0 to 0.8
)

/**
 * Background type enum
 */
enum class BackgroundType {
    NONE,           // No background (use default)
    SOLID_COLOR,    // Single color fill
    IMAGE,          // Image from gallery
    VIDEO,          // Video loop from gallery
    GRADIENT,       // Multi-color gradient
    BLUR,           // Blur the global background
    DEFAULT         // Use global app background (for per-chat)
}

/**
 * Preset colors for quick selection
 */
/**
 * Preset colors and gradients for quick selection
 */
object BackgroundPresets {
    val PRESET_COLORS = listOf(
        "#000000", // Black
        "#0A0A0A", // Near black
        "#1A0000", // Dark red
        "#2D0000", // Blood red dark
        "#8B0000", // Dark red
        "#CC0000", // Red
        "#FF0000", // Bright red
        "#1A1A2E", // Dark blue
        "#0F0F1A", // Dark purple
        "#111111", // Dark gray
        "#1F1F1F", // Gray
        "#2D2D44"  // Dark slate
    )
    
    // Named gradient presets for beautiful wallpapers
    data class GradientPreset(
        val name: String,
        val colors: List<String>
    )
    
    val GRADIENT_PRESETS = listOf(
        listOf("#FF0000", "#8B0000"), // Red gradient
        listOf("#CC0000", "#1A0000"), // Dark red gradient
        listOf("#000000", "#2D0000"), // Black to red
        listOf("#0A0A0A", "#1A1A2E"), // Dark to dark blue
        listOf("#7C3AED", "#5B21B6"), // Purple gradient
        listOf("#111111", "#000000")  // Gray to black
    )
    
    val NAMED_GRADIENT_PRESETS = listOf(
        // 🌅 Sunset & Warm
        GradientPreset("Atardecer", listOf("#FF6B35", "#F7931E", "#FFD700")),
        GradientPreset("Fuego", listOf("#FF0000", "#FF6600", "#FF9900")),
        GradientPreset("Lava", listOf("#CC0000", "#FF3300", "#1A0000")),
        GradientPreset("Rosa Dorado", listOf("#F093FB", "#F5576C", "#FFD700")),
        // 🌊 Ocean & Cool
        GradientPreset("Océano", listOf("#0077B6", "#00B4D8", "#90E0EF")),
        GradientPreset("Medianoche", listOf("#0F0C29", "#302B63", "#24243E")),
        GradientPreset("Hielo", listOf("#E0EAFC", "#CFDEF3", "#89CFF0")),
        GradientPreset("Mar Profundo", listOf("#000428", "#004E92")),
        // 🌌 Aurora & Cosmic
        GradientPreset("Aurora", listOf("#7B5CFA", "#00D4FF", "#00E676")),
        GradientPreset("Galaxia", listOf("#0F0F1A", "#7B2FBE", "#E040FB")),
        GradientPreset("Nebulosa", listOf("#1A002E", "#5C258D", "#4389A2")),
        GradientPreset("Cósmico", listOf("#000000", "#130F40", "#6C5CE7")),
        // 💜 Neon & Vibrant
        GradientPreset("Neón", listOf("#FF00FF", "#00FFFF")),
        GradientPreset("Cyberpunk", listOf("#FF006E", "#8338EC", "#3A86FF")),
        GradientPreset("Tropical", listOf("#11998E", "#38EF7D")),
        GradientPreset("Arcoíris", listOf("#FF0000", "#FF7700", "#FFFF00", "#00FF00", "#0000FF", "#8B00FF")),
        // 🌿 Nature & Earth
        GradientPreset("Bosque", listOf("#134E5E", "#71B280")),
        GradientPreset("Amanecer", listOf("#FF512F", "#F09819")),
        GradientPreset("Lavanda", listOf("#E6DEE9", "#A855F7", "#6D28D9")),
        GradientPreset("Tierra", listOf("#3E2723", "#795548", "#D7CCC8")),
        // 🖤 Dark & Elegant
        GradientPreset("Obsidiana", listOf("#000000", "#1A1A1A", "#333333")),
        GradientPreset("Carbón", listOf("#0F0F0F", "#2D2D2D", "#0F0F0F")),
        GradientPreset("Noche", listOf("#0A0A0A", "#141E30", "#243B55")),
        GradientPreset("Elegante", listOf("#1A1A2E", "#16213E", "#0F3460")),
        // 🎨 Pastel & Soft
        GradientPreset("Algodón", listOf("#FFECD2", "#FCB69F")),
        GradientPreset("Sueño", listOf("#A18CD1", "#FBC2EB")),
        GradientPreset("Melocotón", listOf("#FFE5D9", "#FFB4A2", "#E5989B")),
        GradientPreset("Menta", listOf("#D4EFDF", "#A3D9C9", "#7FB3A8"))
    )
}


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
    
    val GRADIENT_PRESETS = listOf(
        listOf("#FF0000", "#8B0000"), // Red gradient
        listOf("#CC0000", "#1A0000"), // Dark red gradient
        listOf("#000000", "#2D0000"), // Black to red
        listOf("#0A0A0A", "#1A1A2E"), // Dark to dark blue
        listOf("#7C3AED", "#5B21B6"), // Purple gradient
        listOf("#111111", "#000000")  // Gray to black
    )
}

package com.Azelmods.App.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Typography Scale - Using SansSerif for better emoji support
// FontFamily.Default can cause emoji rendering issues on some devices
// SansSerif provides better fallback to system emoji fonts
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,  // Better emoji support
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,  // Better emoji support
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,  // Better emoji support
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,  // Better emoji support
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,  // Better emoji support
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,  // Better emoji support
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )
)


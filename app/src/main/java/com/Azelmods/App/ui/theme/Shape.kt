package com.Azelmods.App.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),       // Buttons, chips
    medium = RoundedCornerShape(12.dp),     // Message bubbles
    large = RoundedCornerShape(16.dp),      // Cards, dialogs
    extraLarge = RoundedCornerShape(28.dp)  // Bottom sheets
)

// Custom Shapes
val AvatarShape = CircleShape                // 50% rounded (circle)
val StoryRingShape = CircleShape
val MessageBubbleShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 16.dp,
    bottomStart = 4.dp,
    bottomEnd = 16.dp
)

package com.Azelmods.App.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),  // Etiquetas
    small = RoundedCornerShape(8.dp),       // Chips, campos
    medium = RoundedCornerShape(10.dp),     // Botones, tarjetas
    large = RoundedCornerShape(14.dp),      // Diálogos
    extraLarge = RoundedCornerShape(20.dp)  // Hojas inferiores
)

// Formas propias
val AvatarShape = CircleShape
val StoryRingShape = CircleShape
val MessageBubbleShape = RoundedCornerShape(
    topStart = 14.dp,
    topEnd = 14.dp,
    bottomStart = 4.dp,
    bottomEnd = 14.dp
)

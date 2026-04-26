package com.Azelmods.App.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage

@Composable
fun UserAvatar(
    name: String,
    photoUrl: String?,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUrl.isNullOrBlank()) {
            // Load image with Coil
            AsyncImage(
                model = photoUrl,
                contentDescription = "Profile photo of $name",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
                onError = {
                    // Fallback to initials if image fails to load
                }
            )
        } else {
            // Fallback: Show initials with gradient background
            if (backgroundColor != null) {
                // Use solid color
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(backgroundColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = (size.value * 0.4f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Use gradient
                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(name.hashCode() or 0xFF000000.toInt()),
                                    Color((name.hashCode() shl 8) or 0xFF000000.toInt())
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = (size.value * 0.4f).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

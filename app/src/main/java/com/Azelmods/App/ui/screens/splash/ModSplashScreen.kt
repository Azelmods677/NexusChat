package com.Azelmods.App.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.ui.theme.rememberThemeColor
import kotlinx.coroutines.delay

/**
 * Mod-style splash screen with anime aesthetic
 * 
 * Features:
 * - Full screen black background
 * - Animated logo with red glow pulse
 * - Anime-style decorative lines
 * - App name with red gradient
 * - Version number
 * - 2.5s display then navigate
 */
@Composable
fun ModSplashScreen(
    navController: NavController,
    startDestination: String
) {
    val themeColor = rememberThemeColor()
    
    // Animation states
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    var fadeIn by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        // Fade in animation
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(800)
        ) { value, _ ->
            fadeIn = value
        }
        
        // Wait 2.5 seconds then navigate
        delay(2500)
        navController.navigate(startDestination) {
            popUpTo("mod_splash") { inclusive = true }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(fadeIn),
        contentAlignment = Alignment.Center
    ) {
        // Decorative lines radiating from center (anime style)
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(
                    Brush.radialGradient(
                        listOf(
                            themeColor.copy(alpha = glowAlpha * 0.3f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Logo placeholder with red glow
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(logoScale)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                themeColor.copy(alpha = glowAlpha),
                                themeColor.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "N",
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            // App name with red gradient
            Text(
                text = "NexusChat",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White,
                            themeColor,
                            Color.White
                        )
                    )
                )
            )
            
            // Subtitle
            Text(
                text = "by Azel Mods",
                fontSize = 14.sp,
                color = themeColor,
                fontWeight = FontWeight.Medium
            )
        }
        
        // Version at bottom
        Text(
            text = "v1.0.0",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

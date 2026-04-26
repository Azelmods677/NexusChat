package com.Azelmods.App.ui.screens.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.Pink
import com.Azelmods.App.ui.theme.Purple
import com.Azelmods.App.ui.theme.Teal

@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    
    var startAnimation by remember { mutableStateOf(false) }
    
    // Logo scale animation
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.4f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "logo_scale"
    )
    
    // Particle animations
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    
    val particle1OffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particle1"
    )
    
    val particle2OffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particle2"
    )
    
    val particle3OffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "particle3"
    )
    
    LaunchedEffect(Unit) {
        startAnimation = true
    }
    
    // Navigate when auth state is determined
    LaunchedEffect(state.isAuthenticated) {
        state.isAuthenticated?.let { isAuth ->
            if (isAuth) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            } else {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Purple.copy(alpha = 0.3f),
                        Color(0xFF0F0F1A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Floating particles
        Box(
            modifier = Modifier
                .offset(x = (-100).dp, y = particle1OffsetY.dp)
                .size(20.dp)
                .background(Purple.copy(alpha = 0.5f), CircleShape)
        )
        
        Box(
            modifier = Modifier
                .offset(x = 120.dp, y = particle2OffsetY.dp)
                .size(15.dp)
                .background(Teal.copy(alpha = 0.5f), CircleShape)
        )
        
        Box(
            modifier = Modifier
                .offset(x = 80.dp, y = particle3OffsetY.dp)
                .size(18.dp)
                .background(Pink.copy(alpha = 0.5f), CircleShape)
        )
        
        // Logo
        Text(
            text = "NC",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.scale(scale)
        )
    }
}

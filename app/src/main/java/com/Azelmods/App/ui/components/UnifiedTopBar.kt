package com.Azelmods.App.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.ripple
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.Azelmods.App.ui.theme.DarkSurface
import com.Azelmods.App.ui.theme.Purple
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * UNIFIED TOP BAR COMPONENT - ULTRA MODERN 2026 DESIGN
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * Componente ultra moderno con las últimas tendencias de diseño:
 * • Glassmorphism (efecto vidrio esmerilado)
 * • Animaciones fluidas y micro-interacciones
 * • Gradientes animados
 * • Indicadores de estado animados
 * • Transiciones suaves
 * 
 * @since 2026
 * @version 3.0.0 Ultra Modern Edition
 * @author AzelMods677
 * ═══════════════════════════════════════════════════════════════════════════
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedTopBar(
    title: String,
    modifier: Modifier = Modifier,
    // User Profile Info
    userName: String? = null,
    userPhotoUrl: String? = null,
    userSubtitle: String? = null,
    onUserClick: (() -> Unit)? = null,
    // Navigation
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    // Actions
    actions: @Composable RowScope.() -> Unit = {},
    // Styling
    backgroundColor: Color = DarkSurface,
    contentColor: Color = Color.White,
    // Modern Effects
    enableGlassmorphism: Boolean = true,
    enableAnimations: Boolean = true
) {
    // Coroutine scope for animations
    val coroutineScope = rememberCoroutineScope()
    
    // Animated gradient for modern look
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradient_offset"
    )
    
    // Scale animation for interactions
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    // Glassmorphism effect — translucent gradient background.
    // NOTE: blur() must never wrap the content (title/icons/avatar) or it
    // renders the whole bar blurry. We only tint the background here.
    val glassBackground = if (enableGlassmorphism) {
        Modifier
            .background(
                Brush.verticalGradient(
                    listOf(
                        backgroundColor.copy(alpha = 0.92f),
                        backgroundColor.copy(alpha = 0.98f)
                    )
                )
            )
    } else {
        Modifier.background(backgroundColor)
    }
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color.Transparent,
        tonalElevation = 8.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(glassBackground)
        ) {
            // Animated gradient overlay
            if (enableAnimations) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Purple.copy(alpha = 0.1f),
                                    Color.Transparent,
                                    Purple.copy(alpha = 0.1f)
                                ),
                                startX = gradientOffset,
                                endX = gradientOffset + 500f
                            )
                        )
                )
            }
            
            TopAppBar(
                title = {
                    if (userName != null) {
                        // User Profile Mode with animations
                        AnimatedContent(
                            targetState = userName,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) + 
                                    slideInVertically { it / 2 } togetherWith
                                    fadeOut(animationSpec = tween(300)) + 
                                    slideOutVertically { -it / 2 }
                            },
                            label = "user_content"
                        ) { targetUserName ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .then(
                                        if (onUserClick != null) {
                                            Modifier.clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) {
                                                isPressed = true
                                                onUserClick()
                                                coroutineScope.launch {
                                                    delay(100)
                                                    isPressed = false
                                                }
                                            }
                                        } else Modifier
                                    )
                                    .scale(scale),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                // Animated profile photo with gradient ring
                                Box(
                                    modifier = Modifier.size(46.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Animated gradient ring
                                    if (enableAnimations) {
                                        val ringRotation by infiniteTransition.animateFloat(
                                            initialValue = 0f,
                                            targetValue = 360f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(2000, easing = LinearEasing)
                                            ),
                                            label = "ring"
                                        )
                                        
                                        androidx.compose.foundation.Canvas(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .graphicsLayer { rotationZ = ringRotation }
                                        ) {
                                            drawCircle(
                                                brush = Brush.sweepGradient(
                                                    listOf(
                                                        Purple,
                                                        Color(0xFF00D4FF),
                                                        Purple
                                                    )
                                                ),
                                                radius = size.minDimension / 2,
                                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                            )
                                        }
                                    }
                                    
                                    // Profile photo
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                Brush.radialGradient(
                                                    listOf(
                                                        Purple.copy(alpha = 0.5f),
                                                        Purple.copy(alpha = 0.3f)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!userPhotoUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = userPhotoUrl,
                                                contentDescription = "Profile Photo",
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(
                                                text = targetUserName.take(1).uppercase(),
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                // Name + Subtitle with animations
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = targetUserName,
                                        color = contentColor,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    
                                    if (!userSubtitle.isNullOrBlank()) {
                                        AnimatedContent(
                                            targetState = userSubtitle,
                                            transitionSpec = {
                                                fadeIn(tween(200)) togetherWith fadeOut(tween(200))
                                            },
                                            label = "subtitle"
                                        ) { subtitle ->
                                            Text(
                                                text = subtitle,
                                                color = contentColor.copy(alpha = 0.7f),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Standard Mode: Just show title with gradient
                        Text(
                            text = title,
                            color = contentColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = LocalTextStyle.current.copy(
                                brush = if (enableAnimations) {
                                    Brush.linearGradient(
                                        listOf(
                                            contentColor,
                                            contentColor.copy(alpha = 0.8f),
                                            contentColor
                                        )
                                    )
                                } else null
                            )
                        )
                    }
                },
                navigationIcon = {
                    if (showBackButton && onBackClick != null) {
                        ModernIconButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBackClick,
                            tint = contentColor,
                            enableAnimation = enableAnimations
                        )
                    }
                },
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = contentColor,
                    navigationIconContentColor = contentColor,
                    actionIconContentColor = contentColor
                )
            )
        }
    }
}

/**
 * Modern icon button with animations
 */
@Composable
fun ModernIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = Color.White,
    enableAnimation: Boolean = true
) {
    // Coroutine scope for animations
    val coroutineScope = rememberCoroutineScope()
    
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    val buttonBackground = if (enableAnimation) {
        tint.copy(alpha = if (isPressed) 0.2f else 0.1f)
    } else {
        Color.Transparent
    }
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(if (enableAnimation) scale else 1f)
            .clip(CircleShape)
            .background(buttonBackground)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true)
            ) {
                isPressed = true
                onClick()
                coroutineScope.launch {
                    delay(100)
                    isPressed = false
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Profile TopBar for profile screens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedProfileTopBar(
    userName: String,
    userPhotoUrl: String? = null,
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = userName,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            ModernIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                onClick = onBackClick,
                tint = Color.White
            )
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = Color.White,
            actionIconContentColor = Color.White
        ),
        modifier = modifier.statusBarsPadding()
    )
}

/**
 * Modern action icon for toolbars
 */
@Composable
fun TopBarActionIcon(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    tint: Color = Color.White
) {
    ModernIconButton(
        icon = icon,
        contentDescription = contentDescription,
        onClick = onClick,
        tint = tint
    )
}

package com.Azelmods.App.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.BuildConfig
import com.Azelmods.App.ui.components.NexusGlassCard
import com.Azelmods.App.ui.components.NexusStatusBadge
import com.Azelmods.App.ui.theme.NexusTokens
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    
    // Animaciones
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "about_anim")
    
    // Logo pulse
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "pulse"
    )
    
    // Gradiente del anillo rotando
    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "ring"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Acerca de", 
                        fontWeight = FontWeight.Bold,
                        color = NexusTokens.Color.TextPrimary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = NexusTokens.Color.TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NexusTokens.Color.BgBase
                )
            )
        },
        containerColor = NexusTokens.Color.BgBase
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(NexusTokens.Gradient.Background))
        ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + NexusTokens.Space.xxl,
                    bottom = NexusTokens.Space.xxxl,
                    start = NexusTokens.Space.md,
                    end = NexusTokens.Space.md
                ),
                verticalArrangement = Arrangement.spacedBy(NexusTokens.Space.md)
            ) {
                
                // ── LOGO ────────────────────────────────
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(800)) + scaleIn(tween(800))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                                // Anillo exterior rotando
                                Box(
                                    Modifier
                                        .size(140.dp)
                                        .rotate(ringRotation)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.sweepGradient(NexusTokens.Gradient.AzelAI)
                                        )
                                )
                                // Separador
                                Box(
                                    Modifier
                                        .size(132.dp)
                                        .clip(CircleShape)
                                        .background(NexusTokens.Color.BgBase)
                                )
                                // Logo
                                Box(
                                    Modifier
                                        .size(110.dp)
                                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                                        .clip(RoundedCornerShape(NexusTokens.Radius.xxl))
                                        .background(Brush.linearGradient(NexusTokens.Gradient.Brand)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "NC",
                                        fontSize = 42.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(Modifier.height(NexusTokens.Space.lg))
                        }
                    }
                }
                
                // ── NOMBRE + VERSIÓN ────────────────────
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(800, delayMillis = 200)) + slideInVertically(
                            tween(800, delayMillis = 200)
                        ) { 50 }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "NexusChat",
                                color = NexusTokens.Color.TextPrimary,
                                fontSize = NexusTokens.FontSize.h1,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                "by AzelMods677",
                                color = NexusTokens.Color.Secondary,
                                fontSize = NexusTokens.FontSize.md,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(NexusTokens.Space.sm))
                            Row(horizontalArrangement = Arrangement.spacedBy(NexusTokens.Space.sm)) {
                                NexusStatusBadge(
                                    "v${BuildConfig.VERSION_NAME}",
                                    NexusTokens.Color.Primary
                                )
                                NexusStatusBadge("2026", NexusTokens.Color.Secondary)
                                NexusStatusBadge("MIT", NexusTokens.Color.Gold)
                            }
                        }
                    }
                }
                
                item { Spacer(Modifier.height(NexusTokens.Space.xl)) }
                
                // ── FEATURES GRID ───────────────────────
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(800, delayMillis = 400)) + slideInVertically(
                            tween(800, delayMillis = 400)
                        ) { 50 }
                    ) {
                        Column {
                            val features = listOf(
                                Triple("🔐", "Cifrado E2EE", "Grado militar"),
                                Triple("🤖", "AzelAI", "Multi-proveedor IA"),
                                Triple("📹", "WebRTC HD", "Voz + Video P2P"),
                                Triple("🧅", "Tor Browser", "Privacidad total"),
                                Triple("💻", "Terminal", "Shell local real"),
                                Triple("🎨", "15 Temas", "Ultra personalizable")
                            )
                            features.chunked(2).forEach { row ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(NexusTokens.Space.sm)
                                ) {
                                    row.forEach { (icon, title, sub) ->
                                        NexusGlassCard(modifier = Modifier.weight(1f)) {
                                            Text(icon, fontSize = 26.sp)
                                            Spacer(Modifier.height(NexusTokens.Space.xs))
                                            Text(
                                                title,
                                                color = NexusTokens.Color.TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = NexusTokens.FontSize.md
                                            )
                                            Text(
                                                sub,
                                                color = NexusTokens.Color.TextMuted,
                                                fontSize = NexusTokens.FontSize.sm
                                            )
                                        }
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                                Spacer(Modifier.height(NexusTokens.Space.sm))
                            }
                        }
                    }
                }
                
                item { Spacer(Modifier.height(NexusTokens.Space.md)) }
                
                // ── STACK TÉCNICO ───────────────────────
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(800, delayMillis = 600)) + slideInVertically(
                            tween(800, delayMillis = 600)
                        ) { 50 }
                    ) {
                        NexusGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderGlow = true
                        ) {
                            Text(
                                "🛠 Stack Técnico",
                                color = NexusTokens.Color.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = NexusTokens.FontSize.lg
                            )
                            Spacer(Modifier.height(NexusTokens.Space.sm))
                            val stack = listOf(
                                "Kotlin 100%" to NexusTokens.Color.Primary,
                                "Jetpack Compose" to NexusTokens.Color.Secondary,
                                "Hilt DI" to NexusTokens.Color.Accent,
                                "Firebase BOM 33.7" to NexusTokens.Color.Gold,
                                "WebRTC 1.1.3" to NexusTokens.Color.Online,
                                "Clean Architecture" to NexusTokens.Color.PrimaryLight
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(NexusTokens.Space.xs),
                                verticalArrangement = Arrangement.spacedBy(NexusTokens.Space.xs)
                            ) {
                                stack.forEach { (name, color) ->
                                    NexusStatusBadge(name, color)
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(Modifier.height(NexusTokens.Space.md)) }
                
                // ── LINKS ───────────────────────────────
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(800, delayMillis = 800)) + slideInVertically(
                            tween(800, delayMillis = 800)
                        ) { 50 }
                    ) {
                        Column {
                            val links = listOf(
                                Triple("▶", "YouTube", "@AzelModsx677") to "https://youtube.com/@AzelModsx677",
                                Triple("✈", "Telegram", "t.me/AzelModsx7779") to "https://t.me/AzelModsx7779",
                                Triple("♪", "TikTok", "@azelmods677") to "https://tiktok.com/@azelmods677"
                            )
                            links.forEach { (data, url) ->
                                val (icon, platform, handle) = data
                                NexusGlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { uriHandler.openUri(url) }
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(NexusTokens.Space.sm),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(icon, fontSize = 20.sp)
                                            Column {
                                                Text(
                                                    platform,
                                                    color = NexusTokens.Color.TextPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = NexusTokens.FontSize.md
                                                )
                                                Text(
                                                    handle,
                                                    color = NexusTokens.Color.TextMuted,
                                                    fontSize = NexusTokens.FontSize.sm
                                                )
                                            }
                                        }
                                        Text("→", color = NexusTokens.Color.Primary, fontSize = 18.sp)
                                    }
                                }
                                Spacer(Modifier.height(NexusTokens.Space.xs))
                            }
                        }
                    }
                }
                
                // ── FOOTER ──────────────────────────────
                item {
                    Spacer(Modifier.height(NexusTokens.Space.md))
                    Text(
                        "© 2026 AzelMods677 · MIT License",
                        color = NexusTokens.Color.TextDisabled,
                        fontSize = NexusTokens.FontSize.xs,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "⭐ Si te gusta el proyecto, dale una estrella en GitHub",
                        color = NexusTokens.Color.TextMuted,
                        fontSize = NexusTokens.FontSize.xs,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

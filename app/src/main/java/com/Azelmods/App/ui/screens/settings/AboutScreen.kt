package com.Azelmods.App.ui.screens.settings

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

private const val REPO_URL = "https://github.com/Azelmods677/NexusChat"

/**
 * Pantalla "Acerca de" — cierre de la v5.
 *
 * Criterio: una ficha técnica de la que uno pueda fiarse. Todo lo que se afirma aquí
 * está verificado contra el código o contra `build.gradle.kts`; si algo no está
 * implementado, se dice, no se adorna.
 *
 * Correcciones respecto a la versión anterior:
 *  - "Cifrado E2EE — Grado militar": eslogan sin significado técnico. Ahora se nombra
 *    el algoritmo real (ECDH P-256 + AES-256-GCM) y se acota su alcance.
 *  - "15 Temas": son 25 acentos (`AppTheme.ACCENT_SWATCHES`).
 *  - Se invitaba a dar una estrella en GitHub sin enlazar el repositorio en ninguna
 *    parte. Ahora el repositorio es el primer enlace.
 *  - Versiones de librerías corregidas y alineadas con el build real.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AboutScreen(navController: NavController) {
    val uriHandler = LocalUriHandler.current

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }

    val infiniteTransition = rememberInfiniteTransition(label = "about_anim")

    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ), label = "pulse"
    )

    val ringRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
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
                    containerColor = Color.Transparent
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
                    top = padding.calculateTopPadding() + NexusTokens.Space.lg,
                    bottom = NexusTokens.Space.xxxl,
                    start = NexusTokens.Space.md,
                    end = NexusTokens.Space.md
                ),
                verticalArrangement = Arrangement.spacedBy(NexusTokens.Space.md)
            ) {

                // ── IDENTIDAD ───────────────────────────────────────────────
                item {
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(800)) + scaleIn(tween(800))
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.size(132.dp), contentAlignment = Alignment.Center) {
                                Box(
                                    Modifier
                                        .size(132.dp)
                                        .rotate(ringRotation)
                                        .clip(CircleShape)
                                        .background(Brush.sweepGradient(NexusTokens.Gradient.AzelAI))
                                )
                                Box(
                                    Modifier
                                        .size(124.dp)
                                        .clip(CircleShape)
                                        .background(NexusTokens.Color.BgBase)
                                )
                                Box(
                                    Modifier
                                        .size(104.dp)
                                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                                        .clip(RoundedCornerShape(NexusTokens.Radius.xxl))
                                        .background(Brush.linearGradient(NexusTokens.Gradient.Brand)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "NC",
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(Modifier.height(NexusTokens.Space.lg))
                        }
                    }
                }

                item {
                    Reveal(visible, 200) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Nexus Chat",
                                color = NexusTokens.Color.TextPrimary,
                                fontSize = NexusTokens.FontSize.h1,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                            Spacer(Modifier.height(NexusTokens.Space.xs))
                            Text(
                                "Mensajería con la privacidad como requisito de diseño",
                                color = NexusTokens.Color.TextMuted,
                                fontSize = NexusTokens.FontSize.sm,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = NexusTokens.Space.lg)
                            )
                            Spacer(Modifier.height(NexusTokens.Space.md))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(NexusTokens.Space.xs),
                                verticalArrangement = Arrangement.spacedBy(NexusTokens.Space.xs)
                            ) {
                                NexusStatusBadge("v${BuildConfig.VERSION_NAME}", NexusTokens.Color.Primary)
                                NexusStatusBadge("Android 12+", NexusTokens.Color.Secondary)
                                NexusStatusBadge("MIT", NexusTokens.Color.Gold)
                                NexusStatusBadge("Open Source", NexusTokens.Color.Online)
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(NexusTokens.Space.sm)) }

                // ── AUTORÍA ─────────────────────────────────────────────────
                item {
                    Reveal(visible, 300) {
                        NexusGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            borderGlow = true
                        ) {
                            Text(
                                "Desarrollado por",
                                color = NexusTokens.Color.TextMuted,
                                fontSize = NexusTokens.FontSize.xs
                            )
                            Spacer(Modifier.height(NexusTokens.Space.xs))
                            Text(
                                "Azel Mods",
                                color = NexusTokens.Color.TextPrimary,
                                fontSize = NexusTokens.FontSize.xxl,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.height(NexusTokens.Space.xs))
                            Text(
                                "Autor y propietario único del proyecto. Diseño, arquitectura, " +
                                    "backend e interfaz desarrollados de principio a fin por una sola persona.",
                                color = NexusTokens.Color.TextSecondary,
                                fontSize = NexusTokens.FontSize.sm,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                // ── CAPACIDADES ─────────────────────────────────────────────
                item {
                    Reveal(visible, 400) {
                        Column {
                            SectionTitle("Qué incluye")
                            Spacer(Modifier.height(NexusTokens.Space.sm))
                            val features = listOf(
                                Triple("🔐", "Cifrado E2EE", "ECDH P-256 + AES-256-GCM"),
                                Triple("🤖", "Azel IA", "Proveedor y modelo a tu elección"),
                                Triple("📹", "Llamadas P2P", "Voz y vídeo con WebRTC"),
                                Triple("🧅", "Red Tor", "Navegación .onion vía Orbot"),
                                Triple("📸", "Historias", "24 h, con música y dibujo"),
                                Triple("💻", "Editor y terminal", "Resaltado y vista previa"),
                                Triple("🎨", "25 acentos", "Fondos de imagen y vídeo"),
                                Triple("🔔", "Push propias", "Servidas por Cloud Functions")
                            )
                            features.chunked(2).forEach { row ->
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(NexusTokens.Space.sm)
                                ) {
                                    row.forEach { (icon, title, sub) ->
                                        NexusGlassCard(modifier = Modifier.weight(1f)) {
                                            Text(icon, fontSize = 24.sp)
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
                                                fontSize = NexusTokens.FontSize.xs,
                                                lineHeight = 15.sp
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

                // ── ALCANCE REAL DEL CIFRADO ────────────────────────────────
                // Un "acerca de" que promete más seguridad de la que hay es un
                // problema, no un adorno: el usuario decide qué contar por el chat
                // basándose en esto.
                item {
                    Reveal(visible, 500) {
                        NexusGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "🛡  Alcance del cifrado",
                                color = NexusTokens.Color.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = NexusTokens.FontSize.lg
                            )
                            Spacer(Modifier.height(NexusTokens.Space.sm))
                            Text(
                                "Los chats de dos personas se cifran en el dispositivo: la clave " +
                                    "privada nunca sale de él y el servidor solo almacena texto cifrado.",
                                color = NexusTokens.Color.TextSecondary,
                                fontSize = NexusTokens.FontSize.sm,
                                lineHeight = 19.sp
                            )
                            Spacer(Modifier.height(NexusTokens.Space.sm))
                            Text(
                                "Con la misma honestidad: los grupos todavía no van cifrados de " +
                                    "extremo a extremo, no hay secreto hacia adelante (las claves de " +
                                    "identidad son estables) y los metadatos —quién habla con quién y " +
                                    "cuándo— siguen siendo visibles para el servidor.",
                                color = NexusTokens.Color.TextMuted,
                                fontSize = NexusTokens.FontSize.sm,
                                lineHeight = 19.sp
                            )
                        }
                    }
                }

                // ── STACK ───────────────────────────────────────────────────
                item {
                    Reveal(visible, 600) {
                        NexusGlassCard(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "🛠  Stack técnico",
                                color = NexusTokens.Color.TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = NexusTokens.FontSize.lg
                            )
                            Spacer(Modifier.height(NexusTokens.Space.sm))
                            val stack = listOf(
                                "Kotlin 2.1.20" to NexusTokens.Color.Primary,
                                "Jetpack Compose" to NexusTokens.Color.Secondary,
                                "Material 3" to NexusTokens.Color.Secondary,
                                "Hilt 2.54" to NexusTokens.Color.Accent,
                                "Firebase BOM 33.9" to NexusTokens.Color.Gold,
                                "WebRTC 1.1.3" to NexusTokens.Color.Online,
                                "Room" to NexusTokens.Color.PrimaryLight,
                                "Clean Architecture" to NexusTokens.Color.PrimaryLight
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(NexusTokens.Space.xs),
                                verticalArrangement = Arrangement.spacedBy(NexusTokens.Space.xs)
                            ) {
                                stack.forEach { (name, color) -> NexusStatusBadge(name, color) }
                            }
                        }
                    }
                }

                // ── ENLACES ─────────────────────────────────────────────────
                item {
                    Reveal(visible, 700) {
                        Column {
                            SectionTitle("Enlaces")
                            Spacer(Modifier.height(NexusTokens.Space.sm))
                            val links = listOf(
                                LinkRow("⌘", "Código fuente", "github.com/Azelmods677/NexusChat", REPO_URL),
                                LinkRow("▶", "YouTube", "@AzelModsx677", "https://youtube.com/@AzelModsx677"),
                                LinkRow("✈", "Telegram", "t.me/AzelModsx7779", "https://t.me/AzelModsx7779"),
                                LinkRow("♪", "TikTok", "@azelmods677", "https://tiktok.com/@azelmods677")
                            )
                            links.forEach { link ->
                                NexusGlassCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { uriHandler.openUri(link.url) }
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
                                            Text(link.icon, fontSize = 18.sp)
                                            Column {
                                                Text(
                                                    link.platform,
                                                    color = NexusTokens.Color.TextPrimary,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = NexusTokens.FontSize.md
                                                )
                                                Text(
                                                    link.handle,
                                                    color = NexusTokens.Color.TextMuted,
                                                    fontSize = NexusTokens.FontSize.xs
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

                // ── PIE ─────────────────────────────────────────────────────
                item {
                    Reveal(visible, 800) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Spacer(Modifier.height(NexusTokens.Space.md))
                            Text(
                                "Plantilla libre para desarrolladores. Clónala, estúdiala y " +
                                    "construye la tuya encima.",
                                color = NexusTokens.Color.TextMuted,
                                fontSize = NexusTokens.FontSize.xs,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            Spacer(Modifier.height(NexusTokens.Space.sm))
                            Text(
                                "© 2026 Azel Mods · Licencia MIT",
                                color = NexusTokens.Color.TextDisabled,
                                fontSize = NexusTokens.FontSize.xs,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Entrada escalonada: cada bloque aparece un poco después del anterior.
 *
 * Se declara al nivel del fichero y no como función local dentro de [AboutScreen]:
 * los composables locales complican el trabajo del compilador de Compose y no
 * aportan nada aquí.
 */
@Composable
private fun Reveal(
    visible: Boolean,
    delayMs: Int,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600, delayMillis = delayMs)) +
            slideInVertically(tween(600, delayMillis = delayMs)) { 40 }
    ) { content() }
}

/** Enlace externo de la ficha. */
private data class LinkRow(
    val icon: String,
    val platform: String,
    val handle: String,
    val url: String
)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = NexusTokens.Color.TextSecondary,
        fontSize = NexusTokens.FontSize.sm,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Start
    )
}

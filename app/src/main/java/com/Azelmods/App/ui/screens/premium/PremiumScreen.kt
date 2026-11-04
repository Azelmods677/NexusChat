package com.Azelmods.App.ui.screens.premium

import com.Azelmods.App.i18n.tr

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.data.repository.PremiumPlan
import com.Azelmods.App.ui.components.staggeredEnter
import com.Azelmods.App.ui.theme.NexusTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Gold = NexusTokens.Color.Gold
private val GoldLight = Color(0xFFF3D27A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    navController: NavController,
    viewModel: PremiumViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // El cobro ocurre en Mercado Pago, fuera de la app. Al volver, el estado cambia solo.
    LaunchedEffect(state.checkoutUrl) {
        state.checkoutUrl?.let { url ->
            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            viewModel.onCheckoutOpened()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("NexusChat Premium")) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = NexusTokens.Color.BgDeep
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PremiumEmblem(active = state.status.isActiveNow())
            Spacer(Modifier.height(16.dp))
            Text(
                if (state.status.isActiveNow()) tr("Ya eres Premium") else tr("Haz tuyo NexusChat"),
                color = NexusTokens.Color.TextPrimary,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (state.status.isActiveNow()) activeDescription(state.status.expiresAt)
                else tr("Más personalización y apoyo directo al desarrollo"),
                color = NexusTokens.Color.TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
            Spacer(Modifier.height(24.dp))

            Benefit(Icons.Filled.Verified, "Insignia Premium", "Junto a tu nombre, a la vista en tu perfil", 0)
            Benefit(Icons.Outlined.Palette, "Colores exclusivos", "Galaxia, Arcoíris, Oro y 13 más para tu perfil", 1)
            Benefit(Icons.Outlined.Collections, "Galería sin límite", "Todas las fotos y vídeos de perfil que quieras (10 sin Premium)", 2)
            Benefit(Icons.Outlined.FavoriteBorder, "Apoyas el proyecto", "NexusChat es independiente y sin anuncios", 3)

            if (!state.status.isActiveNow()) {
                Spacer(Modifier.height(20.dp))
                AnimatedContent(
                    targetState = Triple(state.isLoadingPlans, state.plansError, state.plans),
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                    label = "planes"
                ) { (loading, error, plans) ->
                    when {
                        loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Gold, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                        }
                        error != null -> UnavailableCard(error, viewModel::loadPlans)
                        else -> Column {
                            plans.forEach { plan ->
                                PlanRow(plan, plan.id == state.selectedPlanId) { viewModel.selectPlan(plan.id) }
                            }
                        }
                    }
                }

                if (state.plans.isNotEmpty()) {
                    state.checkoutError?.let { message ->
                        Text(
                            message,
                            color = NexusTokens.Color.Error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = viewModel::startCheckout,
                        enabled = !state.isOpeningCheckout && state.selectedPlanId != null,
                        shape = RoundedCornerShape(NexusTokens.Radius.xl),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Color.Black),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(54.dp)
                    ) {
                        if (state.isOpeningCheckout) {
                            CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                        } else {
                            Text(tr("Continuar con Mercado Pago"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Lock, null, tint = NexusTokens.Color.TextMuted, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            tr("Pagas en la web de Mercado Pago: NexusChat nunca ve tu tarjeta. Premium se activa " +
                                "solo en cuanto se confirma el pago."),
                            color = NexusTokens.Color.TextMuted,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }

    if (state.justActivated) {
        ActivationCelebration(onDone = viewModel::onCelebrationShown)
    }
}

private fun activeDescription(expiresAt: Long?): String =
    if (expiresAt == null) tr("Tu Premium es para siempre. ¡Gracias por apoyar el proyecto!")
    else "Activo hasta el ${SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale.getDefault()).format(Date(expiresAt))}"

/** Emblema con un anillo dorado que gira: es lo primero que se ve y marca el tono de la pantalla. */
@Composable
private fun PremiumEmblem(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "emblema")
    val angle by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(7_000, easing = LinearEasing)),
        label = "giro"
    )
    val glow by transition.animateFloat(
        0.35f, 0.75f,
        infiniteRepeatable(tween(1_800), RepeatMode.Reverse),
        label = "brillo"
    )
    val entrance = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Box(
        Modifier
            .padding(top = 8.dp)
            .size(132.dp)
            .scale(entrance.value),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(listOf(Gold.copy(alpha = glow), Color.Transparent)),
                radius = size.minDimension / 2f
            )
            rotate(angle) {
                drawCircle(
                    Brush.sweepGradient(listOf(GoldLight, Gold, Color.Transparent, GoldLight)),
                    radius = size.minDimension / 2.7f,
                    style = Stroke(width = 5f)
                )
            }
        }
        Icon(
            if (active) Icons.Filled.Check else Icons.Filled.Verified,
            null,
            tint = GoldLight,
            modifier = Modifier.size(52.dp)
        )
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, text: String, index: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .staggeredEnter(index)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(NexusTokens.Radius.lg))
                .background(Gold.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = Gold, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(tr(title), color = NexusTokens.Color.TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(tr(text), color = NexusTokens.Color.TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PlanRow(plan: PremiumPlan, selected: Boolean, onClick: () -> Unit) {
    val border by animateColorAsState(if (selected) Gold else NexusTokens.Color.Hairline, label = "bordePlan")
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(if (selected) Gold.copy(alpha = 0.10f) else NexusTokens.Color.BgSurface)
            .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(NexusTokens.Radius.xl))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .border(2.dp, if (selected) Gold else NexusTokens.Color.TextMuted, CircleShape)
                .background(if (selected) Gold else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            if (selected) Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plan.title, color = NexusTokens.Color.TextPrimary, style = MaterialTheme.typography.titleMedium)
                plan.badge?.let { badge ->
                    Spacer(Modifier.width(8.dp))
                    Text(
                        badge,
                        color = Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(NexusTokens.Radius.xs))
                            .background(Gold)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (plan.period.isNotBlank()) {
                Text(plan.period, color = NexusTokens.Color.TextMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Text(plan.priceLabel, color = NexusTokens.Color.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun UnavailableCard(message: String, onRetry: () -> Unit) {
    Surface(
        color = NexusTokens.Color.BgSurface,
        shape = RoundedCornerShape(NexusTokens.Radius.xl),
        border = BorderStroke(1.dp, NexusTokens.Color.Hairline),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.CloudOff, null, tint = NexusTokens.Color.TextMuted)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                color = NexusTokens.Color.TextSecondary,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                tr("No se te ha cobrado nada."),
                color = NexusTokens.Color.TextMuted,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp)
            )
            TextButton(onClick = onRetry) { Text(tr("Reintentar"), color = Gold) }
        }
    }
}

/** Confeti dorado de un par de segundos cuando el pago se confirma con la pantalla abierta. */
@Composable
private fun ActivationCelebration(onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(2_400, easing = LinearEasing))
        onDone()
    }
    val pieces = remember {
        val random = java.util.Random(7)
        List(70) {
            Triple(random.nextFloat(), random.nextFloat() * 0.5f, 0.5f + random.nextFloat())
        }
    }
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            pieces.forEachIndexed { i, (x, delay, speed) ->
                val t = ((progress.value - delay) / (1f - delay)).coerceIn(0f, 1f)
                if (t > 0f) {
                    drawCircle(
                        color = if (i % 3 == 0) GoldLight else if (i % 3 == 1) Gold else Color.White,
                        radius = 7f,
                        center = Offset(x * size.width, t * speed * size.height * 1.3f),
                        alpha = 1f - t
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = true,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(color = NexusTokens.Color.BgElevated, shape = RoundedCornerShape(NexusTokens.Radius.xl)) {
                Text(
                    tr("¡Premium activado!"),
                    color = GoldLight,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
        }
    }
}

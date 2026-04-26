package com.Azelmods.App.ui.screens.premium

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.ui.components.safeClickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    navController: NavController
) {
    var selectedPlan by remember { mutableStateOf(1) } // 0=Monthly, 1=Annual, 2=Lifetime
    val view = LocalView.current
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Premium", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = Color(0xFF0F0F1A)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                // Crown icon with shimmer
                PremiumCrownIcon()
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Unlock Premium",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                
                Text(
                    text = "Get the most out of Nexus Chat",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Plan cards
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlanCard(
                        title = "Monthly",
                        price = "$4.99",
                        period = "/month",
                        isSelected = selectedPlan == 0,
                        onClick = { selectedPlan = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    
                    PlanCard(
                        title = "Annual",
                        price = "$39.99",
                        period = "/year",
                        badge = "Save 33%",
                        badgeColor = Color(0xFFF59E0B),
                        isSelected = selectedPlan == 1,
                        onClick = { selectedPlan = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    
                    PlanCard(
                        title = "Lifetime",
                        price = "$99.99",
                        period = "once",
                        badge = "Best Value",
                        badgeColor = Color(0xFFEF4444),
                        isSelected = selectedPlan == 2,
                        onClick = { selectedPlan = 2 },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Benefits list
                Text(
                    text = "PREMIUM BENEFITS",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BenefitItem(
                        icon = Icons.Default.Wallpaper,
                        title = "Animated Wallpapers",
                        description = "Beautiful animated chat backgrounds"
                    )
                    
                    BenefitItem(
                        icon = Icons.Default.Star,
                        title = "VIP Badge",
                        description = "Show your premium status"
                    )
                    
                    BenefitItem(
                        icon = Icons.Default.Psychology,
                        title = "Advanced AI Tools",
                        description = "Unlimited AI features and priority access"
                    )
                    
                    BenefitItem(
                        icon = Icons.Default.CloudUpload,
                        title = "Unlimited Storage",
                        description = "Store all your media without limits"
                    )
                    
                    BenefitItem(
                        icon = Icons.Default.NotificationsOff,
                        title = "Ad-Free Experience",
                        description = "No ads, ever"
                    )
                    
                    BenefitItem(
                        icon = Icons.Default.Speed,
                        title = "Priority Support",
                        description = "Get help faster with premium support"
                    )
                    
                    BenefitItem(
                        icon = Icons.Default.Palette,
                        title = "Custom Themes",
                        description = "Create and use custom color themes"
                    )
                    
                    BenefitItem(
                        icon = Icons.Default.Group,
                        title = "Unlimited Groups",
                        description = "Create unlimited groups and channels"
                    )
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
            
            // Upgrade button (fixed at bottom)
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
                shadowElevation = 8.dp
            ) {
                val scale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "scale"
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .scale(scale)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                            ),
                            RoundedCornerShape(16.dp)
                        )
                        .safeClickable {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                            // Show success
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Upgrade Now · ${
                            when (selectedPlan) {
                                0 -> "$4.99"
                                1 -> "$39.99"
                                else -> "$99.99"
                            }
                        }",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
fun PremiumCrownIcon() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -500f,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )
    
    Box(contentAlignment = Alignment.Center) {
        // Glow effect
        Box(
            modifier = Modifier
                .size(80.dp)
                .blur(16.dp)
                .background(Color(0xFFF59E0B), RoundedCornerShape(20.dp))
        )
        
        // Icon with shimmer
        Surface(
            modifier = Modifier
                .size(80.dp)
                .shadow(16.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier.background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFF59E0B),
                            Color(0xFFEF4444),
                            Color(0xFFF59E0B)
                        ),
                        start = Offset(shimmerOffset, 0f),
                        end = Offset(shimmerOffset + 200f, 0f)
                    )
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
fun PlanCard(
    title: String,
    price: String,
    period: String,
    badge: String? = null,
    badgeColor: Color = Color(0xFFF59E0B),
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(140.dp)
            .safeClickable(onClick = onClick)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = Color(0xFFF59E0B),
                        shape = RoundedCornerShape(16.dp)
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            ),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFFF59E0B).copy(alpha = 0.1f) else Color(0xFF1A1A2E)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = price,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                
                Text(
                    text = period,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            // Badge
            if (badge != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp),
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun BenefitItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1A1A2E)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            listOf(Color(0xFFF59E0B), Color(0xFFEF4444))
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
            
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color(0xFFF59E0B),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

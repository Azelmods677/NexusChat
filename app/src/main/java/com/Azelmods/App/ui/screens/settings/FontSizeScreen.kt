package com.Azelmods.App.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSizeScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val fontSize by viewModel.fontSize.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Font Size", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = Color(0xFF0F0F1A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Font size selector
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf("Small", "Medium", "Large").forEachIndexed { index, size ->
                    SegmentedButton(
                        selected = fontSize == size,
                        onClick = { viewModel.setFontSize(size) },
                        shape = when (index) {
                            0 -> RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                            2 -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                            else -> RoundedCornerShape(0.dp)
                        },
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = Color.White,
                            inactiveContainerColor = Color(0xFF1A1A2E),
                            inactiveContentColor = Color.Gray
                        )
                    ) {
                        Text(size)
                    }
                }
            }
            
            // Preview card
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 2.dp,
                color = Color(0xFF1A1A2E)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Preview",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    val scale = when (fontSize) {
                        "Small" -> 0.85f
                        "Large" -> 1.15f
                        else -> 1.0f
                    }
                    
                    Text(
                        text = "The quick brown fox jumps over the lazy dog",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = MaterialTheme.typography.bodyLarge.fontSize * scale
                        ),
                        color = Color.White,
                        lineHeight = (MaterialTheme.typography.bodyLarge.lineHeight.value * scale).sp
                    )
                    
                    Text(
                        text = "This is how your messages will look with the selected font size.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize * scale
                        ),
                        color = Color.Gray,
                        lineHeight = (MaterialTheme.typography.bodyMedium.lineHeight.value * scale).sp
                    )
                }
            }
            
            // Info text
            Text(
                text = "Font size changes apply immediately throughout the app",
                color = Color.Gray,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

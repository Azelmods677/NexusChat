package com.Azelmods.App.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSizeScreen(navController: NavController) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("nexus_prefs", Context.MODE_PRIVATE)
    var selectedSize by remember {
        mutableStateOf(prefs.getString("font_size", "Medium") ?: "Medium")
    }
    
    val sizes = listOf(
        Triple("Pequeño", "Small", 13.sp),
        Triple("Normal", "Medium", 15.sp),
        Triple("Grande", "Large", 17.sp),
        Triple("Muy Grande", "XLarge", 19.sp)
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Tamaño de Fuente",
                        color = Color.White, 
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            tint = Color.White, 
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = Color(0xFF0D0D1A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Preview
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1A2E),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Vista Previa", 
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hola! ¿Cómo estás?",
                        color = Color.White,
                        fontSize = when(selectedSize) {
                            "Small" -> 13.sp
                            "Large" -> 17.sp
                            "XLarge" -> 19.sp
                            else -> 15.sp
                        }
                    )
                }
            }
            
            Spacer(Modifier.height(8.dp))
            
            sizes.forEach { (label, key, size) ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedSize = key
                            prefs.edit().putString("font_size", key).apply()
                        },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selectedSize == key)
                        MaterialTheme.colorScheme.primary.copy(0.2f)
                    else 
                        Color(0xFF1A1A2E),
                    border = BorderStroke(
                        if (selectedSize == key) 1.dp else 0.dp,
                        MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label, 
                            color = Color.White,
                            fontSize = size, 
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedSize == key) {
                            Icon(
                                Icons.Default.Check,
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

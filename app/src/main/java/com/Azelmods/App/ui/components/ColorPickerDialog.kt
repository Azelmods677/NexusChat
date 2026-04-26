package com.Azelmods.App.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.Azelmods.App.data.model.BackgroundPresets

/**
 * Color picker dialog with hex input and preset swatches
 * 
 * Features:
 * - 20 preset colors in red/dark theme
 * - Custom hex input
 * - Live preview circle
 * - Confirm/cancel buttons
 */
@Composable
fun ColorPickerDialog(
    initialColor: String = "#CC0000",
    onDismiss: () -> Unit,
    onColorSelected: (String) -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    var hexInput by remember { mutableStateOf(initialColor.removePrefix("#")) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F0F0F),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = "Seleccionar Color",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // Preview circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .align(Alignment.CenterHorizontally)
                        .clip(CircleShape)
                        .background(parseColorSafe(selectedColor))
                        .border(2.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                )
                
                // Hex input
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Código Hex",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1A1A1A)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            
                            BasicTextField(
                                value = hexInput,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isLetterOrDigit() }.take(6)
                                    hexInput = filtered.uppercase()
                                    if (filtered.length == 6) {
                                        selectedColor = "#$filtered"
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp),
                                textStyle = LocalTextStyle.current.copy(
                                    color = Color.White,
                                    fontSize = 16.sp
                                ),
                                singleLine = true
                            )
                        }
                    }
                }
                
                // Preset colors grid
                Text(
                    text = "Colores Predefinidos",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(6),
                    modifier = Modifier.height(200.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(BackgroundPresets.PRESET_COLORS) { colorHex ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(parseColorSafe(colorHex))
                                .border(
                                    width = if (selectedColor.equals(colorHex, ignoreCase = true)) 3.dp else 1.dp,
                                    color = if (selectedColor.equals(colorHex, ignoreCase = true)) 
                                        Color.White else Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColor = colorHex
                                    hexInput = colorHex.removePrefix("#")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor.equals(colorHex, ignoreCase = true)) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancelar")
                    }
                    
                    Button(
                        onClick = { onColorSelected(selectedColor) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFCC0000)
                        )
                    ) {
                        Text("Aplicar")
                    }
                }
            }
        }
    }
}

private fun parseColorSafe(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorInt = cleanHex.toLong(16)
        Color(colorInt or 0xFF000000)
    } catch (e: Exception) {
        Color(0xFFCC0000)
    }
}

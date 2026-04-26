package com.Azelmods.App.ui.screens.cybersec

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CyberSecScreen(
    onBack: () -> Unit,
    vm: CyberSecViewModel = hiltViewModel()
) {
    val result by vm.result.collectAsState()
    val loading by vm.loading.collectAsState()
    val context = LocalContext.current
    
    var selectedTool by remember { mutableStateOf("port_scan") }
    var input1 by remember { mutableStateOf("") }
    var input2 by remember { mutableStateOf("") }
    var input3 by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1117))
            .systemBarsPadding()
    ) {
        // TopBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF39FF14))
            }
            Text(
                "CyberSec Toolkit",
                color = Color(0xFF39FF14),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { vm.clearResult() }) {
                Icon(Icons.Default.DeleteOutline, null, tint = Color(0xFF8B949E))
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Tool selector
            Text(
                "Herramienta:",
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTool == "port_scan",
                    onClick = { selectedTool = "port_scan" },
                    label = { Text("Port Scan") }
                )
                FilterChip(
                    selected = selectedTool == "dns",
                    onClick = { selectedTool = "dns" },
                    label = { Text("DNS") }
                )
                FilterChip(
                    selected = selectedTool == "headers",
                    onClick = { selectedTool = "headers" },
                    label = { Text("Headers") }
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedTool == "hash",
                    onClick = { selectedTool = "hash" },
                    label = { Text("Hash") }
                )
                FilterChip(
                    selected = selectedTool == "base64",
                    onClick = { selectedTool = "base64" },
                    label = { Text("Base64") }
                )
                FilterChip(
                    selected = selectedTool == "network",
                    onClick = { selectedTool = "network" },
                    label = { Text("Network") }
                )
            }
            
            HorizontalDivider(color = Color(0xFF161B22))
            
            // Inputs based on tool
            when (selectedTool) {
                "port_scan" -> {
                    OutlinedTextField(
                        value = input1,
                        onValueChange = { input1 = it },
                        label = { Text("Host") },
                        placeholder = { Text("192.168.1.1") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = input2,
                            onValueChange = { input2 = it },
                            label = { Text("From Port") },
                            placeholder = { Text("1") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = input3,
                            onValueChange = { input3 = it },
                            label = { Text("To Port") },
                            placeholder = { Text("1000") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Button(
                        onClick = {
                            vm.scanPorts(
                                input1,
                                input2.toIntOrNull() ?: 1,
                                input3.toIntOrNull() ?: 1000
                            )
                        },
                        enabled = !loading && input1.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Escanear Puertos")
                    }
                }
                "dns" -> {
                    OutlinedTextField(
                        value = input1,
                        onValueChange = { input1 = it },
                        label = { Text("Host") },
                        placeholder = { Text("google.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { vm.dnsLookup(input1) },
                        enabled = !loading && input1.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("DNS Lookup")
                    }
                }
                "headers" -> {
                    OutlinedTextField(
                        value = input1,
                        onValueChange = { input1 = it },
                        label = { Text("URL") },
                        placeholder = { Text("https://example.com") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { vm.analyzeHeaders(input1) },
                        enabled = !loading && input1.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Analizar Headers")
                    }
                }
                "hash" -> {
                    OutlinedTextField(
                        value = input1,
                        onValueChange = { input1 = it },
                        label = { Text("Texto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { vm.generateHash(input1, "MD5") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("MD5")
                        }
                        Button(
                            onClick = { vm.generateHash(input1, "SHA-256") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SHA-256")
                        }
                    }
                }
                "base64" -> {
                    OutlinedTextField(
                        value = input1,
                        onValueChange = { input1 = it },
                        label = { Text("Texto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { vm.base64Encode(input1) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Encode")
                        }
                        Button(
                            onClick = { vm.base64Decode(input1) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Decode")
                        }
                    }
                }
                "network" -> {
                    Button(
                        onClick = { vm.getNetworkInfo(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Obtener Info de Red")
                    }
                }
            }
            
            HorizontalDivider(color = Color(0xFF161B22))
            
            // Output
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
            
            if (result.isNotBlank()) {
                Text(
                    "Resultado:",
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF161B22),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        result,
                        color = Color(0xFFE6EDF3),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

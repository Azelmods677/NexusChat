package com.Azelmods.App.ui.screens.terminal

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

private val TermBg = Color(0xFF0D1117)
private val TermGreen = Color(0xFF39FF14)
private val TermRed = Color(0xFFFF4444)
private val TermYellow = Color(0xFFFFD700)
private val TermCyan = Color(0xFF00FFFF)
private val TermWhite = Color(0xFFE6EDF3)
private val TermGray = Color(0xFF8B949E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onBack: () -> Unit,
    vm: TerminalViewModel = hiltViewModel()
) {
    val history by vm.history.collectAsState()
    val isRunning by vm.isRunning.collectAsState()
    var input by remember { mutableStateOf("") }
    var useRoot by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Quick commands
    val quickCmds = listOf(
        "uname -a", "id", "whoami", "ifconfig",
        "netstat -an", "ps aux", "ls -la", "cat /proc/version"
    )
    
    LaunchedEffect(history.size) {
        if (history.isNotEmpty())
            listState.animateScrollToItem(history.lastIndex)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TermBg)
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
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TermGreen)
            }
            Text(
                "AzelShell",
                color = TermGreen,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            
            // Toggle ROOT
            FilterChip(
                selected = useRoot,
                onClick = { useRoot = !useRoot },
                label = {
                    Text(
                        if (useRoot) "ROOT" else "SHELL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = TermRed.copy(alpha = 0.2f),
                    selectedLabelColor = TermRed,
                    containerColor = Color.Transparent,
                    labelColor = TermGray
                )
            )
            
            IconButton(onClick = { vm.clear() }) {
                Icon(Icons.Default.DeleteOutline, null, tint = TermGray)
            }
        }
        
        // Info bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A0E13))
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (vm.hasRoot) "● ROOT disponible" else "● Sin root",
                color = if (vm.hasRoot) TermGreen else TermYellow,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
            Spacer(Modifier.width(16.dp))
            Text(
                "Android Shell v${android.os.Build.VERSION.RELEASE}",
                color = TermGray,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        }
        
        // Quick commands
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickCmds.forEach { cmd ->
                SuggestionChip(
                    onClick = { vm.execute(cmd, useRoot) },
                    label = {
                        Text(
                            cmd,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color(0xFF161B22),
                        labelColor = TermCyan
                    ),
                    border = SuggestionChipDefaults.suggestionChipBorder(
                        enabled = true,
                        borderColor = TermCyan.copy(alpha = 0.3f)
                    )
                )
            }
        }
        
        // Output
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item {
                Text(
                    "AzelShell 1.0 — ${
                        java.text.SimpleDateFormat(
                            "yyyy-MM-dd HH:mm",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date())
                    }",
                    color = TermGreen,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
            
            items(history, key = { it.id }) { entry ->
                Column {
                    // Input
                    Text(
                        text = "${if (useRoot) "root" else "azel"}@framework:~$ ${entry.input}",
                        color = TermYellow,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                    // Output
                    Text(
                        text = entry.output,
                        color = if (entry.isError) TermRed else TermWhite,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            
            if (isRunning) {
                item {
                    Text(
                        "Ejecutando...",
                        color = TermGreen,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
        
        // Input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${if (useRoot) "root" else "azel"}@framework:~$ ",
                color = TermGreen,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = TermWhite,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        vm.execute(input.trim(), useRoot)
                        input = ""
                    }
                ),
                singleLine = true,
                cursorBrush = SolidColor(TermGreen)
            )
            IconButton(
                onClick = {
                    vm.execute(input.trim(), useRoot)
                    input = ""
                },
                enabled = input.isNotBlank() && !isRunning
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send, null,
                    tint = if (input.isNotBlank()) TermGreen else TermGray
                )
            }
        }
    }
}

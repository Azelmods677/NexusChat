package com.Azelmods.App.ui.screens.terminal

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.DeleteSweep
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onBack: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val lines by viewModel.lines.collectAsState()
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty())
            listState.animateScrollToItem(lines.size - 1)
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val blink by rememberInfiniteTransition(label = "b")
                            .animateFloat(
                                0f, 1f,
                                infiniteRepeatable(tween(600), RepeatMode.Reverse),
                                label = "bv"
                            )
                        Box(
                            Modifier
                                .size(8.dp)
                                .background(Color(0xFF00FF41).copy(blink), CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Terminal",
                            color = Color(0xFF00FF41),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            tint = Color(0xFF00FF41),
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clear() }) {
                        Icon(
                            Icons.Default.DeleteSweep,
                            tint = Color(0xFF00FF41),
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A)
                )
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Output
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                items(lines) { line ->
                    Text(
                        text = line.text,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = when (line.type) {
                            RealTerminalEmulator.TerminalLine.Type.SYSTEM -> Color(0xFF7B5CFA)
                            RealTerminalEmulator.TerminalLine.Type.INPUT -> Color(0xFF00FF41)
                            RealTerminalEmulator.TerminalLine.Type.OUTPUT -> Color(0xFFCCCCCC)
                            RealTerminalEmulator.TerminalLine.Type.SUCCESS -> Color(0xFF00FF41)
                            RealTerminalEmulator.TerminalLine.Type.ERROR -> Color(0xFFFF4444)
                            RealTerminalEmulator.TerminalLine.Type.WARNING -> Color(0xFFFFAA00)
                        }
                    )
                }
            }

            HorizontalDivider(color = Color(0xFF00FF41).copy(0.2f))

            // Quick commands
            LazyRow(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val quickCmds = listOf(
                    "help",
                    "clear",
                    "ls /sdcard",
                    "df -h",
                    "free",
                    "ps",
                    "date",
                    "uname -a"
                )
                items(quickCmds) { cmd ->
                    Surface(
                        onClick = { viewModel.execute(cmd) },
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF00FF41).copy(0.1f),
                        border = BorderStroke(1.dp, Color(0xFF00FF41).copy(0.3f))
                    ) {
                        Text(
                            cmd,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = Color(0xFF00FF41),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Input row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111))
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$ ",
                    color = Color(0xFF00FF41),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 15.sp
                )
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        color = Color(0xFF00FF41),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(Color(0xFF00FF41)),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (input.isNotBlank()) {
                                viewModel.execute(input)
                                input = ""
                            }
                        }
                    ),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            viewModel.execute(input)
                            input = ""
                        }
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        tint = Color(0xFF00FF41),
                        modifier = Modifier.size(18.dp),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

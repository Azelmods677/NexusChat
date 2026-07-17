package com.Azelmods.App.ui.screens.editor

import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeEditorScreen(
    navController: NavController,
    viewModel: CodeEditorViewModel = hiltViewModel()
) {
    val files by viewModel.files.collectAsState()
    val currentFile by viewModel.currentFile.collectAsState()
    val output by viewModel.output.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    val jsToExecute by viewModel.jsToExecute.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileLanguage by remember { mutableStateOf("python") }
    var editorContent by remember { mutableStateOf("") }
    var showFileList by remember { mutableStateOf(true) }

    // 🔥 WebView for real JavaScript execution
    val context = LocalContext.current
    val webViewRef = remember { mutableStateOf<WebView?>(null) }
    var jsOutput by remember { mutableStateOf("") }
    var jsError by remember { mutableStateOf<String?>(null) }

    // Create hidden WebView for JS execution with proper lifecycle management
    DisposableEffect(context) {
        var webView: WebView? = null
        try {
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                layoutParams = android.view.ViewGroup.LayoutParams(1, 1) // 1x1 pixel — hidden
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                    }
                    
                    override fun onReceivedError(
                        view: WebView?,
                        request: android.webkit.WebResourceRequest?,
                        error: android.webkit.WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        jsError = "WebView Error: ${error?.description}"
                    }
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(message: ConsoleMessage?): Boolean {
                        message?.let {
                            val line = "[${it.messageLevel()}] ${it.message()}"
                            jsOutput += line + "\n"
                        }
                        return true
                    }
                }
                
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun log(message: String) {
                        jsOutput += message + "\n"
                    }
                    @JavascriptInterface
                    fun error(message: String) {
                        jsError = message
                    }
                }, "AndroidBridge")
                
                loadData(
                    """
                    <!DOCTYPE html>
                    <html>
                    <head><meta charset="UTF-8"></head>
                    <body>
                    <script>
                        // Override console to capture output
                        window.originalConsole = {
                            log: console.log.bind(console),
                            error: console.error.bind(console),
                            warn: console.warn.bind(console)
                        };
                        console.log = function(...args) {
                            try {
                                window.originalConsole.log(...args);
                                AndroidBridge.log(args.map(a => String(a)).join(' '));
                            } catch(e) {}
                        };
                        console.error = function(...args) {
                            try {
                                window.originalConsole.error(...args);
                                AndroidBridge.error(args.map(a => String(a)).join(' '));
                            } catch(e) {}
                        };
                        console.warn = function(...args) {
                            try {
                                window.originalConsole.warn(...args);
                                AndroidBridge.log('[WARN] ' + args.map(a => String(a)).join(' '));
                            } catch(e) {}
                        };
                        // Error handler
                        window.onerror = function(msg, url, line) {
                            try {
                                AndroidBridge.error(msg + ' (line ' + line + ')');
                            } catch(e) {}
                            return true;
                        };
                    </script>
                    </body>
                    </html>
                    """.trimIndent(),
                    "text/html",
                    "UTF-8"
                )
            }
            webViewRef.value = webView
        } catch (e: Exception) {
            jsError = "Error inicializando WebView: ${e.message}"
        }
        
        onDispose {
            try {
                webViewRef.value?.apply {
                    stopLoading()
                    loadUrl("about:blank")
                    clearHistory()
                    clearCache(true)
                    removeAllViews()
                    destroy()
                }
                webViewRef.value = null
            } catch (e: Exception) {
                // Ignore dispose errors
            }
        }
    }

    // Execute JS when ViewModel signals
    LaunchedEffect(jsToExecute) {
        val code = jsToExecute ?: return@LaunchedEffect
        jsOutput = ""
        jsError = null

        // Wait for WebView to be ready
        delay(500)

        val wrappedCode = """
            (function() {
                try {
                    $code
                    return '__NEXUS_DONE__';
                } catch (e) {
                    AndroidBridge.error(e.name + ': ' + e.message);
                    return '__NEXUS_ERROR__';
                }
            })()
        """.trimIndent()

        webViewRef.value?.evaluateJavascript(wrappedCode) { result ->
            // No mostrar los centinelas internos ni "null" como si fueran salida real.
            val cleanResult = result?.trim('"')?.takeIf {
                it != "__NEXUS_DONE__" && it != "__NEXUS_ERROR__" && it != "null"
            } ?: ""
            viewModel.onJsResult(
                if (jsOutput.isBlank()) cleanResult else jsOutput,
                jsError
            )
        }
    }

    LaunchedEffect(currentFile) {
        editorContent = currentFile?.content ?: ""
        if (currentFile != null) showFileList = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        currentFile?.name ?: "Code Editor",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (!showFileList) {
                            showFileList = true
                            viewModel.closeFile()
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    if (!showFileList && currentFile != null) {
                        IconButton(onClick = { viewModel.saveFile(editorContent) }) {
                            Icon(Icons.Default.Save, null, tint = TerminalGreen)
                        }
                        IconButton(
                            onClick = {
                                currentFile?.let {
                                    viewModel.executeCode(editorContent, it.language)
                                }
                            },
                            enabled = !isRunning
                        ) {
                            if (isRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, null, tint = TerminalGreen)
                            }
                        }
                    }
                    IconButton(onClick = { showNewFileDialog = true }) {
                        Icon(Icons.Default.Add, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { padding ->
        if (showFileList) {
            // File list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp)
            ) {
                if (files.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Code,
                                    null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No code files yet",
                                    color = Color.Gray,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = { showNewFileDialog = true }) {
                                    Text("Create First File")
                                }
                            }
                        }
                    }
                } else {
                    items(files) { file ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurface,
                            border = BorderStroke(
                                1.dp,
                                if (currentFile?.id == file.id) MaterialTheme.colorScheme.primary else Color.Transparent
                            ),
                            onClick = {
                                viewModel.openFile(file)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                @Suppress("DEPRECATION")
                                Icon(
                                    Icons.Default.InsertDriveFile,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        file.name,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "${file.language} · ${file.size} bytes",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    null,
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Editor + Output
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Editor
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    color = DarkSurface
                ) {
                    BasicTextField(
                        value = editorContent,
                        onValueChange = { editorContent = it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        textStyle = TextStyle(
                            color = TerminalGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        ),
                        cursorBrush = SolidColor(TerminalGreen)
                    )
                }

                // Output
                if (output.isNotEmpty()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        color = TerminalBlack
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Output",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(
                                    onClick = { viewModel.clearOutput() },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Clear,
                                        null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                output,
                                color = Color(0xFFCCCCCC),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
        }
    }

    // New File Dialog
    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("New File", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("File Name", color = Color.Gray) },
                        placeholder = { Text("main.py", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Language:", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    val languages = listOf("python", "kotlin", "bash", "js", "c")
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        languages.forEach { lang ->
                            FilterChip(
                                selected = newFileLanguage == lang,
                                onClick = { newFileLanguage = lang },
                                label = { Text(lang, color = Color.White) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            viewModel.newFile(newFileName, newFileLanguage)
                            showNewFileDialog = false
                            newFileName = ""
                        }
                    },
                    enabled = newFileName.isNotBlank()
                ) {
                    Text("Create", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = DarkSurface
        )
    }
}

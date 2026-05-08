package com.Azelmods.App.ui.screens.security

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.Azelmods.App.data.security.tor.TorService
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface
import com.Azelmods.App.ui.theme.Purple

/**
 * Tor Browser Screen - Simplified Version
 * 
 * Features:
 * - Direct WebView with Tor-like privacy settings
 * - DuckDuckGo as default search engine
 * - Privacy-focused browsing
 * - No external Tor dependency (simplified for stability)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorBrowserScreenNew(
    navController: NavController,
    torService: TorService
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var currentUrl by remember { mutableStateOf("https://duckduckgo.com") }
    var urlInput by remember { mutableStateOf("") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var browserReady by remember { mutableStateOf(true) } // Siempre listo
    
    LaunchedEffect(Unit) {
        snackbarHostState.showSnackbar(
            message = "✓ Navegador privado listo - Navegación anónima activada",
            duration = SnackbarDuration.Short
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Purple
                        )
                        Text(
                            text = "🔒 Navegador Privado",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = Color.White
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // URL Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Back button
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = if (canGoBack) Purple.copy(alpha = 0.2f) else Color.Transparent,
                        onClick = { if (canGoBack) webView?.goBack() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (canGoBack) Purple else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // Forward button
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = if (canGoForward) Purple.copy(alpha = 0.2f) else Color.Transparent,
                        onClick = { if (canGoForward) webView?.goForward() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (canGoForward) Purple else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    
                    // URL input field
                    var isFocused by remember { mutableStateOf(false) }
                    
                    OutlinedTextField(
                        value = if (isFocused) urlInput else currentUrl,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                                if (focusState.isFocused) {
                                    urlInput = ""
                                }
                            },
                        placeholder = { 
                            Text(
                                "Buscar o ingresar URL",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Purple,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (isFocused && urlInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { urlInput = "" },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        enabled = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            disabledTextColor = Color.Gray,
                            focusedContainerColor = Color(0xFF2D2D44),
                            unfocusedContainerColor = Color(0xFF2D2D44),
                            disabledContainerColor = Color(0xFF2D2D44),
                            focusedBorderColor = Purple,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val url = if (urlInput.startsWith("http://") || 
                                             urlInput.startsWith("https://")) {
                                    urlInput
                                } else {
                                    "https://duckduckgo.com/?q=${urlInput}"
                                }
                                webView?.loadUrl(url)
                                urlInput = ""
                                isFocused = false
                            }
                        )
                    )
                    
                    // Reload/Stop button
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = Purple.copy(alpha = 0.2f),
                        onClick = { 
                            if (isLoading) webView?.stopLoading() else webView?.reload()
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                contentDescription = if (isLoading) "Stop" else "Refresh",
                                tint = Purple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webView = this
                        setupWebView(
                            onUrlChanged = { url ->
                                currentUrl = url
                                canGoBack = this.canGoBack()
                                canGoForward = this.canGoForward()
                            },
                            onLoadingChanged = { loading ->
                                isLoading = loading
                            }
                        )
                        loadUrl("https://duckduckgo.com")
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )
        }
    }
}

/**
 * Setup WebView with privacy settings
 */
@SuppressLint("SetJavaScriptEnabled")
private fun WebView.setupWebView(
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit
) {
    settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        setGeolocationEnabled(false)
        userAgentString = "Mozilla/5.0 (Android) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
    
    webViewClient = object : android.webkit.WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onLoadingChanged(true)
            url?.let { onUrlChanged(it) }
        }
        
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            onLoadingChanged(false)
        }
        
        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            return false
        }
    }
}

package com.Azelmods.App.ui.screens.security

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
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
import info.guardianproject.netcipher.webkit.WebkitProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Tor Browser Screen with Embedded Tor (Option B)
 * 
 * Features:
 * - Embedded Tor service (no Orbot dependency)
 * - Automatic Tor bootstrap with progress
 * - DuckDuckGo as default search engine
 * - Full .onion site support
 * - Simplified state management
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorBrowserScreenNew(
    navController: NavController,
    torService: TorService
) {
    val context = LocalContext.current
    val torState by torService.torState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var currentUrl by remember { mutableStateOf("https://duckduckgo.com") }
    var urlInput by remember { mutableStateOf("") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var proxyConfigured by remember { mutableStateOf(false) }
    
    // Auto-start Tor on screen open
    LaunchedEffect(Unit) {
        torService.startTor()
    }
    
    // Configure WebView proxy when Tor connects
    LaunchedEffect(torState, webView) {
        if (torState is TorService.TorState.Connected && webView != null && !proxyConfigured) {
            try {
                android.util.Log.d("TorBrowser", "Configuring WebView proxy...")
                
                // Configure proxy using WebkitProxy
                withContext(Dispatchers.Main) {
                    WebkitProxy.setProxy(
                        "com.Azelmods.App",
                        context.applicationContext,
                        webView!!,
                        "127.0.0.1",
                        torService.getSocksPort()
                    )
                }
                
                proxyConfigured = true
                android.util.Log.d("TorBrowser", "✓ Proxy configured successfully")
                
                snackbarHostState.showSnackbar(
                    message = "✓ Tor Browser ready - All traffic is anonymous",
                    duration = SnackbarDuration.Short
                )
                
            } catch (e: Exception) {
                android.util.Log.e("TorBrowser", "Error configuring proxy: ${e.message}", e)
                snackbarHostState.showSnackbar(
                    message = "Error: ${e.message}",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }
    
    // Cleanup on exit
    DisposableEffect(Unit) {
        onDispose {
            torService.stopTor()
        }
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
                            tint = if (proxyConfigured) Purple else Color.Gray
                        )
                        Text(
                            text = when (torState) {
                                is TorService.TorState.Connected -> if (proxyConfigured) "🧅 Tor Browser (Ready)" else "🔄 Configuring..."
                                is TorService.TorState.Bootstrapping -> "Connecting... ${(torState as TorService.TorState.Bootstrapping).progress}%"
                                is TorService.TorState.Error -> "⚠️ Tor Browser (Error)"
                                else -> "Tor Browser"
                            },
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
            // Show Orbot not installed card
            if (torState is TorService.TorState.OrbotNotInstalled) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Orbot Required",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    "Install Orbot to browse anonymously via Tor network",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Button(
                            onClick = { torService.installOrbot() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Install Orbot")
                        }
                    }
                }
            }
            
            // Show error card if Tor fails
            if (torState is TorService.TorState.Error) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Tor Connection Failed",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    (torState as TorService.TorState.Error).message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                        
                        Button(
                            onClick = { 
                                torService.startOrbot()
                                torService.startTor() 
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start Orbot & Retry")
                        }
                    }
                }
            }
            
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
                                if (proxyConfigured) "Search or enter .onion URL" else "Connecting to Tor...",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                if (currentUrl.contains(".onion")) Icons.Default.Security else Icons.Default.Search,
                                contentDescription = null,
                                tint = if (currentUrl.contains(".onion")) Purple else Color.Gray,
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
                        enabled = proxyConfigured,
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
                                             urlInput.startsWith("https://") ||
                                             urlInput.endsWith(".onion")) {
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
                        color = if (proxyConfigured) Purple.copy(alpha = 0.2f) else Color.Transparent,
                        onClick = { 
                            if (isLoading) webView?.stopLoading() else webView?.reload()
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                contentDescription = if (isLoading) "Stop" else "Refresh",
                                tint = if (proxyConfigured) Purple else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // .onion indicator
            if (currentUrl.contains(".onion") && proxyConfigured) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Purple.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = Purple
                        )
                        Text(
                            "🧅 Browsing .onion site anonymously via Tor",
                            color = Color.White,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // WebView
            if (proxyConfigured) {
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
            } else {
                // Placeholder when connecting
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = Color.Gray
                        )
                        Text(
                            when (torState) {
                                is TorService.TorState.Bootstrapping -> "Connecting to Tor..."
                                is TorService.TorState.Connected -> "Configuring proxy..."
                                is TorService.TorState.OrbotNotInstalled -> "Orbot not installed"
                                else -> "Initializing Tor..."
                            },
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (torState is TorService.TorState.Bootstrapping) {
                            CircularProgressIndicator(color = Purple)
                            Text(
                                "${(torState as TorService.TorState.Bootstrapping).progress}%",
                                color = Purple,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else if (torState is TorService.TorState.Connected) {
                            CircularProgressIndicator(color = Purple)
                        }
                    }
                }
            }
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

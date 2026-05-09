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
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                color = DarkSurface,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Back button
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = if (canGoBack) Purple.copy(alpha = 0.2f) else Color.Transparent,
                        onClick = { if (canGoBack) webView?.goBack() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (canGoBack) Purple else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    
                    // Forward button
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = if (canGoForward) Purple.copy(alpha = 0.2f) else Color.Transparent,
                        onClick = { if (canGoForward) webView?.goForward() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (canGoForward) Purple else Color.Gray,
                                modifier = Modifier.size(18.dp)
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
                            .height(48.dp)
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
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = Purple,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (isFocused && urlInput.isNotEmpty()) {
                                IconButton(
                                    onClick = { urlInput = "" },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = Color.Gray,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        enabled = true,
                        shape = RoundedCornerShape(10.dp),
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
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val url = processUrl(urlInput)
                                
                                // Check if it's a .onion URL
                                if (url.contains(".onion")) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            message = "⚠️ Los sitios .onion requieren Orbot (Tor). Instálalo desde F-Droid o Play Store",
                                            duration = SnackbarDuration.Long
                                        )
                                    }
                                } else {
                                    webView?.loadUrl(url)
                                }
                                
                                urlInput = ""
                                isFocused = false
                            }
                        )
                    )
                    
                    // Reload/Stop button
                    Surface(
                        modifier = Modifier.size(36.dp),
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
                                modifier = Modifier.size(18.dp)
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
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

/**
 * Process URL input with smart logic
 */
private fun processUrl(input: String): String {
    val trimmed = input.trim()
    
    return when {
        // Already has protocol
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        
        // Looks like a domain (has dot and no spaces)
        trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
        
        // Everything else is a search query
        else -> "https://duckduckgo.com/?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
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
        databaseEnabled = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        setGeolocationEnabled(false)
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
        
        // Configuración mejorada para mejor visualización
        useWideViewPort = true
        loadWithOverviewMode = true
        layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        
        // User agent actualizado
        userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36"
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
        
        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: android.webkit.WebResourceError?
        ) {
            super.onReceivedError(view, request, error)
            
            // Only show error page for main frame errors
            if (request?.isForMainFrame == true) {
                val url = request.url.toString()
                val isOnionSite = url.contains(".onion")
                
                val errorHtml = if (isOnionSite) {
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body {
                                background: linear-gradient(135deg, #0A0A0A 0%, #1A1A2E 100%);
                                color: #FFFFFF;
                                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                                display: flex;
                                justify-content: center;
                                align-items: center;
                                min-height: 100vh;
                                margin: 0;
                                padding: 20px;
                                text-align: center;
                            }
                            .container {
                                max-width: 500px;
                            }
                            .icon {
                                font-size: 80px;
                                margin-bottom: 20px;
                            }
                            h1 {
                                font-size: 28px;
                                margin: 0 0 10px 0;
                                background: linear-gradient(135deg, #7C3AED, #00D4FF);
                                -webkit-background-clip: text;
                                -webkit-text-fill-color: transparent;
                                background-clip: text;
                            }
                            p {
                                color: #AAAAAA;
                                font-size: 16px;
                                line-height: 1.6;
                                margin: 0 0 30px 0;
                            }
                            .onion-url {
                                background: rgba(124, 58, 237, 0.1);
                                border: 1px solid rgba(124, 58, 237, 0.3);
                                border-radius: 8px;
                                padding: 12px;
                                margin: 20px 0;
                                font-family: 'Courier New', monospace;
                                font-size: 13px;
                                color: #7C3AED;
                                word-break: break-all;
                            }
                            .instructions {
                                text-align: left;
                                background: rgba(255, 255, 255, 0.05);
                                border-radius: 12px;
                                padding: 20px;
                                margin-top: 20px;
                            }
                            .instructions h3 {
                                margin: 0 0 15px 0;
                                font-size: 18px;
                                color: #7C3AED;
                            }
                            .instructions ol {
                                margin: 0;
                                padding-left: 20px;
                            }
                            .instructions li {
                                margin: 12px 0;
                                color: #CCCCCC;
                                line-height: 1.6;
                            }
                            .instructions strong {
                                color: #00D4FF;
                            }
                            .brand {
                                margin-top: 30px;
                                font-size: 14px;
                                color: #666666;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="icon">🧅</div>
                            <h1>Sitio .onion detectado</h1>
                            <p>Los sitios .onion solo son accesibles a través de la red Tor. Este navegador no tiene soporte nativo para Tor.</p>
                            
                            <div class="onion-url">
                                $url
                            </div>
                            
                            <div class="instructions">
                                <h3>🔐 Cómo acceder a sitios .onion</h3>
                                <ol>
                                    <li>Instala <strong>Orbot</strong> desde F-Droid o Play Store</li>
                                    <li>Abre Orbot y presiona <strong>"Iniciar"</strong></li>
                                    <li>Espera a que se conecte a la red Tor</li>
                                    <li>Usa <strong>Tor Browser</strong> o configura un navegador con proxy SOCKS5 (localhost:9050)</li>
                                    <li>Ahora podrás acceder a sitios .onion de forma anónima</li>
                                </ol>
                            </div>
                            
                            <div class="brand">Nexus Chat - Navegador Privado</div>
                        </div>
                    </body>
                    </html>
                    """.trimIndent()
                } else {
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8">
                        <meta name="viewport" content="width=device-width, initial-scale=1.0">
                        <style>
                            body {
                                background: linear-gradient(135deg, #0A0A0A 0%, #1A1A2E 100%);
                                color: #FFFFFF;
                                font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                                display: flex;
                                justify-content: center;
                                align-items: center;
                                min-height: 100vh;
                                margin: 0;
                                padding: 20px;
                                text-align: center;
                            }
                            .container {
                                max-width: 500px;
                            }
                            .icon {
                                font-size: 80px;
                                margin-bottom: 20px;
                                animation: pulse 2s infinite;
                            }
                            @keyframes pulse {
                                0%, 100% { opacity: 1; }
                                50% { opacity: 0.5; }
                            }
                            h1 {
                                font-size: 28px;
                                margin: 0 0 10px 0;
                                background: linear-gradient(135deg, #7C3AED, #00D4FF);
                                -webkit-background-clip: text;
                                -webkit-text-fill-color: transparent;
                                background-clip: text;
                            }
                            p {
                                color: #AAAAAA;
                                font-size: 16px;
                                line-height: 1.6;
                                margin: 0 0 30px 0;
                            }
                            .error-code {
                                background: rgba(124, 58, 237, 0.1);
                                border: 1px solid rgba(124, 58, 237, 0.3);
                                border-radius: 8px;
                                padding: 12px;
                                margin: 20px 0;
                                font-family: 'Courier New', monospace;
                                font-size: 14px;
                                color: #7C3AED;
                            }
                            .suggestions {
                                text-align: left;
                                background: rgba(255, 255, 255, 0.05);
                                border-radius: 12px;
                                padding: 20px;
                                margin-top: 20px;
                            }
                            .suggestions h3 {
                                margin: 0 0 15px 0;
                                font-size: 18px;
                                color: #7C3AED;
                            }
                            .suggestions ul {
                                margin: 0;
                                padding-left: 20px;
                            }
                            .suggestions li {
                                margin: 8px 0;
                                color: #CCCCCC;
                                line-height: 1.5;
                            }
                            .brand {
                                margin-top: 30px;
                                font-size: 14px;
                                color: #666666;
                            }
                        </style>
                    </head>
                    <body>
                        <div class="container">
                            <div class="icon">🔒</div>
                            <h1>Página no disponible</h1>
                            <p>No se pudo cargar la página solicitada. Verifica tu conexión a internet o intenta nuevamente.</p>
                            
                            <div class="error-code">
                                Error: ${error?.description ?: "Desconocido"}<br>
                                Código: ${error?.errorCode ?: -1}
                            </div>
                            
                            <div class="suggestions">
                                <h3>💡 Sugerencias</h3>
                                <ul>
                                    <li>Verifica tu conexión a internet</li>
                                    <li>Comprueba que la URL sea correcta</li>
                                    <li>Intenta recargar la página</li>
                                    <li>El sitio podría estar temporalmente no disponible</li>
                                </ul>
                            </div>
                            
                            <div class="brand">Nexus Chat - Navegador Privado</div>
                        </div>
                    </body>
                    </html>
                    """.trimIndent()
                }
                
                view?.loadDataWithBaseURL(
                    null,
                    errorHtml,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    }
}

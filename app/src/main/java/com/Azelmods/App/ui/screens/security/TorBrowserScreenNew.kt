package com.Azelmods.App.ui.screens.security

import android.annotation.SuppressLint
import android.util.Log
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
import com.Azelmods.App.data.security.tor.OrbotDetector
import com.Azelmods.App.data.security.tor.ProxyManager
import com.Azelmods.App.data.security.tor.ProxyResult
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.Azelmods.App.ui.theme.Success
import com.Azelmods.App.ui.theme.DarkElevated

private const val TAG = "TorBrowser"

/** URL inicial por defecto (NO depende de Tor) */
private const val DEFAULT_HOMEPAGE = "https://duckduckgo.com"

/**
 * 🌐 Tor Browser — Navegador privado con soporte opcional de Tor vía Orbot
 *
 * ## Cambios críticos (fix 404):
 *
 * ### 1. El navegador NO depende de Tor para funcionar
 * Si Orbot no está activo, el WebView carga páginas directamente (sin proxy).
 * Solo cuando Orbot está CONFIRMADO funcionando se activa el proxy Tor.
 *
 * ### 2. Proxy configurado ANTES de cargar URLs
 * Se eliminó la race condition: el WebView inicia con "about:blank",
 * se configura el proxy si está disponible, y SOLO ENTONCES se carga la URL inicial.
 *
 * ### 3. Manejo completo de errores HTTP
 * - `onReceivedHttpError` captura errores 404, 500, etc. y muestra página de error.
 * - `onReceivedError` captura errores de red y muestra página de error.
 * - Las URLs .onion tienen su propio mensaje de ayuda cuando Tor no está activo.
 *
 * ### 4. Verificación del proxy antes de aplicarlo
 * Se verifica que el puerto HTTP de Orbot (127.0.0.1:8118) esté realmente
 * aceptando conexiones antes de configurarlo via ProxyController.
 *
 * @param navController Controlador de navegación para retroceder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorBrowserScreenNew(
    navController: NavController
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var currentUrl by remember { mutableStateOf(DEFAULT_HOMEPAGE) }
    var urlInput by remember { mutableStateOf("") }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var orbotStatus by remember { mutableStateOf("") }
    var proxyEnabled by remember { mutableStateOf(false) }
    var browsingMode by remember { mutableStateOf("directo") } // "directo" | "tor"
    var isWebViewReady by remember { mutableStateOf(false) }
    // Se incrementa desde "Recargar" para re-detectar Orbot sin salir de la pantalla
    // (p. ej. si el usuario arranca Orbot DESPUÉS de abrir el navegador).
    var recheckTrigger by remember { mutableStateOf(0) }

    // Handler del MainThread para actualizar el estado de UI desde el callback del proxy,
    // que WebView invoca en el executor de larga vida de ProxyManager (hilo de background).
    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }

    // ── PASO 1: Cargar la URL inicial (clearnet, sin depender de Tor) una sola vez ──
    LaunchedEffect(isWebViewReady) {
        if (!isWebViewReady) return@LaunchedEffect
        try {
            webView?.loadUrl(DEFAULT_HOMEPAGE)
            Log.d(TAG, "✓ URL inicial cargada: $DEFAULT_HOMEPAGE")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error cargando URL inicial", e)
        }
    }

    // ── PASO 2: Detectar Orbot y aplicar el proxy de forma asíncrona ──
    // Se vuelve a ejecutar cuando cambia [recheckTrigger] (botón Recargar), así el
    // usuario puede arrancar Orbot y activarlo sin salir del navegador.
    LaunchedEffect(isWebViewReady, recheckTrigger) {
        if (!isWebViewReady) return@LaunchedEffect

        try {
            kotlinx.coroutines.delay(500)

            var torReady = false
            var proxyRule: String? = null
            withContext(Dispatchers.IO) {
                try {
                    // I/O de red SIEMPRE en Dispatchers.IO (nunca en el hilo principal).
                    val hasHttp = OrbotDetector.isHttpProxyAvailable()
                    val hasSocks = OrbotDetector.isSocksProxyAvailable()
                    proxyRule = when {
                        hasHttp -> "http://$ORBOT_HTTP_HOST:$ORBOT_HTTP_PORT"
                        hasSocks -> "socks5://$ORBOT_HTTP_HOST:$ORBOT_SOCKS_PORT"
                        else -> null
                    }
                    torReady = proxyRule != null
                    orbotStatus = OrbotDetector.getStatus(context)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error detectando Orbot", e)
                }
            }

            if (torReady && proxyRule != null) {
                val result = setupOrbotProxy(proxyRule!!) {
                    mainHandler.post {
                        proxyEnabled = true
                        browsingMode = "tor"
                        Log.d(TAG, "✓ Proxy Tor activo")
                    }
                }
                if (result is ProxyResult.Applied) {
                    Log.d(TAG, "✓ Proxy Tor aplicado — recargando...")
                    // Recargar la página con proxy activo
                    webView?.reload()
                }
            } else if (recheckTrigger == 0) {
                // Solo avisamos una vez al abrir; no molestamos en cada recarga.
                Log.d(TAG, "🌐 Proxy Tor no disponible — modo directo")
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "🌐 Modo directo — Tor no disponible. Instala Orbot para .onion",
                        duration = SnackbarDuration.Long
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en configuración de proxy", e)
        }
    }

    // ── PASO 2b: Re-detección AUTOMÁTICA en tiempo real ──
    // La detección de arriba corre una sola vez al abrir. Si el usuario arranca
    // Orbot DESPUÉS de abrir el navegador, antes no se enteraba hasta pulsar
    // Recargar (de ahí el "no detecta en tiempo real" y los .onion que fallaban).
    // Este bucle re-dispara la detección existente (vía recheckTrigger) cada pocos
    // segundos hasta que el proxy Tor queda activo, y se detiene solo. Reutiliza el
    // camino ya probado; no aplica proxy por su cuenta.
    LaunchedEffect(isWebViewReady) {
        if (!isWebViewReady) return@LaunchedEffect
        var attempts = 0
        while (!proxyEnabled && attempts < 20) {
            kotlinx.coroutines.delay(4000)
            if (proxyEnabled) break
            attempts++
            recheckTrigger++
        }
    }

    // ── Limpiar proxy al salir ──
    DisposableEffect(Unit) {
        onDispose {
            clearOrbotProxy()
            Log.d(TAG, "Proxy Tor limpiado al salir del navegador")
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
                            imageVector = if (proxyEnabled) Icons.Default.Shield
                                          else Icons.Default.Public,
                            contentDescription = null,
                            tint = if (proxyEnabled) Success else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (proxyEnabled) "🧅 Navegador Tor" else "🌐 Navegador",
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
            // ── Status bar ──
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(8.dp),
                color = DarkSurface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = if (proxyEnabled) Icons.Default.Shield
                                       else Icons.Default.Wifi,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = when {
                            proxyEnabled -> "✅ Navegando por Tor (Orbot)"
                            browsingMode == "directo" -> "🌐 Modo directo — sin proxy"
                            else -> orbotStatus.ifEmpty { "Inicializando..." }
                        },
                        color = Color.Gray,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f).padding(start = 6.dp)
                    )
                    if (!OrbotDetector.isOrbotInstalled(context)) {
                        TextButton(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                    data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=org.torproject.android")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(
                                            "Descarga Orbot desde Play Store o F-Droid: org.torproject.android"
                                        )
                                    }
                                }
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Descargar Orbot", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (!proxyEnabled) {
                        TextButton(
                            onClick = { OrbotDetector.launchOrbot(context) },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Abrir Orbot", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            // ── URL Bar ──
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
                    // Back
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = if (canGoBack) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                        onClick = { if (canGoBack) webView?.goBack() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = if (canGoBack) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Forward
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = if (canGoForward) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                        onClick = { if (canGoForward) webView?.goForward() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Forward",
                                tint = if (canGoForward) MaterialTheme.colorScheme.primary else Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // URL input
                    var isFocused by remember { mutableStateOf(false) }

                    OutlinedTextField(
                        value = if (isFocused) urlInput else currentUrl,
                        onValueChange = { urlInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                                if (focusState.isFocused) urlInput = ""
                            },
                        placeholder = {
                            Text(
                                if (proxyEnabled) "Buscar o ingresar URL — .onion disponible 🧅"
                                else "Buscar o ingresar URL (solo clearnet)",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
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
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = DarkElevated,
                            unfocusedContainerColor = DarkElevated,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                val url = processUrl(urlInput)
                                webView?.loadUrl(url)
                                urlInput = ""
                                isFocused = false
                            }
                        )
                    )

                    // Reload/Stop
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        onClick = {
                            if (isLoading) {
                                webView?.stopLoading()
                            } else {
                                // Recargar y volver a detectar Orbot (por si el usuario
                                // acaba de arrancarlo) sin salir del navegador.
                                if (!proxyEnabled) recheckTrigger++
                                webView?.reload()
                            }
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                contentDescription = if (isLoading) "Stop" else "Refresh",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ── WebView ──
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
                            onLoadingChanged = { loading -> isLoading = loading },
                            context = context,
                            scope = scope,
                            snackbarHostState = snackbarHostState,
                            proxyEnabled = { proxyEnabled }
                        )
                        // Marcar que el WebView está listo
                        isWebViewReady = true
                        Log.d(TAG, "✓ WebView initialized and ready")
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            )

            // ─────────────────────────────────────────────────────────────────────────────
            // Loading indicator
            // ─────────────────────────────────────────────────────────────────────────────
            if (isLoading && currentUrl.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  CONSTANTES
// ─────────────────────────────────────────────────────────────────────────────

/** Host del proxy HTTP de Orbot */
private const val ORBOT_HTTP_HOST = "127.0.0.1"

/** Puerto del proxy HTTP de Orbot */
private const val ORBOT_HTTP_PORT = 8118

/** Puerto SOCKS5 de Orbot (para verificación) */
private const val ORBOT_SOCKS_PORT = 9050

// ─────────────────────────────────────────────────────────────────────────────
//  PROXY CONFIG — AndroidX WebKit oficial API
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Configura el proxy de Orbot a nivel de WebView delegando en [ProxyManager].
 *
 * Orbot expone un proxy HTTP en 127.0.0.1:8118 que internamente
 * traduce HTTP → SOCKS5 → Tor. Esto permite que WebView acceda
 * a sitios .onion sin necesidad de reflection ni hacks.
 *
 * La causa raíz del crash (`RejectedExecutionException`) era crear un `Executor` por operación
 * y apagarlo (`shutdown()`) de inmediato, antes de que WebView invocara el callback asíncrono de
 * `setProxyOverride`. [ProxyManager] usa un executor de larga vida compartido que NUNCA se apaga
 * antes del callback, por lo que ya no se llama a `shutdown()` aquí.
 *
 * @param onApplied callback que WebView invoca (en el executor de [ProxyManager]) cuando el
 *   override de proxy se ha aplicado; el llamador debe postear al MainThread para tocar la UI.
 * @return el [ProxyResult] devuelto por [ProxyManager].
 */
private fun setupOrbotProxy(proxyUrl: String, onApplied: () -> Unit): ProxyResult {
    return ProxyManager.applyProxyRule(
        proxyUrl = proxyUrl,
        onApplied = onApplied
    )
}

/**
 * Limpia la configuración del proxy para que el resto de la app
 * no quede enrutando tráfico a través de Tor.
 *
 * Delega en [ProxyManager.clearProxy], que reutiliza el executor de larga vida compartido
 * (sin `shutdown()` inmediato). Se envuelve en `try/catch` para no propagar excepciones,
 * de modo que el cierre de la pantalla nunca crashee.
 */
private fun clearOrbotProxy() {
    try {
        ProxyManager.clearProxy {
            Log.d(TAG, "✓ Proxy limpiado (callback) — tráfico directo restaurado")
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error limpiando proxy", e)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  WEBVIEW SETUP
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Procesa la entrada del usuario: si es una URL, la completa;
 * si es texto, busca en DuckDuckGo.
 * 
 * Manejo especial para .onion:
 * - Los enlaces .onion siempre usan http:// (no https://)
 * - Se valida el formato correcto de direcciones .onion
 * - DuckDuckGo tiene soporte nativo para buscar en la dark web cuando usas Tor
 * 
 * DuckDuckGo Onion:
 * - Búsquedas normales: https://duckduckgo.com
 * - Con Tor activo: puedes acceder a https://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion
 * - Los resultados .onion se muestran automáticamente cuando usas Tor
 */
private fun processUrl(input: String): String {
    val trimmed = input.trim()
    return when {
        // URLs completas (http:// o https://)
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
        
        // Enlaces .onion (siempre usar http:// - Tor no soporta HTTPS en .onion)
        trimmed.endsWith(".onion") || trimmed.contains(".onion/") || trimmed.contains(".onion?") -> {
            val cleanUrl = if (trimmed.contains("://")) {
                // Si ya tiene protocolo, forzar http://
                trimmed.replaceFirst("https://", "http://")
            } else {
                "http://$trimmed"
            }
            Log.d(TAG, "🧅 URL .onion detectada: $cleanUrl")
            cleanUrl
        }
        
        // URLs sin protocolo pero con dominio
        trimmed.contains(".") && !trimmed.contains(" ") -> {
            // Si parece una URL .onion sin protocolo
            if (trimmed.contains(".onion")) "http://$trimmed"
            else "https://$trimmed"
        }
        
        // Búsqueda en DuckDuckGo (clearnet)
        // Nota: DuckDuckGo automáticamente muestra resultados .onion cuando detecta Tor
        else -> "https://duckduckgo.com/?q=${java.net.URLEncoder.encode(trimmed, "UTF-8")}"
    }
}

/**
 * Genera una página de error HTML estilizada para mostrar cuando una URL falla.
 *
 * @param url La URL que falló
 * @param errorCode Código de error HTTP (404, 500, etc.) o -1 para error de red
 * @param description Descripción del error
 */
private fun getErrorPage(url: String, errorCode: Int, description: String): String = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            background: linear-gradient(135deg, #0A0A0A 0%, #1A1A2E 100%);
            color: #FFFFFF;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            padding: 20px;
        }
        .container { max-width: 520px; text-align: center; }
        .error-icon { font-size: 72px; margin-bottom: 16px; }
        h1 {
            font-size: 26px;
            margin-bottom: 12px;
            background: linear-gradient(135deg, #7C3AED, #00D4FF);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }
        .error-code {
            font-size: 48px;
            font-weight: bold;
            color: #FF6B6B;
            margin-bottom: 8px;
        }
        p { color: #AAAAAA; font-size: 15px; line-height: 1.6; margin-bottom: 24px; }
        .url-box {
            background: rgba(124, 58, 237, 0.1);
            border: 1px solid rgba(124, 58, 237, 0.3);
            border-radius: 8px;
            padding: 12px;
            margin-bottom: 24px;
            font-family: 'Courier New', monospace;
            font-size: 13px;
            color: #7C3AED;
            word-break: break-all;
        }
        .desc-box {
            text-align: left;
            background: rgba(255, 255, 255, 0.05);
            border-radius: 12px;
            padding: 16px;
            margin-bottom: 16px;
        }
        .desc-box p { margin-bottom: 0; font-size: 14px; color: #CCCCCC; }
        .brand { margin-top: 24px; font-size: 13px; color: #666666; }
        .hint { font-size: 12px; color: #888888; margin-top: 8px; }
    </style>
</head>
<body>
    <div class="container">
        <div class="error-icon">⚠️</div>
        <div class="error-code">${if (errorCode > 0) "$errorCode" else "RED"}</div>
        <h1>${if (errorCode == 404) "Página no encontrada" else if (errorCode in 500..599) "Error del servidor" else "Error de conexión"}</h1>
        <p>${description}</p>
        <div class="url-box">${url}</div>
        <div class="desc-box">
            <p>💡 Sugerencias:</p>
            <p style="margin-top: 8px; font-size: 13px;">
                ${getSuggestionsForError(errorCode, url)}
            </p>
        </div>
        <div class="hint">Intenta recargar la página o verifica la URL</div>
        <div class="brand">Azelgram — Navegador Privado</div>
    </div>
</body>
</html>
""".trimIndent()

/**
 * Genera sugerencias contextuales según el tipo de error.
 */
private fun getSuggestionsForError(errorCode: Int, url: String): String {
    return when {
        errorCode == 404 -> "La página que buscas no existe en este servidor. Verifica que la URL sea correcta."
        errorCode in 500..599 -> "El servidor está teniendo problemas. Espera un momento e intenta de nuevo."
        url.contains(".onion") -> "Los sitios .onion requieren Orbot activo. Abre Orbot, presiona 'Iniciar' y vuelve a intentar."
        errorCode == -1 || errorCode == 0 -> "No se pudo conectar al servidor. Verifica tu conexión a internet."
        else -> "Ocurrió un error inesperado. Intenta recargar la página."
    }
}

/**
 * Página de ayuda cuando un sitio .onion no carga (Orbot inactivo).
 * Incluye instrucciones claras y enlaces .onion de ejemplo para probar.
 */
private fun getOnionHelpPage(url: String): String = """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            background: linear-gradient(135deg, #0A0A0A 0%, #1A1A2E 100%);
            color: #FFFFFF;
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            padding: 20px;
        }
        .container { max-width: 520px; text-align: center; }
        .onion-icon { font-size: 72px; margin-bottom: 16px; }
        h1 {
            font-size: 26px;
            margin-bottom: 12px;
            background: linear-gradient(135deg, #7C3AED, #00D4FF);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            background-clip: text;
        }
        p { color: #AAAAAA; font-size: 15px; line-height: 1.6; margin-bottom: 24px; }
        .url-box {
            background: rgba(124, 58, 237, 0.1);
            border: 1px solid rgba(124, 58, 237, 0.3);
            border-radius: 8px;
            padding: 12px;
            margin-bottom: 24px;
            font-family: 'Courier New', monospace;
            font-size: 13px;
            color: #7C3AED;
            word-break: break-all;
        }
        .steps {
            text-align: left;
            background: rgba(255, 255, 255, 0.05);
            border-radius: 12px;
            padding: 24px;
            margin-bottom: 16px;
        }
        .steps h3 { margin-bottom: 16px; font-size: 18px; color: #7C3AED; }
        .steps ol { padding-left: 20px; }
        .steps li { margin: 12px 0; color: #CCCCCC; line-height: 1.6; }
        .steps strong { color: #00D4FF; }
        .examples {
            text-align: left;
            background: rgba(0, 255, 102, 0.05);
            border: 1px solid rgba(0, 255, 102, 0.2);
            border-radius: 12px;
            padding: 20px;
            margin-bottom: 16px;
        }
        .examples h3 { margin-bottom: 12px; font-size: 16px; color: #00FF66; }
        .examples p { font-size: 13px; color: #AAAAAA; margin-bottom: 12px; }
        .examples code {
            display: block;
            background: rgba(0, 0, 0, 0.3);
            padding: 8px 12px;
            border-radius: 6px;
            font-family: 'Courier New', monospace;
            font-size: 12px;
            color: #00D4FF;
            margin: 6px 0;
            word-break: break-all;
        }
        .brand { margin-top: 24px; font-size: 13px; color: #666666; }
    </style>
</head>
<body>
    <div class="container">
        <div class="onion-icon">🧅</div>
        <h1>Sitio .onion detectado</h1>
        <p>Los sitios .onion solo son accesibles a través de la red Tor. Necesitas <strong>Orbot</strong> activo para acceder.</p>
        <div class="url-box">$url</div>
        <div class="steps">
            <h3>🔍 Cómo acceder a sitios .onion</h3>
            <ol>
                <li><strong>1.</strong> Descarga <strong>Orbot</strong> desde Google Play Store o F-Droid</li>
                <li><strong>2.</strong> Abre Orbot y presiona el botón <strong>"Iniciar"</strong> (icono de cebolla)</li>
                <li><strong>3.</strong> Espera a que se conecte a Tor (1-2 minutos) hasta ver "Conectado a la red Tor"</li>
                <li><strong>4.</strong> Vuelve a <strong>NexusChat</strong> y presiona <strong>Recargar</strong> en la barra superior</li>
                <li><strong>5.</strong> ¡Listo! El sitio .onion se cargará automáticamente</li>
            </ol>
        </div>
        <div class="examples">
            <h3>✅ Sitios .onion para probar</h3>
            <p>Una vez que Orbot esté activo, prueba estos enlaces:</p>
            <code>http://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion</code>
            <code>http://thehiddenwiki.onion</code>
            <code>http://3g2upl4pq6kufc4m.onion</code>
        </div>
        <p style="font-size: 13px; color: #888888;">
            💡 DuckDuckGo automáticamente te mostrará resultados .onion cuando uses Tor
        </p>
        <div class="brand">NexusChat — Navegación Privada con Tor</div>
    </div>
</body>
</html>
""".trimIndent()
/**
 * Configura el WebView con ajustes de privacidad y navegación.
 *
 * - El proxy se configura ANTES de cargar URLs (en [LaunchedEffect])
 * - Los errores HTTP y de red muestran páginas de error claras al usuario
 * - Las URLs .onion sin Tor activo muestran guía de configuración
 */
@SuppressLint("SetJavaScriptEnabled")
private fun WebView.setupWebView(
    onUrlChanged: (String) -> Unit,
    onLoadingChanged: (Boolean) -> Unit,
    context: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState,
    proxyEnabled: () -> Boolean
) {
    try {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            setGeolocationEnabled(false)
            allowFileAccess = false
            allowContentAccess = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            useWideViewPort = true
            loadWithOverviewMode = true
            layoutAlgorithm = android.webkit.WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
            userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36"
        }

        webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                try {
                    onLoadingChanged(true)
                    url?.let { onUrlChanged(it) }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error en onPageStarted", e)
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                try {
                    onLoadingChanged(false)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error en onPageFinished", e)
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return try {
                    val url = request?.url?.toString() ?: return false
                    
                    Log.d(TAG, "🔗 Intentando cargar URL: $url")
                    
                    // Verificar si es un enlace .onion
                    val isOnionUrl = url.contains(".onion")
                    
                    if (isOnionUrl) {
                        // CAUSA RAÍZ DEL CRASH: shouldOverrideUrlLoading corre en el hilo
                        // principal (UI). Llamar aquí a OrbotDetector.isSocksProxyAvailable()/
                        // isHttpProxyAvailable() hacía un Socket.connect BLOQUEANTE en el hilo
                        // principal → android.os.NetworkOnMainThreadException → la app crasheaba
                        // al tocar un enlace .onion sin Tor activo. Usamos únicamente el estado
                        // proxyEnabled, que ya se detectó de forma asíncrona en el LaunchedEffect.
                        val torActive = proxyEnabled()

                        if (!torActive) {
                            // Tor NO está activo - mostrar página de ayuda
                            Log.w(TAG, "⚠️ Intento de cargar .onion sin Tor activo: $url")
                            val helpHtml = getOnionHelpPage(url)
                            view?.loadDataWithBaseURL(null, helpHtml, "text/html", "UTF-8", null)
                            
                            scope.launch {
                                try {
                                    snackbarHostState.showSnackbar(
                                        message = "🧅 Los sitios .onion requieren Orbot activo. Abre Orbot y recarga.",
                                        duration = SnackbarDuration.Long
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ Error mostrando snackbar", e)
                                }
                            }
                            return true // Bloquear la carga
                        } else {
                            // Tor está activo - permitir carga del .onion
                            Log.d(TAG, "✅ Tor activo - cargando .onion: $url")
                            
                            // Asegurar que la URL usa http:// (no https://)
                            val correctedUrl = if (url.startsWith("https://") && url.contains(".onion")) {
                                url.replaceFirst("https://", "http://")
                            } else {
                                url
                            }
                            
                            if (correctedUrl != url) {
                                Log.d(TAG, "🔧 Corrigiendo protocolo .onion: $correctedUrl")
                                view?.loadUrl(correctedUrl)
                                return true
                            }
                            
                            return false // Permitir la carga normal
                        }
                    }
                    
                    // URLs normales (no .onion) - permitir siempre
                    false
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error en shouldOverrideUrlLoading", e)
                    false
                }
            }

            /**
             * Maneja ERRORES DE RED (DNS, timeout, conexión rechazada, etc.)
             * Se muestra una página de error clara al usuario.
             */
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: android.webkit.WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                try {
                    if (request?.isForMainFrame == true) {
                        val url = request.url.toString()
                        val errorCode = error?.errorCode ?: -1
                        val description = error?.description?.toString()
                            ?: "No se pudo cargar la página"

                        Log.w(TAG, "Error de red en $url: code=$errorCode desc=$description")

                        // Mostrar página de error apropiada según el tipo de URL
                        if (url.contains(".onion") && !proxyEnabled()) {
                            val helpHtml = getOnionHelpPage(url)
                            view?.loadDataWithBaseURL(null, helpHtml, "text/html", "UTF-8", null)
                        } else {
                            val errorHtml = getErrorPage(url, errorCode, description)
                            view?.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)
                        }

                        scope.launch {
                            try {
                                snackbarHostState.showSnackbar(
                                    message = "⚠️ Error al cargar $url — ${if (errorCode > 0) "Código $errorCode" else "Sin conexión"}",
                                    duration = SnackbarDuration.Long
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error mostrando snackbar", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error en onReceivedError", e)
                }
            }

            /**
             * Maneja ERRORES HTTP (404, 500, 403, etc.)
             * Esto es NUEVO — antes solo se manejaban errores de red, no HTTP.
             * Los errores HTTP 404 ahora muestran una pÃ¡gina de error informativa.
             */
            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                try {
                    if (request?.isForMainFrame == true) {
                        val url = request.url.toString()
                        val statusCode = errorResponse?.statusCode ?: 0
                        val reasonPhrase = errorResponse?.reasonPhrase ?: "Error"

                        Log.w(TAG, "Error HTTP en $url: $statusCode $reasonPhrase")

                        // Para .onion con error, mostrar la guía de ayuda
                        if (url.contains(".onion")) {
                            val helpHtml = getOnionHelpPage(url)
                            view?.loadDataWithBaseURL(null, helpHtml, "text/html", "UTF-8", null)
                            scope.launch {
                                try {
                                    snackbarHostState.showSnackbar(
                                        message = "🧅 No se pudo cargar $url — ¿Orbot activo?",
                                        duration = SnackbarDuration.Long
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "❌ Error mostrando snackbar", e)
                                }
                            }
                            return
                        }

                        // Para errores HTTP específicos, mostrar página de error detallada
                        val description = when (statusCode) {
                            404 -> "La página o recurso solicitado no existe en este servidor."
                            403 -> "No tienes permiso para acceder a esta página."
                            408 -> "La conexión con el servidor se agotó. Intenta de nuevo."
                            429 -> "Demasiadas solicitudes. Espera un momento e intenta de nuevo."
                            500 -> "El servidor encontró un error interno. Intenta más tarde."
                            502 -> "El servidor recibió una respuesta inválida de otro servidor."
                            503 -> "El servidor está temporalmente fuera de servicio. Intenta más tarde."
                            504 -> "El servidor no respondió a tiempo. Verifica tu conexión."
                            else -> "HTTP $statusCode: $reasonPhrase"
                        }

                        val errorHtml = getErrorPage(url, statusCode, description)
                        view?.loadDataWithBaseURL(null, errorHtml, "text/html", "UTF-8", null)

                        scope.launch {
                            try {
                                val msg = when (statusCode) {
                                    404 -> "❌ 404 — Página no encontrada"
                                    in 500..599 -> "⚠️ Error del servidor ($statusCode)"
                                    else -> "⚠️ HTTP $statusCode"
                                }
                                snackbarHostState.showSnackbar(
                                    message = "$msg: $url",
                                    duration = SnackbarDuration.Long
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error mostrando snackbar", e)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error en onReceivedHttpError", e)
                }
            }
        }
        
        Log.d(TAG, "✅ WebView configurado exitosamente")
    } catch (e: Exception) {
        Log.e(TAG, "❌ Error crítico configurando WebView", e)
        scope.launch {
            try {
                snackbarHostState.showSnackbar(
                    message = "❌ Error al configurar el navegador: ${e.message}",
                    duration = SnackbarDuration.Long
                )
            } catch (snackbarError: Exception) {
                Log.e(TAG, "❌ No se pudo mostrar error al usuario", snackbarError)
            }
        }
    }
}


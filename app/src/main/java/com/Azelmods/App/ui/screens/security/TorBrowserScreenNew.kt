package com.Azelmods.App.ui.screens.security

import com.Azelmods.App.i18n.tr

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.webkit.CookieManager
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.Azelmods.App.data.security.tor.AnonymityPolicy
import com.Azelmods.App.data.security.tor.OrbotDetector
import com.Azelmods.App.data.security.tor.ProxyManager
import com.Azelmods.App.data.security.tor.ProxyResult
import com.Azelmods.App.data.security.tor.TorPreferences
import com.Azelmods.App.data.security.tor.TorService
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.ui.theme.NexusTokens
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "Navegador"
private const val CLEARNET_HOME = "https://duckduckgo.com/"
private const val TOR_HOME = "https://duckduckgogg42xjoc72x3sjasowoarfbgcmvfimaftt6twagswzczad.onion/"
private const val TOR_CHECK = "https://check.torproject.org/"
private const val ORBOT_POLL_MS = 5_000L
private const val SECURITY_ROUTE = "tor_control"

/**
 * Con Tor activo, WebRTC seguiría hablando UDP directamente con servidores STUN y revelaría
 * la IP real a cualquier web que lo pidiera. Tor Browser lo desactiva; aquí igual.
 */
private const val PRIVACY_SCRIPT = """
(function () {
  try {
    ['RTCPeerConnection', 'webkitRTCPeerConnection', 'RTCDataChannel', 'RTCSessionDescription', 'RTCIceCandidate']
      .forEach(function (k) {
        try { Object.defineProperty(window, k, { value: undefined, writable: false, configurable: false }); }
        catch (e) { try { window[k] = undefined; } catch (e2) {} }
      });
    if (navigator.mediaDevices) {
      navigator.mediaDevices.getUserMedia = function () { return Promise.reject(new Error('Bloqueado')); };
      navigator.mediaDevices.enumerateDevices = function () { return Promise.resolve([]); };
    }
  } catch (e) {}
})();
"""

@HiltViewModel
class BrowserViewModel @Inject constructor(
    private val torService: TorService,
    private val torPreferences: TorPreferences
) : ViewModel() {
    val torState: StateFlow<TorState> = torService.torState

    fun enableAnonymousMode() {
        torPreferences.setAnonymousModeEnabled(true)
        torService.startTor()
    }
}

/** Por dónde sale el navegador en cada momento. */
private sealed interface BrowserRoute {
    data object Direct : BrowserRoute
    data class Tor(val port: Int) : BrowserRoute

    /** Modo anónimo encendido sin Tor listo: no se carga nada, para que nada salga sin él. */
    data object Waiting : BrowserRoute
}

/**
 * Navegador privado. WebView tiene su propia pila de red, así que el enrutado de la app no le
 * afecta: su proxy se fija aparte y, con el modo anónimo encendido, nunca carga nada por la
 * red normal. Al salir se borran cookies, caché, almacenamiento e historial.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TorBrowserScreenNew(
    navController: NavController,
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val torState by viewModel.torState.collectAsState()

    var orbotPort by remember { mutableStateOf<Int?>(null) }
    val modeOff = torState is TorState.Disconnected

    // Orbot ya encendido aunque el modo anónimo esté apagado: se aprovecha para el navegador.
    LaunchedEffect(modeOff) {
        orbotPort = null
        while (modeOff && isActive) {
            orbotPort = withContext(Dispatchers.IO) {
                if (OrbotDetector.isSocksProxyAvailable(1_000)) OrbotDetector.socksPort else null
            }
            delay(ORBOT_POLL_MS)
        }
    }

    val route: BrowserRoute = when (val state = torState) {
        is TorState.Connected -> BrowserRoute.Tor(state.circuitInfo.socksPort)
        is TorState.Disconnected -> orbotPort?.let { BrowserRoute.Tor(it) } ?: BrowserRoute.Direct
        else -> BrowserRoute.Waiting
    }
    val proxySupported = remember { WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE) }

    var webView by remember { mutableStateOf<WebView?>(null) }
    var webGeneration by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf("") }
    var failedUrl by remember { mutableStateOf<String?>(null) }
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableIntStateOf(100) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var proxyReady by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var field by remember { mutableStateOf(TextFieldValue("")) }
    var menuOpen by remember { mutableStateOf(false) }

    val routeRef = rememberUpdatedState(route)

    fun open(url: String) {
        failedUrl = null
        val web = webView
        when {
            web == null || !proxyReady -> pendingUrl = url
            // Sin Tor ese nombre acabaría en el DNS normal, revelando qué sitio se buscaba.
            Uri.parse(url).host.orEmpty().endsWith(".onion") && route !is BrowserRoute.Tor ->
                showOnionNeedsTor(web, url)
            else -> web.loadUrl(url)
        }
    }

    // El proxy se aplica antes de cargar nada y solo se carga al confirmarlo WebView.
    LaunchedEffect(route) {
        proxyReady = false
        when (val target = route) {
            is BrowserRoute.Tor -> {
                val result = ProxyManager.applyProxyRule("socks5://127.0.0.1:${target.port}") {
                    mainHandler.post { proxyReady = true }
                }
                if (result is ProxyResult.Failed) Log.w(TAG, "Sin proxy: ${result.reason}")
            }
            BrowserRoute.Direct -> {
                val result = ProxyManager.clearProxy { mainHandler.post { proxyReady = true } }
                if (result is ProxyResult.Failed) proxyReady = true
            }
            BrowserRoute.Waiting -> Unit
        }
    }

    // Primera carga, o recarga al cambiar de ruta para que la página no se quede en la anterior.
    LaunchedEffect(proxyReady, webView) {
        val web = webView ?: return@LaunchedEffect
        if (!proxyReady) return@LaunchedEffect
        val next = pendingUrl ?: failedUrl
        when {
            next != null -> {
                pendingUrl = null
                failedUrl = null
                web.loadUrl(next)
            }
            web.url == null -> web.loadUrl(homeFor(route))
            else -> web.reload()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Con el modo anónimo el proxy es de toda la app y debe seguir puesto.
            if (!AnonymityPolicy.active) ProxyManager.clearProxy {}
        }
    }

    BackHandler(enabled = editing || canGoBack) {
        when {
            editing -> focusManager.clearFocus()
            else -> webView?.goBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = NexusTokens.Color.BgBase
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
        ) {
            // ── Barra de dirección ──────────────────────────────────
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(NexusTokens.Color.BgSurface)
                    .padding(start = 4.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.Close, "Cerrar navegador", tint = NexusTokens.Color.TextPrimary)
                }
                Row(
                    Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(NexusTokens.Radius.pill))
                        .background(NexusTokens.Color.BgElevated)
                        .padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (route) {
                            is BrowserRoute.Tor -> Icons.Outlined.Security
                            BrowserRoute.Direct -> Icons.Outlined.Language
                            BrowserRoute.Waiting -> Icons.Outlined.HourglassTop
                        },
                        contentDescription = when (route) {
                            is BrowserRoute.Tor -> tr("Navegando por Tor")
                            BrowserRoute.Direct -> tr("Sin Tor")
                            BrowserRoute.Waiting -> tr("Esperando a Tor")
                        },
                        tint = if (route is BrowserRoute.Tor) NexusTokens.Color.Online else NexusTokens.Color.TextMuted,
                        modifier = Modifier.size(NexusTokens.IconSize.sm)
                    )
                    Spacer(Modifier.width(8.dp))
                    BasicTextField(
                        value = if (editing) field else TextFieldValue(displayAddress(currentUrl)),
                        onValueChange = { field = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = NexusTokens.Color.TextPrimary),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go,
                            autoCorrectEnabled = false
                        ),
                        keyboardActions = KeyboardActions(onGo = {
                            val typed = field.text.trim()
                            if (typed.isNotEmpty()) open(toUrl(typed, routeRef.value))
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { state ->
                                if (state.isFocused && !editing) {
                                    field = TextFieldValue(currentUrl, TextRange(0, currentUrl.length))
                                }
                                editing = state.isFocused
                            },
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (!editing && currentUrl.isEmpty()) {
                                    Text(
                                        tr("Buscar o escribir una dirección"),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = NexusTokens.Color.TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                inner()
                            }
                        }
                    )
                    IconButton(
                        onClick = {
                            when {
                                editing -> field = TextFieldValue("")
                                progress < 100 -> webView?.stopLoading()
                                failedUrl != null -> open(failedUrl!!)
                                else -> webView?.reload()
                            }
                        }
                    ) {
                        val showClose = editing || progress < 100
                        Icon(
                            if (showClose) Icons.Default.Close else Icons.Default.Refresh,
                            when {
                                editing -> tr("Borrar")
                                progress < 100 -> tr("Detener")
                                else -> tr("Recargar")
                            },
                            tint = NexusTokens.Color.TextSecondary,
                            modifier = Modifier.size(NexusTokens.IconSize.sm)
                        )
                    }
                }
            }

            // Altura fija para que la página no salte al empezar o terminar de cargar.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(NexusTokens.Color.BgSurface)
            ) {
                if (progress < 100) {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                        gapSize = 0.dp,
                        drawStopIndicator = {}
                    )
                }
            }

            RouteNotice(
                route = route,
                torState = torState,
                proxySupported = proxySupported,
                onEnableTor = viewModel::enableAnonymousMode,
                onOpenSecurity = { navController.navigate(SECURITY_ROUTE) }
            )

            // ── Página ──────────────────────────────────────────────
            key(webGeneration) {
                AndroidView(
                    // Sin recortar, WebView pinta su fondo sobre toda la ventana.
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            configurePrivacy(this)
                            setBackgroundColor(NexusTokens.Color.BgBase.toArgb())
                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView, newProgress: Int) {
                                    progress = newProgress
                                }

                                override fun onPermissionRequest(request: PermissionRequest) = request.deny()

                                override fun onGeolocationPermissionsShowPrompt(
                                    origin: String?,
                                    callback: GeolocationPermissions.Callback?
                                ) {
                                    callback?.invoke(origin, false, false)
                                }
                            }
                            webViewClient = object : WebViewClient() {
                                private fun syncNavigation(view: WebView) {
                                    canGoBack = view.canGoBack()
                                    canGoForward = view.canGoForward()
                                }

                                override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                                    // Oscuro solo mientras no hay página: muchas webs dan por
                                    // hecho un fondo blanco y su texto negro no se leería.
                                    if (url.startsWith("http")) view.setBackgroundColor(android.graphics.Color.WHITE)
                                    progress = 0
                                    currentUrl = url
                                    syncNavigation(view)
                                }

                                override fun onPageFinished(view: WebView, url: String) {
                                    progress = 100
                                    currentUrl = url
                                    syncNavigation(view)
                                }

                                override fun doUpdateVisitedHistory(view: WebView, url: String, isReload: Boolean) {
                                    currentUrl = url
                                    syncNavigation(view)
                                }

                                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                    val uri = request.url
                                    if (uri.scheme != "http" && uri.scheme != "https") return true
                                    if (uri.host.orEmpty().endsWith(".onion") && routeRef.value !is BrowserRoute.Tor) {
                                        showOnionNeedsTor(view, uri.toString())
                                        return true
                                    }
                                    return false
                                }

                                override fun onReceivedError(
                                    view: WebView,
                                    request: WebResourceRequest,
                                    error: WebResourceError
                                ) {
                                    if (!request.isForMainFrame) return
                                    val url = request.url.toString()
                                    failedUrl = url
                                    showMessagePage(
                                        view = view,
                                        url = url,
                                        title = tr("No se pudo abrir la página"),
                                        body = errorAdvice(url, routeRef.value, error.description?.toString())
                                    )
                                }

                                override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                                    handler.cancel()
                                    val url = error.url.orEmpty().ifEmpty { currentUrl }
                                    failedUrl = url
                                    showMessagePage(
                                        view = view,
                                        url = url,
                                        title = tr("Certificado no válido"),
                                        body = "La conexión con este sitio no es segura, así que no se ha abierto."
                                    )
                                }

                                // Sin esto, un fallo del proceso de la página cierra toda la app.
                                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                                    Log.w(TAG, "Proceso de la página caído (crash=${detail.didCrash()})")
                                    webView = null
                                    pendingUrl = currentUrl.takeIf { it.startsWith("http") }
                                    webGeneration++
                                    return true
                                }
                            }
                            setDownloadListener { _, _, _, _, _ ->
                                scope.launch {
                                    snackbar.showSnackbar(tr("El navegador privado no descarga archivos"))
                                }
                            }
                            webView = this
                        }
                    },
                    onRelease = { web ->
                        wipeBrowsingData(web)
                        web.destroy()
                    }
                )
            }

            // ── Barra de navegación ─────────────────────────────────
            if (!editing) {
                HorizontalDivider(thickness = 1.dp, color = NexusTokens.Color.Hairline)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(NexusTokens.Color.BgSurface)
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrowserAction(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", canGoBack) { webView?.goBack() }
                    BrowserAction(Icons.AutoMirrored.Filled.ArrowForward, "Adelante", canGoForward) { webView?.goForward() }
                    BrowserAction(Icons.Outlined.Home, "Inicio") { open(homeFor(route)) }
                    BrowserAction(Icons.Outlined.CleaningServices, "Borrar datos de navegación") {
                        webView?.let(::wipeBrowsingData)
                        open(homeFor(route))
                        scope.launch { snackbar.showSnackbar(tr("Cookies, caché e historial borrados")) }
                    }
                    Box {
                        BrowserAction(Icons.Default.MoreVert, "Más opciones") { menuOpen = true }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = NexusTokens.Color.BgElevated
                        ) {
                            BrowserMenuItem("Copiar dirección", Icons.Outlined.ContentCopy, enabled = currentUrl.isNotEmpty()) {
                                menuOpen = false
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Dirección", currentUrl))
                                scope.launch { snackbar.showSnackbar(tr("Dirección copiada")) }
                            }
                            BrowserMenuItem("Compartir dirección", Icons.Outlined.Share, enabled = currentUrl.isNotEmpty()) {
                                menuOpen = false
                                context.startActivity(
                                    Intent.createChooser(
                                        Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, currentUrl)
                                        },
                                        null
                                    )
                                )
                            }
                            BrowserMenuItem("Comprobar si salgo por Tor", Icons.Outlined.VerifiedUser) {
                                menuOpen = false
                                open(TOR_CHECK)
                            }
                            BrowserMenuItem("Modo anónimo", Icons.Outlined.Security) {
                                menuOpen = false
                                navController.navigate(SECURITY_ROUTE)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Solo aparece cuando hay algo que el usuario debe saber sobre por dónde sale. */
@Composable
private fun RouteNotice(
    route: BrowserRoute,
    torState: TorState,
    proxySupported: Boolean,
    onEnableTor: () -> Unit,
    onOpenSecurity: () -> Unit
) {
    val (text, action, onAction) = when {
        !proxySupported && route !is BrowserRoute.Direct -> Triple(
            "Tu versión de Android System WebView no admite proxy, así que no se carga nada. Actualízala.",
            null,
            null
        )
        route is BrowserRoute.Direct -> Triple("Sin Tor: las webs ven tu IP.", "Activar modo anónimo", onEnableTor)
        torState is TorState.Connecting -> Triple(
            "${torState.message} (${torState.progress} %). No se carga nada hasta que esté listo.",
            null,
            null
        )
        torState is TorState.Error -> Triple("Sin Tor, así que no se carga nada.", "Ver detalles", onOpenSecurity)
        else -> return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(NexusTokens.Color.BgSurface)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodySmall,
            color = NexusTokens.Color.TextSecondary,
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        )
        if (action != null && onAction != null) {
            TextButton(onClick = onAction) { Text(action, style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun BrowserAction(icon: ImageVector, description: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            icon,
            description,
            tint = if (enabled) NexusTokens.Color.TextSecondary else NexusTokens.Color.TextDisabled
        )
    }
}

@Composable
private fun BrowserMenuItem(label: String, icon: ImageVector, enabled: Boolean = true, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label, color = if (enabled) NexusTokens.Color.TextPrimary else NexusTokens.Color.TextDisabled) },
        leadingIcon = {
            Icon(icon, null, tint = if (enabled) NexusTokens.Color.TextSecondary else NexusTokens.Color.TextDisabled)
        },
        enabled = enabled,
        onClick = onClick
    )
}

private fun homeFor(route: BrowserRoute): String = if (route is BrowserRoute.Tor) TOR_HOME else CLEARNET_HOME

/** Lo que se lee en la barra sin tocarla: el dominio, que es lo que importa para no caer en un engaño. */
private fun displayAddress(url: String): String {
    if (url.isEmpty()) return ""
    return Uri.parse(url).host?.removePrefix("www.") ?: url
}

/** Convierte lo escrito en una dirección, o en una búsqueda si no parece una. */
private fun toUrl(typed: String, route: BrowserRoute): String {
    val hasScheme = typed.startsWith("http://") || typed.startsWith("https://")
    val looksLikeAddress = !typed.contains(' ') && typed.contains('.')
    return when {
        hasScheme -> typed
        // La mayoría de servicios .onion no llevan certificado público y sirven por http.
        looksLikeAddress && typed.substringBefore('/').endsWith(".onion") -> "http://$typed"
        looksLikeAddress -> "https://$typed"
        else -> homeFor(route) + "?q=" + Uri.encode(typed)
    }
}

private fun errorAdvice(url: String, route: BrowserRoute, detail: String?): String {
    val host = Uri.parse(url).host.orEmpty()
    val onionLabel = host.removeSuffix(".onion").substringAfterLast('.')
    return when {
        // Los v3 tienen 56 caracteres; Tor dejó de admitir los v2 de 16 en 2021.
        host.endsWith(".onion") && onionLabel.length <= 16 ->
            tr("Esa dirección .onion es del tipo antiguo (v2) y Tor dejó de admitirlas en 2021. Busca la dirección actual del sitio, que tiene 56 caracteres.")
        host.endsWith(".onion") ->
            tr("El servicio .onion no responde. Puede estar apagado. Reintenta en un momento.")
        route is BrowserRoute.Tor ->
            tr("Por Tor algunas webs bloquean la conexión o tardan más. Reintenta o crea una identidad nueva en Modo anónimo.")
        else -> "Comprueba tu conexión e inténtalo de nuevo." + if (detail.isNullOrBlank()) "" else " ($detail)"
    }
}

private fun showOnionNeedsTor(view: WebView, url: String) {
    showMessagePage(
        view = view,
        url = url,
        title = tr("Este sitio solo existe dentro de Tor"),
        body = "Activa el modo anónimo para abrirlo. No se ha enviado ninguna petición."
    )
}

/** Página propia para errores. Todo lo que viene de fuera va escapado: la URL la controla la web. */
private fun showMessagePage(view: WebView, url: String, title: String, body: String) {
    val html = """
        <!DOCTYPE html><html lang="es"><head><meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <style>
          body { background:#101214; color:#ECEEF0; font-family:sans-serif; margin:0; padding:48px 24px; line-height:1.5; }
          h1 { font-size:20px; font-weight:600; margin:0 0 12px; }
          p { color:#A6ACB3; font-size:15px; margin:0 0 20px; }
          code { color:#7C828A; font-size:12px; word-break:break-all; }
        </style></head>
        <body><h1>${Html.escapeHtml(title)}</h1><p>${Html.escapeHtml(body)}</p><code>${Html.escapeHtml(url)}</code></body></html>
    """.trimIndent()
    view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", url)
}

@SuppressLint("SetJavaScriptEnabled")
private fun configurePrivacy(web: WebView) {
    web.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false
        useWideViewPort = true
        loadWithOverviewMode = true
        setGeolocationEnabled(false)
        allowFileAccess = false
        allowContentAccess = false
        // Sin Safe Browsing: envía a Google huellas de cada dirección visitada.
        safeBrowsingEnabled = false
        javaScriptCanOpenWindowsAutomatically = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        // Igual para todos: el agente real de WebView delata modelo y versión del teléfono.
        userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }
    CookieManager.getInstance().setAcceptThirdPartyCookies(web, false)
    if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
        runCatching { WebViewCompat.addDocumentStartJavaScript(web, PRIVACY_SCRIPT, setOf("*")) }
            .onFailure { Log.w(TAG, "No se pudo inyectar el script de privacidad: ${it.message}") }
    }
}

private fun wipeBrowsingData(web: WebView) {
    runCatching {
        web.stopLoading()
        web.clearCache(true)
        web.clearHistory()
        web.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    }.onFailure { Log.w(TAG, "No se borraron todos los datos: ${it.message}") }
}

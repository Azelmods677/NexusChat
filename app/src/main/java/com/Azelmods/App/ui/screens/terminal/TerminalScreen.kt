package com.Azelmods.App.ui.screens.terminal

import com.Azelmods.App.i18n.tr

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.Azelmods.App.ui.theme.NexusTokens
import com.Azelmods.App.ui.theme.TerminalBlack
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

private const val DEFAULT_FONT_PX = 34
private const val MIN_FONT_PX = 16
private const val MAX_FONT_PX = 80

/**
 * Terminal real: emulador VT100/xterm conectado a una shell del sistema.
 *
 * Lo que Android permite sin root: los comandos del sistema (toybox) dentro de la carpeta
 * de la app, con los permisos de la app. No hay `apt` ni `pkg`, porque Android no deja
 * ejecutar binarios descargados a una app moderna; Termux lo resuelve con un sistema
 * completo que no cabe aquí.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    onBack: () -> Unit,
    viewModel: TerminalViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val sessions by viewModel.sessions.collectAsState()
    val current by viewModel.current.collectAsState()

    var ctrlOn by remember { mutableStateOf(false) }
    var altOn by remember { mutableStateOf(false) }
    var fontPx by remember { mutableIntStateOf(DEFAULT_FONT_PX) }
    val viewHolder = remember { arrayOfNulls<TerminalView>(1) }

    LaunchedEffect(Unit) {
        if (viewModel.sessions.value.isEmpty()) viewModel.newSession()
    }

    fun clipboard() = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    fun pasteInto(session: TerminalSession?) {
        val text = clipboard().primaryClip?.getItemAt(0)?.coerceToText(context)?.toString() ?: return
        session?.emulator?.paste(text)
    }

    DisposableEffect(viewModel) {
        viewModel.onSessionEvent = object : TerminalSessionClient {
            override fun onTextChanged(session: TerminalSession) {
                if (session === viewModel.current.value) viewHolder[0]?.onScreenUpdated()
            }
            override fun onTitleChanged(session: TerminalSession) = Unit
            override fun onSessionFinished(session: TerminalSession) {
                viewModel.removeFinished(session)
            }
            override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
                clipboard().setPrimaryClip(ClipData.newPlainText("Terminal", text))
            }
            override fun onPasteTextFromClipboard(session: TerminalSession?) = pasteInto(session)
            override fun onBell(session: TerminalSession) = Unit
            override fun onColorsChanged(session: TerminalSession) {
                viewHolder[0]?.onScreenUpdated()
            }
            override fun onTerminalCursorStateChange(state: Boolean) = Unit
            override fun getTerminalCursorStyle(): Int = 0
            override fun logError(tag: String?, message: String?) = Unit
            override fun logWarn(tag: String?, message: String?) = Unit
            override fun logInfo(tag: String?, message: String?) = Unit
            override fun logDebug(tag: String?, message: String?) = Unit
            override fun logVerbose(tag: String?, message: String?) = Unit
            override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) = Unit
            override fun logStackTrace(tag: String?, e: Exception?) = Unit
        }
        onDispose { viewModel.onSessionEvent = null }
    }

    Scaffold(
        containerColor = TerminalBlack,
        topBar = {
            TopAppBar(
                title = { Text(tr("Terminal"), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás")
                    }
                },
                actions = {
                    IconButton(onClick = { pasteInto(current) }) {
                        Icon(Icons.Outlined.ContentPaste, "Pegar")
                    }
                    IconButton(onClick = { viewModel.newSession() }) {
                        Icon(Icons.Default.Add, "Nueva sesión")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NexusTokens.Color.BgSurface,
                    titleContentColor = NexusTokens.Color.TextPrimary,
                    navigationIconContentColor = NexusTokens.Color.TextPrimary,
                    actionIconContentColor = NexusTokens.Color.TextSecondary
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
        ) {
            if (sessions.size > 1) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(NexusTokens.Color.BgSurface)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    sessions.forEachIndexed { index, session ->
                        val selected = session === current
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(NexusTokens.Radius.sm))
                                .background(if (selected) NexusTokens.Color.BgHigh else NexusTokens.Color.BgElevated)
                                .clickable { viewModel.select(session) }
                                .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Sesión ${index + 1}",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (selected) NexusTokens.Color.TextPrimary else NexusTokens.Color.TextMuted
                            )
                            Icon(
                                Icons.Default.Close,
                                "Cerrar sesión",
                                tint = NexusTokens.Color.TextMuted,
                                modifier = Modifier
                                    .padding(start = 4.dp)
                                    .size(16.dp)
                                    .clickable { viewModel.close(session) }
                            )
                        }
                    }
                }
            }

            key(current) {
                AndroidView(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.Black),
                    factory = { ctx ->
                        TerminalView(ctx, null).apply {
                            viewHolder[0] = this
                            isFocusable = true
                            isFocusableInTouchMode = true
                            setTextSize(fontPx)
                            setTypeface(Typeface.MONOSPACE)
                            setTerminalViewClient(
                                ComposeViewClient(
                                    view = this,
                                    readCtrl = {
                                        val value = ctrlOn
                                        if (value) ctrlOn = false
                                        value
                                    },
                                    readAlt = {
                                        val value = altOn
                                        if (value) altOn = false
                                        value
                                    },
                                    onZoom = { factor ->
                                        fontPx = (fontPx * factor).toInt().coerceIn(MIN_FONT_PX, MAX_FONT_PX)
                                        setTextSize(fontPx)
                                    }
                                )
                            )
                            current?.let { attachSession(it) }
                            // Lista para escribir al entrar. Aún no está en pantalla: sin el
                            // post, el foco se pierde y el teclado no aparece.
                            post {
                                requestFocus()
                                val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
                            }
                        }
                    },
                    update = { view ->
                        current?.let { session ->
                            if (view.mTermSession !== session) view.attachSession(session)
                        }
                    }
                )
            }

            // Teclas que un teclado de móvil no tiene. Ctrl y Alt se aplican a la siguiente.
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(NexusTokens.Color.BgSurface)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                fun send(text: String) { current?.write(text) }
                fun key(code: Int) {
                    viewHolder[0]?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, code))
                    viewHolder[0]?.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, code))
                }
                TermKey("Esc") { send("\u001b") }
                TermKey("Tab") { send("\t") }
                TermKey("Ctrl", active = ctrlOn) { ctrlOn = !ctrlOn }
                TermKey("Alt", active = altOn) { altOn = !altOn }
                TermKey("↑") { key(KeyEvent.KEYCODE_DPAD_UP) }
                TermKey("↓") { key(KeyEvent.KEYCODE_DPAD_DOWN) }
                TermKey("←") { key(KeyEvent.KEYCODE_DPAD_LEFT) }
                TermKey("→") { key(KeyEvent.KEYCODE_DPAD_RIGHT) }
                listOf("/", "-", "|", "~", "&", ">", "<", "*", "$", ".", ";", "\"", "'").forEach { symbol ->
                    TermKey(symbol) { send(symbol) }
                }
                TermKey("Inicio") { key(KeyEvent.KEYCODE_MOVE_HOME) }
                TermKey("Fin") { key(KeyEvent.KEYCODE_MOVE_END) }
                TermKey("RePág") { key(KeyEvent.KEYCODE_PAGE_UP) }
                TermKey("AvPág") { key(KeyEvent.KEYCODE_PAGE_DOWN) }
            }
        }
    }
}

@Composable
private fun TermKey(label: String, active: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(NexusTokens.Radius.xs))
            .background(if (active) MaterialTheme.colorScheme.primary else NexusTokens.Color.BgElevated)
            .clickable(onClick = onClick)
            .defaultMinSize(minWidth = 38.dp, minHeight = 36.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = if (active) MaterialTheme.colorScheme.onPrimary else NexusTokens.Color.TextPrimary
        )
    }
}

private class ComposeViewClient(
    private val view: TerminalView,
    private val readCtrl: () -> Boolean,
    private val readAlt: () -> Boolean,
    private val onZoom: (Float) -> Unit
) : TerminalViewClient {
    override fun onScale(scale: Float): Float {
        if (scale < 0.9f || scale > 1.1f) {
            onZoom(scale)
            return 1f
        }
        return scale
    }

    override fun onSingleTapUp(e: MotionEvent) {
        view.requestFocus()
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean = false
    override fun shouldEnforceCharBasedInput(): Boolean = true
    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
    override fun isTerminalViewSelected(): Boolean = true
    override fun copyModeChanged(copyMode: Boolean) = Unit
    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean = false
    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false
    override fun onLongPress(event: MotionEvent): Boolean = false
    override fun readControlKey(): Boolean = readCtrl()
    override fun readAltKey(): Boolean = readAlt()
    override fun readShiftKey(): Boolean = false
    override fun readFnKey(): Boolean = false
    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false
    override fun onEmulatorSet() = Unit
    override fun logError(tag: String?, message: String?) = Unit
    override fun logWarn(tag: String?, message: String?) = Unit
    override fun logInfo(tag: String?, message: String?) = Unit
    override fun logDebug(tag: String?, message: String?) = Unit
    override fun logVerbose(tag: String?, message: String?) = Unit
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) = Unit
    override fun logStackTrace(tag: String?, e: Exception?) = Unit
}

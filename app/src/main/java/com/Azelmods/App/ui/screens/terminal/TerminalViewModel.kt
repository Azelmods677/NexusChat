package com.Azelmods.App.ui.screens.terminal

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject

/**
 * Sesiones de terminal.
 *
 * Son shells reales sobre una pseudoterminal (PTY) del sistema: `/system/bin/sh` con un
 * proceso vivo. Se conserva el estado entre comandos (`cd`, `export`), funcionan los
 * programas interactivos (`top`, `vi`), Ctrl+C interrumpe y los colores se ven. La versión
 * anterior lanzaba cada comando por separado y esperaba a que terminara antes de mostrar
 * nada, con un tope de 60 segundos.
 *
 * Viven en el ViewModel para sobrevivir a rotar la pantalla o abrir otra un momento.
 */
@HiltViewModel
class TerminalViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _sessions = MutableStateFlow<List<TerminalSession>>(emptyList())
    val sessions: StateFlow<List<TerminalSession>> = _sessions.asStateFlow()

    private val _current = MutableStateFlow<TerminalSession?>(null)
    val current: StateFlow<TerminalSession?> = _current.asStateFlow()

    /** Lo reciben las sesiones; la pantalla lo conecta con su vista y el portapapeles. */
    var onSessionEvent: TerminalSessionClient? = null

    private val home: File by lazy { File(context.filesDir, "home").apply { mkdirs() } }

    private val client = object : TerminalSessionClient {
        override fun onTextChanged(session: TerminalSession) { onSessionEvent?.onTextChanged(session) }
        override fun onTitleChanged(session: TerminalSession) { onSessionEvent?.onTitleChanged(session) }
        override fun onSessionFinished(session: TerminalSession) { onSessionEvent?.onSessionFinished(session) }
        override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
            onSessionEvent?.onCopyTextToClipboard(session, text)
        }
        override fun onPasteTextFromClipboard(session: TerminalSession?) {
            onSessionEvent?.onPasteTextFromClipboard(session)
        }
        override fun onBell(session: TerminalSession) = Unit
        override fun onColorsChanged(session: TerminalSession) { onSessionEvent?.onColorsChanged(session) }
        override fun onTerminalCursorStateChange(state: Boolean) = Unit
        override fun getTerminalCursorStyle(): Int = TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE
        override fun logError(tag: String?, message: String?) { Log.e(tag ?: TAG, message.orEmpty()) }
        override fun logWarn(tag: String?, message: String?) { Log.w(tag ?: TAG, message.orEmpty()) }
        override fun logInfo(tag: String?, message: String?) = Unit
        override fun logDebug(tag: String?, message: String?) = Unit
        override fun logVerbose(tag: String?, message: String?) = Unit
        override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {
            Log.e(tag ?: TAG, message.orEmpty(), e)
        }
        override fun logStackTrace(tag: String?, e: Exception?) { Log.e(tag ?: TAG, "Error", e) }
    }

    fun newSession(): TerminalSession {
        val env = arrayOf(
            "TERM=xterm-256color",
            "COLORTERM=truecolor",
            "HOME=${home.absolutePath}",
            // La shell compone el indicador con este nombre; sin él empezaba por ":".
            "HOSTNAME=nexus",
            "TMPDIR=${context.cacheDir.absolutePath}",
            "PATH=/system/bin:/system/xbin:/vendor/bin:/product/bin",
            "LANG=es_ES.UTF-8",
            "ANDROID_ROOT=/system",
            "ANDROID_DATA=/data",
            "EXTERNAL_STORAGE=/sdcard",
            "PS1=\$PWD \$ "
        )
        val session = TerminalSession(
            "/system/bin/sh",
            home.absolutePath,
            arrayOf("sh"),
            env,
            TRANSCRIPT_ROWS,
            client
        )
        _sessions.value = _sessions.value + session
        _current.value = session
        return session
    }

    fun select(session: TerminalSession) {
        _current.value = session
    }

    fun close(session: TerminalSession) {
        session.finishIfRunning()
        removeFinished(session)
    }

    fun removeFinished(session: TerminalSession) {
        val remaining = _sessions.value - session
        _sessions.value = remaining
        if (_current.value === session) _current.value = remaining.lastOrNull()
    }

    override fun onCleared() {
        super.onCleared()
        onSessionEvent = null
        _sessions.value.forEach { it.finishIfRunning() }
    }

    private companion object {
        const val TAG = "Terminal"
        const val TRANSCRIPT_ROWS = 5_000
    }
}

package com.Azelmods.App.data.terminal

import android.content.Context
import android.os.Build
import android.system.Os
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.tukaani.xz.XZInputStream
import java.io.BufferedInputStream
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

sealed interface LinuxState {
    /** Este teléfono no puede ejecutarlo (solo hay binarios de 64 bits). */
    data object Unsupported : LinuxState
    data object NotInstalled : LinuxState
    data class Downloading(val progress: Float) : LinuxState
    data class Extracting(val files: Int) : LinuxState
    data object Ready : LinuxState
    data class Failed(val reason: String) : LinuxState
}

/**
 * Kali Linux dentro de la app, sin root.
 *
 * Android no deja a una app ejecutar lo que descarga, pero sí lo que viene en su carpeta de
 * librerías nativas: ahí va `proot`, que hace creer a los programas de Linux que están en su
 * propio sistema. El sistema en sí (126 MB) solo se descarga cuando alguien lo pide, y con él
 * funcionan `apt update`, `apt install` y cualquier herramienta de Kali.
 */
@Singleton
class LinuxEnvironment @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val base get() = File(context.filesDir, "linux")
    private val rootfs get() = File(base, "kali")
    private val marker get() = File(base, ".installed")
    private val libs get() = File(base, "lib")
    private val tmp get() = File(base, "tmp")
    private val nativeDir get() = File(context.applicationInfo.nativeLibraryDir)

    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<LinuxState> = _state.asStateFlow()

    private fun initialState(): LinuxState = when {
        !isSupported() -> LinuxState.Unsupported
        marker.exists() -> LinuxState.Ready
        else -> LinuxState.NotInstalled
    }

    private fun isSupported(): Boolean =
        "arm64-v8a" in Build.SUPPORTED_ABIS && File(nativeDir, "libproot.so").exists()

    fun install() {
        val current = _state.value
        if (current is LinuxState.Downloading || current is LinuxState.Extracting) return
        _state.value = LinuxState.Downloading(0f)
        scope.launch {
            runCatching {
                base.mkdirs()
                rootfs.deleteRecursively()
                val archive = download()
                extract(archive)
                archive.delete()
                configure()
                marker.writeText(ROOTFS_URL)
            }.onSuccess { _state.value = LinuxState.Ready }
                .onFailure {
                    Log.e(TAG, "No se pudo instalar Kali", it)
                    _state.value = LinuxState.Failed(it.message ?: it.javaClass.simpleName)
                }
        }
    }

    fun uninstall() {
        scope.launch {
            base.deleteRecursively()
            _state.value = initialState()
        }
    }

    private fun download(): File {
        val archive = File(base, "kali.tar.xz.part")
        // Si se cortó, continúa donde iba: son más de 100 MB.
        val already = if (archive.exists()) archive.length() else 0L
        val client = OkHttpClient.Builder().readTimeout(60, TimeUnit.SECONDS).build()
        val request = Request.Builder().url(ROOTFS_URL).apply {
            if (already > 0) header("Range", "bytes=$already-")
        }.build()
        client.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            val resumed = response.code == 206
            val body = response.body!!
            val total = body.contentLength().coerceAtLeast(1L) + if (resumed) already else 0L
            var done = if (resumed) already else 0L
            body.byteStream().use { input ->
                java.io.FileOutputStream(archive, resumed).use { output ->
                    val buffer = ByteArray(128 * 1024)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        _state.value = LinuxState.Downloading((done.toFloat() / total).coerceAtMost(1f))
                    }
                }
            }
        }
        return archive
    }

    private fun extract(archive: File) {
        rootfs.mkdirs()
        val root = rootfs.canonicalPath
        var count = 0
        // Los enlaces duros se copian al final: el archivo al que apuntan puede venir después.
        val hardLinks = mutableListOf<Pair<File, File>>()
        TarArchiveInputStream(XZInputStream(BufferedInputStream(archive.inputStream()))).use { tar ->
            generateSequence { tar.nextEntry }.forEach { entry: TarArchiveEntry ->
                val relative = stripRootFolder(entry.name) ?: return@forEach
                val target = File(rootfs, relative)
                // Un archivo con rutas tipo "../.." no debe poder escribir fuera de su carpeta.
                if (!target.canonicalPath.startsWith(root)) return@forEach
                when {
                    entry.isDirectory -> {
                        target.mkdirs()
                        chmod(target, entry.mode)
                    }
                    entry.isSymbolicLink -> {
                        target.parentFile?.mkdirs()
                        target.delete()
                        runCatching { Os.symlink(entry.linkName, target.path) }
                    }
                    entry.isLink -> {
                        val source = stripRootFolder(entry.linkName)?.let { File(rootfs, it) }
                        if (source != null) hardLinks += source to target
                    }
                    entry.isFile -> {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { tar.copyTo(it) }
                        chmod(target, entry.mode)
                    }
                }
                if (++count % 200 == 0) _state.value = LinuxState.Extracting(count)
            }
        }
        hardLinks.forEach { (source, target) ->
            runCatching {
                target.parentFile?.mkdirs()
                source.copyTo(target, overwrite = true)
                target.setExecutable(source.canExecute())
            }
        }
    }

    private fun chmod(file: File, mode: Int) {
        // Se añade siempre el permiso del dueño: sin él, la propia app no podría usarlos.
        runCatching { Os.chmod(file.path, (mode and 0xFFF) or 0x180 or if (file.isDirectory) 0x40 else 0) }
    }

    private fun configure() {
        File(rootfs, "etc").mkdirs()
        File(rootfs, "etc/resolv.conf").apply { delete() }.writeText("nameserver 1.1.1.1\nnameserver 8.8.8.8\n")
        File(rootfs, "etc/hosts").apply { delete() }.writeText("127.0.0.1 localhost\n::1 localhost\n")
        // apt intenta cambiar a su usuario `_apt`, y dentro de proot eso falla.
        File(rootfs, "etc/apt/apt.conf.d").mkdirs()
        File(rootfs, "etc/apt/apt.conf.d/99nexus").writeText("APT::Sandbox::User \"root\";\n")
        File(rootfs, "root").mkdirs()
        File(rootfs, "root/.hushlogin").writeText("")
    }

    /** Programa, argumentos y entorno para abrir una shell de Kali en una sesión de terminal. */
    fun shellCommand(): ShellCommand {
        tmp.mkdirs()
        libs.mkdirs()
        // proot busca la librería con este nombre, que no se puede empaquetar como nativa.
        val talloc = File(libs, "libtalloc.so.2")
        val talloCSource = File(nativeDir, "libtalloc.so").path
        talloc.delete()
        Os.symlink(talloCSource, talloc.path)

        val proot = File(nativeDir, "libproot.so").path
        val args = arrayOf(
            "proot",
            "--kill-on-exit",
            "--link2symlink",
            "-0",
            "-r", rootfs.path,
            "-b", "/dev",
            "-b", "/proc",
            "-b", "/sys",
            "-b", "/sdcard",
            "-b", "${tmp.path}:/dev/shm",
            "-w", "/root",
            "/usr/bin/env", "-i",
            "HOME=/root",
            "USER=root",
            "TERM=xterm-256color",
            "COLORTERM=truecolor",
            "LANG=C.UTF-8",
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin",
            "/bin/bash", "--login"
        )
        val env = arrayOf(
            "PROOT_TMP_DIR=${tmp.path}",
            "PROOT_LOADER=${File(nativeDir, "libproot-loader.so").path}",
            "LD_LIBRARY_PATH=${libs.path}:${nativeDir.path}",
            "TERM=xterm-256color"
        )
        return ShellCommand(proot, base.path, args, env)
    }

    data class ShellCommand(
        val executable: String,
        val workingDirectory: String,
        val args: Array<String>,
        val environment: Array<String>
    )

    companion object {
        private const val TAG = "LinuxEnvironment"
        private const val ROOTFS_URL =
            "https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-arm64.tar.xz"
    }
}

/** El archivo trae todo dentro de una carpeta como "kali-arm64/": se quita ese primer nivel. */
internal fun stripRootFolder(name: String): String? {
    val clean = name.removePrefix("./").trimEnd('/')
    val slash = clean.indexOf('/')
    return if (slash < 0) null else clean.substring(slash + 1).ifEmpty { null }
}

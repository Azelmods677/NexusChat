package com.Azelmods.App.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.Azelmods.App.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.messaging.FirebaseMessaging
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
import org.json.JSONObject
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Una versión publicada más nueva que la instalada. */
data class AppUpdate(
    val versionName: String,
    val notes: String,
    /** Enlace del APK por arquitectura ("arm64-v8a", "armeabi-v7a") y "universal". */
    val apkUrls: Map<String, String>,
    val pageUrl: String,
    /** Obligatoria: no se puede seguir usando la app sin instalarla. */
    val mandatory: Boolean = true
)

sealed interface UpdateDownload {
    data object Idle : UpdateDownload
    data class Running(val progress: Float) : UpdateDownload
    data class Ready(val file: File) : UpdateDownload
    data object Failed : UpdateDownload
}

/**
 * Avisa de las versiones nuevas en cuanto se publican.
 *
 * Una función del servidor vigila las versiones de GitHub, escribe la última en
 * `config/appUpdate` y envía un aviso a todos. Aquí se escucha ese nodo mientras la app está
 * abierta, y al arrancar se pregunta también a GitHub por si el servidor no ha llegado aún.
 */
@Singleton
class AppUpdateManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http by lazy { OkHttpClient() }

    private val _update = MutableStateFlow<AppUpdate?>(null)
    val update: StateFlow<AppUpdate?> = _update.asStateFlow()

    private val _download = MutableStateFlow<UpdateDownload>(UpdateDownload.Idle)
    val download: StateFlow<UpdateDownload> = _download.asStateFlow()

    private var listening = false

    fun start() {
        FirebaseMessaging.getInstance().subscribeToTopic(UPDATES_TOPIC)
        scope.launch { runCatching { offer(fetchLatestFromGitHub()) }.onFailure { Log.w(TAG, "GitHub: ${it.message}") } }
        // El nodo de configuración solo se puede leer con la sesión iniciada.
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (auth.currentUser != null && !listening) {
                listening = true
                listenToServer()
            }
        }
    }

    private fun listenToServer() {
        FirebaseDatabase.getInstance().getReference(CONFIG_PATH)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val version = snapshot.child("versionName").getValue(String::class.java) ?: return
                    offer(
                        AppUpdate(
                            versionName = version,
                            notes = snapshot.child("notes").getValue(String::class.java).orEmpty(),
                            apkUrls = snapshot.child("assets").children.mapNotNull { asset ->
                                val url = asset.getValue(String::class.java) ?: return@mapNotNull null
                                asset.key?.let { it to url }
                            }.toMap(),
                            pageUrl = snapshot.child("pageUrl").getValue(String::class.java) ?: RELEASES_PAGE,
                            mandatory = snapshot.child("mandatory").getValue(Boolean::class.java) ?: true
                        )
                    )
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "No se pudo escuchar las versiones: ${error.message}")
                    listening = false
                }
            })
    }

    private fun fetchLatestFromGitHub(): AppUpdate {
        val request = Request.Builder()
            .url(LATEST_RELEASE_API)
            .header("Accept", "application/vnd.github+json")
            .build()
        http.newCall(request).execute().use { response ->
            check(response.isSuccessful) { "HTTP ${response.code}" }
            val release = JSONObject(response.body!!.string())
            val assets = release.getJSONArray("assets")
            val urls = buildMap {
                for (i in 0 until assets.length()) {
                    val asset = assets.getJSONObject(i)
                    val name = asset.getString("name")
                    if (name.endsWith(".apk")) put(abiOf(name), asset.getString("browser_download_url"))
                }
            }
            return AppUpdate(
                versionName = release.getString("tag_name").removePrefix("v"),
                notes = release.optString("body"),
                apkUrls = urls,
                pageUrl = release.optString("html_url", RELEASES_PAGE),
                mandatory = isMandatoryRelease(release.optString("body"))
            )
        }
    }

    private fun offer(candidate: AppUpdate) {
        if (isNewerVersion(candidate.versionName, BuildConfig.VERSION_NAME)) _update.value = candidate
    }

    /** El APK que le corresponde a este teléfono, si está publicado. */
    fun apkUrlFor(update: AppUpdate): String? {
        val abi = Build.SUPPORTED_ABIS.firstOrNull { update.apkUrls.containsKey(it) }
        return (update.apkUrls[abi] ?: update.apkUrls["universal"])?.takeIf { it.startsWith(TRUSTED_DOWNLOADS) }
    }

    fun startDownload(update: AppUpdate) {
        val url = apkUrlFor(update) ?: run {
            _download.value = UpdateDownload.Failed
            return
        }
        if (_download.value is UpdateDownload.Running) return
        _download.value = UpdateDownload.Running(0f)
        scope.launch {
            runCatching {
                val folder = File(context.cacheDir, "updates").apply { mkdirs() }
                folder.listFiles()?.forEach { it.delete() }
                val target = File(folder, "nexuschat-${update.versionName}.apk")
                http.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    check(response.isSuccessful) { "HTTP ${response.code}" }
                    val body = response.body!!
                    val total = body.contentLength().coerceAtLeast(1L)
                    var done = 0L
                    body.byteStream().use { input ->
                        target.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                done += read
                                _download.value = UpdateDownload.Running((done.toFloat() / total).coerceAtMost(1f))
                            }
                        }
                    }
                }
                target
            }.onSuccess { _download.value = UpdateDownload.Ready(it) }
                .onFailure {
                    Log.w(TAG, "Descarga fallida: ${it.message}")
                    _download.value = UpdateDownload.Failed
                }
        }
    }

    /** Android pide confirmar y comprueba que el APK esté firmado con la misma llave. */
    fun install(file: File) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    companion object {
        private const val TAG = "AppUpdate"
        const val UPDATES_TOPIC = "app_updates"
        private const val CONFIG_PATH = "config/appUpdate"
        private const val RELEASES_PAGE = "https://github.com/Azelmods677/NexusChat/releases/latest"
        private const val LATEST_RELEASE_API = "https://api.github.com/repos/Azelmods677/NexusChat/releases/latest"
        private const val TRUSTED_DOWNLOADS = "https://github.com/Azelmods677/NexusChat/releases/download/"
    }
}

/** Toda versión es obligatoria salvo que sus notas digan [opcional]. */
internal fun isMandatoryRelease(notes: String): Boolean = !notes.contains("[opcional]", ignoreCase = true)

/** "app-arm64-v8a-release.apk" → "arm64-v8a"; lo que no indique arquitectura sirve para todas. */
internal fun abiOf(fileName: String): String =
    listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86").firstOrNull { fileName.contains(it) } ?: "universal"

/** Compara "8.1.0" con "8.0.10" número a número: como texto, "8.0.10" saldría menor que "8.0.9". */
internal fun isNewerVersion(remote: String, installed: String): Boolean {
    val a = remote.removePrefix("v").split(".", "-").map { it.toIntOrNull() ?: 0 }
    val b = installed.removePrefix("v").split(".", "-").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(a.size, b.size)) {
        val left = a.getOrElse(i) { 0 }
        val right = b.getOrElse(i) { 0 }
        if (left != right) return left > right
    }
    return false
}

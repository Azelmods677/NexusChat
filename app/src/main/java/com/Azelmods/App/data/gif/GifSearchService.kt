package com.Azelmods.App.data.gif

import android.content.Context
import com.Azelmods.App.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Un GIF de la búsqueda: miniatura ligera para la cuadrícula y archivo para enviar. */
data class GifResult(val id: String, val previewUrl: String, val sendUrl: String, val aspectRatio: Float)

/**
 * Búsqueda de GIF con GIPHY.
 *
 * Tenor cerró su API el 30 de junio de 2026, así que GIPHY es el proveedor disponible.
 * Requiere una clave gratuita (developers.giphy.com). No se incluye ninguna en la app:
 * una clave empaquetada en el APK es de cualquiera que lo abra. Se lee de
 * `local.properties` (GIPHY_API_KEY) o de la que introduzca el usuario.
 *
 * Con el modo anónimo activo estas peticiones pasan por Tor como el resto. Aun así, lo que
 * se busca lo ve GIPHY: la interfaz lo advierte.
 */
@Singleton
class GifSearchService @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("gif_search", Context.MODE_PRIVATE)
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun apiKey(): String =
        prefs.getString(KEY_API_KEY, null)?.takeIf { it.isNotBlank() } ?: BuildConfig.GIPHY_API_KEY

    fun hasApiKey(): Boolean = apiKey().isNotBlank()

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
    }

    suspend fun trending(): List<GifResult> = request("trending", emptyMap())

    suspend fun search(query: String): List<GifResult> =
        if (query.isBlank()) trending() else request("search", mapOf("q" to query.trim()))

    private suspend fun request(endpoint: String, params: Map<String, String>): List<GifResult> =
        withContext(Dispatchers.IO) {
            val url = "https://api.giphy.com/v1/gifs/$endpoint".toHttpUrl().newBuilder()
                .addQueryParameter("api_key", apiKey())
                .addQueryParameter("limit", "30")
                .addQueryParameter("rating", "pg-13")
                .addQueryParameter("bundle", "messaging_non_clips")
                .apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }
                .build()
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (response.code == 401 || response.code == 403) {
                    throw IllegalStateException("La clave de GIPHY no es válida")
                }
                if (!response.isSuccessful) throw IllegalStateException("GIPHY respondió ${response.code}")
                parse(response.body?.string().orEmpty())
            }
        }

    private fun parse(body: String): List<GifResult> {
        val data = JSONObject(body).optJSONArray("data") ?: return emptyList()
        return (0 until data.length()).mapNotNull { i ->
            val item = data.optJSONObject(i) ?: return@mapNotNull null
            val images = item.optJSONObject("images") ?: return@mapNotNull null
            val preview = images.optJSONObject("fixed_width") ?: return@mapNotNull null
            val send = images.optJSONObject("downsized_medium")
                ?: images.optJSONObject("downsized")
                ?: preview
            val width = preview.optString("width").toFloatOrNull() ?: 1f
            val height = preview.optString("height").toFloatOrNull()?.takeIf { it > 0 } ?: 1f
            GifResult(
                id = item.optString("id"),
                previewUrl = preview.optString("url"),
                sendUrl = send.optString("url"),
                aspectRatio = width / height
            )
        }.filter { it.previewUrl.isNotBlank() && it.sendUrl.isNotBlank() }
    }

    private companion object {
        const val KEY_API_KEY = "giphy_api_key"
    }
}

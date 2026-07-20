package com.Azelmods.App.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 🔔 FcmNotificationSender — Envía notificaciones push FCM a otros dispositivos.
 *
 * ## ⚠️ REQUIERE CONFIGURACIÓN
 *
 * ### Opción 1 (recomendada): Firebase Cloud Functions
 * Despliega la función en `functions/` que escucha nuevos mensajes en Realtime Database
 * y envía FCM automáticamente. No requiere guardar la server key en la app.
 *
 * ### Opción 2: Server Key en BuildConfig (alternativa)
 * 1. Ve a Firebase Console → Project Settings → Cloud Messaging
 * 2. Copia la "Server Key" (o "Clave del servidor")
 * 3. Agrégala a `local.properties`:
 *    ```
 *    FCM_SERVER_KEY=AAAA...tu_clave
 *    ```
 * 4. Descomenta la línea en app/build.gradle.kts:
 *    ```
 *    buildConfigField("String", "FCM_SERVER_KEY", "\"${fcmServerKey}\"")
 *    ```
 *
 * ⚠️ La Server Key es sensible. No la subas a GitHub ni la expongas.
 * Usa Firebase Cloud Functions siempre que sea posible.
 */
object FcmNotificationSender {

    private const val TAG = "FcmNotificationSender"
    private const val FCM_API_URL = "https://fcm.googleapis.com/fcm/send"

    /** Debe coincidir con @color/notification_accent (payload FCM viaja como string hex). */
    private const val ACCENT_COLOR_HEX = "#7B5CFA"
    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * Send a push notification to a specific FCM token.
     *
     * @param fcmToken The recipient's FCM device token
     * @param serverKey FCM server key (from Firebase Console)
     * @param title Notification title (sender name)
     * @param body Notification body (message preview)
     * @param dataPayload Additional data to pass to the notification (chatId, senderId, etc.)
     */
    suspend fun sendToToken(
        fcmToken: String,
        serverKey: String,
        title: String,
        body: String,
        dataPayload: Map<String, String> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val dataJson = JSONObject()
            dataPayload.forEach { (key, value) -> dataJson.put(key, value) }

            val notificationJson = JSONObject().apply {
                put("title", title)
                put("body", body)
                put("sound", "default")
                put("badge", "1")
                put("icon", "ic_notification")
                put("color", ACCENT_COLOR_HEX)
            }

            val messageJson = JSONObject().apply {
                put("to", fcmToken)
                put("notification", notificationJson)
                // Data payload — received even when app is in background
                put("data", dataJson)
                put("priority", "high")
            }

            val requestBody = messageJson.toString().toRequestBody(JSON_MEDIA_TYPE)

            val request = Request.Builder()
                .url(FCM_API_URL)
                .addHeader("Authorization", "key=$serverKey")
                .addHeader("Content-Type", "application/json")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                Log.d(TAG, "✅ FCM sent successfully to token: $responseBody")
                true
            } else {
                Log.w(TAG, "❌ FCM failed: ${response.code} - $responseBody")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ FCM error: ${e.message}", e)
            false
        }
    }

    /**
     * Send notification to multiple FCM tokens (batch).
     */
    suspend fun sendToMultipleTokens(
        tokens: List<String>,
        serverKey: String,
        title: String,
        body: String,
        dataPayload: Map<String, String> = emptyMap()
    ): Int = withContext(Dispatchers.IO) {
        var successCount = 0
        for (token in tokens) {
            if (sendToToken(token, serverKey, title, body, dataPayload)) {
                successCount++
            }
        }
        successCount
    }

    /**
     * Build standard data payload for a chat message notification.
     */
    fun buildMessagePayload(
        chatId: String,
        senderId: String,
        senderName: String,
        senderPhotoUrl: String? = null,
        mediaType: String? = null,
        body: String = ""
    ): Map<String, String> {
        val payload = mutableMapOf(
            "type" to "message",
            "chatId" to chatId,
            "senderId" to senderId,
            "senderName" to senderName,
            "body" to body
        )
        if (senderPhotoUrl != null) payload["senderPhotoUrl"] = senderPhotoUrl
        if (mediaType != null) {
            payload["mediaType"] = mediaType
            payload["body"] = when (mediaType) {
                "IMAGE" -> "📷 Photo"
                "VIDEO" -> "🎥 Video"
                "AUDIO" -> "🎤 Voice message"
                "DOCUMENT" -> "📄 Document"
                "LOCATION" -> "📍 Location"
                "STICKER" -> "Sticker"
                else -> body
            }
        }
        return payload
    }
}

package com.Azelmods.App.data.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🌐 TranslationService — automatic message translation.
 *
 * Uses the free MyMemory API (no API key required for basic usage,
 * ~1000 words/day). All network work runs on [Dispatchers.IO].
 */
@Singleton
class TranslationService @Inject constructor() {

    private val baseUrl = "https://api.mymemory.translated.net/get"

    /**
     * Translates [text] into [targetLang]. Use "auto" for [sourceLang] to let
     * the service infer the source language.
     */
    suspend fun translate(
        text: String,
        targetLang: String = "es",
        sourceLang: String = "auto"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) return@withContext Result.failure(Exception("Texto vacío"))
            val encoded = URLEncoder.encode(text.take(500), "UTF-8")
            val langPair = if (sourceLang == "auto") "en|$targetLang"
            else "$sourceLang|$targetLang"
            val url = "$baseUrl?q=$encoded&langpair=$langPair"

            val response = URL(url).readText()
            val json = JSONObject(response)
            
            // Check for API errors (e.g. quota limit)
            val responseStatus = json.optInt("responseStatus", 200)
            if (responseStatus != 200) {
                val errorDetails = json.optString("responseDetails", "Error $responseStatus")
                return@withContext Result.failure(Exception("Error de traducción: $errorDetails"))
            }
            
            val translated = json
                .getJSONObject("responseData")
                .getString("translatedText")

            // Sometimes the API puts the quota warning directly in the translated text with a 200 status
            if (translated.startsWith("MYMEMORY WARNING", ignoreCase = true)) {
                return@withContext Result.failure(Exception("Límite diario de traducciones agotado"))
            }

            if (translated.isBlank()) Result.failure(Exception("Traducción vacía"))
            else Result.success(translated)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Best-effort language detection. Returns a 2-letter code, defaulting to "en".
     */
    suspend fun detectLanguage(text: String): String = withContext(Dispatchers.IO) {
        try {
            val encoded = URLEncoder.encode(text.take(100), "UTF-8")
            val url = "$baseUrl?q=$encoded&langpair=auto|en"
            val response = URL(url).readText()
            val json = JSONObject(response)
            json.getString("responseDetails")
                .substringAfter("TRANSLATED FROM: ", "")
                .substringBefore(";")
                .lowercase()
                .takeIf { it.length == 2 } ?: "en"
        } catch (e: Exception) {
            "en"
        }
    }

    companion object {
        val SUPPORTED_LANGUAGES = mapOf(
            "Español" to "es",
            "English" to "en",
            "Français" to "fr",
            "Deutsch" to "de",
            "Português" to "pt",
            "Italiano" to "it",
            "日本語" to "ja",
            "中文" to "zh",
            "한국어" to "ko",
            "Русский" to "ru",
            "العربية" to "ar"
        )
    }
}

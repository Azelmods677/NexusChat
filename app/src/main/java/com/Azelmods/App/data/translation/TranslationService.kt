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
     * 
     * MULTI-LANGUAGE FIX: Mejora la detección y soporte para todos los idiomas soportados.
     */
    suspend fun translate(
        text: String,
        targetLang: String = "es",
        sourceLang: String = "auto"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) return@withContext Result.failure(Exception("Texto vacío"))
            
            // Normalizar códigos de idioma
            val normalizedTarget = normalizeLanguageCode(targetLang)
            val normalizedSource = if (sourceLang == "auto" || sourceLang.isBlank()) "autodetect" else normalizeLanguageCode(sourceLang)
            
            android.util.Log.d("TranslationService", "🌐 Translating to: $normalizedTarget (from: $normalizedSource)")
            
            val encoded = URLEncoder.encode(text.take(500), "UTF-8")
            
            val detectedSource = normalizedSource
            
            android.util.Log.d("TranslationService", "🌐 Source language: $detectedSource")
            
            // Si el texto ya está en el idioma objetivo explícitamente, no traducir
            if (detectedSource != "autodetect" && detectedSource.equals(normalizedTarget, ignoreCase = true)) {
                android.util.Log.d("TranslationService", "🌐 Text already in target language")
                return@withContext Result.success(text)
            }
            
            // MyMemory usa "source|target" para pares explícitos
            val langPair = "$detectedSource|$normalizedTarget"
            val url = "$baseUrl?q=$encoded&langpair=$langPair"
            
            android.util.Log.d("TranslationService", "🌐 Requesting: $url")

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

            android.util.Log.d("TranslationService", "🌐 Success: '$text' -> '$translated'")

            // Sometimes the API puts the quota warning directly in the translated text with a 200 status
            if (translated.startsWith("MYMEMORY WARNING", ignoreCase = true) || 
                translated.contains("LIMIT EXCEEDED", ignoreCase = true)) {
                return@withContext Result.failure(Exception("Límite diario de traducciones agotado. Intenta de nuevo mañana."))
            }

            // Verificar que la traducción es válida
            if (translated.isBlank()) {
                return@withContext Result.failure(Exception("Traducción vacía recibida"))
            }
            
            // Si la traducción es idéntica, puede que ya esté en el idioma correcto
            if (translated.equals(text, ignoreCase = true)) {
                android.util.Log.d("TranslationService", "🌐 Translation identical to original - text may already be in target language")
                return@withContext Result.success(text)
            }

            Result.success(translated)
        } catch (e: java.net.UnknownHostException) {
            android.util.Log.e("TranslationService", "🌐 No internet connection", e)
            Result.failure(Exception("Sin conexión a internet. Verifica tu conexión."))
        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.e("TranslationService", "🌐 Connection timeout", e)
            Result.failure(Exception("Tiempo de espera agotado. Intenta de nuevo."))
        } catch (e: Exception) {
            android.util.Log.e("TranslationService", "🌐 Translation failed: ${e.message}", e)
            Result.failure(Exception("Error de traducción: ${e.message ?: "Error desconocido"}"))
        }
    }
    
    /**
     * Normaliza códigos de idioma a formato de 2 letras
     */
    private fun normalizeLanguageCode(code: String): String {
        return when (code.lowercase()) {
            "es", "spa", "spanish", "español" -> "es"
            "en", "eng", "english" -> "en"
            "fr", "fra", "french", "français" -> "fr"
            "de", "deu", "ger", "german", "deutsch" -> "de"
            "pt", "por", "portuguese", "português" -> "pt"
            "it", "ita", "italian", "italiano" -> "it"
            "ja", "jpn", "japanese", "日本語" -> "ja"
            "zh", "chi", "chinese", "中文" -> "zh"
            "ko", "kor", "korean", "한국어" -> "ko"
            "ru", "rus", "russian", "русский" -> "ru"
            "ar", "ara", "arabic", "العربية" -> "ar"
            else -> code.take(2).lowercase()
        }
    }

    /**
     * Best-effort language detection. Returns a 2-letter code, defaulting to "en".
     * IMPROVED: Better detection and normalization
     */
    suspend fun detectLanguage(text: String): String = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) return@withContext "en"
            
            val encoded = URLEncoder.encode(text.take(200), "UTF-8")
            val url = "$baseUrl?q=$encoded&langpair=auto|en"
            
            val response = URL(url).readText()
            val json = JSONObject(response)
            
            // Intentar obtener el idioma de responseDetails
            val details = json.optString("responseDetails", "")
            val detected = details
                .substringAfter("TRANSLATED FROM: ", "")
                .substringBefore(";", "")
                .trim()
                .lowercase()
            
            android.util.Log.d("TranslationService", "🌐 Detected language from API: $detected")
            
            // Normalizar el código detectado
            val normalized = normalizeLanguageCode(detected)
            
            // Si no se detectó nada válido, intentar heurística simple
            if (normalized.length != 2) {
                return@withContext detectLanguageHeuristic(text)
            }
            
            return@withContext normalized
        } catch (e: Exception) {
            android.util.Log.e("TranslationService", "🌐 Language detection failed, using heuristic", e)
            return@withContext detectLanguageHeuristic(text)
        }
    }
    
    /**
     * Detección heurística simple basada en caracteres
     */
    private fun detectLanguageHeuristic(text: String): String {
        val sample = text.take(100).lowercase()
        
        return when {
            // Detectar caracteres específicos de idiomas
            sample.any { it in '가'..'힣' } -> "ko" // Coreano
            sample.any { it in '\u4E00'..'\u9FFF' } -> "zh" // Chino
            sample.any { it in '\u3040'..'\u309F' || it in '\u30A0'..'\u30FF' } -> "ja" // Japonés
            sample.any { it in '\u0400'..'\u04FF' } -> "ru" // Cirílico (Ruso)
            sample.any { it in '\u0600'..'\u06FF' } -> "ar" // Árabe
            
            // Palabras comunes en diferentes idiomas
            sample.contains(Regex("\\b(el|la|los|las|de|que|es|en|un|una)\\b")) -> "es" // Español
            sample.contains(Regex("\\b(the|is|are|of|to|and|a|in|that|have)\\b")) -> "en" // Inglés
            sample.contains(Regex("\\b(le|la|les|de|que|est|et|un|une|dans)\\b")) -> "fr" // Francés
            sample.contains(Regex("\\b(der|die|das|ist|und|von|zu|den|dem)\\b")) -> "de" // Alemán
            sample.contains(Regex("\\b(o|a|os|as|de|que|é|e|em|um|uma)\\b")) -> "pt" // Portugués
            sample.contains(Regex("\\b(il|lo|la|di|che|è|e|un|una|per)\\b")) -> "it" // Italiano
            
            else -> "en" // Default a inglés
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

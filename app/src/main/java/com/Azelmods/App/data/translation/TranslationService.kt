package com.Azelmods.App.data.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resultado de una traducción exitosa.
 *
 * @property text          El texto traducido.
 * @property wasTruncated  true si el original superaba [TranslationService.MAX_CHARS]
 *                         y solo se tradujo el prefijo. Antes esto pasaba en silencio
 *                         y el usuario no sabía que le faltaba parte del mensaje.
 * @property remainingWords Palabras estimadas restantes de la cuota diaria local.
 */
data class Translation(
    val text: String,
    val wasTruncated: Boolean,
    val remainingWords: Int
)

/**
 * 🌐 TranslationService — traducción de mensajes.
 *
 * ## Dos motores, en este orden
 *
 * 1. **El modelo de IA del usuario**, si tiene uno configurado. MyMemory es una
 *    *memoria de traducción* alimentada por aportaciones de la comunidad: con
 *    frases completas y formales va bien, pero con lo que se escribe en un chat
 *    —mensajes de tres palabras, jerga, emoji, sin contexto— devuelve la
 *    coincidencia más parecida que tenga guardada, que a menudo no significa lo
 *    mismo. De ahí que "traduzca mal". Un modelo de lenguaje entiende el
 *    registro y acierta mucho más.
 * 2. **MyMemory** como respaldo gratuito cuando no hay clave de IA o el
 *    proveedor falla, para que la función nunca deje de existir.
 *
 * Todo el trabajo de red va en [Dispatchers.IO].
 */
@Singleton
class TranslationService @Inject constructor(
    private val quotaTracker: TranslationQuotaTracker,
    private val aiAssistService: com.Azelmods.App.data.ai.AiAssistService
) {

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
    ): Result<Translation> = withContext(Dispatchers.IO) {
        try {
            if (text.isBlank()) {
                android.util.Log.w("TranslationService", "🌐 Empty text provided")
                return@withContext Result.failure(Exception("Texto vacío"))
            }

            // Normalizar códigos de idioma
            val normalizedTarget = normalizeLanguageCode(targetLang)

            // ── Motor preferente: el modelo del usuario ──
            // No consume la cuota de MyMemory y respeta jerga, emoji y tono.
            if (aiAssistService.isAvailable()) {
                val languageName = languageName(normalizedTarget)
                val aiResult = aiAssistService.translate(text, languageName)
                aiResult.getOrNull()?.takeIf { it.isNotBlank() }?.let { translated ->
                    android.util.Log.d("TranslationService", "🌐 Traducido con el modelo del usuario")
                    return@withContext Result.success(
                        Translation(
                            text = translated,
                            wasTruncated = false,
                            remainingWords = quotaTracker.remainingWordsToday()
                        )
                    )
                }
                android.util.Log.w(
                    "TranslationService",
                    "🌐 La IA no pudo traducir (${aiResult.exceptionOrNull()?.message}); se usa MyMemory"
                )
            }

            // CAUSA RAÍZ (fix): MyMemory NO soporta "auto" como idioma origen. Con
            // langpair="auto|es" devuelve 403 ("'AUTO' IS AN INVALID SOURCE LANGUAGE")
            // y la traducción fallaba SIEMPRE, porque ChatViewModel siempre llama con
            // sourceLang="auto". Detectamos el idioma localmente y enviamos un código
            // ISO de 2 letras válido en su lugar.
            val wasAutoSource = sourceLang == "auto" || sourceLang.isBlank()
            val normalizedSource = if (wasAutoSource) {
                detectLanguageHeuristic(text)
            } else {
                normalizeLanguageCode(sourceLang)
            }

            android.util.Log.d("TranslationService", "🌐 Translating text: '${text.take(50)}...' to: $normalizedTarget (from: $normalizedSource${if (wasAutoSource) " auto-detected" else ""})")

            // Limitar a MAX_CHARS para la API gratuita. El flag viaja en el resultado
            // para que la UI avise en lugar de truncar en silencio.
            val wasTruncated = text.length > MAX_CHARS
            val textToTranslate = text.take(MAX_CHARS)
            val encoded = URLEncoder.encode(textToTranslate, "UTF-8")

            // Solo cortocircuitamos si el ORIGEN fue explícito y coincide con el destino.
            // Si el origen fue auto-detectado (la heurística puede equivocarse), dejamos
            // que la API traduzca en vez de asumir que ya está en el idioma correcto.
            if (!wasAutoSource && normalizedSource.equals(normalizedTarget, ignoreCase = true)) {
                android.util.Log.d("TranslationService", "🌐 Text already in target language")
                return@withContext Result.success(
                    Translation(text, wasTruncated = false, remainingWords = quotaTracker.remainingWordsToday())
                )
            }
            
            // MyMemory usa "source|target" para pares explícitos
            // Cuando source es "auto", MyMemory detecta automáticamente el idioma
            val langPair = "$normalizedSource|$normalizedTarget"
            val url = "$baseUrl?q=$encoded&langpair=$langPair"
            
            android.util.Log.d("TranslationService", "🌐 Requesting: $url")

            try {
                val response = httpGet(url)
                android.util.Log.d("TranslationService", "🌐 API Response: ${response.take(200)}")
                
                val json = JSONObject(response)
                
                // Check for API errors (e.g. quota limit)
                val responseStatus = json.optInt("responseStatus", 200)
                if (responseStatus != 200) {
                    val errorDetails = json.optString("responseDetails", "Error $responseStatus")
                    android.util.Log.e("TranslationService", "🌐 API returned error status: $responseStatus - $errorDetails")
                    return@withContext Result.failure(Exception("Error de traducción: $errorDetails"))
                }
                
                val responseData = json.optJSONObject("responseData")
                if (responseData == null) {
                    android.util.Log.e("TranslationService", "🌐 No responseData in JSON: $response")
                    return@withContext Result.failure(Exception("Respuesta inválida del servicio de traducción"))
                }
                
                val translated = decodeHtmlEntities(responseData.optString("translatedText", ""))
                
                if (translated.isBlank()) {
                    android.util.Log.e("TranslationService", "🌐 Blank translation received")
                    return@withContext Result.failure(Exception("Traducción vacía recibida"))
                }

                android.util.Log.d("TranslationService", "🌐 Success: '$textToTranslate' -> '$translated'")

                // Sometimes the API puts the quota warning directly in the translated text with a 200 status
                if (translated.startsWith("MYMEMORY WARNING", ignoreCase = true) ||
                    translated.contains("LIMIT EXCEEDED", ignoreCase = true)) {
                    android.util.Log.w("TranslationService", "🌐 Quota limit reached")
                    return@withContext Result.failure(Exception("Límite diario de traducciones agotado. Intenta de nuevo mañana."))
                }

                // La request consumió cuota real: registrar las palabras enviadas
                // en el contador local para poder avisar ANTES de que la API falle.
                val remainingWords = quotaTracker.recordUsage(textToTranslate)

                // Si la traducción es idéntica, puede que ya esté en el idioma correcto
                if (translated.equals(textToTranslate, ignoreCase = true)) {
                    android.util.Log.d("TranslationService", "🌐 Translation identical to original - text may already be in target language")
                    return@withContext Result.success(Translation(text, wasTruncated = false, remainingWords = remainingWords))
                }

                return@withContext Result.success(Translation(translated, wasTruncated = wasTruncated, remainingWords = remainingWords))
            } catch (e: org.json.JSONException) {
                android.util.Log.e("TranslationService", "🌐 JSON parsing error", e)
                return@withContext Result.failure(Exception("Error al procesar respuesta del traductor"))
            }
        } catch (e: java.net.UnknownHostException) {
            android.util.Log.e("TranslationService", "🌐 No internet connection", e)
            return@withContext Result.failure(Exception("Sin conexión a internet. Verifica tu conexión."))
        } catch (e: java.net.SocketTimeoutException) {
            android.util.Log.e("TranslationService", "🌐 Connection timeout", e)
            return@withContext Result.failure(Exception("Tiempo de espera agotado. Intenta de nuevo."))
        } catch (e: java.io.IOException) {
            android.util.Log.e("TranslationService", "🌐 Network I/O error", e)
            return@withContext Result.failure(Exception("Error de red: ${e.message ?: "Error desconocido"}"))
        } catch (e: Exception) {
            android.util.Log.e("TranslationService", "🌐 Translation failed: ${e.message}", e)
            return@withContext Result.failure(Exception("Error de traducción: ${e.message ?: "Error desconocido"}"))
        }
    }
    
    /**
     * GET con timeouts EXPLÍCITOS.
     *
     * CAUSA RAÍZ (fix): antes se usaba `URL(url).readText()`, que abre una
     * `URLConnection` con timeout por defecto = 0, es decir INFINITO. Si MyMemory
     * respondía lento o el DNS por Tor no resolvía, la corrutina se quedaba
     * bloqueada para siempre y el indicador de "traduciendo…" no se apagaba nunca.
     * Ese era el "las traducciones cargan infinitamente".
     */
    private fun httpGet(urlString: String): String {
        val conn = (URL(urlString).openConnection() as java.net.HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000   // 10s para establecer conexión
            readTimeout = 15_000      // 15s para leer la respuesta
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "NexusChat/6")
        }
        return try {
            val stream = if (conn.responseCode in 200..299) conn.inputStream
                         else conn.errorStream ?: conn.inputStream
            stream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
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

            val response = httpGet(url)
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
     * Detección heurística por puntuación.
     *
     * La versión anterior devolvía el PRIMER idioma cuya lista de palabras
     * comunes casara, y el español estaba el primero. Como "de", "que", "la" y
     * "un" son palabras corrientes también en francés, portugués e italiano,
     * cualquier texto en esas lenguas se detectaba como español y se acababa
     * pidiendo a la API un `langpair=es|es`, que devuelve el original o una
     * coincidencia sin sentido. Ésa era una de las causas de "traduce mal".
     *
     * Ahora se cuentan las coincidencias de TODOS los idiomas y gana el que más
     * saque; sólo se cae al inglés si ninguno puntúa.
     */
    private fun detectLanguageHeuristic(text: String): String {
        val sample = text.take(200).lowercase()

        // Los alfabetos no latinos son inequívocos: no hace falta puntuar.
        when {
            sample.any { it in '가'..'힣' } -> return "ko"
            sample.any { it in '぀'..'ゟ' || it in '゠'..'ヿ' } -> return "ja"
            sample.any { it in '一'..'鿿' } -> return "zh"
            sample.any { it in 'Ѐ'..'ӿ' } -> return "ru"
            sample.any { it in '؀'..'ۿ' } -> return "ar"
        }

        val stopWords = mapOf(
            "es" to listOf("el", "la", "los", "las", "que", "es", "un", "una", "por", "con", "para", "pero", "como", "esta", "muy", "esto", "hola"),
            "en" to listOf("the", "is", "are", "of", "to", "and", "in", "that", "have", "for", "with", "you", "this", "was", "it"),
            "fr" to listOf("le", "les", "est", "et", "une", "dans", "pour", "pas", "vous", "avec", "sur", "ce", "qui", "bonjour"),
            "de" to listOf("der", "die", "das", "ist", "und", "von", "zu", "den", "dem", "nicht", "mit", "auf", "ein", "eine"),
            "pt" to listOf("os", "as", "nao", "não", "em", "uma", "para", "com", "mas", "isso", "voce", "você", "esta", "muito", "ola", "olá"),
            "it" to listOf("il", "lo", "gli", "di", "che", "una", "per", "non", "sono", "con", "questo", "anche", "ciao")
        )

        // Se compara palabra a palabra: con `contains` el "de" de "desde" contaba
        // como preposición suelta e inflaba la puntuación del español.
        val words = sample.split(Regex("[^\\p{L}]+")).filter { it.isNotBlank() }.toSet()

        val best = stopWords
            .mapValues { (_, list) -> list.count { it in words } }
            .filterValues { it > 0 }
            .maxByOrNull { it.value }

        return best?.key ?: "en"
    }

    /** Nombre del idioma para el prompt de la IA ("es" -> "Español"). */
    private fun languageName(code: String): String =
        SUPPORTED_LANGUAGES.entries.firstOrNull { it.value == code }?.key ?: code

    /**
     * MyMemory devuelve el texto con entidades HTML (&#39;, &quot;, &amp;).
     * Sin decodificarlas, una traducción con un apóstrofo llegaba a pantalla como
     * "don&#39;t" y parecía un fallo del traductor.
     */
    private fun decodeHtmlEntities(raw: String): String {
        var out = raw
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
        out = Regex("&#(\\d{2,5});").replace(out) { match ->
            match.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: match.value
        }
        return out
    }

    companion object {
        /** Máximo de caracteres por request de la API gratuita de MyMemory. */
        const val MAX_CHARS = 500

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

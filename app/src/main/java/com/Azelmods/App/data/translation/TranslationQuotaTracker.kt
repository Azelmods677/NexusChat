package com.Azelmods.App.data.translation

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 📊 Contador LOCAL de cuota de traducción.
 *
 * La API gratuita de MyMemory permite ~1000 palabras/día por IP, pero no expone
 * ningún endpoint para consultar cuánto queda: el usuario solo se enteraba del
 * límite cuando la request ya había fallado. Este tracker estima el consumo
 * contando las palabras enviadas desde este dispositivo y lo persiste en
 * SharedPreferences, con reset automático al cambiar el día.
 *
 * Es una ESTIMACIÓN local (otra app/dispositivo detrás de la misma IP también
 * consume cuota), por eso se usa para avisar preventivamente y no para bloquear.
 */
@Singleton
class TranslationQuotaTracker @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _remainingWords = MutableStateFlow(remainingWordsToday())
    /** Palabras estimadas restantes hoy, observable desde la UI. */
    val remainingWords: StateFlow<Int> = _remainingWords.asStateFlow()

    /**
     * Registra las palabras de un texto enviado a traducir y devuelve
     * las palabras estimadas restantes tras el consumo.
     */
    fun recordUsage(text: String): Int {
        val words = countWords(text)
        val usedToday = wordsUsedToday() + words
        prefs.edit()
            .putString(KEY_DATE, today())
            .putInt(KEY_WORDS_USED, usedToday)
            .apply()
        val remaining = (DAILY_WORD_LIMIT - usedToday).coerceAtLeast(0)
        _remainingWords.value = remaining
        return remaining
    }

    /** Palabras consumidas hoy según el registro local (0 si cambió el día). */
    fun wordsUsedToday(): Int {
        val storedDate = prefs.getString(KEY_DATE, null)
        return if (storedDate == today()) prefs.getInt(KEY_WORDS_USED, 0) else 0
    }

    /** Palabras estimadas restantes hoy. */
    fun remainingWordsToday(): Int =
        (DAILY_WORD_LIMIT - wordsUsedToday()).coerceAtLeast(0)

    private fun countWords(text: String): Int =
        text.trim().split(WHITESPACE).count { it.isNotBlank() }

    private fun today(): String = LocalDate.now().toString()

    companion object {
        /** Límite diario aproximado de la API gratuita de MyMemory. */
        const val DAILY_WORD_LIMIT = 1000

        private const val PREFS_NAME = "translation_quota"
        private const val KEY_DATE = "quota_date"
        private const val KEY_WORDS_USED = "words_used"
        private val WHITESPACE = Regex("\\s+")
    }
}

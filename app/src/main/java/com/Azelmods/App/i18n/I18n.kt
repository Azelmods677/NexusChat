package com.Azelmods.App.i18n

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import org.json.JSONObject
import java.util.Locale

/** Idioma de la interfaz. Con [SYSTEM] se sigue el del teléfono. */
enum class AppLanguage(val tag: String?, val label: String) {
    SYSTEM(null, "Igual que el teléfono"),
    SPANISH("es", "Español"),
    ENGLISH("en", "English")
}

/**
 * Traducción de la interfaz. El texto en español del código es la clave, así que sigue siendo
 * legible, y funciona igual en una pantalla que en un ViewModel. La tabla inglesa está en
 * `assets/i18n/en.json`; lo que no aparezca en ella se muestra en español en vez de fallar.
 *
 * Cambiar de idioma recrea la pantalla, y con ello se vuelve a pedir cada texto.
 */
object I18n {
    private const val TAG = "I18n"
    private const val PREFS = "app_language"
    private const val KEY_LANGUAGE = "language"
    private const val ENGLISH_TABLE = "i18n/en.json"

    @Volatile
    private var table: Map<String, String> = emptyMap()

    @Volatile
    private var locale: Locale = Locale.forLanguageTag("es")

    /** Debe llamarse antes de pintar nada: Application.attachBaseContext. */
    fun init(context: Context) {
        val systemLanguage = Resources.getSystem().configuration.locales[0]
        val resolved = when (selected(context)) {
            AppLanguage.SPANISH -> Locale.forLanguageTag("es")
            AppLanguage.ENGLISH -> Locale.forLanguageTag("en")
            AppLanguage.SYSTEM -> if (systemLanguage.language == "es") systemLanguage else Locale.forLanguageTag("en")
        }
        locale = resolved
        Locale.setDefault(resolved)
        table = if (resolved.language == "en") loadEnglish(context) else emptyMap()
    }

    fun selected(context: Context): AppLanguage {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_LANGUAGE, null)
        return AppLanguage.entries.firstOrNull { it.name == stored } ?: AppLanguage.SYSTEM
    }

    fun select(activity: Activity, language: AppLanguage) {
        activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_LANGUAGE, language.name)
            .commit()
        // Hay textos que se fijan una vez al cargar su clase (catálogos, listas de ajustes):
        // recrear la pantalla los dejaría en el idioma anterior, así que se empieza de cero.
        val restart = activity.packageManager.getLaunchIntentForPackage(activity.packageName)
        if (restart == null) {
            init(activity.applicationContext)
            activity.recreate()
            return
        }
        restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        activity.startActivity(restart)
        Runtime.getRuntime().exit(0)
    }

    /** Para que Android y las fechas usen también el idioma elegido en cada pantalla. */
    fun wrap(base: Context): Context {
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(locale)
        return base.createConfigurationContext(configuration)
    }

    fun translate(text: String): String = table[text] ?: text

    private fun loadEnglish(context: Context): Map<String, String> = runCatching {
        val json = context.assets.open(ENGLISH_TABLE).bufferedReader().use { it.readText() }
        val source = JSONObject(json)
        buildMap(source.length()) {
            source.keys().forEach { key -> put(key, source.getString(key)) }
        }
    }.onFailure { Log.e(TAG, "No se pudo cargar la tabla de inglés", it) }.getOrDefault(emptyMap())
}

/** Texto en el idioma de la app. */
fun tr(text: String): String = I18n.translate(text)

/** Igual, con valores: `tr("Hola %s", nombre)`. */
fun tr(format: String, vararg args: Any?): String =
    runCatching { String.format(Locale.getDefault(), I18n.translate(format), *args) }.getOrDefault(format)

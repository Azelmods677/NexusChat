package com.Azelmods.App.data.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.database.FirebaseDatabase

/**
 * Arranque seguro ("safe boot") para la persistencia offline de Firebase RTDB.
 *
 * ## Problema que resuelve
 *
 * La persistencia offline de Firebase se guarda en una base SQLite interna
 * (`SqlPersistenceStorageEngine`). Si una escritura pendiente queda "envenenada"
 * o el archivo se corrompe, Firebase la vuelve a reproducir en CADA arranque en
 * su propio hilo de trabajo y lanza una `SQLiteException` que NO se puede capturar
 * con try/catch desde la app (corre en un hilo interno). Resultado: la app
 * crashea al entrar (y al hacer el primer write de login/registro) en un bucle.
 *
 * ## Cómo funciona el guard
 *
 * - Antes de activar la persistencia se marca un flag [KEY_PENDING] = true.
 * - Cuando la app arranca de forma sana, [markHealthy] pone el flag en false.
 * - Si en el siguiente arranque el flag SIGUE en true, significa que el arranque
 *   anterior murió con la persistencia activa: entramos en MODO SEGURO, NO se
 *   activa la persistencia esta vez y se purgan las escrituras pendientes
 *   envenenadas ([FirebaseDatabase.purgeOutstandingWrites]). Así la app siempre
 *   puede arrancar y se auto-recupera del bucle de crashes.
 *
 * La caché offline de chats sigue cubierta por Room (CacheManager / PendingMessageDao),
 * por lo que entrar en modo seguro no deja al usuario sin historial local.
 */
object FirebasePersistenceGuard {

    private const val TAG = "FirebasePersistence"
    private const val PREFS = "fb_persistence_guard"
    private const val KEY_PENDING = "init_pending"

    /** Tamaño máximo de la caché SQLite de persistencia (20 MB). */
    private const val CACHE_SIZE_BYTES = 20L * 1024 * 1024

    @Volatile
    var safeModeActive: Boolean = false
        private set

    /**
     * Activa la persistencia offline de forma segura. Debe llamarse UNA sola vez,
     * antes de cualquier otro uso de [FirebaseDatabase] (por eso se invoca desde el
     * provider de Hilt, que es el primer punto que construye la instancia).
     */
    fun enable(context: Context, database: FirebaseDatabase) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lastLaunchCrashed = prefs.getBoolean(KEY_PENDING, false)

        if (lastLaunchCrashed) {
            // El arranque anterior murió con la persistencia activa → modo seguro.
            safeModeActive = true
            prefs.edit().putBoolean(KEY_PENDING, false).apply()
            try {
                // Descarta escrituras pendientes que podrían estar envenenadas y
                // volver a crashear el motor de persistencia al reproducirse.
                database.purgeOutstandingWrites()
            } catch (e: Exception) {
                Log.e(TAG, "No se pudieron purgar las escrituras pendientes", e)
            }
            Log.w(TAG, "⚠️ Persistencia Firebase en MODO SEGURO (el arranque previo crasheó). Se omite setPersistenceEnabled esta vez.")
            return
        }

        // Marca que vamos a activar la persistencia; se limpia con markHealthy()
        // cuando la app confirma que arrancó bien.
        prefs.edit().putBoolean(KEY_PENDING, true).apply()

        try {
            database.setPersistenceEnabled(true)
            database.setPersistenceCacheSizeBytes(CACHE_SIZE_BYTES)
            Log.d(TAG, "✅ Persistencia offline de Firebase activada (cache 20 MB)")
        } catch (e: Exception) {
            // setPersistenceEnabled lanza si la base ya se usó; lo registramos y
            // seguimos sin caché en lugar de un crash duro en el arranque.
            Log.e(TAG, "No se pudo activar la persistencia (¿uso previo de FirebaseDatabase?)", e)
        }
    }

    /**
     * Marca el arranque como sano. Se llama desde la UI una vez que la app superó
     * la ventana de arranque sin crashear, de modo que el próximo inicio NO entre
     * en modo seguro.
     */
    fun markHealthy(context: Context) {
        try {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_PENDING, false).apply()
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo marcar el arranque como sano", e)
        }
    }
}

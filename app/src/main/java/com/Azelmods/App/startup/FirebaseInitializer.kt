package com.Azelmods.App.startup

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import com.google.firebase.FirebaseApp

/**
 * FirebaseInitializer - App Startup Initializer for Firebase.
 *
 * Integrates Firebase initialization into the AndroidX App Startup framework,
 * giving explicit control over initialization order and timing.
 *
 * To enable lazy/manual initialization (disable automatic Firebase ContentProvider),
 * add the following to AndroidManifest.xml inside <application>:
 *
 *   <provider
 *       android:name="com.google.firebase.provider.FirebaseInitProvider"
 *       android:authorities="${applicationId}.firebaseinitprovider"
 *       android:exported="false"
 *       tools:node="remove" />
 *
 * Then register this initializer via App Startup in AndroidManifest.xml:
 *
 *   <provider
 *       android:name="androidx.startup.InitializationProvider"
 *       android:authorities="${applicationId}.androidx-startup"
 *       android:exported="false"
 *       tools:node="merge">
 *       <meta-data
 *           android:name="com.Azelmods.App.startup.FirebaseInitializer"
 *           android:value="androidx.startup" />
 *   </provider>
 */
class FirebaseInitializer : Initializer<FirebaseApp> {

    companion object {
        private const val TAG = "FirebaseInitializer"
    }

    override fun create(context: Context): FirebaseApp {
        return try {
            // initializeApp returns null if Firebase is already initialized,
            // so we fall back to getInstance() in that case.
            val app = FirebaseApp.initializeApp(context)
                ?: FirebaseApp.getInstance()
            Log.d(TAG, "Firebase initialized successfully: ${app.name}")
            app
        } catch (e: IllegalStateException) {
            // FirebaseApp already initialized — retrieve existing instance.
            Log.d(TAG, "Firebase was already initialized, retrieving existing instance.")
            FirebaseApp.getInstance()
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Firebase initialization", e)
            throw e
        }
    }

    /**
     * No dependencies required before Firebase initializes.
     * Add other Initializer classes here if Firebase must run after them.
     */
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

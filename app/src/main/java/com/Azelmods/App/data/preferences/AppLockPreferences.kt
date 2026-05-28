package com.Azelmods.App.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appLockDataStore: DataStore<Preferences> by preferencesDataStore("app_lock_prefs")

@Singleton
class AppLockPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.appLockDataStore

    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("lock_enabled")
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        private val KEY_AUTO_LOCK_MIN = intPreferencesKey("auto_lock_minutes")
    }

    val isLockEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_ENABLED] ?: false }
    val autoLockMinutes: Flow<Int> = dataStore.data.map { it[KEY_AUTO_LOCK_MIN] ?: 5 }
    val isBiometricEnabled: Flow<Boolean> = dataStore.data.map { it[KEY_BIOMETRIC] ?: false }

    suspend fun setLockEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setAutoLockMinutes(minutes: Int) {
        dataStore.edit { it[KEY_AUTO_LOCK_MIN] = minutes }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BIOMETRIC] = enabled }
    }

    suspend fun setPin(pin: String) {
        val hash = hashPin(pin)
        dataStore.edit { it[KEY_PIN_HASH] = hash }
    }

    suspend fun clearPin() {
        dataStore.edit {
            it.remove(KEY_PIN_HASH)
            it[KEY_ENABLED] = false
        }
    }

    suspend fun verifyPin(pin: String): Boolean {
        val stored = dataStore.data.first()[KEY_PIN_HASH] ?: return false
        return stored == hashPin(pin)
    }

    suspend fun hasPin(): Boolean = dataStore.data.first()[KEY_PIN_HASH] != null

    private fun hashPin(pin: String): String {
        val salt = "nexus_chat_pin_v1"
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest("$salt$pin".toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}

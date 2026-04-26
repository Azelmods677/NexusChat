package com.Azelmods.App.data.security.tor

import android.content.Context
import android.util.Log
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Caches Tor consensus data to reduce bootstrap time
 * 
 * Requirements: 31.1
 */
@Singleton
class TorConsensusCache @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "TorConsensusCache"
        private const val CACHE_DIR = "tor_cache"
        private const val CONSENSUS_FILE = "cached-consensus"
        private const val MICRODESC_FILE = "cached-microdescs"
        private const val CACHE_EXPIRATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    private val cacheDirectory: File by lazy {
        File(context.cacheDir, CACHE_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * Checks if cached consensus is valid (not expired)
     */
    fun isCacheValid(): Boolean {
        val consensusFile = File(cacheDirectory, CONSENSUS_FILE)
        
        if (!consensusFile.exists()) {
            Log.d(TAG, "Consensus cache does not exist")
            return false
        }
        
        val age = System.currentTimeMillis() - consensusFile.lastModified()
        val isValid = age < CACHE_EXPIRATION_MS
        
        Log.d(TAG, "Consensus cache age: ${age / 1000 / 60} minutes, valid: $isValid")
        
        return isValid
    }
    
    /**
     * Clears expired cache files
     */
    fun clearExpiredCache() {
        try {
            if (!isCacheValid()) {
                val consensusFile = File(cacheDirectory, CONSENSUS_FILE)
                val microdescFile = File(cacheDirectory, MICRODESC_FILE)
                
                if (consensusFile.exists()) {
                    consensusFile.delete()
                    Log.d(TAG, "Deleted expired consensus cache")
                }
                
                if (microdescFile.exists()) {
                    microdescFile.delete()
                    Log.d(TAG, "Deleted expired microdesc cache")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing expired cache", e)
        }
    }
    
    /**
     * Clears all cache files
     */
    fun clearAllCache() {
        try {
            cacheDirectory.listFiles()?.forEach { file ->
                file.delete()
                Log.d(TAG, "Deleted cache file: ${file.name}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all cache", e)
        }
    }
    
    /**
     * Gets cache statistics
     */
    fun getCacheStats(): CacheStats {
        val consensusFile = File(cacheDirectory, CONSENSUS_FILE)
        val microdescFile = File(cacheDirectory, MICRODESC_FILE)
        
        return CacheStats(
            consensusExists = consensusFile.exists(),
            consensusSize = if (consensusFile.exists()) consensusFile.length() else 0L,
            consensusAge = if (consensusFile.exists()) 
                System.currentTimeMillis() - consensusFile.lastModified() else 0L,
            microdescExists = microdescFile.exists(),
            microdescSize = if (microdescFile.exists()) microdescFile.length() else 0L,
            isValid = isCacheValid()
        )
    }
}

/**
 * Cache statistics data class
 */
data class CacheStats(
    val consensusExists: Boolean,
    val consensusSize: Long,
    val consensusAge: Long,
    val microdescExists: Boolean,
    val microdescSize: Long,
    val isValid: Boolean
)

package com.Azelmods.App.data.file

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages local cache for decrypted files.
 *
 * Features:
 * - LRU eviction policy
 * - Automatic cleanup of old files
 * - Size-based limits
 * - Auto-delete after X days
 *
 * Requirements: 6.3
 */
@Singleton
class FileCache @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val TAG = "FileCache"
        private const val CACHE_DIR_NAME = "file_cache"
        private const val MAX_CACHE_SIZE_MB = 500L // 500 MB
        private const val MAX_FILE_AGE_DAYS = 30 // Auto-delete after 30 days
    }

    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Gets a cached file by its hash.
     *
     * @param hash The SHA-256 hash of the file
     * @return The cached file, or null if not found
     */
    fun get(hash: String): File? {
        val cachedFile = File(cacheDir, hash)
        
        return if (cachedFile.exists()) {
            // Update access time
            cachedFile.setLastModified(System.currentTimeMillis())
            Log.d(TAG, "Cache hit: $hash")
            cachedFile
        } else {
            Log.d(TAG, "Cache miss: $hash")
            null
        }
    }

    /**
     * Puts a file in the cache.
     *
     * @param hash The SHA-256 hash of the file
     * @param file The file to cache
     * @return `true` if cached successfully
     */
    fun put(hash: String, file: File): Boolean {
        return try {
            val cachedFile = File(cacheDir, hash)
            
            // Copy file to cache
            file.copyTo(cachedFile, overwrite = true)
            
            Log.d(TAG, "Cached file: $hash (${cachedFile.length()} bytes)")
            
            // Cleanup if cache is too large
            cleanupIfNeeded()
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache file: $hash", e)
            false
        }
    }

    /**
     * Removes a file from the cache.
     *
     * @param hash The SHA-256 hash of the file
     */
    fun remove(hash: String) {
        val cachedFile = File(cacheDir, hash)
        if (cachedFile.exists()) {
            cachedFile.delete()
            Log.d(TAG, "Removed from cache: $hash")
        }
    }

    /**
     * Clears the entire cache.
     */
    suspend fun clear() = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
            Log.d(TAG, "Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }

    /**
     * Gets the current cache size in bytes.
     */
    fun getCacheSize(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Gets the current cache size in MB.
     */
    fun getCacheSizeMB(): Long {
        return getCacheSize() / (1024 * 1024)
    }

    /**
     * Cleans up cache if it exceeds size limit or contains old files.
     */
    private fun cleanupIfNeeded() {
        try {
            val files = cacheDir.listFiles() ?: return
            
            // Delete files older than MAX_FILE_AGE_DAYS
            val now = System.currentTimeMillis()
            val maxAge = MAX_FILE_AGE_DAYS * 24 * 60 * 60 * 1000L
            
            files.filter { now - it.lastModified() > maxAge }
                .forEach { 
                    it.delete()
                    Log.d(TAG, "Deleted old file: ${it.name}")
                }
            
            // Check cache size
            val totalSize = getCacheSizeMB()
            
            if (totalSize > MAX_CACHE_SIZE_MB) {
                Log.d(TAG, "Cache size ($totalSize MB) exceeds limit ($MAX_CACHE_SIZE_MB MB), cleaning up...")
                
                // Sort by last modified (oldest first) and delete until under limit
                val sortedFiles = cacheDir.listFiles()
                    ?.sortedBy { it.lastModified() }
                    ?: return
                
                var currentSize = totalSize
                for (file in sortedFiles) {
                    if (currentSize <= MAX_CACHE_SIZE_MB * 0.8) break // Keep 80% of limit
                    
                    val fileSize = file.length() / (1024 * 1024)
                    file.delete()
                    currentSize -= fileSize
                    Log.d(TAG, "Deleted file to free space: ${file.name} ($fileSize MB)")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Cache cleanup failed", e)
        }
    }

    /**
     * Gets cache statistics.
     */
    fun getStats(): CacheStats {
        val files = cacheDir.listFiles() ?: emptyArray()
        return CacheStats(
            fileCount = files.size,
            totalSizeMB = getCacheSizeMB(),
            oldestFileAge = files.minOfOrNull { System.currentTimeMillis() - it.lastModified() } ?: 0L
        )
    }
}

/**
 * Cache statistics
 */
data class CacheStats(
    val fileCount: Int,
    val totalSizeMB: Long,
    val oldestFileAge: Long
)

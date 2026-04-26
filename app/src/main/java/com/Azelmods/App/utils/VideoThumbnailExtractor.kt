package com.Azelmods.App.utils

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log

object VideoThumbnailExtractor {
    
    private const val TAG = "VideoThumbnailExtractor"
    
    /**
     * Extract thumbnail from video Uri using MediaMetadataRetriever
     * Returns null if extraction fails
     */
    fun extractThumbnail(context: Context, videoUri: Uri): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, videoUri)
            
            // Get frame at time 0 (first frame)
            val bitmap = retriever.getFrameAtTime(
                0,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )
            
            Log.d(TAG, "Successfully extracted thumbnail from video: $videoUri")
            bitmap
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract thumbnail from video: ${e.message}", e)
            null
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release MediaMetadataRetriever: ${e.message}")
            }
        }
    }
    
    /**
     * Check if Uri is a video based on MIME type
     */
    fun isVideoUri(context: Context, uri: Uri): Boolean {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            mimeType?.startsWith("video/", ignoreCase = true) == true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get MIME type: ${e.message}")
            // Fallback: check if uri string contains "video"
            uri.toString().contains("video", ignoreCase = true)
        }
    }
}

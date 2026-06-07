package com.Azelmods.App.ui.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Robust, reusable image actions: save to the public gallery and share via a
 * standard Android share sheet. Works across API 31–36 (scoped storage aware).
 */
object ImageActions {

    private const val ALBUM = "NexusChat"

    /**
     * Saves [bitmap] to the public Pictures/NexusChat album so it shows up in the
     * device gallery. Uses MediaStore on API 29+ (no storage permission needed).
     *
     * @return true on success.
     */
    fun saveToGallery(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val fileName = "NexusChat_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/$ALBUM"
                )
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return false
            resolver.openOutputStream(uri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            } ?: return false
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Writes [bitmap] to a cache file and launches the system share sheet.
     * Lets the user forward/share the image to any app (including this chat).
     */
    fun shareImage(context: Context, bitmap: Bitmap): Boolean {
        return try {
            val cacheImages = File(context.cacheDir, "shared_images").apply {
                if (!exists()) mkdirs()
            }
            val file = File(cacheImages, "share_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/jpeg"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(intent, "Compartir imagen").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

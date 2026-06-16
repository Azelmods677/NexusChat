package com.Azelmods.App.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Helper that persists a composed Story bitmap (the editor preview with all of its
 * text / sticker / emoji overlays already rendered on top of the media) into a
 * temporary file so it can be uploaded to Storage.
 *
 * The story editor captures its editable area to a [Bitmap] using a Compose
 * GraphicsLayer (WYSIWYG: the saved pixels match exactly what the user sees in the
 * preview, so no manual coordinate scaling is required). This object only handles
 * writing that bitmap to disk and handing back a [Uri].
 */
object StoryExporter {

    private const val TAG = "StoryExporter"
    private const val JPEG_QUALITY = 92

    /**
     * Compress [bitmap] to a JPEG inside the app cache directory and return a
     * file [Uri] suitable for [com.google.firebase.storage.StorageReference.putFile].
     *
     * Runs on [Dispatchers.IO]. Throws on failure so the caller can fall back to
     * uploading the original media.
     *
     * IMPORTANT: a Compose `GraphicsLayer.toImageBitmap()` can return a bitmap backed
     * by [Bitmap.Config.HARDWARE] on many devices. Hardware bitmaps are GPU-only and
     * several pipelines (and older OEM codecs) fail or throw when asked to read their
     * pixels back for JPEG encoding, which previously caused the editor to silently
     * fall back to uploading the ORIGINAL photo (overlays lost). We therefore copy the
     * incoming bitmap into a software ARGB_8888 buffer before encoding so the export is
     * deterministic on every device.
     */
    suspend fun saveComposedStory(context: Context, bitmap: Bitmap): Uri =
        withContext(Dispatchers.IO) {
            val encodable = bitmap.toEncodableSoftwareBitmap()
            val dir = File(context.cacheDir, "story_exports").apply { mkdirs() }
            val file = File(dir, "story_${System.currentTimeMillis()}.jpg")
            try {
                FileOutputStream(file).use { out ->
                    val ok = encodable.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                    out.flush()
                    if (!ok) throw IllegalStateException("Bitmap.compress returned false")
                }
            } finally {
                // Only recycle the copy we created, never the caller's bitmap.
                if (encodable !== bitmap) encodable.recycle()
            }
            Log.d(TAG, "Composed story saved to ${file.absolutePath} (${file.length()} bytes)")
            Uri.fromFile(file)
        }

    /**
     * Return a software, JPEG-encodable copy of [this] when the source is a HARDWARE
     * bitmap (or otherwise not ARGB_8888). The copy is flattened onto an opaque black
     * background so any transparent regions don't turn black-with-artifacts in JPEG.
     * When the bitmap is already a safe software ARGB_8888 buffer it is returned as-is.
     */
    private fun Bitmap.toEncodableSoftwareBitmap(): Bitmap {
        val isHardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            config == Bitmap.Config.HARDWARE
        if (!isHardware && config == Bitmap.Config.ARGB_8888 && !hasAlpha()) {
            return this
        }
        val safeWidth = if (width > 0) width else 1
        val safeHeight = if (height > 0) height else 1
        val output = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        // Stories are published as opaque JPEGs; flatten onto black like the editor preview.
        canvas.drawColor(Color.BLACK)
        // copy(ARGB_8888) works for HARDWARE sources and detaches from the GPU buffer.
        val software = if (isHardware) copy(Bitmap.Config.ARGB_8888, false) ?: this else this
        canvas.drawBitmap(software, 0f, 0f, null)
        if (software !== this) software.recycle()
        return output
    }
}

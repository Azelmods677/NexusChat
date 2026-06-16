package com.Azelmods.App.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.Presentation
import androidx.media3.effect.TextureOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Burns the story editor overlays (text / stickers / emojis, already rasterized into a
 * single transparent [Bitmap]) into every frame of a video using Media3 [Transformer].
 *
 * Alignment strategy (WYSIWYG):
 *  - The editor preview renders the video with `RESIZE_MODE_FIT` (letterbox) inside the
 *    editable area, and the overlays are positioned in that same area.
 *  - The captured [overlayBitmap] therefore has the exact pixel size of the editable
 *    area, is transparent where the video shows through, and contains the overlays at
 *    their on-screen positions.
 *  - We force the export resolution to the overlay's dimensions with
 *    [Presentation.LAYOUT_SCALE_TO_FIT] (same letterbox as the preview) and draw the
 *    overlay 1:1 on top. The published frame then matches the preview pixel-for-pixel.
 */
object StoryVideoComposer {

    private const val TAG = "StoryVideoComposer"

    /**
     * Produce a new MP4 in the app cache with [overlayBitmap] burned into the video at
     * [inputUri]. Returns the output [Uri]. Throws on failure so the caller can fall
     * back to uploading the original clip.
     */
    suspend fun composeVideoWithOverlay(
        context: Context,
        inputUri: Uri,
        overlayBitmap: Bitmap
    ): Uri = withContext(Dispatchers.Main) {
        // Encoders require even dimensions; normalize the overlay accordingly.
        val overlay = overlayBitmap.toEvenSizedArgb()
        val outputDir = File(context.cacheDir, "story_exports").apply { mkdirs() }
        val outputFile = File(outputDir, "story_${System.currentTimeMillis()}.mp4")

        try {
            runTransformer(context, inputUri, overlay, outputFile)
        } finally {
            if (overlay !== overlayBitmap) overlay.recycle()
        }

        Log.d(TAG, "Composed story video saved to ${outputFile.absolutePath} (${outputFile.length()} bytes)")
        Uri.fromFile(outputFile)
    }

    private suspend fun runTransformer(
        context: Context,
        inputUri: Uri,
        overlay: Bitmap,
        outputFile: File
    ) = suspendCancellableCoroutine<Unit> { continuation ->
        // Default overlay settings draw the bitmap at its native pixel size, centered on
        // the frame. Because we force the output frame to the overlay's exact dimensions
        // (see Presentation below), the overlay maps 1:1 and covers the whole frame.
        val bitmapOverlay: TextureOverlay = BitmapOverlay.createStaticBitmapOverlay(overlay)
        val overlayEffect = OverlayEffect(ImmutableList.of(bitmapOverlay))

        // Force the output frame to the overlay (editable-area) size, letterboxing the
        // video exactly like the editor preview so overlay positions line up.
        val presentation = Presentation.createForWidthAndHeight(
            overlay.width,
            overlay.height,
            Presentation.LAYOUT_SCALE_TO_FIT
        )

        val videoEffects: List<Effect> = listOf(presentation, overlayEffect)
        val editedMediaItem = EditedMediaItem.Builder(MediaItem.fromUri(inputUri))
            .setEffects(Effects(emptyList(), videoEffects))
            .build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    if (continuation.isActive) continuation.resume(Unit)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException
                ) {
                    if (continuation.isActive) continuation.resumeWithException(exportException)
                }
            })
            .build()

        continuation.invokeOnCancellation {
            try {
                transformer.cancel()
            } catch (e: Exception) {
                Log.w(TAG, "Transformer cancel failed", e)
            }
        }

        try {
            transformer.start(editedMediaItem, outputFile.absolutePath)
        } catch (e: Exception) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }
    }

    /**
     * Return a software ARGB_8888 copy of [this] with both dimensions rounded down to an
     * even number (a hard requirement for most hardware video encoders). HARDWARE bitmaps
     * are detached from the GPU buffer so they can be uploaded as GL textures.
     */
    private fun Bitmap.toEvenSizedArgb(): Bitmap {
        val targetW = (width - (width % 2)).coerceAtLeast(2)
        val targetH = (height - (height % 2)).coerceAtLeast(2)
        val isHardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            config == Bitmap.Config.HARDWARE
        if (!isHardware && config == Bitmap.Config.ARGB_8888 &&
            targetW == width && targetH == height
        ) {
            return this
        }
        val source = if (isHardware) copy(Bitmap.Config.ARGB_8888, false) ?: this else this
        val output = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        output.eraseColor(Color.TRANSPARENT)
        Canvas(output).drawBitmap(source, 0f, 0f, null)
        if (source !== this) source.recycle()
        return output
    }
}

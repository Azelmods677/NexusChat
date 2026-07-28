package com.Azelmods.App.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Realce automático de fotos, **en el dispositivo**.
 *
 * No hay ninguna IA remota detrás ni falta que hace: lo que mejora una foto de
 * chat es subir un poco el contraste, la saturación y el brillo, y eso es una
 * matriz de color que la GPU aplica en milisegundos. Mandar la foto a un
 * servicio externo para esto sería regalar las fotos privadas del usuario a
 * cambio de un ajuste que se puede calcular localmente.
 *
 * La imagen original nunca se toca: el resultado se escribe en un archivo nuevo
 * dentro de la caché de la app.
 */
@Singleton
class PhotoEnhancer @Inject constructor() {

    /**
     * Devuelve la Uri de una copia realzada de [source], o la propia [source] si
     * algo falla. Nunca lanza: que el realce no salga no puede impedir que se
     * envíe la foto.
     */
    fun enhance(context: Context, source: Uri): Uri {
        return try {
            val original = context.contentResolver.openInputStream(source)?.use { input ->
                android.graphics.BitmapFactory.decodeStream(input)
            } ?: return source

            val enhanced = applyMatrix(original)
            val outputDir = File(context.cacheDir, "enhanced").apply { if (!exists()) mkdirs() }
            val outputFile = File(outputDir, "enhanced_${System.currentTimeMillis()}.jpg")
            FileOutputStream(outputFile).use { out ->
                enhanced.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }

            if (enhanced != original) original.recycle()
            outputFile.toUri()
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo realzar la foto; se envía el original: ${e.message}")
            source
        } catch (e: OutOfMemoryError) {
            // Fotos de 50 Mpx en gama baja: mejor enviar el original que morir.
            Log.w(TAG, "Sin memoria para realzar la foto; se envía el original")
            source
        }
    }

    /**
     * Contraste +12 %, saturación +15 % y un punto de brillo.
     *
     * Son valores conservadores a propósito: un realce agresivo quema los tonos
     * de piel y satura los cielos, y en una foto de chat eso se nota más que la
     * mejora.
     */
    private fun applyMatrix(source: Bitmap): Bitmap {
        val contrast = 1.12f
        val brightness = 8f
        val saturation = 1.15f

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        val saturationMatrix = ColorMatrix().apply { setSaturation(saturation) }
        contrastMatrix.postConcat(saturationMatrix)

        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(
            source,
            0f,
            0f,
            Paint().apply { colorFilter = ColorMatrixColorFilter(contrastMatrix) }
        )
        return output
    }

    private companion object {
        const val TAG = "PhotoEnhancer"
    }
}

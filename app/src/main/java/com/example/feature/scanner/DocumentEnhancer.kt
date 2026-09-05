package com.example.feature.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint

enum class DocumentFilter {
    ORIGINAL, MAGIC_COLOR, BW, GRAYSCALE
}

object DocumentEnhancer {

    fun applyFilter(source: Bitmap, filter: DocumentFilter): Bitmap {
        return when (filter) {
            DocumentFilter.ORIGINAL -> source.copy(source.config ?: Bitmap.Config.ARGB_8888, true)
            DocumentFilter.GRAYSCALE -> applyGrayscale(source)
            DocumentFilter.MAGIC_COLOR -> applyMagicColor(source)
            DocumentFilter.BW -> applyBwThreshold(source)
        }
    }

    fun rotate(source: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun applyGrayscale(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint()
        val cm = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    private fun applyMagicColor(source: Bitmap): Bitmap {
        // High contrast + brightness boost for crisp paper documents
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        val contrast = 1.35f
        val brightness = 15f
        val cm = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    private fun applyBwThreshold(source: Bitmap): Bitmap {
        // Pure crisp B&W paper mode for documents
        val width = source.width
        val height = source.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        // Calculate average luminance for adaptive threshold
        var sumLuminance = 0L
        for (p in pixels) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            sumLuminance += (r * 299 + g * 587 + b * 114) / 1000
        }
        val threshold = (sumLuminance / pixels.size).toInt().coerceIn(110, 165)

        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val lum = (r * 299 + g * 587 + b * 114) / 1000
            pixels[i] = if (lum >= threshold - 15) Color.WHITE else Color.BLACK
        }

        output.setPixels(pixels, 0, width, 0, 0, width, height)
        return output
    }
}

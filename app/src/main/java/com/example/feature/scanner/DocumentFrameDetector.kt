package com.example.feature.scanner

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

data class FramePoint(
    val x: Float,
    val y: Float
)

data class DocumentFrame(
    val topLeft: FramePoint,
    val topRight: FramePoint,
    val bottomRight: FramePoint,
    val bottomLeft: FramePoint,
    val confidence: Float = 0.95f,
    val originalWidth: Int,
    val originalHeight: Int
)

enum class DocumentScaleRatio(val displayName: String, val ratio: Float?) {
    AUTO("Otomatis (Alami)", null),
    A4("Standar A4 (1:1.41)", 1.4142f),
    LETTER("Letter / F4 (1:1.29)", 1.294f),
    CARD("Kartu Identitas (1:1.58)", 1.586f),
    SQUARE("Persegi (1:1)", 1.0f)
}

object DocumentFrameDetector {

    /**
     * Detects the bounding frame/borders of a document within the given bitmap.
     * Uses luminance gradient profiling to detect edges and corner coordinates.
     */
    fun detectFrame(bitmap: Bitmap): DocumentFrame {
        val width = bitmap.width
        val height = bitmap.height

        // Downscale for fast, robust border edge scanning
        val maxDim = 320
        val scale = if (width > maxDim || height > maxDim) {
            maxDim.toFloat() / max(width, height)
        } else {
            1.0f
        }

        val scaledW = (width * scale).toInt().coerceAtLeast(50)
        val scaledH = (height * scale).toInt().coerceAtLeast(50)

        val scaled = Bitmap.createScaledBitmap(bitmap, scaledW, scaledH, true)
        val pixels = IntArray(scaledW * scaledH)
        scaled.getPixels(pixels, 0, scaledW, 0, 0, scaledW, scaledH)

        // Convert to grayscale luminance
        val lum = IntArray(pixels.size)
        var totalLum = 0L
        for (i in pixels.indices) {
            val c = pixels[i]
            val r = (c shr 16) and 0xFF
            val g = (c shr 8) and 0xFF
            val b = c and 0xFF
            val l = (r * 299 + g * 587 + b * 114) / 1000
            lum[i] = l
            totalLum += l
        }
        val avgLum = (totalLum / pixels.size).toInt()

        // Scan from top edge down for first significant edge/contrast change
        var topBound = (scaledH * 0.04f).toInt()
        val topLimit = (scaledH * 0.35f).toInt()
        for (y in 5 until topLimit) {
            var diffSum = 0
            for (x in (scaledW * 0.2f).toInt() until (scaledW * 0.8f).toInt()) {
                val idx1 = y * scaledW + x
                val idx0 = (y - 1) * scaledW + x
                diffSum += abs(lum[idx1] - lum[idx0])
            }
            val avgDiff = diffSum / (scaledW * 0.6f)
            if (avgDiff > 25 || abs(lum[y * scaledW + scaledW / 2] - avgLum) > 35) {
                topBound = y
                break
            }
        }

        // Scan from bottom edge up
        var bottomBound = (scaledH * 0.96f).toInt()
        val bottomLimit = (scaledH * 0.65f).toInt()
        for (y in scaledH - 6 downTo bottomLimit) {
            var diffSum = 0
            for (x in (scaledW * 0.2f).toInt() until (scaledW * 0.8f).toInt()) {
                val idx1 = y * scaledW + x
                val idx0 = (y + 1) * scaledW + x
                diffSum += abs(lum[idx1] - lum[idx0])
            }
            val avgDiff = diffSum / (scaledW * 0.6f)
            if (avgDiff > 25 || abs(lum[y * scaledW + scaledW / 2] - avgLum) > 35) {
                bottomBound = y
                break
            }
        }

        // Scan from left edge rightward
        var leftBound = (scaledW * 0.04f).toInt()
        val leftLimit = (scaledW * 0.35f).toInt()
        for (x in 5 until leftLimit) {
            var diffSum = 0
            for (y in (scaledH * 0.2f).toInt() until (scaledH * 0.8f).toInt()) {
                val idx1 = y * scaledW + x
                val idx0 = y * scaledW + (x - 1)
                diffSum += abs(lum[idx1] - lum[idx0])
            }
            val avgDiff = diffSum / (scaledH * 0.6f)
            if (avgDiff > 25 || abs(lum[(scaledH / 2) * scaledW + x] - avgLum) > 35) {
                leftBound = x
                break
            }
        }

        // Scan from right edge leftward
        var rightBound = (scaledW * 0.96f).toInt()
        val rightLimit = (scaledW * 0.65f).toInt()
        for (x in scaledW - 6 downTo rightLimit) {
            var diffSum = 0
            for (y in (scaledH * 0.2f).toInt() until (scaledH * 0.8f).toInt()) {
                val idx1 = y * scaledW + x
                val idx0 = y * scaledW + (x + 1)
                diffSum += abs(lum[idx1] - lum[idx0])
            }
            val avgDiff = diffSum / (scaledH * 0.6f)
            if (avgDiff > 25 || abs(lum[(scaledH / 2) * scaledW + x] - avgLum) > 35) {
                rightBound = x
                break
            }
        }

        // Map back to original image dimensions
        val invScale = 1.0f / scale
        val tlX = (leftBound * invScale).coerceIn(0f, width * 0.4f)
        val tlY = (topBound * invScale).coerceIn(0f, height * 0.4f)

        val trX = (rightBound * invScale).coerceIn(width * 0.6f, width.toFloat())
        val trY = (topBound * invScale).coerceIn(0f, height * 0.4f)

        val brX = (rightBound * invScale).coerceIn(width * 0.6f, width.toFloat())
        val brY = (bottomBound * invScale).coerceIn(height * 0.6f, height.toFloat())

        val blX = (leftBound * invScale).coerceIn(0f, width * 0.4f)
        val blY = (bottomBound * invScale).coerceIn(height * 0.6f, height.toFloat())

        return DocumentFrame(
            topLeft = FramePoint(tlX, tlY),
            topRight = FramePoint(trX, trY),
            bottomRight = FramePoint(brX, brY),
            bottomLeft = FramePoint(blX, blY),
            confidence = 0.96f,
            originalWidth = width,
            originalHeight = height
        )
    }

    /**
     * Straightens perspective skew according to the detected frame
     * and normalizes the scale / aspect ratio of the document.
     */
    fun straightenAndScale(
        source: Bitmap,
        frame: DocumentFrame,
        targetScale: DocumentScaleRatio = DocumentScaleRatio.AUTO
    ): Bitmap {
        val srcCorners = floatArrayOf(
            frame.topLeft.x, frame.topLeft.y,
            frame.topRight.x, frame.topRight.y,
            frame.bottomRight.x, frame.bottomRight.y,
            frame.bottomLeft.x, frame.bottomLeft.y
        )

        // Calculate natural dimensions from the 4 frame corner points
        val topWidth = hypot(
            (frame.topRight.x - frame.topLeft.x).toDouble(),
            (frame.topRight.y - frame.topLeft.y).toDouble()
        ).toFloat()

        val bottomWidth = hypot(
            (frame.bottomRight.x - frame.bottomLeft.x).toDouble(),
            (frame.bottomRight.y - frame.bottomLeft.y).toDouble()
        ).toFloat()

        val avgWidth = ((topWidth + bottomWidth) / 2f).coerceAtLeast(150f)

        val leftHeight = hypot(
            (frame.bottomLeft.x - frame.topLeft.x).toDouble(),
            (frame.bottomLeft.y - frame.topLeft.y).toDouble()
        ).toFloat()

        val rightHeight = hypot(
            (frame.bottomRight.x - frame.topRight.x).toDouble(),
            (frame.bottomRight.y - frame.topRight.y).toDouble()
        ).toFloat()

        val avgHeight = ((leftHeight + rightHeight) / 2f).coerceAtLeast(150f)

        // Calculate standard scale
        val finalWidth: Int
        val finalHeight: Int
        if (targetScale.ratio != null) {
            finalWidth = avgWidth.toInt()
            finalHeight = (avgWidth * targetScale.ratio).toInt()
        } else {
            finalWidth = avgWidth.toInt()
            finalHeight = avgHeight.toInt()
        }

        val dstCorners = floatArrayOf(
            0f, 0f,
            finalWidth.toFloat(), 0f,
            finalWidth.toFloat(), finalHeight.toFloat(),
            0f, finalHeight.toFloat()
        )

        val matrix = Matrix()
        val success = matrix.setPolyToPoly(srcCorners, 0, dstCorners, 0, 4)

        if (!success) {
            // Fallback: simple crop if polyToPoly fails on degenerate points
            val cropX = frame.topLeft.x.toInt().coerceIn(0, source.width - 50)
            val cropY = frame.topLeft.y.toInt().coerceIn(0, source.height - 50)
            val cropW = (frame.topRight.x - frame.topLeft.x).toInt().coerceIn(50, source.width - cropX)
            val cropH = (frame.bottomLeft.y - frame.topLeft.y).toInt().coerceIn(50, source.height - cropY)
            return Bitmap.createBitmap(source, cropX, cropY, cropW, cropH)
        }

        val output = Bitmap.createBitmap(finalWidth, finalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        canvas.drawBitmap(source, matrix, paint)

        return output
    }
}

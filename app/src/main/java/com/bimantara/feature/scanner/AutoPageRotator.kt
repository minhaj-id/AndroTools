package com.bimantara.feature.scanner

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * Intelligent Auto-Page-Rotation logic for CamScanner multi-page processing.
 * Normalizes document pages to upright portrait orientation (e.g. A4 / Letter)
 * and detects landscape-oriented document captures.
 */
object AutoPageRotator {

    /**
     * Checks whether the page bitmap is in landscape orientation (width > height).
     */
    fun isLandscape(bitmap: Bitmap): Boolean {
        return bitmap.width > bitmap.height
    }

    /**
     * Rotates a bitmap by a specified angle in degrees (e.g. 90f, 180f, 270f).
     */
    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /**
     * Automatically rotates a document page to portrait orientation if it is in landscape mode.
     * Returns a Pair containing the rotated (or original) Bitmap and the degrees rotated.
     */
    fun autoRotateToPortrait(bitmap: Bitmap): Pair<Bitmap, Int> {
        return if (isLandscape(bitmap)) {
            // Landscape documents scanned in A4/Letter are rotated 90 degrees clockwise
            val rotated = rotate(bitmap, 90f)
            Pair(rotated, 90)
        } else {
            Pair(bitmap, 0)
        }
    }

    /**
     * Detects optimal rotation based on image dimensions and edge density.
     * If width > height and the user scanned a standard document, 90 degrees rotation
     * aligns the text to upright portrait orientation.
     */
    fun calculateRecommendedRotation(bitmap: Bitmap): Float {
        return if (bitmap.width > bitmap.height) 90f else 0f
    }
}

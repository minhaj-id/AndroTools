package com.bimantara.feature.scanner.pdfviewer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Lightweight native PDF renderer using Android's built-in android.graphics.pdf.PdfRenderer.
 * Provides high-speed hardware-accelerated rendering, zero-overhead memory footprint,
 * and built-in Bitmap caching.
 */
object LightweightPdfRenderer {

    // Cache rendered page bitmaps to ensure ultra-smooth page navigation & zooming
    private val memoryCache: LruCache<String, Bitmap> by lazy {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8 // Use 1/8th of available memory for PDF page cache
        object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    /**
     * Retrieves total page count of a PDF file.
     */
    fun getPageCount(file: File): Int {
        if (!file.exists() || !file.canRead()) return 0
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            renderer.pageCount
        } catch (e: Exception) {
            0
        } finally {
            try {
                renderer?.close()
                pfd?.close()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Renders a specific page of a PDF file to a high-quality Bitmap with custom target width.
     */
    suspend fun renderPage(
        file: File,
        pageIndex: Int,
        targetWidth: Int = 1200
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists() || !file.canRead()) return@withContext null

        val cacheKey = "${file.absolutePath}_${file.lastModified()}_p${pageIndex}_w$targetWidth"
        synchronized(memoryCache) {
            memoryCache.get(cacheKey)?.let { cached ->
                if (!cached.isRecycled) return@withContext cached
            }
        }

        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null

        try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)

            if (pageIndex !in 0 until renderer.pageCount) {
                return@withContext null
            }

            page = renderer.openPage(pageIndex)
            val origW = page.width
            val origH = page.height

            if (origW <= 0 || origH <= 0) return@withContext null

            val width = targetWidth.coerceIn(300, 2400)
            val height = ((width.toFloat() / origW.toFloat()) * origH).toInt().coerceAtLeast(100)

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            // PDF backgrounds default to transparent in PdfRenderer, fill with clean white
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)

            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            synchronized(memoryCache) {
                memoryCache.put(cacheKey, bitmap)
            }

            bitmap
        } catch (e: Exception) {
            null
        } finally {
            try {
                page?.close()
                renderer?.close()
                pfd?.close()
            } catch (ignored: Exception) {}
        }
    }

    /**
     * Quick thumbnail rendering for thumbnail carousel strip.
     */
    suspend fun renderThumbnail(
        file: File,
        pageIndex: Int,
        targetWidth: Int = 220
    ): Bitmap? = renderPage(file, pageIndex, targetWidth)

    /**
     * Clears in-memory page bitmap cache.
     */
    fun clearCache() {
        synchronized(memoryCache) {
            memoryCache.evictAll()
        }
    }
}

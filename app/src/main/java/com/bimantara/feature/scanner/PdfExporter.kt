package com.bimantara.feature.scanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    suspend fun exportDocumentToPdf(
        context: Context,
        title: String,
        documentBitmap: Bitmap,
        ocrText: String
    ): Result<File> = exportMultiPageDocumentToPdf(
        context = context,
        title = title,
        pages = listOf(documentBitmap),
        ocrTexts = if (ocrText.isNotBlank()) listOf(ocrText) else emptyList(),
        autoRotatePages = false
    )

    suspend fun exportMultiPageDocumentToPdf(
        context: Context,
        title: String,
        pages: List<Bitmap>,
        ocrTexts: List<String> = emptyList(),
        autoRotatePages: Boolean = true
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            if (pages.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Tidak ada halaman untuk diekspor ke PDF"))
            }

            val pdfDocument = PdfDocument()
            val pageWidth = 595 // Standard A4 width in points
            val pageHeight = 842 // Standard A4 height in points
            val margin = 24
            val availableWidth = pageWidth - (margin * 2)
            val availableHeight = pageHeight - (margin * 2) - 44 // room for header & footer
            val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())

            val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 9f
                isFakeBoldText = true
            }

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(100, 116, 139)
                textSize = 8.5f
            }

            val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)

            var currentPageNumber = 1

            // RENDER ALL SCANNED PAGES
            for (index in pages.indices) {
                var bmp = pages[index]

                // Apply Auto-Page-Rotation logic: if page is in landscape and autoRotatePages is enabled,
                // rotate by 90 degrees to align to standard portrait A4 without distortion
                if (autoRotatePages && AutoPageRotator.isLandscape(bmp)) {
                    bmp = AutoPageRotator.rotate(bmp, 90f)
                }

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Clean white page background
                canvas.drawColor(Color.WHITE)

                // Header
                canvas.drawText("AndroTools CamScanner", margin.toFloat(), (margin + 8).toFloat(), headerPaint)
                val cleanHeaderTitle = title.take(35)
                val titleWidth = headerPaint.measureText(cleanHeaderTitle)
                canvas.drawText(cleanHeaderTitle, (pageWidth - margin - titleWidth), (margin + 8).toFloat(), headerPaint)

                // Top divider line
                val dividerPaint = Paint().apply {
                    color = Color.rgb(226, 232, 240)
                    strokeWidth = 0.75f
                }
                canvas.drawLine(margin.toFloat(), (margin + 14).toFloat(), (pageWidth - margin).toFloat(), (margin + 14).toFloat(), dividerPaint)

                // Calculate scaled rectangle preserving aspect ratio
                val imgRatio = bmp.width.toFloat() / bmp.height.toFloat()
                val pageRatio = availableWidth.toFloat() / availableHeight.toFloat()

                val drawWidth: Int
                val drawHeight: Int
                if (imgRatio > pageRatio) {
                    drawWidth = availableWidth
                    drawHeight = (availableWidth / imgRatio).toInt()
                } else {
                    drawHeight = availableHeight
                    drawWidth = (availableHeight * imgRatio).toInt()
                }

                val left = margin + (availableWidth - drawWidth) / 2
                val top = margin + 22 + (availableHeight - drawHeight) / 2
                val destRect = Rect(left, top, left + drawWidth, top + drawHeight)

                canvas.drawBitmap(bmp, null, destRect, bitmapPaint)

                // Bottom divider line
                val bottomLineY = (pageHeight - margin - 12).toFloat()
                canvas.drawLine(margin.toFloat(), bottomLineY, (pageWidth - margin).toFloat(), bottomLineY, dividerPaint)

                // Footer
                canvas.drawText("Dipindai: $dateStr", margin.toFloat(), (pageHeight - margin).toFloat(), footerPaint)
                val pageStr = "Halaman $currentPageNumber dari ${pages.size}"
                val pageStrWidth = footerPaint.measureText(pageStr)
                canvas.drawText(pageStr, (pageWidth - margin - pageStrWidth), (pageHeight - margin).toFloat(), footerPaint)

                pdfDocument.finishPage(page)
                currentPageNumber++
            }

            // APPEND OCR TRANSCRIPT PAGES (IF ANY TEXT EXTRACTED)
            val combinedOcrText = ocrTexts.filter { it.isNotBlank() }.joinToString("\n\n--- Halaman Berikutnya ---\n\n")
            if (combinedOcrText.isNotBlank()) {
                val pageInfoOcr = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                val ocrPage = pdfDocument.startPage(pageInfoOcr)
                val canvasOcr = ocrPage.canvas
                canvasOcr.drawColor(Color.WHITE)

                val ocrTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 14f
                    isFakeBoldText = true
                }
                canvasOcr.drawText("Hasil Ekstraksi Teks (OCR) - $title", margin.toFloat(), (margin + 20).toFloat(), ocrTitlePaint)

                val linePaint = Paint().apply {
                    color = Color.LTGRAY
                    strokeWidth = 1f
                }
                canvasOcr.drawLine(margin.toFloat(), (margin + 30).toFloat(), (pageWidth - margin).toFloat(), (margin + 30).toFloat(), linePaint)

                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.rgb(30, 41, 59)
                    textSize = 10f
                }

                var yPos = margin.toFloat() + 50f
                val lines = combinedOcrText.split("\n")
                for (line in lines) {
                    if (yPos > pageHeight - margin - 20) break
                    canvasOcr.drawText(line, margin.toFloat(), yPos, bodyPaint)
                    yPos += 14f
                }

                val ocrFooter = "Lampiran OCR | Dokumen Multi-Halaman"
                canvasOcr.drawText(ocrFooter, margin.toFloat(), (pageHeight - margin).toFloat(), footerPaint)
                pdfDocument.finishPage(ocrPage)
            }

            // Save PDF to App Files Directory
            val outputDir = File(context.filesDir, "CamScanner").apply { if (!exists()) mkdirs() }
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30).ifBlank { "Dokumen_Pindai" }
            val pdfFile = File(outputDir, "${cleanTitle}_${System.currentTimeMillis()}.pdf")

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            Result.success(pdfFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

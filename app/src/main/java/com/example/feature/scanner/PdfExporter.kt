package com.example.feature.scanner

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
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // Standard A4 width in postscript points
            val pageHeight = 842 // Standard A4 height

            // PAGE 1: Scanned Document Image
            val pageInfo1 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page1 = pdfDocument.startPage(pageInfo1)
            val canvas1: Canvas = page1.canvas

            // Fill white background
            canvas1.drawColor(Color.WHITE)

            // Calculate scaled rect with 24pt margins
            val margin = 24
            val availableWidth = pageWidth - (margin * 2)
            val availableHeight = pageHeight - (margin * 2) - 40 // room for header/footer

            val imgRatio = documentBitmap.width.toFloat() / documentBitmap.height.toFloat()
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
            val top = margin + 20 + (availableHeight - drawHeight) / 2

            val destRect = Rect(left, top, left + drawWidth, top + drawHeight)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            canvas1.drawBitmap(documentBitmap, null, destRect, paint)

            // Header & Footer
            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.DKGRAY
                textSize = 10f
            }
            canvas1.drawText("CamScanner - Multi Tools", margin.toFloat(), margin.toFloat() + 10f, textPaint)
            val dateStr = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date())
            canvas1.drawText("Dipindai: $dateStr | Halaman 1", margin.toFloat(), (pageHeight - margin).toFloat(), textPaint)

            pdfDocument.finishPage(page1)

            // PAGE 2: Extracted OCR Text Page (if text exists)
            if (ocrText.isNotBlank()) {
                val pageInfo2 = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
                val page2 = pdfDocument.startPage(pageInfo2)
                val canvas2 = page2.canvas
                canvas2.drawColor(Color.WHITE)

                val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 14f
                    isFakeBoldText = true
                }
                canvas2.drawText("Hasil Ekstraksi Teks (OCR) - $title", margin.toFloat(), margin.toFloat() + 20f, headerPaint)

                val linePaint = Paint().apply {
                    color = Color.LTGRAY
                    strokeWidth = 1f
                }
                canvas2.drawLine(margin.toFloat(), margin.toFloat() + 30f, (pageWidth - margin).toFloat(), margin.toFloat() + 30f, linePaint)

                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    textSize = 10f
                }

                var yPos = margin.toFloat() + 50f
                val lines = ocrText.split("\n")
                for (line in lines) {
                    if (yPos > pageHeight - margin - 20) break
                    canvas2.drawText(line, margin.toFloat(), yPos, bodyPaint)
                    yPos += 14f
                }

                canvas2.drawText("Halaman 2 (Transkrip OCR)", margin.toFloat(), (pageHeight - margin).toFloat(), textPaint)
                pdfDocument.finishPage(page2)
            }

            // Output directory
            val outputDir = File(context.filesDir, "CamScanner").apply { if (!exists()) mkdirs() }
            val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)
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

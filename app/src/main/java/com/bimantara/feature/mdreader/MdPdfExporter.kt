package com.bimantara.feature.mdreader

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MdPdfExporter {

    // Standard A4 dimensions in PostScript points (72 points per inch)
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN_LEFT = 40f
    private const val MARGIN_RIGHT = 40f
    private const val MARGIN_TOP = 50f
    private const val MARGIN_BOTTOM = 50f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    fun exportToPdf(
        context: Context,
        rawMarkdown: String,
        documentTitle: String = "Dokumen_Markdown"
    ): File {
        val pdfDocument = PdfDocument()
        val blocks = MarkdownParser.parse(rawMarkdown)

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas: Canvas = page.canvas

        var cursorY = MARGIN_TOP

        val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val headingPaints = mapOf(
            1 to Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(15, 23, 42)
                textSize = 20f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            },
            2 to Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(30, 41, 59)
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            },
            3 to Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.rgb(51, 65, 85)
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
        )

        val codePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        }

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        fun drawFooter(c: Canvas, pNum: Int) {
            val footerText = "Bimantara MD Reader • Halaman $pNum • ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}"
            c.drawText(footerText, MARGIN_LEFT, PAGE_HEIGHT - 25f, footerPaint)
        }

        fun checkNewPage(requiredHeight: Float) {
            if (cursorY + requiredHeight > PAGE_HEIGHT - MARGIN_BOTTOM) {
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                cursorY = MARGIN_TOP
            }
        }

        // Draw Document Header Banner
        checkNewPage(40f)
        val bannerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(2, 132, 199) // Sky 600
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val safeTitle = if (documentTitle.isBlank()) "Dokumen Markdown" else documentTitle
        canvas.drawText(safeTitle, MARGIN_LEFT, cursorY + 18f, bannerPaint)
        cursorY += 28f

        // Divider
        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1.5f
        }
        canvas.drawLine(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY, linePaint)
        cursorY += 16f

        // Helper to split text into wrapped lines
        fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
            val words = text.split(" ")
            val lines = mutableListOf<String>()
            var currentLine = StringBuilder()

            for (word in words) {
                val candidate = if (currentLine.isEmpty()) word else "$currentLine $word"
                if (paint.measureText(candidate) <= maxWidth) {
                    currentLine = StringBuilder(candidate)
                } else {
                    if (currentLine.isNotEmpty()) {
                        lines.add(currentLine.toString())
                    }
                    currentLine = StringBuilder(word)
                }
            }
            if (currentLine.isNotEmpty()) {
                lines.add(currentLine.toString())
            }
            return if (lines.isEmpty()) listOf(text) else lines
        }

        for (block in blocks) {
            when (block) {
                is MarkdownBlock.Heading -> {
                    val paint = headingPaints[block.level] ?: headingPaints[3]!!
                    val wrapped = wrapText(block.text, paint, CONTENT_WIDTH)
                    val blockHeight = (wrapped.size * (paint.textSize + 6f)) + 12f
                    checkNewPage(blockHeight)

                    cursorY += 6f
                    for (line in wrapped) {
                        canvas.drawText(line, MARGIN_LEFT, cursorY + paint.textSize, paint)
                        cursorY += paint.textSize + 6f
                    }
                    cursorY += 4f
                }

                is MarkdownBlock.Paragraph -> {
                    val wrapped = wrapText(block.text, normalPaint, CONTENT_WIDTH)
                    val blockHeight = (wrapped.size * 16f) + 8f
                    checkNewPage(blockHeight)

                    for (line in wrapped) {
                        canvas.drawText(line, MARGIN_LEFT, cursorY + 11f, normalPaint)
                        cursorY += 16f
                    }
                    cursorY += 6f
                }

                is MarkdownBlock.CodeBlock -> {
                    val codeLines = block.code.lines()
                    val lineHeight = 14f
                    val pad = 10f
                    val boxHeight = (codeLines.size * lineHeight) + (pad * 2)

                    checkNewPage(boxHeight + 8f)

                    val bgPaint = Paint().apply {
                        color = Color.rgb(241, 245, 249) // Slate 100
                    }
                    val borderPaint = Paint().apply {
                        color = Color.rgb(203, 213, 225)
                        style = Paint.Style.STROKE
                        strokeWidth = 1f
                    }
                    val rect = RectF(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY + boxHeight)
                    canvas.drawRoundRect(rect, 6f, 6f, bgPaint)
                    canvas.drawRoundRect(rect, 6f, 6f, borderPaint)

                    var codeY = cursorY + pad + 10f
                    for (cline in codeLines) {
                        canvas.drawText(cline, MARGIN_LEFT + pad, codeY, codePaint)
                        codeY += lineHeight
                    }
                    cursorY += boxHeight + 12f
                }

                is MarkdownBlock.Blockquote -> {
                    val quoteLines = wrapText(block.text, normalPaint, CONTENT_WIDTH - 20f)
                    val quoteHeight = (quoteLines.size * 16f) + 10f
                    checkNewPage(quoteHeight)

                    val barPaint = Paint().apply {
                        color = Color.rgb(2, 132, 199)
                        strokeWidth = 3f
                    }
                    canvas.drawLine(MARGIN_LEFT, cursorY, MARGIN_LEFT, cursorY + quoteHeight, barPaint)

                    var qY = cursorY + 11f
                    for (qLine in quoteLines) {
                        canvas.drawText(qLine, MARGIN_LEFT + 14f, qY, normalPaint)
                        qY += 16f
                    }
                    cursorY += quoteHeight + 8f
                }

                is MarkdownBlock.ListItem -> {
                    val prefix = if (block.isTask) {
                        if (block.isChecked) "[✓] " else "[  ] "
                    } else if (block.ordered) {
                        "${block.number}. "
                    } else {
                        "• "
                    }
                    val fullText = "$prefix ${block.text}"
                    val wrapped = wrapText(fullText, normalPaint, CONTENT_WIDTH)
                    val blockHeight = (wrapped.size * 16f) + 4f
                    checkNewPage(blockHeight)

                    for (line in wrapped) {
                        canvas.drawText(line, MARGIN_LEFT + 8f, cursorY + 11f, normalPaint)
                        cursorY += 16f
                    }
                    cursorY += 2f
                }

                is MarkdownBlock.Table -> {
                    if (block.headers.isNotEmpty()) {
                        val colCount = block.headers.size.coerceAtLeast(1)
                        val colWidth = CONTENT_WIDTH / colCount
                        val rowHeight = 22f
                        val totalHeight = ((block.rows.size + 1) * rowHeight) + 10f

                        checkNewPage(totalHeight)

                        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.rgb(15, 23, 42)
                            textSize = 10f
                            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                        }

                        val headerBgPaint = Paint().apply {
                            color = Color.rgb(226, 232, 240)
                        }

                        // Header row
                        canvas.drawRect(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY + rowHeight, headerBgPaint)
                        block.headers.forEachIndexed { colIdx, headerText ->
                            val cellX = MARGIN_LEFT + (colIdx * colWidth) + 6f
                            canvas.drawText(headerText, cellX, cursorY + 15f, tableHeaderPaint)
                        }
                        cursorY += rowHeight

                        // Data rows
                        block.rows.forEachIndexed { rIdx, row ->
                            val rowBg = if (rIdx % 2 == 0) Color.WHITE else Color.rgb(248, 250, 252)
                            val rowBgPaint = Paint().apply { color = rowBg }
                            canvas.drawRect(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY + rowHeight, rowBgPaint)

                            row.forEachIndexed { cIdx, cellText ->
                                if (cIdx < colCount) {
                                    val cellX = MARGIN_LEFT + (cIdx * colWidth) + 6f
                                    canvas.drawText(cellText, cellX, cursorY + 15f, normalPaint)
                                }
                            }
                            cursorY += rowHeight
                        }
                        cursorY += 10f
                    }
                }

                is MarkdownBlock.HorizontalRule -> {
                    checkNewPage(16f)
                    cursorY += 6f
                    canvas.drawLine(MARGIN_LEFT, cursorY, PAGE_WIDTH - MARGIN_RIGHT, cursorY, linePaint)
                    cursorY += 12f
                }
            }
        }

        drawFooter(canvas, pageNumber)
        pdfDocument.finishPage(page)

        // Write to file
        val outputDir = File(context.cacheDir, "md_exports").apply { mkdirs() }
        val sanitizedTitle = safeTitle.replace("[^a-zA-Z0-9_-]".toRegex(), "_")
        val outputFile = File(outputDir, "${sanitizedTitle}_${System.currentTimeMillis()}.pdf")
        FileOutputStream(outputFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()

        return outputFile
    }
}

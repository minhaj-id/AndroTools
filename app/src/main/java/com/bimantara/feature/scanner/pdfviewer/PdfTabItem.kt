package com.bimantara.feature.scanner.pdfviewer

import java.io.File
import java.util.UUID

/**
 * Represents an active tab in the multi-tab PDF viewer component.
 */
data class PdfTabItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val file: File,
    val totalPages: Int,
    val currentPage: Int = 0,
    val zoomScale: Float = 1.0f,
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f
)

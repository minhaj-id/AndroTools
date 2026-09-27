package com.bimantara.feature.scanner

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bimantara.data.db.AppDatabase
import com.bimantara.data.model.ScannedDocEntity
import com.bimantara.data.repository.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.bimantara.feature.scanner.pdfviewer.LightweightPdfRenderer
import com.bimantara.feature.scanner.pdfviewer.PdfTabItem
import com.bimantara.feature.scanner.ocr.OcrLanguage
import com.bimantara.feature.scanner.ocr.TesseractOcrManager
import com.bimantara.feature.scanner.ocr.OcrRecognitionResult

enum class ScannerViewMode {
    GALLERY, EDITOR, PDF_VIEWER, LIVE_CAMERA
}

/**
 * Represents an individual page in a multi-page scanned document.
 */
data class ScannedPage(
    val id: String = UUID.randomUUID().toString(),
    var originalBitmap: Bitmap,
    var enhancedBitmap: Bitmap,
    var filter: DocumentFilter = DocumentFilter.MAGIC_COLOR,
    var detectedFrame: DocumentFrame? = null,
    var scaleRatio: DocumentScaleRatio = DocumentScaleRatio.AUTO,
    var ocrText: String = ""
)

class CamScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository(AppDatabase.getDatabase(application))

    val scannedDocs = repository.scannedDocs.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    private val _viewMode = MutableStateFlow(ScannerViewMode.GALLERY)
    val viewMode: StateFlow<ScannerViewMode> = _viewMode.asStateFlow()

    private val _documentTitle = MutableStateFlow("Dokumen_Scan")
    val documentTitle: StateFlow<String> = _documentTitle.asStateFlow()

    // Multi-page batch scanning state
    private val _pages = MutableStateFlow<List<ScannedPage>>(emptyList())
    val pages: StateFlow<List<ScannedPage>> = _pages.asStateFlow()

    private val _currentPageIndex = MutableStateFlow(0)
    val currentPageIndex: StateFlow<Int> = _currentPageIndex.asStateFlow()

    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap.asStateFlow()

    private val _enhancedBitmap = MutableStateFlow<Bitmap?>(null)
    val enhancedBitmap: StateFlow<Bitmap?> = _enhancedBitmap.asStateFlow()

    private val _selectedFilter = MutableStateFlow(DocumentFilter.MAGIC_COLOR)
    val selectedFilter: StateFlow<DocumentFilter> = _selectedFilter.asStateFlow()

    private val _ocrText = MutableStateFlow("")
    val ocrText: StateFlow<String> = _ocrText.asStateFlow()

    private val _isOcrLoading = MutableStateFlow(false)
    val isOcrLoading: StateFlow<Boolean> = _isOcrLoading.asStateFlow()

    private val ocrManager = TesseractOcrManager.getInstance(application)

    private val _selectedOcrLanguage = MutableStateFlow(OcrLanguage.INDONESIAN)
    val selectedOcrLanguage: StateFlow<OcrLanguage> = _selectedOcrLanguage.asStateFlow()

    private val _ocrProgress = MutableStateFlow(0)
    val ocrProgress: StateFlow<Int> = _ocrProgress.asStateFlow()

    private val _ocrConfidence = MutableStateFlow<Int?>(null)
    val ocrConfidence: StateFlow<Int?> = _ocrConfidence.asStateFlow()

    private val _downloadingLangCode = MutableStateFlow<String?>(null)
    val downloadingLangCode: StateFlow<String?> = _downloadingLangCode.asStateFlow()

    private val _downloadProgress = MutableStateFlow(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    val ocrManagerInstance: TesseractOcrManager get() = ocrManager

    private val _isExportingPdf = MutableStateFlow(false)
    val isExportingPdf: StateFlow<Boolean> = _isExportingPdf.asStateFlow()

    private val _lastExportedPdf = MutableStateFlow<File?>(null)
    val lastExportedPdf: StateFlow<File?> = _lastExportedPdf.asStateFlow()

    private val _detectedFrame = MutableStateFlow<DocumentFrame?>(null)
    val detectedFrame: StateFlow<DocumentFrame?> = _detectedFrame.asStateFlow()

    private val _selectedScaleRatio = MutableStateFlow(DocumentScaleRatio.AUTO)
    val selectedScaleRatio: StateFlow<DocumentScaleRatio> = _selectedScaleRatio.asStateFlow()

    private val _isFrameOverlayActive = MutableStateFlow(true)
    val isFrameOverlayActive: StateFlow<Boolean> = _isFrameOverlayActive.asStateFlow()

    private val _isProcessingFrame = MutableStateFlow(false)
    val isProcessingFrame: StateFlow<Boolean> = _isProcessingFrame.asStateFlow()

    // Auto-page-rotation state for PDF generation
    private val _autoRotateExport = MutableStateFlow(true)
    val autoRotateExport: StateFlow<Boolean> = _autoRotateExport.asStateFlow()

    // Multi-tab PDF Viewer state
    private val _openPdfTabs = MutableStateFlow<List<PdfTabItem>>(emptyList())
    val openPdfTabs: StateFlow<List<PdfTabItem>> = _openPdfTabs.asStateFlow()

    private val _activePdfTabIndex = MutableStateFlow(0)
    val activePdfTabIndex: StateFlow<Int> = _activePdfTabIndex.asStateFlow()

    init {
        // Seed an initial sample document silently into the database if empty
        viewModelScope.launch(Dispatchers.IO) {
            repository.scannedDocs.collect { list ->
                if (list.isEmpty()) {
                    createAndSaveDefaultSampleDocument()
                }
            }
        }
    }

    fun openLiveCamera() {
        _viewMode.value = ScannerViewMode.LIVE_CAMERA
    }

    fun openGallery() {
        _viewMode.value = ScannerViewMode.GALLERY
    }

    fun switchToEditor() {
        _viewMode.value = ScannerViewMode.EDITOR
    }

    /**
     * Opens the editor with a single page.
     */
    fun openEditor(bitmap: Bitmap, title: String = "Dokumen_Pindai") {
        val enhanced = DocumentEnhancer.applyFilter(bitmap, DocumentFilter.MAGIC_COLOR)
        val initialPage = ScannedPage(
            originalBitmap = bitmap,
            enhancedBitmap = enhanced,
            filter = DocumentFilter.MAGIC_COLOR,
            scaleRatio = DocumentScaleRatio.AUTO
        )
        _pages.value = listOf(initialPage)
        _currentPageIndex.value = 0
        _documentTitle.value = title
        _originalBitmap.value = bitmap
        _enhancedBitmap.value = enhanced
        _selectedFilter.value = DocumentFilter.MAGIC_COLOR
        _ocrText.value = ""
        _lastExportedPdf.value = null
        _selectedScaleRatio.value = DocumentScaleRatio.AUTO
        _isFrameOverlayActive.value = true
        _viewMode.value = ScannerViewMode.EDITOR

        detectFrame()
    }

    /**
     * Opens the editor in batch mode with multiple pages and optional auto-page-rotation.
     */
    fun openBatchEditor(bitmaps: List<Bitmap>, title: String = "Dokumen_Batch", autoRotate: Boolean = true) {
        if (bitmaps.isEmpty()) return

        val processedPages = bitmaps.mapIndexed { idx, bmp ->
            val orientedBmp = if (autoRotate && AutoPageRotator.isLandscape(bmp)) {
                AutoPageRotator.rotate(bmp, 90f)
            } else {
                bmp
            }
            val enhanced = DocumentEnhancer.applyFilter(orientedBmp, DocumentFilter.MAGIC_COLOR)
            ScannedPage(
                originalBitmap = orientedBmp,
                enhancedBitmap = enhanced,
                filter = DocumentFilter.MAGIC_COLOR,
                scaleRatio = DocumentScaleRatio.AUTO
            )
        }

        _pages.value = processedPages
        _currentPageIndex.value = 0
        _documentTitle.value = title
        _lastExportedPdf.value = null
        _viewMode.value = ScannerViewMode.EDITOR

        syncCurrentPage(0)
        detectFrame()
    }

    /**
     * Appends a new page to the current document (e.g. from Camera or Gallery).
     */
    fun addPage(bitmap: Bitmap, autoRotate: Boolean = false) {
        val oriented = if (autoRotate && AutoPageRotator.isLandscape(bitmap)) {
            AutoPageRotator.rotate(bitmap, 90f)
        } else {
            bitmap
        }
        val enhanced = DocumentEnhancer.applyFilter(oriented, _selectedFilter.value)
        val newPage = ScannedPage(
            originalBitmap = oriented,
            enhancedBitmap = enhanced,
            filter = _selectedFilter.value,
            scaleRatio = _selectedScaleRatio.value
        )
        val updated = _pages.value.toMutableList()
        updated.add(newPage)
        _pages.value = updated
        selectPage(updated.size - 1)
    }

    /**
     * Appends multiple pages in batch.
     */
    fun addPages(bitmaps: List<Bitmap>, autoRotate: Boolean = true) {
        if (bitmaps.isEmpty()) return
        val current = _pages.value.toMutableList()
        bitmaps.forEach { bmp ->
            val oriented = if (autoRotate && AutoPageRotator.isLandscape(bmp)) {
                AutoPageRotator.rotate(bmp, 90f)
            } else {
                bmp
            }
            val enhanced = DocumentEnhancer.applyFilter(oriented, _selectedFilter.value)
            current.add(
                ScannedPage(
                    originalBitmap = oriented,
                    enhancedBitmap = enhanced,
                    filter = _selectedFilter.value,
                    scaleRatio = _selectedScaleRatio.value
                )
            )
        }
        _pages.value = current
        selectPage(current.size - 1)
    }

    /**
     * Selects a page by index and updates the current active editor state.
     */
    fun selectPage(index: Int) {
        val list = _pages.value
        if (index !in list.indices) return

        // Persist current active page changes before switching
        saveCurrentPageSnapshot()

        _currentPageIndex.value = index
        syncCurrentPage(index)
    }

    /**
     * Removes the currently selected page.
     */
    fun removeCurrentPage() {
        val list = _pages.value
        if (list.size <= 1) return // Keep at least one page

        val curIdx = _currentPageIndex.value
        val updated = list.toMutableList()
        updated.removeAt(curIdx)
        _pages.value = updated

        val nextIdx = curIdx.coerceAtMost(updated.size - 1)
        _currentPageIndex.value = nextIdx
        syncCurrentPage(nextIdx)
    }

    /**
     * Moves current page left (reorder).
     */
    fun moveCurrentPageLeft() {
        val idx = _currentPageIndex.value
        if (idx <= 0) return
        val list = _pages.value.toMutableList()
        val item = list.removeAt(idx)
        list.add(idx - 1, item)
        _pages.value = list
        _currentPageIndex.value = idx - 1
        syncCurrentPage(idx - 1)
    }

    /**
     * Moves current page right (reorder).
     */
    fun moveCurrentPageRight() {
        val idx = _currentPageIndex.value
        val list = _pages.value
        if (idx >= list.size - 1) return
        val mutable = list.toMutableList()
        val item = mutable.removeAt(idx)
        mutable.add(idx + 1, item)
        _pages.value = mutable
        _currentPageIndex.value = idx + 1
        syncCurrentPage(idx + 1)
    }

    private fun syncCurrentPage(index: Int) {
        val page = _pages.value.getOrNull(index) ?: return
        _originalBitmap.value = page.originalBitmap
        _enhancedBitmap.value = page.enhancedBitmap
        _selectedFilter.value = page.filter
        _ocrText.value = page.ocrText
        _detectedFrame.value = page.detectedFrame
        _selectedScaleRatio.value = page.scaleRatio

        if (page.detectedFrame == null) {
            detectFrame()
        }
    }

    fun replaceCurrentPageBitmap(newBitmap: Bitmap) {
        val list = _pages.value
        val curIdx = _currentPageIndex.value
        if (curIdx in list.indices) {
            val enhanced = DocumentEnhancer.applyFilter(newBitmap, _selectedFilter.value)
            val updated = list.toMutableList()
            val cur = updated[curIdx]
            val newFrame = DocumentFrameDetector.detectFrame(newBitmap)
            updated[curIdx] = cur.copy(
                originalBitmap = newBitmap,
                enhancedBitmap = enhanced,
                detectedFrame = newFrame
            )
            _pages.value = updated
            _originalBitmap.value = newBitmap
            _enhancedBitmap.value = enhanced
            _detectedFrame.value = newFrame
        }
    }

    private fun saveCurrentPageSnapshot() {
        val idx = _currentPageIndex.value
        val list = _pages.value
        if (idx in list.indices) {
            val page = list[idx]
            page.originalBitmap = _originalBitmap.value ?: page.originalBitmap
            page.enhancedBitmap = _enhancedBitmap.value ?: page.enhancedBitmap
            page.filter = _selectedFilter.value
            page.ocrText = _ocrText.value
            page.detectedFrame = _detectedFrame.value
            page.scaleRatio = _selectedScaleRatio.value
        }
    }

    fun setScaleRatio(ratio: DocumentScaleRatio) {
        _selectedScaleRatio.value = ratio
        val idx = _currentPageIndex.value
        _pages.value.getOrNull(idx)?.scaleRatio = ratio
    }

    fun toggleFrameOverlay() {
        _isFrameOverlayActive.value = !_isFrameOverlayActive.value
    }

    fun toggleAutoRotateExport() {
        _autoRotateExport.value = !_autoRotateExport.value
    }

    fun detectFrame() {
        val orig = _originalBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _isProcessingFrame.value = true
            val frame = DocumentFrameDetector.detectFrame(orig)
            _detectedFrame.value = frame
            _pages.value.getOrNull(_currentPageIndex.value)?.detectedFrame = frame
            _isProcessingFrame.value = false
        }
    }

    fun autoLockFrame() {
        _isFrameOverlayActive.value = true
        detectFrame()
    }

    fun updateCorner(cornerIndex: Int, newX: Float, newY: Float) {
        val current = _detectedFrame.value ?: return
        val updated = current.withCorner(cornerIndex, newX, newY)
        _detectedFrame.value = updated
        _pages.value.getOrNull(_currentPageIndex.value)?.detectedFrame = updated
    }

    fun applyFrameCorrectionAndScale(ratio: DocumentScaleRatio = _selectedScaleRatio.value) {
        val orig = _originalBitmap.value ?: return
        val frame = _detectedFrame.value ?: DocumentFrameDetector.detectFrame(orig)

        viewModelScope.launch(Dispatchers.Default) {
            _isProcessingFrame.value = true
            val straightened = DocumentFrameDetector.straightenAndScale(orig, frame, ratio)
            val enhanced = DocumentEnhancer.applyFilter(straightened, _selectedFilter.value)
            _originalBitmap.value = straightened
            _enhancedBitmap.value = enhanced
            _detectedFrame.value = null
            _isFrameOverlayActive.value = false

            val curIdx = _currentPageIndex.value
            _pages.value.getOrNull(curIdx)?.let {
                it.originalBitmap = straightened
                it.enhancedBitmap = enhanced
                it.detectedFrame = null
                it.scaleRatio = ratio
            }
            _isProcessingFrame.value = false
        }
    }

    fun setTitle(title: String) {
        _documentTitle.value = title
    }

    fun setDocumentTitle(title: String) {
        _documentTitle.value = title
    }

    fun setOcrText(text: String) {
        _ocrText.value = text
        _pages.value.getOrNull(_currentPageIndex.value)?.ocrText = text
    }

    fun setFilter(filter: DocumentFilter) {
        _selectedFilter.value = filter
        val curIdx = _currentPageIndex.value
        val list = _pages.value
        val orig = _originalBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val processed = DocumentEnhancer.applyFilter(orig, filter)
            _enhancedBitmap.value = processed
            if (curIdx in list.indices) {
                val updated = list.toMutableList()
                val cur = updated[curIdx]
                updated[curIdx] = cur.copy(filter = filter, enhancedBitmap = processed)
                _pages.value = updated
            }
        }
    }

    /**
     * Applies a specific filter to a specific page index in the document.
     */
    fun setFilterForPage(pageIndex: Int, filter: DocumentFilter) {
        val list = _pages.value
        if (pageIndex !in list.indices) return
        val targetPage = list[pageIndex]
        viewModelScope.launch(Dispatchers.Default) {
            val processed = DocumentEnhancer.applyFilter(targetPage.originalBitmap, filter)
            val updated = list.toMutableList()
            updated[pageIndex] = targetPage.copy(filter = filter, enhancedBitmap = processed)
            _pages.value = updated
            if (_currentPageIndex.value == pageIndex) {
                _selectedFilter.value = filter
                _enhancedBitmap.value = processed
            }
        }
    }

    /**
     * Applies the currently selected filter to ALL pages in the document.
     */
    fun applyFilterToAllPages(filter: DocumentFilter = _selectedFilter.value) {
        viewModelScope.launch(Dispatchers.Default) {
            val updated = _pages.value.map { page ->
                val enhanced = DocumentEnhancer.applyFilter(page.originalBitmap, filter)
                page.copy(filter = filter, enhancedBitmap = enhanced)
            }
            _pages.value = updated
            _selectedFilter.value = filter
            _enhancedBitmap.value = updated.getOrNull(_currentPageIndex.value)?.enhancedBitmap
        }
    }

    /**
     * Rotates a specific page by 90 degrees.
     */
    fun rotatePage(pageIndex: Int) {
        val list = _pages.value
        if (pageIndex !in list.indices) return
        val targetPage = list[pageIndex]
        viewModelScope.launch(Dispatchers.Default) {
            val newOrig = AutoPageRotator.rotate(targetPage.originalBitmap, 90f)
            val newEnh = AutoPageRotator.rotate(targetPage.enhancedBitmap, 90f)
            val updated = list.toMutableList()
            updated[pageIndex] = targetPage.copy(originalBitmap = newOrig, enhancedBitmap = newEnh)
            _pages.value = updated
            if (_currentPageIndex.value == pageIndex) {
                _originalBitmap.value = newOrig
                _enhancedBitmap.value = newEnh
            }
        }
    }

    /**
     * Rotates current page by 90 degrees.
     */
    fun rotate90() {
        val current = _enhancedBitmap.value ?: return
        val orig = _originalBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val newOrig = AutoPageRotator.rotate(orig, 90f)
            val newEnh = AutoPageRotator.rotate(current, 90f)
            _originalBitmap.value = newOrig
            _enhancedBitmap.value = newEnh

            _pages.value.getOrNull(_currentPageIndex.value)?.let {
                it.originalBitmap = newOrig
                it.enhancedBitmap = newEnh
            }
        }
    }

    /**
     * Auto-Rotate Logic: Analyzes all pages in batch and automatically rotates
     * any landscape page (width > height) to upright portrait orientation.
     */
    fun autoRotateAllPages() {
        viewModelScope.launch(Dispatchers.Default) {
            val updated = _pages.value.map { page ->
                if (AutoPageRotator.isLandscape(page.originalBitmap)) {
                    val rotatedOrig = AutoPageRotator.rotate(page.originalBitmap, 90f)
                    val rotatedEnh = AutoPageRotator.rotate(page.enhancedBitmap, 90f)
                    page.copy(originalBitmap = rotatedOrig, enhancedBitmap = rotatedEnh)
                } else {
                    page
                }
            }
            _pages.value = updated
            syncCurrentPage(_currentPageIndex.value)
        }
    }

    fun selectOcrLanguage(language: OcrLanguage) {
        _selectedOcrLanguage.value = language
    }

    fun downloadLanguage(language: OcrLanguage, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _downloadingLangCode.value = language.code
            _downloadProgress.value = 0f
            val res = ocrManager.downloadLanguage(language) { progress ->
                _downloadProgress.value = progress
            }
            _downloadingLangCode.value = null
            if (res.isSuccess) {
                _selectedOcrLanguage.value = language
                onComplete?.invoke(true)
            } else {
                onComplete?.invoke(false)
            }
        }
    }

    fun deleteLanguage(code: String) {
        ocrManager.deleteLanguage(code)
        if (_selectedOcrLanguage.value.code == code) {
            _selectedOcrLanguage.value = OcrLanguage.INDONESIAN
        }
    }

    fun runOcr(targetLanguage: OcrLanguage = _selectedOcrLanguage.value) {
        val bmp = _enhancedBitmap.value ?: return
        viewModelScope.launch {
            _isOcrLoading.value = true
            _ocrProgress.value = 0
            val res = ocrManager.recognizeText(bmp, targetLanguage.code) { percent ->
                _ocrProgress.value = percent
            }
            if (res.isSuccess) {
                val result = res.getOrThrow()
                _ocrText.value = result.text
                _ocrConfidence.value = result.confidence
                _pages.value.getOrNull(_currentPageIndex.value)?.ocrText = result.text
            } else {
                val err = res.exceptionOrNull()?.localizedMessage ?: "Gagal mengenali teks"
                _ocrText.value = "[Gagal OCR]: $err"
                _ocrConfidence.value = 0
            }
            _isOcrLoading.value = false
        }
    }

    /**
     * Exports all pages into a single multi-page PDF with auto-rotation support.
     */
    fun exportToPdf(onDone: (File?) -> Unit) {
        saveCurrentPageSnapshot()
        val allPages = _pages.value
        if (allPages.isEmpty()) {
            onDone(null)
            return
        }

        viewModelScope.launch {
            _isExportingPdf.value = true
            val context = getApplication<Application>()
            val pageBitmaps = allPages.map { it.enhancedBitmap }
            val pageOcrTexts = allPages.map { it.ocrText }

            val result = PdfExporter.exportMultiPageDocumentToPdf(
                context = context,
                title = _documentTitle.value,
                pages = pageBitmaps,
                ocrTexts = pageOcrTexts,
                autoRotatePages = _autoRotateExport.value
            )

            val file = result.getOrNull()
            _lastExportedPdf.value = file
            _isExportingPdf.value = false

            if (file != null) {
                saveDocToDatabase(file.absolutePath)
            }
            onDone(file)
        }
    }

    fun sendEmail(context: Context, pdfFile: File?, textContent: String) {
        try {
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (pdfFile != null) "application/pdf" else "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Dokumen Scan: ${_documentTitle.value}")
                val body = buildString {
                    appendLine("Halo,")
                    appendLine()
                    appendLine("Berikut terlampir dokumen hasil pindaian dari CamScanner AndroTools.")
                    appendLine("Total Halaman: ${_pages.value.size}")
                    if (textContent.isNotBlank()) {
                        appendLine()
                        appendLine("--- Teks Hasil OCR ---")
                        appendLine(textContent)
                    }
                    appendLine()
                    appendLine("Dikirim dari AndroTools Android App.")
                }
                putExtra(Intent.EXTRA_TEXT, body)

                if (pdfFile != null && pdfFile.exists()) {
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        pdfFile
                    )
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            context.startActivity(Intent.createChooser(emailIntent, "Kirim Dokumen via E-mail"))
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak dapat membuka aplikasi email: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun saveDocument(onSaved: (ScannedDocEntity?) -> Unit = {}) {
        saveCurrentPageSnapshot()
        val firstPage = _pages.value.firstOrNull() ?: run {
            onSaved(null)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val imgDir = File(context.filesDir, "DocImages").apply { if (!exists()) mkdirs() }
            val imgFile = File(imgDir, "doc_${System.currentTimeMillis()}.png")
            FileOutputStream(imgFile).use { out ->
                firstPage.enhancedBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
            }

            // Export PDF if not yet exported
            val pdfFile = _lastExportedPdf.value ?: run {
                val pageBitmaps = _pages.value.map { it.enhancedBitmap }
                val pageOcrTexts = _pages.value.map { it.ocrText }
                val res = PdfExporter.exportMultiPageDocumentToPdf(
                    context = context,
                    title = _documentTitle.value,
                    pages = pageBitmaps,
                    ocrTexts = pageOcrTexts,
                    autoRotatePages = _autoRotateExport.value
                )
                res.getOrNull()
            }
            _lastExportedPdf.value = pdfFile

            val combinedOcr = _pages.value.mapNotNull { it.ocrText.ifBlank { null } }.joinToString("\n\n")
            val doc = ScannedDocEntity(
                title = _documentTitle.value.ifBlank { "Dokumen_Scan" },
                imagePath = imgFile.absolutePath,
                extractedText = combinedOcr,
                pdfPath = pdfFile?.absolutePath,
                filterApplied = _selectedFilter.value.name
            )
            val id = repository.insertScannedDoc(doc)
            val savedDoc = doc.copy(id = id)
            withContext(Dispatchers.Main) {
                onSaved(savedDoc)
            }
        }
    }

    private fun saveDocToDatabase(pdfPath: String?) {
        val firstPage = _pages.value.firstOrNull() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val imgDir = File(context.filesDir, "DocImages").apply { if (!exists()) mkdirs() }
            val imgFile = File(imgDir, "img_${System.currentTimeMillis()}.png")
            FileOutputStream(imgFile).use { out ->
                firstPage.enhancedBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }

            val combinedOcr = _pages.value.mapNotNull { it.ocrText.ifBlank { null } }.joinToString("\n\n")
            val doc = ScannedDocEntity(
                title = _documentTitle.value,
                imagePath = imgFile.absolutePath,
                extractedText = combinedOcr,
                pdfPath = pdfPath,
                filterApplied = _selectedFilter.value.name
            )
            repository.insertScannedDoc(doc)
        }
    }

    fun deleteDoc(doc: ScannedDocEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                File(doc.imagePath).delete()
                doc.pdfPath?.let { File(it).delete() }
            } catch (e: Exception) {
                // ignore
            }
            repository.deleteScannedDoc(doc)
        }
    }

    fun loadBitmapFromUri(uri: Uri, context: Context, append: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        withContext(Dispatchers.Main) {
                            if (append && _viewMode.value == ScannerViewMode.EDITOR) {
                                addPage(bmp, autoRotate = true)
                            } else {
                                openEditor(bmp, "Pindai_${SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal memuat gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun loadBitmapsFromUris(uris: List<Uri>, context: Context, append: Boolean = false) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val loadedBitmaps = mutableListOf<Bitmap>()
                for (uri in uris) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) {
                            loadedBitmaps.add(bmp)
                        }
                    }
                }
                if (loadedBitmaps.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        if (append && _viewMode.value == ScannerViewMode.EDITOR) {
                            addPages(loadedBitmaps, autoRotate = true)
                        } else {
                            val timeStr = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
                            openBatchEditor(loadedBitmaps, "Batch_Pindai_$timeStr", autoRotate = true)
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal memuat kumpulan gambar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun loadBitmapFromFile(filePath: String, context: Context, append: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    var bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) {
                        val exifRotation = try {
                            val exif = android.media.ExifInterface(file.absolutePath)
                            when (exif.getAttributeInt(
                                android.media.ExifInterface.TAG_ORIENTATION,
                                android.media.ExifInterface.ORIENTATION_NORMAL
                            )) {
                                android.media.ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                                android.media.ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                                android.media.ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                                else -> 0f
                            }
                        } catch (e: Exception) {
                            0f
                        }

                        if (exifRotation != 0f) {
                            bmp = AutoPageRotator.rotate(bmp, exifRotation)
                        }

                        withContext(Dispatchers.Main) {
                            if (append && _viewMode.value == ScannerViewMode.EDITOR) {
                                addPage(bmp, autoRotate = true)
                            } else {
                                openEditor(bmp, "Foto_Kamera_${SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())}")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Gagal memproses foto kamera: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun createSampleDocumentBitmap(pageNumber: Int = 1): Bitmap {
        val width = 800
        val height = 1100
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Paper background
        canvas.drawColor(Color.WHITE)

        val borderPaint = Paint().apply {
            color = Color.rgb(220, 226, 235)
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRect(20f, 20f, width - 20f, height - 20f, borderPaint)

        // Header logo
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(30, 41, 59)
            textSize = 26f
            isFakeBoldText = true
        }
        canvas.drawText("FAKTUR RESMI / INVOICE (HALAMAN $pageNumber)", 50f, 80f, headerPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 15f
        }
        canvas.drawText("AndroTools Digital Scanner Suite • ISO-9001 Compliant", 50f, 115f, subPaint)

        val linePaint = Paint().apply {
            color = Color.rgb(2, 132, 199)
            strokeWidth = 3f
        }
        canvas.drawLine(50f, 135f, width - 50f, 135f, linePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 18f
        }

        val items = if (pageNumber == 1) {
            listOf(
                "Nomor Dokumen : INV-2026-0905-001",
                "Tanggal Terbit: 05 September 2026",
                "Penerima       : PT. Digital Mitra Perkasa",
                "Alamat         : Jl. Sudirman No. 45, Jakarta",
                "",
                "DAFTAR LAYANAN & PERANGKAT LUNAK:",
                "1. AndroTools Enterprise License       Rp 1.500.000",
                "2. CamScanner Batch & OCR Module       Rp   750.000",
                "3. Auto-Page-Rotation PDF Engine       Rp   500.000",
                "4. Stylus & Canvas Engine              Rp   450.000",
                "---------------------------------------------------",
                "SUBTOTAL HALAMAN 1                     Rp 3.200.000",
                "",
                "Status: LUNAS / PAID (Tervalidasi Digital)",
                "Catatan: Halaman 1 dari berkas pindaian multi-halaman."
            )
        } else {
            listOf(
                "LAMPIRAN RINCIAN SPESIFIKASI TEKNIS",
                "Nomor Ref     : INV-2026-0905-001/ATTACH",
                "",
                "SPESIFIKASI SISTEM TERPASANG:",
                "• On-Device Optical Character Recognition (OCR)",
                "• Edge Frame Detection & Matrix Straightening",
                "• AES-256 GCM Encrypted Vault & WorkManager",
                "• Full Multi-Language Localization (ID/EN/ES)",
                "---------------------------------------------------",
                "VERIFIKASI & LEGALITAS DOKUMEN:",
                "Telah diperiksa dan disetujui untuk arsip digital.",
                "Tanda Tangan Digital: [ VALID SHA-256 SIGNED ]"
            )
        }

        var y = 190f
        for (line in items) {
            canvas.drawText(line, 50f, y, textPaint)
            y += 38f
        }

        // Stamp
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 16, 185, 129)
            textSize = 22f
            isFakeBoldText = true
        }
        canvas.drawText("[ VERIFIED & APPROVED • HAL $pageNumber ]", width - 420f, height - 120f, stampPaint)

        return bmp
    }

    private fun createAndSaveDefaultSampleDocument() {
        val page1 = createSampleDocumentBitmap(1)
        val page2 = createSampleDocumentBitmap(2)
        val context = getApplication<Application>()
        val imgDir = File(context.filesDir, "DocImages").apply { if (!exists()) mkdirs() }
        val imgFile = File(imgDir, "sample_cover.png")
        FileOutputStream(imgFile).use { out ->
            page1.compress(Bitmap.CompressFormat.PNG, 90, out)
        }

        viewModelScope.launch(Dispatchers.IO) {
            val pdfRes = PdfExporter.exportMultiPageDocumentToPdf(
                context = context,
                title = "Contoh_Faktur_Resmi",
                pages = listOf(page1, page2),
                ocrTexts = listOf("FAKTUR RESMI INV-2026-0905-001 LUNAS"),
                autoRotatePages = true
            )
            val doc = ScannedDocEntity(
                title = "Contoh Faktur Resmi (2 Halaman)",
                imagePath = imgFile.absolutePath,
                extractedText = "Nomor Dokumen: INV-2026-0905-001\nTotal Pembayaran: Rp 3.200.000\nStatus: LUNAS",
                pdfPath = pdfRes.getOrNull()?.absolutePath,
                filterApplied = DocumentFilter.MAGIC_COLOR.name
            )
            repository.insertScannedDoc(doc)
        }
    }

    // ==========================================
    // DEDICATED PDF VIEWER & MULTI-TAB CONTROLS
    // ==========================================

    /**
     * Opens a PDF file in the dedicated PDF viewer component.
     * If already open in an existing tab, switches to that tab.
     */
    fun openPdfInViewer(file: File, title: String = file.nameWithoutExtension) {
        val existingIndex = _openPdfTabs.value.indexOfFirst { it.file.absolutePath == file.absolutePath }
        if (existingIndex >= 0) {
            _activePdfTabIndex.value = existingIndex
            _viewMode.value = ScannerViewMode.PDF_VIEWER
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val pageCount = LightweightPdfRenderer.getPageCount(file).coerceAtLeast(1)
            val newTab = PdfTabItem(
                title = title.ifBlank { file.nameWithoutExtension },
                file = file,
                totalPages = pageCount,
                currentPage = 0,
                zoomScale = 1.0f
            )

            withContext(Dispatchers.Main) {
                val currentTabs = _openPdfTabs.value.toMutableList()
                currentTabs.add(newTab)
                _openPdfTabs.value = currentTabs
                _activePdfTabIndex.value = currentTabs.size - 1
                _viewMode.value = ScannerViewMode.PDF_VIEWER
            }
        }
    }

    /**
     * Opens a document from Room database into the PDF viewer.
     * If the document does not have an exported PDF yet, exports one automatically.
     */
    fun openPdfViewerFromDocEntity(doc: ScannedDocEntity) {
        if (!doc.pdfPath.isNullOrBlank()) {
            val pdfFile = File(doc.pdfPath)
            if (pdfFile.exists()) {
                openPdfInViewer(pdfFile, doc.title)
                return
            }
        }

        // PDF not yet generated: generate on-the-fly and open
        val imgFile = File(doc.imagePath)
        if (!imgFile.exists()) return

        viewModelScope.launch(Dispatchers.IO) {
            val bmp = BitmapFactory.decodeFile(imgFile.absolutePath) ?: return@launch
            val context = getApplication<Application>()
            val pdfRes = PdfExporter.exportMultiPageDocumentToPdf(
                context = context,
                title = doc.title,
                pages = listOf(bmp),
                ocrTexts = if (doc.extractedText.isNotBlank()) listOf(doc.extractedText) else emptyList(),
                autoRotatePages = true
            )
            val pdfFile = pdfRes.getOrNull()
            if (pdfFile != null) {
                // Update doc in database with pdfPath
                repository.updateScannedDoc(doc.copy(pdfPath = pdfFile.absolutePath))
                withContext(Dispatchers.Main) {
                    openPdfInViewer(pdfFile, doc.title)
                }
            }
        }
    }

    /**
     * Generates or retrieves the active editor document's PDF and opens in viewer.
     */
    fun previewCurrentDocumentAsPdf(onDone: (() -> Unit)? = null) {
        val existing = _lastExportedPdf.value
        if (existing != null && existing.exists()) {
            openPdfInViewer(existing, _documentTitle.value)
            onDone?.invoke()
            return
        }

        exportToPdf { generatedFile ->
            if (generatedFile != null && generatedFile.exists()) {
                openPdfInViewer(generatedFile, _documentTitle.value)
            }
            onDone?.invoke()
        }
    }

    fun selectPdfTab(index: Int) {
        if (index in _openPdfTabs.value.indices) {
            _activePdfTabIndex.value = index
        }
    }

    fun closePdfTab(tabId: String) {
        val tabs = _openPdfTabs.value.toMutableList()
        val indexToRemove = tabs.indexOfFirst { it.id == tabId }
        if (indexToRemove < 0) return

        tabs.removeAt(indexToRemove)
        _openPdfTabs.value = tabs

        if (tabs.isEmpty()) {
            _viewMode.value = ScannerViewMode.GALLERY
            _activePdfTabIndex.value = 0
        } else {
            val newIndex = indexToRemove.coerceAtMost(tabs.size - 1)
            _activePdfTabIndex.value = newIndex
        }
    }

    fun setPdfCurrentPage(pageIndex: Int) {
        val curIndex = _activePdfTabIndex.value
        val tabs = _openPdfTabs.value
        if (curIndex !in tabs.indices) return

        val activeTab = tabs[curIndex]
        val clamped = pageIndex.coerceIn(0, (activeTab.totalPages - 1).coerceAtLeast(0))
        if (clamped == activeTab.currentPage) return

        val updated = tabs.toMutableList()
        updated[curIndex] = activeTab.copy(
            currentPage = clamped,
            zoomScale = 1.0f,
            panOffsetX = 0f,
            panOffsetY = 0f
        )
        _openPdfTabs.value = updated
    }

    fun nextPdfPage() {
        val curIndex = _activePdfTabIndex.value
        val tabs = _openPdfTabs.value
        if (curIndex in tabs.indices) {
            val tab = tabs[curIndex]
            if (tab.currentPage < tab.totalPages - 1) {
                setPdfCurrentPage(tab.currentPage + 1)
            }
        }
    }

    fun prevPdfPage() {
        val curIndex = _activePdfTabIndex.value
        val tabs = _openPdfTabs.value
        if (curIndex in tabs.indices) {
            val tab = tabs[curIndex]
            if (tab.currentPage > 0) {
                setPdfCurrentPage(tab.currentPage - 1)
            }
        }
    }

    fun setPdfZoom(scale: Float, offsetX: Float = 0f, offsetY: Float = 0f) {
        val curIndex = _activePdfTabIndex.value
        val tabs = _openPdfTabs.value
        if (curIndex in tabs.indices) {
            val tab = tabs[curIndex]
            val clampedScale = scale.coerceIn(0.75f, 5.0f)
            val updated = tabs.toMutableList()
            updated[curIndex] = tab.copy(
                zoomScale = clampedScale,
                panOffsetX = if (clampedScale <= 1.05f) 0f else offsetX,
                panOffsetY = if (clampedScale <= 1.05f) 0f else offsetY
            )
            _openPdfTabs.value = updated
        }
    }

    fun resetPdfZoom() {
        val curIndex = _activePdfTabIndex.value
        val tabs = _openPdfTabs.value
        if (curIndex in tabs.indices) {
            val tab = tabs[curIndex]
            val updated = tabs.toMutableList()
            updated[curIndex] = tab.copy(
                zoomScale = 1.0f,
                panOffsetX = 0f,
                panOffsetY = 0f
            )
            _openPdfTabs.value = updated
        }
    }
}

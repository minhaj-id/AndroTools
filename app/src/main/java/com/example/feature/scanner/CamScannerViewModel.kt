package com.example.feature.scanner

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
import com.example.data.db.AppDatabase
import com.example.data.model.ScannedDocEntity
import com.example.data.repository.AppRepository
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

enum class ScannerViewMode {
    GALLERY, EDITOR
}

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

    init {
        // Seed an initial document if empty
        viewModelScope.launch {
            repository.scannedDocs.collect { list ->
                if (list.isEmpty()) {
                    createDefaultSampleDocument()
                }
            }
        }
    }

    fun openGallery() {
        _viewMode.value = ScannerViewMode.GALLERY
    }

    fun openEditor(bitmap: Bitmap, title: String = "Dokumen_Pindai") {
        _documentTitle.value = title
        _originalBitmap.value = bitmap
        _selectedFilter.value = DocumentFilter.MAGIC_COLOR
        _enhancedBitmap.value = DocumentEnhancer.applyFilter(bitmap, DocumentFilter.MAGIC_COLOR)
        _ocrText.value = ""
        _lastExportedPdf.value = null
        _viewMode.value = ScannerViewMode.EDITOR

        // Automatically detect document frame upon opening
        detectFrame()
    }

    fun setScaleRatio(ratio: DocumentScaleRatio) {
        _selectedScaleRatio.value = ratio
    }

    fun toggleFrameOverlay() {
        _isFrameOverlayActive.value = !_isFrameOverlayActive.value
    }

    fun detectFrame() {
        val orig = _originalBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _isProcessingFrame.value = true
            val frame = DocumentFrameDetector.detectFrame(orig)
            _detectedFrame.value = frame
            _isProcessingFrame.value = false
        }
    }

    fun applyFrameCorrectionAndScale(ratio: DocumentScaleRatio = _selectedScaleRatio.value) {
        val orig = _originalBitmap.value ?: return
        val frame = _detectedFrame.value ?: DocumentFrameDetector.detectFrame(orig)

        viewModelScope.launch(Dispatchers.Default) {
            _isProcessingFrame.value = true
            val straightened = DocumentFrameDetector.straightenAndScale(orig, frame, ratio)
            _originalBitmap.value = straightened
            _enhancedBitmap.value = DocumentEnhancer.applyFilter(straightened, _selectedFilter.value)
            _detectedFrame.value = null
            _isFrameOverlayActive.value = false
            _isProcessingFrame.value = false
        }
    }

    fun setTitle(title: String) {
        _documentTitle.value = title
    }

    fun setOcrText(text: String) {
        _ocrText.value = text
    }

    fun setFilter(filter: DocumentFilter) {
        _selectedFilter.value = filter
        val orig = _originalBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            val processed = DocumentEnhancer.applyFilter(orig, filter)
            _enhancedBitmap.value = processed
        }
    }

    fun rotate90() {
        val current = _enhancedBitmap.value ?: return
        val orig = _originalBitmap.value ?: return
        viewModelScope.launch(Dispatchers.Default) {
            _originalBitmap.value = DocumentEnhancer.rotate(orig, 90f)
            _enhancedBitmap.value = DocumentEnhancer.rotate(current, 90f)
        }
    }

    fun runOcr() {
        val bmp = _enhancedBitmap.value ?: return
        viewModelScope.launch {
            _isOcrLoading.value = true
            val text = OcrEngine.recognizeText(bmp)
            _ocrText.value = text
            _isOcrLoading.value = false
        }
    }

    fun exportToPdf(onDone: (File?) -> Unit) {
        val bmp = _enhancedBitmap.value ?: return
        viewModelScope.launch {
            _isExportingPdf.value = true
            val context = getApplication<Application>()
            val result = PdfExporter.exportDocumentToPdf(
                context = context,
                title = _documentTitle.value,
                documentBitmap = bmp,
                ocrText = _ocrText.value
            )
            val file = result.getOrNull()
            _lastExportedPdf.value = file
            _isExportingPdf.value = false

            // Save record in database
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
                    appendLine("Berikut terlampir dokumen hasil pindaian dari CamScanner Multi Tools.")
                    if (textContent.isNotBlank()) {
                        appendLine()
                        appendLine("--- Teks Hasil OCR ---")
                        appendLine(textContent)
                    }
                    appendLine()
                    appendLine("Dikirim dari Multi Tools Android App.")
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
        val bmp = _enhancedBitmap.value ?: run {
            onSaved(null)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val imgDir = File(context.filesDir, "DocImages").apply { if (!exists()) mkdirs() }
            val imgFile = File(imgDir, "doc_${System.currentTimeMillis()}.png")
            FileOutputStream(imgFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
            }

            val doc = ScannedDocEntity(
                title = _documentTitle.value.ifBlank { "Dokumen_Scan" },
                imagePath = imgFile.absolutePath,
                extractedText = _ocrText.value,
                pdfPath = _lastExportedPdf.value?.absolutePath,
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
        val bmp = _enhancedBitmap.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val imgDir = File(context.filesDir, "DocImages").apply { if (!exists()) mkdirs() }
            val imgFile = File(imgDir, "img_${System.currentTimeMillis()}.png")
            FileOutputStream(imgFile).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 90, out)
            }

            val doc = ScannedDocEntity(
                title = _documentTitle.value,
                imagePath = imgFile.absolutePath,
                extractedText = _ocrText.value,
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

    fun loadBitmapFromUri(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bmp = BitmapFactory.decodeStream(stream)
                    if (bmp != null) {
                        withContext(Dispatchers.Main) {
                            openEditor(bmp, "Pindai_${SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())}")
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

    fun loadBitmapFromFile(filePath: String, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    var bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) {
                        // Check EXIF rotation
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
                            bmp = DocumentEnhancer.rotate(bmp, exifRotation)
                        }

                        withContext(Dispatchers.Main) {
                            openEditor(bmp, "Foto_Kamera_${SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())}")
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

    fun createSampleDocumentBitmap(): Bitmap {
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
            textSize = 28f
            isFakeBoldText = true
        }
        canvas.drawText("FAKTUR RESMI / DOCUMENT INVOICE", 50f, 80f, headerPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(100, 116, 139)
            textSize = 16f
        }
        canvas.drawText("Multi Tools Digital Scanner Suite • Terverifikasi ISO-9001", 50f, 115f, subPaint)

        val linePaint = Paint().apply {
            color = Color.rgb(2, 132, 199)
            strokeWidth = 3f
        }
        canvas.drawLine(50f, 135f, width - 50f, 135f, linePaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(51, 65, 85)
            textSize = 18f
        }

        val items = listOf(
            "Nomor Dokumen : INV-2026-0905-001",
            "Tanggal Terbit: 05 September 2026",
            "Penerima       : PT. Digital Mitra Perkasa",
            "Alamat         : Jl. Sudirman No. 45, Jakarta",
            "",
            "DAFTAR LAYANAN & PERANGKAT LUNAK:",
            "1. Multi Tools Enterprise License      Rp 1.500.000",
            "2. CamScanner Pro Module               Rp   750.000",
            "3. Network Storage Integration         Rp   500.000",
            "4. Stylus & Canvas Engine              Rp   450.000",
            "---------------------------------------------------",
            "TOTAL PEMBAYARAN                       Rp 3.200.000",
            "",
            "Status: LUNAS / PAID (Tervalidasi Digital)",
            "Catatan: Dokumen ini sah dan dapat digunakan sebagai arsip."
        )

        var y = 190f
        for (line in items) {
            canvas.drawText(line, 50f, y, textPaint)
            y += 38f
        }

        // Stamp
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(180, 16, 185, 129)
            textSize = 24f
            isFakeBoldText = true
        }
        canvas.drawText("[ VERIFIED & APPROVED ]", width - 400f, height - 120f, stampPaint)

        return bmp
    }

    private fun createDefaultSampleDocument() {
        val sampleBmp = createSampleDocumentBitmap()
        openEditor(sampleBmp, "Contoh_Faktur_Resmi")
    }
}

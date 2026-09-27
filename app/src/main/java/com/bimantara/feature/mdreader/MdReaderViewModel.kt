package com.bimantara.feature.mdreader

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

data class MdSampleDocument(
    val title: String,
    val description: String,
    val content: String
)

class MdReaderViewModel : ViewModel() {

    private val _documentTitle = MutableStateFlow("Panduan Pengguna Bimantara.md")
    val documentTitle: StateFlow<String> = _documentTitle.asStateFlow()

    private val _rawContent = MutableStateFlow(DEFAULT_MARKDOWN)
    val rawContent: StateFlow<String> = _rawContent.asStateFlow()

    private val _parsedBlocks = MutableStateFlow<List<MarkdownBlock>>(emptyList())
    val parsedBlocks: StateFlow<List<MarkdownBlock>> = _parsedBlocks.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isExportingPdf = MutableStateFlow(false)
    val isExportingPdf: StateFlow<Boolean> = _isExportingPdf.asStateFlow()

    private val _lastExportedPdf = MutableStateFlow<File?>(null)
    val lastExportedPdf: StateFlow<File?> = _lastExportedPdf.asStateFlow()

    val samples: List<MdSampleDocument> = listOf(
        MdSampleDocument(
            title = "Panduan Fitur Aplikasi.md",
            description = "Ringkasan lengkap 6 modul utama Bimantara Multi-Tools",
            content = DEFAULT_MARKDOWN
        ),
        MdSampleDocument(
            title = "Spesifikasi Teknis CamScanner.md",
            description = "Dokumentasi algoritma dewarping & 4-corner frame lock",
            content = """
                # 📷 Spesifikasi Teknis CamScanner Pro
                
                Aplikasi ini dilengkapi algoritma **Koreksi Perspektif (Homography Dewarping)** berbasis *Android Native Matrix Poly-to-Poly*.
                
                ---
                
                ## ✨ Fitur Utama Scanner
                - **Kunci Bingkai Otomatis**: Mendeteksi 4 sudut tepi kertas dokumen.
                - **Interaksi Drag 4 Sudut**: Geser sudut manapun secara presisi dengan visual crosshair.
                - **Rasio Proporsional**:
                  - Dokumen Asli (Natural Euclidean Ratio)
                  - Standar A4 (1:1.414)
                  - KTP / ID Card (1.586:1)
                  - Format Surat / Letter
                - **Dewarping 4-Titik**: Mengubah foto miring atau terlipat menjadi datar dan tegak lurus.
                - **Penyimpanan Multi-Halaman**: Export dokumen multi-halaman ke PDF resolusi tinggi.
                
                ---
                
                ## ⚙️ Cuplikan Konfigurasi
                ```kotlin
                // Native Projective Homography
                val matrix = Matrix()
                val success = matrix.setPolyToPoly(srcCorners, 0, dstCorners, 0, 4)
                if (success) {
                    canvas.drawBitmap(original, matrix, paint)
                }
                ```
                
                > Hasil dokumen tersimpan rapi, bersih, dan berorientasi tegak lurus!
            """.trimIndent()
        ),
        MdSampleDocument(
            title = "Rencana Tugas & Catatan Tim.md",
            description = "Contoh dokumen task list, tabel, dan formatting checklist",
            content = """
                # 📋 Rencana Kerja Proyek & Task List
                
                Berikut adalah ringkasan progres dan target implementasi fitur.
                
                ---
                
                ### ✅ Checklist Pekerjaan
                - [x] Menu Home dengan ikon semua fitur
                - [x] Fitur .md Reader & Ekspor PDF
                - [x] CamScanner Auto Frame Lock & Dewarping
                - [x] Package com.bimantara
                - [ ] Uji coba multi-tab PDF Viewer
                
                ---
                
                ### 📊 Tabel Matriks Modul
                | Modul | Status | Format Ekspor |
                | --- | --- | --- |
                | File Explorer | Selesai | ZIP, Folder |
                | CamScanner Pro | Selesai | PDF, JPG |
                | MD Reader | Selesai | PDF A4 |
                | Quick Notes | Selesai | TXT, Audio |
                | Planner & Alarm | Selesai | Widget, Notifikasi |
                | Deep Cleaner | Selesai | Cache Report |
                
                ---
                
                > Bimantara Multi-Tools siap digunakan dengan performa optimal!
            """.trimIndent()
        )
    )

    init {
        parseMarkdown(_rawContent.value)
    }

    fun updateContent(newContent: String) {
        _rawContent.value = newContent
        parseMarkdown(newContent)
    }

    fun setDocumentTitle(title: String) {
        _documentTitle.value = title
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun setEditMode(edit: Boolean) {
        _isEditMode.value = edit
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadSample(sample: MdSampleDocument) {
        _documentTitle.value = sample.title
        updateContent(sample.content)
        _isEditMode.value = false
    }

    fun loadFromUri(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val text = BufferedReader(InputStreamReader(stream)).readText()
                    val fileName = getFileName(context, uri) ?: "Dokumen.md"
                    withContext(Dispatchers.Main) {
                        _documentTitle.value = fileName
                        updateContent(text)
                        _isEditMode.value = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
        return name
    }

    fun exportToPdf(context: Context, onComplete: (File) -> Unit) {
        viewModelScope.launch(Dispatchers.Default) {
            _isExportingPdf.value = true
            try {
                val file = MdPdfExporter.exportToPdf(
                    context = context,
                    rawMarkdown = _rawContent.value,
                    documentTitle = _documentTitle.value
                )
                _lastExportedPdf.value = file
                withContext(Dispatchers.Main) {
                    onComplete(file)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isExportingPdf.value = false
            }
        }
    }

    private fun parseMarkdown(content: String) {
        viewModelScope.launch(Dispatchers.Default) {
            val blocks = MarkdownParser.parse(content)
            _parsedBlocks.value = blocks
        }
    }

    companion object {
        val DEFAULT_MARKDOWN = """
            # 🚀 Bimantara Multi-Tools & MD Reader
            
            Selamat datang di **Bimantara MD Reader**, pembaca format Markdown profesional dengan fitur pratinjau instan dan **ekspor langsung ke dokumen PDF standar A4**.
            
            ---
            
            ## 📱 Modul Lengkap Tersedia
            Aplikasi Bimantara mengintegrasikan 6 alat produktivitas canggih:
            
            1. **Home Dashboard**: Akses cepat satu ketukan ke seluruh modul aplikasi.
            2. **File Explorer**: Kelola berkas lokal, penyimpanan internal, dan operasi berkas terpadu.
            3. **CamScanner Pro**: Pindai dokumen dengan **Koreksi Perspektif 4-Sudut**, perataan otomatis proporsional, dan multi-halaman PDF.
            4. **Markdown Reader & PDF**: Baca, edit, dan cetak dokumen *.md* langsung ke PDF profesional.
            5. **Catatan Cepat (Notes)**: Catat ide, tulisan tangan, serta perekaman suara.
            6. **Pengingat & Perencana (Planner)**: Jadwal tugas harian dengan alarm terintegrasi dan App Widget.
            7. **Pembersih Penyimpanan (Cleaner)**: Optimalkan memori RAM dan hapus berkas sampah.
            
            ---
            
            ## 💻 Contoh Blok Kode
            ```markdown
            # Format Judul H1
            **Teks Tebal** dan *Teks Miring*
            - [x] Fitur selesai diimplementasikan
            - [ ] Fitur berikutnya
            ```
            
            > **Tips**: Ketuk tombol **Ekspor PDF** di pojok kanan atas untuk mengonversi dokumen ini menjadi berkas PDF siap cetak atau dibagikan ke aplikasi lain!
        """.trimIndent()
    }
}

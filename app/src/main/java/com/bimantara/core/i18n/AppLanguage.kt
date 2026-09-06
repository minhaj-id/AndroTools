package com.bimantara.core.i18n

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class AppLanguage(
    val code: String,
    val displayName: String,
    val flagEmoji: String,
    val locale: Locale
) {
    INDONESIAN("in", "Bahasa Indonesia", "🇮🇩", Locale("in", "ID")),
    ENGLISH("en", "English", "🇬🇧", Locale.ENGLISH),
    SPANISH("es", "Español", "🇪🇸", Locale("es", "ES"))
}

data class AppStrings(
    // Common
    val appName: String,
    val language: String,
    val selectLanguage: String,
    val ok: String,
    val cancel: String,
    val delete: String,
    val save: String,
    val edit: String,
    val clear: String,
    val close: String,
    val search: String,
    val refresh: String,
    val back: String,
    val success: String,
    val error: String,
    val confirm: String,
    val warning: String,
    val items: String,

    // Navigation Tabs
    val tabExplorer: String,
    val tabScanner: String,
    val tabNotes: String,
    val tabPlanner: String,
    val tabCleaner: String,

    // File Manager
    val fmTitle: String,
    val fmDriveSubtitle: String,
    val fmNetworkSubtitle: String,
    val fmTabFiles: String,
    val fmTabNetwork: String,
    val fmNewFolder: String,
    val fmNewFolderPrompt: String,
    val fmFolderCreated: String,
    val fmFolderFailed: String,
    val fmSortBy: String,
    val fmSortName: String,
    val fmSortDate: String,
    val fmSortSize: String,
    val fmSortType: String,
    val fmSearchPlaceholder: String,
    val fmAdvancedSearch: String,
    val fmSearchInContent: String,
    val fmDateModified: String,
    val fmDateAny: String,
    val fmDateToday: String,
    val fmDate7Days: String,
    val fmDate30Days: String,
    val fmFileSize: String,
    val fmSizeAny: String,
    val fmSizeSmall: String,
    val fmSizeMedium: String,
    val fmSizeLarge: String,
    val fmMatchedInName: String,
    val fmMatchedInContent: String,
    val fmFoundFilesCount: String,
    val fmNoFilesFound: String,
    val fmPreviewTitle: String,
    val fmActionOpen: String,
    val fmActionDelete: String,
    val fmActionRename: String,
    val fmActionDetails: String,
    val fmNetworkTitle: String,
    val fmLocalIp: String,
    val fmGateway: String,
    val fmSubnet: String,
    val fmLinkSpeed: String,
    val fmScanNetwork: String,
    val fmScanning: String,
    val fmDiscoveredDevices: String,
    val fmPingTarget: String,

    // CamScanner
    val scanTitle: String,
    val scanSubtitle: String,
    val scanNoDocs: String,
    val scanPrompt: String,
    val scanCamera: String,
    val scanGallery: String,
    val filterMagicColor: String,
    val filterBw: String,
    val filterGrayscale: String,
    val filterOriginal: String,
    val rotate90: String,
    val ocrExtract: String,
    val ocrLoading: String,
    val ocrResultTitle: String,
    val pdfExport: String,
    val pdfExporting: String,
    val emailShare: String,
    val savePdfSuccess: String,

    // Notes
    val notesTitle: String,
    val notesSubtitle: String,
    val notesAll: String,
    val notesType: String,
    val notesStylus: String,
    val notesPinned: String,
    val notesSearchPlaceholder: String,
    val notesAdvancedSearch: String,
    val notesLengthFilter: String,
    val notesLengthAny: String,
    val notesLengthShort: String,
    val notesLengthMedium: String,
    val notesLengthLong: String,
    val notesDateFilter: String,
    val notesFoundCount: String,
    val notesNoNotesFound: String,
    val notesEmptyTitle: String,
    val notesEmptyDesc: String,
    val notesNewType: String,
    val notesNewStylus: String,
    val notesTitleHint: String,
    val notesContentHint: String,
    val notesVoiceInput: String,
    val notesPenSize: String,
    val notesEraser: String,
    val notesUndo: String,
    val notesClearCanvas: String,

    // Planner
    val planTitle: String,
    val planSubtitle: String,
    val planAddTask: String,
    val planPriorityHigh: String,
    val planPriorityMed: String,
    val planPriorityLow: String,
    val planCatWork: String,
    val planCatPersonal: String,
    val planCatStudy: String,
    val planCatUrgent: String,
    val planAll: String,
    val planToday: String,
    val planUpcoming: String,
    val planCompleted: String,
    val planTestAlarm: String,
    val planAlarmScheduled: String,

    // Cleaner
    val cleanTitle: String,
    val cleanSubtitle: String,
    val cleanTabJunk: String,
    val cleanTabRam: String,
    val cleanTabApk: String,
    val cleanScanJunk: String,
    val cleanCleanNow: String,
    val cleanBoostRam: String,
    val cleanRamAvail: String,
    val cleanRamUsed: String,
    val cleanApkBackup: String,
    val cleanApkShare: String
) {
    val navExplorer: String get() = tabExplorer
    val navScanner: String get() = tabScanner
    val navNotes: String get() = tabNotes
    val navPlanner: String get() = tabPlanner
    val navCleaner: String get() = tabCleaner

    val tabNetwork: String get() = fmTabNetwork
    val searchPlaceholder: String get() = fmSearchPlaceholder
    val fmFilterDate: String get() = fmDateModified
    val fmFilterSize: String get() = fmFileSize
    val fmFilterContent: String get() = fmSearchInContent
    val fmFoundFiles: String get() = fmFoundFilesCount
    val fmResetFilters: String get() = clear
    val actionClose: String get() = close
    val actionSave: String get() = save
    val actionAdd: String get() = if (appName == "Multi Tools") "Add Note" else "Tambah"

    val toolNotes: String get() = notesTitle
    val tabNotesType: String get() = notesType
    val tabNotesStylus: String get() = notesStylus
    val actionVoiceInput: String get() = notesVoiceInput
    val notesSearchHint: String get() = notesSearchPlaceholder
    val notesSizeShort: String get() = notesLengthShort
    val notesSizeMedium: String get() = notesLengthMedium
    val notesSizeLong: String get() = notesLengthLong
    val notesFilterAll: String get() = notesAll
    val notesEmpty: String get() = notesEmptyTitle
    val notesCharCount: String get() = items
    val notesMatchedInContent: String get() = fmMatchedInContent
    val notesTitleLabel: String get() = notesTitleHint

    val plannerTitle: String get() = planTitle
    val plannerSubtitle: String get() = planSubtitle
    val plannerAll: String get() = planAll
    val plannerToday: String get() = planToday
    val plannerUpcoming: String get() = planUpcoming
    val plannerCompleted: String get() = planCompleted
    val plannerAlarm: String get() = planTestAlarm
    val actionCancel: String get() = cancel

    val cleanerTitle: String get() = cleanTitle
    val cleanerSubtitle: String get() = cleanSubtitle
    val cleanerDeepClean: String get() = cleanTabJunk
    val cleanerRamBooster: String get() = cleanTabRam
    val cleanerApkBackup: String get() = cleanTabApk
    val backupSuccess: String get() = success
    val backupShare: String get() = cleanApkShare

    val cleanerDataBackup: String get() = when (language) {
        "English" -> "Data Backup (AES-256)"
        "Español" -> "Copia de Datos (AES-256)"
        else -> "Backup Data (AES-256)"
    }
    val backupWorkerTitle: String get() = when (language) {
        "English" -> "WorkManager Daily Maintenance"
        "Español" -> "Mantenimiento Diario WorkManager"
        else -> "WorkManager Pemeliharaan Harian"
    }
    val backupWorkerDesc: String get() = when (language) {
        "English" -> "Runs automatically every 24 hours. Performs AES-256-GCM encrypted database backup of Notes & Planner, plus cache cleanup."
        "Español" -> "Se ejecuta automáticamente cada 24 horas. Realiza una copia de seguridad cifrada con AES-256-GCM de Notas y Planificador, además de limpieza de caché."
        else -> "Berjalan otomatis setiap 24 jam. Mencadangkan database Catatan & Rencana dengan enkripsi AES-256-GCM dan membersihkan cache usang."
    }
    val backupRunWorkerNow: String get() = when (language) {
        "English" -> "Run WorkManager Now"
        "Español" -> "Ejecutar WorkManager Ahora"
        else -> "Jalankan WorkManager Sekarang"
    }
    val backupCreateNow: String get() = when (language) {
        "English" -> "Create Encrypted Backup"
        "Español" -> "Crear Copia Cifrada"
        else -> "Buat Cadangan Terenkripsi"
    }
    val backupEncryptedListTitle: String get() = when (language) {
        "English" -> "Encrypted Backups on Device"
        "Español" -> "Copias de Seguridad Cifradas"
        else -> "Daftar Cadangan Terenkripsi di Perangkat"
    }
    val backupRestoreAction: String get() = when (language) {
        "English" -> "Restore"
        "Español" -> "Restaurar"
        else -> "Pulihkan"
    }
    val backupRestoreConfirm: String get() = when (language) {
        "English" -> "Are you sure you want to restore data from this backup? Notes and planner tasks will be merged into your database."
        "Español" -> "¿Está seguro de que desea restaurar los datos de esta copia? Las notas y tareas se fusionarán en su base de datos."
        else -> "Apakah Anda yakin ingin memulihkan data dari cadangan ini? Catatan dan rencana akan digabungkan ke dalam database Anda."
    }
    val backupRestoredSuccess: String get() = when (language) {
        "English" -> "Data Restored Successfully!"
        "Español" -> "¡Datos restaurados con éxito!"
        else -> "Data Berhasil Dipulihkan!"
    }
    val scanDetectFrame: String get() = when (language) {
        "English" -> "Detect Frame"
        "Español" -> "Detectar Marco"
        else -> "Deteksi Bingkai"
    }
    val scanStraightenScale: String get() = when (language) {
        "English" -> "Straighten & Scale"
        "Español" -> "Enderezar y Escalar"
        else -> "Rapikan Skala & Bingkai"
    }
    val scanScaleRatio: String get() = when (language) {
        "English" -> "Document Scale Ratio"
        "Español" -> "Relación de Escala"
        else -> "Rasio Skala Dokumen"
    }
    val scanFrameDetectedSuccess: String get() = when (language) {
        "English" -> "Frame & scale straightened successfully!"
        "Español" -> "¡Marco y escala enderezados con éxito!"
        else -> "Bingkai & skala berhasil dirapikan!"
    }
    val scanTakePhoto: String get() = when (language) {
        "English" -> "Take Photo"
        "Español" -> "Tomar Foto"
        else -> "Ambil Foto"
    }
    val scanFrameDetectedBadge: String get() = when (language) {
        "English" -> "Frame Detected • 96% Precision"
        "Español" -> "Marco Detectado • 96% Precisión"
        else -> "Bingkai Terbaca • 96% Presisi"
    }
    val scanBatchTitle: String get() = when (language) {
        "English" -> "Batch Scan (Multi-Page)"
        "Español" -> "Escaneo por Lotes (Multipágina)"
        else -> "Pindai Batch (Multi-Halaman)"
    }
    val scanPickMultiple: String get() = when (language) {
        "English" -> "Pick Multiple Images"
        "Español" -> "Elegir Varias Imágenes"
        else -> "Pilih Banyak Gambar"
    }
    val scanAddPage: String get() = when (language) {
        "English" -> "Add Page"
        "Español" -> "Agregar Página"
        else -> "Tambah Halaman"
    }
    val scanAutoRotateAll: String get() = when (language) {
        "English" -> "Auto-Rotate All"
        "Español" -> "Auto-Rotar Todo"
        else -> "Rotasi Otomatis Semua"
    }
    val scanAutoRotatePdf: String get() = when (language) {
        "English" -> "Auto-Rotate to Portrait in PDF"
        "Español" -> "Auto-Rotar a Vertical en PDF"
        else -> "Auto-Rotasi ke Portrait saat Ekspor PDF"
    }
    val scanApplyAll: String get() = when (language) {
        "English" -> "Apply to All Pages"
        "Español" -> "Aplicar a Todas"
        else -> "Terapkan ke Semua Halaman"
    }
    val menuAbout: String get() = when (language) {
        "English" -> "About"
        "Español" -> "Acerca de"
        else -> "Tentang"
    }
}

val IndonesianStrings = AppStrings(
    appName = "Multi Tools",
    language = "Bahasa",
    selectLanguage = "Pilih Bahasa Aplikasi",
    ok = "OK",
    cancel = "Batal",
    delete = "Hapus",
    save = "Simpan",
    edit = "Edit",
    clear = "Bersihkan",
    close = "Tutup",
    search = "Cari",
    refresh = "Segarkan",
    back = "Kembali",
    success = "Berhasil",
    error = "Terjadi Kesalahan",
    confirm = "Konfirmasi",
    warning = "Peringatan",
    items = "item",

    tabExplorer = "Explorer",
    tabScanner = "Scanner",
    tabNotes = "Catatan",
    tabPlanner = "Planner",
    tabCleaner = "Cleaner",

    fmTitle = "File Manager Windows",
    fmDriveSubtitle = "Penjelajah Drive",
    fmNetworkSubtitle = "Penjelajah Jaringan & LAN",
    fmTabFiles = "File Explorer",
    fmTabNetwork = "Koneksi Jaringan",
    fmNewFolder = "Folder Baru",
    fmNewFolderPrompt = "Masukkan nama folder baru",
    fmFolderCreated = "Folder berhasil dibuat",
    fmFolderFailed = "Gagal membuat folder",
    fmSortBy = "Urutkan",
    fmSortName = "Berdasarkan Nama",
    fmSortDate = "Berdasarkan Tanggal",
    fmSortSize = "Berdasarkan Ukuran",
    fmSortType = "Berdasarkan Tipe",
    fmSearchPlaceholder = "Cari nama atau teks di file...",
    fmAdvancedSearch = "Pencarian Lanjutan",
    fmSearchInContent = "Cari konten teks di dalam berkas",
    fmDateModified = "Tanggal Modifikasi",
    fmDateAny = "Semua Waktu",
    fmDateToday = "Hari Ini (< 24 Jam)",
    fmDate7Days = "7 Hari Terakhir",
    fmDate30Days = "30 Hari Terakhir",
    fmFileSize = "Ukuran Berkas",
    fmSizeAny = "Semua Ukuran",
    fmSizeSmall = "Kecil (< 1 MB)",
    fmSizeMedium = "Sedang (1 - 10 MB)",
    fmSizeLarge = "Besar (> 10 MB)",
    fmMatchedInName = "Nama",
    fmMatchedInContent = "Konten Teks",
    fmFoundFilesCount = "Ditemukan %d berkas",
    fmNoFilesFound = "Tidak ada berkas yang cocok dengan kriteria pencarian",
    fmPreviewTitle = "Pratinjau Berkas",
    fmActionOpen = "Buka",
    fmActionDelete = "Hapus",
    fmActionRename = "Ubah Nama",
    fmActionDetails = "Rincian",
    fmNetworkTitle = "Diagnostik & Pemindai Jaringan LAN",
    fmLocalIp = "IP Lokal",
    fmGateway = "Gateway",
    fmSubnet = "Subnet",
    fmLinkSpeed = "Kecepatan Link",
    fmScanNetwork = "Pindai Perangkat LAN",
    fmScanning = "Memindai Jaringan...",
    fmDiscoveredDevices = "Perangkat Ditemukan",
    fmPingTarget = "Ping Alamat",

    scanTitle = "Dokumen CamScanner Pro",
    scanSubtitle = "Pindai, Sempurnakan & OCR Dokumen",
    scanNoDocs = "Belum Ada Dokumen",
    scanPrompt = "Ambil foto atau pilih dari galeri untuk memindai dokumen",
    scanCamera = "Kamera",
    scanGallery = "Galeri",
    filterMagicColor = "Warna Ajaib",
    filterBw = "Hitam Putih",
    filterGrayscale = "Skala Abu",
    filterOriginal = "Asli",
    rotate90 = "Putar 90°",
    ocrExtract = "Ekstrak Teks (OCR)",
    ocrLoading = "Menganalisis Teks Dokumen...",
    ocrResultTitle = "Hasil Ekstraksi OCR",
    pdfExport = "Simpan ke PDF",
    pdfExporting = "Membuat Berkas PDF...",
    emailShare = "Kirim E-mail",
    savePdfSuccess = "PDF tersimpan di folder Dokumen",

    notesTitle = "Notes with Stylus",
    notesSubtitle = "Mode Ketik, Tulis Stylus & Input Suara",
    notesAll = "Semua",
    notesType = "Mode Ketik",
    notesStylus = "Tulis Stylus",
    notesPinned = "Disematkan",
    notesSearchPlaceholder = "Cari catatan berdasarkan judul, isi, tag...",
    notesAdvancedSearch = "Pencarian Lanjutan",
    notesLengthFilter = "Panjang Catatan",
    notesLengthAny = "Semua Panjang",
    notesLengthShort = "Pendek (< 100 kar)",
    notesLengthMedium = "Sedang (100 - 500 kar)",
    notesLengthLong = "Panjang (> 500 kar)",
    notesDateFilter = "Waktu Catatan",
    notesFoundCount = "Ditemukan %d catatan",
    notesNoNotesFound = "Tidak ada catatan yang cocok dengan filter pencarian",
    notesEmptyTitle = "Belum Ada Catatan",
    notesEmptyDesc = "Buat catatan baru dengan mengetik atau menggambar stylus",
    notesNewType = "Catatan Baru",
    notesNewStylus = "Tulis Stylus",
    notesTitleHint = "Judul Catatan...",
    notesContentHint = "Tuliskan catatan di sini...",
    notesVoiceInput = "Dikte Suara",
    notesPenSize = "Ukuran Pena",
    notesEraser = "Penghapus",
    notesUndo = "Urungkan",
    notesClearCanvas = "Hapus Semua",

    planTitle = "Planner & To-Do",
    planSubtitle = "Manajemen Agenda & Alarm Notifikasi",
    planAddTask = "Tambah Agenda",
    planPriorityHigh = "Tinggi",
    planPriorityMed = "Sedang",
    planPriorityLow = "Rendah",
    planCatWork = "Pekerjaan",
    planCatPersonal = "Pribadi",
    planCatStudy = "Belajar",
    planCatUrgent = "Mendesak",
    planAll = "Semua",
    planToday = "Hari Ini",
    planUpcoming = "Mendatang",
    planCompleted = "Selesai",
    planTestAlarm = "Uji Alarm Notifikasi",
    planAlarmScheduled = "Alarm pengingat telah disetel",

    cleanTitle = "Cleaner & Optimizer",
    cleanSubtitle = "Pembersih Sampah, RAM Booster & Backup APK",
    cleanTabJunk = "Pembersih Sampah",
    cleanTabRam = "Booster Memori",
    cleanTabApk = "Backup APK",
    cleanScanJunk = "Pindai Sampah",
    cleanCleanNow = "Bersihkan Sekarang",
    cleanBoostRam = "Kosongkan RAM",
    cleanRamAvail = "Tersedia",
    cleanRamUsed = "Digunakan",
    cleanApkBackup = "Cadangkan APK",
    cleanApkShare = "Bagikan APK"
)

val EnglishStrings = AppStrings(
    appName = "Multi Tools",
    language = "Language",
    selectLanguage = "Select App Language",
    ok = "OK",
    cancel = "Cancel",
    delete = "Delete",
    save = "Save",
    edit = "Edit",
    clear = "Clear",
    close = "Close",
    search = "Search",
    refresh = "Refresh",
    back = "Back",
    success = "Success",
    error = "Error",
    confirm = "Confirm",
    warning = "Warning",
    items = "items",

    tabExplorer = "Explorer",
    tabScanner = "Scanner",
    tabNotes = "Notes",
    tabPlanner = "Planner",
    tabCleaner = "Cleaner",

    fmTitle = "Windows File Manager",
    fmDriveSubtitle = "Drive Explorer",
    fmNetworkSubtitle = "Network & LAN Explorer",
    fmTabFiles = "File Explorer",
    fmTabNetwork = "Network Connection",
    fmNewFolder = "New Folder",
    fmNewFolderPrompt = "Enter new folder name",
    fmFolderCreated = "Folder created successfully",
    fmFolderFailed = "Failed to create folder",
    fmSortBy = "Sort",
    fmSortName = "Sort by Name",
    fmSortDate = "Sort by Date",
    fmSortSize = "Sort by Size",
    fmSortType = "Sort by Type",
    fmSearchPlaceholder = "Search file name or content...",
    fmAdvancedSearch = "Advanced Search",
    fmSearchInContent = "Search inside file text content",
    fmDateModified = "Date Modified",
    fmDateAny = "Any Time",
    fmDateToday = "Today (< 24 Hours)",
    fmDate7Days = "Last 7 Days",
    fmDate30Days = "Last 30 Days",
    fmFileSize = "File Size",
    fmSizeAny = "Any Size",
    fmSizeSmall = "Small (< 1 MB)",
    fmSizeMedium = "Medium (1 - 10 MB)",
    fmSizeLarge = "Large (> 10 MB)",
    fmMatchedInName = "Name",
    fmMatchedInContent = "Text Content",
    fmFoundFilesCount = "Found %d files",
    fmNoFilesFound = "No files matched your search criteria",
    fmPreviewTitle = "File Preview",
    fmActionOpen = "Open",
    fmActionDelete = "Delete",
    fmActionRename = "Rename",
    fmActionDetails = "Details",
    fmNetworkTitle = "LAN Network Diagnostics & Scanner",
    fmLocalIp = "Local IP",
    fmGateway = "Gateway",
    fmSubnet = "Subnet",
    fmLinkSpeed = "Link Speed",
    fmScanNetwork = "Scan LAN Devices",
    fmScanning = "Scanning Network...",
    fmDiscoveredDevices = "Discovered Devices",
    fmPingTarget = "Ping Address",

    scanTitle = "CamScanner Pro Documents",
    scanSubtitle = "Scan, Enhance & OCR Documents",
    scanNoDocs = "No Documents Yet",
    scanPrompt = "Take photo or select from gallery to scan document",
    scanCamera = "Camera",
    scanGallery = "Gallery",
    filterMagicColor = "Magic Color",
    filterBw = "B&W Document",
    filterGrayscale = "Grayscale",
    filterOriginal = "Original",
    rotate90 = "Rotate 90°",
    ocrExtract = "Extract Text (OCR)",
    ocrLoading = "Analyzing Document Text...",
    ocrResultTitle = "OCR Extraction Result",
    pdfExport = "Save as PDF",
    pdfExporting = "Generating PDF File...",
    emailShare = "Send Email",
    savePdfSuccess = "PDF saved in Documents folder",

    notesTitle = "Notes with Stylus",
    notesSubtitle = "Type Mode, Stylus Canvas & Voice Input",
    notesAll = "All",
    notesType = "Type Mode",
    notesStylus = "Stylus Canvas",
    notesPinned = "Pinned",
    notesSearchPlaceholder = "Search notes by title, content, tag...",
    notesAdvancedSearch = "Advanced Search",
    notesLengthFilter = "Note Length",
    notesLengthAny = "Any Length",
    notesLengthShort = "Short (< 100 chars)",
    notesLengthMedium = "Medium (100 - 500 chars)",
    notesLengthLong = "Long (> 500 chars)",
    notesDateFilter = "Note Time",
    notesFoundCount = "Found %d notes",
    notesNoNotesFound = "No notes matched your search filter",
    notesEmptyTitle = "No Notes Yet",
    notesEmptyDesc = "Create a new note by typing or drawing with stylus",
    notesNewType = "New Note",
    notesNewStylus = "Stylus Note",
    notesTitleHint = "Note Title...",
    notesContentHint = "Write your note here...",
    notesVoiceInput = "Voice Input",
    notesPenSize = "Pen Size",
    notesEraser = "Eraser",
    notesUndo = "Undo",
    notesClearCanvas = "Clear All",

    planTitle = "Planner & To-Do",
    planSubtitle = "Schedule & Notification Alarms",
    planAddTask = "Add Task",
    planPriorityHigh = "High",
    planPriorityMed = "Medium",
    planPriorityLow = "Low",
    planCatWork = "Work",
    planCatPersonal = "Personal",
    planCatStudy = "Study",
    planCatUrgent = "Urgent",
    planAll = "All",
    planToday = "Today",
    planUpcoming = "Upcoming",
    planCompleted = "Completed",
    planTestAlarm = "Test Notification Alarm",
    planAlarmScheduled = "Reminder alarm scheduled",

    cleanTitle = "Cleaner & Optimizer",
    cleanSubtitle = "Junk Cleaner, RAM Booster & APK Backup",
    cleanTabJunk = "Junk Cleaner",
    cleanTabRam = "Memory Booster",
    cleanTabApk = "APK Backup",
    cleanScanJunk = "Scan Junk",
    cleanCleanNow = "Clean Now",
    cleanBoostRam = "Free RAM",
    cleanRamAvail = "Available",
    cleanRamUsed = "Used",
    cleanApkBackup = "Backup APK",
    cleanApkShare = "Share APK"
)

val SpanishStrings = AppStrings(
    appName = "Multi Tools",
    language = "Idioma",
    selectLanguage = "Seleccionar Idioma de la Aplicación",
    ok = "Aceptar",
    cancel = "Cancelar",
    delete = "Eliminar",
    save = "Guardar",
    edit = "Editar",
    clear = "Limpiar",
    close = "Cerrar",
    search = "Buscar",
    refresh = "Actualizar",
    back = "Atrás",
    success = "Éxito",
    error = "Error",
    confirm = "Confirmar",
    warning = "Advertencia",
    items = "elementos",

    tabExplorer = "Explorador",
    tabScanner = "Escáner",
    tabNotes = "Notas",
    tabPlanner = "Planificador",
    tabCleaner = "Limpiador",

    fmTitle = "Explorador de Archivos Windows",
    fmDriveSubtitle = "Explorador de Unidad",
    fmNetworkSubtitle = "Explorador de Red y LAN",
    fmTabFiles = "Explorador de Archivos",
    fmTabNetwork = "Conexión de Red",
    fmNewFolder = "Nueva Carpeta",
    fmNewFolderPrompt = "Ingrese el nombre de la nueva carpeta",
    fmFolderCreated = "Carpeta creada con éxito",
    fmFolderFailed = "Error al crear la carpeta",
    fmSortBy = "Ordenar",
    fmSortName = "Ordenar por Nombre",
    fmSortDate = "Ordenar por Fecha",
    fmSortSize = "Ordenar por Tamaño",
    fmSortType = "Ordenar por Tipo",
    fmSearchPlaceholder = "Buscar archivo por nombre o contenido...",
    fmAdvancedSearch = "Búsqueda Avanzada",
    fmSearchInContent = "Buscar contenido de texto dentro del archivo",
    fmDateModified = "Fecha de Modificación",
    fmDateAny = "Cualquier Momento",
    fmDateToday = "Hoy (< 24 Horas)",
    fmDate7Days = "Últimos 7 Días",
    fmDate30Days = "Últimos 30 Días",
    fmFileSize = "Tamaño de Archivo",
    fmSizeAny = "Cualquier Tamaño",
    fmSizeSmall = "Pequeño (< 1 MB)",
    fmSizeMedium = "Mediano (1 - 10 MB)",
    fmSizeLarge = "Grande (> 10 MB)",
    fmMatchedInName = "Nombre",
    fmMatchedInContent = "Texto de Contenido",
    fmFoundFilesCount = "Se encontraron %d archivos",
    fmNoFilesFound = "No se encontraron archivos con ese criterio",
    fmPreviewTitle = "Vista Previa de Archivo",
    fmActionOpen = "Abrir",
    fmActionDelete = "Eliminar",
    fmActionRename = "Renombrar",
    fmActionDetails = "Detalles",
    fmNetworkTitle = "Diagnóstico y Escáner de Red LAN",
    fmLocalIp = "IP Local",
    fmGateway = "Puerta de Enlace",
    fmSubnet = "Máscara de Subred",
    fmLinkSpeed = "Velocidad de Enlace",
    fmScanNetwork = "Escanear Dispositivos LAN",
    fmScanning = "Escaneando Red...",
    fmDiscoveredDevices = "Dispositivos Encontrados",
    fmPingTarget = "Dirección de Ping",

    scanTitle = "Documentos CamScanner Pro",
    scanSubtitle = "Escanear, Mejorar y OCR de Documentos",
    scanNoDocs = "Aún No Hay Documentos",
    scanPrompt = "Tome una foto o seleccione de la galería para escanear",
    scanCamera = "Cámara",
    scanGallery = "Galería",
    filterMagicColor = "Color Mágico",
    filterBw = "Documento B/N",
    filterGrayscale = "Escala de Grises",
    filterOriginal = "Original",
    rotate90 = "Rotar 90°",
    ocrExtract = "Extraer Texto (OCR)",
    ocrLoading = "Analizando Texto del Documento...",
    ocrResultTitle = "Resultado de Extracción OCR",
    pdfExport = "Guardar como PDF",
    pdfExporting = "Generando Archivo PDF...",
    emailShare = "Enviar Correo",
    savePdfSuccess = "PDF guardado en la carpeta Documentos",

    notesTitle = "Notas con Stylus",
    notesSubtitle = "Modo Escritura, Lienzo Stylus y Entrada de Voz",
    notesAll = "Todas",
    notesType = "Modo Escritura",
    notesStylus = "Lienzo Stylus",
    notesPinned = "Fijadas",
    notesSearchPlaceholder = "Buscar notas por título, contenido, etiqueta...",
    notesAdvancedSearch = "Búsqueda Avanzada",
    notesLengthFilter = "Longitud de Nota",
    notesLengthAny = "Cualquier Longitud",
    notesLengthShort = "Corta (< 100 car)",
    notesLengthMedium = "Media (100 - 500 car)",
    notesLengthLong = "Larga (> 500 car)",
    notesDateFilter = "Fecha de Nota",
    notesFoundCount = "Se encontraron %d notas",
    notesNoNotesFound = "No se encontraron notas con el filtro de búsqueda",
    notesEmptyTitle = "Aún No Hay Notas",
    notesEmptyDesc = "Cree una nueva nota escribiendo o dibujando con el stylus",
    notesNewType = "Nueva Nota",
    notesNewStylus = "Nota Stylus",
    notesTitleHint = "Título de la Nota...",
    notesContentHint = "Escriba su nota aquí...",
    notesVoiceInput = "Entrada de Voz",
    notesPenSize = "Grosor del Lápiz",
    notesEraser = "Borrador",
    notesUndo = "Deshacer",
    notesClearCanvas = "Limpiar Todo",

    planTitle = "Planificador y Tareas",
    planSubtitle = "Gestión de Agenda y Alarmas de Notificación",
    planAddTask = "Agregar Tarea",
    planPriorityHigh = "Alta",
    planPriorityMed = "Media",
    planPriorityLow = "Baja",
    planCatWork = "Trabajo",
    planCatPersonal = "Personal",
    planCatStudy = "Estudio",
    planCatUrgent = "Urgente",
    planAll = "Todas",
    planToday = "Hoy",
    planUpcoming = "Próximas",
    planCompleted = "Completadas",
    planTestAlarm = "Probar Alarma de Notificación",
    planAlarmScheduled = "Alarma de recordatorio programada",

    cleanTitle = "Limpiador y Optimizador",
    cleanSubtitle = "Limpiador de Basura, RAM Booster y Respaldo APK",
    cleanTabJunk = "Limpiador de Basura",
    cleanTabRam = "Booster de Memoria",
    cleanTabApk = "Respaldo APK",
    cleanScanJunk = "Escanear Basura",
    cleanCleanNow = "Limpiar Ahora",
    cleanBoostRam = "Liberar RAM",
    cleanRamAvail = "Disponible",
    cleanRamUsed = "Usado",
    cleanApkBackup = "Respaldar APK",
    cleanApkShare = "Compartir APK"
)

val LocalAppStrings = compositionLocalOf { IndonesianStrings }

object AppLanguageManager {
    private const val PREFS_NAME = "multi_tools_prefs"
    private const val KEY_LANG = "app_language"

    private val _currentLanguage = MutableStateFlow(AppLanguage.INDONESIAN)
    val currentLanguage: StateFlow<AppLanguage> = _currentLanguage.asStateFlow()

    private val _strings = MutableStateFlow(IndonesianStrings)
    val strings: StateFlow<AppStrings> = _strings.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedLangCode = prefs.getString(KEY_LANG, AppLanguage.INDONESIAN.code)
        val matchedLang = AppLanguage.entries.find { it.code == savedLangCode } ?: AppLanguage.INDONESIAN
        setLanguage(matchedLang, context, save = false)
    }

    fun setLanguage(context: Context, language: AppLanguage, save: Boolean = true) {
        setLanguage(language, context, save)
    }

    fun setLanguage(language: AppLanguage, context: Context, save: Boolean = true) {
        _currentLanguage.value = language
        _strings.value = when (language) {
            AppLanguage.INDONESIAN -> IndonesianStrings
            AppLanguage.ENGLISH -> EnglishStrings
            AppLanguage.SPANISH -> SpanishStrings
        }

        if (save) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LANG, language.code).apply()
        }

        // Apply locale system-wide
        try {
            Locale.setDefault(language.locale)
            val config = context.resources.configuration
            config.setLocale(language.locale)
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
        } catch (e: Exception) {
            // ignore
        }
    }
}

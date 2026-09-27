# AndroTools 🛠️📱

**AndroTools** adalah aplikasi produktivitas dan utilitas serba bisa (*all-in-one productivity & utility suite*) untuk sistem operasi Android. Dibangun secara modern menggunakan **Kotlin** dan **Jetpack Compose** dengan desain **Material 3 (M3)** yang elegan, responsif, dan mendukung penggunaan *offline-first*.

Aplikasi ini mengintegrasikan lima modul utama: **File Explorer** bergaya desktop, **CamScanner** pemindai dokumen cerdas dengan OCR & deteksi bingkai, **Catatan & Kanvas Stylus**, **Perencana Tugas (Planner)** dengan alarm terjadwal & widget layar utama, serta **Deep Cleaner & Backup Data Terenkripsi**.

---

## 🌟 Modul & Fitur Utama

### 1. 📂 File Explorer (Pengelola Berkas Windows-Style)
- **Navigasi Berkas Lengkap**: Tampilan navigasi hirarkis dengan breadcrumbs interaktif ala desktop.
- **Operasi Berkas**: Membuat folder, mengganti nama, menyalin, memindahkan, dan menghapus berkas.
- **Penganalisis Penyimpanan (Storage Analyzer)**: Visualisasi ruang penyimpanan internal yang terpakai dan tersedia.
- **Jelajah Jaringan/Awan**: Menjelajah direktori berkas lokal dan simulasi integrasi cloud/network explorer.
- **Filter Berdasarkan Tipe**: Pemilahan instan dokumen, gambar, audio, video, dan arsip.

### 2. 📷 CamScanner Pro (Pemindai Dokumen Cerdas & Batch Multi-Halaman)
- **Pemindaian Batch (Multi-Page Batch Scanning)**:
  - Mendukung pengambilan dan pengimporan banyak halaman dokumen sekaligus (*batch capture*).
  - Carousel strip thumbnail interaktif untuk melihat, memilih, mengatur urutan (geser kiri/kanan), dan menambah/menghapus halaman dokumen.
- **Rotasi Otomatis & Standarisasi Dokumen (Auto-Page Rotation)**:
  - Deteksi orientasi cerdas (`AutoPageRotator`) untuk menormalkan halaman mendatar (*landscape*) menjadi tegak lurus (*portrait*).
  - Tombol **Rotasi Otomatis Semua Halaman** sekali klik.
  - Sakelar **Auto-Rotate ke Portrait saat Ekspor PDF** untuk menjamin seluruh halaman berorientasi tegak seragam tanpa teks terbalik atau miring.
- **Kamera & Galeri Terintegrasi**: Pengambilan foto dokumen secara langsung via kamera perangkat menggunakan `FileProvider` beresolusi penuh atau memilih dari galeri (tunggal maupun multi-berkas).
- **Deteksi Bingkai Otomatis (Document Frame Edge Detection)**: Analisis gradien kontras tepi dokumen secara otomatis untuk mendeteksi 4 titik sudut batas kertas dokumen.
- **Overlay & Koreksi Sudut Interaktif**: Garis tepi hijau dan pin sudut interaktif yang dapat disembunyikan atau ditampilkan.
- **Perapian Skala & Rasio Dokumen**:
  - Pilihan rasio aspek standar: **Otomatis (Auto)**, **A4**, **Letter**, **Kartu ID / KTP**, dan **Persegi (1:1)**.
  - Tombol **Rapikan Skala** sebelum menyimpan untuk memastikan dokumen tegak lurus dan simetris.
- **Filter Peningkatan Gambar**:
  - *Original*, *B&W (Hitam Putih)*, *Magic Color*, dan *Grayscale*.
  - Opsi **Terapkan ke Semua Halaman** untuk menyeragamkan filter seluruh halaman dalam batch.
- **Mesin OCR (Optical Character Recognition) On-Device**: Ekstraksi teks dari gambar dokumen secara instan tanpa perlu koneksi internet per halaman dengan tombol salin teks.
- **Ekspor PDF Multi-Halaman & Berbagi Langsung**: Konversi seluruh rangkaian halaman dokumen menjadi satu file PDF standar yang rapi dan bagikan via Email, WhatsApp, atau aplikasi perpesanan lainnya.
- **Komponen Pratinjau PDF Berdedikasi (Dedicated Multi-Tab PDF Viewer)**:
  - **Pratinjau Ringan & Cepat**: Menggunakan `android.graphics.pdf.PdfRenderer` bawaan Android untuk rendering PDF perangkat keras tanpa beban pustaka pihak ketiga yang berat.
  - **Dukungan Multi-Tab (Multi-Tab PDF Navigation)**: Membuka beberapa dokumen PDF secara bersamaan dalam tab terpisah. Pengguna dapat beralih antar tab berkas, menutup tab, atau menambah dokumen lain dengan cepat.
  - **Navigasi & Loncat Halaman**: Tombol navigasi halaman sebelumnya/selanjutnya, indikator halaman dinamis, serta dialog lompat ke halaman spesifik (*jump to page*).
  - **Kontrol Zoom & Panning Gesture Interaktif**: Mendukung cubit untuk zoom (*pinch-to-zoom* hingga 400%), geser posisi (*pan*), ketuk ganda untuk reset zoom (*double-tap to reset*), serta tombol zoom in / zoom out / fit-to-screen.
  - **Carousel Strip Thumbnail Halaman**: Panel pratinjau thumbnail halaman interaktif di bagian bawah layar yang dapat diperluas atau disembunyikan untuk melompat antar halaman secara visual.
  - **Fitur Berbagi Berkas & Info PDF**: Berbagi berkas PDF yang sedang dibuka dan melihat rincian metadata dokumen (nama berkas, total halaman, ukuran byte, lokasi penyimpanan).
- **Penyimpanan Lokal**: Gambar dan metadata dokumen tersimpan aman di basis data lokal Room.

### 3. 📝 Catatan Cerdas & Kanvas Stylus (Smart Notes)
- **Catatan Teks Kaya**: Buat catatan penting dengan penandaan warna, tag kustom, dan fitur pin/sematkan ke bagian teratas.
- **Kanvas Gambar Bebas (Stylus / Handwriting Canvas)**:
  - Dukungan sentuhan jari dan pena stylus dengan respon cepat (*smooth bezier path drawing*).
  - Pilihan ketebalan goresan kuas, palet warna dinamis, undo langkah coretan, dan bersihkan kanvas.
- **Input Suara (Voice-to-Text)**: Diktekan ide dan teks catatan secara langsung lewat mikrofon perangkat.
- **Pencarian Cepat**: Temukan catatan berdasarkan judul atau isi teks secara instan.

### 4. 📅 Perencana Jadwal & Tugas (Daily Planner & Reminders)
- **Manajemen Tugas & Agenda**: Atur tugas dengan kategori (Kerja, Pribadi, Belajar, Mendesak) dan tingkat prioritas (Tinggi, Sedang, Rendah).
- **Pengingat Alarm & Notifikasi Tepat Waktu**:
  - Menggunakan `AlarmManager` dengan `SCHEDULE_EXACT_ALARM` dan `PlannerAlarmReceiver`.
  - Notifikasi suara dan getar yang muncul tepat pada waktu yang dijadwalkan.
- **Input Jadwal Berbasis Suara**: Buat agenda baru secara cepat menggunakan input pengenalan ucapan.
- **Widget Layar Utama (Android App Widget)**:
  - `PlannerWidgetProvider` menampilkan daftar tugas mendatang langsung pada beranda perangkat (*Homescreen*).
  - Dilengkapi tombol pintasan aksi cepat untuk membuat catatan kilat (*Quick Note*) dan membuka aplikasi.

### 5. 🧹 Pembersih Penyimpanan & Cadangan Terenkripsi (Deep Cleaner & Backup)
- **Pembersih Sampah**: Memindai dan membersihkan file cache sisa, file duplikat, dan file berukuran besar (*Large Files Finder*).
- **Pencadangan APK (APK Extractor)**: Ekstraksi dan pencadangan paket instalasi APK dari aplikasi yang terpasang di perangkat.
- **Cadangan Data Terenkripsi (Encrypted Backup & Restore)**:
  - Enkripsi kuat **AES-GCM (256-bit)** melalui modul `BackupCrypto`.
  - Ekspor seluruh data catatan dan rencana harian dalam format aman `.androtools.bak`.
  - Fitur pemulihan data (*restore*) kapan saja dengan validasi integritas payload.
- **Pemeliharaan Latar Belakang Otomatis**: Integrasi `WorkManager` (`DailyMaintenanceWorker`) untuk jadwal pemeliharaan rutin harian secara senyap dan hemat daya.

### 6. 🌐 Multi-Bahasa (Internationalization / i18n)
- Mendukung pergantian bahasa secara dinamis tanpa perlu me-restart aplikasi:
  - 🇮🇩 **Bahasa Indonesia**
  - 🇬🇧 **English**
  - 🇪🇸 **Español**

---

## 🏗️ Arsitektur & Teknologi (Tech Stack)

AndroTools dirancang mengikuti prinsip **Clean Architecture & MVVM (Model-View-ViewModel)** yang modular dan mudah diuji:

| Komponen | Teknologi / Pustaka |
|---|---|
| **Bahasa Pemrograman** | [Kotlin](https://kotlinlang.org/) 100% |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) dengan [Material Design 3 (M3)](https://m3.material.io/) |
| **Basis Data Lokal** | [Room Database](https://developer.android.com/training/data-storage/room) v2.7.1 dengan KSP (Kotlin Symbol Processing) |
| **State Management** | Android Jetpack `ViewModel`, `StateFlow`, `collectAsStateWithLifecycle` |
| **Pekerjaan Latar Belakang** | [AndroidX WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) |
| **Pemrosesan Dokumen** | Android Native `PdfDocument`, `Bitmap` & `Canvas` Matrix Manipulation |
| **Kriptografi & Keamanan** | Java Cryptography Extension (JCE) dengan `AES/GCM/NoPadding` |
| **Widget** | Android AppWidget Provider & RemoteViewsFactory |
| **Testing** | Robolectric & JUnit untuk pengujian unit di JVM lokal |

---

## 📁 Struktur Direktori Proyek

```
app/src/main/java/com/bimantara/
├── MainActivity.kt                 # Entri utama aplikasi & navigasi bawah (Bottom Navigation)
├── core/
│   ├── i18n/                       # Pengelola bahasa (AppLanguageManager, dialog, resource string)
│   ├── security/                   # Enkripsi & dekripsi cadangan data (BackupCrypto)
│   └── worker/                     # Penjadwalan tugas harian WorkManager (DailyMaintenanceWorker)
├── data/
│   ├── backup/                     # Manajemen backup data, model payload, dan file cadangan
│   ├── db/                         # Room Database (AppDatabase, NoteDao, PlannerDao, DocDao)
│   ├── model/                      # Entitas database (NoteEntity, PlannerItemEntity, ScannedDocEntity)
│   └── repository/                 # Repository layer yang menjembatani Room dengan ViewModel
├── feature/
│   ├── cleaner/                    # Layar pembersih penyimpanan, cadangan APK, dan backup terenkripsi
│   ├── filemanager/                # File Explorer Windows-style, storage analyzer & network explorer
│   ├── notes/                      # Catatan teks, input suara, dan kanvas gambar tangan Stylus
│   ├── planner/                    # Rencana tugas harian, alarm receiver, dan sistem notifikasi
│   └── scanner/                    # CamScanner: Kamera, deteksi bingkai, koreksi skala, filter, OCR & PDF
├── ui/
│   └── theme/                      # Definisi tema Material 3 (Color, Typography, Shape, Theme)
└── widget/
    ├── PlannerWidgetProvider.kt    # Provider widget beranda Android
    └── PlannerWidgetService.kt     # Factory item RemoteViews untuk widget beranda
```

---

## 🔒 Izin Sistem (Permissions Declared)

Aplikasi mendeklarasikan izin yang diperlukan secara transparan dan meminta izin runtime sesuai standar Google Play:

- `android.permission.CAMERA`: Mengambil foto dokumen langsung melalui kamera.
- `android.hardware.camera`: Fitur opsional untuk perangkat berkamera.
- `android.permission.POST_NOTIFICATIONS`: Menampilkan pengingat alarm tugas (Android 13+).
- `android.permission.SCHEDULE_EXACT_ALARM`: Menjadwalkan alarm tepat waktu untuk jadwal tugas.
- `android.permission.RECORD_AUDIO`: Masukan suara untuk catatan kilat dan tugas baru.
- `android.permission.VIBRATE`: Efek getar saat alarm tugas berbunyi.
- `android.permission.READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE`: Akses pengelola berkas pada sistem lama (dengan penanganan scoped storage Android 10+).

---

## 🚀 Panduan Build & Pengujian

### Prasyarat
- **Android SDK**: compileSdk 35, targetSdk 35, minSdk 26
- **JDK**: Java 17 atau Java 21
- **Gradle**: 8.x+ (didukung Kotlin DSL)

### Perintah Gradle

1. **Kompilasi Proyek**:
   ```bash
   gradle :app:assembleDebug
   ```

2. **Menjalankan Unit Test & Robolectric**:
   ```bash
   gradle :app:testDebugUnitTest
   ```

---

## 👨‍💻 Informasi Pengembang (Author)

Aplikasi ini dikembangkan dan dirawat dengan dedikasi oleh:
- **Nama Pengembang**: Nur Sahid, S.P,, M.Kom.
- **Situs Web Resmi**: [https://nursahid.com](https://nursahid.com)
- **Organisasi / Tim Pengembang**: Bimantara Dev (`com.bimantara`)

---

## 💖 Donasi & Dukungan Pengembangan

Bagi Anda yang ingin berpartisipasi dan mendukung pengembangan fitur-fitur baru aplikasi AndroTools, Anda dapat memberikan donasi sukarela melalui:

- **Transfer Bank**:
  - **Bank**: Bank Jago
  - **No. Rekening**: `104718633346`
  - **Atas Nama**: Bimantara Dev
- **Donasi Online**:
  - [https://nursahid.com/projects/androtools#donation-section](https://nursahid.com/projects/androtools#donation-section)

Terima kasih sebesar-besarnya atas setiap apresiasi dan dukungan Anda untuk kelanjutan proyek ini!

---

## 📄 Lisensi & Hak Cipta

Dikembangkan untuk ekosistem Android modern oleh **Bimantara** (`com.bimantara`).
Dibuat dengan dedikasi untuk performa tinggi, kebersihan kode, dan kemudahan produktivitas harian pengguna.

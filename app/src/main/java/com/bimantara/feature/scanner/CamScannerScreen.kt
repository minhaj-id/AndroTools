package com.bimantara.feature.scanner

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.hypot
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import com.bimantara.feature.scanner.ocr.OcrLanguageDialog
import com.bimantara.feature.scanner.ocr.OcrLanguage
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.bimantara.core.i18n.AppLanguageManager
import com.bimantara.core.i18n.AppLanguageSelectionDialog
import com.bimantara.core.i18n.LocalAppStrings
import com.bimantara.data.model.ScannedDocEntity
import com.bimantara.feature.scanner.camera.CameraScannerScreen
import com.bimantara.feature.scanner.camera.CameraViewModel
import com.bimantara.ui.about.AboutDialog
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.FileOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CamScannerScreen(
    viewModel: CamScannerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewMode by viewModel.viewMode.collectAsState()
    val scannedDocs by viewModel.scannedDocs.collectAsState()
    val openPdfTabs by viewModel.openPdfTabs.collectAsState()

    var photoCapturePath by remember { mutableStateOf<String?>(null) }
    var isAppendingPage by remember { mutableStateOf(false) }

    // Camera picture capture launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val path = photoCapturePath
        if (success && path != null) {
            viewModel.loadBitmapFromFile(path, context, append = isAppendingPage)
        }
    }

    // Camera runtime permission request
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val photoFile = File(context.cacheDir, "cam_scan_${System.currentTimeMillis()}.jpg")
                photoCapturePath = photoFile.absolutePath
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                cameraLauncher.launch(photoUri)
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal meluncurkan kamera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin kamera diperlukan untuk memindai dokumen", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera(append: Boolean = false) {
        isAppendingPage = append
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            try {
                val photoFile = File(context.cacheDir, "cam_scan_${System.currentTimeMillis()}.jpg")
                photoCapturePath = photoFile.absolutePath
                val photoUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    photoFile
                )
                cameraLauncher.launch(photoUri)
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal meluncurkan kamera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Single photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.loadBitmapFromUri(uri, context, append = isAppendingPage)
        }
    }

    // Multiple photo picker launcher for batch scanning
    val multiPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 25)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.loadBitmapsFromUris(uris, context, append = isAppendingPage)
        }
    }

    when (viewMode) {
        ScannerViewMode.GALLERY -> {
            DocGalleryView(
                docs = scannedDocs,
                onOpenDoc = { doc ->
                    val file = File(doc.imagePath)
                    if (file.exists()) {
                        val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        if (bmp != null) {
                            viewModel.openEditor(bmp, doc.title)
                            viewModel.setOcrText(doc.extractedText)
                        }
                    }
                },
                onOpenPdf = { doc ->
                    viewModel.openPdfViewerFromDocEntity(doc)
                },
                onDeleteDoc = { viewModel.deleteDoc(it) },
                onTakePhoto = {
                    isAppendingPage = false
                    viewModel.openLiveCamera()
                },
                onPickImage = {
                    isAppendingPage = false
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickMultipleImages = {
                    isAppendingPage = false
                    multiPhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onScanSample = {
                    val sample1 = viewModel.createSampleDocumentBitmap(1)
                    val sample2 = viewModel.createSampleDocumentBitmap(2)
                    val timeStr = SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())
                    viewModel.openBatchEditor(listOf(sample1, sample2), "Faktur_MultiHalaman_$timeStr", autoRotate = true)
                },
                onShareEmail = { doc ->
                    val pdf = doc.pdfPath?.let { File(it) }
                    viewModel.sendEmail(context, pdf, doc.extractedText)
                },
                openPdfTabsCount = openPdfTabs.size,
                onSwitchToPdfViewer = {
                    if (openPdfTabs.isNotEmpty()) {
                        viewModel.selectPdfTab(0)
                        viewModel.openPdfInViewer(openPdfTabs.first().file, openPdfTabs.first().title)
                    }
                }
            )
        }
        ScannerViewMode.EDITOR -> {
            DocEditorView(
                viewModel = viewModel,
                onBack = { viewModel.openGallery() },
                onAddPageCamera = {
                    isAppendingPage = true
                    viewModel.openLiveCamera()
                },
                onAddPageGallery = {
                    isAppendingPage = true
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onAddPagesBatch = {
                    isAppendingPage = true
                    multiPhotoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }
        ScannerViewMode.PDF_VIEWER -> {
            com.bimantara.feature.scanner.pdfviewer.PdfViewerComponent(
                viewModel = viewModel,
                onBack = { viewModel.openGallery() }
            )
        }
        ScannerViewMode.LIVE_CAMERA -> {
            val cameraViewModel: CameraViewModel = viewModel()
            CameraScannerScreen(
                viewModel = cameraViewModel,
                onDocumentProcessed = { processedBitmap ->
                    if (isAppendingPage) {
                        viewModel.addPage(processedBitmap)
                        viewModel.switchToEditor()
                    } else {
                        val timeStr = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
                        viewModel.openEditor(processedBitmap, "Pindai_$timeStr")
                    }
                },
                onClose = {
                    if (isAppendingPage) {
                        viewModel.switchToEditor()
                    } else {
                        viewModel.openGallery()
                    }
                }
            )
        }
    }
}

@Composable
fun DocGalleryView(
    docs: List<ScannedDocEntity>,
    onOpenDoc: (ScannedDocEntity) -> Unit,
    onOpenPdf: (ScannedDocEntity) -> Unit = {},
    onDeleteDoc: (ScannedDocEntity) -> Unit,
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit,
    onPickMultipleImages: () -> Unit,
    onScanSample: () -> Unit,
    onShareEmail: (ScannedDocEntity) -> Unit,
    openPdfTabsCount: Int = 0,
    onSwitchToPdfViewer: () -> Unit = {}
) {
    val currentLanguage by AppLanguageManager.currentLanguage.collectAsState()
    val strings = LocalAppStrings.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Header with About & Language buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.scanTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "CamScanner Pro • Multi-Tab PDF Viewer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Active PDF tabs shortcut chip
                if (openPdfTabsCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier
                            .clickable { onSwitchToPdfViewer() }
                            .testTag("gallery_active_pdf_tabs_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "PDF ($openPdfTabsCount)",
                                color = Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }

                // About Menu Button
                IconButton(
                    onClick = { showAboutDialog = true },
                    modifier = Modifier.testTag("scanner_about_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Tentang Aplikasi",
                        tint = Color(0xFF10B981)
                    )
                }

                // Language Switcher Button
                IconButton(
                    onClick = { showLanguageDialog = true },
                    modifier = Modifier.testTag("scanner_language_btn")
                ) {
                    Text(
                        text = currentLanguage.flagEmoji,
                        fontSize = 20.sp
                    )
                }
            }

            // Batch Scanning & Multi-Page Banner Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.scanBatchTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669)
                            )
                        }
                        Text(
                            text = "Auto-Rotate PDF",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onPickMultipleImages,
                            modifier = Modifier.weight(1.1f).testTag("scanner_batch_gallery_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.scanPickMultiple, fontSize = 11.sp, maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = onScanSample,
                            modifier = Modifier.weight(0.9f).testTag("scanner_batch_sample_btn"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Contoh 2 Halaman", fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }
            }

            // Standard Single Capture Actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTakePhoto,
                    modifier = Modifier.weight(1.1f).testTag("scanner_camera_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.scanTakePhoto, fontSize = 12.sp, maxLines = 1)
                }

                OutlinedButton(
                    onClick = onPickImage,
                    modifier = Modifier.weight(0.95f).testTag("scanner_pick_image_btn"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.scanGallery, fontSize = 12.sp, maxLines = 1)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (docs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.scanNoDocs,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Ambil foto atau pilih beberapa berkas untuk pemindaian multi-halaman",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(docs, key = { it.id }) { doc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenDoc(doc) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Doc thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (File(doc.imagePath).exists()) {
                                        AsyncImage(
                                            model = File(doc.imagePath),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Description,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = doc.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(doc.createdAt)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                    if (doc.pdfPath != null) {
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                            modifier = Modifier
                                                .clickable { onOpenPdf(doc) }
                                                .testTag("gallery_chip_pdf_${doc.id}")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(11.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "Lihat PDF",
                                                    color = Color(0xFFEF4444),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                // Open in Dedicated PDF Viewer
                                IconButton(
                                    onClick = { onOpenPdf(doc) },
                                    modifier = Modifier.testTag("gallery_view_pdf_${doc.id}")
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = "Pratinjau PDF", tint = Color(0xFFEF4444))
                                }

                                // Share / Email action
                                IconButton(onClick = { onShareEmail(doc) }) {
                                    Icon(Icons.Default.Email, contentDescription = "Email", tint = Color(0xFF0284C7))
                                }

                                // Delete action
                                IconButton(onClick = { onDeleteDoc(doc) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button for Camera
        FloatingActionButton(
            onClick = onTakePhoto,
            containerColor = Color(0xFF10B981),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("scanner_fab_camera")
        ) {
            Icon(Icons.Default.CameraAlt, contentDescription = strings.scanTakePhoto)
        }

        if (showLanguageDialog) {
            AppLanguageSelectionDialog(
                onDismiss = { showLanguageDialog = false }
            )
        }

        if (showAboutDialog) {
            AboutDialog(
                onDismiss = { showAboutDialog = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocEditorView(
    viewModel: CamScannerViewModel,
    onBack: () -> Unit,
    onAddPageCamera: () -> Unit,
    onAddPageGallery: () -> Unit,
    onAddPagesBatch: () -> Unit
) {
    val context = LocalContext.current
    val title by viewModel.documentTitle.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val currentPageIndex by viewModel.currentPageIndex.collectAsState()
    val enhancedBitmap by viewModel.enhancedBitmap.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val ocrText by viewModel.ocrText.collectAsState()
    val isOcrLoading by viewModel.isOcrLoading.collectAsState()
    val selectedOcrLanguage by viewModel.selectedOcrLanguage.collectAsState()
    val ocrProgress by viewModel.ocrProgress.collectAsState()
    val ocrConfidence by viewModel.ocrConfidence.collectAsState()
    val downloadingLangCode by viewModel.downloadingLangCode.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val isExportingPdf by viewModel.isExportingPdf.collectAsState()
    val lastExportedPdf by viewModel.lastExportedPdf.collectAsState()
    val detectedFrame by viewModel.detectedFrame.collectAsState()
    val selectedScaleRatio by viewModel.selectedScaleRatio.collectAsState()
    val isFrameOverlayActive by viewModel.isFrameOverlayActive.collectAsState()
    val isProcessingFrame by viewModel.isProcessingFrame.collectAsState()
    val autoRotateExport by viewModel.autoRotateExport.collectAsState()
    val strings = LocalAppStrings.current

    var showOcrLanguageDialog by remember { mutableStateOf(false) }
    var showPdfSuccessDialog by remember { mutableStateOf(false) }
    var showPdfFinalizationSheet by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var activeDraggingCorner by remember { mutableStateOf<Int?>(null) }
    var currentTouchPos by remember { mutableStateOf(Offset.Zero) }
    var editorContainerSize by remember { mutableStateOf(Size.Zero) }

    // Integrated CanHub Image Cropper Launcher for current editor page
    val editorCropLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val croppedBmp = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (croppedBmp != null) {
                        viewModel.replaceCurrentPageBitmap(croppedBmp)
                        Toast.makeText(context, "Halaman berhasil dipotong", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal memotong: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val launchDocCropper: (Bitmap) -> Unit = { bmp ->
        try {
            val tempFile = File(context.cacheDir, "editor_crop_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(tempFile)
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            fos.flush()
            fos.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )

            val options = CropImageContractOptions(
                uri = uri,
                cropImageOptions = CropImageOptions(
                    guidelines = CropImageView.Guidelines.ON,
                    cropShape = CropImageView.CropShape.RECTANGLE,
                    fixAspectRatio = false,
                    activityTitle = "Potong & Putar Halaman",
                    toolbarColor = android.graphics.Color.BLACK,
                    activityMenuIconColor = android.graphics.Color.WHITE
                )
            )
            editorCropLauncher.launch(options)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka cropper: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Editor App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.setTitle(it) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(8.dp)
            )

            // About Dialog
            IconButton(onClick = { showAboutDialog = true }) {
                Icon(Icons.Default.Info, contentDescription = "About", tint = Color(0xFF10B981))
            }

            // Preview PDF in Dedicated Viewer Button
            IconButton(
                onClick = {
                    viewModel.previewCurrentDocumentAsPdf()
                },
                modifier = Modifier.testTag("scanner_top_preview_pdf_btn")
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = "Pratinjau PDF", tint = Color(0xFFEF4444))
            }

            // Save Document to Device Button
            IconButton(
                onClick = {
                    if (detectedFrame != null) {
                        viewModel.applyFrameCorrectionAndScale(selectedScaleRatio)
                    }
                    viewModel.saveDocument {
                        Toast.makeText(context, "Dokumen & PDF multi-halaman tersimpan di perangkat!", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                },
                modifier = Modifier.testTag("scanner_top_save_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = strings.save, tint = Color(0xFF10B981))
            }
        }

        // BATCH / MULTI-PAGE CAROUSEL STRIP
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kelola Halaman (${currentPageIndex + 1} dari ${pages.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Reorder left
                        IconButton(
                            onClick = { viewModel.moveCurrentPageLeft() },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Pindah Kiri", modifier = Modifier.size(16.dp))
                        }
                        // Reorder right
                        IconButton(
                            onClick = { viewModel.moveCurrentPageRight() },
                            enabled = currentPageIndex < pages.size - 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Pindah Kanan", modifier = Modifier.size(16.dp))
                        }
                        // Delete page
                        IconButton(
                            onClick = { viewModel.removeCurrentPage() },
                            enabled = pages.size > 1,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus Halaman", tint = if (pages.size > 1) MaterialTheme.colorScheme.error else Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Thumbnail Strip
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    itemsIndexed(pages) { index, page ->
                        val isSelected = index == currentPageIndex
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(95.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(
                                    width = if (isSelected) 2.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF10B981) else Color(0xFF475569),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .background(Color(0xFF0F172A))
                                .clickable { viewModel.selectPage(index) }
                        ) {
                            Image(
                                bitmap = page.enhancedBitmap.asImageBitmap(),
                                contentDescription = "Halaman ${index + 1}",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Page Number Badge
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(if (isSelected) Color(0xFF10B981) else Color.Black.copy(alpha = 0.7f))
                                    .padding(vertical = 2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Hal. ${index + 1}",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Add Page Buttons in strip
                    item {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            OutlinedButton(
                                onClick = onAddPageCamera,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Kamera", fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = onAddPageGallery,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Galeri", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Preview of Enhanced Document with Frame Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .background(Color(0xFF0F172A))
                .padding(10.dp)
                .onSizeChanged { editorContainerSize = Size(it.width.toFloat(), it.height.toFloat()) },
            contentAlignment = Alignment.Center
        ) {
            if (enhancedBitmap != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = enhancedBitmap!!.asImageBitmap(),
                        contentDescription = "Scanned Document",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Fit
                    )

                    // Interactive Perspective Dewarping Frame Overlay
                    if (isFrameOverlayActive && detectedFrame != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(detectedFrame) {
                                    detectDragGestures(
                                        onDragStart = { touchOffset ->
                                            currentTouchPos = touchOffset
                                            val frame = detectedFrame ?: return@detectDragGestures
                                            val origW = frame.originalWidth.toFloat()
                                            val origH = frame.originalHeight.toFloat()
                                            if (origW <= 0f || origH <= 0f) return@detectDragGestures

                                            val scaleFactor = minOf(size.width / origW, size.height / origH)
                                            val offsetX = (size.width - origW * scaleFactor) / 2f
                                            val offsetY = (size.height - origH * scaleFactor) / 2f

                                            val corners = listOf(
                                                Offset(offsetX + frame.topLeft.x * scaleFactor, offsetY + frame.topLeft.y * scaleFactor),
                                                Offset(offsetX + frame.topRight.x * scaleFactor, offsetY + frame.topRight.y * scaleFactor),
                                                Offset(offsetX + frame.bottomRight.x * scaleFactor, offsetY + frame.bottomRight.y * scaleFactor),
                                                Offset(offsetX + frame.bottomLeft.x * scaleFactor, offsetY + frame.bottomLeft.y * scaleFactor)
                                            )

                                            val hitThreshold = 52.dp.toPx()
                                            var closestIndex: Int? = null
                                            var minDistance = Float.MAX_VALUE
                                            corners.forEachIndexed { index, cornerPt ->
                                                val dist = hypot((cornerPt.x - touchOffset.x).toDouble(), (cornerPt.y - touchOffset.y).toDouble()).toFloat()
                                                if (dist < minDistance && dist <= hitThreshold) {
                                                    minDistance = dist
                                                    closestIndex = index
                                                }
                                            }
                                            activeDraggingCorner = closestIndex
                                        },
                                        onDrag = { change, _ ->
                                            currentTouchPos = change.position
                                            val activeCorner = activeDraggingCorner ?: return@detectDragGestures
                                            val frame = detectedFrame ?: return@detectDragGestures
                                            val origW = frame.originalWidth.toFloat()
                                            val origH = frame.originalHeight.toFloat()
                                            if (origW <= 0f || origH <= 0f) return@detectDragGestures

                                            val scaleFactor = minOf(size.width / origW, size.height / origH)
                                            val offsetX = (size.width - origW * scaleFactor) / 2f
                                            val offsetY = (size.height - origH * scaleFactor) / 2f

                                            val bmpX = (change.position.x - offsetX) / scaleFactor
                                            val bmpY = (change.position.y - offsetY) / scaleFactor

                                            viewModel.updateCorner(activeCorner, bmpX, bmpY)
                                            change.consume()
                                        },
                                        onDragEnd = { activeDraggingCorner = null },
                                        onDragCancel = { activeDraggingCorner = null }
                                    )
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val frame = detectedFrame ?: return@Canvas
                                val origW = frame.originalWidth.toFloat()
                                val origH = frame.originalHeight.toFloat()
                                if (origW > 0 && origH > 0) {
                                    val scaleFactor = minOf(size.width / origW, size.height / origH)
                                    val offsetX = (size.width - origW * scaleFactor) / 2f
                                    val offsetY = (size.height - origH * scaleFactor) / 2f

                                    val p0 = Offset(offsetX + frame.topLeft.x * scaleFactor, offsetY + frame.topLeft.y * scaleFactor)
                                    val p1 = Offset(offsetX + frame.topRight.x * scaleFactor, offsetY + frame.topRight.y * scaleFactor)
                                    val p2 = Offset(offsetX + frame.bottomRight.x * scaleFactor, offsetY + frame.bottomRight.y * scaleFactor)
                                    val p3 = Offset(offsetX + frame.bottomLeft.x * scaleFactor, offsetY + frame.bottomLeft.y * scaleFactor)

                                    val path = Path().apply {
                                        moveTo(p0.x, p0.y)
                                        lineTo(p1.x, p1.y)
                                        lineTo(p2.x, p2.y)
                                        lineTo(p3.x, p3.y)
                                        close()
                                    }

                                    // Shaded interior polygon
                                    drawPath(path, color = Color(0x3310B981))
                                    // Glowing bounding stroke
                                    drawPath(path, color = Color(0xFF10B981), style = Stroke(width = 3.dp.toPx()))

                                    // Interior 3x3 perspective guide grid lines (like CamScanner)
                                    fun interpolate(a: Offset, b: Offset, fraction: Float): Offset {
                                        return Offset(a.x + (b.x - a.x) * fraction, a.y + (b.y - a.y) * fraction)
                                    }

                                    val gridColor = Color(0x5510B981)
                                    drawLine(gridColor, interpolate(p0, p3, 0.33f), interpolate(p1, p2, 0.33f), strokeWidth = 1.dp.toPx())
                                    drawLine(gridColor, interpolate(p0, p3, 0.67f), interpolate(p1, p2, 0.67f), strokeWidth = 1.dp.toPx())
                                    drawLine(gridColor, interpolate(p0, p1, 0.33f), interpolate(p3, p2, 0.33f), strokeWidth = 1.dp.toPx())
                                    drawLine(gridColor, interpolate(p0, p1, 0.67f), interpolate(p3, p2, 0.67f), strokeWidth = 1.dp.toPx())

                                    // 4 Corner interactive grab handles
                                    val corners = listOf(p0, p1, p2, p3)
                                    corners.forEachIndexed { idx, pt ->
                                        val isCurrentActive = activeDraggingCorner == idx
                                        val handleRadius = if (isCurrentActive) 14.dp.toPx() else 8.dp.toPx()

                                        if (isCurrentActive) {
                                            // Glowing outer pulse ring
                                            drawCircle(Color(0x6610B981), radius = handleRadius + 8.dp.toPx(), center = pt)
                                            // Crosshairs
                                            drawLine(Color.White, Offset(pt.x - 18.dp.toPx(), pt.y), Offset(pt.x + 18.dp.toPx(), pt.y), strokeWidth = 2.dp.toPx())
                                            drawLine(Color.White, Offset(pt.x, pt.y - 18.dp.toPx()), Offset(pt.x, pt.y + 18.dp.toPx()), strokeWidth = 2.dp.toPx())
                                        }

                                        drawCircle(Color.White, radius = handleRadius + 2.dp.toPx(), center = pt)
                                        drawCircle(if (isCurrentActive) Color(0xFF059669) else Color(0xFF10B981), radius = handleRadius, center = pt)
                                        drawCircle(Color.White, radius = 2.5.dp.toPx(), center = pt)
                                    }
                                }
                            }

                            // Magnified Loupe during corner dragging in DocEditorView
                            if (activeDraggingCorner != null && enhancedBitmap != null) {
                                val cornerIdx = activeDraggingCorner!!
                                val frame = detectedFrame
                                if (frame != null) {
                                    val pt = when (cornerIdx) {
                                        0 -> Offset(frame.topLeft.x, frame.topLeft.y)
                                        1 -> Offset(frame.topRight.x, frame.topRight.y)
                                        2 -> Offset(frame.bottomRight.x, frame.bottomRight.y)
                                        else -> Offset(frame.bottomLeft.x, frame.bottomLeft.y)
                                    }
                                    CornerMagnifierLoupe(
                                        bitmap = enhancedBitmap!!,
                                        bmpPoint = pt,
                                        touchPosition = currentTouchPos,
                                        cornerIndex = cornerIdx,
                                        containerSize = editorContainerSize,
                                        zoomFactor = 2.6f
                                    )
                                }
                            }
                        }
                    }

                    // Badge: Frame Detected Indicator & Control
                    if (detectedFrame != null && isFrameOverlayActive) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.85f))
                                .border(1.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Crop,
                                contentDescription = null,
                                tint = Color(0xFF34D399),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (activeDraggingCorner != null) "Menyesuaikan Sudut #${activeDraggingCorner!! + 1}" else "Bingkai Terkunci • Geser 4 Sudut",
                                color = Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Action Buttons: Potong Bebas (Cropper) & Luruskan Perspektif
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    enhancedBitmap?.let { bmp ->
                                        launchDocCropper(bmp)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Potong", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    viewModel.applyFrameCorrectionAndScale(selectedScaleRatio)
                                    Toast.makeText(context, "Dokumen berhasil diluruskan & diproporsionalkan!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(18.dp)
                            ) {
                                Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Luruskan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                CircularProgressIndicator()
            }

            if (isOcrLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF10B981))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = strings.ocrLoading,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // AUTO-PAGE-ROTATION & ROTATION CONTROLS
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RotateRight,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Orientasi & Rotasi Halaman",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Rotate active page 90 degrees
                    Button(
                        onClick = { viewModel.rotate90() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Putar 90°", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Auto-Rotate All Pages Button
                Button(
                    onClick = {
                        viewModel.autoRotateAllPages()
                        Toast.makeText(context, "Semua halaman diselaraskan ke orientasi tegak (portrait)!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("scanner_auto_rotate_all_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.scanAutoRotateAll, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Auto-Rotate to Portrait on PDF Export Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleAutoRotateExport() }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.scanAutoRotatePdf,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Menyeragamkan seluruh halaman PDF tetap tegak dan proporsional.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.5.sp
                        )
                    }
                    Switch(
                        checked = autoRotateExport,
                        onCheckedChange = { viewModel.toggleAutoRotateExport() }
                    )
                }
            }
        }

        // Enhancement Filter Selector (Color Enhance, B&W, Grayscale, Original)
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Filter Dokumen",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Terapkan filter ke halaman ini sebelum ekspor PDF",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.5.sp
                    )
                }

                if (pages.size > 1) {
                    AssistChip(
                        onClick = {
                            viewModel.applyFilterToAllPages(selectedFilter)
                            Toast.makeText(context, "Filter ${selectedFilter.displayName} diterapkan ke semua ${pages.size} halaman!", Toast.LENGTH_SHORT).show()
                        },
                        label = { Text("Terapkan ke Semua (${pages.size} Hal)", fontSize = 10.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(13.dp))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilter == DocumentFilter.MAGIC_COLOR,
                        onClick = { viewModel.setFilter(DocumentFilter.MAGIC_COLOR) },
                        label = { Text(strings.filterMagicColor) },
                        leadingIcon = {
                            Icon(
                                if (selectedFilter == DocumentFilter.MAGIC_COLOR) Icons.Default.Check else Icons.Default.AutoFixHigh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedFilter == DocumentFilter.MAGIC_COLOR) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == DocumentFilter.BW,
                        onClick = { viewModel.setFilter(DocumentFilter.BW) },
                        label = { Text(strings.filterBw) },
                        leadingIcon = {
                            Icon(
                                if (selectedFilter == DocumentFilter.BW) Icons.Default.Check else Icons.Default.Contrast,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == DocumentFilter.GRAYSCALE,
                        onClick = { viewModel.setFilter(DocumentFilter.GRAYSCALE) },
                        label = { Text(strings.filterGrayscale) },
                        leadingIcon = {
                            Icon(
                                if (selectedFilter == DocumentFilter.GRAYSCALE) Icons.Default.Check else Icons.Default.FilterBAndW,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == DocumentFilter.ORIGINAL,
                        onClick = { viewModel.setFilter(DocumentFilter.ORIGINAL) },
                        label = { Text(strings.filterOriginal) },
                        leadingIcon = {
                            Icon(
                                if (selectedFilter == DocumentFilter.ORIGINAL) Icons.Default.Check else Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        // Section: Pembacaan Bingkai & Perapian Skala Dokumen
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Crop,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.scanStraightenScale,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (detectedFrame != null) {
                        AssistChip(
                            onClick = { viewModel.toggleFrameOverlay() },
                            label = {
                                Text(
                                    if (isFrameOverlayActive) "Sembunyikan" else "Lihat Bingkai",
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    if (isFrameOverlayActive) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.scanScaleRatio,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Ratio choices: AUTO, A4, LETTER, CARD, SQUARE
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(DocumentScaleRatio.entries) { ratio ->
                        FilterChip(
                            selected = selectedScaleRatio == ratio,
                            onClick = { viewModel.setScaleRatio(ratio) },
                            label = { Text(ratio.displayName, fontSize = 11.sp) },
                            leadingIcon = {
                                if (selectedScaleRatio == ratio) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action buttons: "Deteksi Bingkai" and "Rapikan Skala Sekarang"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.detectFrame() },
                        modifier = Modifier.weight(1f).testTag("scanner_detect_frame_btn"),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.scanDetectFrame, fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            viewModel.applyFrameCorrectionAndScale(selectedScaleRatio)
                            Toast.makeText(context, strings.scanFrameDetectedSuccess, Toast.LENGTH_SHORT).show()
                        },
                        enabled = !isProcessingFrame,
                        modifier = Modifier.weight(1.3f).testTag("scanner_apply_straighten_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (isProcessingFrame) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AspectRatio, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rapikan Skala", fontSize = 12.sp, maxLines = 1)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Save to Device Button
        Button(
            onClick = {
                if (detectedFrame != null) {
                    viewModel.applyFrameCorrectionAndScale(selectedScaleRatio)
                }
                viewModel.saveDocument {
                    Toast.makeText(context, "Dokumen & seluruh halaman tersimpan di perangkat!", Toast.LENGTH_SHORT).show()
                    onBack()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("scanner_save_device_btn"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Simpan Dokumen ke Perangkat (${pages.size} Hal)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Export & Preview Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preview in Dedicated PDF Viewer Button
            Button(
                onClick = { viewModel.previewCurrentDocumentAsPdf() },
                modifier = Modifier.weight(1f).testTag("scanner_preview_pdf_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pratinjau PDF", fontSize = 11.sp, maxLines = 1)
            }

            // Export Multi-Page PDF Button (Opens Pre-Export Finalization & Filter Review)
            Button(
                onClick = {
                    showPdfFinalizationSheet = true
                },
                modifier = Modifier.weight(1.2f).testTag("scanner_export_pdf_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(10.dp),
                enabled = !isExportingPdf
            ) {
                if (isExportingPdf) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ekspor PDF (${pages.size} Hal)", fontSize = 11.sp, maxLines = 1)
                }
            }

            // Email Button
            Button(
                onClick = {
                    if (lastExportedPdf != null) {
                        viewModel.sendEmail(context, lastExportedPdf, ocrText)
                    } else {
                        viewModel.exportToPdf { file ->
                            viewModel.sendEmail(context, file, ocrText)
                        }
                    }
                },
                modifier = Modifier.weight(0.9f).testTag("scanner_send_email_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.emailShare, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tesseract Multi-Language OCR Card & Results
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header with Tesseract badge & language picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tesseract OCR Multi-Bahasa",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ekstraksi teks on-device (Halaman ${currentPageIndex + 1})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Target Language Selector Button / Chip
                    OutlinedButton(
                        onClick = { showOcrLanguageDialog = true },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("scanner_ocr_select_lang_btn")
                    ) {
                        Text(selectedOcrLanguage.flagEmoji, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            selectedOcrLanguage.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Language,
                            contentDescription = "Ganti Bahasa OCR",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // OCR Run Button & Progress
                val isInstalled = viewModel.ocrManagerInstance.isLanguageInstalled(selectedOcrLanguage.code)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isInstalled) {
                                viewModel.runOcr()
                            } else {
                                showOcrLanguageDialog = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("scanner_run_ocr_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isInstalled) Color(0xFF0284C7) else Color(0xFFD97706)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isOcrLoading
                    ) {
                        if (isOcrLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (ocrProgress > 0) "Mengenali ($ocrProgress%)..." else "Memproses OCR...",
                                fontSize = 11.sp
                            )
                        } else if (!isInstalled) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Unduh Bahasa (${selectedOcrLanguage.code.uppercase()})", fontSize = 11.sp)
                        } else {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pindai Teks (${selectedOcrLanguage.code.uppercase()})", fontSize = 11.sp)
                        }
                    }

                    if (ocrText.isNotBlank()) {
                        FilledTonalIconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("OCR Text", ocrText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Teks halaman ${currentPageIndex + 1} berhasil disalin", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(38.dp).testTag("scanner_ocr_copy_btn")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Salin Teks", modifier = Modifier.size(18.dp))
                        }

                        FilledTonalIconButton(
                            onClick = { viewModel.setOcrText("") },
                            modifier = Modifier.size(38.dp).testTag("scanner_ocr_clear_btn")
                        ) {
                            Icon(Icons.Default.Clear, contentDescription = "Bersihkan Teks", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                if (isOcrLoading && ocrProgress > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { ocrProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(0xFF0284C7)
                    )
                }

                if (ocrConfidence != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = if (ocrConfidence!! > 75) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Akurasi OCR: $ocrConfidence%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (ocrConfidence!! > 75) Color(0xFF059669) else Color(0xFFD97706),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Bahasa: ${selectedOcrLanguage.name}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = ocrText,
                    onValueChange = { viewModel.setOcrText(it) },
                    placeholder = {
                        Text("Hasil teks OCR (${selectedOcrLanguage.name}) akan muncul di sini. Anda dapat langsung mengeditnya...")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .testTag("scanner_ocr_result_input"),
                    shape = RoundedCornerShape(8.dp),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    if (showPdfSuccessDialog && lastExportedPdf != null) {
        val pdf = lastExportedPdf!!
        AlertDialog(
            onDismissRequest = { showPdfSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PDF Multi-Halaman Siap!")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nama: ${pdf.name}", fontWeight = FontWeight.SemiBold)
                    Text("Jumlah Halaman: ${pages.size} Halaman")
                    Text("Auto-Rotation: ${if (autoRotateExport) "Aktif (Tegak Portrait)" else "Nonaktif"}")
                    Text("Ukuran: ${pdf.length() / 1024} KB")
                    Text("Lokasi: ${pdf.absolutePath}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            showPdfSuccessDialog = false
                            viewModel.openPdfInViewer(pdf, title)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.testTag("dialog_open_pdf_viewer_btn")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pratinjau PDF")
                    }
                    Button(
                        onClick = {
                            showPdfSuccessDialog = false
                            viewModel.sendEmail(context, pdf, ocrText)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(strings.emailShare)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfSuccessDialog = false }) {
                    Text(strings.actionClose)
                }
            }
        )
    }

    if (showOcrLanguageDialog) {
        OcrLanguageDialog(
            selectedLanguageCode = selectedOcrLanguage.code,
            ocrManager = viewModel.ocrManagerInstance,
            downloadingCode = downloadingLangCode,
            downloadProgress = downloadProgress,
            onSelectLanguage = { lang ->
                viewModel.selectOcrLanguage(lang)
            },
            onDownloadLanguage = { lang ->
                viewModel.downloadLanguage(lang)
            },
            onDeleteLanguage = { code ->
                viewModel.deleteLanguage(code)
            },
            onDismissRequest = {
                showOcrLanguageDialog = false
            }
        )
    }

    if (showPdfFinalizationSheet) {
        PdfExportFinalizationSheet(
            viewModel = viewModel,
            onDismiss = { showPdfFinalizationSheet = false },
            onPdfGenerated = { _ ->
                showPdfFinalizationSheet = false
                showPdfSuccessDialog = true
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
}

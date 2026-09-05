package com.example.feature.scanner

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.core.i18n.AppLanguageManager
import com.example.core.i18n.AppLanguageSelectionDialog
import com.example.core.i18n.LocalAppStrings
import com.example.data.model.ScannedDocEntity
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

    var photoCapturePath by remember { mutableStateOf<String?>(null) }

    // Camera picture capture launcher (saves to file, handles native camera capture)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val path = photoCapturePath
        if (success && path != null) {
            viewModel.loadBitmapFromFile(path, context)
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
            Toast.makeText(context, "Izin kamera diperlukan untuk mengambil gambar dokumen", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchCamera() {
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

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.loadBitmapFromUri(uri, context)
        }
    }

    if (viewMode == ScannerViewMode.GALLERY) {
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
            onDeleteDoc = { viewModel.deleteDoc(it) },
            onTakePhoto = { launchCamera() },
            onPickImage = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onScanSample = {
                val sample = viewModel.createSampleDocumentBitmap()
                viewModel.openEditor(sample, "Faktur_Dokumen_${SimpleDateFormat("HHmm", Locale.getDefault()).format(Date())}")
            },
            onShareEmail = { doc ->
                val pdf = doc.pdfPath?.let { File(it) }
                viewModel.sendEmail(context, pdf, doc.extractedText)
            }
        )
    } else {
        DocEditorView(
            viewModel = viewModel,
            onBack = { viewModel.openGallery() }
        )
    }
}

@Composable
fun DocGalleryView(
    docs: List<ScannedDocEntity>,
    onOpenDoc: (ScannedDocEntity) -> Unit,
    onDeleteDoc: (ScannedDocEntity) -> Unit,
    onTakePhoto: () -> Unit,
    onPickImage: () -> Unit,
    onScanSample: () -> Unit,
    onShareEmail: (ScannedDocEntity) -> Unit
) {
    val currentLanguage by AppLanguageManager.currentLanguage.collectAsState()
    val strings = LocalAppStrings.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
        // Header
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
                    text = strings.scanSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

        // Action banner buttons: Camera button (requested), Gallery button, and Sample button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onTakePhoto,
                modifier = Modifier.weight(1.1f).testTag("scanner_camera_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
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
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.scanGallery, fontSize = 12.sp, maxLines = 1)
            }

            OutlinedButton(
                onClick = onScanSample,
                modifier = Modifier.weight(0.95f).testTag("scanner_sample_doc_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.scanPrompt, fontSize = 12.sp, maxLines = 1)
            }
        }

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
                        text = strings.scanPrompt,
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
                                if (doc.extractedText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "OCR: " + doc.extractedText.replace("\n", " ").take(45) + "...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF10B981),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
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

        // Floating Action Button for Quick Camera Capture
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
    }
}

@Composable
fun DocEditorView(
    viewModel: CamScannerViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val title by viewModel.documentTitle.collectAsState()
    val enhancedBitmap by viewModel.enhancedBitmap.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val ocrText by viewModel.ocrText.collectAsState()
    val isOcrLoading by viewModel.isOcrLoading.collectAsState()
    val isExportingPdf by viewModel.isExportingPdf.collectAsState()
    val lastExportedPdf by viewModel.lastExportedPdf.collectAsState()
    val detectedFrame by viewModel.detectedFrame.collectAsState()
    val selectedScaleRatio by viewModel.selectedScaleRatio.collectAsState()
    val isFrameOverlayActive by viewModel.isFrameOverlayActive.collectAsState()
    val isProcessingFrame by viewModel.isProcessingFrame.collectAsState()
    val strings = LocalAppStrings.current

    var showPdfSuccessDialog by remember { mutableStateOf(false) }

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
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            IconButton(onClick = { viewModel.rotate90() }) {
                Icon(Icons.Default.RotateRight, contentDescription = "Putar 90°")
            }
            IconButton(
                onClick = {
                    if (detectedFrame != null) {
                        viewModel.applyFrameCorrectionAndScale(selectedScaleRatio)
                    }
                    viewModel.saveDocument {
                        Toast.makeText(context, "Dokumen & skala berhasil disimpan ke perangkat!", Toast.LENGTH_SHORT).show()
                        onBack()
                    }
                },
                modifier = Modifier.testTag("scanner_top_save_btn")
            ) {
                Icon(Icons.Default.Save, contentDescription = strings.save, tint = Color(0xFF10B981))
            }
        }

        // Preview of Enhanced Document with Frame Overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp)
                .background(Color(0xFF0F172A))
                .padding(10.dp),
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

                    // Frame Overlay Canvas: Reads and draws the document frame border and corners
                    if (isFrameOverlayActive && detectedFrame != null) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val frame = detectedFrame ?: return@Canvas
                            val origW = frame.originalWidth.toFloat()
                            val origH = frame.originalHeight.toFloat()
                            if (origW > 0 && origH > 0) {
                                val scaleFactor = minOf(size.width / origW, size.height / origH)
                                val offsetX = (size.width - origW * scaleFactor) / 2f
                                val offsetY = (size.height - origH * scaleFactor) / 2f

                                val p1 = Offset(offsetX + frame.topLeft.x * scaleFactor, offsetY + frame.topLeft.y * scaleFactor)
                                val p2 = Offset(offsetX + frame.topRight.x * scaleFactor, offsetY + frame.topRight.y * scaleFactor)
                                val p3 = Offset(offsetX + frame.bottomRight.x * scaleFactor, offsetY + frame.bottomRight.y * scaleFactor)
                                val p4 = Offset(offsetX + frame.bottomLeft.x * scaleFactor, offsetY + frame.bottomLeft.y * scaleFactor)

                                val path = Path().apply {
                                    moveTo(p1.x, p1.y)
                                    lineTo(p2.x, p2.y)
                                    lineTo(p3.x, p3.y)
                                    lineTo(p4.x, p4.y)
                                    close()
                                }

                                // Semi-transparent document quad highlight
                                drawPath(path, color = Color(0x3310B981))
                                // Crisp boundary stroke
                                drawPath(path, color = Color(0xFF10B981), style = Stroke(width = 3.dp.toPx()))

                                // 4 Corner Anchor Pinpoints
                                val cornerRadius = 6.dp.toPx()
                                listOf(p1, p2, p3, p4).forEach { pt ->
                                    drawCircle(Color.White, radius = cornerRadius + 2.dp.toPx(), center = pt)
                                    drawCircle(Color(0xFF10B981), radius = cornerRadius, center = pt)
                                }
                            }
                        }
                    }

                    // Badge: Frame Detected Indicator
                    if (detectedFrame != null && isFrameOverlayActive) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color.Black.copy(alpha = 0.75f))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = strings.scanFrameDetectedBadge,
                                color = Color(0xFF34D399),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
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

        // Enhancement Filter Selector ("Lebih Rapih")
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = strings.filterMagicColor,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = selectedFilter == DocumentFilter.MAGIC_COLOR,
                        onClick = { viewModel.setFilter(DocumentFilter.MAGIC_COLOR) },
                        label = { Text(strings.filterMagicColor) },
                        leadingIcon = {
                            if (selectedFilter == DocumentFilter.MAGIC_COLOR) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == DocumentFilter.BW,
                        onClick = { viewModel.setFilter(DocumentFilter.BW) },
                        label = { Text(strings.filterBw) },
                        leadingIcon = {
                            if (selectedFilter == DocumentFilter.BW) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == DocumentFilter.GRAYSCALE,
                        onClick = { viewModel.setFilter(DocumentFilter.GRAYSCALE) },
                        label = { Text(strings.filterGrayscale) },
                        leadingIcon = {
                            if (selectedFilter == DocumentFilter.GRAYSCALE) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    )
                }
                item {
                    FilterChip(
                        selected = selectedFilter == DocumentFilter.ORIGINAL,
                        onClick = { viewModel.setFilter(DocumentFilter.ORIGINAL) },
                        label = { Text(strings.filterOriginal) },
                        leadingIcon = {
                            if (selectedFilter == DocumentFilter.ORIGINAL) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
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

        // Save to Device Button (Perapian Skala Sebelum Disimpan di Perangkat)
        Button(
            onClick = {
                if (detectedFrame != null) {
                    viewModel.applyFrameCorrectionAndScale(selectedScaleRatio)
                }
                viewModel.saveDocument {
                    Toast.makeText(context, "Dokumen & skala berhasil disimpan ke perangkat!", Toast.LENGTH_SHORT).show()
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
            Text("Simpan Gambar ke Perangkat", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Primary Functional Action Buttons: OCR, PDF, Email
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // OCR Button
            Button(
                onClick = { viewModel.runOcr() },
                modifier = Modifier.weight(1f).testTag("scanner_run_ocr_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.ocrExtract, fontSize = 12.sp)
            }

            // Export PDF Button
            Button(
                onClick = {
                    viewModel.exportToPdf { file ->
                        if (file != null) {
                            showPdfSuccessDialog = true
                        } else {
                            Toast.makeText(context, "Gagal membuat PDF", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f).testTag("scanner_export_pdf_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(10.dp),
                enabled = !isExportingPdf
            ) {
                if (isExportingPdf) {
                    CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(strings.pdfExport, fontSize = 12.sp)
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
                modifier = Modifier.weight(1f).testTag("scanner_send_email_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.emailShare, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Extracted OCR Text Card (Editable)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.ocrResultTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    if (ocrText.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("OCR Text", ocrText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Teks berhasil disalin ke clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Text", modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = ocrText,
                    onValueChange = { viewModel.setOcrText(it) },
                    placeholder = { Text("Tekan 'Scan OCR' untuk mengekstrak teks dari gambar dokumen ini...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
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
                    Text(strings.savePdfSuccess)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("PDF: ${pdf.name}", fontWeight = FontWeight.SemiBold)
                    Text("Size: ${pdf.length() / 1024} KB")
                    Text("${pdf.absolutePath}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPdfSuccessDialog = false
                        viewModel.sendEmail(context, pdf, ocrText)
                    }
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.emailShare)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPdfSuccessDialog = false }) {
                    Text(strings.actionClose)
                }
            }
        )
    }
}

package com.bimantara.feature.scanner.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.FilterFrames
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.canhub.cropper.CropImageContract
import com.canhub.cropper.CropImageContractOptions
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView
import com.bimantara.feature.scanner.CornerMagnifierLoupe
import com.bimantara.feature.scanner.DocumentFilter
import com.bimantara.feature.scanner.DocumentFrame
import com.bimantara.feature.scanner.DocumentScaleRatio
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot

@Composable
fun CameraScannerScreen(
    viewModel: CameraViewModel,
    onDocumentProcessed: (Bitmap) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val capturedBitmap by viewModel.capturedBitmap.collectAsState()
    val capturedFrame by viewModel.capturedFrame.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Izin kamera dibutuhkan untuk memindai dokumen", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Integrated CanHub Image Cropper library launcher
    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = CropImageContract()
    ) { result ->
        if (result.isSuccessful) {
            result.uriContent?.let { uri ->
                try {
                    val stream = context.contentResolver.openInputStream(uri)
                    val croppedBitmap = BitmapFactory.decodeStream(stream)
                    stream?.close()
                    if (croppedBitmap != null) {
                        viewModel.setCapturedBitmap(croppedBitmap)
                        Toast.makeText(context, "Foto berhasil dipotong dengan Cropper", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Gagal memuat hasil potong: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun launchCanHubCropper(bitmap: Bitmap) {
        try {
            val tempFile = File(context.cacheDir, "crop_input_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(tempFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
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
                    activityTitle = "Potong & Putar Dokumen",
                    toolbarColor = android.graphics.Color.BLACK,
                    activityMenuIconColor = android.graphics.Color.WHITE
                )
            )
            cropImageLauncher.launch(options)
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka cropper: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.unbindCamera()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (capturedBitmap != null && capturedFrame != null) {
            // Screen 2: Manual Corner Adjustment & Perspective Distortion Rectification
            ManualCornerRectificationView(
                bitmap = capturedBitmap!!,
                frame = capturedFrame!!,
                viewModel = viewModel,
                onUpdateCorner = { index, x, y ->
                    viewModel.updateCorner(index, x, y)
                },
                onResetCorners = {
                    viewModel.resetCornersToFull()
                },
                onLaunchCropper = {
                    launchCanHubCropper(capturedBitmap!!)
                },
                onApplyAndSave = {
                    val rectified = viewModel.rectifyPerspective()
                    if (rectified != null) {
                        onDocumentProcessed(rectified)
                    } else {
                        Toast.makeText(context, "Gagal meluruskan perspektif dokumen", Toast.LENGTH_SHORT).show()
                    }
                },
                onRetake = {
                    viewModel.clearCapturedState()
                }
            )
        } else {
            // Screen 1: CameraX Live Camera Preview, Frame Detection & Shutter
            if (hasCameraPermission) {
                CameraLiveViewfinder(
                    viewModel = viewModel,
                    onClose = onClose,
                    onPhotoCaptured = { _, _ -> }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Izin Kamera Diperlukan", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Aplikasi membutuhkan akses kamera untuk memindai dokumen secara langsung.",
                            color = Color.LightGray,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Izinkan Kamera")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CameraLiveViewfinder(
    viewModel: CameraViewModel,
    onClose: () -> Unit,
    onPhotoCaptured: (Bitmap, DocumentFrame) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isCameraReady by viewModel.isCameraReady.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val liveDetectedFrame by viewModel.liveDetectedFrame.collectAsState()
    val isFrameDetected by viewModel.isFrameDetected.collectAsState()
    val isBatchMode by viewModel.isBatchMode.collectAsState()
    val batchCount by viewModel.batchCount.collectAsState()

    var previewViewRef by remember { mutableStateOf<PreviewView?>(null) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            viewModel.unbindCamera()
        }
    }

    // Single photo picker from gallery as alternative
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    viewModel.setCapturedBitmap(bitmap)
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal memuat gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // CameraX Live PreviewView using COMPATIBLE (TextureView) to prevent SurfaceView BufferQueue abandonment
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewViewRef = this
                    viewModel.bindCamera(lifecycleOwner, this)
                }
            },
            onRelease = {
                viewModel.unbindCamera()
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        previewViewRef?.let { pv ->
                            viewModel.focusAt(offset.x, offset.y, pv)
                        }
                    }
                }
        )

        // Real-time Live Frame Detection Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frame = liveDetectedFrame
            if (frame != null && isFrameDetected) {
                val origW = frame.originalWidth.toFloat()
                val origH = frame.originalHeight.toFloat()
                if (origW > 0 && origH > 0) {
                    val scaleFactor = maxOf(size.width / origW, size.height / origH)
                    val offsetX = (size.width - origW * scaleFactor) / 2f
                    val offsetY = (size.height - origH * scaleFactor) / 2f

                    val p0 = Offset(offsetX + frame.topLeft.x * scaleFactor, offsetY + frame.topLeft.y * scaleFactor)
                    val p1 = Offset(offsetX + frame.topRight.x * scaleFactor, offsetY + frame.topRight.y * scaleFactor)
                    val p2 = Offset(offsetX + frame.bottomRight.x * scaleFactor, offsetY + frame.bottomRight.y * scaleFactor)
                    val p3 = Offset(offsetX + frame.bottomLeft.x * scaleFactor, offsetY + frame.bottomLeft.y * scaleFactor)

                    val poly = Path().apply {
                        moveTo(p0.x, p0.y)
                        lineTo(p1.x, p1.y)
                        lineTo(p2.x, p2.y)
                        lineTo(p3.x, p3.y)
                        close()
                    }

                    // Illuminated live detection frame
                    drawPath(poly, color = Color(0x2A10B981))
                    drawPath(poly, color = Color(0xFF10B981), style = Stroke(width = 2.5.dp.toPx()))

                    // Corner indicators
                    val corners = listOf(p0, p1, p2, p3)
                    corners.forEach { pt ->
                        drawCircle(Color.White, radius = 6.dp.toPx(), center = pt)
                        drawCircle(Color(0xFF10B981), radius = 4.dp.toPx(), center = pt)
                    }
                }
            }
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
            }

            // Status chip: Live Frame Detection indicator
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, if (isFrameDetected) Color(0xFF10B981) else Color.Gray, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isFrameDetected) Color(0xFF10B981) else Color.Yellow)
                )
                Text(
                    text = if (isFrameDetected) "Bingkai Terdeteksi" else "Mencari Dokumen...",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Torch / Flashlight button
                IconButton(
                    onClick = { viewModel.toggleTorch() },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isTorchOn) Color(0xFFF59E0B) else Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (isTorchOn) Color.Black else Color.White
                    )
                }

                // Switch Camera Lens
                IconButton(
                    onClick = {
                        previewViewRef?.let { pv ->
                            viewModel.switchCamera(lifecycleOwner, pv)
                        }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.Default.Cameraswitch, contentDescription = "Ganti Kamera", tint = Color.White)
                }
            }
        }

        // Bottom Capture Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Batch Mode Toggle
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .clickable { viewModel.toggleBatchMode() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Layers,
                    contentDescription = null,
                    tint = if (isBatchMode) Color(0xFF10B981) else Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = if (isBatchMode) "Mode Multi-Halaman ($batchCount)" else "Mode Satu Halaman",
                    color = if (isBatchMode) Color(0xFF10B981) else Color.LightGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Shutter Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery shortcut
                IconButton(
                    onClick = { galleryPicker.launch("image/*") },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Galeri", tint = Color.White)
                }

                // Main CameraX Shutter Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable(enabled = !isCapturing) {
                            viewModel.takePhoto(
                                context = context,
                                onSuccess = { bmp, frame ->
                                    onPhotoCaptured(bmp, frame)
                                },
                                onError = { error ->
                                    Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(color = Color(0xFF10B981), modifier = Modifier.size(36.dp))
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(if (isFrameDetected) Color(0xFF10B981) else Color.White)
                        )
                    }
                }

                // Spacer for balanced layout
                Box(modifier = Modifier.size(48.dp))
            }
        }
    }
}

/**
 * Interactive UI Component allowing users to manually adjust the 4 corners of the captured image
 * to rectify perspective distortion and straighten slanted/skewed documents.
 */
@Composable
fun ManualCornerRectificationView(
    bitmap: Bitmap,
    frame: DocumentFrame,
    viewModel: CameraViewModel,
    onUpdateCorner: (Int, Float, Float) -> Unit,
    onResetCorners: () -> Unit,
    onLaunchCropper: () -> Unit,
    onApplyAndSave: () -> Unit,
    onRetake: () -> Unit
) {
    val selectedRatio by viewModel.selectedRatio.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var activeDraggingCorner by remember { mutableStateOf<Int?>(null) }
    var currentTouchPos by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(Size.Zero) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRetake) {
                Icon(Icons.Default.Close, contentDescription = "Ulangi", tint = Color.White)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Sesuaikan Sudut Dokumen",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Text(
                    text = if (activeDraggingCorner != null) "Menggeser Sudut #${activeDraggingCorner!! + 1}" else "Tarik sudut untuk memperbaiki perspektif",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onResetCorners) {
                Icon(Icons.Default.RestartAlt, contentDescription = "Reset Sudut", tint = Color.White)
            }
        }

        // Canvas Area with interactive 4 draggable corners and perspective grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
                .onSizeChanged { containerSize = Size(it.width.toFloat(), it.height.toFloat()) },
            contentAlignment = Alignment.Center
        ) {
            // Background captured bitmap
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Hasil Foto",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )

            // Touch gesture detector & overlay canvas
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(frame) {
                        detectDragGestures(
                            onDragStart = { touchOffset ->
                                currentTouchPos = touchOffset
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

                                val hitThreshold = 54.dp.toPx()
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
                                val origW = frame.originalWidth.toFloat()
                                val origH = frame.originalHeight.toFloat()
                                if (origW <= 0f || origH <= 0f) return@detectDragGestures

                                val scaleFactor = minOf(size.width / origW, size.height / origH)
                                val offsetX = (size.width - origW * scaleFactor) / 2f
                                val offsetY = (size.height - origH * scaleFactor) / 2f

                                val bmpX = (change.position.x - offsetX) / scaleFactor
                                val bmpY = (change.position.y - offsetY) / scaleFactor

                                onUpdateCorner(activeCorner, bmpX, bmpY)
                                change.consume()
                            },
                            onDragEnd = { activeDraggingCorner = null },
                            onDragCancel = { activeDraggingCorner = null }
                        )
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
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

                        val polygonPath = Path().apply {
                            moveTo(p0.x, p0.y)
                            lineTo(p1.x, p1.y)
                            lineTo(p2.x, p2.y)
                            lineTo(p3.x, p3.y)
                            close()
                        }

                        // Interior quadrilateral fill
                        drawPath(polygonPath, color = Color(0x3310B981))
                        // Exterior border line
                        drawPath(polygonPath, color = Color(0xFF10B981), style = Stroke(width = 3.dp.toPx()))

                        // 3x3 Perspective grid lines
                        fun interpolate(a: Offset, b: Offset, fraction: Float): Offset {
                            return Offset(a.x + (b.x - a.x) * fraction, a.y + (b.y - a.y) * fraction)
                        }

                        val gridColor = Color(0x6610B981)
                        drawLine(gridColor, interpolate(p0, p3, 0.33f), interpolate(p1, p2, 0.33f), strokeWidth = 1.dp.toPx())
                        drawLine(gridColor, interpolate(p0, p3, 0.67f), interpolate(p1, p2, 0.67f), strokeWidth = 1.dp.toPx())
                        drawLine(gridColor, interpolate(p0, p1, 0.33f), interpolate(p3, p2, 0.33f), strokeWidth = 1.dp.toPx())
                        drawLine(gridColor, interpolate(p0, p1, 0.67f), interpolate(p3, p2, 0.67f), strokeWidth = 1.dp.toPx())

                        // 4 Interactive Corner Handles
                        val corners = listOf(p0, p1, p2, p3)
                        corners.forEachIndexed { idx, pt ->
                            val isCurrentActive = activeDraggingCorner == idx
                            val radius = if (isCurrentActive) 14.dp.toPx() else 8.dp.toPx()

                            if (isCurrentActive) {
                                drawCircle(Color(0x6610B981), radius = radius + 8.dp.toPx(), center = pt)
                                drawLine(Color.White, Offset(pt.x - 16.dp.toPx(), pt.y), Offset(pt.x + 16.dp.toPx(), pt.y), strokeWidth = 2.dp.toPx())
                                drawLine(Color.White, Offset(pt.x, pt.y - 16.dp.toPx()), Offset(pt.x, pt.y + 16.dp.toPx()), strokeWidth = 2.dp.toPx())
                            }

                            drawCircle(Color.White, radius = radius + 2.dp.toPx(), center = pt)
                            drawCircle(if (isCurrentActive) Color(0xFF059669) else Color(0xFF10B981), radius = radius, center = pt)
                            drawCircle(Color.White, radius = 2.5.dp.toPx(), center = pt)
                        }
                    }
                }
            }

            // Magnified View Loupe when dragging a corner
            if (activeDraggingCorner != null) {
                val cornerIdx = activeDraggingCorner!!
                val pt = when (cornerIdx) {
                    0 -> Offset(frame.topLeft.x, frame.topLeft.y)
                    1 -> Offset(frame.topRight.x, frame.topRight.y)
                    2 -> Offset(frame.bottomRight.x, frame.bottomRight.y)
                    else -> Offset(frame.bottomLeft.x, frame.bottomLeft.y)
                }
                CornerMagnifierLoupe(
                    bitmap = bitmap,
                    bmpPoint = pt,
                    touchPosition = currentTouchPos,
                    cornerIndex = cornerIdx,
                    containerSize = containerSize,
                    zoomFactor = 2.6f
                )
            }
        }

        // Bottom Controls: Ratio Presets, Cropper Shortcut & Dewarp Button
        Surface(
            color = Color(0xFF1E293B),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Aspect Ratio Selector
                Text(
                    text = "Rasio Proporsional:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(DocumentScaleRatio.values()) { ratio ->
                        FilterChip(
                            selected = selectedRatio == ratio,
                            onClick = { viewModel.setScaleRatio(ratio) },
                            label = { Text(ratio.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF334155),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Document Filter Selector (Color Enhancement, B&W, Grayscale, Original)
                Text(
                    text = "Pilih Filter Dokumen:",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(DocumentFilter.values()) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.displayName, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF10B981),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFF334155),
                                labelColor = Color.LightGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Action Buttons: Potong Bebas (Cropper Library) & Luruskan Dokumen (Perspective Rectification)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Button to launch image cropper library
                    Button(
                        onClick = onLaunchCropper,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Crop, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Potong Bebas", fontSize = 12.sp, color = Color.White)
                    }

                    // Button to apply homography perspective correction & save
                    Button(
                        onClick = onApplyAndSave,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Luruskan & Simpan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

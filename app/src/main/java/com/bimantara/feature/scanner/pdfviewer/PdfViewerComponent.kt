package com.bimantara.feature.scanner.pdfviewer

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FitScreen
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.bimantara.data.model.ScannedDocEntity
import com.bimantara.feature.scanner.CamScannerViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Dedicated PDF viewing component with:
 * - Multi-Tab support (open multiple documents simultaneously)
 * - Hardware-accelerated lightweight rendering
 * - Smooth Pinch-to-Zoom, double-tap zoom, and floating zoom controls
 * - Page navigation with previous/next, jump slider, and horizontal thumbnail carousel
 * - Share, print, and metadata preview
 */
@Composable
fun PdfViewerComponent(
    viewModel: CamScannerViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val openTabs by viewModel.openPdfTabs.collectAsState()
    val activeTabIndex by viewModel.activePdfTabIndex.collectAsState()
    val scannedDocs by viewModel.scannedDocs.collectAsState()

    var showOpenDocDialog by remember { mutableStateOf(false) }
    var showDocDetailsDialog by remember { mutableStateOf(false) }
    var showThumbnailStrip by remember { mutableStateOf(true) }
    var showJumpPageDialog by remember { mutableStateOf(false) }

    val activeTab = openTabs.getOrNull(activeTabIndex)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .testTag("pdf_viewer_root")
    ) {
        // TOP APP BAR
        Surface(
            color = Color(0xFF1E293B),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("pdf_viewer_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali ke Galeri",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = activeTab?.title ?: "Pratinjau PDF",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (activeTab != null) {
                        Text(
                            text = "${activeTab.file.name} • ${activeTab.totalPages} Hal • ${activeTab.file.length() / 1024} KB",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (activeTab != null) {
                    // Document Details / Metadata Button
                    IconButton(
                        onClick = { showDocDetailsDialog = true },
                        modifier = Modifier.size(38.dp).testTag("pdf_viewer_info_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Informasi Dokumen",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Share PDF Button
                    IconButton(
                        onClick = {
                            sharePdfFile(context, activeTab.file, activeTab.title)
                        },
                        modifier = Modifier.size(38.dp).testTag("pdf_viewer_share_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Bagikan PDF",
                            tint = Color(0xFF34D399),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // MULTI-TAB BAR
        PdfTabBar(
            tabs = openTabs,
            activeTabIndex = activeTabIndex,
            onSelectTab = { viewModel.selectPdfTab(it) },
            onCloseTab = { viewModel.closePdfTab(it) },
            onOpenNewTab = { showOpenDocDialog = true }
        )

        // MAIN CONTENT (ACTIVE PDF OR EMPTY STATE)
        if (activeTab != null && activeTab.file.exists()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                PdfPageView(
                    activeTab = activeTab,
                    onZoomChanged = { scale, offX, offY ->
                        viewModel.setPdfZoom(scale, offX, offY)
                    },
                    onResetZoom = {
                        viewModel.resetPdfZoom()
                    }
                )

                // FLOATING ZOOM CONTROLS (Top Right of Viewport)
                FloatingZoomControls(
                    zoomScale = activeTab.zoomScale,
                    onZoomIn = { viewModel.setPdfZoom((activeTab.zoomScale + 0.25f).coerceAtMost(5.0f)) },
                    onZoomOut = { viewModel.setPdfZoom((activeTab.zoomScale - 0.25f).coerceAtLeast(0.75f)) },
                    onResetZoom = { viewModel.resetPdfZoom() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )

                // FLOATING BOTTOM DOCK: Page Controls & Jump
                FloatingPageControls(
                    currentPage = activeTab.currentPage,
                    totalPages = activeTab.totalPages,
                    onPrevPage = { viewModel.prevPdfPage() },
                    onNextPage = { viewModel.nextPdfPage() },
                    onPageIndicatorClick = { showJumpPageDialog = true },
                    isThumbnailStripVisible = showThumbnailStrip,
                    onToggleThumbnailStrip = { showThumbnailStrip = !showThumbnailStrip },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (showThumbnailStrip) 100.dp else 16.dp)
                )

                // BOTTOM THUMBNAIL CAROUSEL STRIP
                Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                    AnimatedVisibility(
                        visible = showThumbnailStrip,
                        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                    ) {
                        PdfThumbnailStrip(
                            file = activeTab.file,
                            totalPages = activeTab.totalPages,
                            currentPage = activeTab.currentPage,
                            onSelectPage = { pageIdx ->
                                viewModel.setPdfCurrentPage(pageIdx)
                            }
                        )
                    }
                }
            }
        } else {
            // Empty Tab State
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tidak Ada Tab Dokumen Terbuka",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Buka dokumen PDF dari daftar pindaian Anda untuk mulai membaca.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    androidx.compose.material3.Button(
                        onClick = { showOpenDocDialog = true },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0284C7)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pilih Dokumen PDF")
                    }
                }
            }
        }
    }

    // DIALOG: Open Document from Database into a Tab
    if (showOpenDocDialog) {
        OpenPdfTabDialog(
            docs = scannedDocs,
            onSelectDoc = { doc ->
                showOpenDocDialog = false
                viewModel.openPdfViewerFromDocEntity(doc)
            },
            onDismiss = { showOpenDocDialog = false }
        )
    }

    // DIALOG: Jump to Specific Page
    if (showJumpPageDialog && activeTab != null) {
        JumpPageDialog(
            currentPage = activeTab.currentPage,
            totalPages = activeTab.totalPages,
            onConfirmJump = { targetPage ->
                viewModel.setPdfCurrentPage(targetPage)
                showJumpPageDialog = false
            },
            onDismiss = { showJumpPageDialog = false }
        )
    }

    // DIALOG: PDF Metadata & Details
    if (showDocDetailsDialog && activeTab != null) {
        AlertDialog(
            onDismissRequest = { showDocDetailsDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Informasi Dokumen PDF",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Judul: ${activeTab.title}", fontWeight = FontWeight.SemiBold)
                    Text(text = "Nama Berkas: ${activeTab.file.name}")
                    Text(text = "Total Halaman: ${activeTab.totalPages} Halaman")
                    Text(text = "Ukuran Berkas: ${activeTab.file.length() / 1024} KB")
                    Text(
                        text = "Terakhir Dimodifikasi: ${SimpleDateFormat("dd MMMM yyyy HH:mm", Locale.getDefault()).format(Date(activeTab.file.lastModified()))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Lokasi: ${activeTab.file.absolutePath}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showDocDetailsDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

/**
 * Multi-Tab Header Bar with horizontal scroll, active indicators, and tab close buttons.
 */
@Composable
private fun PdfTabBar(
    tabs: List<PdfTabItem>,
    activeTabIndex: Int,
    onSelectTab: (Int) -> Unit,
    onCloseTab: (String) -> Unit,
    onOpenNewTab: () -> Unit
) {
    Surface(
        color = Color(0xFF0B132B),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isActive = index == activeTabIndex
                Surface(
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    color = if (isActive) Color(0xFF1E293B) else Color(0xFF162032),
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clickable { onSelectTab(index) }
                        .testTag("pdf_tab_$index")
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = if (isActive) Color(0xFFEF4444) else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.title,
                            color = if (isActive) Color.White else Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 130.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Close tab button
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .clickable { onCloseTab(tab.id) }
                                .testTag("pdf_close_tab_${index}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup Tab",
                                tint = if (isActive) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }

            // Add Tab (+) Button
            IconButton(
                onClick = onOpenNewTab,
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFF1E293B), CircleShape)
                    .testTag("pdf_new_tab_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Buka Tab Baru",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Main PDF Page rendering view with Hardware-accelerated native rendering and Pan/Zoom gestures.
 */
@Composable
private fun PdfPageView(
    activeTab: PdfTabItem,
    onZoomChanged: (Float, Float, Float) -> Unit,
    onResetZoom: () -> Unit
) {
    var renderedBitmap by remember(activeTab.file.absolutePath, activeTab.currentPage) {
        mutableStateOf<Bitmap?>(null)
    }
    var isLoading by remember(activeTab.file.absolutePath, activeTab.currentPage) {
        mutableStateOf(true)
    }

    // Local gesture state
    var scale by remember(activeTab.id, activeTab.currentPage) {
        mutableFloatStateOf(activeTab.zoomScale)
    }
    var offsetX by remember(activeTab.id, activeTab.currentPage) {
        mutableFloatStateOf(activeTab.panOffsetX)
    }
    var offsetY by remember(activeTab.id, activeTab.currentPage) {
        mutableFloatStateOf(activeTab.panOffsetY)
    }

    // Sync external zoom state changes (e.g. from +/- buttons)
    LaunchedEffect(activeTab.zoomScale) {
        scale = activeTab.zoomScale
        if (scale <= 1.05f) {
            offsetX = 0f
            offsetY = 0f
        }
    }

    // Render page asynchronously
    LaunchedEffect(activeTab.file.absolutePath, activeTab.currentPage) {
        isLoading = true
        val bmp = LightweightPdfRenderer.renderPage(
            file = activeTab.file,
            pageIndex = activeTab.currentPage,
            targetWidth = 1400
        )
        renderedBitmap = bmp
        isLoading = false
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .pointerInput(activeTab.id, activeTab.currentPage) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (scale * zoom).coerceIn(0.75f, 5.0f)
                    val maxPanX = (size.width * (newScale - 1f) / 2f).coerceAtLeast(0f)
                    val maxPanY = (size.height * (newScale - 1f) / 2f).coerceAtLeast(0f)

                    val newOffsetX = if (newScale > 1.0f) {
                        (offsetX + pan.x * newScale).coerceIn(-maxPanX, maxPanX)
                    } else 0f

                    val newOffsetY = if (newScale > 1.0f) {
                        (offsetY + pan.y * newScale).coerceIn(-maxPanY, maxPanY)
                    } else 0f

                    scale = newScale
                    offsetX = newOffsetX
                    offsetY = newOffsetY
                    onZoomChanged(newScale, newOffsetX, newOffsetY)
                }
            }
            .pointerInput(activeTab.id, activeTab.currentPage) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1.2f) {
                            scale = 1.0f
                            offsetX = 0f
                            offsetY = 0f
                            onResetZoom()
                        } else {
                            scale = 2.5f
                            offsetX = 0f
                            offsetY = 0f
                            onZoomChanged(2.5f, 0f, 0f)
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF38BDF8),
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Memuat Halaman ${activeTab.currentPage + 1}...",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        } else if (renderedBitmap != null) {
            Card(
                shape = RoundedCornerShape(4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offsetX
                        translationY = offsetY
                    }
                    .padding(16.dp)
            ) {
                Image(
                    bitmap = renderedBitmap!!.asImageBitmap(),
                    contentDescription = "Halaman ${activeTab.currentPage + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(0.96f)
                )
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Gagal merender halaman dokumen ini.",
                    color = Color(0xFFF87171),
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Floating Zoom Control Bar with +, -, Percentage, and Fit-to-Screen buttons.
 */
@Composable
private fun FloatingZoomControls(
    zoomScale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xCC1E293B),
        shadowElevation = 6.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onZoomOut,
                modifier = Modifier.size(32.dp).testTag("pdf_zoom_out_btn"),
                enabled = zoomScale > 0.75f
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = "Perkecil",
                    tint = if (zoomScale > 0.75f) Color.White else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = "${(zoomScale * 100).roundToInt()}%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .clickable { onResetZoom() }
            )

            IconButton(
                onClick = onZoomIn,
                modifier = Modifier.size(32.dp).testTag("pdf_zoom_in_btn"),
                enabled = zoomScale < 5.0f
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Perbesar",
                    tint = if (zoomScale < 5.0f) Color.White else Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(2.dp))

            IconButton(
                onClick = onResetZoom,
                modifier = Modifier.size(32.dp).testTag("pdf_zoom_fit_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.FitScreen,
                    contentDescription = "Reset Zoom",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

/**
 * Floating Dock for Page Navigation (Previous, Current/Total, Next, Thumbnail Toggle).
 */
@Composable
private fun FloatingPageControls(
    currentPage: Int,
    totalPages: Int,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onPageIndicatorClick: () -> Unit,
    isThumbnailStripVisible: Boolean,
    onToggleThumbnailStrip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xEE1E293B),
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Previous Page Button
            IconButton(
                onClick = onPrevPage,
                modifier = Modifier.size(36.dp).testTag("pdf_prev_page_btn"),
                enabled = currentPage > 0
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateBefore,
                    contentDescription = "Halaman Sebelumnya",
                    tint = if (currentPage > 0) Color.White else Color(0xFF475569),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Page Indicator Button (Clickable to jump)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF334155),
                modifier = Modifier
                    .clickable { onPageIndicatorClick() }
                    .testTag("pdf_page_indicator_btn")
            ) {
                Text(
                    text = "Hal. ${currentPage + 1} / $totalPages",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Next Page Button
            IconButton(
                onClick = onNextPage,
                modifier = Modifier.size(36.dp).testTag("pdf_next_page_btn"),
                enabled = currentPage < totalPages - 1
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.NavigateNext,
                    contentDescription = "Halaman Berikutnya",
                    tint = if (currentPage < totalPages - 1) Color.White else Color(0xFF475569),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Thumbnail Strip Toggle Button
            IconButton(
                onClick = onToggleThumbnailStrip,
                modifier = Modifier.size(36.dp).testTag("pdf_toggle_thumbnails_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.GridView,
                    contentDescription = "Toggle Strip Thumbnail",
                    tint = if (isThumbnailStripVisible) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Bottom Carousel Strip showing thumbnails of all pages in the PDF for instant visual jumping.
 */
@Composable
private fun PdfThumbnailStrip(
    file: File,
    totalPages: Int,
    currentPage: Int,
    onSelectPage: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    // Scroll to current page thumbnail
    LaunchedEffect(currentPage) {
        if (currentPage in 0 until totalPages) {
            listState.animateScrollToItem(currentPage)
        }
    }

    Surface(
        color = Color(0xF00B132B),
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = "Strip Thumbnail Halaman Dokumen",
                color = Color(0xFF94A3B8),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
            )

            LazyRow(
                state = listState,
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(76.dp)
            ) {
                items(totalPages) { pageIndex ->
                    PdfThumbnailItem(
                        file = file,
                        pageIndex = pageIndex,
                        isSelected = pageIndex == currentPage,
                        onClick = { onSelectPage(pageIndex) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PdfThumbnailItem(
    file: File,
    pageIndex: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    var thumbBitmap by remember(file.absolutePath, pageIndex) {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(file.absolutePath, pageIndex) {
        thumbBitmap = LightweightPdfRenderer.renderThumbnail(file, pageIndex, 160)
    }

    Box(
        modifier = Modifier
            .width(52.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF1E293B))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() }
            .testTag("pdf_thumb_$pageIndex"),
        contentAlignment = Alignment.Center
    ) {
        if (thumbBitmap != null) {
            Image(
                bitmap = thumbBitmap!!.asImageBitmap(),
                contentDescription = "Thumbnail Hal ${pageIndex + 1}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = Color(0xFF38BDF8),
                modifier = Modifier.size(16.dp)
            )
        }

        // Page Number Badge
        Surface(
            color = if (isSelected) Color(0xFF0284C7) else Color(0xCC000000),
            shape = RoundedCornerShape(bottomStart = 4.dp),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Text(
                text = "${pageIndex + 1}",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }
    }
}

/**
 * Dialog to open an existing scanned document into a new PDF tab.
 */
@Composable
private fun OpenPdfTabDialog(
    docs: List<ScannedDocEntity>,
    onSelectDoc: (ScannedDocEntity) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFF0284C7))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buka Dokumen di Tab Baru")
            }
        },
        text = {
            if (docs.isEmpty()) {
                Text("Belum ada dokumen pindaian yang tersimpan di perangkat.")
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Text(
                        text = "Pilih dokumen untuk dibuka ke dalam tab pratinjau:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(docs) { _, doc ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelectDoc(doc) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = Color(0xFF0284C7),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = doc.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (doc.pdfPath != null) "PDF Tersedia" else "Buat PDF Instan",
                                            fontSize = 11.sp,
                                            color = if (doc.pdfPath != null) Color(0xFF10B981) else Color(0xFFF59E0B)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

/**
 * Jump to Specific Page Dialog with Slider.
 */
@Composable
private fun JumpPageDialog(
    currentPage: Int,
    totalPages: Int,
    onConfirmJump: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var targetPage by remember { mutableIntStateOf(currentPage + 1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lompat ke Halaman") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Halaman $targetPage dari $totalPages",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF0284C7)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Slider(
                    value = targetPage.toFloat(),
                    onValueChange = { targetPage = it.roundToInt() },
                    valueRange = 1f..totalPages.toFloat().coerceAtLeast(1f),
                    steps = (totalPages - 2).coerceAtLeast(0),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirmJump(targetPage - 1) }) {
                Text("Lompat")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

/**
 * Share PDF file helper via Android Intent.
 */
private fun sharePdfFile(context: Context, pdfFile: File, title: String) {
    try {
        if (!pdfFile.exists()) {
            Toast.makeText(context, "Berkas PDF tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            pdfFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Dokumen PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

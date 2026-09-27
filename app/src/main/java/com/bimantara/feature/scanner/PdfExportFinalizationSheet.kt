package com.bimantara.feature.scanner

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterBAndW
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimantara.core.i18n.LocalAppStrings
import java.io.File

/**
 * Bottom Sheet dialog displayed before finalizing the multi-page PDF export.
 *
 * Allows users to review all pages, apply Grayscale, Black & White, Color Enhancement (Magic Color),
 * or Original filters either globally (to all pages in one tap) or selectively per-page,
 * rotate individual pages, adjust document title, and confirm PDF compilation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfExportFinalizationSheet(
    viewModel: CamScannerViewModel,
    onDismiss: () -> Unit,
    onPdfGenerated: (File) -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    val pages by viewModel.pages.collectAsState()
    val isExportingPdf by viewModel.isExportingPdf.collectAsState()
    val autoRotateExport by viewModel.autoRotateExport.collectAsState()
    val currentTitle by viewModel.documentTitle.collectAsState()

    var editableTitle by remember(currentTitle) { mutableStateOf(currentTitle) }
    var globalFilterAppliedFeedback by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = { if (!isExportingPdf) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
        modifier = modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            // Header Bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFDC2626).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Finalisasi Ekspor PDF",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${pages.size} Halaman Dokumen Scan",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        enabled = !isExportingPdf
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Scrollable Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Document Name Input
                Text(
                    text = "Nama Dokumen PDF",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = editableTitle,
                    onValueChange = {
                        editableTitle = it
                        viewModel.setDocumentTitle(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_doc_title_input"),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 1: GLOBAL FILTER QUICK ACTIONS (APPLY TO ALL PAGES)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
                    ),
                    border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Terapkan Filter ke Semua Halaman",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Pilih filter di bawah untuk mengubah seluruh ${pages.size} halaman sekaligus",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 4 Global Filter Options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GlobalFilterActionButton(
                                title = "Warna Ajaib",
                                subtitle = "Tingkatkan",
                                icon = Icons.Default.AutoFixHigh,
                                accentColor = Color(0xFF10B981),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.applyFilterToAllPages(DocumentFilter.MAGIC_COLOR)
                                    globalFilterAppliedFeedback = "Filter Warna Ajaib diterapkan ke semua halaman"
                                    Toast.makeText(context, "Semua halaman diubah ke Warna Ajaib", Toast.LENGTH_SHORT).show()
                                }
                            )
                            GlobalFilterActionButton(
                                title = "Hitam-Putih",
                                subtitle = "B&W Bersih",
                                icon = Icons.Default.Contrast,
                                accentColor = Color(0xFF1E293B),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.applyFilterToAllPages(DocumentFilter.BW)
                                    globalFilterAppliedFeedback = "Filter Hitam-Putih diterapkan ke semua halaman"
                                    Toast.makeText(context, "Semua halaman diubah ke Hitam-Putih", Toast.LENGTH_SHORT).show()
                                }
                            )
                            GlobalFilterActionButton(
                                title = "Grayscale",
                                subtitle = "Abu-abu",
                                icon = Icons.Default.FilterBAndW,
                                accentColor = Color(0xFF64748B),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.applyFilterToAllPages(DocumentFilter.GRAYSCALE)
                                    globalFilterAppliedFeedback = "Filter Grayscale diterapkan ke semua halaman"
                                    Toast.makeText(context, "Semua halaman diubah ke Grayscale", Toast.LENGTH_SHORT).show()
                                }
                            )
                            GlobalFilterActionButton(
                                title = "Foto Asli",
                                subtitle = "Natural",
                                icon = Icons.Default.Image,
                                accentColor = Color(0xFF3B82F6),
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.applyFilterToAllPages(DocumentFilter.ORIGINAL)
                                    globalFilterAppliedFeedback = "Filter Foto Asli diterapkan ke semua halaman"
                                    Toast.makeText(context, "Semua halaman diubah ke Foto Asli", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }

                        AnimatedVisibility(visible = globalFilterAppliedFeedback != null) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.12f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = globalFilterAppliedFeedback ?: "",
                                        color = Color(0xFF047857),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // SECTION 2: PER-PAGE THUMBNAIL & FILTER ADJUSTMENT CAROUSEL
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Tinjau & Atur Filter per Halaman",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ubah filter atau rotasi khusus pada halaman tertentu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = "${pages.size} Hal",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Horizontal Page Review Strip
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 4.dp)
                ) {
                    itemsIndexed(pages) { index, page ->
                        PageReviewItemCard(
                            pageIndex = index,
                            totalPages = pages.size,
                            page = page,
                            onFilterSelected = { filter ->
                                viewModel.setFilterForPage(index, filter)
                            },
                            onRotate = {
                                viewModel.rotatePage(index)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // SECTION 3: EXPORT SETTINGS & ROTATION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Otomatis Tegakkan Halaman Landscape",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Rotasi otomatis halaman melebar ke format tegak (portrait) A4 agar rapi",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = autoRotateExport,
                                onCheckedChange = { viewModel.toggleAutoRotateExport() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF10B981)
                                )
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Standar Output: Dokumen PDF Multihalaman (A4 595x842 pt)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Bottom Action Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cancel / Return Button
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isExportingPdf
                    ) {
                        Text("Batal", maxLines = 1)
                    }

                    // Confirm & Export PDF Button
                    Button(
                        onClick = {
                            viewModel.exportToPdf { file ->
                                if (file != null) {
                                    onPdfGenerated(file)
                                } else {
                                    Toast.makeText(context, "Gagal membuat PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(2f)
                            .testTag("confirm_export_pdf_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        enabled = !isExportingPdf && pages.isNotEmpty()
                    ) {
                        if (isExportingPdf) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mengompilasi PDF...", fontSize = 13.sp)
                        } else {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Konfirmasi & Ekspor PDF (${pages.size} Hal)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Individual Page Card inside the Pre-Export Finalization Carousel.
 */
@Composable
private fun PageReviewItemCard(
    pageIndex: Int,
    totalPages: Int,
    page: ScannedPage,
    onFilterSelected: (DocumentFilter) -> Unit,
    onRotate: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Page Header & Rotation Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Hal ${pageIndex + 1} / $totalPages",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = onRotate,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.RotateRight,
                        contentDescription = "Putar 90°",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Thumbnail Preview with Filter Applied
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = page.enhancedBitmap.asImageBitmap(),
                    contentDescription = "Halaman ${pageIndex + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Current Filter Badge over thumbnail
                val (badgeBg, badgeText) = when (page.filter) {
                    DocumentFilter.MAGIC_COLOR -> Color(0xFF10B981) to "Warna Ajaib"
                    DocumentFilter.BW -> Color(0xFF1E293B) to "Hitam-Putih"
                    DocumentFilter.GRAYSCALE -> Color(0xFF64748B) to "Grayscale"
                    DocumentFilter.ORIGINAL -> Color(0xFF3B82F6) to "Foto Asli"
                }

                Surface(
                    color = badgeBg.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Filter Selector Chips for this specific page
            Text(
                text = "Pilih Filter Halaman Ini:",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MiniFilterOptionButton(
                    label = "Warna",
                    icon = Icons.Default.AutoFixHigh,
                    selected = page.filter == DocumentFilter.MAGIC_COLOR,
                    activeColor = Color(0xFF10B981),
                    modifier = Modifier.weight(1f),
                    onClick = { onFilterSelected(DocumentFilter.MAGIC_COLOR) }
                )
                MiniFilterOptionButton(
                    label = "B&W",
                    icon = Icons.Default.Contrast,
                    selected = page.filter == DocumentFilter.BW,
                    activeColor = Color(0xFF1E293B),
                    modifier = Modifier.weight(1f),
                    onClick = { onFilterSelected(DocumentFilter.BW) }
                )
                MiniFilterOptionButton(
                    label = "Gray",
                    icon = Icons.Default.FilterBAndW,
                    selected = page.filter == DocumentFilter.GRAYSCALE,
                    activeColor = Color(0xFF64748B),
                    modifier = Modifier.weight(1f),
                    onClick = { onFilterSelected(DocumentFilter.GRAYSCALE) }
                )
                MiniFilterOptionButton(
                    label = "Asli",
                    icon = Icons.Default.Image,
                    selected = page.filter == DocumentFilter.ORIGINAL,
                    activeColor = Color(0xFF3B82F6),
                    modifier = Modifier.weight(1f),
                    onClick = { onFilterSelected(DocumentFilter.ORIGINAL) }
                )
            }
        }
    }
}

/**
 * Compact icon button for choosing a filter on an individual page thumbnail.
 */
@Composable
private fun MiniFilterOptionButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) activeColor else MaterialTheme.colorScheme.surface
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (selected) activeColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            fontSize = 8.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = contentColor,
            maxLines = 1
        )
    }
}

/**
 * Action button in the Global Filter Bar (e.g. Apply to All Pages).
 */
@Composable
private fun GlobalFilterActionButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(width = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 8.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

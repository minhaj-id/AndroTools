package com.bimantara.feature.mdreader

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MdReaderScreen(
    viewModel: MdReaderViewModel,
    onBack: (() -> Unit)? = null,
    onOpenPdfViewer: ((File) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val title by viewModel.documentTitle.collectAsState()
    val rawContent by viewModel.rawContent.collectAsState()
    val parsedBlocks by viewModel.parsedBlocks.collectAsState()
    val isEditMode by viewModel.isEditMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isExportingPdf by viewModel.isExportingPdf.collectAsState()
    val lastExportedPdf by viewModel.lastExportedPdf.collectAsState()

    var showSearchRow by remember { mutableStateOf(false) }
    var showSamplesMenu by remember { mutableStateOf(false) }
    var exportedDialogFile by remember { mutableStateOf<File?>(null) }

    // Open file launcher (.md, .txt, etc.)
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.loadFromUri(uri, context)
            Toast.makeText(context, "Berkas Markdown dimuat", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("screen_md_reader"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = if (isEditMode) "Mode Editor Markdown" else "Mode Pratinjau Terformat",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "MD Reader",
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.padding(start = 12.dp).size(26.dp)
                        )
                    }
                },
                actions = {
                    // Search toggle
                    IconButton(onClick = {
                        showSearchRow = !showSearchRow
                        if (!showSearchRow) viewModel.setSearchQuery("")
                    }) {
                        Icon(Icons.Default.Search, contentDescription = "Cari Teks")
                    }

                    // Open File
                    IconButton(onClick = {
                        openFileLauncher.launch(arrayOf("text/markdown", "text/plain", "*/*"))
                    }) {
                        Icon(Icons.Default.FolderOpen, contentDescription = "Buka Berkas .md")
                    }

                    // Sample documents
                    Box {
                        IconButton(onClick = { showSamplesMenu = true }) {
                            Icon(Icons.Default.MenuBook, contentDescription = "Contoh Dokumen")
                        }
                        DropdownMenu(
                            expanded = showSamplesMenu,
                            onDismissRequest = { showSamplesMenu = false }
                        ) {
                            viewModel.samples.forEach { sample ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(sample.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Text(sample.description, fontSize = 11.sp, color = Color.Gray)
                                        }
                                    },
                                    onClick = {
                                        viewModel.loadSample(sample)
                                        showSamplesMenu = false
                                        Toast.makeText(context, "Memuat ${sample.title}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }

                    // Toggle Preview / Edit Mode
                    IconButton(
                        onClick = { viewModel.toggleEditMode() },
                        modifier = Modifier.testTag("md_toggle_mode_btn")
                    ) {
                        Icon(
                            imageVector = if (isEditMode) Icons.Default.Visibility else Icons.Default.Edit,
                            contentDescription = if (isEditMode) "Pratinjau" else "Edit",
                            tint = if (isEditMode) Color(0xFF10B981) else Color(0xFF0284C7)
                        )
                    }

                    // Export PDF Button
                    IconButton(
                        onClick = {
                            viewModel.exportToPdf(context) { file ->
                                exportedDialogFile = file
                            }
                        },
                        enabled = !isExportingPdf,
                        modifier = Modifier.testTag("md_export_pdf_btn")
                    ) {
                        if (isExportingPdf) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "Ekspor ke PDF",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Row
            AnimatedVisibility(visible = showSearchRow) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Cari kata dalam dokumen...", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f).height(48.dp),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Bersihkan", modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                    }
                }
            }

            // Quick formatting bar when in edit mode
            if (isEditMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SnippetButton("H1") { viewModel.updateContent("# " + rawContent) }
                    SnippetButton("H2") { viewModel.updateContent("## " + rawContent) }
                    SnippetButton("B") { viewModel.updateContent("$rawContent\n**Teks Tebal**") }
                    SnippetButton("I") { viewModel.updateContent("$rawContent\n*Teks Miring*") }
                    SnippetButton("Code") { viewModel.updateContent("$rawContent\n```\nkode di sini\n```") }
                    SnippetButton("Quote") { viewModel.updateContent("$rawContent\n> Kutipan penting") }
                    SnippetButton("List") { viewModel.updateContent("$rawContent\n- Butir 1\n- Butir 2") }
                    SnippetButton("Task") { viewModel.updateContent("$rawContent\n- [ ] Tugas baru") }
                    SnippetButton("Tabel") { viewModel.updateContent("$rawContent\n| Kolom 1 | Kolom 2 |\n| --- | --- |\n| Data A | Data B |") }
                }
            }

            // Body: Preview or Raw Editor
            if (isEditMode) {
                OutlinedTextField(
                    value = rawContent,
                    onValueChange = { viewModel.updateContent(it) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .testTag("md_raw_editor_field"),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp
                    ),
                    placeholder = { Text("Tulis dokumen Markdown di sini...") }
                )
            } else {
                // Formatted Preview
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("md_preview_list"),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(parsedBlocks) { block ->
                        MarkdownBlockItem(
                            block = block,
                            searchQuery = searchQuery,
                            onTaskToggle = { isChecked ->
                                // Optional interactive toggle
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }

    // Export PDF Dialog
    exportedDialogFile?.let { pdfFile ->
        AlertDialog(
            onDismissRequest = { exportedDialogFile = null },
            icon = {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(36.dp))
            },
            title = {
                Text("Ekspor PDF Berhasil!", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Dokumen berhasil dikonversi ke PDF standar A4.")
                    Text("Nama Berkas: ${pdfFile.name}", fontSize = 12.sp, color = Color.Gray)
                    Text("Ukuran: ${pdfFile.length() / 1024} KB", fontSize = 12.sp, color = Color.Gray)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fileToOpen = pdfFile
                        exportedDialogFile = null
                        if (onOpenPdfViewer != null) {
                            onOpenPdfViewer(fileToOpen)
                        } else {
                            sharePdf(context, fileToOpen)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    Text(if (onOpenPdfViewer != null) "Buka di PDF Viewer" else "Buka / Bagikan")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        sharePdf(context, pdfFile)
                    }) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bagikan")
                    }
                    TextButton(onClick = { exportedDialogFile = null }) {
                        Text("Tutup")
                    }
                }
            }
        )
    }
}

@Composable
fun SnippetButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.height(32.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MarkdownBlockItem(
    block: MarkdownBlock,
    searchQuery: String,
    onTaskToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current

    when (block) {
        is MarkdownBlock.Heading -> {
            val (fontSize, fontWeight, color) = when (block.level) {
                1 -> Triple(22.sp, FontWeight.ExtraBold, MaterialTheme.colorScheme.primary)
                2 -> Triple(18.sp, FontWeight.Bold, MaterialTheme.colorScheme.onSurface)
                3 -> Triple(15.sp, FontWeight.SemiBold, MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Triple(14.sp, FontWeight.Medium, MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = block.text,
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = color,
                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
            )
        }

        is MarkdownBlock.Paragraph -> {
            val formatted = MarkdownParser.parseInlineFormatting(block.text)
            Text(
                text = formatted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        is MarkdownBlock.CodeBlock -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (block.language.isNotBlank()) block.language.uppercase() else "CODE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Kode", block.code))
                                Toast.makeText(context, "Kode disalin ke papan klip", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Salin Kode",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = block.code,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        is MarkdownBlock.Blockquote -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0284C7).copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                    .border(
                        width = 1.dp,
                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .padding(vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .background(Color(0xFF0284C7), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = MarkdownParser.parseInlineFormatting(block.text),
                    fontSize = 13.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        is MarkdownBlock.ListItem -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (block.isTask) {
                    var checked by remember { mutableStateOf(block.isChecked) }
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            checked = it
                            onTaskToggle(it)
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF0284C7)
                        ),
                        modifier = Modifier.size(28.dp)
                    )
                } else {
                    Text(
                        text = if (block.ordered) "${block.number}." else "•",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7),
                        modifier = Modifier.width(22.dp)
                    )
                }
                Text(
                    text = MarkdownParser.parseInlineFormatting(block.text),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        is MarkdownBlock.Table -> {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Headers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                            .padding(8.dp)
                    ) {
                        block.headers.forEach { h ->
                            Text(
                                text = h,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    // Rows
                    block.rows.forEachIndexed { idx, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (idx % 2 == 0) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                                .padding(8.dp)
                        ) {
                            row.forEach { cell ->
                                Text(
                                    text = cell,
                                    fontSize = 12.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        is MarkdownBlock.HorizontalRule -> {
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 10.dp),
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

private fun sharePdf(context: Context, file: File) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Dokumen PDF"))
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal membagikan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

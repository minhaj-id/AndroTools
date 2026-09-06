package com.bimantara.feature.notes

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.ui.text.font.FontFamily
import com.bimantara.core.i18n.AppLanguage
import com.bimantara.core.i18n.AppLanguageManager
import com.bimantara.core.i18n.LocalAppStrings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimantara.data.model.NoteEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    modifier: Modifier = Modifier
) {
    val isEditing by viewModel.isEditing.collectAsState()

    if (isEditing) {
        NoteEditorView(viewModel = viewModel)
    } else {
        NotesListView(viewModel = viewModel)
    }
}

@Composable
fun NotesListView(viewModel: NotesViewModel) {
    val filteredResults by viewModel.filteredNotes.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val sizeFilter by viewModel.sizeFilter.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()

    val strings = LocalAppStrings.current
    val currentLanguage by AppLanguageManager.currentLanguage.collectAsState()
    val context = LocalContext.current

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAdvancedSearch by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.toolNotes,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${strings.tabNotesType}, ${strings.tabNotesStylus} & ${strings.actionVoiceInput}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // About button
                IconButton(
                    onClick = { showAboutDialog = true },
                    modifier = Modifier.size(36.dp).testTag("notes_about_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Tentang Aplikasi",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Language button
                IconButton(
                    onClick = { showLanguageDialog = true },
                    modifier = Modifier.size(36.dp).testTag("notes_lang_btn")
                ) {
                    Text(
                        text = currentLanguage.flagEmoji,
                        fontSize = 18.sp
                    )
                }
            }

            // Search Bar & Filter Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text(strings.notesSearchHint, fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f).height(48.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                val hasActiveFilters = dateFilter != NoteDateFilter.ANY || sizeFilter != NoteSizeFilter.ANY
                IconButton(
                    onClick = { showAdvancedSearch = !showAdvancedSearch },
                    modifier = Modifier
                        .size(44.dp)
                        .then(
                            if (hasActiveFilters || showAdvancedSearch)
                                Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            else Modifier
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Filter",
                        tint = if (hasActiveFilters || showAdvancedSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Advanced Search Expansion Box
            AnimatedVisibility(visible = showAdvancedSearch || dateFilter != NoteDateFilter.ANY || sizeFilter != NoteSizeFilter.ANY) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date filter row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.fmFilterDate}:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(68.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = dateFilter == NoteDateFilter.ANY,
                                    onClick = { viewModel.setDateFilter(NoteDateFilter.ANY) },
                                    label = { Text(strings.fmDateAny, fontSize = 10.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                                FilterChip(
                                    selected = dateFilter == NoteDateFilter.TODAY,
                                    onClick = { viewModel.setDateFilter(NoteDateFilter.TODAY) },
                                    label = { Text(strings.fmDateToday, fontSize = 10.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                                FilterChip(
                                    selected = dateFilter == NoteDateFilter.PAST_7_DAYS,
                                    onClick = { viewModel.setDateFilter(NoteDateFilter.PAST_7_DAYS) },
                                    label = { Text(strings.fmDate7Days, fontSize = 10.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                                FilterChip(
                                    selected = dateFilter == NoteDateFilter.PAST_30_DAYS,
                                    onClick = { viewModel.setDateFilter(NoteDateFilter.PAST_30_DAYS) },
                                    label = { Text(strings.fmDate30Days, fontSize = 10.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }

                        // Size filter row (Length of text content)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${strings.fmFilterSize}:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(68.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                FilterChip(
                                    selected = sizeFilter == NoteSizeFilter.ANY,
                                    onClick = { viewModel.setSizeFilter(NoteSizeFilter.ANY) },
                                    label = { Text(strings.fmSizeAny, fontSize = 10.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                                FilterChip(
                                    selected = sizeFilter == NoteSizeFilter.SHORT_UNDER_100,
                                    onClick = { viewModel.setSizeFilter(NoteSizeFilter.SHORT_UNDER_100) },
                                    label = { Text(strings.notesSizeShort, fontSize = 10.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                                FilterChip(
                                    selected = sizeFilter == NoteSizeFilter.MEDIUM_100_TO_500,
                                    onClick = { viewModel.setSizeFilter(NoteSizeFilter.MEDIUM_100_TO_500) },
                                    label = { Text(strings.notesSizeMedium, fontSize = 10.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                                FilterChip(
                                    selected = sizeFilter == NoteSizeFilter.LONG_OVER_500,
                                    onClick = { viewModel.setSizeFilter(NoteSizeFilter.LONG_OVER_500) },
                                    label = { Text(strings.notesSizeLong, fontSize = 10.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }

                        // Status & Reset row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${strings.notesFoundCount}: ${filteredResults.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = { viewModel.clearFilters() },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Text(strings.fmResetFilters, fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Quick Type Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = typeFilter == "ALL",
                        onClick = { viewModel.setTypeFilter("ALL") },
                        label = { Text("${strings.notesFilterAll} (${allNotes.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = typeFilter == "TYPE",
                        onClick = { viewModel.setTypeFilter("TYPE") },
                        label = { Text(strings.tabNotesType) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = typeFilter == "STYLUS",
                        onClick = { viewModel.setTypeFilter("STYLUS") },
                        label = { Text(strings.tabNotesStylus) },
                        leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = typeFilter == "PINNED",
                        onClick = { viewModel.setTypeFilter("PINNED") },
                        label = { Text(strings.notesPinned) },
                        leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (filteredResults.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EditNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = strings.notesEmpty,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = strings.notesEmptyDesc,
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
                    items(filteredResults, key = { it.note.id }) { result ->
                        NoteCardItem(
                            result = result,
                            onClick = { viewModel.openNote(result.note) },
                            onDelete = { viewModel.deleteNote(result.note) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Action FABs (Stylus Mode & Type Mode)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Stylus note FAB
            FloatingActionButton(
                onClick = { viewModel.startNewNote(NoteMode.STYLUS) },
                containerColor = Color(0xFFF59E0B),
                contentColor = Color.White,
                modifier = Modifier.testTag("notes_new_stylus_btn")
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Brush, contentDescription = "Stylus")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.tabNotesStylus, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Type note FAB
            FloatingActionButton(
                onClick = { viewModel.startNewNote(NoteMode.TYPE) },
                containerColor = Color(0xFF0284C7),
                contentColor = Color.White,
                modifier = Modifier.testTag("notes_new_type_btn")
            ) {
                Row(modifier = Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.actionAdd, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        com.bimantara.core.i18n.AppLanguageSelectionDialog(
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showAboutDialog) {
        com.bimantara.ui.about.AboutDialog(
            onDismiss = { showAboutDialog = false }
        )
    }
}

@Composable
fun NoteCardItem(
    result: NoteSearchResult,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val note = result.note
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (note.mode == "STYLUS") Color(0xFFF59E0B).copy(alpha = 0.15f)
                            else Color(0xFF0284C7).copy(alpha = 0.15f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (note.mode == "STYLUS") "✍️ ${strings.tabNotesStylus}" else "⌨️ ${strings.tabNotesType}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (note.mode == "STYLUS") Color(0xFFF59E0B) else Color(0xFF0284C7)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (note.isPinned) {
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                }
            }

            // Search Match Snippet or Normal Preview
            Spacer(modifier = Modifier.height(6.dp))

            if (result.matchedInContent && result.snippet != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0284C7).copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.notesMatchedInContent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = result.snippet,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = if (note.mode == "STYLUS") "[${strings.tabNotesStylus}]" else note.content.ifBlank { strings.notesEmptyDesc },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(note.updatedAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )

                Text(
                    text = "${note.content.length} ${strings.notesCharCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun NoteEditorView(viewModel: NotesViewModel) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val mode by viewModel.currentMode.collectAsState()
    val isPinned by viewModel.isPinned.collectAsState()

    var selectedColor by remember { mutableStateOf(Color(0xFF0F172A)) }
    var strokeWidth by remember { mutableFloatStateOf(6f) }
    var isEraser by remember { mutableStateOf(false) }

    var showVoiceInputDialog by remember { mutableStateOf(false) }

    // Android Speech Recognizer Launcher
    val voiceRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.appendVoiceInput(spokenText)
                Toast.makeText(context, "${strings.actionVoiceInput}: $spokenText", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Editor App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.closeEditor() }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }

            Text(
                text = if (mode == NoteMode.TYPE) strings.tabNotesType else strings.tabNotesStylus,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            // Voice input action button
            IconButton(
                onClick = {
                    try {
                        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                            putExtra(RecognizerIntent.EXTRA_PROMPT, strings.actionVoiceInput)
                        }
                        voiceRecognitionLauncher.launch(intent)
                    } catch (e: Exception) {
                        showVoiceInputDialog = true
                    }
                },
                modifier = Modifier.testTag("notes_voice_input_btn")
            ) {
                Icon(Icons.Default.Mic, contentDescription = strings.actionVoiceInput, tint = Color(0xFFDC2626))
            }

            // Toggle Pinned
            IconButton(onClick = { viewModel.togglePinned() }) {
                Icon(
                    if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                    contentDescription = "Pin",
                    tint = if (isPinned) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Save Note
            Button(
                onClick = { viewModel.saveNote() },
                modifier = Modifier.testTag("notes_save_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(strings.actionSave)
            }
        }

        // Mode Switcher (Ketik vs Stylus)
        TabRow(
            selectedTabIndex = if (mode == NoteMode.TYPE) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = mode == NoteMode.TYPE,
                onClick = { viewModel.setMode(NoteMode.TYPE) },
                text = { Text(strings.tabNotesType) },
                icon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = mode == NoteMode.STYLUS,
                onClick = { viewModel.setMode(NoteMode.STYLUS) },
                text = { Text(strings.tabNotesStylus) },
                icon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        // Title Bar
        OutlinedTextField(
            value = title,
            onValueChange = { viewModel.setTitle(it) },
            label = { Text(strings.notesTitleLabel) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(8.dp)
        )

        if (mode == NoteMode.TYPE) {
            // Type Mode Editor
            OutlinedTextField(
                value = content,
                onValueChange = { viewModel.setContent(it) },
                placeholder = { Text(strings.notesContentHint) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(8.dp)
            )
        } else {
            // Stylus Canvas Mode
            Column(modifier = Modifier.fillMaxSize()) {
                // Stylus Toolbar (Pena, Penghapus, Ukuran, Palet Warna, Undo, Hapus)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Tool toggle (Pen vs Eraser)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { isEraser = false },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isEraser) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        ) {
                            Icon(Icons.Default.Brush, contentDescription = "Pena", tint = if (!isEraser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = { isEraser = true },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isEraser) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                        ) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = "Penghapus", tint = if (isEraser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    // Stroke width selectors
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        listOf(3f to "Halus", 7f to "Sedang", 14f to "Tebal", 26f to "Stabilo").forEach { (widthVal, label) ->
                            val isSelected = strokeWidth == widthVal
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                                    .clickable { strokeWidth = widthVal }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 10.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    // Undo & Clear actions
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.undoStroke() }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.History, contentDescription = "Undo")
                        }
                        IconButton(onClick = { viewModel.clearCanvas() }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear Canvas")
                        }
                    }
                }

                // Color Palette Row
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val colors = listOf(
                        Color(0xFF0F172A), // Black Slate
                        Color(0xFF0284C7), // Blue
                        Color(0xFFDC2626), // Red
                        Color(0xFF10B981), // Emerald
                        Color(0xFF8B5CF6), // Purple
                        Color(0xFFF59E0B), // Amber
                        Color(0x99FACC15)  // Highlighter Yellow with alpha
                    )
                    items(colors) { c ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(
                                    width = if (selectedColor == c && !isEraser) 2.5.dp else 1.dp,
                                    color = if (selectedColor == c && !isEraser) MaterialTheme.colorScheme.primary else Color.Gray,
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColor = c
                                    isEraser = false
                                }
                        )
                    }
                }

                // Stylus Canvas Area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                ) {
                    StylusCanvas(
                        strokes = viewModel.currentStrokes,
                        selectedColor = selectedColor,
                        strokeWidth = strokeWidth,
                        isEraser = isEraser,
                        backgroundColor = Color.White
                    )
                }
            }
        }
    }

    // Fallback Voice Input dialog if system recognizer unavailable
    if (showVoiceInputDialog) {
        var voiceTypedText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showVoiceInputDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Input Dikte Suara")
                }
            },
            text = {
                Column {
                    Text("Layanan Pengenal Suara sistem tidak aktif di lingkungan simulator. Anda dapat mendiktekan teks secara langsung:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = voiceTypedText,
                        onValueChange = { voiceTypedText = it },
                        label = { Text("Teks Dikte Suara") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (voiceTypedText.isNotBlank()) {
                            viewModel.appendVoiceInput(voiceTypedText)
                        }
                        showVoiceInputDialog = false
                    }
                ) {
                    Text("Tambahkan ke Catatan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoiceInputDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

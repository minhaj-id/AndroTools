package com.bimantara.feature.filemanager

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Wifi
import com.bimantara.core.i18n.AppLanguage
import com.bimantara.core.i18n.AppLanguageManager
import com.bimantara.core.i18n.LocalAppStrings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val currentDrive by viewModel.currentDrive.collectAsState()
    val currentPath by viewModel.currentPath.collectAsState()
    val fileItems by viewModel.fileItems.collectAsState()
    val viewMode by viewModel.viewMode.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val sizeFilter by viewModel.sizeFilter.collectAsState()
    val searchInContent by viewModel.searchInContent.collectAsState()
    val isSearchingContent by viewModel.isSearchingContent.collectAsState()

    val strings = LocalAppStrings.current
    val currentLanguage by AppLanguageManager.currentLanguage.collectAsState()

    val networkInfo by viewModel.networkInfo.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()
    val isScanningNetwork by viewModel.isScanningNetwork.collectAsState()
    val pingResult by viewModel.pingResult.collectAsState()

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var selectedItemForAction by remember { mutableStateOf<FileItem?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDetailsDialog by remember { mutableStateOf(false) }
    var showTextPreviewDialog by remember { mutableStateOf<FileItem?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAdvancedSearch by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Windows Explorer Title Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0284C7).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "Explorer",
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "File Manager Windows",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (currentTab == ExplorerTab.FILES) "Drive $currentDrive Explorer" else "Network & Lan Explorer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Quick drive badges & Language button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("C:", "D:").forEach { drive ->
                    val isSelected = currentDrive == drive && currentTab == ExplorerTab.FILES
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable {
                                viewModel.setTab(ExplorerTab.FILES)
                                viewModel.setDrive(drive)
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = drive,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(
                    onClick = { showLanguageDialog = true },
                    modifier = Modifier.size(32.dp).testTag("fm_lang_btn")
                ) {
                    Text(
                        text = currentLanguage.flagEmoji,
                        fontSize = 16.sp
                    )
                }
            }
        }

        // Navigation Tabs (Files vs Network Connection)
        PrimaryTabRow(
            selectedTabIndex = if (currentTab == ExplorerTab.FILES) 0 else 1,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = currentTab == ExplorerTab.FILES,
                onClick = { viewModel.setTab(ExplorerTab.FILES) },
                text = { Text("${strings.tabExplorer} (${fileItems.size})") },
                icon = { Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = currentTab == ExplorerTab.NETWORK,
                onClick = { viewModel.setTab(ExplorerTab.NETWORK) },
                text = { Text(strings.tabNetwork) },
                icon = { Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        if (currentTab == ExplorerTab.FILES) {
            // Windows Explorer Path / Breadcrumb & Ribbon Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                // Address Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.navigateUp() },
                        modifier = Modifier.size(32.dp).testTag("explorer_navigate_up")
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Up", modifier = Modifier.size(18.dp))
                    }

                    Text(
                        text = "$currentDrive ${currentPath.replace("/storage/emulated/0", "C:").replace("/", " > ")}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 6.dp)
                    )

                    IconButton(
                        onClick = { viewModel.refreshCurrentDirectory() },
                        modifier = Modifier.size(32.dp).testTag("explorer_refresh")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Toolbar Actions: New Folder, View Mode, Sort, Search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showNewFolderDialog = true },
                            modifier = Modifier.height(34.dp).testTag("explorer_new_folder_btn"),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(strings.fmNewFolder, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // View mode toggle
                        IconButton(
                            onClick = {
                                viewModel.setViewMode(
                                    if (viewMode == FileViewMode.DETAILS_LIST) FileViewMode.GRID else FileViewMode.DETAILS_LIST
                                )
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                if (viewMode == FileViewMode.DETAILS_LIST) Icons.Default.GridView else Icons.Default.ViewList,
                                contentDescription = "Toggle View",
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Sort menu
                        Box {
                            IconButton(
                                onClick = { showSortMenu = true },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(Icons.Default.Sort, contentDescription = "Sort", modifier = Modifier.size(20.dp))
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(strings.fmSortName) },
                                    onClick = { viewModel.setSortMode(FileSortMode.NAME); showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.fmSortDate) },
                                    onClick = { viewModel.setSortMode(FileSortMode.DATE); showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.fmSortSize) },
                                    onClick = { viewModel.setSortMode(FileSortMode.SIZE); showSortMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(strings.fmSortType) },
                                    onClick = { viewModel.setSortMode(FileSortMode.TYPE); showSortMenu = false }
                                )
                            }
                        }

                        // Advanced Filter Toggle Button
                        val hasActiveFilters = dateFilter != DateFilterOption.ANY || sizeFilter != SizeFilterOption.ANY || searchInContent
                        IconButton(
                            onClick = { showAdvancedSearch = !showAdvancedSearch },
                            modifier = Modifier
                                .size(34.dp)
                                .then(
                                    if (hasActiveFilters || showAdvancedSearch)
                                        Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Filter",
                                tint = if (hasActiveFilters || showAdvancedSearch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text(strings.searchPlaceholder, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .width(155.dp)
                            .height(44.dp)
                    )
                }

                // Advanced Search & Filter Expansion Box
                AnimatedVisibility(visible = showAdvancedSearch || dateFilter != DateFilterOption.ANY || sizeFilter != SizeFilterOption.ANY || searchInContent) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Date modified row
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
                                        selected = dateFilter == DateFilterOption.ANY,
                                        onClick = { viewModel.setDateFilter(DateFilterOption.ANY) },
                                        label = { Text(strings.fmDateAny, fontSize = 10.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                    FilterChip(
                                        selected = dateFilter == DateFilterOption.TODAY,
                                        onClick = { viewModel.setDateFilter(DateFilterOption.TODAY) },
                                        label = { Text(strings.fmDateToday, fontSize = 10.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                    FilterChip(
                                        selected = dateFilter == DateFilterOption.PAST_7_DAYS,
                                        onClick = { viewModel.setDateFilter(DateFilterOption.PAST_7_DAYS) },
                                        label = { Text(strings.fmDate7Days, fontSize = 10.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                    FilterChip(
                                        selected = dateFilter == DateFilterOption.PAST_30_DAYS,
                                        onClick = { viewModel.setDateFilter(DateFilterOption.PAST_30_DAYS) },
                                        label = { Text(strings.fmDate30Days, fontSize = 10.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }

                            // Size filter row
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
                                        selected = sizeFilter == SizeFilterOption.ANY,
                                        onClick = { viewModel.setSizeFilter(SizeFilterOption.ANY) },
                                        label = { Text(strings.fmSizeAny, fontSize = 10.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                    FilterChip(
                                        selected = sizeFilter == SizeFilterOption.SMALL_UNDER_1MB,
                                        onClick = { viewModel.setSizeFilter(SizeFilterOption.SMALL_UNDER_1MB) },
                                        label = { Text(strings.fmSizeSmall, fontSize = 10.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                    FilterChip(
                                        selected = sizeFilter == SizeFilterOption.MEDIUM_1_TO_10MB,
                                        onClick = { viewModel.setSizeFilter(SizeFilterOption.MEDIUM_1_TO_10MB) },
                                        label = { Text(strings.fmSizeMedium, fontSize = 10.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                    FilterChip(
                                        selected = sizeFilter == SizeFilterOption.LARGE_OVER_10MB,
                                        onClick = { viewModel.setSizeFilter(SizeFilterOption.LARGE_OVER_10MB) },
                                        label = { Text(strings.fmSizeLarge, fontSize = 10.sp) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }

                            // Content Search Switch and Results count
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                FilterChip(
                                    selected = searchInContent,
                                    onClick = { viewModel.toggleSearchInContent(!searchInContent) },
                                    label = { Text(strings.fmFilterContent, fontSize = 11.sp) },
                                    leadingIcon = {
                                        Icon(Icons.Default.FindInPage, contentDescription = null, modifier = Modifier.size(14.dp))
                                    },
                                    modifier = Modifier.height(30.dp)
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isSearchingContent) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Scanning...", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = "${strings.fmFoundFiles}: ${fileItems.size}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
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
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            // File items content
            Box(modifier = Modifier.fillMaxSize()) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                    }
                } else if (fileItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Folder kosong",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Gunakan tombol + untuk membuat folder baru",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    if (viewMode == FileViewMode.DETAILS_LIST) {
                        // Windows Explorer Detailed List
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(fileItems, key = { it.path }) { item ->
                                FileListItemRow(
                                    item = item,
                                    onClick = {
                                        if (item.isDirectory) {
                                            viewModel.navigateTo(item.file)
                                        } else {
                                            handleOpenFile(context, item) { showTextPreviewDialog = it }
                                        }
                                    },
                                    onMoreClick = { selectedItemForAction = item }
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 56.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                    } else {
                        // Grid View
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 100.dp),
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(fileItems, key = { it.path }) { item ->
                                FileGridItem(
                                    item = item,
                                    onClick = {
                                        if (item.isDirectory) {
                                            viewModel.navigateTo(item.file)
                                        } else {
                                            handleOpenFile(context, item) { showTextPreviewDialog = it }
                                        }
                                    },
                                    onMoreClick = { selectedItemForAction = item }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Network Connection Explorer Screen
            NetworkConnectionView(
                networkInfo = networkInfo,
                discoveredDevices = discoveredDevices,
                isScanning = isScanningNetwork,
                pingResult = pingResult,
                onRefresh = { viewModel.refreshNetworkInfo() },
                onScan = { viewModel.scanLocalSubnet() },
                onPing = { host -> viewModel.runPingTest(host) }
            )
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        var newFolderName by remember { mutableStateOf("New Folder") }
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("Buat Folder Baru") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text("Nama Folder") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                            showNewFolderDialog = false
                        }
                    }
                ) {
                    Text("Buat")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Rename Dialog
    if (showRenameDialog && selectedItemForAction != null) {
        val target = selectedItemForAction!!
        var renameText by remember { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Ganti Nama") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Nama Baru") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameText.isNotBlank()) {
                            viewModel.renameItem(target, renameText) { success ->
                                Toast.makeText(
                                    context,
                                    if (success) "Nama berhasil diubah" else "Gagal mengubah nama",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            showRenameDialog = false
                            selectedItemForAction = null
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false; selectedItemForAction = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Properties / Details Dialog
    if (showDetailsDialog && selectedItemForAction != null) {
        val target = selectedItemForAction!!
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false; selectedItemForAction = null },
            title = { Text("Properti File") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nama: ${target.name}", fontWeight = FontWeight.SemiBold)
                    Text("Tipe: ${if (target.isDirectory) "Folder Berkas" else target.extension}")
                    Text("Ukuran: ${target.formattedSize}")
                    Text("Lokasi: ${target.path}", style = MaterialTheme.typography.bodySmall)
                    Text("Dimodifikasi: ${target.formattedDate}")
                }
            },
            confirmButton = {
                Button(onClick = { showDetailsDialog = false; selectedItemForAction = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Context Action Sheet / Dialog for Selected File
    if (selectedItemForAction != null && !showRenameDialog && !showDetailsDialog) {
        val item = selectedItemForAction!!
        AlertDialog(
            onDismissRequest = { selectedItemForAction = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                        contentDescription = null,
                        tint = if (item.isDirectory) Color(0xFFF59E0B) else Color(0xFF0284C7)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = item.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDetailsDialog = true
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Properti / Detail")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showRenameDialog = true
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Ganti Nama (Rename)")
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.deleteItem(item) { success ->
                                    Toast.makeText(
                                        context,
                                        if (success) "Berhasil dihapus" else "Gagal menghapus",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                selectedItemForAction = null
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Hapus Berkas", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedItemForAction = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Text file preview dialog
    if (showTextPreviewDialog != null) {
        val target = showTextPreviewDialog!!
        val content = remember(target) {
            try {
                if (target.file.length() < 100000) target.file.readText() else "Berkas terlalu besar untuk ditampilkan."
            } catch (e: Exception) {
                "Tidak dapat membaca berkas: ${e.message}"
            }
        }
        AlertDialog(
            onDismissRequest = { showTextPreviewDialog = null },
            title = { Text(target.name) },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    item {
                        Text(
                            text = content,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showTextPreviewDialog = null }) {
                    Text("Selesai")
                }
            }
        )
    }

    // Language Selection Dialog
    if (showLanguageDialog) {
        com.bimantara.core.i18n.AppLanguageSelectionDialog(
            onDismiss = { showLanguageDialog = false }
        )
    }
}

private fun handleOpenFile(context: android.content.Context, item: FileItem, onShowTextPreview: (FileItem) -> Unit) {
    val ext = item.file.extension.lowercase()
    if (ext in listOf("txt", "log", "json", "xml", "kt", "dart", "md", "csv")) {
        onShowTextPreview(item)
    } else {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                item.file
            )
            val mime = when (ext) {
                "pdf" -> "application/pdf"
                "png", "jpg", "jpeg", "webp" -> "image/*"
                "apk" -> "application/vnd.android.package-archive"
                else -> "*/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Buka Berkas"))
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak dapat membuka berkas: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun FileListItemRow(
    item: FileItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, tint) = getFileIconAndColor(item)
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (item.matchedInContent) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = strings.fmMatchedInContent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }
            if (item.matchedInContent && item.contentSnippet != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.contentSnippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
                Text(
                    text = item.formattedSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }

        IconButton(onClick = onMoreClick, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun FileGridItem(
    item: FileItem,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                val (icon, tint) = getFileIconAndColor(item)
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier
                        .size(40.dp)
                        .align(Alignment.Center)
                )
                IconButton(
                    onClick = onMoreClick,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options", modifier = Modifier.size(14.dp))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (item.matchedInContent) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = strings.fmMatchedInContent,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.formattedSize,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontSize = 10.sp
            )
        }
    }
}

private fun getFileIconAndColor(item: FileItem): Pair<androidx.compose.ui.graphics.vector.ImageVector, Color> {
    if (item.isDirectory) {
        return Pair(Icons.Default.Folder, Color(0xFFF59E0B))
    }
    val ext = item.file.extension.lowercase()
    return when (ext) {
        "pdf" -> Pair(Icons.Default.Description, Color(0xFFEF4444))
        "png", "jpg", "jpeg", "webp", "gif" -> Pair(Icons.Default.Image, Color(0xFF8B5CF6))
        "apk" -> Pair(Icons.Default.PlayArrow, Color(0xFF10B981))
        "txt", "md", "doc", "docx" -> Pair(Icons.Default.Description, Color(0xFF0284C7))
        else -> Pair(Icons.Default.Description, Color(0xFF64748B))
    }
}

@Composable
fun NetworkConnectionView(
    networkInfo: NetworkInfoState,
    discoveredDevices: List<DiscoveredDevice>,
    isScanning: Boolean,
    pingResult: String?,
    onRefresh: () -> Unit,
    onScan: () -> Unit,
    onPing: (String) -> Unit
) {
    var customHost by remember { mutableStateOf("8.8.8.8") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Network Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (networkInfo.isConnected) Color(0xFF10B981).copy(alpha = 0.2f)
                                        else Color(0xFFEF4444).copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = if (networkInfo.isConnected) Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (networkInfo.isConnected) "Jaringan Terhubung" else "Jaringan Terputus",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = networkInfo.connectionType,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        NetworkStatItem("Local IP", networkInfo.localIp)
                        NetworkStatItem("Gateway", networkInfo.gateway)
                        NetworkStatItem("Subnet", networkInfo.subnet)
                    }
                }
            }
        }

        // Ping Diagnostics Tool
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Tes Koneksi / Ping",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customHost,
                            onValueChange = { customHost = it },
                            label = { Text("IP / Hostname") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { onPing(customHost) },
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Ping")
                        }
                    }

                    if (pingResult != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = pingResult,
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // LAN Network Device Discovery
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Perangkat di Jaringan Lokal (LAN)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${discoveredDevices.size} perangkat ditemukan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onScan,
                    enabled = !isScanning,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Memindai...")
                    } else {
                        Icon(Icons.Default.Lan, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Scan LAN")
                    }
                }
            }
        }

        items(discoveredDevices) { device ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Router,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.hostname,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = device.ip,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${device.pingMs} ms",
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetworkStatItem(label: String, value: String) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

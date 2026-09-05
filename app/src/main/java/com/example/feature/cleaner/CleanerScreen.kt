package com.bimantara.feature.cleaner

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import java.io.File
import com.bimantara.core.i18n.AppLanguage
import com.bimantara.core.i18n.AppLanguageManager
import com.bimantara.core.i18n.AppLanguageSelectionDialog
import com.bimantara.core.i18n.LocalAppStrings

@Composable
fun CleanerScreen(
    viewModel: CleanerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsState()
    val currentLanguage by AppLanguageManager.currentLanguage.collectAsState()
    val strings = LocalAppStrings.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    // Junk states
    val junkCategories by viewModel.junkCategories.collectAsState()
    val isScanningJunk by viewModel.isScanningJunk.collectAsState()
    val isCleaningJunk by viewModel.isCleaningJunk.collectAsState()
    val lastCleanedAmount by viewModel.lastCleanedAmount.collectAsState()

    // Memory states
    val memoryStats by viewModel.memoryStats.collectAsState()
    val isOptimizingMemory by viewModel.isOptimizingMemory.collectAsState()
    val freedRamMessage by viewModel.freedRamMessage.collectAsState()

    // APK Backup states
    val installedApps by viewModel.installedApps.collectAsState()
    val isLoadingApps by viewModel.isLoadingApps.collectAsState()
    val filterOnlyUserApps by viewModel.filterOnlyUserApps.collectAsState()
    val backupProgress by viewModel.backupProgress.collectAsState()

    var showApkSuccessDialog by remember { mutableStateOf<Pair<String, File>?>(null) }

    Column(
        modifier = modifier
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
                    .background(Color(0xFFEF4444).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CleaningServices,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.cleanerTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = strings.cleanerSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Language Switcher Button
            IconButton(
                onClick = { showLanguageDialog = true },
                modifier = Modifier.testTag("cleaner_language_btn")
            ) {
                Text(
                    text = currentLanguage.flagEmoji,
                    fontSize = 20.sp
                )
            }
        }

        // Sub Tabs
        TabRow(
            selectedTabIndex = when (currentTab) {
                CleanerTab.JUNK_CLEANER -> 0
                CleanerTab.MEMORY_BOOSTER -> 1
                CleanerTab.APK_BACKUP -> 2
                CleanerTab.DATA_BACKUP -> 3
            },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Tab(
                selected = currentTab == CleanerTab.JUNK_CLEANER,
                onClick = { viewModel.setTab(CleanerTab.JUNK_CLEANER) },
                text = { Text(strings.cleanerDeepClean) },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = currentTab == CleanerTab.MEMORY_BOOSTER,
                onClick = { viewModel.setTab(CleanerTab.MEMORY_BOOSTER) },
                text = { Text(strings.cleanerRamBooster) },
                icon = { Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = currentTab == CleanerTab.APK_BACKUP,
                onClick = { viewModel.setTab(CleanerTab.APK_BACKUP) },
                text = { Text(strings.cleanerApkBackup) },
                icon = { Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
            Tab(
                selected = currentTab == CleanerTab.DATA_BACKUP,
                onClick = { viewModel.setTab(CleanerTab.DATA_BACKUP) },
                text = { Text(strings.cleanerDataBackup) },
                icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) }
            )
        }

        when (currentTab) {
            CleanerTab.JUNK_CLEANER -> {
                DeepCleanerTabContent(
                    junkCategories = junkCategories,
                    isScanning = isScanningJunk,
                    isCleaning = isCleaningJunk,
                    lastCleanedAmount = lastCleanedAmount,
                    formatSize = { viewModel.formatFileSize(it) },
                    onScan = { viewModel.scanJunkFiles() },
                    onClean = {
                        viewModel.cleanJunkNow { freed ->
                            Toast.makeText(context, "Sistem bersih! Berhasil membebaskan $freed sampah.", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
            CleanerTab.MEMORY_BOOSTER -> {
                MemoryBoosterTabContent(
                    memoryStats = memoryStats,
                    isOptimizing = isOptimizingMemory,
                    freedMessage = freedRamMessage,
                    formatSize = { viewModel.formatFileSize(it) },
                    onRefresh = { viewModel.updateMemoryStats() },
                    onBoost = {
                        viewModel.boostMemoryNow { msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
            CleanerTab.APK_BACKUP -> {
                val filteredApps = remember(installedApps, filterOnlyUserApps) {
                    if (filterOnlyUserApps) installedApps.filter { !it.isSystemApp } else installedApps
                }
                ApkBackupTabContent(
                    apps = filteredApps,
                    isLoading = isLoadingApps,
                    filterOnlyUser = filterOnlyUserApps,
                    backupProgress = backupProgress,
                    onToggleUserOnly = { viewModel.toggleFilterUserApps(it) },
                    onRefresh = { viewModel.loadInstalledApps() },
                    onBackup = { appItem ->
                        viewModel.backupAppToApk(appItem) { success, file ->
                            if (success && file != null) {
                                showApkSuccessDialog = Pair(appItem.appName, file)
                            } else {
                                Toast.makeText(context, "Gagal mengekstrak APK ${appItem.appName}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            CleanerTab.DATA_BACKUP -> {
                DataBackupTabContent(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    // Success Dialog after APK Backup
    if (showApkSuccessDialog != null) {
        val (appName, apkFile) = showApkSuccessDialog!!
        AlertDialog(
            onDismissRequest = { showApkSuccessDialog = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Android, contentDescription = null, tint = Color(0xFF10B981))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup APK Berhasil!")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Aplikasi: $appName", fontWeight = FontWeight.SemiBold)
                    Text("Berkas: ${apkFile.name}")
                    Text("Ukuran: ${viewModel.formatFileSize(apkFile.length())}")
                    Text("Lokasi: ${apkFile.absolutePath}", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.shareApk(context, apkFile)
                        showApkSuccessDialog = null
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bagikan APK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showApkSuccessDialog = null }) {
                    Text(strings.actionClose)
                }
            }
        )
    }

    if (showLanguageDialog) {
        AppLanguageSelectionDialog(
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
fun DeepCleanerTabContent(
    junkCategories: List<JunkCategory>,
    isScanning: Boolean,
    isCleaning: Boolean,
    lastCleanedAmount: String?,
    formatSize: (Long) -> String,
    onScan: () -> Unit,
    onClean: () -> Unit
) {
    val totalJunkBytes = junkCategories.sumOf { it.sizeBytes }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Scan Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (totalJunkBytes > 0) Color(0xFFEF4444).copy(alpha = 0.12f)
                    else Color(0xFF10B981).copy(alpha = 0.12f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(
                                if (totalJunkBytes > 0) Color(0xFFEF4444).copy(alpha = 0.2f)
                                else Color(0xFF10B981).copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (totalJunkBytes > 0) Icons.Default.Delete else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (totalJunkBytes > 0) Color(0xFFEF4444) else Color(0xFF10B981),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (totalJunkBytes > 0) formatSize(totalJunkBytes) else "0 B",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (totalJunkBytes > 0) Color(0xFFEF4444) else Color(0xFF10B981)
                    )

                    Text(
                        text = if (totalJunkBytes > 0) "File Sampah Ditemukan" else "Perangkat Bersih & Optimal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    if (lastCleanedAmount != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Pembersihan Terakhir: $lastCleanedAmount dibebaskan",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF10B981)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onScan,
                            enabled = !isScanning && !isCleaning
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Memindai...")
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pindai Ulang")
                            }
                        }

                        Button(
                            onClick = onClean,
                            enabled = totalJunkBytes > 0 && !isCleaning,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                            modifier = Modifier.testTag("cleaner_deep_clean_btn")
                        ) {
                            if (isCleaning) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Membersihkan...")
                            } else {
                                Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Deep Clean Sekarang")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Rincian Berkas Sampah",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(junkCategories, key = { it.id }) { cat ->
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0284C7).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = cat.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(text = "${cat.description} • ${cat.fileCount} berkas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = formatSize(cat.sizeBytes),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = if (cat.sizeBytes > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MemoryBoosterTabContent(
    memoryStats: MemoryStats,
    isOptimizing: Boolean,
    freedMessage: String?,
    formatSize: (Long) -> String,
    onRefresh: () -> Unit,
    onBoost: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Gauge / RAM meter card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(
                                if (memoryStats.usedPercent > 80) Color(0xFFEF4444).copy(alpha = 0.15f)
                                else Color(0xFF0284C7).copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${memoryStats.usedPercent}%",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (memoryStats.usedPercent > 80) Color(0xFFEF4444) else Color(0xFF0284C7)
                            )
                            Text(text = "RAM Terpakai", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { memoryStats.usedPercent / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (memoryStats.usedPercent > 80) Color(0xFFEF4444) else Color(0xFF0284C7),
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("RAM Tersedia", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatSize(memoryStats.availRamBytes), fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("RAM Terpakai", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatSize(memoryStats.usedRamBytes), fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total RAM", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatSize(memoryStats.totalRamBytes), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (freedMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = freedMessage,
                                color = Color(0xFF10B981),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = onBoost,
                        enabled = !isOptimizing,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("cleaner_boost_memory_btn")
                    ) {
                        if (isOptimizing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mengoptimalkan Memori...")
                        } else {
                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kosongkan Memori (Boost RAM)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApkBackupTabContent(
    apps: List<InstalledAppItem>,
    isLoading: Boolean,
    filterOnlyUser: Boolean,
    backupProgress: String?,
    onToggleUserOnly: (Boolean) -> Unit,
    onRefresh: () -> Unit,
    onBackup: (InstalledAppItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Filter toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = filterOnlyUser,
                    onClick = { onToggleUserOnly(true) },
                    label = { Text("Aplikasi Pengguna") }
                )
                FilterChip(
                    selected = !filterOnlyUser,
                    onClick = { onToggleUserOnly(false) },
                    label = { Text("Semua Aplikasi") }
                )
            }

            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
            }
        }

        if (backupProgress != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = backupProgress, fontSize = 12.sp, color = Color(0xFF0284C7))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (apps.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tidak ada aplikasi yang ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apps, key = { it.packageName }) { app ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // App icon
                            if (app.icon != null) {
                                Image(
                                    bitmap = app.icon.toBitmap(96, 96).asImageBitmap(),
                                    contentDescription = app.appName,
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Android, contentDescription = null, tint = Color(0xFF10B981))
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = app.appName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "v${app.versionName} • ${app.formattedSize}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = app.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp
                                )
                            }

                            Button(
                                onClick = { onBackup(app) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Backup APK", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.bimantara.feature.cleaner

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bimantara.core.i18n.LocalAppStrings
import com.bimantara.data.backup.BackupFileInfo
import com.bimantara.data.backup.RestoreResult
import java.io.File

@Composable
fun DataBackupTabContent(
    viewModel: CleanerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current

    val encryptedBackups by viewModel.encryptedBackups.collectAsState()
    val lastBackupInfo by viewModel.lastBackupInfo.collectAsState()
    val isBackingUp by viewModel.isBackingUpData.collectAsState()
    val isRestoring by viewModel.isRestoringData.collectAsState()
    val isWorkManagerRunning by viewModel.isWorkManagerRunning.collectAsState()

    var fileToRestore by remember { mutableStateOf<File?>(null) }
    var restoreResultMessage by remember { mutableStateOf<RestoreResult?>(null) }
    var fileToDelete by remember { mutableStateOf<File?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. WorkManager Status & Encryption Hero Banner
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.backupWorkerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "WorkManager Background Service",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = strings.backupWorkerDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("AES-256-GCM 🔒", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Setiap 24 Jam ⏱️", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Pembersihan Cache 🧹", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.triggerWorkManagerMaintenance {
                                    Toast.makeText(context, "WorkManager maintenance selesai!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isWorkManagerRunning && !isBackingUp,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_trigger_workmanager")
                        ) {
                            if (isWorkManagerRunning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Menjalankan...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.backupRunWorkerNow, fontSize = 12.sp)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.createEncryptedBackupNow { result ->
                                    if (result.isSuccess) {
                                        Toast.makeText(
                                            context,
                                            "Cadangan terenkripsi berhasil dibuat (${result.notesCount} catatan, ${result.plannerCount} rencana)",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(context, "Gagal mencadangkan: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            enabled = !isBackingUp && !isWorkManagerRunning,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_create_manual_backup")
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mencadangkan...", fontSize = 12.sp)
                            } else {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(strings.backupCreateNow, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // 2. Last Backup Card
        if (lastBackupInfo != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cadangan Terakhir Tersimpan",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = lastBackupInfo!!.formattedDate,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${lastBackupInfo!!.notesCount} Catatan • ${lastBackupInfo!!.plannerCount} Rencana • ${viewModel.formatFileSize(lastBackupInfo!!.fileSizeBytes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Encrypted Backups List Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${strings.backupEncryptedListTitle} (${encryptedBackups.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = { viewModel.loadEncryptedBackups() },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(18.dp))
                }
            }
        }

        // 4. Encrypted Backup Items
        if (encryptedBackups.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum ada berkas cadangan terenkripsi",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "WorkManager akan otomatis membuat cadangan harian, atau klik 'Buat Cadangan Terenkripsi' di atas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        } else {
            items(encryptedBackups, key = { it.file.absolutePath }) { backup ->
                EncryptedBackupItemCard(
                    backup = backup,
                    formattedSize = viewModel.formatFileSize(backup.sizeBytes),
                    onRestore = { fileToRestore = backup.file },
                    onShare = { viewModel.shareEncryptedBackup(context, backup.file) },
                    onDelete = { fileToDelete = backup.file }
                )
            }
        }
    }

    // Restore Confirmation Dialog
    if (fileToRestore != null) {
        AlertDialog(
            onDismissRequest = { fileToRestore = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Restore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(strings.backupRestoreAction)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(strings.backupRestoreConfirm)
                    Text(
                        text = "Berkas: ${fileToRestore!!.name}",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val file = fileToRestore!!
                        fileToRestore = null
                        viewModel.restoreBackup(file) { result ->
                            restoreResultMessage = result
                        }
                    },
                    modifier = Modifier.testTag("btn_confirm_restore")
                ) {
                    Text(strings.backupRestoreAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRestore = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    // Restore Result Dialog
    if (restoreResultMessage != null) {
        val res = restoreResultMessage!!
        AlertDialog(
            onDismissRequest = { restoreResultMessage = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (res.isSuccess) Icons.Default.CheckCircle else Icons.Default.Delete,
                        contentDescription = null,
                        tint = if (res.isSuccess) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (res.isSuccess) strings.backupRestoredSuccess else strings.error)
                }
            },
            text = {
                if (res.isSuccess) {
                    Text("Berhasil memulihkan ${res.restoredNotes} catatan dan ${res.restoredPlannerItems} item perencanaan dari cadangan terenkripsi.")
                } else {
                    Text("Gagal memulihkan cadangan: ${res.errorMessage}")
                }
            },
            confirmButton = {
                Button(onClick = { restoreResultMessage = null }) {
                    Text(strings.ok)
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text(strings.delete) },
            text = { Text("Hapus berkas cadangan ${fileToDelete!!.name} secara permanen?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEncryptedBackup(fileToDelete!!)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(strings.delete)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
fun EncryptedBackupItemCard(
    backup: BackupFileInfo,
    formattedSize: String,
    onRestore: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF6366F1).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = backup.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${backup.formattedDate} • $formattedSize",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (backup.isEncrypted) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "AES-256",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onRestore,
                    modifier = Modifier.testTag("btn_restore_${backup.name}")
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pulihkan", fontSize = 12.sp)
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Bagikan", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

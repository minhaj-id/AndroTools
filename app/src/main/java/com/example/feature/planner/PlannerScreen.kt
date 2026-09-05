package com.example.feature.planner

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlannerItemEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.AppLanguageManager
import com.example.core.i18n.AppLanguageSelectionDialog
import com.example.core.i18n.LocalAppStrings

@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items by viewModel.allItems.collectAsState()
    val filterTab by viewModel.filterTab.collectAsState()
    val currentLanguage by AppLanguageManager.currentLanguage.collectAsState()
    val strings = LocalAppStrings.current
    var showLanguageDialog by remember { mutableStateOf(false) }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<PlannerItemEntity?>(null) }

    val filteredItems = remember(items, filterTab) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val endOfToday = calendar.timeInMillis

        when (filterTab) {
            "TODAY" -> items.filter { !it.isCompleted && it.dueDateMillis <= endOfToday }
            "UPCOMING" -> items.filter { !it.isCompleted && it.dueDateMillis > endOfToday }
            "COMPLETED" -> items.filter { it.isCompleted }
            else -> items
        }
    }

    val totalCount = items.size
    val completedCount = items.count { it.isCompleted }
    val pendingCount = totalCount - completedCount

    Box(
        modifier = modifier
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
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF8B5CF6),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.plannerTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = strings.plannerSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Language Switcher Button
                IconButton(
                    onClick = { showLanguageDialog = true },
                    modifier = Modifier.testTag("planner_language_btn")
                ) {
                    Text(
                        text = currentLanguage.flagEmoji,
                        fontSize = 20.sp
                    )
                }
            }

            // Stat Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PlannerStatCard(
                    title = strings.plannerAll,
                    count = totalCount.toString(),
                    color = Color(0xFF0284C7),
                    modifier = Modifier.weight(1f)
                )
                PlannerStatCard(
                    title = strings.plannerUpcoming,
                    count = pendingCount.toString(),
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
                PlannerStatCard(
                    title = strings.plannerCompleted,
                    count = completedCount.toString(),
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )
            }

            // Filter Tabs
            TabRow(
                selectedTabIndex = when (filterTab) {
                    "TODAY" -> 1
                    "UPCOMING" -> 2
                    "COMPLETED" -> 3
                    else -> 0
                },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = filterTab == "ALL",
                    onClick = { viewModel.setFilterTab("ALL") },
                    text = { Text("${strings.plannerAll} ($totalCount)") }
                )
                Tab(
                    selected = filterTab == "TODAY",
                    onClick = { viewModel.setFilterTab("TODAY") },
                    text = { Text(strings.plannerToday) }
                )
                Tab(
                    selected = filterTab == "UPCOMING",
                    onClick = { viewModel.setFilterTab("UPCOMING") },
                    text = { Text(strings.plannerUpcoming) }
                )
                Tab(
                    selected = filterTab == "COMPLETED",
                    onClick = { viewModel.setFilterTab("COMPLETED") },
                    text = { Text("${strings.plannerCompleted} ($completedCount)") }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            if (filteredItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak Ada Rencana di Kategori Ini",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Gunakan tombol + untuk membuat rencana baru dengan alarm",
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
                    items(filteredItems, key = { it.id }) { item ->
                        PlannerItemCard(
                            item = item,
                            onToggleComplete = { viewModel.toggleCompleted(item) },
                            onEdit = {
                                itemToEdit = item
                                showAddEditDialog = true
                            },
                            onDelete = { viewModel.deleteItem(item) },
                            onTestNotification = { viewModel.testNotificationNow(item) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }

        // Add Plan FAB
        FloatingActionButton(
            onClick = {
                itemToEdit = null
                showAddEditDialog = true
            },
            containerColor = Color(0xFF8B5CF6),
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("planner_add_plan_btn")
        ) {
            Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Add, contentDescription = "Tambah")
                Spacer(modifier = Modifier.width(6.dp))
                Text("Rencana Baru", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }

    if (showAddEditDialog) {
        PlannerAddEditDialog(
            item = itemToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { id, title, desc, due, priority, cat, alarm ->
                viewModel.saveItem(id, title, desc, due, priority, cat, alarm)
                showAddEditDialog = false
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
fun PlannerStatCard(title: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = count, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
fun PlannerItemCard(
    item: PlannerItemEntity,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTestNotification: () -> Unit
) {
    val priorityColor = when (item.priority) {
        "HIGH" -> Color(0xFFEF4444)
        "MEDIUM" -> Color(0xFFF59E0B)
        else -> Color(0xFF0284C7)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    modifier = Modifier.size(32.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                    )
                    if (item.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(priorityColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.priority,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (item.alarmEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = if (item.alarmEnabled) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(item.dueDateMillis)),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Test notification button
                    OutlinedButton(
                        onClick = onTestNotification,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Tes Alarm", fontSize = 10.sp)
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun PlannerAddEditDialog(
    item: PlannerItemEntity?,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, Long, String, String, Boolean) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(item?.title ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var priority by remember { mutableStateOf(item?.priority ?: "MEDIUM") }
    var category by remember { mutableStateOf(item?.category ?: "WORK") }
    var alarmEnabled by remember { mutableStateOf(item?.alarmEnabled ?: true) }
    var dueDateMillis by remember { mutableLongStateOf(item?.dueDateMillis ?: (System.currentTimeMillis() + 3600000L)) }

    // Voice recognition launcher for dictating task
    val voiceRecognitionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!spoken.isNullOrBlank()) {
                if (title.isBlank()) {
                    title = spoken
                } else {
                    description = if (description.isBlank()) spoken else "$description $spoken"
                }
                Toast.makeText(context, "Dikte suara berhasil ditambahkan", Toast.LENGTH_SHORT).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (item == null) "Tambah Rencana Baru" else "Edit Rencana")
                IconButton(
                    onClick = {
                        try {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Diktekan rencana kegiatan Anda...")
                            }
                            voiceRecognitionLauncher.launch(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Dikte suara tidak didukung di perangkat ini", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("planner_voice_dictate_btn")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Dikte Suara", tint = Color(0xFFDC2626))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Judul Rencana / Tugas") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Deskripsi Rencana") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick time offset selector
                Text("Waktu Pengingat:", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val now = System.currentTimeMillis()
                    listOf(
                        "10 Menit" to 600000L,
                        "1 Jam" to 3600000L,
                        "3 Jam" to 10800000L,
                        "Besok Pagi" to 86400000L
                    ).forEach { (label, offset) ->
                        val isSelected = (dueDateMillis - now) in (offset - 50000L)..(offset + 50000L)
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { dueDateMillis = now + offset }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Priority selector
                Text("Prioritas:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HIGH" to "Tinggi", "MEDIUM" to "Sedang", "LOW" to "Rendah").forEach { (pKey, pLabel) ->
                        FilterChip(
                            selected = priority == pKey,
                            onClick = { priority = pKey },
                            label = { Text(pLabel) }
                        )
                    }
                }

                // Notification alarm toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Aktifkan Notifikasi Alarm", style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = alarmEnabled,
                        onCheckedChange = { alarmEnabled = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            item?.id ?: 0L,
                            title,
                            description,
                            dueDateMillis,
                            priority,
                            category,
                            alarmEnabled
                        )
                    }
                }
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

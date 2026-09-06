package com.bimantara

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bimantara.core.i18n.AppLanguageManager
import com.bimantara.core.i18n.AppStrings
import com.bimantara.core.i18n.LocalAppStrings
import com.bimantara.feature.cleaner.CleanerScreen
import com.bimantara.feature.cleaner.CleanerViewModel
import com.bimantara.feature.filemanager.FileManagerScreen
import com.bimantara.feature.filemanager.FileManagerViewModel
import com.bimantara.feature.notes.NotesScreen
import com.bimantara.feature.notes.NotesViewModel
import com.bimantara.core.worker.WorkManagerScheduler
import com.bimantara.feature.planner.PlannerNotificationHelper
import com.bimantara.feature.planner.PlannerScreen
import com.bimantara.feature.planner.PlannerViewModel
import com.bimantara.feature.scanner.CamScannerScreen
import com.bimantara.feature.scanner.CamScannerViewModel
import com.bimantara.ui.theme.MyApplicationTheme

enum class MainDestination(
    val defaultTitle: String,
    val icon: ImageVector,
    val activeColor: Color,
    val testTag: String
) {
    FILE_MANAGER("Explorer", Icons.Default.Folder, Color(0xFF0284C7), "nav_file_manager"),
    CAM_SCANNER("Scanner", Icons.Default.DocumentScanner, Color(0xFF10B981), "nav_cam_scanner"),
    NOTES("Catatan", Icons.Default.EditNote, Color(0xFFF59E0B), "nav_notes"),
    PLANNER("Planner", Icons.Default.EventNote, Color(0xFF8B5CF6), "nav_planner"),
    CLEANER("Cleaner", Icons.Default.CleaningServices, Color(0xFFEF4444), "nav_cleaner")
}

fun MainDestination.getLocalizedTitle(strings: AppStrings): String = when (this) {
    MainDestination.FILE_MANAGER -> strings.navExplorer
    MainDestination.CAM_SCANNER -> strings.navScanner
    MainDestination.NOTES -> strings.navNotes
    MainDestination.PLANNER -> strings.navPlanner
    MainDestination.CLEANER -> strings.navCleaner
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create notification channel early for planner alarms
        PlannerNotificationHelper.scheduleAlarm(this, 0, "", "", 0)
        com.bimantara.feature.planner.PlannerAlarmReceiver.createNotificationChannel(this)

        // Schedule WorkManager daily background maintenance & encrypted backup
        WorkManagerScheduler.scheduleDailyMaintenance(this)

        setContent {
            val appStrings by AppLanguageManager.strings.collectAsState()
            CompositionLocalProvider(LocalAppStrings provides appStrings) {
                MyApplicationTheme(dynamicColor = false) {
                    MultiToolsMainApp()
                }
            }
        }
    }
}

@Composable
fun MultiToolsMainApp() {
    val context = LocalContext.current
    var selectedDestination by rememberSaveable { mutableStateOf(MainDestination.FILE_MANAGER) }
    var showAboutDialog by remember { mutableStateOf(false) }

    // Request runtime permissions on launch (Notifications on Android 13+, Audio recording, Camera)
    val permissionsToRequest = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
            add(Manifest.permission.RECORD_AUDIO)
            add(Manifest.permission.CAMERA)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // Permissions granted or dismissed gracefully
    }

    LaunchedEffect(Unit) {
        val missing = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    // ViewModels for the 5 tools
    val fileManagerViewModel: FileManagerViewModel = viewModel()
    val camScannerViewModel: CamScannerViewModel = viewModel()
    val notesViewModel: NotesViewModel = viewModel()
    val plannerViewModel: PlannerViewModel = viewModel()
    val cleanerViewModel: CleanerViewModel = viewModel()
    val strings = LocalAppStrings.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        if (isExpandedScreen) {
            // Wide / Tablet screen layout with NavigationRail
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    MainDestination.entries.forEach { destination ->
                        val isSelected = selectedDestination == destination
                        val localizedTitle = destination.getLocalizedTitle(strings)
                        NavigationRailItem(
                            selected = isSelected,
                            onClick = { selectedDestination = destination },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = localizedTitle,
                                    tint = if (isSelected) destination.activeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    text = localizedTitle,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag(destination.testTag)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    NavigationRailItem(
                        selected = false,
                        onClick = { showAboutDialog = true },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Tentang Aplikasi",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        label = {
                            Text(
                                text = "About",
                                fontSize = 11.sp
                            )
                        },
                        modifier = Modifier.testTag("nav_about_item")
                    )
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    DestinationScreenContent(
                        destination = selectedDestination,
                        fileManagerViewModel = fileManagerViewModel,
                        camScannerViewModel = camScannerViewModel,
                        notesViewModel = notesViewModel,
                        plannerViewModel = plannerViewModel,
                        cleanerViewModel = cleanerViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            // Mobile / Compact screen layout with Bottom NavigationBar
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MainDestination.entries.forEach { destination ->
                            val isSelected = selectedDestination == destination
                            val localizedTitle = destination.getLocalizedTitle(strings)
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = { selectedDestination = destination },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = localizedTitle,
                                        tint = if (isSelected) destination.activeColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = localizedTitle,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = destination.activeColor.copy(alpha = 0.15f)
                                ),
                                modifier = Modifier.testTag(destination.testTag)
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    DestinationScreenContent(
                        destination = selectedDestination,
                        fileManagerViewModel = fileManagerViewModel,
                        camScannerViewModel = camScannerViewModel,
                        notesViewModel = notesViewModel,
                        plannerViewModel = plannerViewModel,
                        cleanerViewModel = cleanerViewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        if (showAboutDialog) {
            com.bimantara.ui.about.AboutDialog(
                onDismiss = { showAboutDialog = false }
            )
        }
    }
}

@Composable
fun DestinationScreenContent(
    destination: MainDestination,
    fileManagerViewModel: FileManagerViewModel,
    camScannerViewModel: CamScannerViewModel,
    notesViewModel: NotesViewModel,
    plannerViewModel: PlannerViewModel,
    cleanerViewModel: CleanerViewModel,
    modifier: Modifier = Modifier
) {
    when (destination) {
        MainDestination.FILE_MANAGER -> {
            FileManagerScreen(viewModel = fileManagerViewModel, modifier = modifier)
        }
        MainDestination.CAM_SCANNER -> {
            CamScannerScreen(viewModel = camScannerViewModel, modifier = modifier)
        }
        MainDestination.NOTES -> {
            NotesScreen(viewModel = notesViewModel, modifier = modifier)
        }
        MainDestination.PLANNER -> {
            PlannerScreen(viewModel = plannerViewModel, modifier = modifier)
        }
        MainDestination.CLEANER -> {
            CleanerScreen(viewModel = cleanerViewModel, modifier = modifier)
        }
    }
}

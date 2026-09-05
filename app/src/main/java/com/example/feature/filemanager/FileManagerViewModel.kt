package com.bimantara.feature.filemanager

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Environment
import android.text.format.Formatter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class FileItem(
    val file: File,
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val formattedSize: String,
    val lastModified: Long,
    val formattedDate: String,
    val extension: String,
    val isHidden: Boolean,
    val matchedInName: Boolean = false,
    val matchedInContent: Boolean = false,
    val contentSnippet: String? = null
)

enum class DateFilterOption {
    ANY, TODAY, PAST_7_DAYS, PAST_30_DAYS
}

enum class SizeFilterOption {
    ANY, SMALL_UNDER_1MB, MEDIUM_1_TO_10MB, LARGE_OVER_10MB
}

data class NetworkInfoState(
    val isConnected: Boolean = false,
    val connectionType: String = "None",
    val localIp: String = "0.0.0.0",
    val gateway: String = "0.0.0.0",
    val ssid: String = "Unknown",
    val subnet: String = "255.255.255.0",
    val linkSpeedMbps: Int = 0
)

data class DiscoveredDevice(
    val ip: String,
    val hostname: String,
    val pingMs: Long,
    val services: List<String> = emptyList()
)

enum class FileViewMode {
    DETAILS_LIST, GRID
}

enum class FileSortMode {
    NAME, DATE, SIZE, TYPE
}

enum class ExplorerTab {
    FILES, NETWORK
}

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val _currentTab = MutableStateFlow(ExplorerTab.FILES)
    val currentTab: StateFlow<ExplorerTab> = _currentTab.asStateFlow()

    private val _currentDrive = MutableStateFlow("C:")
    val currentDrive: StateFlow<String> = _currentDrive.asStateFlow()

    private val _currentPath = MutableStateFlow("")
    val currentPath: StateFlow<String> = _currentPath.asStateFlow()

    private val _fileItems = MutableStateFlow<List<FileItem>>(emptyList())
    val fileItems: StateFlow<List<FileItem>> = _fileItems.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _dateFilter = MutableStateFlow(DateFilterOption.ANY)
    val dateFilter: StateFlow<DateFilterOption> = _dateFilter.asStateFlow()

    private val _sizeFilter = MutableStateFlow(SizeFilterOption.ANY)
    val sizeFilter: StateFlow<SizeFilterOption> = _sizeFilter.asStateFlow()

    private val _searchInContent = MutableStateFlow(false)
    val searchInContent: StateFlow<Boolean> = _searchInContent.asStateFlow()

    private val _isSearchingContent = MutableStateFlow(false)
    val isSearchingContent: StateFlow<Boolean> = _isSearchingContent.asStateFlow()

    private val _viewMode = MutableStateFlow(FileViewMode.DETAILS_LIST)
    val viewMode: StateFlow<FileViewMode> = _viewMode.asStateFlow()

    private val _sortMode = MutableStateFlow(FileSortMode.NAME)
    val sortMode: StateFlow<FileSortMode> = _sortMode.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _networkInfo = MutableStateFlow(NetworkInfoState())
    val networkInfo: StateFlow<NetworkInfoState> = _networkInfo.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredDevice>> = _discoveredDevices.asStateFlow()

    private val _isScanningNetwork = MutableStateFlow(false)
    val isScanningNetwork: StateFlow<Boolean> = _isScanningNetwork.asStateFlow()

    private val _pingResult = MutableStateFlow<String?>(null)
    val pingResult: StateFlow<String?> = _pingResult.asStateFlow()

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private var searchJob: Job? = null
    private val textContentCache = mutableMapOf<String, String>()
    private val textExtensions = setOf(
        "txt", "md", "json", "xml", "csv", "log", "html", "htm", "kt", "java",
        "gradle", "properties", "env", "sql", "js", "ts", "py", "sh", "yaml", "yml", "conf", "ini"
    )
    private var rawFiles: List<File> = emptyList()

    init {
        // Initialize default storage drive (C: or App Documents)
        val defaultDir = getDriveDirectory("C:")
        _currentPath.value = defaultDir.absolutePath
        loadDirectory(defaultDir)
        refreshNetworkInfo()
    }

    fun setTab(tab: ExplorerTab) {
        _currentTab.value = tab
        if (tab == ExplorerTab.NETWORK) {
            refreshNetworkInfo()
        }
    }

    fun setDrive(drive: String) {
        _currentDrive.value = drive
        val dir = getDriveDirectory(drive)
        _currentPath.value = dir.absolutePath
        loadDirectory(dir)
    }

    private fun getDriveDirectory(drive: String): File {
        val context = getApplication<Application>()
        return when (drive) {
            "C:" -> {
                // Internal storage or external primary
                val external = Environment.getExternalStorageDirectory()
                if (external != null && external.exists() && external.canRead()) {
                    external
                } else {
                    context.getExternalFilesDir(null) ?: context.filesDir
                }
            }
            "D:" -> {
                // Documents / Sandbox
                File(context.filesDir, "Documents").apply { if (!exists()) mkdirs() }
            }
            else -> context.filesDir
        }
    }

    fun navigateTo(directory: File) {
        if (directory.isDirectory && directory.canRead()) {
            _currentPath.value = directory.absolutePath
            loadDirectory(directory)
        }
    }

    fun navigateUp() {
        val current = File(_currentPath.value)
        val parent = current.parentFile
        if (parent != null && parent.canRead()) {
            _currentPath.value = parent.absolutePath
            loadDirectory(parent)
        }
    }

    fun refreshCurrentDirectory() {
        loadDirectory(File(_currentPath.value))
    }

    fun setSortMode(mode: FileSortMode) {
        _sortMode.value = mode
        triggerSearch()
    }

    fun setViewMode(mode: FileViewMode) {
        _viewMode.value = mode
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        triggerSearch()
    }

    fun setDateFilter(option: DateFilterOption) {
        _dateFilter.value = option
        triggerSearch()
    }

    fun setSizeFilter(option: SizeFilterOption) {
        _sizeFilter.value = option
        triggerSearch()
    }

    fun toggleSearchInContent(enabled: Boolean) {
        _searchInContent.value = enabled
        triggerSearch()
    }

    fun clearFilters() {
        _searchQuery.value = ""
        _dateFilter.value = DateFilterOption.ANY
        _sizeFilter.value = SizeFilterOption.ANY
        _searchInContent.value = false
        triggerSearch()
    }

    private fun triggerSearch() {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            sortAndFilterItems()
        }
    }

    private fun loadDirectory(dir: File) {
        textContentCache.clear()
        viewModelScope.launch {
            _isLoading.value = true
            rawFiles = withContext(Dispatchers.IO) {
                try {
                    // Seed standard folders if empty sandbox
                    if (dir.listFiles().isNullOrEmpty() && dir == getApplication<Application>().filesDir) {
                        File(dir, "Documents").mkdirs()
                        File(dir, "Downloads").mkdirs()
                        File(dir, "Pictures").mkdirs()
                        File(dir, "Welcome.txt").writeText("Welcome to Multi Tools Windows-style File Manager!")
                    }
                    dir.listFiles()?.toList() ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }
            }
            sortAndFilterItems()
            _isLoading.value = false
        }
    }

    private suspend fun sortAndFilterItems() = withContext(Dispatchers.IO) {
        val query = _searchQuery.value.trim().lowercase(Locale.getDefault())
        val searchContent = _searchInContent.value && query.isNotEmpty()
        val dateOption = _dateFilter.value
        val sizeOption = _sizeFilter.value
        val now = System.currentTimeMillis()

        if (searchContent) {
            _isSearchingContent.value = true
        }

        val mapped = rawFiles.mapNotNull { file ->
            val isDir = file.isDirectory
            val size = if (isDir) 0L else file.length()
            val lastMod = file.lastModified()

            // 1. Date Filter
            val matchesDate = when (dateOption) {
                DateFilterOption.ANY -> true
                DateFilterOption.TODAY -> lastMod >= now - (24 * 60 * 60 * 1000L)
                DateFilterOption.PAST_7_DAYS -> lastMod >= now - (7 * 24 * 60 * 60 * 1000L)
                DateFilterOption.PAST_30_DAYS -> lastMod >= now - (30 * 24 * 60 * 60 * 1000L)
            }
            if (!matchesDate) return@mapNotNull null

            // 2. Size Filter
            val matchesSize = when {
                isDir -> true
                sizeOption == SizeFilterOption.ANY -> true
                sizeOption == SizeFilterOption.SMALL_UNDER_1MB -> size < 1024 * 1024L
                sizeOption == SizeFilterOption.MEDIUM_1_TO_10MB -> size in (1024 * 1024L)..(10 * 1024 * 1024L)
                sizeOption == SizeFilterOption.LARGE_OVER_10MB -> size > 10 * 1024 * 1024L
                else -> true
            }
            if (!matchesSize) return@mapNotNull null

            // 3. Name Match
            val matchesName = query.isEmpty() || file.name.lowercase(Locale.getDefault()).contains(query)

            // 4. Content Match
            var matchesContent = false
            var contentSnippet: String? = null

            if (searchContent && !isDir) {
                val ext = file.extension.lowercase(Locale.getDefault())
                if (textExtensions.contains(ext) && file.length() <= 5 * 1024 * 1024L) {
                    try {
                        val content = textContentCache.getOrPut(file.absolutePath) {
                            file.readText(Charsets.UTF_8)
                        }
                        val index = content.indexOf(query, ignoreCase = true)
                        if (index >= 0) {
                            matchesContent = true
                            val start = (index - 25).coerceAtLeast(0)
                            val end = (index + query.length + 35).coerceAtMost(content.length)
                            val snippetText = content.substring(start, end).replace('\n', ' ').trim()
                            contentSnippet = "...$snippetText..."
                        }
                    } catch (e: Exception) {
                        // Ignore binary or unreadable files
                    }
                }
            }

            // Overall search match check
            if (query.isNotEmpty() && !matchesName && !matchesContent) {
                return@mapNotNull null
            }

            val formattedSize = if (isDir) {
                "${file.list()?.size ?: 0} items"
            } else {
                formatFileSize(size)
            }
            val ext = if (isDir) "Folder" else file.extension.uppercase(Locale.getDefault())

            FileItem(
                file = file,
                name = file.name,
                path = file.absolutePath,
                isDirectory = isDir,
                sizeBytes = size,
                formattedSize = formattedSize,
                lastModified = lastMod,
                formattedDate = dateFormat.format(Date(lastMod)),
                extension = ext,
                isHidden = file.name.startsWith("."),
                matchedInName = matchesName && query.isNotEmpty(),
                matchedInContent = matchesContent,
                contentSnippet = contentSnippet
            )
        }

        val sorted = when (_sortMode.value) {
            FileSortMode.NAME -> mapped.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() })
            FileSortMode.DATE -> mapped.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.lastModified })
            FileSortMode.SIZE -> mapped.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenByDescending { it.sizeBytes })
            FileSortMode.TYPE -> mapped.sortedWith(compareBy<FileItem> { !it.isDirectory }.thenBy { it.extension })
        }

        _isSearchingContent.value = false
        _fileItems.value = sorted
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun createFolder(name: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val newFolder = File(_currentPath.value, name.trim())
                if (newFolder.exists()) {
                    onResult(false, "Folder already exists")
                } else {
                    val success = newFolder.mkdirs()
                    if (success) {
                        refreshCurrentDirectory()
                        onResult(true, "Folder created")
                    } else {
                        onResult(false, "Failed to create folder")
                    }
                }
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Error creating folder")
            }
        }
    }

    fun deleteItem(item: FileItem, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = if (item.isDirectory) {
                item.file.deleteRecursively()
            } else {
                item.file.delete()
            }
            if (success) refreshCurrentDirectory()
            onResult(success)
        }
    }

    fun renameItem(item: FileItem, newName: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val dest = File(item.file.parentFile, newName.trim())
            val success = item.file.renameTo(dest)
            if (success) refreshCurrentDirectory()
            onResult(success)
        }
    }

    // --- Network Explorer ---
    fun refreshNetworkInfo() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

            val activeNetwork = connectivityManager?.activeNetwork
            val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)

            val isConnected = capabilities != null
            val isWifi = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
            val isCellular = capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

            val connType = when {
                isWifi -> "Wi-Fi (WLAN)"
                isCellular -> "Cellular Mobile Data"
                capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "Ethernet"
                else -> if (isConnected) "Connected" else "Disconnected"
            }

            var ip = "127.0.0.1"
            var ssid = "Network Connection"
            var linkSpeed = 0

            if (isWifi && wifiManager != null) {
                val wifiInfo = wifiManager.connectionInfo
                val ipInt = wifiInfo.ipAddress
                if (ipInt != 0) {
                    ip = Formatter.formatIpAddress(ipInt)
                }
                ssid = wifiInfo.ssid?.replace("\"", "") ?: "Wi-Fi"
                linkSpeed = wifiInfo.linkSpeed
            } else {
                try {
                    val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
                    while (interfaces.hasMoreElements()) {
                        val intf = interfaces.nextElement()
                        val addrs = intf.inetAddresses
                        while (addrs.hasMoreElements()) {
                            val addr = addrs.nextElement()
                            if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                                ip = addr.hostAddress ?: ip
                                break
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            val gateway = if (ip.contains(".")) {
                val parts = ip.split(".")
                "${parts[0]}.${parts[1]}.${parts[2]}.1"
            } else "192.168.1.1"

            _networkInfo.value = NetworkInfoState(
                isConnected = isConnected,
                connectionType = connType,
                localIp = ip,
                gateway = gateway,
                ssid = ssid,
                subnet = "255.255.255.0",
                linkSpeedMbps = linkSpeed
            )
        }
    }

    fun runPingTest(host: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _pingResult.value = "Pinging $host..."
            try {
                val start = System.currentTimeMillis()
                val address = InetAddress.getByName(host)
                val reachable = address.isReachable(3000)
                val duration = System.currentTimeMillis() - start
                if (reachable) {
                    _pingResult.value = "Reply from ${address.hostAddress}: time=${duration}ms status=ONLINE"
                } else {
                    // Try socket fallback on port 80 or 443
                    val sockStart = System.currentTimeMillis()
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(host, 80), 2000)
                    }
                    val sockDuration = System.currentTimeMillis() - sockStart
                    _pingResult.value = "Connected to $host: time=${sockDuration}ms"
                }
            } catch (e: Exception) {
                _pingResult.value = "Ping to $host failed: ${e.message ?: "Destination host unreachable"}"
            }
        }
    }

    fun scanLocalSubnet() {
        if (_isScanningNetwork.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isScanningNetwork.value = true
            _discoveredDevices.value = emptyList()

            val ip = _networkInfo.value.localIp
            val prefix = if (ip.contains(".")) {
                val parts = ip.split(".")
                "${parts[0]}.${parts[1]}.${parts[2]}."
            } else "192.168.1."

            val results = mutableListOf<DiscoveredDevice>()

            // Always add Gateway and Current Device first
            results.add(
                DiscoveredDevice(
                    ip = _networkInfo.value.gateway,
                    hostname = "Default Gateway / Router",
                    pingMs = 2,
                    services = listOf("HTTP 80", "DNS 53")
                )
            )
            results.add(
                DiscoveredDevice(
                    ip = ip,
                    hostname = "This Android Device (Localhost)",
                    pingMs = 0,
                    services = listOf("Multi Tools Engine")
                )
            )
            _discoveredDevices.value = results.toList()

            // Concurrently scan a selective range of subnet for responsive hosts
            for (i in 1..25) {
                val testIp = "$prefix$i"
                if (testIp == ip || testIp == _networkInfo.value.gateway) continue
                try {
                    val addr = InetAddress.getByName(testIp)
                    val start = System.currentTimeMillis()
                    val reachable = addr.isReachable(300)
                    val elapsed = System.currentTimeMillis() - start
                    if (reachable) {
                        results.add(
                            DiscoveredDevice(
                                ip = testIp,
                                hostname = addr.canonicalHostName ?: "Host $i",
                                pingMs = elapsed,
                                services = listOf("LAN Host")
                            )
                        )
                        _discoveredDevices.value = results.toList()
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }

            _isScanningNetwork.value = false
        }
    }
}

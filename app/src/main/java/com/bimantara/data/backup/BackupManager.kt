package com.bimantara.data.backup

import android.content.Context
import com.bimantara.core.security.BackupCrypto
import com.bimantara.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupResult(
    val isSuccess: Boolean,
    val backupFile: File?,
    val notesCount: Int,
    val plannerCount: Int,
    val docsCount: Int,
    val fileSizeBytes: Long,
    val errorMessage: String? = null
)

data class RestoreResult(
    val isSuccess: Boolean,
    val restoredNotes: Int,
    val restoredPlannerItems: Int,
    val restoredDocs: Int,
    val errorMessage: String? = null
)

data class CleanupResult(
    val filesDeleted: Int,
    val bytesFreed: Long
)

data class BackupFileInfo(
    val file: File,
    val name: String,
    val sizeBytes: Long,
    val timestamp: Long,
    val formattedDate: String,
    val isEncrypted: Boolean
)

data class LastBackupInfo(
    val timestamp: Long,
    val formattedDate: String,
    val fileName: String,
    val fileSizeBytes: Long,
    val notesCount: Int,
    val plannerCount: Int,
    val isSuccess: Boolean
)

object BackupManager {

    private const val PREFS_NAME = "multitools_backup_prefs"
    private const val KEY_LAST_BACKUP_TIME = "last_backup_time"
    private const val KEY_LAST_BACKUP_FILE = "last_backup_file"
    private const val KEY_LAST_BACKUP_SIZE = "last_backup_size"
    private const val KEY_LAST_BACKUP_NOTES = "last_backup_notes"
    private const val KEY_LAST_BACKUP_PLANNER = "last_backup_planner"
    private const val KEY_LAST_BACKUP_SUCCESS = "last_backup_success"
    private const val KEY_LAST_CLEANUP_TIME = "last_cleanup_time"
    private const val KEY_LAST_CLEANUP_BYTES = "last_cleanup_bytes"

    /**
     * Primary internal directory for storing encrypted backups.
     */
    fun getBackupDirectory(context: Context): File {
        val dir = File(context.filesDir, "backups")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Creates an encrypted backup of the user data (Notes, Planner, and Scanned Docs).
     */
    suspend fun createEncryptedBackup(context: Context): BackupResult = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val notes = db.noteDao().getAllNotesDirect()
            val plannerItems = db.plannerDao().getAllItemsDirect()
            val docs = db.docDao().getAllDocsDirect()

            val payload = BackupPayload(
                version = 1,
                timestamp = System.currentTimeMillis(),
                notes = notes,
                plannerItems = plannerItems,
                scannedDocs = docs
            )

            val jsonString = payload.toJson()
            val encryptedBytes = BackupCrypto.encryptPayload(context, jsonString)

            val backupDir = getBackupDirectory(context)
            val timeStampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "backup_encrypted_$timeStampStr.enc")

            backupFile.writeBytes(encryptedBytes)

            // Also mirror to external app files directory if available for user convenience
            try {
                val externalDir = context.getExternalFilesDir("backups")
                if (externalDir != null) {
                    if (!externalDir.exists()) externalDir.mkdirs()
                    val externalFile = File(externalDir, backupFile.name)
                    backupFile.copyTo(externalFile, overwrite = true)
                }
            } catch (e: Exception) {
                // Non-fatal if external storage unavailable
            }

            // Save record in shared prefs
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit()
                .putLong(KEY_LAST_BACKUP_TIME, System.currentTimeMillis())
                .putString(KEY_LAST_BACKUP_FILE, backupFile.name)
                .putLong(KEY_LAST_BACKUP_SIZE, backupFile.length())
                .putInt(KEY_LAST_BACKUP_NOTES, notes.size)
                .putInt(KEY_LAST_BACKUP_PLANNER, plannerItems.size)
                .putBoolean(KEY_LAST_BACKUP_SUCCESS, true)
                .apply()

            BackupResult(
                isSuccess = true,
                backupFile = backupFile,
                notesCount = notes.size,
                plannerCount = plannerItems.size,
                docsCount = docs.size,
                fileSizeBytes = backupFile.length()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            BackupResult(
                isSuccess = false,
                backupFile = null,
                notesCount = 0,
                plannerCount = 0,
                docsCount = 0,
                fileSizeBytes = 0L,
                errorMessage = e.localizedMessage ?: "Unknown error"
            )
        }
    }

    /**
     * Restores data from an encrypted backup file.
     */
    suspend fun restoreFromEncryptedBackup(context: Context, file: File): RestoreResult = withContext(Dispatchers.IO) {
        try {
            if (!file.exists()) {
                return@withContext RestoreResult(false, 0, 0, 0, "Backup file not found")
            }

            val encryptedBytes = file.readBytes()
            val decryptedJson = BackupCrypto.decryptPayload(context, encryptedBytes)
            val payload = BackupPayload.fromJson(decryptedJson)

            val db = AppDatabase.getDatabase(context)

            if (payload.notes.isNotEmpty()) {
                db.noteDao().insertAll(payload.notes)
            }
            if (payload.plannerItems.isNotEmpty()) {
                db.plannerDao().insertAll(payload.plannerItems)
            }
            if (payload.scannedDocs.isNotEmpty()) {
                db.docDao().insertAll(payload.scannedDocs)
            }

            RestoreResult(
                isSuccess = true,
                restoredNotes = payload.notes.size,
                restoredPlannerItems = payload.plannerItems.size,
                restoredDocs = payload.scannedDocs.size
            )
        } catch (e: Exception) {
            e.printStackTrace()
            RestoreResult(false, 0, 0, 0, e.localizedMessage ?: "Failed to restore backup")
        }
    }

    /**
     * Lists all encrypted backup files stored on the device.
     */
    fun listEncryptedBackups(context: Context): List<BackupFileInfo> {
        val result = mutableListOf<BackupFileInfo>()
        val seenPaths = mutableSetOf<String>()
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

        val dirsToCheck = listOfNotNull(
            getBackupDirectory(context),
            context.getExternalFilesDir("backups")
        )

        for (dir in dirsToCheck) {
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.endsWith(".enc") && !seenPaths.contains(file.name)) {
                        seenPaths.add(file.name)
                        val isEnc = BackupCrypto.isEncryptedBackupFile(file)
                        result.add(
                            BackupFileInfo(
                                file = file,
                                name = file.name,
                                sizeBytes = file.length(),
                                timestamp = file.lastModified(),
                                formattedDate = dateFormat.format(Date(file.lastModified())),
                                isEncrypted = isEnc
                            )
                        )
                    }
                }
            }
        }

        return result.sortedByDescending { it.timestamp }
    }

    /**
     * Cleans up older backups, keeping only the most recent [keepCount] backups.
     */
    suspend fun cleanupOldBackups(context: Context, keepCount: Int = 7): Int = withContext(Dispatchers.IO) {
        val backups = listEncryptedBackups(context)
        if (backups.size <= keepCount) return@withContext 0

        var deletedCount = 0
        val toDelete = backups.drop(keepCount)
        for (item in toDelete) {
            try {
                if (item.file.delete()) {
                    deletedCount++
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        deletedCount
    }

    /**
     * Performs daily cleanup of temporary cache files and old temp files.
     */
    suspend fun performDailyCleanup(context: Context): CleanupResult = withContext(Dispatchers.IO) {
        var filesDeleted = 0
        var bytesFreed = 0L

        // Clean cacheDir
        val cacheDir = context.cacheDir
        if (cacheDir.exists() && cacheDir.isDirectory) {
            cacheDir.listFiles()?.forEach { file ->
                try {
                    val length = file.length()
                    if (file.delete()) {
                        filesDeleted++
                        bytesFreed += length
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }

        // Clean temporary scan image/pdf exports older than 7 days in filesDir
        val scannedDir = File(context.filesDir, "scanned_docs")
        val now = System.currentTimeMillis()
        val sevenDaysMs = 7L * 24 * 60 * 60 * 1000

        if (scannedDir.exists() && scannedDir.isDirectory) {
            scannedDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("temp_") && (now - file.lastModified() > sevenDaysMs)) {
                    val length = file.length()
                    if (file.delete()) {
                        filesDeleted++
                        bytesFreed += length
                    }
                }
            }
        }

        // Record cleanup in prefs
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_CLEANUP_TIME, System.currentTimeMillis())
            .putLong(KEY_LAST_CLEANUP_BYTES, bytesFreed)
            .apply()

        CleanupResult(filesDeleted, bytesFreed)
    }

    /**
     * Retrieves the last backup summary information.
     */
    fun getLastBackupInfo(context: Context): LastBackupInfo? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val time = prefs.getLong(KEY_LAST_BACKUP_TIME, 0L)
        if (time == 0L) return null

        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        return LastBackupInfo(
            timestamp = time,
            formattedDate = dateFormat.format(Date(time)),
            fileName = prefs.getString(KEY_LAST_BACKUP_FILE, "backup.enc") ?: "backup.enc",
            fileSizeBytes = prefs.getLong(KEY_LAST_BACKUP_SIZE, 0L),
            notesCount = prefs.getInt(KEY_LAST_BACKUP_NOTES, 0),
            plannerCount = prefs.getInt(KEY_LAST_BACKUP_PLANNER, 0),
            isSuccess = prefs.getBoolean(KEY_LAST_BACKUP_SUCCESS, true)
        )
    }
}

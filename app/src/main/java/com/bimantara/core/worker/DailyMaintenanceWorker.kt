package com.bimantara.core.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.bimantara.R
import com.bimantara.core.i18n.AppLanguageManager
import com.bimantara.data.backup.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scheduled background worker (WorkManager) that performs daily cleanup and
 * encrypted backup of user data (Notes, Planner, and Scanned Docs database)
 * to local internal storage.
 */
class DailyMaintenanceWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val CHANNEL_ID = "multitools_maintenance_channel"
        const val NOTIFICATION_ID = 2001
        const val UNIQUE_PERIODIC_WORK_NAME = "multitools_daily_maintenance"
        const val UNIQUE_ONE_TIME_WORK_NAME = "multitools_immediate_maintenance"

        const val KEY_BACKUP_SUCCESS = "key_backup_success"
        const val KEY_NOTES_COUNT = "key_notes_count"
        const val KEY_PLANNER_COUNT = "key_planner_count"
        const val KEY_BYTES_FREED = "key_bytes_freed"
        const val KEY_FILES_CLEANED = "key_files_cleaned"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // 1. Perform encrypted backup of Notes and Planner database
            val backupResult = BackupManager.createEncryptedBackup(appContext)

            // 2. Cleanup older backups (retain last 7 backups)
            val oldBackupsPruned = BackupManager.cleanupOldBackups(appContext, keepCount = 7)

            // 3. Perform daily cleanup of cache and obsolete temporary files
            val cleanupResult = BackupManager.performDailyCleanup(appContext)

            // 4. Send background notification if backup succeeded
            if (backupResult.isSuccess) {
                sendMaintenanceNotification(
                    notesCount = backupResult.notesCount,
                    plannerCount = backupResult.plannerCount,
                    bytesFreed = cleanupResult.bytesFreed
                )
            }

            val outputData = workDataOf(
                KEY_BACKUP_SUCCESS to backupResult.isSuccess,
                KEY_NOTES_COUNT to backupResult.notesCount,
                KEY_PLANNER_COUNT to backupResult.plannerCount,
                KEY_BYTES_FREED to cleanupResult.bytesFreed,
                KEY_FILES_CLEANED to (cleanupResult.filesDeleted + oldBackupsPruned)
            )

            if (backupResult.isSuccess) {
                Result.success(outputData)
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(workDataOf("error" to (e.localizedMessage ?: "Maintenance failed")))
        }
    }

    private fun sendMaintenanceNotification(
        notesCount: Int,
        plannerCount: Int,
        bytesFreed: Long
    ) {
        try {
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Daily Maintenance & Backup",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Notifications for automatic encrypted backups and cleanup"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val strings = AppLanguageManager.strings.value
            val title = if (strings.appName == "Multi Tools") {
                "Daily Backup & Cleanup Complete"
            } else {
                "Pencadangan & Pembersihan Harian Selesai"
            }

            val freedMb = bytesFreed / (1024 * 1024)
            val message = if (strings.appName == "Multi Tools") {
                "Secured $notesCount notes, $plannerCount tasks. Freed ${freedMb}MB storage."
            } else {
                "Mengamankan $notesCount catatan, $plannerCount rencana. Mengosongkan ${freedMb}MB memori."
            }

            val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Non-critical if notification fails
        }
    }
}

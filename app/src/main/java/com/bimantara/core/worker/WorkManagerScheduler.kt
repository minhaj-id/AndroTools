package com.bimantara.core.worker

import android.content.Context
import android.util.Log
import androidx.lifecycle.asFlow
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.UUID
import java.util.concurrent.TimeUnit

object WorkManagerScheduler {

    private const val TAG = "WorkManagerScheduler"

    private fun getWorkManagerSafely(context: Context): WorkManager? {
        return try {
            if (!WorkManager.isInitialized()) {
                val config = Configuration.Builder()
                    .setMinimumLoggingLevel(Log.INFO)
                    .build()
                WorkManager.initialize(context.applicationContext, config)
            }
            WorkManager.getInstance(context)
        } catch (e: Exception) {
            try {
                WorkManager.getInstance(context)
            } catch (ex: Exception) {
                Log.w(TAG, "WorkManager initialization deferred or unavailable in current environment: ${ex.message}")
                null
            }
        }
    }

    /**
     * Schedules daily background maintenance (encrypted backup & cleanup)
     * using WorkManager with battery and storage constraints.
     */
    fun scheduleDailyMaintenance(context: Context) {
        val wm = getWorkManagerSafely(context) ?: return
        try {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresStorageNotLow(true)
                .build()

            val dailyWorkRequest = PeriodicWorkRequestBuilder<DailyMaintenanceWorker>(
                repeatInterval = 24,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = 4,
                flexTimeIntervalUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            wm.enqueueUniquePeriodicWork(
                DailyMaintenanceWorker.UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                dailyWorkRequest
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule daily maintenance", e)
        }
    }

    /**
     * Triggers an immediate one-time backup & cleanup worker.
     */
    fun triggerImmediateMaintenance(context: Context): UUID? {
        val wm = getWorkManagerSafely(context) ?: return null
        return try {
            val immediateRequest = OneTimeWorkRequestBuilder<DailyMaintenanceWorker>()
                .build()

            wm.enqueueUniqueWork(
                DailyMaintenanceWorker.UNIQUE_ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                immediateRequest
            )

            immediateRequest.id
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trigger immediate maintenance", e)
            null
        }
    }

    /**
     * Observes the status of the immediate maintenance task as a Flow.
     */
    fun observeImmediateMaintenance(context: Context): Flow<List<WorkInfo>> {
        val wm = getWorkManagerSafely(context) ?: return emptyFlow()
        return try {
            wm.getWorkInfosForUniqueWorkLiveData(DailyMaintenanceWorker.UNIQUE_ONE_TIME_WORK_NAME)
                .asFlow()
        } catch (e: Exception) {
            emptyFlow()
        }
    }
}

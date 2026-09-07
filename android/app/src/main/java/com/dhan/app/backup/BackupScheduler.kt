package com.dhan.app.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules/cancels the recurring background backup job to match the user's frequency and
 * network-preference settings (Settings -> Data & Privacy -> Backup). "Back up now" does
 * NOT go through this scheduler — it calls BackupRepository.backup() directly and
 * immediately, bypassing both the schedule and the network-type constraint, since it's an
 * explicit one-time action the user is watching happen.
 */
object BackupScheduler {
    private const val UNIQUE_WORK_NAME = "dhan_scheduled_backup"

    fun reschedule(context: Context) {
        val prefs = BackupPrefs(context)
        val workManager = WorkManager.getInstance(context)
        val intervalHours = when (prefs.frequency) {
            BackupFrequency.DAILY -> 24L
            BackupFrequency.WEEKLY -> 24L * 7
            BackupFrequency.MONTHLY -> 24L * 30
            BackupFrequency.MANUAL -> null
        }
        if (intervalHours == null) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (prefs.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()
        val request = PeriodicWorkRequestBuilder<BackupWorker>(intervalHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

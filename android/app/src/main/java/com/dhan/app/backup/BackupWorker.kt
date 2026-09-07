package com.dhan.app.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Runs on WorkManager's schedule (see BackupScheduler) with no UI present — failures are
 *  recorded to BackupPrefs for Settings to surface, never retried noisily or alerted. */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        BackupRepository(applicationContext).runScheduledBackup()
        return Result.success()
    }
}

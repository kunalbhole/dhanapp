package com.dhan.app.backup

import android.content.Context

enum class BackupFrequency { DAILY, WEEKLY, MONTHLY, MANUAL }

/** Plain (unencrypted) SharedPreferences for backup settings and status — none of this is
 *  sensitive; the passphrase itself lives separately in [PassphraseStore]. */
class BackupPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("dhan_backup_prefs", Context.MODE_PRIVATE)

    var frequency: BackupFrequency
        get() = runCatching { BackupFrequency.valueOf(prefs.getString(KEY_FREQUENCY, null) ?: "") }
            .getOrDefault(BackupFrequency.MANUAL)
        set(value) = prefs.edit().putString(KEY_FREQUENCY, value.name).apply()

    var wifiOnly: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, true)
        set(value) = prefs.edit().putBoolean(KEY_WIFI_ONLY, value).apply()

    val lastBackupTimestampMillis: Long get() = prefs.getLong(KEY_LAST_TIMESTAMP, 0L)
    val lastBackupSizeBytes: Long get() = prefs.getLong(KEY_LAST_SIZE, 0L)
    val lastBackupFailed: Boolean get() = prefs.getBoolean(KEY_LAST_FAILED, false)
    val lastBackupErrorMessage: String? get() = prefs.getString(KEY_LAST_ERROR, null)

    fun recordSuccess(timestampMillis: Long, sizeBytes: Long) {
        prefs.edit()
            .putLong(KEY_LAST_TIMESTAMP, timestampMillis)
            .putLong(KEY_LAST_SIZE, sizeBytes)
            .putBoolean(KEY_LAST_FAILED, false)
            .putString(KEY_LAST_ERROR, null)
            .apply()
    }

    fun recordFailure(errorMessage: String) {
        prefs.edit()
            .putBoolean(KEY_LAST_FAILED, true)
            .putString(KEY_LAST_ERROR, errorMessage)
            .apply()
    }

    companion object {
        private const val KEY_FREQUENCY = "frequency"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_LAST_TIMESTAMP = "last_timestamp"
        private const val KEY_LAST_SIZE = "last_size"
        private const val KEY_LAST_FAILED = "last_failed"
        private const val KEY_LAST_ERROR = "last_error"
    }
}

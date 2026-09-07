package com.dhan.app.backup

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Caches the user's backup passphrase ON THIS device only, encrypted at rest via an
 * Android Keystore-backed master key (androidx.security). This exists purely so a scheduled
 * background backup ([BackupWorker]) can run without a UI to prompt the user for the
 * passphrase each time — it never leaves the device and is never uploaded. A fresh device
 * has no access to this store, so restoring there always prompts the user to type the
 * passphrase in by hand; that's intentional, not a gap.
 */
class PassphraseStore(context: Context) {
    private val prefs = run {
        val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context,
            "dhan_backup_passphrase",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun save(passphrase: String) {
        prefs.edit().putString(KEY, passphrase).apply()
    }

    fun get(): String? = prefs.getString(KEY, null)

    fun clear() {
        prefs.edit().remove(KEY).apply()
    }

    companion object {
        private const val KEY = "passphrase"
    }
}

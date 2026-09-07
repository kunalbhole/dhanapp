package com.dhan.app.backup

import android.content.Context
import com.dhan.app.db.DhanDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class BackupResult(val timestampMillis: Long, val sizeBytes: Long)
data class ExistingBackupInfo(val modifiedTimeMillis: Long, val sizeBytes: Long)
data class RestoreResult(val restoredTransactionCount: Int)

class BackupNotSignedInException : Exception("Not signed in to Google")
class BackupNotFoundException : Exception("No backup found for this account")

/**
 * Orchestrates the full backup ("export DB -> encrypt -> upload") and restore
 * ("download -> decrypt -> import DB") flows. Shared by the interactive bridge module
 * (DhanBackupModule, called from JS with a passphrase the user just typed) and the
 * background BackupWorker (which reads the cached passphrase from PassphraseStore since
 * there's no UI to ask). Holds no state of its own — everything needed is read fresh from
 * DhanDb/prefs on each call, so a manual call and a scheduled call behave identically.
 */
class BackupRepository(private val context: Context) {
    private val auth = GoogleAuthManager(context)
    private val drive = DriveBackupClient()
    private val prefs = BackupPrefs(context)

    fun isSignedIn(): Boolean = auth.currentAccount() != null

    fun signedInEmail(): String? = auth.currentAccount()?.email

    suspend fun backup(passphrase: String): BackupResult = withContext(Dispatchers.IO) {
        val account = auth.currentAccount() ?: throw BackupNotSignedInException()
        val plaintext = DhanDb.get(context).exportAllJson().toByteArray(Charsets.UTF_8)
        val encrypted = BackupCrypto.encrypt(plaintext, passphrase)

        val token = auth.getAccessToken(account)
        val existing = drive.findBackupFile(token)
        try {
            drive.upload(token, existing?.id, encrypted)
        } catch (e: DriveBackupClient.DriveException) {
            if (e.code != 401) throw e
            // Cached token had just expired — refresh once and retry before giving up.
            auth.invalidateToken(token)
            val freshToken = auth.getAccessToken(account)
            drive.upload(freshToken, existing?.id, encrypted)
        }

        val now = System.currentTimeMillis()
        prefs.recordSuccess(now, encrypted.size.toLong())
        BackupResult(now, encrypted.size.toLong())
    }

    suspend fun checkForExistingBackup(): ExistingBackupInfo? = withContext(Dispatchers.IO) {
        val account = auth.currentAccount() ?: throw BackupNotSignedInException()
        val token = auth.getAccessToken(account)
        val file = drive.findBackupFile(token) ?: return@withContext null
        ExistingBackupInfo(file.modifiedTimeMillis, file.sizeBytes)
    }

    /** Throws [BackupCrypto.DecryptionFailedException] on a wrong passphrase or corrupted
     *  file, and [BackupNotFoundException] if the account has no backup — callers must
     *  handle both by falling back to fresh onboarding rather than leaving a half state. */
    suspend fun restore(passphrase: String): RestoreResult = withContext(Dispatchers.IO) {
        val account = auth.currentAccount() ?: throw BackupNotSignedInException()
        val token = auth.getAccessToken(account)
        val file = drive.findBackupFile(token) ?: throw BackupNotFoundException()
        val encrypted = drive.download(token, file.id)
        val plaintext = BackupCrypto.decrypt(encrypted, passphrase)
        val count = DhanDb.get(context).importAllJson(String(plaintext, Charsets.UTF_8))
        RestoreResult(count)
    }

    /** Entry point for [BackupWorker]. Never throws — a scheduled run has no UI watching
     *  it, so failures are recorded to prefs for Settings to surface instead. */
    suspend fun runScheduledBackup() {
        val passphrase = PassphraseStore(context).get()
        if (passphrase == null || !isSignedIn()) return
        try {
            backup(passphrase)
        } catch (e: Exception) {
            prefs.recordFailure(e.message ?: e.javaClass.simpleName)
        }
    }
}

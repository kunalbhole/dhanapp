package com.dhan.app.bridge

import android.app.Activity
import android.content.Intent
import com.dhan.app.backup.BackupCrypto
import com.dhan.app.backup.BackupFrequency
import com.dhan.app.backup.BackupNotFoundException
import com.dhan.app.backup.BackupNotSignedInException
import com.dhan.app.backup.BackupPrefs
import com.dhan.app.backup.BackupRepository
import com.dhan.app.backup.BackupScheduler
import com.dhan.app.backup.GoogleAuthManager
import com.dhan.app.backup.PassphraseStore
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.BaseActivityEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.WritableMap
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val RC_SIGN_IN = 4200

/**
 * JS <-> native bridge for Google Drive backup/restore. Every backup/restore call runs on
 * a coroutine off the main thread and resolves/rejects the JS promise when done; sign-in
 * is the one call that has to round-trip through an Activity result, handled via
 * ActivityEventListener the same way RN itself expects for any auth SDK.
 */
class DhanBackupModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private val auth = GoogleAuthManager(reactContext)
    private val repository = BackupRepository(reactContext)
    private val prefs = BackupPrefs(reactContext)
    private val passphraseStore = PassphraseStore(reactContext)

    private var pendingSignInPromise: Promise? = null

    private val activityEventListener = object : BaseActivityEventListener() {
        override fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
            if (requestCode != RC_SIGN_IN) return
            val promise = pendingSignInPromise ?: return
            pendingSignInPromise = null
            try {
                val account = auth.accountFromSignInResult(data)
                promise.resolve(account.email)
            } catch (e: ApiException) {
                promise.reject("dhan_signin_failed", "Google sign-in failed or was cancelled", e)
            }
        }
    }

    init {
        reactContext.addActivityEventListener(activityEventListener)
    }

    override fun getName(): String = "DhanBackup"

    override fun invalidate() {
        super.invalidate()
        reactContext.removeActivityEventListener(activityEventListener)
        scope.cancel()
    }

    @ReactMethod
    fun isSignedIn(promise: Promise) {
        promise.resolve(repository.isSignedIn())
    }

    @ReactMethod
    fun getSignedInEmail(promise: Promise) {
        promise.resolve(repository.signedInEmail())
    }

    @ReactMethod
    fun signIn(promise: Promise) {
        val activity = currentActivity
        if (activity == null) {
            promise.reject("dhan_no_activity", "No active screen to sign in from")
            return
        }
        pendingSignInPromise = promise
        activity.startActivityForResult(auth.signInIntent(), RC_SIGN_IN)
    }

    @ReactMethod
    fun signOut(promise: Promise) {
        scope.launch {
            runCatching { auth.signOut() }
                .onSuccess { promise.resolve(null) }
                .onFailure { promise.reject("dhan_signout_failed", it.message, it) }
        }
    }

    @ReactMethod
    fun hasBackupPassphraseSet(promise: Promise) {
        promise.resolve(passphraseStore.get() != null)
    }

    /** Caches the passphrase on-device (Keystore-backed) so scheduled background backups
     *  can run without prompting — see PassphraseStore. Called once when the user sets up
     *  or changes their backup passphrase. */
    @ReactMethod
    fun setBackupPassphrase(passphrase: String, promise: Promise) {
        passphraseStore.save(passphrase)
        promise.resolve(null)
    }

    /** Uses the passphrase already cached on this device (set via setBackupPassphrase) —
     *  same as a scheduled background backup, so a returning user is never re-prompted for
     *  it. Rejects with dhan_passphrase_not_set if backup has never been set up here. */
    @ReactMethod
    fun backupNow(promise: Promise) {
        val passphrase = passphraseStore.get()
        if (passphrase == null) {
            promise.reject("dhan_passphrase_not_set", "No backup passphrase set on this device yet")
            return
        }
        scope.launch {
            runCatching { repository.backup(passphrase) }
                .onSuccess { promise.resolve(resultMap(it.timestampMillis, it.sizeBytes)) }
                .onFailure { promise.reject(errorCodeFor(it), it.message, it) }
        }
    }

    @ReactMethod
    fun checkForExistingBackup(promise: Promise) {
        scope.launch {
            runCatching { repository.checkForExistingBackup() }
                .onSuccess { info ->
                    if (info == null) {
                        promise.resolve(null)
                    } else {
                        val map = Arguments.createMap()
                        map.putDouble("modifiedTimeMillis", info.modifiedTimeMillis.toDouble())
                        map.putDouble("sizeBytes", info.sizeBytes.toDouble())
                        promise.resolve(map)
                    }
                }
                .onFailure { promise.reject(errorCodeFor(it), it.message, it) }
        }
    }

    @ReactMethod
    fun restoreFromBackup(passphrase: String, promise: Promise) {
        scope.launch {
            runCatching { repository.restore(passphrase) }
                .onSuccess {
                    val map = Arguments.createMap()
                    map.putInt("restoredTransactionCount", it.restoredTransactionCount)
                    promise.resolve(map)
                }
                .onFailure { promise.reject(errorCodeFor(it), it.message, it) }
        }
    }

    @ReactMethod
    fun getBackupSettings(promise: Promise) {
        val map = Arguments.createMap()
        map.putString("frequency", prefs.frequency.name)
        map.putBoolean("wifiOnly", prefs.wifiOnly)
        promise.resolve(map)
    }

    @ReactMethod
    fun setBackupSettings(frequency: String, wifiOnly: Boolean, promise: Promise) {
        runCatching { BackupFrequency.valueOf(frequency) }
            .onSuccess {
                prefs.frequency = it
                prefs.wifiOnly = wifiOnly
                BackupScheduler.reschedule(reactContext)
                promise.resolve(null)
            }
            .onFailure { promise.reject("dhan_bad_frequency", "Unknown frequency: $frequency") }
    }

    @ReactMethod
    fun getLastBackupStatus(promise: Promise) {
        val map = Arguments.createMap()
        map.putDouble("timestampMillis", prefs.lastBackupTimestampMillis.toDouble())
        map.putDouble("sizeBytes", prefs.lastBackupSizeBytes.toDouble())
        map.putBoolean("failed", prefs.lastBackupFailed)
        map.putString("errorMessage", prefs.lastBackupErrorMessage)
        promise.resolve(map)
    }

    private fun resultMap(timestampMillis: Long, sizeBytes: Long): WritableMap {
        val map = Arguments.createMap()
        map.putDouble("timestampMillis", timestampMillis.toDouble())
        map.putDouble("sizeBytes", sizeBytes.toDouble())
        return map
    }

    private fun errorCodeFor(t: Throwable): String = when (t) {
        is BackupNotSignedInException -> "dhan_not_signed_in"
        is BackupNotFoundException -> "dhan_backup_not_found"
        is BackupCrypto.DecryptionFailedException -> "dhan_decrypt_failed"
        else -> "dhan_backup_error"
    }
}

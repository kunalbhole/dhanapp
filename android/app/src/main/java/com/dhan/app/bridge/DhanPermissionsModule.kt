package com.dhan.app.bridge

import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import org.json.JSONObject

/**
 * Notification-listener access isn't a normal Android runtime permission (there's no
 * PermissionsAndroid entry for it) — checking its state and sending the user to the one
 * system settings screen that grants it both need native code. SMS permission itself is
 * a normal dangerous permission and is requested from JS via React Native's built-in
 * PermissionsAndroid API — no bridge method needed for that half.
 *
 * Also carries [getAppVersion] — unrelated to permissions, but it's the smallest existing
 * native-utility module, and a single getter doesn't justify a whole new NativeModule +
 * DhanPackage registration on its own.
 */
class DhanPermissionsModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "DhanPermissions"

    /**
     * versionName/versionCode read straight from this APK's own installed PackageInfo —
     * deliberately not app.json's `version` field, which eas.json's `appVersionSource:
     * "local"` means EAS Build never even reads for a native Android build; the only
     * value guaranteed to match what's actually installed on a device is the one Android
     * itself stamped from android/app/build.gradle's versionName/versionCode at build
     * time. See PROJECT_STATUS.md for the versionName/app.json version mismatch this
     * sidesteps.
     */
    @ReactMethod
    fun getAppVersion(promise: Promise) {
        runCatching {
            val context = reactApplicationContext
            val pkgInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pkgInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pkgInfo.versionCode
            }
            JSONObject().apply {
                put("versionName", pkgInfo.versionName)
                put("versionCode", versionCode)
            }.toString()
        }.onSuccess { promise.resolve(it) }
            .onFailure { promise.reject("dhan_version_error", it) }
    }

    @ReactMethod
    fun isNotificationListenerEnabled(promise: com.facebook.react.bridge.Promise) {
        val context = reactApplicationContext
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?.split(":")
            ?.any { it.contains(context.packageName) } ?: false
        promise.resolve(enabled)
    }

    @ReactMethod
    fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        reactApplicationContext.startActivity(intent)
    }
}

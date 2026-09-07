package com.dhan.app.bridge

import android.content.Intent
import android.provider.Settings
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

/**
 * Notification-listener access isn't a normal Android runtime permission (there's no
 * PermissionsAndroid entry for it) — checking its state and sending the user to the one
 * system settings screen that grants it both need native code. SMS permission itself is
 * a normal dangerous permission and is requested from JS via React Native's built-in
 * PermissionsAndroid API — no bridge method needed for that half.
 */
class DhanPermissionsModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "DhanPermissions"

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

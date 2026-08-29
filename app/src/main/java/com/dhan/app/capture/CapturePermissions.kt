package com.dhan.app.capture

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/** Permission/access checks for the two capture sources. SMS is a normal runtime
 *  permission; notification access is a special grant the user flips in system
 *  Settings (there is no in-app request dialog for it). */
object CapturePermissions {
    const val SMS_PERMISSION = android.Manifest.permission.RECEIVE_SMS
    const val READ_SMS_PERMISSION = android.Manifest.permission.READ_SMS

    fun hasSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, SMS_PERMISSION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, READ_SMS_PERMISSION) == PackageManager.PERMISSION_GRANTED

    fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabledPackages = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            ?: return false
        return enabledPackages.split(":").any { it.contains(context.packageName) }
    }
}

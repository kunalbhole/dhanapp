package com.dhan.app.data.prefs

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf

/** Small local, unsynced app-state store — the Android equivalent of the prototype's
 *  localStorage usage in app.jsx (dhan-onboarded, tweaks.userName, tweaks.isPlus, etc).
 *  The PIN here gates the in-app lock screen only; it is not used for encryption. */
class UserPrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("dhan_prefs", Context.MODE_PRIVATE)

    var hasOnboarded: Boolean
        get() = sp.getBoolean(KEY_ONBOARDED, false)
        set(value) = sp.edit().putBoolean(KEY_ONBOARDED, value).apply()

    var userName: String
        get() = sp.getString(KEY_USER_NAME, "") ?: ""
        set(value) = sp.edit().putString(KEY_USER_NAME, value).apply()

    var isPlus: Boolean
        get() = sp.getBoolean(KEY_IS_PLUS, false)
        set(value) = sp.edit().putBoolean(KEY_IS_PLUS, value).apply()

    var pin: String?
        get() = sp.getString(KEY_PIN, null)
        set(value) = sp.edit().putString(KEY_PIN, value).apply()

    var smsCaptureEnabled: Boolean
        get() = sp.getBoolean(KEY_SMS_CAPTURE, false)
        set(value) = sp.edit().putBoolean(KEY_SMS_CAPTURE, value).apply()

    var notifCaptureEnabled: Boolean
        get() = sp.getBoolean(KEY_NOTIF_CAPTURE, false)
        set(value) = sp.edit().putBoolean(KEY_NOTIF_CAPTURE, value).apply()

    fun clear() = sp.edit().clear().apply()

    companion object {
        private const val KEY_ONBOARDED = "onboarded"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_IS_PLUS = "is_plus"
        private const val KEY_PIN = "pin"
        private const val KEY_SMS_CAPTURE = "sms_capture_enabled"
        private const val KEY_NOTIF_CAPTURE = "notif_capture_enabled"
    }
}

val LocalUserPrefs = staticCompositionLocalOf<UserPrefs> {
    error("LocalUserPrefs not provided — wrap the app in CompositionLocalProvider(LocalUserPrefs provides ...)")
}

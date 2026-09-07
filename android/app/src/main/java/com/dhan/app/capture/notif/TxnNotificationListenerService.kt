package com.dhan.app.capture.notif

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.dhan.app.capture.CaptureIngest

/**
 * Reads notifications only from a known set of bank/UPI/wallet apps ([TRACKED_PACKAGES])
 * to auto-log transactions — deliberately NOT every app's notifications, even though a
 * granted NotificationListenerService technically has access to all of them.
 */
class TxnNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val packageName = sbn.packageName ?: return
        if (packageName !in TRACKED_PACKAGES) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val combined = listOf(title, text, bigText).filter { it.isNotBlank() }.joinToString(". ")
        if (combined.isBlank()) return

        CaptureIngest.process(
            context = applicationContext,
            text = combined,
            source = "NOTIFICATION",
            sourceApp = TRACKED_PACKAGES[packageName] ?: packageName,
            timestampMillis = sbn.postTime,
        )
    }

    companion object {
        val TRACKED_PACKAGES: Map<String, String> = mapOf(
            "com.google.android.apps.nbu.paisa.user" to "Google Pay",
            "com.phonepe.app" to "PhonePe",
            "net.one97.paytm" to "Paytm",
            "in.amazon.mShop.android.shopping" to "Amazon Pay",
            "com.whatsapp" to "WhatsApp Pay",
            "com.freecharge.android" to "Freecharge",
            "com.mobikwik_new" to "MobiKwik",
            "com.sbi.SBIFreedomPlus" to "SBI",
            "com.snapwork.hdfc" to "HDFC Bank",
            "com.csam.icici.bank.imobile" to "ICICI Bank",
            "com.axis.mobile" to "Axis Bank",
            "com.kotak.mobile.k2ftx" to "Kotak Bank",
            "com.msf.kbank.mobile" to "Kotak Bank",
            "com.idbibank.mobile" to "IDBI Bank",
            "com.infrasofttech.kotak" to "Kotak Bank",
            "com.fss.pnbpsp" to "PNB",
            "com.bankofbaroda.upi" to "Bank of Baroda",
            "com.union.upi" to "Union Bank",
        )
    }
}

package com.dhan.app.capture.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.dhan.app.capture.CaptureIngest

/** Reads incoming bank/UPI SMS to auto-log transactions. Only ever inspects message
 *  bodies at receive time — nothing is uploaded, everything stays in the on-device
 *  SQLite database (see db/DhanDb.kt). Plain synchronous SQLite writes are fast enough
 *  to run directly in onReceive without a coroutine dispatch. */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        for (sms in messages) {
            val body = sms.messageBody ?: continue
            val sender = sms.originatingAddress
            CaptureIngest.process(
                context = context,
                text = body,
                source = "SMS",
                sourceApp = sender,
                timestampMillis = sms.timestampMillis,
            )
        }
    }
}

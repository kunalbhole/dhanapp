package com.dhan.app.capture.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.dhan.app.DhanApplication
import com.dhan.app.capture.CaptureIngest
import com.dhan.app.data.db.TxnSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Reads incoming bank/UPI SMS to auto-log transactions. Only ever inspects message
 *  bodies at receive time — nothing is uploaded, everything stays in the on-device
 *  Room database (see data/repo/DhanRepository). */
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val app = context.applicationContext as DhanApplication
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                for (sms in messages) {
                    val body = sms.messageBody ?: continue
                    val sender = sms.originatingAddress
                    CaptureIngest.process(
                        repository = app.repository,
                        text = body,
                        source = TxnSource.SMS,
                        sourceApp = sender,
                        timestampMillis = sms.timestampMillis,
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }
}

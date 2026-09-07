package com.dhan.app.capture

import android.content.Context
import android.provider.Telephony
import com.dhan.app.capture.parser.TransactionParser
import com.dhan.app.db.DhanDb

/** Shared "parse this message, log it, and store a transaction if it matched" path used
 *  by both SmsReceiver and TxnNotificationListenerService. Runs entirely native-side so
 *  it works whether or not the JS runtime/app is currently open. */
object CaptureIngest {
    /** Returns true if the message matched and a transaction was created. */
    fun process(context: Context, text: String, source: String, sourceApp: String?, timestampMillis: Long): Boolean {
        val db = DhanDb.get(context)
        val parsed = TransactionParser.parse(text, sourceApp)
        val createdId = parsed?.let {
            db.insertTransaction(
                merchant = it.merchant,
                note = null,
                amount = it.amountRupees,
                category = it.category,
                timestampMillis = timestampMillis,
                source = source,
                sourceApp = sourceApp,
                rawText = text,
                accountHint = it.accountHint,
            )
        }
        db.logCaptureEvent(
            source = source,
            sourceApp = sourceApp,
            rawText = text,
            timestampMillis = timestampMillis,
            matched = parsed != null,
            createdTxnId = createdId,
        )
        return parsed != null
    }

    /**
     * One-time scan of the device's existing SMS inbox (as opposed to SmsReceiver, which only
     * ever sees new incoming messages going forward). Run right after the user grants SMS
     * permission during onboarding so the app starts with real transaction history instead of
     * an empty ledger. Returns the number of messages that matched and became transactions.
     */
    fun scanHistoricalSms(context: Context): Int {
        var matchedCount = 0
        val projection = arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE)
        val cursor = context.contentResolver.query(
            Telephony.Sms.Inbox.CONTENT_URI,
            projection,
            null,
            null,
            "${Telephony.Sms.DATE} ASC",
        )
        cursor?.use {
            val addressIdx = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIdx = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIdx = it.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (it.moveToNext()) {
                val body = it.getString(bodyIdx) ?: continue
                val address = it.getString(addressIdx)
                val date = it.getLong(dateIdx)
                val matched = process(
                    context = context,
                    text = body,
                    source = "SMS",
                    sourceApp = address,
                    timestampMillis = date,
                )
                if (matched) matchedCount++
            }
        }
        return matchedCount
    }
}

package com.dhan.app.capture

import com.dhan.app.capture.parser.TransactionParser
import com.dhan.app.data.db.CaptureEventEntity
import com.dhan.app.data.db.TransactionEntity
import com.dhan.app.data.db.TxnSource
import com.dhan.app.data.repo.DhanRepository

/** Shared "parse this message, log it, and store a transaction if it matched" path used
 *  by both [com.dhan.app.capture.sms.SmsReceiver] and
 *  [com.dhan.app.capture.notif.TxnNotificationListenerService]. */
object CaptureIngest {
    suspend fun process(
        repository: DhanRepository,
        text: String,
        source: TxnSource,
        sourceApp: String?,
        timestampMillis: Long,
    ) {
        val parsed = TransactionParser.parse(text, sourceApp)
        val createdId = parsed?.let {
            repository.addTransaction(
                TransactionEntity(
                    merchant = it.merchant,
                    amountRupees = it.amountRupees,
                    category = it.category,
                    timestampMillis = timestampMillis,
                    source = source,
                    sourceApp = sourceApp,
                    rawText = text,
                    accountHint = it.accountHint,
                ),
            )
        }
        repository.logCaptureEvent(
            CaptureEventEntity(
                source = source,
                sourceApp = sourceApp,
                rawText = text,
                timestampMillis = timestampMillis,
                matched = parsed != null,
                createdTxnId = createdId,
            ),
        )
    }
}

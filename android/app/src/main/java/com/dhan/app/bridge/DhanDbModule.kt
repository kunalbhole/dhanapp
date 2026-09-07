package com.dhan.app.bridge

import com.dhan.app.capture.CaptureIngest
import com.dhan.app.db.DhanDb
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

/**
 * JS <-> native bridge over [DhanDb]. Every read returns a JSON string (parsed with
 * JSON.parse on the JS side) rather than a WritableMap/WritableArray tree — simpler and
 * harder to get subtly wrong than hand-building bridge collection types field by field.
 */
class DhanDbModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "DhanDb"

    private val db get() = DhanDb.get(reactApplicationContext)

    @ReactMethod
    fun getTransactions(promise: Promise) {
        runCatching { db.getTransactionsJson() }
            .onSuccess { promise.resolve(it) }
            .onFailure { promise.reject("dhan_db_error", it) }
    }

    @ReactMethod
    fun addTransaction(merchant: String, note: String?, amount: Double, category: String, timestampMillis: Double, source: String, promise: Promise) {
        runCatching { db.insertTransaction(merchant, note, amount, category, timestampMillis.toLong(), source, null, null, null) }
            .onSuccess { promise.resolve(it.toDouble()) }
            .onFailure { promise.reject("dhan_db_error", it) }
    }

    @ReactMethod
    fun deleteTransaction(id: Double, promise: Promise) {
        runCatching { db.deleteTransaction(id.toLong()) }
            .onSuccess { promise.resolve(null) }
            .onFailure { promise.reject("dhan_db_error", it) }
    }

    @ReactMethod
    fun getCaptureEvents(promise: Promise) {
        runCatching { db.getCaptureEventsJson() }
            .onSuccess { promise.resolve(it) }
            .onFailure { promise.reject("dhan_db_error", it) }
    }

    @ReactMethod
    fun getBudgets(monthKey: String, promise: Promise) {
        runCatching { db.getBudgetsJson(monthKey) }
            .onSuccess { promise.resolve(it) }
            .onFailure { promise.reject("dhan_db_error", it) }
    }

    @ReactMethod
    fun setBudget(category: String, monthKey: String, limitAmount: Double, promise: Promise) {
        runCatching { db.upsertBudget(category, monthKey, limitAmount) }
            .onSuccess { promise.resolve(null) }
            .onFailure { promise.reject("dhan_db_error", it) }
    }

    @ReactMethod
    fun getBills(promise: Promise) {
        runCatching { db.getBillsJson() }
            .onSuccess { promise.resolve(it) }
            .onFailure { promise.reject("dhan_db_error", it) }
    }

    @ReactMethod
    fun addBill(name: String, amount: Double, dueDateMillis: Double, status: String, repeatMonthly: Boolean, category: String, promise: Promise) {
        runCatching { db.insertBill(name, amount, dueDateMillis.toLong(), status, repeatMonthly, category) }
            .onSuccess { promise.resolve(it.toDouble()) }
            .onFailure { promise.reject("dhan_db_error", it) }
    }

    @ReactMethod
    fun setBillStatus(id: Double, status: String, promise: Promise) {
        runCatching { db.setBillStatus(id.toLong(), status) }
            .onSuccess { promise.resolve(null) }
            .onFailure { promise.reject("dhan_db_error", it) }
    }

    @ReactMethod
    fun getGoals(promise: Promise) {
        runCatching { db.getGoalsJson() }
            .onSuccess { promise.resolve(it) }
            .onFailure { promise.reject("dhan_db_error", it) }
    }

    @ReactMethod
    fun addGoal(name: String, targetAmount: Double, savedAmount: Double, promise: Promise) {
        runCatching { db.insertGoal(name, targetAmount, savedAmount) }
            .onSuccess { promise.resolve(it.toDouble()) }
            .onFailure { promise.reject("dhan_db_error", it) }
    }

    @ReactMethod
    fun updateGoalSaved(id: Double, savedAmount: Double, promise: Promise) {
        runCatching { db.updateGoalSaved(id.toLong(), savedAmount) }
            .onSuccess { promise.resolve(null) }
            .onFailure { promise.reject("dhan_db_error", it) }
    }

    @ReactMethod
    fun getFriends(promise: Promise) {
        runCatching { db.getFriendsJson() }
            .onSuccess { promise.resolve(it) }
            .onFailure { promise.reject("dhan_db_error", it) }
    }

    /** One-time scan of the device's existing SMS inbox (requires READ_SMS, already granted
     *  by the time this is called). Returns the number of matched transactions created. */
    @ReactMethod
    fun scanHistoricalSms(promise: Promise) {
        runCatching { CaptureIngest.scanHistoricalSms(reactApplicationContext) }
            .onSuccess { promise.resolve(it) }
            .onFailure { promise.reject("dhan_sms_scan_error", it) }
    }

    /** Re-labels already-stored SMS transactions using the current parser (fixes titles
     *  captured before the sender-ID humanizing fix). Returns the number of rows changed. */
    @ReactMethod
    fun relabelSmsTransactions(promise: Promise) {
        runCatching { db.relabelSmsTransactions() }
            .onSuccess { promise.resolve(it) }
            .onFailure { promise.reject("dhan_db_error", it) }
    }
}

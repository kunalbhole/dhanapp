package com.dhan.app.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray
import org.json.JSONObject

/**
 * Plain android.database.sqlite persistence — deliberately not Room/KSP, to avoid adding
 * an AndroidX annotation-processor dependency chain to a project that already has enough
 * new moving parts (React Native's own native build). Shared by the native capture engine
 * (SmsReceiver, TxnNotificationListenerService — which must persist data even when the JS
 * runtime isn't running) and the DhanDbModule bridge that the RN UI reads/writes through.
 */
class DhanDb private constructor(context: Context) : SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                merchant TEXT NOT NULL,
                note TEXT,
                amount REAL NOT NULL,
                category TEXT NOT NULL,
                timestampMillis INTEGER NOT NULL,
                source TEXT NOT NULL,
                sourceApp TEXT,
                rawText TEXT,
                accountHint TEXT
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                category TEXT NOT NULL,
                monthKey TEXT NOT NULL,
                limitAmount REAL NOT NULL,
                UNIQUE(category, monthKey)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE bills (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                amount REAL NOT NULL,
                dueDateMillis INTEGER NOT NULL,
                status TEXT NOT NULL,
                repeatMonthly INTEGER NOT NULL DEFAULT 0,
                category TEXT NOT NULL DEFAULT 'bills'
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE friends (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                balance REAL NOT NULL DEFAULT 0
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE debt_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                friendId INTEGER NOT NULL,
                description TEXT NOT NULL,
                amount REAL NOT NULL,
                timestampMillis INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                targetAmount REAL NOT NULL,
                savedAmount REAL NOT NULL DEFAULT 0,
                targetDateMillis INTEGER
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE capture_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                source TEXT NOT NULL,
                sourceApp TEXT,
                rawText TEXT NOT NULL,
                timestampMillis INTEGER NOT NULL,
                matched INTEGER NOT NULL,
                createdTxnId INTEGER
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // No prior versions shipped yet.
    }

    fun insertTransaction(merchant: String, note: String?, amount: Double, category: String, timestampMillis: Long, source: String, sourceApp: String?, rawText: String?, accountHint: String?): Long {
        val values = ContentValues().apply {
            put("merchant", merchant)
            put("note", note)
            put("amount", amount)
            put("category", category)
            put("timestampMillis", timestampMillis)
            put("source", source)
            put("sourceApp", sourceApp)
            put("rawText", rawText)
            put("accountHint", accountHint)
        }
        return writableDatabase.insert("transactions", null, values)
    }

    fun deleteTransaction(id: Long) {
        writableDatabase.delete("transactions", "id = ?", arrayOf(id.toString()))
    }

    fun getTransactionsJson(): String {
        val cursor = readableDatabase.rawQuery("SELECT * FROM transactions ORDER BY timestampMillis DESC", null)
        val arr = JSONArray()
        cursor.use {
            while (it.moveToNext()) {
                arr.put(rowToJson(it))
            }
        }
        return arr.toString()
    }

    fun logCaptureEvent(source: String, sourceApp: String?, rawText: String, timestampMillis: Long, matched: Boolean, createdTxnId: Long?) {
        val values = ContentValues().apply {
            put("source", source)
            put("sourceApp", sourceApp)
            put("rawText", rawText)
            put("timestampMillis", timestampMillis)
            put("matched", if (matched) 1 else 0)
            if (createdTxnId != null) put("createdTxnId", createdTxnId) else putNull("createdTxnId")
        }
        writableDatabase.insert("capture_events", null, values)
    }

    fun getCaptureEventsJson(): String {
        val cursor = readableDatabase.rawQuery("SELECT * FROM capture_events ORDER BY timestampMillis DESC LIMIT 200", null)
        val arr = JSONArray()
        cursor.use {
            while (it.moveToNext()) {
                arr.put(rowToJson(it))
            }
        }
        return arr.toString()
    }

    fun upsertBudget(category: String, monthKey: String, limitAmount: Double) {
        writableDatabase.execSQL(
            "INSERT INTO budgets (category, monthKey, limitAmount) VALUES (?, ?, ?) " +
                "ON CONFLICT(category, monthKey) DO UPDATE SET limitAmount = excluded.limitAmount",
            arrayOf(category, monthKey, limitAmount),
        )
    }

    fun getBudgetsJson(monthKey: String): String {
        val cursor = readableDatabase.rawQuery("SELECT * FROM budgets WHERE monthKey = ?", arrayOf(monthKey))
        val arr = JSONArray()
        cursor.use {
            while (it.moveToNext()) {
                arr.put(rowToJson(it))
            }
        }
        return arr.toString()
    }

    fun insertBill(name: String, amount: Double, dueDateMillis: Long, status: String, repeatMonthly: Boolean, category: String): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("amount", amount)
            put("dueDateMillis", dueDateMillis)
            put("status", status)
            put("repeatMonthly", if (repeatMonthly) 1 else 0)
            put("category", category)
        }
        return writableDatabase.insert("bills", null, values)
    }

    fun setBillStatus(id: Long, status: String) {
        writableDatabase.execSQL("UPDATE bills SET status = ? WHERE id = ?", arrayOf(status, id.toString()))
    }

    fun getBillsJson(): String {
        val cursor = readableDatabase.rawQuery("SELECT * FROM bills ORDER BY dueDateMillis ASC", null)
        val arr = JSONArray()
        cursor.use {
            while (it.moveToNext()) {
                arr.put(rowToJson(it))
            }
        }
        return arr.toString()
    }

    fun getGoalsJson(): String {
        val cursor = readableDatabase.rawQuery("SELECT * FROM goals ORDER BY id ASC", null)
        val arr = JSONArray()
        cursor.use {
            while (it.moveToNext()) {
                arr.put(rowToJson(it))
            }
        }
        return arr.toString()
    }

    fun insertGoal(name: String, targetAmount: Double, savedAmount: Double): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("targetAmount", targetAmount)
            put("savedAmount", savedAmount)
        }
        return writableDatabase.insert("goals", null, values)
    }

    fun updateGoalSaved(id: Long, savedAmount: Double) {
        writableDatabase.execSQL("UPDATE goals SET savedAmount = ? WHERE id = ?", arrayOf(savedAmount.toString(), id.toString()))
    }

    fun getFriendsJson(): String {
        val cursor = readableDatabase.rawQuery("SELECT * FROM friends ORDER BY name ASC", null)
        val arr = JSONArray()
        cursor.use {
            while (it.moveToNext()) {
                arr.put(rowToJson(it))
            }
        }
        return arr.toString()
    }

    fun insertFriend(name: String, balance: Double): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("balance", balance)
        }
        return writableDatabase.insert("friends", null, values)
    }

    fun getDebtEntriesJson(): String {
        val cursor = readableDatabase.rawQuery("SELECT * FROM debt_entries ORDER BY timestampMillis ASC", null)
        val arr = JSONArray()
        cursor.use {
            while (it.moveToNext()) {
                arr.put(rowToJson(it))
            }
        }
        return arr.toString()
    }

    fun insertDebtEntry(friendId: Long, description: String, amount: Double, timestampMillis: Long): Long {
        val values = ContentValues().apply {
            put("friendId", friendId)
            put("description", description)
            put("amount", amount)
            put("timestampMillis", timestampMillis)
        }
        return writableDatabase.insert("debt_entries", null, values)
    }

    /** All budgets across every month, for backup export (getBudgetsJson is scoped to one
     *  monthKey for the Budget screen's own use). */
    fun getAllBudgetsJson(): String {
        val cursor = readableDatabase.rawQuery("SELECT * FROM budgets", null)
        val arr = JSONArray()
        cursor.use {
            while (it.moveToNext()) {
                arr.put(rowToJson(it))
            }
        }
        return arr.toString()
    }

    /**
     * Full local-data snapshot for Google Drive backup. Deliberately excludes
     * capture_events (a re-derivable activity log, not user financial data) and anything
     * that only lives in JS-side AsyncStorage (e.g. display name) — see BackupRepository.
     */
    fun exportAllJson(): String {
        val out = JSONObject()
        out.put("transactions", JSONArray(getTransactionsJson()))
        out.put("budgets", JSONArray(getAllBudgetsJson()))
        out.put("bills", JSONArray(getBillsJson()))
        out.put("goals", JSONArray(getGoalsJson()))
        out.put("friends", JSONArray(getFriendsJson()))
        out.put("debtEntries", JSONArray(getDebtEntriesJson()))
        return out.toString()
    }

    /**
     * Populates an (assumed empty) local database from a backup produced by [exportAllJson].
     * Row "id" fields are ignored on purpose — SQLite assigns fresh autoincrement ids — except
     * debtEntries.friendId, which is remapped from the backup's old friend id to the newly
     * inserted friend's id so debts still point at the right person. Runs as one transaction
     * so a failure partway through leaves the database untouched rather than half-restored.
     * Returns the number of transactions restored (used by onboarding to decide whether the
     * historical SMS scan should still run).
     */
    fun importAllJson(json: String): Int {
        val obj = JSONObject(json)
        val db = writableDatabase
        var restoredTxnCount = 0
        db.beginTransaction()
        try {
            val transactions = obj.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until transactions.length()) {
                val t = transactions.getJSONObject(i)
                insertTransaction(
                    merchant = t.getString("merchant"),
                    note = t.optStringOrNull("note"),
                    amount = t.getDouble("amount"),
                    category = t.getString("category"),
                    timestampMillis = t.getLong("timestampMillis"),
                    source = t.getString("source"),
                    sourceApp = t.optStringOrNull("sourceApp"),
                    rawText = t.optStringOrNull("rawText"),
                    accountHint = t.optStringOrNull("accountHint"),
                )
                restoredTxnCount++
            }

            val budgets = obj.optJSONArray("budgets") ?: JSONArray()
            for (i in 0 until budgets.length()) {
                val b = budgets.getJSONObject(i)
                upsertBudget(b.getString("category"), b.getString("monthKey"), b.getDouble("limitAmount"))
            }

            val bills = obj.optJSONArray("bills") ?: JSONArray()
            for (i in 0 until bills.length()) {
                val b = bills.getJSONObject(i)
                insertBill(
                    name = b.getString("name"),
                    amount = b.getDouble("amount"),
                    dueDateMillis = b.getLong("dueDateMillis"),
                    status = b.getString("status"),
                    repeatMonthly = b.getInt("repeatMonthly") != 0,
                    category = b.getString("category"),
                )
            }

            val goals = obj.optJSONArray("goals") ?: JSONArray()
            for (i in 0 until goals.length()) {
                val g = goals.getJSONObject(i)
                insertGoal(g.getString("name"), g.getDouble("targetAmount"), g.getDouble("savedAmount"))
            }

            val friends = obj.optJSONArray("friends") ?: JSONArray()
            val friendIdRemap = HashMap<Long, Long>()
            for (i in 0 until friends.length()) {
                val f = friends.getJSONObject(i)
                val newId = insertFriend(f.getString("name"), f.getDouble("balance"))
                friendIdRemap[f.getLong("id")] = newId
            }

            val debtEntries = obj.optJSONArray("debtEntries") ?: JSONArray()
            for (i in 0 until debtEntries.length()) {
                val d = debtEntries.getJSONObject(i)
                val oldFriendId = d.getLong("friendId")
                val newFriendId = friendIdRemap[oldFriendId] ?: continue
                insertDebtEntry(newFriendId, d.getString("description"), d.getDouble("amount"), d.getLong("timestampMillis"))
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return restoredTxnCount
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (isNull(key) || !has(key)) null else getString(key)

    private fun rowToJson(cursor: android.database.Cursor): JSONObject {
        val obj = JSONObject()
        for (i in 0 until cursor.columnCount) {
            val name = cursor.getColumnName(i)
            when (cursor.getType(i)) {
                android.database.Cursor.FIELD_TYPE_NULL -> obj.put(name, JSONObject.NULL)
                android.database.Cursor.FIELD_TYPE_INTEGER -> obj.put(name, cursor.getLong(i))
                android.database.Cursor.FIELD_TYPE_FLOAT -> obj.put(name, cursor.getDouble(i))
                else -> obj.put(name, cursor.getString(i))
            }
        }
        return obj
    }

    companion object {
        private const val DB_NAME = "dhan.db"
        private const val DB_VERSION = 1

        @Volatile private var instance: DhanDb? = null

        fun get(context: Context): DhanDb =
            instance ?: synchronized(this) {
                instance ?: DhanDb(context).also { instance = it }
            }
    }
}

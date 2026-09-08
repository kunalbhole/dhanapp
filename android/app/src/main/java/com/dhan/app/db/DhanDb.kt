package com.dhan.app.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.dhan.app.capture.parser.TransactionParser
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
                accountHint TEXT,
                dedupKey TEXT
            )
            """.trimIndent(),
        )
        // Exact-match dedup guard for captured (SMS/notification) rows only — dedupKey is
        // left NULL for manual entries and restored backups (see insertCapturedTransaction),
        // and the partial WHERE clause means NULL never participates in the uniqueness check.
        db.execSQL("CREATE UNIQUE INDEX idx_transactions_dedup ON transactions(dedupKey) WHERE dedupKey IS NOT NULL")
        db.execSQL(
            """
            CREATE TABLE budget_defs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                frameworkKey TEXT NOT NULL,
                customFrameworkJson TEXT
            )
            """.trimIndent(),
        )
        db.execSQL("INSERT INTO budget_defs (id, name, type, frameworkKey) VALUES (1, 'Personal', 'PERSONAL', '50-30-20')")
        db.execSQL(
            """
            CREATE TABLE budgets (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                budgetDefId INTEGER NOT NULL DEFAULT 1,
                category TEXT NOT NULL,
                monthKey TEXT NOT NULL,
                limitAmount REAL NOT NULL,
                UNIQUE(budgetDefId, category, monthKey)
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
        if (oldVersion < 2) {
            // Introduces multiple named budgets (Personal + Project) each following a
            // budgeting framework, instead of one flat set of per-category caps. Every
            // budget row that already existed belonged to the single implicit budget, so
            // it's migrated onto a newly-seeded "Personal" budget_defs row (id 1) — no data
            // is lost, including the "__total__" sentinel row used for the total-budget
            // override, which carries over unchanged under the same reserved category key.
            db.execSQL(
                """
                CREATE TABLE budget_defs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    type TEXT NOT NULL,
                    frameworkKey TEXT NOT NULL,
                    customFrameworkJson TEXT
                )
                """.trimIndent(),
            )
            db.execSQL("INSERT INTO budget_defs (id, name, type, frameworkKey) VALUES (1, 'Personal', 'PERSONAL', '50-30-20')")

            db.execSQL("ALTER TABLE budgets RENAME TO budgets_old")
            db.execSQL(
                """
                CREATE TABLE budgets (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    budgetDefId INTEGER NOT NULL DEFAULT 1,
                    category TEXT NOT NULL,
                    monthKey TEXT NOT NULL,
                    limitAmount REAL NOT NULL,
                    UNIQUE(budgetDefId, category, monthKey)
                )
                """.trimIndent(),
            )
            db.execSQL(
                "INSERT INTO budgets (id, budgetDefId, category, monthKey, limitAmount) " +
                    "SELECT id, 1, category, monthKey, limitAmount FROM budgets_old",
            )
            db.execSQL("DROP TABLE budgets_old")
        }
        if (oldVersion < 3) {
            // Data-integrity fix: TransactionParser's amount regex used to silently truncate
            // comma-less amounts of 4+ digits by one digit (e.g. an SMS for ₹5,000 was
            // captured and stored as ₹500) — see the `*` -> `+` fix on its comma-group.
            // Every transaction that already has its original SMS/notification text stored
            // gets re-parsed with the corrected regex and its amount overwritten wherever it
            // now differs; sign (debit/credit) was never affected, only magnitude, so nothing
            // else about the row needs to change.
            val cursor = db.rawQuery(
                "SELECT id, amount, sourceApp, rawText FROM transactions WHERE rawText IS NOT NULL",
                null,
            )
            cursor.use {
                val idIdx = it.getColumnIndexOrThrow("id")
                val amountIdx = it.getColumnIndexOrThrow("amount")
                val sourceAppIdx = it.getColumnIndexOrThrow("sourceApp")
                val rawTextIdx = it.getColumnIndexOrThrow("rawText")
                while (it.moveToNext()) {
                    val rawText = it.getString(rawTextIdx) ?: continue
                    val sourceApp = it.getString(sourceAppIdx)
                    val storedAmount = it.getDouble(amountIdx)
                    val parsed = TransactionParser.parse(rawText, sourceApp) ?: continue
                    if (kotlin.math.abs(parsed.amountRupees - storedAmount) < 0.005) continue
                    db.execSQL(
                        "UPDATE transactions SET amount = ? WHERE id = ?",
                        arrayOf(parsed.amountRupees.toString(), it.getLong(idIdx).toString()),
                    )
                }
            }
        }
        if (oldVersion < 4) {
            // Data-integrity fix #2: transactions had no dedup mechanism at all, so
            // re-processing the same source event (re-running the historical SMS scan,
            // reinstalling, or SMS + notification capture both catching the same bank
            // event) silently inserted duplicate rows. Two layers, going forward:
            // (1) an exact-match `dedupKey` (hash of source|sourceApp|timestampMillis|
            // rawText) enforced by a DB-level unique index — guards same-source
            // re-delivery even across a race, since a SELECT-then-INSERT check alone
            // can't be atomic; (2) a fuzzy cross-source check at insert time (same
            // amount, a *different* source, within a few minutes) — see
            // insertCapturedTransaction/findCrossSourceDuplicateId — since SMS body text
            // and notification text/sourceApp for the same real-world event are never
            // byte-identical, so an exact key can never catch that case. Existing rows
            // keep dedupKey = NULL; only newly-captured rows get one (manual entries and
            // restored backups stay exempt too, via the partial index's WHERE clause).
            db.execSQL("ALTER TABLE transactions ADD COLUMN dedupKey TEXT")
            db.execSQL("CREATE UNIQUE INDEX idx_transactions_dedup ON transactions(dedupKey) WHERE dedupKey IS NOT NULL")

            // One-time cleanup of exact duplicates already sitting in the database: same
            // merchant, amount, timestampMillis, and source, keep only the earliest
            // (lowest id) row per group. Deliberately exact-match only — a destructive
            // DELETE run unattended needs a false-positive-free match, and cross-source
            // near-duplicates already in the database (SMS vs. notification catching the
            // same real event under different timestamps/text) aren't safely identifiable
            // this way; see PROJECT_STATUS.md for why that's left for the user to spot
            // rather than auto-merged.
            db.execSQL(
                """
                DELETE FROM transactions
                WHERE id NOT IN (
                    SELECT MIN(id) FROM transactions
                    GROUP BY merchant, amount, timestampMillis, source
                )
                """.trimIndent(),
            )
        }
    }

    /**
     * SHA-256 hex digest of the fields that identify "this is the same underlying
     * capture event" — two genuinely different transactions essentially never share all
     * of source, sourceApp, timestampMillis, and the exact rawText. Hashed (rather than
     * stored raw) to keep the indexed column a fixed, short size regardless of message
     * length.
     */
    private fun dedupKeyFor(source: String, sourceApp: String?, timestampMillis: Long, rawText: String): String {
        val raw = "$source|${sourceApp.orEmpty()}|$timestampMillis|$rawText"
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(raw.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Fuzzy cross-source duplicate check: is there already a transaction of the same
     * amount, from a *different* source, within [windowMs] of this timestamp? SMS and
     * notification capture can both fire for the same real-world bank event (see
     * PROJECT_STATUS.md — several tracked notification packages are the same banks whose
     * SMS alerts are parsed), but their rawText/sourceApp are never byte-identical and
     * their timestamps (SMS delivery vs. notification post time) can differ by a couple
     * of minutes — so this can't be an exact-key match. Restricted to a *different*
     * source than the one being inserted so two genuinely-distinct same-source
     * transactions of equal amount within the window (e.g. two SMS-captured payments a
     * minute apart) are never affected — same-source exact duplicates are already fully
     * handled by the dedupKey unique index.
     */
    private fun findCrossSourceDuplicateId(amount: Double, timestampMillis: Long, source: String, windowMs: Long = 120_000L): Long? {
        val cursor = readableDatabase.rawQuery(
            "SELECT id FROM transactions WHERE amount = ? AND source != ? AND timestampMillis BETWEEN ? AND ? LIMIT 1",
            arrayOf(amount.toString(), source, (timestampMillis - windowMs).toString(), (timestampMillis + windowMs).toString()),
        )
        return cursor.use { if (it.moveToFirst()) it.getLong(0) else null }
    }

    /**
     * Insert path for SMS/notification-captured transactions only (see CaptureIngest) —
     * distinct from [insertTransaction], which manual entry (AddTransactionScreen) and
     * backup restore use and which deliberately never sets dedupKey, so those rows are
     * naturally exempt from the dedup machinery below. Returns null (no row inserted)
     * when the event is judged a duplicate by either the fuzzy cross-source check or the
     * exact dedupKey unique index, rather than a new transaction id.
     */
    fun insertCapturedTransaction(merchant: String, amount: Double, category: String, timestampMillis: Long, source: String, sourceApp: String?, rawText: String, accountHint: String?): Long? {
        if (findCrossSourceDuplicateId(amount, timestampMillis, source) != null) return null
        val values = ContentValues().apply {
            put("merchant", merchant)
            put("amount", amount)
            put("category", category)
            put("timestampMillis", timestampMillis)
            put("source", source)
            put("sourceApp", sourceApp)
            put("rawText", rawText)
            put("accountHint", accountHint)
            put("dedupKey", dedupKeyFor(source, sourceApp, timestampMillis, rawText))
        }
        val id = writableDatabase.insertWithOnConflict("transactions", null, values, SQLiteDatabase.CONFLICT_IGNORE)
        return if (id == -1L) null else id
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

    /**
     * One-time cleanup for transactions captured before the sender-ID humanizing fix in
     * TransactionParser (e.g. merchant stored as the raw "AX-AXISBK-S" SMS header instead of
     * "Axis Bank"). Re-runs the parser against each SMS-sourced transaction's already-stored
     * rawText/sourceApp and updates merchant/category in place wherever the result differs.
     * Only ever improves existing rows — never inserts or deletes — so it's safe to run
     * repeatedly. Returns the number of rows actually changed.
     */
    fun relabelSmsTransactions(): Int {
        val cursor = readableDatabase.rawQuery(
            "SELECT id, merchant, sourceApp, rawText FROM transactions WHERE source = 'SMS' AND rawText IS NOT NULL",
            null,
        )
        var updatedCount = 0
        val db = writableDatabase
        db.beginTransaction()
        try {
            cursor.use {
                val idIdx = it.getColumnIndexOrThrow("id")
                val merchantIdx = it.getColumnIndexOrThrow("merchant")
                val sourceAppIdx = it.getColumnIndexOrThrow("sourceApp")
                val rawTextIdx = it.getColumnIndexOrThrow("rawText")
                while (it.moveToNext()) {
                    val rawText = it.getString(rawTextIdx) ?: continue
                    val sourceApp = it.getString(sourceAppIdx)
                    val storedMerchant = it.getString(merchantIdx)
                    val parsed = TransactionParser.parse(rawText, sourceApp) ?: continue
                    if (parsed.merchant == storedMerchant) continue
                    val id = it.getLong(idIdx)
                    db.execSQL(
                        "UPDATE transactions SET merchant = ?, category = ?, accountHint = ? WHERE id = ?",
                        arrayOf(parsed.merchant, parsed.category, parsed.accountHint, id.toString()),
                    )
                    updatedCount++
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return updatedCount
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

    fun upsertBudget(budgetDefId: Long, category: String, monthKey: String, limitAmount: Double) {
        writableDatabase.execSQL(
            "INSERT INTO budgets (budgetDefId, category, monthKey, limitAmount) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(budgetDefId, category, monthKey) DO UPDATE SET limitAmount = excluded.limitAmount",
            arrayOf(budgetDefId.toString(), category, monthKey, limitAmount),
        )
    }

    fun getBudgetsJson(budgetDefId: Long, monthKey: String): String {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM budgets WHERE budgetDefId = ? AND monthKey = ?",
            arrayOf(budgetDefId.toString(), monthKey),
        )
        val arr = JSONArray()
        cursor.use {
            while (it.moveToNext()) {
                arr.put(rowToJson(it))
            }
        }
        return arr.toString()
    }

    /** Clears a budget's per-category allocations for one month — used when switching
     *  frameworks, since the old framework's category caps don't necessarily make sense
     *  under the new one. Deliberately keeps the "__total__" sentinel row (the overall
     *  budget amount survives a framework change; only its breakdown resets) — the literal
     *  must match the reserved key on the JS side (src/screens/BudgetScreen.tsx). */
    fun clearBudgetCategories(budgetDefId: Long, monthKey: String) {
        writableDatabase.delete(
            "budgets",
            "budgetDefId = ? AND monthKey = ? AND category != '__total__'",
            arrayOf(budgetDefId.toString(), monthKey),
        )
    }

    fun getBudgetDefsJson(): String {
        val cursor = readableDatabase.rawQuery(
            "SELECT * FROM budget_defs ORDER BY CASE WHEN type = 'PERSONAL' THEN 0 ELSE 1 END, id ASC",
            null,
        )
        val arr = JSONArray()
        cursor.use {
            while (it.moveToNext()) {
                arr.put(rowToJson(it))
            }
        }
        return arr.toString()
    }

    fun insertBudgetDef(name: String, type: String, frameworkKey: String, customFrameworkJson: String?): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("type", type)
            put("frameworkKey", frameworkKey)
            put("customFrameworkJson", customFrameworkJson)
        }
        return writableDatabase.insert("budget_defs", null, values)
    }

    fun updateBudgetDefFramework(id: Long, frameworkKey: String, customFrameworkJson: String?) {
        val values = ContentValues().apply {
            put("frameworkKey", frameworkKey)
            put("customFrameworkJson", customFrameworkJson)
        }
        writableDatabase.update("budget_defs", values, "id = ?", arrayOf(id.toString()))
    }

    /** No-op (rather than an error) for the Personal budget (always id 1) — the UI never
     *  offers Delete for it, but this is a second, native-side backstop against losing it. */
    fun deleteBudgetDef(id: Long) {
        if (id == 1L) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("budgets", "budgetDefId = ?", arrayOf(id.toString()))
            db.delete("budget_defs", "id = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
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
        out.put("budgetDefs", JSONArray(getBudgetDefsJson()))
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

            // budget_defs before budgets, since budgets.budgetDefId references it. Personal
            // (id 1) already exists on any DB reaching this point (seeded by onCreate/
            // onUpgrade) so it's never re-inserted — only remapped, on the safe assumption
            // that Personal is always id 1 on both ends. Project budgets get fresh ids.
            val budgetDefIdRemap = HashMap<Long, Long>()
            val budgetDefs = obj.optJSONArray("budgetDefs") ?: JSONArray()
            for (i in 0 until budgetDefs.length()) {
                val bd = budgetDefs.getJSONObject(i)
                val oldId = bd.getLong("id")
                if (bd.getString("type") == "PERSONAL") {
                    budgetDefIdRemap[oldId] = 1L
                } else {
                    val newId = insertBudgetDef(
                        bd.getString("name"),
                        bd.getString("type"),
                        bd.getString("frameworkKey"),
                        bd.optStringOrNull("customFrameworkJson"),
                    )
                    budgetDefIdRemap[oldId] = newId
                }
            }

            val budgets = obj.optJSONArray("budgets") ?: JSONArray()
            for (i in 0 until budgets.length()) {
                val b = budgets.getJSONObject(i)
                val oldBudgetDefId = if (b.has("budgetDefId")) b.getLong("budgetDefId") else 1L
                val budgetDefId = budgetDefIdRemap[oldBudgetDefId] ?: 1L
                upsertBudget(budgetDefId, b.getString("category"), b.getString("monthKey"), b.getDouble("limitAmount"))
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
        private const val DB_VERSION = 4

        @Volatile private var instance: DhanDb? = null

        fun get(context: Context): DhanDb =
            instance ?: synchronized(this) {
                instance ?: DhanDb(context).also { instance = it }
            }
    }
}

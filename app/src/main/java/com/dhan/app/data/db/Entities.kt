package com.dhan.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TxnSource { MANUAL, SMS, NOTIFICATION }

/**
 * A single money-in or money-out event. [amountRupees] is signed: positive = income
 * (matches TxnRow / HomeScreen's `amount > 0` = incoming convention from the source design).
 */
@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val note: String? = null,
    val amountRupees: Double,
    val category: String,
    val timestampMillis: Long,
    val source: TxnSource,
    val sourceApp: String? = null,
    val rawText: String? = null,
    val accountHint: String? = null,
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val monthKey: String, // "yyyy-MM"
    val limitRupees: Double,
)

enum class BillStatus { UPCOMING, DUE_SOON, OVERDUE, PAID }

@Entity(tableName = "bills")
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountRupees: Double,
    val dueDateMillis: Long,
    val status: BillStatus,
    val repeatMonthly: Boolean = false,
    val category: String = "bills",
)

@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Positive = they owe the user; negative = the user owes them. */
    val balanceRupees: Double,
)

@Entity(
    tableName = "debt_entries",
    foreignKeys = [
        ForeignKey(
            entity = FriendEntity::class,
            parentColumns = ["id"],
            childColumns = ["friendId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("friendId")],
)
data class DebtEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val friendId: Long,
    val description: String,
    val amountRupees: Double,
    val timestampMillis: Long,
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val targetRupees: Double,
    val savedRupees: Double,
    val targetDateMillis: Long? = null,
)

/** Audit trail of every SMS/notification the capture engine looked at, matched or not —
 *  lets Settings show "N messages scanned, M turned into transactions" and lets a user
 *  spot a missed message without re-granting permissions. */
@Entity(tableName = "capture_events")
data class CaptureEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: TxnSource,
    val sourceApp: String? = null,
    val rawText: String,
    val timestampMillis: Long,
    val matched: Boolean,
    val createdTxnId: Long? = null,
)

package com.dhan.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(txn: TransactionEntity): Long

    @Update
    suspend fun update(txn: TransactionEntity)

    @Delete
    suspend fun delete(txn: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestampMillis DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestampMillis >= :fromMillis AND timestampMillis < :toMillis ORDER BY timestampMillis DESC")
    fun observeInRange(fromMillis: Long, toMillis: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT COALESCE(SUM(amountRupees), 0) FROM transactions WHERE amountRupees > 0 AND timestampMillis >= :fromMillis AND timestampMillis < :toMillis")
    fun observeIncomeInRange(fromMillis: Long, toMillis: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(-amountRupees), 0) FROM transactions WHERE amountRupees < 0 AND timestampMillis >= :fromMillis AND timestampMillis < :toMillis")
    fun observeExpenseInRange(fromMillis: Long, toMillis: Long): Flow<Double>

    @Query("SELECT category, COALESCE(SUM(-amountRupees), 0) as spent FROM transactions WHERE amountRupees < 0 AND timestampMillis >= :fromMillis AND timestampMillis < :toMillis GROUP BY category")
    fun observeCategorySpendInRange(fromMillis: Long, toMillis: Long): Flow<List<CategorySpend>>
}

data class CategorySpend(val category: String, val spent: Double)

@Dao
interface BudgetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(budget: BudgetEntity)

    @Query("SELECT * FROM budgets WHERE monthKey = :monthKey")
    fun observeForMonth(monthKey: String): Flow<List<BudgetEntity>>

    @Delete
    suspend fun delete(budget: BudgetEntity)
}

@Dao
interface BillDao {
    @Insert
    suspend fun insert(bill: BillEntity): Long

    @Update
    suspend fun update(bill: BillEntity)

    @Delete
    suspend fun delete(bill: BillEntity)

    @Query("SELECT * FROM bills ORDER BY dueDateMillis ASC")
    fun observeAll(): Flow<List<BillEntity>>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getById(id: Long): BillEntity?

    @Query("UPDATE bills SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: BillStatus)
}

@Dao
interface FriendDao {
    @Insert
    suspend fun insert(friend: FriendEntity): Long

    @Update
    suspend fun update(friend: FriendEntity)

    @Query("SELECT * FROM friends ORDER BY name ASC")
    fun observeAll(): Flow<List<FriendEntity>>

    @Query("SELECT * FROM friends WHERE id = :id")
    suspend fun getById(id: Long): FriendEntity?

    @Insert
    suspend fun insertEntry(entry: DebtEntryEntity): Long

    @Query("SELECT * FROM debt_entries WHERE friendId = :friendId ORDER BY timestampMillis DESC")
    fun observeEntries(friendId: Long): Flow<List<DebtEntryEntity>>
}

@Dao
interface GoalDao {
    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("SELECT * FROM goals ORDER BY id ASC")
    fun observeAll(): Flow<List<GoalEntity>>
}

@Dao
interface CaptureEventDao {
    @Insert
    suspend fun insert(event: CaptureEventEntity): Long

    @Query("SELECT * FROM capture_events ORDER BY timestampMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int = 200): Flow<List<CaptureEventEntity>>

    @Query("SELECT COUNT(*) FROM capture_events")
    fun observeScannedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM capture_events WHERE matched = 1")
    fun observeMatchedCount(): Flow<Int>
}

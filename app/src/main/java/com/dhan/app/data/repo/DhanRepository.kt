package com.dhan.app.data.repo

import com.dhan.app.data.db.AppDatabase
import com.dhan.app.data.db.BillDao
import com.dhan.app.data.db.BillEntity
import com.dhan.app.data.db.BudgetDao
import com.dhan.app.data.db.BudgetEntity
import com.dhan.app.data.db.CaptureEventDao
import com.dhan.app.data.db.CaptureEventEntity
import com.dhan.app.data.db.FriendDao
import com.dhan.app.data.db.FriendEntity
import com.dhan.app.data.db.GoalDao
import com.dhan.app.data.db.GoalEntity
import com.dhan.app.data.db.TransactionDao
import com.dhan.app.data.db.TransactionEntity

/** Thin facade over the DAOs — screens/ViewModels depend on this, not on Room directly. */
class DhanRepository(db: AppDatabase) {
    val transactions: TransactionDao = db.transactionDao()
    val budgets: BudgetDao = db.budgetDao()
    val bills: BillDao = db.billDao()
    val friends: FriendDao = db.friendDao()
    val goals: GoalDao = db.goalDao()
    val captureEvents: CaptureEventDao = db.captureEventDao()

    suspend fun addTransaction(txn: TransactionEntity): Long = transactions.insert(txn)
    suspend fun addBill(bill: BillEntity): Long = bills.insert(bill)
    suspend fun addFriend(friend: FriendEntity): Long = friends.insert(friend)
    suspend fun addGoal(goal: GoalEntity): Long = goals.insert(goal)
    suspend fun setBudget(budget: BudgetEntity) = budgets.upsert(budget)
    suspend fun logCaptureEvent(event: CaptureEventEntity): Long = captureEvents.insert(event)
}

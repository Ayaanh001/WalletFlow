package com.hussain.walletflow.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(transaction: Transaction): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<Transaction>)

    @Update
    suspend fun update(transaction: Transaction): Int

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query(
        "SELECT * FROM transactions WHERE isAddedToMonthly = 0 AND originalSms != '' ORDER BY date DESC"
    )
    fun getPassbookTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE isAddedToMonthly = 1 ORDER BY date DESC")
    fun getMonthlyTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("UPDATE transactions SET isAddedToMonthly = 1 WHERE id = :id")
    suspend fun markAsAddedToMonthly(id: Long)

    @Query("UPDATE transactions SET isAddedToMonthly = 1 WHERE id IN (:ids)")
    suspend fun markMultipleAsAddedToMonthly(ids: List<Long>)

    @Query("DELETE FROM transactions WHERE isAddedToMonthly = 0 AND originalSms != ''")
    suspend fun clearPassbookTransactions()

    @Query(
        "SELECT DISTINCT bankName FROM transactions WHERE isAddedToMonthly = 0 ORDER BY bankName"
    )
    suspend fun getAllBanks(): List<String>

    @Query("UPDATE transactions SET category = :category WHERE id IN (:ids)")
    suspend fun updateCategoryByIds(ids: List<Long>, category: String)

    @Query("UPDATE transactions SET paymentMethod = :paymentMethod WHERE id IN (:ids)")
    suspend fun updatePaymentMethodByIds(ids: List<Long>, paymentMethod: String)

    @Query("UPDATE transactions SET date = :date WHERE id IN (:ids)")
    suspend fun updateDateByIds(ids: List<Long>, date: Long)

    // AUTO-SCAN dedup: skip SMS already stored ANYWHERE (passbook + monthly).
    // Prevents duplicates when SmsReceiver or the initial scan runs automatically.
    @Query("SELECT originalSms FROM transactions WHERE originalSms != ''")
    suspend fun getAllExistingSmsTexts(): List<String>

    // MANUAL REFRESH dedup: skip SMS that are currently in passbook only.
    // SMS that were moved to monthly are NOT excluded, so tapping Refresh
    // brings them back as fresh passbook rows — the user explicitly asked to see them.
    @Query("SELECT originalSms FROM transactions WHERE originalSms != '' AND isAddedToMonthly = 0")
    suspend fun getPassbookSmsTexts(): List<String>

    @Query(
        """SELECT COALESCE(SUM(amount), 0.0) FROM transactions
           WHERE isAddedToMonthly = 1 AND type = 'INCOME'
           AND date >= :startOfMonth AND date <= :endOfMonth"""
    )
    suspend fun getTotalIncomeForMonth(startOfMonth: Long, endOfMonth: Long): Double

    @Query(
        """SELECT COALESCE(SUM(amount), 0.0) FROM transactions
           WHERE isAddedToMonthly = 1 AND type = 'EXPENSE'
           AND date >= :startOfMonth AND date <= :endOfMonth"""
    )
    suspend fun getTotalExpenseForMonth(startOfMonth: Long, endOfMonth: Long): Double
}
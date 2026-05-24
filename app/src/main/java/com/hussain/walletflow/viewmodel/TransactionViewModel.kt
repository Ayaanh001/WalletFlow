package com.hussain.walletflow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hussain.walletflow.data.Transaction
import com.hussain.walletflow.data.TransactionDatabase
import com.hussain.walletflow.data.TransactionType
import com.hussain.walletflow.utils.ParsedTransaction
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = TransactionDatabase.getDatabase(application).transactionDao()
        val passbookTransactions: StateFlow<List<Transaction>> =
        dao.getPassbookTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val monthlyTransactions: StateFlow<List<Transaction>> =
        dao.getMonthlyTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    val allTransactions: StateFlow<List<Transaction>> =
        dao.getAllTransactions()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    // ── Initial-scan gate ────────────────────────────────────────────────────
    // Lives in the ViewModel so it survives recomposition and tab switches.
    // PassbookScreen checks this before triggering the auto-scan so the scan
    // truly only runs once per app session — not on every tab switch.
    private val _initialScanDone = MutableStateFlow(false)
    val initialScanDone: StateFlow<Boolean> = _initialScanDone.asStateFlow()

    fun markInitialScanDone() {
        _initialScanDone.value = true
    }

    // ── Import pipeline ──────────────────────────────────────────────────────
    private val _pendingImport = MutableStateFlow<List<ParsedTransaction>>(emptyList())
    val pendingImport: StateFlow<List<ParsedTransaction>> = _pendingImport.asStateFlow()

    fun setPendingImport(transactions: List<ParsedTransaction>) {
        _pendingImport.value = transactions
    }

    fun clearPendingImport() {
        _pendingImport.value = emptyList()
    }

    // ── Monthly stats — derived from the shared StateFlow ────────────────────
    fun getMonthlyStats(startOfMonth: Long, endOfMonth: Long) =
        monthlyTransactions.map { list ->
            var income = 0.0
            var expense = 0.0
            for (tx in list) {
                if (tx.date !in startOfMonth..endOfMonth) continue
                if (tx.type == TransactionType.INCOME) income += tx.amount
                else expense += tx.amount
            }
            income to expense
        }

    // ── CRUD ─────────────────────────────────────────────────────────────────
    fun insertTransaction(transaction: Transaction) {
        viewModelScope.launch { dao.insert(transaction) }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch { dao.update(transaction) }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch { dao.delete(transaction) }
    }

    fun deleteTransactionsByIds(ids: List<Long>) {
        viewModelScope.launch { dao.deleteByIds(ids) }
    }

    fun updateCategoryByIds(ids: List<Long>, category: String) {
        viewModelScope.launch { dao.updateCategoryByIds(ids, category) }
    }

    fun updatePaymentMethodByIds(ids: List<Long>, paymentMethod: String) {
        viewModelScope.launch { dao.updatePaymentMethodByIds(ids, paymentMethod) }
    }

    fun updateDateByIds(ids: List<Long>, date: Long) {
        viewModelScope.launch { dao.updateDateByIds(ids, date) }
    }

    // ── Passbook → Monthly ───────────────────────────────────────────────────
    fun addToMonthly(transactionId: Long) {
        viewModelScope.launch { dao.markAsAddedToMonthly(transactionId) }
    }

    fun addToMonthlyKeepOriginal(transactionId: Long) {
        viewModelScope.launch {
            val txn = dao.getTransactionById(transactionId) ?: return@launch
            dao.insert(
                txn.copy(
                    id = 0,
                    isAddedToMonthly = true,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun addMultipleToMonthly(ids: List<Long>) {
        viewModelScope.launch { dao.markMultipleAsAddedToMonthly(ids) }
    }

    fun addMultipleToMonthlyKeepOriginals(ids: List<Long>) {
        viewModelScope.launch {
            val copies = ids.mapNotNull { id ->
                dao.getTransactionById(id)?.copy(
                    id = 0,
                    isAddedToMonthly = true,
                    createdAt = System.currentTimeMillis()
                )
            }
            if (copies.isNotEmpty()) dao.insertAll(copies)
        }
    }

    fun copyTransactionsToMonth(
        transactions: List<Transaction>,
        targetYear: Int,
        targetMonth: Int
    ) {
        viewModelScope.launch {
            val copies = transactions.map { txn ->
                val origCal = Calendar.getInstance().apply { timeInMillis = txn.date }
                val targetCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, targetYear)
                    set(Calendar.MONTH, targetMonth)
                    set(
                        Calendar.DAY_OF_MONTH,
                        origCal.get(Calendar.DAY_OF_MONTH)
                            .coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH))
                    )
                    set(Calendar.HOUR_OF_DAY, origCal.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, origCal.get(Calendar.MINUTE))
                    set(Calendar.SECOND, origCal.get(Calendar.SECOND))
                }
                txn.copy(
                    id = 0,
                    date = targetCal.timeInMillis,
                    isAddedToMonthly = true,
                    createdAt = System.currentTimeMillis()
                )
            }
            dao.insertAll(copies)
        }
    }

    suspend fun getTransactionById(id: Long): Transaction? = dao.getTransactionById(id)

    suspend fun getAllBanks(): List<String> = dao.getAllBanks()

    // ── Import ───────────────────────────────────────────────────────────────
    fun importParsedTransactions(
        transactions: List<ParsedTransaction>,
        targetYear: Int,
        targetMonth: Int
    ) {
        viewModelScope.launch {
            val dayGroups = mutableMapOf<String, MutableList<ParsedTransaction>>()
            val dayKey = { parsed: ParsedTransaction ->
                val cal = Calendar.getInstance().apply { timeInMillis = parsed.date }
                "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
            }
            transactions.forEach { p -> dayGroups.getOrPut(dayKey(p)) { mutableListOf() }.add(p) }

            val entities = mutableListOf<Transaction>()
            for ((_, group) in dayGroups) {
                group.forEachIndexed { index, parsed ->
                    val origCal = Calendar.getInstance().apply { timeInMillis = parsed.date }
                    val targetCal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, targetYear)
                        set(Calendar.MONTH, targetMonth)
                        set(
                            Calendar.DAY_OF_MONTH,
                            origCal.get(Calendar.DAY_OF_MONTH)
                                .coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH))
                        )
                        set(Calendar.HOUR_OF_DAY, origCal.get(Calendar.HOUR_OF_DAY))
                        set(Calendar.MINUTE, origCal.get(Calendar.MINUTE))
                        set(Calendar.SECOND, origCal.get(Calendar.SECOND) + index)
                        set(Calendar.MILLISECOND, 0)
                    }
                    entities.add(
                        Transaction(
                            id = 0,
                            date = targetCal.timeInMillis,
                            amount = parsed.amount,
                            type = parsed.type,
                            category = parsed.category.ifBlank {
                                if (parsed.type == TransactionType.INCOME) "Other Income"
                                else "Other Expense"
                            },
                            bankName = "Imported",
                            accountLastFour = "",
                            remark = parsed.narration,
                            originalSms = "",
                            paymentMethod = parsed.paymentMethod,
                            isAddedToMonthly = true,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            }
            if (entities.isNotEmpty()) dao.insertAll(entities)
        }
    }
}
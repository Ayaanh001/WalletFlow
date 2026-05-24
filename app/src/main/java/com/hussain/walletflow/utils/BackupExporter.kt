package com.hussain.walletflow.utils

import android.content.Context
import android.net.Uri
import com.hussain.walletflow.data.CustomItem
import com.hussain.walletflow.data.CustomItemsRepository
import com.hussain.walletflow.data.Transaction
import com.hussain.walletflow.data.TransactionDatabase
import com.hussain.walletflow.data.TransactionType
import kotlinx.coroutines.flow.first
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.*

object BackupExporter {

    // ── Header that identifies a WalletFlow backup (must stay in sync) ────────
    const val BACKUP_HEADER =
        "ID,Date,Type,Amount,Category,Payment Method,Remark,Bank,Account Last 4,Instrument,Added to Monthly,Created At"

    /**
     * Prepares the CSV content and a suggested filename for backup.
     * Returns a Pair of (fileName, csvContent) or null if no transactions exist.
     */
    suspend fun prepareBackupData(context: Context): Pair<String, String>? {
        val dao = TransactionDatabase.getDatabase(context).transactionDao()
        val transactions = dao.getAllTransactions().first()
            .filter { it.isAddedToMonthly }
            .sortedBy { it.date } // oldest first → re-import preserves order

        if (transactions.isEmpty()) return null

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fileName = "walletflow_backup_${
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        }.csv"

        val csv = buildString {
            appendLine(BACKUP_HEADER)
            transactions.forEach { tx ->
                val type = when (tx.type) {
                    TransactionType.INCOME  -> "Income"
                    TransactionType.EXPENSE -> "Expense"
                    else                    -> "Unknown"
                }
                append(tx.id);               append(",")
                append(sdf.format(Date(tx.date))); append(",")
                append(type);                append(",")
                append(tx.amount);           append(",")
                append(escapeCsv(tx.category));       append(",")
                append(escapeCsv(tx.paymentMethod));  append(",")
                append(escapeCsv(tx.remark));         append(",")
                append(escapeCsv(tx.bankName));       append(",")
                append(escapeCsv(tx.accountLastFour));append(",")
                append(escapeCsv(tx.instrumentType)); append(",")
                append(tx.isAddedToMonthly);  append(",")
                appendLine(tx.createdAt)
            }

            // ── Also export Custom Categories & Payment Methods ──────────
            val customRepo = CustomItemsRepository(context)
            val cats = customRepo.customCategoriesFlow.first()
            val pays = customRepo.customPaymentMethodsFlow.first()

            if (cats.isNotEmpty() || pays.isNotEmpty()) {
                appendLine() // empty line separator
                appendLine("META_CONFIG,Name,IconKey,ColorHex,Type")
                cats.forEach {
                    append("META_ITEM,"); append(escapeCsv(it.name)); append(",")
                    append(it.iconKey);   append(","); append(it.colorHex); append(",")
                    appendLine(it.type)
                }
                pays.forEach {
                    append("META_ITEM,"); append(escapeCsv(it.name)); append(",")
                    append(it.iconKey);   append(","); append(it.colorHex); append(",")
                    appendLine(it.type)
                }
            }
        }
        return fileName to csv
    }

    /**
     * Writes the given content to a Uri (provided via SAF).
     */
    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream, Charsets.UTF_8).use { it.write(content) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Detects whether a URI is a WalletFlow backup CSV and restores it.
     * Returns null if the file is not a WalletFlow backup (caller falls
     * through to the generic import path).
     * Returns a human-readable result string on success/failure.
     */
    suspend fun tryRestoreFromUri(context: Context, uri: Uri): String? {
        return try {
            val reader = BufferedReader(
                InputStreamReader(
                    context.contentResolver.openInputStream(uri) ?: return null
                )
            )
            val header = reader.readLine() ?: return null
            if (header.trim() != BACKUP_HEADER) return null // not our backup format

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val dao = TransactionDatabase.getDatabase(context).transactionDao()

            val transactions = mutableListOf<Transaction>()
            val customCats = mutableListOf<CustomItem>()
            val customPays = mutableListOf<CustomItem>()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val row = parseCsvRow(line!!)
                if (row.isEmpty()) continue

                // ── Handle Custom Item Definitions ───────────────────────────
                if (row[0] == "META_ITEM" && row.size >= 5) {
                    val item = CustomItem(
                        name     = row[1].trim(),
                        iconKey  = row[2].trim(),
                        colorHex = row[3].trim(),
                        type     = row[4].trim()
                    )
                    if (item.type == "payment") customPays.add(item)
                    else customCats.add(item)
                    continue
                }

                if (row.size < 11) continue
                try {
                    val date = sdf.parse(row[1])?.time ?: continue
                    val type = when (row[2].trim()) {
                        "Income"  -> TransactionType.INCOME
                        "Expense" -> TransactionType.EXPENSE
                        else      -> TransactionType.UNKNOWN
                    }
                    val createdAt = if (row.size > 11) row[11].trim().toLongOrNull()
                        ?: System.currentTimeMillis()
                    else System.currentTimeMillis()
                    transactions.add(
                        Transaction(
                            id               = 0,          // let Room auto-assign
                            date             = date,
                            amount           = row[3].trim().toDouble(),
                            type             = type,
                            category         = row[4].trim(),
                            paymentMethod    = row[5].trim(),
                            remark           = row[6].trim(),
                            bankName         = row[7].trim(),
                            accountLastFour  = row[8].trim(),
                            instrumentType   = row[9].trim(),
                            isAddedToMonthly = row[10].trim().equals("true", ignoreCase = true),
                            originalSms      = "",
                            createdAt        = createdAt
                        )
                    )
                } catch (e: Exception) {
                    // skip malformed row
                }
            }
            reader.close()

            if (transactions.isEmpty() && customCats.isEmpty() && customPays.isEmpty()) {
                return "Backup file is empty"
            }

            if (transactions.isNotEmpty()) dao.insertAll(transactions)
            if (customCats.isNotEmpty() || customPays.isNotEmpty()) {
                CustomItemsRepository(context).importCustomItems(customCats, customPays)
            }

            val resultMsg = buildString {
                if (transactions.isNotEmpty()) append("Restored ${transactions.size} transactions. ")
                if (customCats.isNotEmpty() || customPays.isNotEmpty()) {
                    append("Imported ${customCats.size + customPays.size} custom categories/payments.")
                }
            }
            resultMsg.trim()
        } catch (e: Exception) {
            "Restore failed: ${e.message}"
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun escapeCsv(value: String): String {
        val needsQuote = value.contains(',') || value.contains('"') || value.contains('\n')
        return if (needsQuote) "\"${value.replace("\"", "\"\"")}\"" else value
    }


    private fun parseCsvRow(line: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuote = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && !inQuote -> inQuote = true
                c == '"' && inQuote  -> {
                    if (i + 1 < line.length && line[i + 1] == '"') { sb.append('"'); i++ }
                    else inQuote = false
                }
                c == ',' && !inQuote -> { fields.add(sb.toString()); sb.clear() }
                else                 -> sb.append(c)
            }
            i++
        }
        fields.add(sb.toString())
        return fields
    }
}

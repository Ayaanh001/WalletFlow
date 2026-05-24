package com.hussain.walletflow.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hussain.walletflow.R
import com.hussain.walletflow.data.CurrencyData
import com.hussain.walletflow.data.Transaction
import com.hussain.walletflow.data.TransactionType
import com.hussain.walletflow.data.UserPreferencesRepository
import com.hussain.walletflow.ui.theme.ExpenseRed
import com.hussain.walletflow.ui.theme.IncomeGreen
import com.hussain.walletflow.utils.FormatUtils
import com.hussain.walletflow.utils.getCategoryColor
import com.hussain.walletflow.utils.getCategoryIcon
import com.hussain.walletflow.utils.getPaymentChipColor
import com.hussain.walletflow.utils.getPaymentIcon
import com.hussain.walletflow.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryTransactionsScreen(
    viewModel: TransactionViewModel,
    category: String,
    initialYear: Int,
    initialMonth: Int,
    onBack: () -> Unit,
    onEditTransaction: (Long) -> Unit,
    // When true, `category` is treated as a payment method name instead of a category name
    filterByPaymentMethod: Boolean = false
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefsRepo = remember { UserPreferencesRepository(context) }
    val selectedCurrency by prefsRepo.currencyFlow.collectAsState(
        initial = UserPreferencesRepository.DEFAULT_CURRENCY
    )
    val currency = remember(selectedCurrency) {
        CurrencyData.currencies.find { it.code == selectedCurrency }
            ?: CurrencyData.currencies.first()
    }

    fun fmt(v: Double) = FormatUtils.formatIndianAmount(v)

    // ── Month state ───────────────────────────────────────────────────────────
    val now = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(initialYear) }
    var selectedMonthIndex by remember { mutableIntStateOf(initialMonth) }
    var showMonthPicker by remember { mutableStateOf(false) }
    val currentYear = remember { now.get(Calendar.YEAR) }

    val startOfMonth by remember(selectedYear, selectedMonthIndex) {
        derivedStateOf {
            Calendar.getInstance().apply {
                set(selectedYear, selectedMonthIndex, 1, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }
    }

    val endOfMonth by remember(selectedYear, selectedMonthIndex) {
        derivedStateOf {
            Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedYear)
                set(Calendar.MONTH, selectedMonthIndex)
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis
        }
    }

    val displayMonth = remember(selectedYear, selectedMonthIndex) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonthIndex)
        }
        if (selectedYear == currentYear)
            SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
        else
            SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)
    }

    // ── Data ──────────────────────────────────────────────────────────────────
    val monthlyTransactions by viewModel.monthlyTransactions.collectAsState()

    val filteredTransactions = remember(monthlyTransactions, startOfMonth, endOfMonth, category, filterByPaymentMethod) {
        monthlyTransactions
            .filter { tx ->
                tx.date in startOfMonth..endOfMonth &&
                        if (filterByPaymentMethod) {
                            // "Other" bucket: transactions with no payment method set
                            if (category == "Other") tx.paymentMethod.isEmpty()
                            else tx.paymentMethod == category
                        } else {
                            // Category filter — same logic as before
                            tx.category == category ||
                                    (tx.category.isEmpty() &&
                                            (category == "Other Expense" || category == "Other Income"))
                        }
            }
            .sortedByDescending { it.date }
    }

    val totalAmount = remember(filteredTransactions) { filteredTransactions.sumOf { it.amount } }

    // ── Header icon + color — differs by mode ─────────────────────────────────
    // For payment method mode: use the chip colour and payment icon
    // For category mode: use the category colour and category icon
    val headerColor = remember(category, filterByPaymentMethod) {
        if (filterByPaymentMethod) getPaymentChipColor(category)
        else getCategoryColor(category)
    }

    // Date formatters
    val dayKeyFmt = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val dayLblFmt = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val dayLblNoYearFmt = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
    val dowFmt    = remember { SimpleDateFormat("EEEE", Locale.getDefault()) }
    val timeFmt   = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    val groupedByDay: List<Pair<Triple<String, String, Double>, List<Transaction>>> =
        remember(filteredTransactions) {
            val curYear = Calendar.getInstance().get(Calendar.YEAR)
            filteredTransactions
                .groupBy { dayKeyFmt.format(Date(it.date)) }
                .entries
                .sortedByDescending { it.key }
                .map { (_, txns) ->
                    val date = Date(txns.first().date)
                    val cal = Calendar.getInstance().apply { time = date }
                    val dayTotal = txns.sumOf {
                        if (it.type == TransactionType.EXPENSE) -it.amount else it.amount
                    }
                    val label = if (cal.get(Calendar.YEAR) == curYear) dayLblNoYearFmt.format(date) else dayLblFmt.format(date)
                    Triple(label, dowFmt.format(date), dayTotal) to txns
                }
        }

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Back", modifier = Modifier.size(28.dp))
                }

                // Icon bubble — payment icon or category icon
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(headerColor),
                    contentAlignment = Alignment.Center
                ) {
                    if (filterByPaymentMethod) {
                        when (category) {
                            "GPay" -> Image(
                                painter = painterResource(R.drawable.gpay),
                                contentDescription = "GPay",
                                modifier = Modifier.size(19.dp)
                            )
                            "PhonePe" -> Image(
                                painter = painterResource(R.drawable.phonepe),
                                contentDescription = "PhonePe",
                                modifier = Modifier.size(19.dp)
                            )
                            else -> Icon(
                                getPaymentIcon(category),
                                contentDescription = null,
                                modifier = Modifier.size(19.dp),
                                tint = Color.White
                            )
                        }
                    } else {
                        Icon(
                            getCategoryIcon(category),
                            contentDescription = null,
                            modifier = Modifier.size(19.dp),
                            tint = Color.White
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = category,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${currency.symbol} ${fmt(totalAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Month picker chip
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { showMonthPicker = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.calendar_month),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = displayMonth,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        if (filteredTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        Icons.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    )
                    Text(
                        text = "No transactions for $category\nin $displayMonth",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 40.dp)
            ) {
                groupedByDay.forEach { (dayMeta, transactions) ->
                    val (dayLabel, dayOfWeek, dayTotal) = dayMeta

                    item(key = "hdr_$dayLabel") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(text = dayLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text(text = dayOfWeek, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "${if (dayTotal < 0) "- " else ""}${currency.symbol} ${fmt(kotlin.math.abs(dayTotal))}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = if (dayTotal >= 0) IncomeGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    transactions.forEach { transaction ->
                        item(key = "tx_${transaction.id}") {
                            CategoryTransactionItem(
                                transaction = transaction,
                                // In payment method mode show the category chip instead;
                                // in category mode show the payment method chip as before.
                                highlightPaymentMethod = !filterByPaymentMethod,
                                accentColor = headerColor,
                                currencySymbol = currency.symbol,
                                timeFmt = timeFmt,
                                onClick = { onEditTransaction(transaction.id) }
                            )
                        }
                    }

                    item(key = "spc_$dayLabel") { Spacer(modifier = Modifier.height(6.dp)) }
                }
            }
        }
    }

    if (showMonthPicker) {
        MonthPickerDialog(
            selectedYear = selectedYear,
            selectedMonth = selectedMonthIndex,
            currentYear = currentYear,
            currentMonth = now.get(Calendar.MONTH),
            onMonthSelected = { year, month ->
                selectedYear = year
                selectedMonthIndex = month
                showMonthPicker = false
            },
            onDismiss = { showMonthPicker = false }
        )
    }
}

// ── Single transaction item ───────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryTransactionItem(
    transaction: Transaction,
    // true  → show payment method chip (original category-screen behaviour)
    // false → show category chip (payment method screen behaviour)
    highlightPaymentMethod: Boolean,
    accentColor: Color,
    currencySymbol: String,
    timeFmt: SimpleDateFormat,
    onClick: () -> Unit
) {
    val displayText = remember(transaction.originalSms, transaction.remark) {
        if (transaction.originalSms.isNotEmpty() && transaction.originalSms != "Manual entry")
            transaction.originalSms.replace("\n", " ").replace("\r", " ").trim()
        else
            transaction.remark
    }
    val amountColor = if (transaction.type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {

            // ── Top chip row ──────────────────────────────────────────────────
            val showTopChips = if (highlightPaymentMethod)
                transaction.paymentMethod.isNotEmpty() || transaction.bankName.isNotEmpty()
            else
                transaction.category.isNotEmpty()

            if (showTopChips) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (highlightPaymentMethod) {
                        // ── Payment method chip ───────────────────────────────
                        if (transaction.paymentMethod.isNotEmpty()) {
                            val chipColor = getPaymentChipColor(transaction.paymentMethod)
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(chipColor.copy(alpha = 0.13f))
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                when (transaction.paymentMethod) {
                                    "GPay" -> Image(painter = painterResource(R.drawable.gpay), contentDescription = "GPay", modifier = Modifier.size(12.dp))
                                    "PhonePe" -> Image(painter = painterResource(R.drawable.phonepe), contentDescription = "PhonePe", modifier = Modifier.size(12.dp))
                                    else -> Icon(getPaymentIcon(transaction.paymentMethod), contentDescription = null, modifier = Modifier.size(12.dp), tint = chipColor)
                                }
                                Text(text = transaction.paymentMethod, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = chipColor)
                            }
                        }

                        // ── Bank chip ─────────────────────────────────────────
                        if (transaction.bankName.isNotEmpty()) {
                            val bankLabel = when (transaction.instrumentType) {
                                "CARD" -> "${transaction.bankName} • Card ${transaction.accountLastFour}"
                                else   -> if (transaction.accountLastFour.isNotEmpty())
                                    "${transaction.bankName} • A/C ${transaction.accountLastFour}"
                                else transaction.bankName
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (transaction.instrumentType == "CARD") Icons.Filled.CreditCard else Icons.Filled.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(text = bankLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    } else {
                        // ── Category chip (shown when browsing by payment method) ──
                        val catName = transaction.category.ifEmpty {
                            if (transaction.type == TransactionType.EXPENSE) "Other Expense" else "Other Income"
                        }
                        val catColor = getCategoryColor(catName)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(catColor.copy(alpha = 0.13f))
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(getCategoryIcon(catName), contentDescription = null, modifier = Modifier.size(12.dp), tint = catColor)
                            Text(text = catName, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = catColor)
                        }

                        // Also show bank chip if available
                        if (transaction.bankName.isNotEmpty()) {
                            val bankLabel = when (transaction.instrumentType) {
                                "CARD" -> "${transaction.bankName} • Card ${transaction.accountLastFour}"
                                else   -> if (transaction.accountLastFour.isNotEmpty())
                                    "${transaction.bankName} • A/C ${transaction.accountLastFour}"
                                else transaction.bankName
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    if (transaction.instrumentType == "CARD") Icons.Filled.CreditCard else Icons.Filled.AccountBalance,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(text = bankLabel, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Description + Amount ──────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    if (displayText.isNotEmpty()) {
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "$currencySymbol ${FormatUtils.formatIndianAmount(transaction.amount)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }

            // ── Time ──────────────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = timeFmt.format(Date(transaction.date)).replace(" AM", "").replace(" PM", ""),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = timeFmt.format(Date(transaction.date)).takeLast(2),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
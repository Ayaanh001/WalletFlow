package com.hussain.walletflow.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hussain.walletflow.R
import com.hussain.walletflow.data.CurrencyData
import com.hussain.walletflow.data.CustomItem
import com.hussain.walletflow.data.CustomItemsRepository
import com.hussain.walletflow.data.Transaction
import com.hussain.walletflow.data.TransactionCategories
import com.hussain.walletflow.data.TransactionType
import com.hussain.walletflow.data.UserPreferencesRepository
import com.hussain.walletflow.ui.theme.BalanceBlue
import com.hussain.walletflow.ui.theme.ExpenseRed
import com.hussain.walletflow.ui.theme.IncomeGreen
import com.hussain.walletflow.utils.getCategoryColor
import com.hussain.walletflow.utils.getCategoryIcon
import com.hussain.walletflow.utils.getPaymentChipColor
import com.hussain.walletflow.utils.getPaymentIcon
import com.hussain.walletflow.utils.registerCustomCategories
import com.hussain.walletflow.utils.registerCustomPaymentMethods
import com.hussain.walletflow.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

// Locale-aware Indian number formatter — created once, not on every call
private val indianNumberFormat: NumberFormat by lazy {
        NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
        }
}

private fun formatIndianAmount(amount: Double): String = indianNumberFormat.format(amount)

/**
 * Renders 4 randomised M3-style shapes (circle, rounded-square, diamond, pill)
 * as a privacy mask instead of dots. [size] controls the height of each shape.
 */
@Composable
private fun HiddenAmountShapes(size: Dp, color: Color) {
        val shapes = remember { listOf(0, 1, 2, 3) } // circle, square, diamond, pill
        Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(size * 0.35f)
        ) {
                shapes.forEachIndexed { idx, _ ->
                        Canvas(modifier = Modifier.size(size)) {
                                val r = size.toPx() / 2f
                                when (idx % 4) {
                                        0 -> drawCircle(color = color, radius = r)                      // circle
                                        1 -> drawRoundRect(                                              // pill
                                                color = color,
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.8f)
                                        )
                                        2 -> {                                                           // diamond
                                                val path = androidx.compose.ui.graphics.Path().apply {
                                                        moveTo(r, 0f); lineTo(r * 2, r); lineTo(r, r * 2); lineTo(0f, r); close()
                                                }
                                                drawPath(path, color = color)
                                        }
                                        else -> drawRoundRect(                                           // rounded square
                                                color = color,
                                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(r * 0.4f)
                                        )
                                }
                        }
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
        viewModel: TransactionViewModel,
        listState: androidx.compose.foundation.lazy.LazyListState,
        onSettingsClick: () -> Unit,
        onEditTransaction: (Long) -> Unit,
        onSelectionModeChanged: (Boolean) -> Unit = {},
        onIncomeClick: (year: Int, month: Int) -> Unit = { _, _ -> },
        onExpenseClick: (year: Int, month: Int) -> Unit = { _, _ -> }
) {
        val haptic = LocalHapticFeedback.current
        val context = LocalContext.current

        // Preferences — create once per composition, not on every recomposition
        val prefsRepository = remember { UserPreferencesRepository(context) }
        val userName by prefsRepository.nameFlow.collectAsState(initial = "")
        val selectedCurrency by prefsRepository.currencyFlow.collectAsState(
                initial = UserPreferencesRepository.DEFAULT_CURRENCY
        )
        val currency = remember(selectedCurrency) {
                CurrencyData.currencies.find { it.code == selectedCurrency }
                        ?: CurrencyData.currencies.first()
        }
        val hideBalance by prefsRepository.hideBalanceFlow.collectAsState(initial = false)
        val hideIncome  by prefsRepository.hideIncomeFlow.collectAsState(initial = false)
        // Tap-to-reveal: track whether the value is temporarily shown
        var balanceRevealed by remember { mutableStateOf(false) }
        var incomeRevealed  by remember { mutableStateOf(false) }
        val scope2 = rememberCoroutineScope()
        LaunchedEffect(hideBalance) { if (!hideBalance) balanceRevealed = false }
        LaunchedEffect(hideIncome)  { if (!hideIncome)  incomeRevealed  = false }

        // ── Selection state ──────────────────────────────────────────────────────
        var isSelectionMode by remember { mutableStateOf(false) }
        // Use a SnapshotStateSet so Compose only recomposes items whose selection changed
        val selectedIds = remember { mutableStateSetOf<Long>() }

        LaunchedEffect(isSelectionMode) { onSelectionModeChanged(isSelectionMode) }

        LaunchedEffect(selectedIds.size) {
                if (isSelectionMode && selectedIds.isEmpty()) isSelectionMode = false
        }

        BackHandler(enabled = isSelectionMode) {
                selectedIds.clear()
                isSelectionMode = false
        }

        // ── Month / year picker ──────────────────────────────────────────────────
        val now = remember { Calendar.getInstance() }
        var selectedYear by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
        var selectedMonthIndex by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }
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
                                set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
                                set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                }
        }

        val displayMonth = remember(selectedYear, selectedMonthIndex) {
                val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, selectedYear); set(Calendar.MONTH, selectedMonthIndex)
                }
                if (selectedYear == currentYear)
                        SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
                else
                        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)
        }

        // ── Data ─────────────────────────────────────────────────────────────────
        // monthlyTransactions is a StateFlow — collectAsState() never re-subscribes
        val monthlyTransactions by viewModel.monthlyTransactions.collectAsState()

        val monthlyStats by viewModel.getMonthlyStats(startOfMonth, endOfMonth)
                .collectAsState(initial = 0.0 to 0.0)
        val (totalIncome, totalExpense) = monthlyStats
        val balance = totalIncome - totalExpense

        // Date formatters — expensive to create; share across recompositions
        val dateFormat = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
        val dayLabelFormat = remember { SimpleDateFormat("MMM dd", Locale.getDefault()) }
        val dayOfWeekFormat = remember { SimpleDateFormat("EEEE", Locale.getDefault()) }

        // Group transactions — only recomputes when the list or month window changes
        val groupedTransactions = remember(monthlyTransactions, startOfMonth, endOfMonth) {
                monthlyTransactions
                        .filter { it.date in startOfMonth..endOfMonth }
                        .sortedByDescending { it.date }
                        .groupBy { dateFormat.format(Date(it.date)) }
                        .map { (_, txns) ->
                                val date = Date(txns.first().date)
                                val dayTotal = txns.sumOf { if (it.type == TransactionType.EXPENSE) -it.amount else it.amount }
                                Triple(
                                        dayLabelFormat.format(date) to dayOfWeekFormat.format(date),
                                        dayTotal,
                                        txns
                                )
                        }
        }

        // Running balances — sorted ascending so cumulative sum is correct
        val runningBalances = remember(monthlyTransactions, startOfMonth, endOfMonth) {
                var cumulative = 0.0
                monthlyTransactions
                        .filter { it.date in startOfMonth..endOfMonth }
                        .sortedBy { it.date }
                        .associate { tx ->
                                cumulative += if (tx.type == TransactionType.INCOME) tx.amount else -tx.amount
                                tx.id to cumulative
                        }
        }

        // derivedStateOf avoids recomposing everything just because selectedIds.size changed
        val allSelected by remember {
                derivedStateOf {
                        val allIds = groupedTransactions.flatMap { it.third }.map { it.id }
                        allIds.isNotEmpty() && selectedIds.containsAll(allIds)
                }
        }

        // ── Bulk-edit sheet state ────────────────────────────────────────────────
        var showBulkEditSheet by remember { mutableStateOf(false) }

        var bulkEditReopenStep by remember { mutableIntStateOf(0) }

        // ── Custom items ─────────────────────────────────────────────────────────
        val customItemsRepo = remember { CustomItemsRepository(context) }
        val customCategories by customItemsRepo.customCategoriesFlow.collectAsState(initial = emptyList())
        val customPaymentMethods by customItemsRepo.customPaymentMethodsFlow.collectAsState(initial = emptyList())
        LaunchedEffect(customCategories) { registerCustomCategories(customCategories) }
        LaunchedEffect(customPaymentMethods) { registerCustomPaymentMethods(customPaymentMethods) }

        // ── Create-new overlays (state lifted here so they render ABOVE ModalBottomSheet popup) ──
        var showNewCategoryFromSheet by remember { mutableStateOf(false) }
        var showNewPaymentFromSheet  by remember { mutableStateOf(false) }
        val homeCoroutineScope = rememberCoroutineScope()

        // Close create-new overlays on system back
        BackHandler(enabled = showNewCategoryFromSheet || showNewPaymentFromSheet) {
                showNewCategoryFromSheet = false
                showNewPaymentFromSheet  = false
        }
        val allBulkCategories = remember(customCategories) {
                TransactionCategories.EXPENSE_CATEGORIES +
                        TransactionCategories.INCOME_CATEGORIES +
                        customCategories.map { it.name }
        }
        val allBulkPaymentMethods = remember(customPaymentMethods) {
                TransactionCategories.PAYMENT_METHODS + customPaymentMethods.map { it.name }
        }

        // ── UI ───────────────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {

                        // Sticky header
                        Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = if (listState.firstVisibleItemIndex > 0) 4.dp else 0.dp
                        ) {
                                Crossfade(targetState = isSelectionMode, label = "topBarFade") { selection ->

                                        val allSelected by remember {
                                                derivedStateOf {
                                                        val allIds = groupedTransactions.flatMap { it.third }.map { it.id }
                                                        allIds.isNotEmpty() && selectedIds.containsAll(allIds)  // ← containsAll not size check
                                                }
                                        }

                                        if (isSelectionMode) {
                                                HomeSelectionHeader(
                                                        selectedCount = selectedIds.size,
                                                        allSelected = allSelected,
                                                        onToggleSelectAll = {
                                                                if (allSelected) {
                                                                        selectedIds.clear()
                                                                } else {
                                                                        selectedIds.addAll(groupedTransactions.flatMap { it.third }.map { it.id })
                                                                }
                                                        },
                                                        onDeleteSelected = {
                                                                viewModel.deleteTransactionsByIds(selectedIds.toList())
                                                                selectedIds.clear()
                                                                isSelectionMode = false
                                                        },
                                                        onMoreSelected = { showBulkEditSheet = true }
                                                )
                                        } else {
                                                Row(
                                                        modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                        Text(
                                                                text = if (userName.isNotEmpty()) "Hi, $userName" else "Hi",
                                                                modifier = Modifier.weight(1f),
                                                                overflow = TextOverflow.Ellipsis,
                                                                maxLines = 1,
                                                                style = MaterialTheme.typography.titleLarge,
                                                                fontWeight = FontWeight.Bold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                // Month chip
                                                                Row(
                                                                        modifier = Modifier
                                                                                .clip(CircleShape)
                                                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                                                                .clickable { if (!isSelectionMode) showMonthPicker = true }
                                                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                        Icon(
                                                                                painter = painterResource(R.drawable.calendar_month),
                                                                                contentDescription = null,
                                                                                modifier = Modifier.size(18.dp),
                                                                                tint = MaterialTheme.colorScheme.onSurface
                                                                        )
                                                                        Text(
                                                                                text = displayMonth,
                                                                                style = MaterialTheme.typography.labelLarge,
                                                                                fontWeight = FontWeight.Medium,
                                                                                color = MaterialTheme.colorScheme.onSurface
                                                                        )
                                                                }
                                                                FilledIconButton(
                                                                        colors = IconButtonDefaults.filledIconButtonColors(
                                                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                                                                contentColor = MaterialTheme.colorScheme.onSurface
                                                                        ),
                                                                        onClick = onSettingsClick
                                                                ) {
                                                                        Icon(
                                                                                painter = painterResource(R.drawable.settings),
                                                                                contentDescription = "Settings"
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                                LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        state = listState,
                                        contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                        // Balance card
                                        item(key = "balance_card") {
                                                Column(
                                                        modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(horizontal = 16.dp)
                                                ) {
                                                        Card(
                                                                modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(bottom = 16.dp)
                                                                        .clip(RoundedCornerShape(20.dp))
                                                                        .then(
                                                                                if (hideBalance)
                                                                                        Modifier.clickable {
                                                                                                balanceRevealed = true
                                                                                                scope2.launch {
                                                                                                        kotlinx.coroutines.delay(5000)
                                                                                                        balanceRevealed = false
                                                                                                }
                                                                                        }
                                                                                else Modifier
                                                                        ),
                                                                colors = CardDefaults.cardColors(
                                                                        containerColor = BalanceBlue.copy(alpha = 0.1f)
                                                                ),
                                                                shape = RoundedCornerShape(20.dp)
                                                        ) {
                                                                Column(
                                                                        modifier = Modifier
                                                                                .fillMaxWidth()
                                                                                .padding(24.dp),
                                                                        horizontalAlignment = Alignment.Start
                                                                ) {
                                                                        Text(
                                                                                text = "Available Balance",
                                                                                style = MaterialTheme.typography.titleSmall,
                                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                        )
                                                                        Spacer(modifier = Modifier.height(8.dp))
                                                                        if (hideBalance && !balanceRevealed) {
                                                                                Row(
                                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                                ) {
                                                                                        Text(
                                                                                                text = currency.symbol,
                                                                                                style = MaterialTheme.typography.displayMedium,
                                                                                                fontWeight = FontWeight.Bold,
                                                                                                color = MaterialTheme.colorScheme.onSurface
                                                                                        )
                                                                                        HiddenAmountShapes(
                                                                                                size = 24.dp,
                                                                                                color = MaterialTheme.colorScheme.onSurface
                                                                                        )
                                                                                }
                                                                        } else {
                                                                                Text(
                                                                                        text = "${currency.symbol} ${formatIndianAmount(balance)}",
                                                                                        style = MaterialTheme.typography.displayMedium,
                                                                                        fontWeight = FontWeight.Bold,
                                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                                )
                                                                        }
                                                                }
                                                        }

                                                        Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                                        ) {
                                                                // Income card
                                                                Card(
                                                                        modifier = Modifier.weight(1f)
                                                                                .clip(RoundedCornerShape(20.dp))
                                                                                .clickable {
                                                                                        when {
                                                                                                hideIncome && !incomeRevealed -> {
                                                                                                        // First tap: reveal the amount
                                                                                                        incomeRevealed = true
                                                                                                        scope2.launch {
                                                                                                                kotlinx.coroutines.delay(5000)
                                                                                                                incomeRevealed = false
                                                                                                        }
                                                                                                }
                                                                                                else -> {
                                                                                                        // Second tap (revealed) or hide is off: navigate
                                                                                                        onIncomeClick(selectedYear, selectedMonthIndex)
                                                                                                }
                                                                                        }
                                                                                },
                                                                        colors = CardDefaults.cardColors(
                                                                                containerColor = IncomeGreen.copy(alpha = 0.1f)
                                                                        ),
                                                                        shape = RoundedCornerShape(20.dp)
                                                                ) {
                                                                        Column(
                                                                                modifier = Modifier
                                                                                        .fillMaxWidth()
                                                                                        .padding(16.dp)
                                                                        ) {
                                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                                        Icon(
                                                                                                Icons.Default.ArrowDownward,
                                                                                                contentDescription = null,
                                                                                                modifier = Modifier.size(16.dp),
                                                                                                tint = IncomeGreen
                                                                                        )
                                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                                        Text(
                                                                                                "Income",
                                                                                                style = MaterialTheme.typography.labelMedium,
                                                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                                        )
                                                                                }
                                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                                if (hideIncome && !incomeRevealed) {
                                                                                        Row(
                                                                                                verticalAlignment = Alignment.CenterVertically,
                                                                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                                                        ) {
                                                                                                Text(
                                                                                                        text = currency.symbol,
                                                                                                        style = MaterialTheme.typography.titleMedium,
                                                                                                        fontWeight = FontWeight.Bold,
                                                                                                        color = IncomeGreen
                                                                                                )
                                                                                                HiddenAmountShapes(
                                                                                                        size = 12.dp,
                                                                                                        color = IncomeGreen.copy(alpha = 0.5f)
                                                                                                )
                                                                                        }
                                                                                } else {
                                                                                        Text(
                                                                                                text = "${currency.symbol} ${formatIndianAmount(totalIncome)}",
                                                                                                style = MaterialTheme.typography.titleMedium,
                                                                                                fontWeight = FontWeight.Bold,
                                                                                                color = IncomeGreen
                                                                                        )
                                                                                }
                                                                        }
                                                                }

                                                                // Expense card
                                                                Card(
                                                                        modifier = Modifier.weight(1f)
                                                                                .clip(RoundedCornerShape(20.dp))
                                                                                .clickable { onExpenseClick(selectedYear, selectedMonthIndex) },
                                                                        colors = CardDefaults.cardColors(
                                                                                containerColor = ExpenseRed.copy(alpha = 0.1f)
                                                                        ),
                                                                        shape = RoundedCornerShape(20.dp)
                                                                ) {
                                                                        Column(
                                                                                modifier = Modifier
                                                                                        .fillMaxWidth()
                                                                                        .padding(16.dp)
                                                                        ) {
                                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                                        Icon(
                                                                                                Icons.Default.ArrowUpward,
                                                                                                contentDescription = null,
                                                                                                modifier = Modifier.size(16.dp),
                                                                                                tint = ExpenseRed
                                                                                        )
                                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                                        Text(
                                                                                                "Expenses",
                                                                                                style = MaterialTheme.typography.labelMedium,
                                                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                                        )
                                                                                }
                                                                                Spacer(modifier = Modifier.height(4.dp))
                                                                                Text(
                                                                                        text = "${currency.symbol} ${formatIndianAmount(totalExpense)}",
                                                                                        style = MaterialTheme.typography.titleMedium,
                                                                                        fontWeight = FontWeight.Bold,
                                                                                        color = ExpenseRed
                                                                                )
                                                                        }
                                                                }
                                                        }
                                                        Spacer(modifier = Modifier.height(24.dp))
                                                }
                                        }

                                        if (groupedTransactions.isEmpty()) {
                                                item(key = "empty_state") {
                                                        Box(
                                                                modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .padding(top = 80.dp),
                                                                contentAlignment = Alignment.Center
                                                        ) {
                                                                Column(
                                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                                                ) {
                                                                        Icon(
                                                                                Icons.Filled.ReceiptLong,
                                                                                contentDescription = null,
                                                                                modifier = Modifier.size(64.dp),
                                                                                tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                                                        )
                                                                        Text(
                                                                                text = "No transactions added yet",
                                                                                style = MaterialTheme.typography.bodyLarge,
                                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                        )
                                                                }
                                                        }
                                                }
                                        } else {
                                                groupedTransactions.forEach { (dayInfo, dayTotal, transactions) ->
                                                        val (dayLabel, dayOfWeek) = dayInfo

                                                        item(key = "header_$dayLabel") {
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
                                                                                Text(
                                                                                        text = dayLabel,
                                                                                        style = MaterialTheme.typography.titleMedium,
                                                                                        fontWeight = FontWeight.Bold
                                                                                )
                                                                                Text(
                                                                                        text = dayOfWeek,
                                                                                        style = MaterialTheme.typography.titleMedium,
                                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                                )
                                                                        }
                                                                        Text(
                                                                                text = "${if (dayTotal <= 0) "- " else ""}${currency.symbol} ${formatIndianAmount(kotlin.math.abs(dayTotal))}",
                                                                                style = MaterialTheme.typography.titleMedium,
                                                                                fontWeight = FontWeight.SemiBold,
                                                                                color = if (dayTotal >= 0) IncomeGreen
                                                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                                                        )
                                                                }
                                                        }

                                                        items(transactions, key = { it.id }) { transaction ->
                                                                val runningBalance = runningBalances[transaction.id] ?: 0.0
                                                                // Read isSelected from the SnapshotStateSet — fine-grained recompose
                                                                val isSelected = selectedIds.contains(transaction.id)
                                                                HomeTransactionItem(
                                                                        transaction = transaction,
                                                                        currencySymbol = currency.symbol,
                                                                        runningBalance = runningBalance,
                                                                        isSelectionMode = isSelectionMode,
                                                                        isSelected = isSelected,
                                                                        onClick = {
                                                                                if (isSelectionMode) {
                                                                                        if (isSelected) selectedIds.remove(transaction.id)
                                                                                        else selectedIds.add(transaction.id)
                                                                                } else {
                                                                                        onEditTransaction(transaction.id)
                                                                                }
                                                                        },
                                                                        onLongClick = {
                                                                                if (!isSelectionMode) {
                                                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                                        isSelectionMode = true
                                                                                        selectedIds.add(transaction.id)
                                                                                }
                                                                        }
                                                                )
                                                        }

                                                        item(key = "spacer_$dayLabel") {
                                                                Spacer(modifier = Modifier.height(8.dp))
                                                        }
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


                        if (showBulkEditSheet) {
                                BulkEditSheet(
                                        selectedCount = selectedIds.size,
                                        initialStep = bulkEditReopenStep,          // ← pass saved step
                                        onDismiss = { showBulkEditSheet = false },
                                        customCategories = customCategories,
                                        customPaymentMethods = customPaymentMethods,
                                        onChangeCategory = { category ->
                                                viewModel.updateCategoryByIds(selectedIds.toList(), category)
                                                showBulkEditSheet = false
                                                bulkEditReopenStep = 0                 // ← reset for next open
                                                selectedIds.clear()
                                                isSelectionMode = false
                                        },
                                        onChangePaymentMethod = { method ->
                                                viewModel.updatePaymentMethodByIds(selectedIds.toList(), method)
                                                showBulkEditSheet = false
                                                bulkEditReopenStep = 0                 // ← reset for next open
                                                selectedIds.clear()
                                                isSelectionMode = false
                                        },
                                        onRequestNewCategory = {
                                                bulkEditReopenStep = 1                 // ← remember we were on step 1
                                                showBulkEditSheet = false
                                                showNewCategoryFromSheet = true
                                        },
                                        onRequestNewPayment = {
                                                bulkEditReopenStep = 2                 // ← remember we were on step 2
                                                showBulkEditSheet = false
                                                showNewPaymentFromSheet = true
                                        }
                                )
                        }
                } // end Column

                // ── Create-new overlays — rendered outside Column so they appear above ModalBottomSheet ──
                if (showNewCategoryFromSheet) {
                        CreateCustomItemScreen(
                                isCategory = true,
                                existingNames = allBulkCategories,
                                onConfirm = { name, iconKey, colorHex, itemType ->
                                        val newItem = CustomItem(name, iconKey, colorHex, itemType)
                                        registerCustomCategories(customCategories + newItem)
                                        homeCoroutineScope.launch {
                                                customItemsRepo.addCustomCategory(newItem)
                                        }
                                        showNewCategoryFromSheet = false
                                        showBulkEditSheet = true           // ← reopen sheet so user can pick the new category
                                },
                                onBack = {
                                        showNewCategoryFromSheet = false
                                        showBulkEditSheet = true           // ← reopen sheet on back press
                                }
                        )
                }
                if (showNewPaymentFromSheet) {
                        CreateCustomItemScreen(
                                isCategory = false,
                                existingNames = allBulkPaymentMethods,
                                onConfirm = { name, iconKey, colorHex, itemType ->
                                        val newItem = CustomItem(name, iconKey, colorHex, itemType)
                                        registerCustomPaymentMethods(customPaymentMethods + newItem)
                                        homeCoroutineScope.launch {
                                                customItemsRepo.addCustomPaymentMethod(newItem)
                                        }
                                        showNewPaymentFromSheet = false
                                        showBulkEditSheet = true           // ← reopen sheet so user can pick the new method
                                },
                                onBack = {
                                        showNewPaymentFromSheet = false
                                        showBulkEditSheet = true           // ← reopen sheet on back press
                                }
                        )
                }
        } // end Box
}

@Composable
private fun HomeSelectionHeader(
        selectedCount: Int,
        allSelected: Boolean,
        onToggleSelectAll: () -> Unit,
        onDeleteSelected: () -> Unit,
        onMoreSelected: () -> Unit
) {
        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        val iconTint by animateColorAsState(
                                targetValue = if (allSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(300),
                                label = "selectAllColor"
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = onToggleSelectAll, modifier = Modifier.size(36.dp)) {
                                        Icon(
                                                imageVector = if (allSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                                contentDescription = "Select all",
                                                tint = iconTint,
                                                modifier = Modifier.size(24.dp)
                                        )
                                }
                                Text("All", style = MaterialTheme.typography.labelSmall, color = iconTint)
                        }
                        Text(
                                text = "$selectedCount selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                }
                Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                ) {
                        IconButton(
                                onClick = onMoreSelected,
                                modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .size(40.dp)
                        ) {
                                Icon(
                                        Icons.Default.MoreHoriz,
                                        contentDescription = "More options",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                        }
                        IconButton(
                                onClick = onDeleteSelected,
                                modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .size(40.dp)
                        ) {
                                Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete selected",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                        }
                }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BulkEditSheet(
        selectedCount: Int,
        initialStep: Int = 0,                  // ← new param
        onDismiss: () -> Unit,
        customCategories: List<CustomItem>,
        customPaymentMethods: List<CustomItem>,
        onChangeCategory: (String) -> Unit,
        onChangePaymentMethod: (String) -> Unit,
        onRequestNewCategory: () -> Unit,
        onRequestNewPayment: () -> Unit
) {
        var step by remember { mutableIntStateOf(initialStep) }

        val allExpenseCategories = remember(customCategories) {
                TransactionCategories.EXPENSE_CATEGORIES +
                        customCategories.filter { it.type == "expense" }.map { it.name }
        }
        val allIncomeCategories = remember(customCategories) {
                TransactionCategories.INCOME_CATEGORIES +
                        customCategories.filter { it.type == "income" }.map { it.name }
        }
        val allPaymentMethods = remember(customPaymentMethods) {
                TransactionCategories.PAYMENT_METHODS + customPaymentMethods.map { it.name }
        }

        ModalBottomSheet(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                containerColor = MaterialTheme.colorScheme.surface
        ) {
                Column(
                        modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .padding(bottom = 32.dp)
                ) {
                        // Title row with optional back button
                        Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                if (step != 0) {
                                        IconButton(
                                                onClick = { step = 0 },
                                                modifier = Modifier.size(32.dp)
                                        ) {
                                                Icon(
                                                        Icons.Default.ArrowBack,
                                                        contentDescription = "Back",
                                                        modifier = Modifier.size(20.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(
                                        text = when (step) {
                                                1    -> "Select Category"
                                                2    -> "Select Payment Method"
                                                else -> "Edit $selectedCount transaction${if (selectedCount > 1) "s" else ""}"
                                        },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                )
                        }

                        when (step) {
                                0 -> {
                                        BulkEditMenuOption(
                                                icon = Icons.Default.Category,
                                                label = "Change Category",
                                                onClick = { step = 1 }
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        BulkEditMenuOption(
                                                icon = Icons.Default.Payment,
                                                label = "Change Payment Method",
                                                onClick = { step = 2 }
                                        )
                                }

                                1 -> {
                                        Column(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {

                                                // ── Expense label ─────────────────────────────────────────────────
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                        HorizontalDivider(modifier = Modifier.weight(1f))
                                                        Text(
                                                                text = "Expense",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        HorizontalDivider(modifier = Modifier.weight(1f))
                                                }

                                                @OptIn(ExperimentalLayoutApi::class)
                                                FlowRow(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                        allExpenseCategories.forEach { category ->
                                                                val chipColor = getCategoryColor(category)
                                                                Row(
                                                                        modifier = Modifier
                                                                                .clip(RoundedCornerShape(13.dp))
                                                                                .background(chipColor.copy(alpha = 0.12f))
                                                                                .border(1.dp, chipColor.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
                                                                                .clickable { onChangeCategory(category) }
                                                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                        Icon(
                                                                                getCategoryIcon(category),
                                                                                contentDescription = null,
                                                                                modifier = Modifier.size(16.dp),
                                                                                tint = chipColor
                                                                        )
                                                                        Text(
                                                                                text = category,
                                                                                style = MaterialTheme.typography.labelMedium,
                                                                                fontWeight = FontWeight.SemiBold,
                                                                                color = chipColor
                                                                        )
                                                                }
                                                        }
                                                }

                                                // ── Income label ──────────────────────────────────────────────────
                                                Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                        HorizontalDivider(modifier = Modifier.weight(1f))
                                                        Text(
                                                                text = "Income",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        HorizontalDivider(modifier = Modifier.weight(1f))
                                                }

                                                @OptIn(ExperimentalLayoutApi::class)
                                                FlowRow(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                        allIncomeCategories.forEach { category ->
                                                                val chipColor = getCategoryColor(category)
                                                                Row(
                                                                        modifier = Modifier
                                                                                .clip(RoundedCornerShape(13.dp))
                                                                                .background(chipColor.copy(alpha = 0.12f))
                                                                                .border(1.dp, chipColor.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
                                                                                .clickable { onChangeCategory(category) }
                                                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                        Icon(
                                                                                getCategoryIcon(category),
                                                                                contentDescription = null,
                                                                                modifier = Modifier.size(16.dp),
                                                                                tint = chipColor
                                                                        )
                                                                        Text(
                                                                                text = category,
                                                                                style = MaterialTheme.typography.labelMedium,
                                                                                fontWeight = FontWeight.SemiBold,
                                                                                color = chipColor
                                                                        )
                                                                }
                                                        }
                                                }

                                                // ── "+ New" chip (standalone at the bottom) ───────────────────────
                                                Row(
                                                        modifier = Modifier
                                                                .clip(RoundedCornerShape(13.dp))
                                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(13.dp))
                                                                .clickable { onRequestNewCategory() }
                                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                        Icon(
                                                                Icons.Default.Add,
                                                                contentDescription = "New category",
                                                                modifier = Modifier.size(14.dp),
                                                                tint = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                                text = "New",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.primary
                                                        )
                                                }
                                        }
                                }                                2 -> {
                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        allPaymentMethods.forEach { method ->
                                                val chipColor = getPaymentChipColor(method)
                                                Row(
                                                        modifier = Modifier
                                                                .clip(RoundedCornerShape(13.dp))
                                                                .background(chipColor.copy(alpha = 0.12f))
                                                                .border(1.dp, chipColor.copy(alpha = 0.3f), RoundedCornerShape(13.dp))
                                                                .clickable { onChangePaymentMethod(method) }
                                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                        when (method) {
                                                                "GPay" -> androidx.compose.foundation.Image(
                                                                        painter = painterResource(R.drawable.gpay),
                                                                        contentDescription = "GPay",
                                                                        modifier = Modifier.size(14.dp)
                                                                )
                                                                "PhonePe" -> androidx.compose.foundation.Image(
                                                                        painter = painterResource(R.drawable.phonepe),
                                                                        contentDescription = "PhonePe",
                                                                        modifier = Modifier.size(14.dp)
                                                                )
                                                                else -> Icon(
                                                                        getPaymentIcon(method),
                                                                        contentDescription = null,
                                                                        modifier = Modifier.size(14.dp),
                                                                        tint = chipColor
                                                                )
                                                        }
                                                        Text(
                                                                text = method,
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = chipColor
                                                        )
                                                }
                                        }
                                        // "+ New" chip
                                        Row(
                                                modifier = Modifier
                                                        .clip(RoundedCornerShape(13.dp))
                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(13.dp))
                                                        .clickable { onRequestNewPayment() }
                                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                                Icon(
                                                        Icons.Default.Add,
                                                        contentDescription = "New payment method",
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                        text = "New",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                )
                                        }
                                }
                        }
                        }
                }
        }

}

@Composable
private fun BulkEditMenuOption(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        label: String,
        onClick: () -> Unit
) {
        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable(onClick = onClick)
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
                Box(
                        modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                ) {
                        Icon(
                                icon,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                        )
                }
                Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }
}

@Composable
fun MonthPickerDialog(
        selectedYear: Int,
        selectedMonth: Int,
        currentYear: Int,
        currentMonth: Int,
        onMonthSelected: (year: Int, month: Int) -> Unit,
        onDismiss: () -> Unit
) {
        var pickerYear by remember { mutableIntStateOf(selectedYear) }
        val monthNames = remember {
                val cal = Calendar.getInstance()
                (0..11).map { m ->
                        cal.set(Calendar.MONTH, m)
                        SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
                }
        }

        AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                IconButton(onClick = { pickerYear-- }) {
                                        Icon(Icons.Default.ChevronLeft, contentDescription = "Previous year")
                                }
                                Text(
                                        text = pickerYear.toString(),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                )
                                IconButton(onClick = { pickerYear++ }) {
                                        Icon(Icons.Default.ChevronRight, contentDescription = "Next year")
                                }
                        }
                },
                text = {
                        var dragAccumulator by remember { mutableFloatStateOf(0f) }
                        Column(
                                modifier = Modifier.pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                                onDragStart = { dragAccumulator = 0f },
                                                onHorizontalDrag = { _, delta -> dragAccumulator += delta },
                                                onDragEnd = {
                                                        if (dragAccumulator > 100f) pickerYear--
                                                        else if (dragAccumulator < -100f) pickerYear++
                                                        dragAccumulator = 0f
                                                }
                                        )
                                },
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                                for (row in 0..3) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                                for (col in 0..2) {
                                                        val monthIndex = row * 3 + col
                                                        val isSelected = pickerYear == selectedYear && monthIndex == selectedMonth
                                                        val isCurrent = pickerYear == currentYear && monthIndex == currentMonth
                                                        val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                        val textColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                        else MaterialTheme.colorScheme.onSurface
                                                        val borderMod = if (isCurrent && !isSelected)
                                                                Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                                        else Modifier

                                                        Surface(
                                                                onClick = { onMonthSelected(pickerYear, monthIndex) },
                                                                modifier = Modifier.weight(1f).then(borderMod),
                                                                shape = RoundedCornerShape(12.dp),
                                                                color = bgColor
                                                        ) {
                                                                Box(
                                                                        contentAlignment = Alignment.Center,
                                                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)
                                                                ) {
                                                                        Text(
                                                                                text = monthNames[monthIndex],
                                                                                style = MaterialTheme.typography.bodyMedium,
                                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                                                color = textColor,
                                                                                maxLines = 1,
                                                                                textAlign = TextAlign.Center
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
        )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeTransactionItem(
        transaction: Transaction,
        currencySymbol: String,
        runningBalance: Double,
        isSelectionMode: Boolean,
        isSelected: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit
) {
        val borderColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(200),
                label = "borderColor"
        )

        // Format amount once, not inside nested lambdas
        val amountText = remember(transaction.amount) {
                String.format("%.2f", transaction.amount)
        }
        val balanceText = remember(runningBalance) {
                String.format("%.2f", runningBalance)
        }
        val displayText = remember(transaction.originalSms, transaction.remark) {
                if (transaction.originalSms.isNotEmpty() && transaction.originalSms != "Manual entry")
                        transaction.originalSms.replace("\n", " ").replace("\r", " ").trim()
                else transaction.remark
        }

        Card(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .then(
                                if (isSelected) Modifier.border(2.dp, borderColor, RoundedCornerShape(20.dp))
                                else Modifier
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .combinedClickable(onClick = onClick, onLongClick = onLongClick),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
        ) {
                @OptIn(ExperimentalLayoutApi::class)
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Column(modifier = Modifier.weight(1f)) {
                                        FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                                // Category chip
                                                if (transaction.category.isNotEmpty()
                                                        && transaction.category != "Other Expense"
                                                        && transaction.category != "Other Income"
                                                ) {
                                                        val catColor = getCategoryColor(transaction.category)
                                                        Row(
                                                                modifier = Modifier
                                                                        .clip(RoundedCornerShape(13.dp))
                                                                        .background(catColor.copy(alpha = 0.15f))
                                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                                Icon(
                                                                        getCategoryIcon(transaction.category),
                                                                        contentDescription = null,
                                                                        modifier = Modifier.size(14.dp),
                                                                        tint = catColor
                                                                )
                                                                Text(
                                                                        text = transaction.category,
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = catColor
                                                                )
                                                        }
                                                }

                                                // Bank chip
// Bank / Instrument chip
                                                if (transaction.bankName.isNotEmpty()) {

                                                        val bankLabel = remember(
                                                                transaction.bankName,
                                                                transaction.accountLastFour,
                                                                transaction.instrumentType
                                                        ) {
                                                                when (transaction.instrumentType) {
                                                                        "CARD" ->
                                                                                "${transaction.bankName} • Card ${transaction.accountLastFour}"


                                                                        else ->
                                                                                if (transaction.accountLastFour.isNotEmpty())
                                                                                        "${transaction.bankName} • A/C ${transaction.accountLastFour}"
                                                                                else
                                                                                        transaction.bankName
                                                                }
                                                        }

                                                        val bankIcon = when (transaction.instrumentType) {
                                                                "CARD" -> Icons.Default.CreditCard
                                                                else -> Icons.Default.AccountBalance
                                                        }

                                                        Row(
                                                                modifier = Modifier
                                                                        .clip(RoundedCornerShape(13.dp))
                                                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                                Icon(
                                                                        bankIcon,
                                                                        contentDescription = null,
                                                                        modifier = Modifier.size(14.dp),
                                                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                                                )
                                                                Text(
                                                                        text = bankLabel,
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                                )
                                                        }
                                                }
                                                // Payment method chip
                                                if (transaction.paymentMethod.isNotEmpty()) {
                                                        val chipColor = getPaymentChipColor(transaction.paymentMethod)
                                                        Row(
                                                                modifier = Modifier
                                                                        .clip(RoundedCornerShape(13.dp))
                                                                        .background(chipColor.copy(alpha = 0.15f))
                                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                        ) {
                                                                when (transaction.paymentMethod) {
                                                                        "GPay" -> Image(
                                                                                painter = painterResource(R.drawable.gpay),
                                                                                contentDescription = "GPay",
                                                                                modifier = Modifier.size(14.dp)
                                                                        )
                                                                        "PhonePe" -> Image(
                                                                                painter = painterResource(R.drawable.phonepe),
                                                                                contentDescription = "PhonePe",
                                                                                modifier = Modifier.size(14.dp)
                                                                        )
                                                                        else -> Icon(
                                                                                getPaymentIcon(transaction.paymentMethod),
                                                                                contentDescription = null,
                                                                                modifier = Modifier.size(14.dp),
                                                                                tint = chipColor
                                                                        )
                                                                }
                                                                Text(
                                                                        text = transaction.paymentMethod,
                                                                        style = MaterialTheme.typography.labelMedium,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = chipColor
                                                                )
                                                        }
                                                }
                                        }

                                        if (displayText.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                        text = displayText,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                                text = "$currencySymbol $amountText",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (transaction.type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                                        )
                                        Text(
                                                text = "Bal: $currencySymbol $balanceText",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                }
                        }
                }
        }
}
package com.hussain.walletflow.ui.screens

import android.Manifest
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.hussain.walletflow.data.CurrencyData
import com.hussain.walletflow.data.Transaction
import com.hussain.walletflow.data.TransactionType
import com.hussain.walletflow.data.UserPreferencesRepository
import com.hussain.walletflow.ui.SmsScanner
import com.hussain.walletflow.ui.theme.ExpenseRed
import com.hussain.walletflow.ui.theme.IncomeGreen
import com.hussain.walletflow.utils.getCategoryIcon
import com.hussain.walletflow.utils.getPaymentIcon
import com.hussain.walletflow.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PassbookScreen(
        viewModel: TransactionViewModel,
        listState: LazyListState,
        onEditTransaction: (Long) -> Unit,
        onSelectionModeChanged: (Boolean) -> Unit = {}
) {
        val context = LocalContext.current
        val haptics = LocalHapticFeedback.current

        val prefsRepository = remember { UserPreferencesRepository(context) }
        val selectedCurrency by prefsRepository.currencyFlow.collectAsState(
                initial = UserPreferencesRepository.DEFAULT_CURRENCY
        )
        val deleteFromPassbook by prefsRepository.deleteFromPassbookFlow.collectAsState(initial = true)

        val currency = remember(selectedCurrency) {
                CurrencyData.currencies.find { it.code == selectedCurrency }
                        ?: CurrencyData.currencies.first()
        }

        val transactions by viewModel.passbookTransactions.collectAsState()

        var showFilterDialog   by remember { mutableStateOf(false) }
        var showCopyMonthPicker by remember { mutableStateOf(false) }
        var selectedFilter     by remember { mutableStateOf(PassbookFilter()) }
        var selectedMonth      by remember { mutableStateOf<String?>(null) } // null = "All"

        // ── Month labels derived from passbook transactions ───────────────────
        val monthLabelFormat = remember { SimpleDateFormat("MMM yy",  Locale.getDefault()) }
        val monthSortFormat  = remember { SimpleDateFormat("yyyyMM",  Locale.getDefault()) }

        val availableMonths = remember(transactions) {
                transactions
                        .map { monthSortFormat.format(Date(it.date)) to monthLabelFormat.format(Date(it.date)) }
                        .distinctBy { it.first }
                        .sortedByDescending { it.first }
                        .map { it.second }
        }

        // ── Filtered list (filter sheet + month selector both applied) ────────
        val filteredTransactions by remember(transactions, selectedFilter, selectedMonth) {
                derivedStateOf {
                        var list = transactions

                        // Filter-sheet filters
                        list = when (selectedFilter.transactionType) {
                                "Income"  -> list.filter { it.type == TransactionType.INCOME }
                                "Expense" -> list.filter { it.type == TransactionType.EXPENSE }
                                else      -> list
                        }
                        if (selectedFilter.selectedAccountIds.isNotEmpty()) {
                                list = list.filter {
                                        "${it.bankName}_${it.accountLastFour}" in selectedFilter.selectedAccountIds
                                }
                        }
                        if (selectedFilter.startDateMs != null) {
                                list = list.filter { it.date >= selectedFilter.startDateMs!! }
                        }
                        if (selectedFilter.endDateMs != null) {
                                list = list.filter { it.date <= selectedFilter.endDateMs!! + 86_399_999L }
                        }

                        // Month selector filter
                        if (selectedMonth != null) {
                                list = list.filter {
                                        monthLabelFormat.format(Date(it.date)) == selectedMonth
                                }
                        }

                        list
                }
        }

        // ── Account chips ─────────────────────────────────────────────────────
        val accounts = remember(transactions) {
                val uniqueAccounts = transactions
                        .distinctBy { it.accountLastFour + it.bankName + it.instrumentType }
                        .map { txn ->
                                AccountChip(
                                        id    = "${txn.bankName}_${txn.accountLastFour}",
                                        label = if (txn.accountLastFour.isNotEmpty())
                                                "${txn.bankName} ••${txn.accountLastFour}"
                                        else txn.bankName,
                                        icon  = if (txn.instrumentType == "CARD") Icons.Default.CreditCard
                                        else Icons.Default.AccountBalance
                                )
                        }
                listOf(AccountChip("all", "All", Icons.Default.Wallet, isAll = true)) + uniqueAccounts
        }

        val selectedIds = remember { mutableStateSetOf<Long>() }
        var isSelectionMode by remember { mutableStateOf(false) }

        LaunchedEffect(isSelectionMode) { onSelectionModeChanged(isSelectionMode) }
        LaunchedEffect(selectedIds.size) {
                if (isSelectionMode && selectedIds.isEmpty()) isSelectionMode = false
        }
        BackHandler(enabled = isSelectionMode) {
                selectedIds.clear()
                isSelectionMode = false
        }

        val allSelected by remember {
                derivedStateOf {
                        filteredTransactions.isNotEmpty() && selectedIds.size == filteredTransactions.size
                }
        }

        val dateGroupFormat = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
        val dateLabelFormat = remember { SimpleDateFormat("MMM d  EEEE", Locale.getDefault()) }

        val groupedTransactions = remember(filteredTransactions) {
                filteredTransactions
                        .sortedByDescending { it.date }
                        .groupBy { dateGroupFormat.format(Date(it.date)) }
                        .map { (_, txns) -> dateLabelFormat.format(Date(txns.first().date)) to txns }
        }

        // ── SMS permissions ───────────────────────────────────────────────────
        val smsPermissions = rememberMultiplePermissionsState(
                permissions = listOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
        )
        var permissionJustRequested by remember { mutableStateOf(false) }
        val initialScanDone by viewModel.initialScanDone.collectAsState()

        LaunchedEffect(initialScanDone) {
                if (!initialScanDone && smsPermissions.allPermissionsGranted) {
                        viewModel.markInitialScanDone()
                        context.startService(Intent(context, SmsScanner::class.java))
                }
        }
        LaunchedEffect(smsPermissions.allPermissionsGranted) {
                if (smsPermissions.allPermissionsGranted && permissionJustRequested) {
                        permissionJustRequested = false
                        viewModel.markInitialScanDone()
                        context.startService(Intent(context, SmsScanner::class.java))
                }
        }

        val surfaceColor = MaterialTheme.colorScheme.surface

        // ── UI ────────────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {

                if (isSelectionMode) {
                        SelectionHeader(
                                selectedCount = selectedIds.size,
                                allSelected = allSelected,
                                onToggleSelectAll = {
                                        if (allSelected) selectedIds.clear()
                                        else selectedIds.addAll(filteredTransactions.map { it.id })
                                },
                                onDeleteSelected = {
                                        viewModel.deleteTransactionsByIds(selectedIds.toList())
                                        selectedIds.clear()
                                        isSelectionMode = false
                                }
                        )
                } else {
                        PassbookHeader(
                                selectedFilter = selectedFilter,
                                onFilterClick  = { showFilterDialog = true },
                                onRefreshClick = {
                                        if (smsPermissions.allPermissionsGranted) {
                                                context.startService(
                                                        Intent(context, SmsScanner::class.java)
                                                                .putExtra(SmsScanner.EXTRA_MANUAL_REFRESH, true)
                                                )
                                        } else {
                                                permissionJustRequested = true
                                                smsPermissions.launchMultiplePermissionRequest()
                                        }
                                }
                        )

                        // ── Month selector OR custom date range card ─────────
                        val hasDateRange = selectedFilter.dateRangeLabel != null
                        if (hasDateRange) {
                                CustomDateRangeBadge(
                                        label    = selectedFilter.dateRangeLabel!!,
                                        onClear  = {
                                                selectedFilter = selectedFilter.copy(
                                                        dateRangeLabel = null,
                                                        startDateMs    = null,
                                                        endDateMs      = null
                                                )
                                        },
                                        onTap    = { showFilterDialog = true }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                        } else if (availableMonths.isNotEmpty()) {
                                MonthSelectorRow(
                                        availableMonths = availableMonths,
                                        selectedMonth   = selectedMonth,
                                        onMonthSelected = { selectedMonth = it }
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                        }
                }

                if (filteredTransactions.isEmpty() && !isSelectionMode) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text(
                                                        text = if (selectedFilter.transactionType != "All")
                                                                "No ${selectedFilter.transactionType} transactions"
                                                        else "No transactions yet",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                        text  = "Tap refresh to scan your SMS",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                        }
                                }
                        }
                } else {
                        Box(modifier = Modifier.weight(1f)) {
                                LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        state    = listState,
                                        contentPadding = PaddingValues(bottom = 80.dp)
                                ) {
                                        groupedTransactions.forEach { (dateLabel, dayTransactions) ->

                                                @OptIn(ExperimentalFoundationApi::class)
                                                stickyHeader(key = "date_$dateLabel") {
                                                        Surface(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                color    = surfaceColor
                                                        ) {
                                                                Text(
                                                                        text  = dateLabel,
                                                                        style = MaterialTheme.typography.titleSmall,
                                                                        fontWeight = FontWeight.SemiBold,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                        modifier = Modifier.padding(
                                                                                horizontal = 16.dp, vertical = 8.dp
                                                                        )
                                                                )
                                                        }
                                                }

                                                items(dayTransactions, key = { it.id }) { transaction ->
                                                        val isSelected = selectedIds.contains(transaction.id)
                                                        TransactionCard(
                                                                transaction    = transaction,
                                                                currencySymbol = currency.symbol,
                                                                isSelected     = isSelected,
                                                                isSelectionMode = isSelectionMode,
                                                                onClick = {
                                                                        if (isSelectionMode) {
                                                                                if (isSelected) selectedIds.remove(transaction.id)
                                                                                else            selectedIds.add(transaction.id)
                                                                        } else {
                                                                                onEditTransaction(transaction.id)
                                                                        }
                                                                },
                                                                onLongClick = {
                                                                        if (!isSelectionMode) {
                                                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                                isSelectionMode = true
                                                                                selectedIds.add(transaction.id)
                                                                        }
                                                                },
                                                                onAddToMonthly = {
                                                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                                        if (deleteFromPassbook) viewModel.addToMonthly(transaction.id)
                                                                        else viewModel.addToMonthlyKeepOriginal(transaction.id)
                                                                },
                                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                                                        )
                                                }
                                        }
                                }

                                if (isSelectionMode) {
                                        Box(
                                                modifier = Modifier
                                                        .align(Alignment.BottomCenter)
                                                        .fillMaxWidth()
                                                        .height(32.dp)
                                                        .background(
                                                                Brush.verticalGradient(listOf(Color.Transparent, surfaceColor))
                                                        )
                                        )
                                }
                        }

                        if (isSelectionMode) {
                                Surface(modifier = Modifier.fillMaxWidth(), color = surfaceColor) {
                                        Row(
                                                modifier = Modifier
                                                        .fillMaxWidth()
                                                        .navigationBarsPadding()
                                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                                Button(
                                                        onClick = { showCopyMonthPicker = true },
                                                        modifier = Modifier.weight(1f),
                                                        colors   = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                                contentColor   = MaterialTheme.colorScheme.onSecondaryContainer
                                                        )
                                                ) {
                                                        Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text("Copy to", fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                        onClick = {
                                                                val ids = selectedIds.toList()
                                                                if (deleteFromPassbook) viewModel.addMultipleToMonthly(ids)
                                                                else viewModel.addMultipleToMonthlyKeepOriginals(ids)
                                                                selectedIds.clear()
                                                                isSelectionMode = false
                                                        },
                                                        modifier = Modifier.weight(1f),
                                                        colors   = ButtonDefaults.buttonColors(
                                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                                contentColor   = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                ) {
                                                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                                                        Spacer(Modifier.width(6.dp))
                                                        Text("Add", fontWeight = FontWeight.Bold)
                                                }
                                        }
                                }
                        }
                }
        }

        // ── Dialogs ───────────────────────────────────────────────────────────
        if (showFilterDialog) {
                FilterBottomSheet(
                        currentFilter = selectedFilter,
                        accounts      = accounts,
                        onApply       = { newFilter ->
                                selectedFilter   = newFilter
                                showFilterDialog = false
                        },
                        onDismiss = { showFilterDialog = false }
                )
        }

        if (showCopyMonthPicker) {
                val now = remember { Calendar.getInstance() }
                CopyToMonthDialog(
                        currentYear  = now.get(Calendar.YEAR),
                        currentMonth = now.get(Calendar.MONTH),
                        onMonthSelected = { year, month ->
                                val selected = filteredTransactions.filter { it.id in selectedIds }
                                viewModel.copyTransactionsToMonth(selected, year, month)
                                selectedIds.clear()
                                isSelectionMode     = false
                                showCopyMonthPicker = false
                        },
                        onDismiss = { showCopyMonthPicker = false }
                )
        }
}

// ── Custom date range badge — replaces month chips when a range is set ───────
@Composable
fun CustomDateRangeBadge(
        label: String,      // e.g. "14 Mar – 18 Mar"
        onClear: () -> Unit,
        onTap: () -> Unit
) {
        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
        ) {
                Surface(
                        onClick        = onTap,
                        modifier       = Modifier.weight(1f),
                        shape          = RoundedCornerShape(14.dp),
                        color          = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        tonalElevation = 0.dp
                ) {
                        Row(
                                modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                                text  = "Custom Date Range",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                        Text(
                                                text  = label,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                }
                                IconButton(
                                        onClick  = onClear,
                                        modifier = Modifier.size(32.dp)
                                ) {
                                        Icon(
                                                imageVector        = Icons.Default.Close,
                                                contentDescription = "Clear date range",
                                                modifier           = Modifier.size(18.dp),
                                                tint               = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                }
                        }
                }
        }
}

// ── Month selector row ────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthSelectorRow(
        availableMonths: List<String>,  // e.g. ["Mar 25", "Feb 25", …] newest first
        selectedMonth: String?,         // null = "All"
        onMonthSelected: (String?) -> Unit
) {
        val options = listOf("All") + availableMonths
        val count   = options.size

        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
        LazyRow(
                modifier              = Modifier.fillMaxWidth(),
                contentPadding        = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
        ) {
                itemsIndexed(options) { index, option ->
                        val isSelected = if (option == "All") selectedMonth == null
                        else selectedMonth == option

                        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                        val shapes = when {
                                count == 1         -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                index == 0         -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else               -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }

                        @OptIn(ExperimentalMaterial3ExpressiveApi::class)
                        ToggleButton(
                                checked         = isSelected,
                                onCheckedChange = { if (it) onMonthSelected(if (option == "All") null else option) },
                                shapes          = shapes,
                                colors          = ToggleButtonDefaults.toggleButtonColors(
                                        checkedContainerColor = MaterialTheme.colorScheme.primary,
                                        checkedContentColor   = MaterialTheme.colorScheme.onPrimary,
                                        containerColor        = MaterialTheme.colorScheme.tertiaryContainer,
                                        contentColor          = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                        ) {
                                Text(
                                        text       = option,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize   = 13.sp
                                )
                        }
                }
        }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
fun SelectionHeader(
        selectedCount: Int,
        allSelected: Boolean,
        onToggleSelectAll: () -> Unit,
        onDeleteSelected: () -> Unit
) {
        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        val iconTint by animateColorAsState(
                                targetValue   = if (allSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(300),
                                label         = "selectAllColor"
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = onToggleSelectAll, modifier = Modifier.size(36.dp)) {
                                        Icon(
                                                imageVector        = if (allSelected) Icons.Filled.CheckCircle
                                                else Icons.Outlined.Circle,
                                                contentDescription = "Select all",
                                                tint               = iconTint
                                        )
                                }
                                Text("All", style = MaterialTheme.typography.labelSmall, color = iconTint)
                        }
                        Text(
                                text       = "$selectedCount selected",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                        )
                }
                IconButton(
                        onClick  = onDeleteSelected,
                        modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .size(40.dp)
                ) {
                        Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete selected",
                                tint               = MaterialTheme.colorScheme.onErrorContainer
                        )
                }
        }
}

@Composable
fun PassbookHeader(
        selectedFilter: PassbookFilter,
        onFilterClick: () -> Unit,
        onRefreshClick: () -> Unit
) {
        val isFilterActive = selectedFilter.transactionType != "All"
                || selectedFilter.selectedAccountIds.isNotEmpty()
                || selectedFilter.dateRangeLabel != null

        Row(
                modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Text(
                        "Passbook",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                )
                Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                        val filterBg by animateColorAsState(
                                targetValue   = if (isFilterActive) MaterialTheme.colorScheme.tertiaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                animationSpec = tween(300),
                                label         = "filterBg"
                        )
                        val filterIconTint by animateColorAsState(
                                targetValue   = if (isFilterActive) MaterialTheme.colorScheme.onTertiaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                animationSpec = tween(300),
                                label         = "filterIcon"
                        )
                        IconButton(
                                onClick  = onFilterClick,
                                modifier = Modifier
                                        .clip(CircleShape)
                                        .background(filterBg)
                                        .size(40.dp)
                        ) { Icon(Icons.Default.FilterList, contentDescription = "Filter", tint = filterIconTint) }
                        IconButton(
                                onClick  = onRefreshClick,
                                modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .size(40.dp)
                        ) { Icon(Icons.Default.Refresh, contentDescription = "Scan SMS") }
                }
        }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionCard(
        transaction: Transaction,
        currencySymbol: String,
        isSelected: Boolean,
        isSelectionMode: Boolean,
        onClick: () -> Unit,
        onLongClick: () -> Unit,
        onAddToMonthly: () -> Unit,
        modifier: Modifier = Modifier
) {
        val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
        val timeText   = remember(transaction.date)   { timeFormat.format(Date(transaction.date)) }
        val amountText = remember(transaction.amount) { String.format("%.2f", transaction.amount) }
        val bankLabel  = remember(transaction.bankName, transaction.accountLastFour, transaction.instrumentType) {
                when (transaction.instrumentType) {
                        "CARD" -> "${transaction.bankName} • Card ${transaction.accountLastFour}"
                        else   -> if (transaction.accountLastFour.isNotEmpty())
                                "${transaction.bankName} • A/C ${transaction.accountLastFour}"
                        else transaction.bankName
                }
        }
        val smsText = remember(transaction.originalSms) {
                transaction.originalSms.replace("\n", " ").replace("\r", " ").trim()
        }

        val borderColor by animateColorAsState(
                targetValue   = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(200),
                label         = "borderColor"
        )

        Card(
                modifier = modifier
                        .fillMaxWidth()
                        .then(
                                if (isSelected) Modifier.border(2.dp, borderColor, RoundedCornerShape(20.dp))
                                else Modifier
                        )
                        .clip(RoundedCornerShape(20.dp))
                        .combinedClickable(onClick = onClick, onLongClick = onLongClick),
                shape  = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
        ) {
                Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {

                        Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                        ) {
                                Row(
                                        modifier = Modifier
                                                .clip(RoundedCornerShape(13.dp))
                                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment     = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                        val showCategory = transaction.category.isNotEmpty()
                                                && transaction.category != "Other Expense"
                                                && transaction.category != "Other Income"

                                        if (showCategory) {
                                                Icon(getCategoryIcon(transaction.category), null,
                                                        Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                                Text(transaction.category,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary)
                                                Text("•", style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }

                                        Icon(Icons.Default.AccountBalance, null,
                                                Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                        Text(bankLabel, style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSecondaryContainer)

                                        if (transaction.paymentMethod.isNotEmpty()) {
                                                Text("•", style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Icon(getPaymentIcon(transaction.paymentMethod), null,
                                                        Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                                                Text(transaction.paymentMethod,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.tertiary)
                                        }
                                }

                                Text(
                                        "$currencySymbol $amountText",
                                        style      = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color      = if (transaction.type == TransactionType.EXPENSE) ExpenseRed else IncomeGreen
                                )
                        }

                        if (smsText.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(smsText, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }

                        Spacer(Modifier.height(8.dp))

                        Row(
                                modifier              = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                        ) {
                                Text(timeText, style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)

                                if (!isSelectionMode) {
                                        Row(
                                                modifier = Modifier
                                                        .clip(RoundedCornerShape(13.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                                        .clickable(onClick = onAddToMonthly)
                                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                                verticalAlignment     = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                                Icon(Icons.Default.Add, null, Modifier.size(16.dp),
                                                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                                Text("ADD", style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                } else {
                                        val checkColor by animateColorAsState(
                                                targetValue   = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                animationSpec = tween(200),
                                                label         = "checkColor"
                                        )
                                        Icon(
                                                imageVector        = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                                                contentDescription = if (isSelected) "Selected" else "Not selected",
                                                tint               = checkColor,
                                                modifier           = Modifier.size(24.dp)
                                        )
                                }
                        }
                }
        }
}

@Composable
fun CopyToMonthDialog(
        currentYear: Int,
        currentMonth: Int,
        onMonthSelected: (year: Int, month: Int) -> Unit,
        onDismiss: () -> Unit
) {
        var pickerYear by remember { mutableIntStateOf(currentYear) }
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
                        Column {
                                Text("Copy to Month", style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically) {
                                        IconButton(onClick = { pickerYear-- }) {
                                                Icon(Icons.Default.ChevronLeft, "Previous year")
                                        }
                                        Text(pickerYear.toString(),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                textAlign  = TextAlign.Center)
                                        IconButton(onClick = { pickerYear++ }) {
                                                Icon(Icons.Default.ChevronRight, "Next year")
                                        }
                                }
                        }
                },
                text = {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (row in 0..3) {
                                        Row(modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                for (col in 0..2) {
                                                        val monthIndex = row * 3 + col
                                                        val isCurrent  = pickerYear == currentYear && monthIndex == currentMonth
                                                        Surface(
                                                                onClick   = { onMonthSelected(pickerYear, monthIndex) },
                                                                modifier  = Modifier.weight(1f),
                                                                shape     = RoundedCornerShape(12.dp),
                                                                color     = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                                        ) {
                                                                Box(contentAlignment = Alignment.Center,
                                                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp)) {
                                                                        Text(monthNames[monthIndex],
                                                                                style      = MaterialTheme.typography.bodyMedium,
                                                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                                                textAlign  = TextAlign.Center,
                                                                                maxLines   = 1)
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
}
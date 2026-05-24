package com.hussain.walletflow.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hussain.walletflow.R
import com.hussain.walletflow.data.TransactionType
import com.hussain.walletflow.ui.theme.ExpenseRed
import com.hussain.walletflow.ui.theme.IncomeGreen
import com.hussain.walletflow.utils.FormatUtils
import com.hussain.walletflow.utils.ParsedTransaction
import com.hussain.walletflow.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private fun formatIndianAmount(amount: Double): String = FormatUtils.formatIndianAmount(amount)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportPreviewScreen(
        viewModel: TransactionViewModel,
        parsedTransactions: List<ParsedTransaction>,
        initialTargetYear: Int,
        initialTargetMonth: Int,
        onImportDone: () -> Unit,
        onBack: () -> Unit
) {
        // ── Selection state ──────────────────────────────────────────────────────
        val selectedIndices = remember { mutableStateOf(parsedTransactions.indices.toHashSet()) }
        val allSelected by remember(selectedIndices.value, parsedTransactions.size) {
                derivedStateOf { selectedIndices.value.size == parsedTransactions.size }
        }

        // ── Month state (destination for import) ─────────────────────────────────
        var targetYear by remember { mutableIntStateOf(initialTargetYear) }
        var targetMonth by remember { mutableIntStateOf(initialTargetMonth) }
        var showMonthPicker by remember { mutableStateOf(false) }

        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonthIndex = now.get(Calendar.MONTH)

        val displayMonth = remember(targetYear, targetMonth) {
                val cal = Calendar.getInstance().apply {
                        set(Calendar.YEAR, targetYear)
                        set(Calendar.MONTH, targetMonth)
                }
                if (targetYear == currentYear)
                        SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)
                else SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(cal.time)
        }

        val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

        // Helper to format date with year safety
        val formatParsedDate = remember {
            { timestamp: Long ->
                val cal = Calendar.getInstance()
                cal.timeInMillis = timestamp
                if (cal.get(Calendar.YEAR) < 100) {
                    cal.set(Calendar.YEAR, 2000 + cal.get(Calendar.YEAR))
                }
                dateFormat.format(cal.time)
            }
        }

        // Derive selected totals for bottom bar summary
        val selectedIncome by remember(selectedIndices.value) {
                derivedStateOf {
                        selectedIndices.value.sumOf { i ->
                                val t = parsedTransactions[i]
                                if (t.type == TransactionType.INCOME) t.amount else 0.0
                        }
                }
        }
        val selectedExpense by remember(selectedIndices.value) {
                derivedStateOf {
                        selectedIndices.value.sumOf { i ->
                                val t = parsedTransactions[i]
                                if (t.type == TransactionType.EXPENSE) t.amount else 0.0
                        }
                }
        }

        Scaffold(
                topBar = {
                        TopAppBar(
                                title = {
                                        Text(
                                                "Import Preview",
                                                fontWeight = FontWeight.Bold
                                        )
                                },
                                navigationIcon = {
                                        FilledIconButton(
                                                modifier = Modifier.padding(12.dp),
                                                onClick = onBack,
                                                colors = IconButtonDefaults.filledIconButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                                        contentColor = MaterialTheme.colorScheme.onSurface
                                                )
                                        ) {
                                                Icon(
                                                        Icons.Default.ArrowBack,
                                                        contentDescription = "Back",
                                                        modifier = Modifier.size(24.dp)
                                                )
                                        }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                )
                        )
                },
                bottomBar = {
                        ImportBottomBar(
                                selectedCount = selectedIndices.value.size,
                                targetMonth = displayMonth,
                                selectedIncome = selectedIncome,
                                selectedExpense = selectedExpense,
                                onImportClick = {
                                        val selected = selectedIndices.value.map { parsedTransactions[it] }
                                        viewModel.importParsedTransactions(selected, targetYear, targetMonth)
                                        viewModel.clearPendingImport()
                                        onImportDone()
                                },
                                onMonthClick = { showMonthPicker = true }
                        )
                }
        ) { innerPadding ->
                if (parsedTransactions.isEmpty()) {
                        Box(
                                modifier = Modifier.fillMaxSize().padding(innerPadding),
                                contentAlignment = Alignment.Center
                        ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                                Icons.Default.FileOpen,
                                                contentDescription = null,
                                                modifier = Modifier.size(64.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                                "No transactions could be parsed",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                                "Check that the file has the right headers",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                        )
                                }
                        }
                        return@Scaffold
                }

                LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                        // ── Select All header ────────────────────────────────────────────
                        item(key = "select_all_header") {
                                Row(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                        selectedIndices.value =
                                                                if (allSelected) hashSetOf()
                                                                else parsedTransactions.indices.toHashSet()
                                                }
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        val checkColor by animateColorAsState(
                                                targetValue = if (allSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                animationSpec = tween(200),
                                                label = "checkAllColor"
                                        )
                                        Icon(
                                                imageVector = if (allSelected) Icons.Default.CheckCircle
                                                else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = "Select All",
                                                tint = checkColor,
                                                modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                                text = if (allSelected) "Deselect All"
                                                else "Select All (${parsedTransactions.size})",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.weight(1f))
                                        // Subtle selected count — only shown when partial selection
                                        if (!allSelected && selectedIndices.value.isNotEmpty()) {
                                                Text(
                                                        text = "${selectedIndices.value.size} selected",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                        }
                                }
                        }

                        // ── Top Summary Card ─────────────────────────────────────────────
                        item {
                                val incomeItems = parsedTransactions.filter { it.type == TransactionType.INCOME }
                                val expenseItems = parsedTransactions.filter { it.type == TransactionType.EXPENSE }
                                val incomeTotal = incomeItems.sumOf { it.amount }
                                val expenseTotal = expenseItems.sumOf { it.amount }

                                Surface(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 4.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
                                        shape = RoundedCornerShape(20.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                ) {
                                        Row(
                                                modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(6.dp) // Gap from outer border
                                                        .height(IntrinsicSize.Min),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp) // Gap between blocks
                                        ) {
                                                val sectionBg = MaterialTheme.colorScheme.surface

                                                // Income Section
                                                Column(
                                                        modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 4.dp, bottomEnd = 4.dp))
                                                                .background(sectionBg)
                                                                .padding(vertical = 12.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                        Text(
                                                                "Income",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = IncomeGreen
                                                        )
                                                        Spacer(Modifier.height(2.dp))
                                                        Text(
                                                                "₹${formatIndianAmount(incomeTotal)}",
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                                "${incomeItems.size} txn",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                }

                                                // Expense Section
                                                Column(
                                                        modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(4.dp))
                                                                .background(sectionBg)
                                                                .padding(vertical = 12.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                        Text(
                                                                "Expense",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = ExpenseRed
                                                        )
                                                        Spacer(Modifier.height(2.dp))
                                                        Text(
                                                                "₹${formatIndianAmount(expenseTotal)}",
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                        Text(
                                                                "${expenseItems.size} txn",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                }

                                                // Net Section
                                                Column(
                                                        modifier = Modifier
                                                                .weight(1f)
                                                                .clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp, topStart = 4.dp, bottomStart = 4.dp))
                                                                .background(sectionBg)
                                                                .padding(vertical = 12.dp),
                                                        horizontalAlignment = Alignment.CenterHorizontally
                                                ) {
                                                        Text(
                                                                "Net",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Spacer(Modifier.height(2.dp))
                                                        Text(
                                                                "₹${formatIndianAmount(incomeTotal - expenseTotal)}",
                                                                style = MaterialTheme.typography.bodyLarge,
                                                                fontWeight = FontWeight.Bold,
                                                                color = if (incomeTotal >= expenseTotal) IncomeGreen else ExpenseRed
                                                        )
                                                        Text(
                                                                "${parsedTransactions.size} total",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                }
                                        }
                                }
                        }

                        items(
                                items = parsedTransactions,
                                key = { "${it.date}_${it.amount}_${it.narration}_${parsedTransactions.indexOf(it)}" }
                        ) { parsed ->
                                val index = parsedTransactions.indexOf(parsed)
                                val isSelected = selectedIndices.value.contains(index)

                                ImportTransactionCard(
                                        parsed = parsed,
                                        isSelected = isSelected,
                                        dateText = formatParsedDate(parsed.date),
                                        onClick = {
                                                selectedIndices.value =
                                                        if (isSelected) selectedIndices.value.minus(index).toHashSet()
                                                        else selectedIndices.value.plus(index).toHashSet()
                                        }
                                )
                        }
                }
        }

        // ── Month Picker Dialog ──────────────────────────────────────────────────
        if (showMonthPicker) {
                MonthPickerDialog(
                        selectedYear = targetYear,
                        selectedMonth = targetMonth,
                        currentYear = currentYear,
                        currentMonth = currentMonthIndex,
                        onMonthSelected = { year, month ->
                                targetYear = year
                                targetMonth = month
                                showMonthPicker = false
                        },
                        onDismiss = { showMonthPicker = false }
                )
        }
}

// ── Import Card ──────────────────────────────────────────────────────────────

@Composable
private fun ImportTransactionCard(
        parsed: ParsedTransaction,
        isSelected: Boolean,
        dateText: String,
        onClick: () -> Unit
) {
        val isIncome = parsed.type == TransactionType.INCOME
        val amountColor = if (isIncome) IncomeGreen else ExpenseRed
        val prefix = if (isIncome) "+" else "-"

        val borderColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                animationSpec = tween(200),
                label = "border"
        )
        val bgColor by animateColorAsState(
                targetValue = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                animationSpec = tween(200),
                label = "cardBg"
        )

        Card(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.5.dp, borderColor),
                colors = CardDefaults.cardColors(containerColor = bgColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
                Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                        Icon(
                                imageVector = if (isSelected) Icons.Filled.CheckCircle
                                else Icons.Default.RadioButtonUnchecked,
                                contentDescription = if (isSelected) "Selected" else "Unselected",
                                tint = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                                Text(
                                        text = parsed.narration,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(3.dp))
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                        Text(
                                                text = dateText,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        // Category chip — shown when we have the original category
                                        if (parsed.category.isNotBlank()) {
                                                Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = amountColor.copy(alpha = 0.12f)
                                                ) {
                                                        Text(
                                                                text = parsed.category,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = amountColor,
                                                                fontWeight = FontWeight.Medium,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                }
                                        }
                                        // Payment method chip
                                        if (parsed.paymentMethod.isNotBlank()) {
                                                Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = MaterialTheme.colorScheme.secondaryContainer
                                                ) {
                                                        Text(
                                                                text = parsed.paymentMethod,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                        )
                                                }
                                        }
                                }
                        }
                        Text(
                                text = "$prefix ₹${formatIndianAmount(parsed.amount)}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = amountColor
                        )
                }
        }
}

// ── Bottom Bar ───────────────────────────────────────────────────────────────

@Composable
private fun ImportBottomBar(
        selectedCount: Int,
        targetMonth: String,
        selectedIncome: Double,
        selectedExpense: Double,
        onImportClick: () -> Unit,
        onMonthClick: () -> Unit
) {
        Surface(
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
        ) {
                Column(
                        modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                        // ── Destination + mini summary row ───────────────────────────────
                        // Single unified card that groups destination month with a snapshot
                        // of what's about to be imported.
                        Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Row(
                                        modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        // Left: label + selected totals
                                        Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                        "Importing to",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                if (selectedCount > 0) {
                                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                                if (selectedIncome > 0) {
                                                                        Text(
                                                                                "+₹${formatIndianAmount(selectedIncome)}",
                                                                                style = MaterialTheme.typography.labelMedium,
                                                                                fontWeight = FontWeight.SemiBold,
                                                                                color = IncomeGreen
                                                                        )
                                                                }
                                                                if (selectedExpense > 0) {
                                                                        Text(
                                                                                "-₹${formatIndianAmount(selectedExpense)}",
                                                                                style = MaterialTheme.typography.labelMedium,
                                                                                fontWeight = FontWeight.SemiBold,
                                                                                color = ExpenseRed
                                                                        )
                                                                }
                                                        }
                                                } else {
                                                        Text(
                                                                "No transactions selected",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                        )
                                                }
                                        }

                                        // Right: tappable month pill
                                        Row(
                                                modifier = Modifier
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.surface)
                                                        .clickable(onClick = onMonthClick)
                                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                                Icon(
                                                        painter = painterResource(R.drawable.calendar_month),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                        text = targetMonth,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.primary
                                                )
                                                Icon(
                                                        Icons.Default.ArrowDropDown,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                )
                                        }
                                }
                        }

                        // ── Import button ────────────────────────────────────────────────
                        Button(
                                onClick = onImportClick,
                                enabled = selectedCount > 0,
                                modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                shape = RoundedCornerShape(14.dp)
                        ) {
                                Icon(
                                        Icons.Default.FileDownload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text = if (selectedCount > 0)
                                                "Import $selectedCount transaction${if (selectedCount > 1) "s" else ""}"
                                        else "Select transactions to import",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                )
                        }
                }
        }
}
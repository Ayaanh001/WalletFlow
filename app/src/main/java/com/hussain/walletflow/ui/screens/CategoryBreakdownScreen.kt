package com.hussain.walletflow.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hussain.walletflow.R
import com.hussain.walletflow.data.CurrencyData
import com.hussain.walletflow.data.TransactionType
import com.hussain.walletflow.data.UserPreferencesRepository
import com.hussain.walletflow.ui.theme.ExpenseRed
import com.hussain.walletflow.ui.theme.IncomeGreen
import com.hussain.walletflow.utils.getCategoryColor
import com.hussain.walletflow.utils.getCategoryIcon
import com.hussain.walletflow.utils.getPaymentChipColor
import com.hussain.walletflow.utils.getPaymentIcon
import com.hussain.walletflow.viewmodel.TransactionViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.sqrt

// ── Formatters ────────────────────────────────────────────────────────────────

private val indianFmt: NumberFormat by lazy {
    NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
}

private fun fmt(v: Double): String = indianFmt.format(v)

// ── Data models ───────────────────────────────────────────────────────────────

data class PieSegment(
    val category: String,
    val amount: Double,
    val color: Color,
    val fraction: Float
)

private enum class BreakdownTab { CATEGORIES, PAYMENT_METHODS }

private val BreakdownTabSaver = androidx.compose.runtime.saveable.Saver<BreakdownTab, String>(
    save    = { it.name },
    restore = { BreakdownTab.valueOf(it) }
)

// ── Main screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryBreakdownScreen(
    viewModel: TransactionViewModel,
    isExpense: Boolean,
    initialYear: Int,
    initialMonth: Int,
    onBack: () -> Unit,
    onCategoryClick: (category: String, year: Int, month: Int) -> Unit,
    onPaymentMethodClick: (paymentMethod: String, year: Int, month: Int) -> Unit = { _, _, _ -> },
    onAddTransaction: () -> Unit = {}
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
    val filteredType = if (isExpense) TransactionType.EXPENSE else TransactionType.INCOME
    val accentColor = if (isExpense) ExpenseRed else IncomeGreen
    val label = if (isExpense) "Expenses" else "Income"
    val centerIcon: ImageVector =
        if (isExpense) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward

    // ── Category totals ───────────────────────────────────────────────────────
    val categoryTotals = remember(monthlyTransactions, startOfMonth, endOfMonth, isExpense) {
        monthlyTransactions
            .filter { it.date in startOfMonth..endOfMonth && it.type == filteredType }
            .groupBy { it.category.ifEmpty { if (isExpense) "Other Expense" else "Other Income" } }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }.toList()
    }

    // ── Payment method totals ─────────────────────────────────────────────────
    val paymentMethodTotals = remember(monthlyTransactions, startOfMonth, endOfMonth, isExpense) {
        monthlyTransactions
            .filter { it.date in startOfMonth..endOfMonth && it.type == filteredType }
            .groupBy { it.paymentMethod.ifEmpty { "Other" } }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
            .entries.sortedByDescending { it.value }.toList()
    }

    val categoryTotal = remember(categoryTotals) { categoryTotals.sumOf { it.value } }
    val paymentTotal  = remember(paymentMethodTotals) { paymentMethodTotals.sumOf { it.value } }

    // ── Tab + selection state ─────────────────────────────────────────────────
    // rememberSaveable survives back-stack pop (returning from transactions screen)
    // so the user lands back on whichever tab they came from.
    var selectedTab by rememberSaveable(stateSaver = BreakdownTabSaver) { mutableStateOf(BreakdownTab.CATEGORIES) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedPaymentMethod by remember { mutableStateOf<String?>(null) }

    // Clear selections on month change
    LaunchedEffect(startOfMonth) {
        selectedCategory = null
        selectedPaymentMethod = null
    }
    // Clear selections when switching tabs
    LaunchedEffect(selectedTab) {
        selectedCategory = null
        selectedPaymentMethod = null
    }

    // ── Active pie segments — switches with the tab ───────────────────────────
    val activePieSegments = remember(selectedTab, categoryTotals, paymentMethodTotals, categoryTotal, paymentTotal) {
        when (selectedTab) {
            BreakdownTab.CATEGORIES -> categoryTotals.map { (cat, amount) ->
                PieSegment(
                    category = cat,
                    amount   = amount,
                    color    = getCategoryColor(cat),
                    fraction = if (categoryTotal > 0) (amount / categoryTotal).toFloat() else 0f
                )
            }
            BreakdownTab.PAYMENT_METHODS -> paymentMethodTotals.map { (method, amount) ->
                PieSegment(
                    category = method,
                    amount   = amount,
                    color    = getPaymentChipColor(method),
                    fraction = if (paymentTotal > 0) (amount / paymentTotal).toFloat() else 0f
                )
            }
        }
    }

    // The single "selected key" passed to the chart — differs by tab
    val activeSelected = when (selectedTab) {
        BreakdownTab.CATEGORIES      -> selectedCategory
        BreakdownTab.PAYMENT_METHODS -> selectedPaymentMethod
    }

    val total = if (selectedTab == BreakdownTab.CATEGORIES) categoryTotal else paymentTotal

    // ── Display lists (selected item floats to top) ───────────────────────────
    val displayCategoryList = remember(categoryTotals, selectedCategory) {
        if (selectedCategory == null) categoryTotals
        else categoryTotals.filter { it.key == selectedCategory } +
                categoryTotals.filter { it.key != selectedCategory }
    }

    val displayPaymentList = remember(paymentMethodTotals, selectedPaymentMethod) {
        if (selectedPaymentMethod == null) paymentMethodTotals
        else paymentMethodTotals.filter { it.key == selectedPaymentMethod } +
                paymentMethodTotals.filter { it.key != selectedPaymentMethod }
    }

    // ── Animation — restarts when tab or underlying data changes ──────────────
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(selectedTab, categoryTotals, paymentMethodTotals) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(900))
    }

    // ── Scroll state ──────────────────────────────────────────────────────────
    val listState = rememberLazyListState()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val heroAmountThresholdPx = remember(density) {
        with(density) { 100.dp.roundToPx() }
    }
    val showAmountInTopBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                    (listState.firstVisibleItemIndex == 0 &&
                            listState.firstVisibleItemScrollOffset > heroAmountThresholdPx)
        }
    }

    BackHandler { onBack() }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Top bar ───────────────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = if (showAmountInTopBar) 4.dp else 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 4.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Back", modifier = Modifier.size(28.dp))
                }

                AnimatedVisibility(
                    visible = showAmountInTopBar,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200)),
                    modifier = Modifier.weight(1f)
                ) {
                    Column {
                        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${currency.symbol} ${fmt(total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = accentColor)
                    }
                }
                if (!showAmountInTopBar) Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { showMonthPicker = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.calendar_month), contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                    Text(text = displayMonth, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                }

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = onAddTransaction,
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = accentColor, contentColor = Color.White)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add transaction", modifier = Modifier.size(18.dp))
                }
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {

            // ── Hero: total + pie chart ───────────────────────────────────────
            item(key = "hero") {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Icon(imageVector = centerIcon, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                        Text(text = label, style = MaterialTheme.typography.titleSmall, color = accentColor, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "${currency.symbol} ${fmt(total)}",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    if (activePieSegments.isEmpty()) {
                        EmptyChartState(label = label, displayMonth = displayMonth)
                    } else {
                        DonutPieChart(
                            segments = activePieSegments,
                            animProgress = animProgress.value,
                            centerIcon = centerIcon,
                            centerColor = accentColor,
                            selectedCategory = activeSelected,
                            onSegmentClick = { key ->
                                when (selectedTab) {
                                    BreakdownTab.CATEGORIES ->
                                        selectedCategory = if (selectedCategory == key) null else key
                                    BreakdownTab.PAYMENT_METHODS ->
                                        selectedPaymentMethod = if (selectedPaymentMethod == key) null else key
                                }
                            },
                            onCenterClick = {
                                selectedCategory = null
                                selectedPaymentMethod = null
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                }
            }

            // ── Tab switcher ──────────────────────────────────────────────────
            item(key = "tab_switcher") {
                TabSwitcher(
                    selectedTab = selectedTab,
                    categoryCount = categoryTotals.size,
                    paymentCount = paymentMethodTotals.size,
                    accentColor = accentColor,
                    onTabSelected = { selectedTab = it }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // ── Categories tab content ────────────────────────────────────────
            if (selectedTab == BreakdownTab.CATEGORIES) {
                if (displayCategoryList.isNotEmpty()) {
                    for (entry in displayCategoryList) {
                        val category = entry.key
                        val amount = entry.value
                        item(key = "cat_$category") {
                            val percent = if (categoryTotal > 0) amount / categoryTotal * 100 else 0.0
                            CategoryCard(
                                category = category,
                                amount = amount,
                                percent = percent,
                                catColor = getCategoryColor(category),
                                currencyCode = currency.code,
                                isSelected = selectedCategory == category,
                                onClick = { onCategoryClick(category, selectedYear, selectedMonthIndex) }
                            )
                        }
                    }
                } else {
                    item(key = "empty_categories") {
                        EmptyTabState("No ${label.lowercase()} categories\nfor $displayMonth")
                    }
                }
            }

            // ── Payment Methods tab content ───────────────────────────────────
            if (selectedTab == BreakdownTab.PAYMENT_METHODS) {
                if (displayPaymentList.isNotEmpty()) {
                    for (entry in displayPaymentList) {
                        val method = entry.key
                        val amount = entry.value
                        item(key = "pay_$method") {
                            val percent = if (paymentTotal > 0) amount / paymentTotal * 100 else 0.0
                            PaymentMethodCard(
                                method = method,
                                amount = amount,
                                percent = percent,
                                chipColor = getPaymentChipColor(method),
                                currencyCode = currency.code,
                                isSelected = selectedPaymentMethod == method,
                                onClick = { onPaymentMethodClick(method, selectedYear, selectedMonthIndex) }
                            )
                        }
                    }
                } else {
                    item(key = "empty_payments") {
                        EmptyTabState("No payment method data\nfor $displayMonth")
                    }
                }
            }
        }
    }

    // ── Month picker ──────────────────────────────────────────────────────────
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

// ── Tab switcher ──────────────────────────────────────────────────────────────

@Composable
private fun TabSwitcher(
    selectedTab: BreakdownTab,
    categoryCount: Int,
    paymentCount: Int,
    accentColor: Color,
    onTabSelected: (BreakdownTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(4.dp)
    ) {
        BreakdownTab.values().forEach { tab ->
            val isSelected = selectedTab == tab
            val count      = if (tab == BreakdownTab.CATEGORIES) categoryCount else paymentCount
            val tabLabel   = if (tab == BreakdownTab.CATEGORIES) "Categories" else "Payment Methods"

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = tabLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Black,
                        color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (count > 0) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSelected) accentColor.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Category card ─────────────────────────────────────────────────────────────

@Composable
private fun CategoryCard(
    category: String,
    amount: Double,
    percent: Double,
    catColor: Color,
    currencyCode: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) catColor.copy(alpha = 0.22f)
            else catColor.copy(alpha = 0.10f)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, catColor.copy(alpha = 0.6f)) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(catColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(getCategoryIcon(category), contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = category, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "${fmt(amount)} $currencyCode", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Surface(shape = CircleShape, color = if (isSelected) catColor.copy(alpha = 0.15f) else catColor.copy(alpha = 0.10f)) {
                Text(text = String.format("%.1f%%", percent), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = catColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
        }
    }
}

// ── Payment method card ───────────────────────────────────────────────────────

@Composable
private fun PaymentMethodCard(
    method: String,
    amount: Double,
    percent: Double,
    chipColor: Color,
    currencyCode: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) chipColor.copy(alpha = 0.22f)
            else chipColor.copy(alpha = 0.10f)
        ),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, chipColor.copy(alpha = 0.6f)) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(chipColor),
                contentAlignment = Alignment.Center
            ) {
                when (method) {
                    "GPay" -> Image(painter = painterResource(R.drawable.gpay), contentDescription = "GPay", modifier = Modifier.size(22.dp))
                    "PhonePe" -> Image(painter = painterResource(R.drawable.phonepe), contentDescription = "PhonePe", modifier = Modifier.size(22.dp))
                    else -> Icon(getPaymentIcon(method), contentDescription = null, modifier = Modifier.size(22.dp), tint = Color.White)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = method, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "${fmt(amount)} $currencyCode", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Surface(shape = CircleShape, color = if (isSelected) chipColor.copy(alpha = 0.15f) else chipColor.copy(alpha = 0.10f)) {
                Text(text = String.format("%.1f%%", percent), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = chipColor, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp))
            }
        }
    }
}

// ── Empty states ──────────────────────────────────────────────────────────────

@Composable
private fun EmptyTabState(message: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Text(text = message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun EmptyChartState(label: String, displayMonth: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(52.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            Text(text = "No ${label.lowercase()}\nfor $displayMonth", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}

// ── Donut pie chart ───────────────────────────────────────────────────────────

@Composable
fun DonutPieChart(
    segments: List<PieSegment>,
    animProgress: Float,
    centerIcon: ImageVector,
    centerColor: Color,
    selectedCategory: String?,
    onSegmentClick: (String) -> Unit,
    onCenterClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val outerRingSize: Dp = 280.dp
    val chartSize: Dp     = 236.dp
    val strokeWidth: Dp   = 52.dp

    val segmentAngles: List<Triple<String, Float, Float>> = remember(segments, animProgress) {
        var cursor = -90f
        segments.map { seg ->
            val fullSweep = seg.fraction * 360f * animProgress
            val entry = Triple(seg.category, cursor, fullSweep.coerceAtLeast(0f))
            cursor += fullSweep
            entry
        }
    }

    Box(modifier = modifier.size(outerRingSize), contentAlignment = Alignment.Center) {

        Canvas(modifier = Modifier.fillMaxSize()) {
            val halfW = size.minDimension / 2f
            drawCircle(color = centerColor.copy(alpha = 0.18f), radius = halfW - 1.5.dp.toPx(), style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = centerColor.copy(alpha = 0.07f), radius = halfW - 10.dp.toPx(), style = Stroke(width = 5.dp.toPx()))
        }

        Box(
            modifier = Modifier
                .size(chartSize)
                .pointerInput(segmentAngles) {
                    val strokePx  = strokeWidth.toPx()
                    val cx        = size.width / 2f
                    val cy        = size.height / 2f
                    val arcRadius = (size.width.toFloat() - strokePx) / 2f
                    val tapPad    = 12.dp.toPx()
                    val tapOuter  = arcRadius + strokePx / 2f + tapPad
                    val tapInner  = (arcRadius - strokePx / 2f - tapPad).coerceAtLeast(0f)
                    val centerR   = tapInner - 2.dp.toPx()

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val dx   = down.position.x - cx
                        val dy   = down.position.y - cy
                        val dist = sqrt(dx * dx + dy * dy)

                        when {
                            dist < centerR -> { down.consume(); onCenterClick() }
                            dist in tapInner..tapOuter -> {
                                val rawAngle   = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                val touchAngle = (rawAngle + 90f + 360f) % 360f
                                val hit = segmentAngles.firstOrNull { (_, startRaw, sweep) ->
                                    val segStart = (startRaw + 90f + 360f) % 360f
                                    val segEnd   = segStart + sweep
                                    if (segEnd <= 360f) touchAngle in segStart..segEnd
                                    else touchAngle >= segStart || touchAngle <= segEnd - 360f
                                }
                                if (hit != null) { down.consume(); onSegmentClick(hit.first) }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke  = strokeWidth.toPx()
                val radius  = (size.minDimension - stroke) / 2f
                val topLeft = Offset(center.x - radius, center.y - radius)
                val arcSize = Size(radius * 2, radius * 2)
                val gap     = if (segments.size > 1) 2.5f else 0f

                segmentAngles.forEachIndexed { idx, (cat, startAngle, hitSweep) ->
                    val drawSweep  = (hitSweep - gap).coerceAtLeast(0f)
                    val seg        = segments[idx]
                    val isSelected = selectedCategory == cat
                    val isDimmed   = selectedCategory != null && !isSelected
                    val color      = if (isDimmed) seg.color.copy(alpha = 0.20f) else seg.color
                    if (drawSweep > 0f) {
                        drawArc(
                            color      = color,
                            startAngle = startAngle,
                            sweepAngle = drawSweep,
                            useCenter  = false,
                            topLeft    = topLeft,
                            size       = arcSize,
                            style      = Stroke(width = if (isSelected) stroke * 1.10f else stroke, cap = StrokeCap.Butt)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(chartSize - strokeWidth * 2 - 10.dp)
                    .clip(CircleShape)
                    .clickable { onCenterClick() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Box(
                        modifier = Modifier.size(60.dp).clip(CircleShape).background(centerColor.copy(alpha = 0.11f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = centerIcon, contentDescription = null, tint = centerColor, modifier = Modifier.size(34.dp))
                    }
                    if (selectedCategory != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Reset", style = MaterialTheme.typography.labelSmall, color = centerColor.copy(alpha = 0.65f), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
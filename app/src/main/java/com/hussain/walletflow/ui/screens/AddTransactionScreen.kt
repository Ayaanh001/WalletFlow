package com.hussain.walletflow.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.hussain.walletflow.R
import com.hussain.walletflow.data.CurrencyData
import com.hussain.walletflow.data.CustomItem
import com.hussain.walletflow.data.CustomItemsRepository
import com.hussain.walletflow.data.Transaction
import com.hussain.walletflow.data.TransactionCategories
import com.hussain.walletflow.data.TransactionType
import com.hussain.walletflow.data.UserPreferencesRepository
import com.hussain.walletflow.ui.theme.ExpenseRed
import com.hussain.walletflow.ui.theme.IncomeGreen
import com.hussain.walletflow.utils.getCategoryColor
import com.hussain.walletflow.utils.getCategoryIcon
import com.hussain.walletflow.utils.getPaymentChipColor
import com.hussain.walletflow.utils.getPaymentIcon
import com.hussain.walletflow.utils.registerCustomCategories
import com.hussain.walletflow.utils.registerCustomPaymentMethods
import com.hussain.walletflow.viewmodel.TransactionViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
        viewModel: TransactionViewModel,
        onBack: () -> Unit,
        transactionId: Long? = null,
        initialTransactionType: String = "EXPENSE"
) {
        val context = LocalContext.current
        val prefsRepository = remember { UserPreferencesRepository(context) }
        val selectedCurrency by
        prefsRepository.currencyFlow.collectAsState(
                initial = UserPreferencesRepository.DEFAULT_CURRENCY
        )
        val currency =
                remember(selectedCurrency) {
                        CurrencyData.currencies.find { it.code == selectedCurrency }
                                ?: CurrencyData.currencies.first()
                }
        var currentTransactionId by remember { mutableStateOf(transactionId) }
        var showDeleteConfirmDialog by remember { mutableStateOf(false) }
        var selectedType by remember {
                mutableStateOf(
                        if (transactionId == null)
                                if (initialTransactionType == "INCOME") TransactionType.INCOME
                                else TransactionType.EXPENSE
                        else TransactionType.EXPENSE  // will be overwritten when existing tx loads
                )
        }
        var amount by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf("") }
        var selectedPaymentMethod by remember { mutableStateOf("Cash") }
        var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
        var showDatePicker by remember { mutableStateOf(false) }
        var showTimePicker by remember { mutableStateOf(false) }
        var categoryExpanded by remember { mutableStateOf(true) }
        var paymentExpanded by remember { mutableStateOf(true) }
        var bankAccountInfo by remember { mutableStateOf<String?>(null) }

        val amountFocusRequester = remember { FocusRequester() }
        val nameFocusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        // ── Custom items ──────────────────────────────────────────────────────
        val customItemsRepo = remember { CustomItemsRepository(context) }
        val coroutineScope = rememberCoroutineScope()
        val customCategories by customItemsRepo.customCategoriesFlow.collectAsState(initial = emptyList())
        val customPaymentMethods by customItemsRepo.customPaymentMethodsFlow.collectAsState(initial = emptyList())

        // Keep IconUtils runtime maps in sync
        LaunchedEffect(customCategories) { registerCustomCategories(customCategories) }
        LaunchedEffect(customPaymentMethods) { registerCustomPaymentMethods(customPaymentMethods) }

        var showNewCategoryDialog by remember { mutableStateOf(false) }
        var showNewPaymentDialog  by remember { mutableStateOf(false) }

        val builtInCategories =
                if (selectedType == TransactionType.INCOME) {
                        TransactionCategories.INCOME_CATEGORIES
                } else {
                        TransactionCategories.EXPENSE_CATEGORIES
                }
        val typeString = if (selectedType == TransactionType.INCOME) "income" else "expense"
        val categories = remember(builtInCategories, customCategories, typeString) {
                builtInCategories + customCategories.filter { it.type == typeString }.map { it.name }
        }

        // Load transaction if in edit mode
        LaunchedEffect(currentTransactionId) {
                if (currentTransactionId != null) {
                        val txn = viewModel.getTransactionById(currentTransactionId!!)
                        if (txn != null) {
                                amount =
                                        if (txn.amount % 1.0 == 0.0) txn.amount.toInt().toString()
                                        else txn.amount.toString()
                                name =
                                        if (txn.originalSms.isNotEmpty() &&
                                                txn.originalSms != "Manual entry"
                                        )
                                                txn.originalSms
                                                        .replace("\n", " ")
                                                        .replace("\r", " ")
                                                        .trim()
                                        else txn.remark
                                selectedType = txn.type
                                selectedCategory = txn.category
                                if (txn.bankName.isNotEmpty() && txn.accountLastFour.isNotEmpty()) {
                                        bankAccountInfo = "${txn.bankName} •${txn.accountLastFour}"
                                        selectedPaymentMethod = bankAccountInfo!!
                                } else {
                                        selectedPaymentMethod = txn.paymentMethod
                                }
                                selectedDate =
                                        Calendar.getInstance().apply { timeInMillis = txn.date }
                        }
                }
        }

        // Auto-open keyboard for amount field when screen opens (only if adding new)
        LaunchedEffect(Unit) {
                if (currentTransactionId == null) {
                        delay(300)
                        amountFocusRequester.requestFocus()
                }
        }

        val dateFormat = remember { SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()) }
        val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

        fun saveTransaction(): Transaction? {
                val amountValue = amount.toDoubleOrNull() ?: 0.0
                if (amountValue > 0) {
                        val transaction =
                                Transaction(
                                        date = selectedDate.timeInMillis,
                                        amount = amountValue,
                                        type = selectedType,
                                        category = selectedCategory,
                                        bankName = "",
                                        accountLastFour = "",
                                        remark = name,
                                        originalSms = "",
                                        paymentMethod = selectedPaymentMethod,
                                        isAddedToMonthly = true
                                )
                                        .copy(id = currentTransactionId ?: 0L)

                        if (currentTransactionId != null) {
                                viewModel.updateTransaction(transaction)
                        } else {
                                viewModel.insertTransaction(transaction)
                        }
                        return transaction
                }
                return null
        }

        fun resetForm() {
                amount = ""
                name = ""
                // Reset category based on current type default
                val currentCategories =
                        if (selectedType == TransactionType.INCOME)
                                TransactionCategories.INCOME_CATEGORIES
                        else TransactionCategories.EXPENSE_CATEGORIES
                selectedCategory = currentCategories.firstOrNull() ?: ""
                selectedPaymentMethod = "Cash"
                selectedDate = Calendar.getInstance()
                currentTransactionId = null
                bankAccountInfo = null
        }

        // Intercept system back when create-new overlays are visible
        BackHandler(enabled = showNewCategoryDialog || showNewPaymentDialog) {
                showNewCategoryDialog = false
                showNewPaymentDialog = false
        }

        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Text(
                                                text =
                                                        if (currentTransactionId != null)
                                                                "Update Transaction"
                                                        else "Add Transaction",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleLarge
                                        )
                                },
                                navigationIcon = {
                                        IconButton(onClick = onBack) {
                                                Icon(
                                                        Icons.AutoMirrored.Filled.ArrowBack,
                                                        contentDescription = "Back"
                                                )
                                        }
                                },
                                colors =
                                        TopAppBarDefaults.centerAlignedTopAppBarColors(
                                                containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                modifier = Modifier.statusBarsPadding(),
                                actions = {
                                        if (currentTransactionId != null) {
                                                Surface(
                                                        onClick = {
                                                                showDeleteConfirmDialog = true
                                                        },
                                                        shape = RoundedCornerShape(12.dp),
                                                        color =
                                                                MaterialTheme.colorScheme.error
                                                                        .copy(alpha = 0.1f),
                                                        contentColor =
                                                                MaterialTheme.colorScheme.error,
                                                        modifier =
                                                                Modifier.padding(end = 8.dp)
                                                                        .size(40.dp)
                                                ) {
                                                        Box(contentAlignment = Alignment.Center) {
                                                                Icon(
                                                                        Icons.Default.Delete,
                                                                        contentDescription =
                                                                                "Delete transaction",
                                                                        modifier =
                                                                                Modifier.size(20.dp)
                                                                )
                                                        }
                                                }
                                        }
                                }
                        )
                },
                containerColor = MaterialTheme.colorScheme.surface,
                bottomBar = {
                        // Bottom buttons — no background color
                        Box(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .navigationBarsPadding()
                                                        .padding(
                                                                horizontal = 16.dp,
                                                                vertical = 12.dp
                                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                        // Save & Add New button (left, outlined)
                                        OutlinedButton(
                                                onClick = {
                                                        if (saveTransaction() != null) {
                                                                resetForm()
                                                        }
                                                },
                                                modifier = Modifier.weight(0.5f),
                                                shape = RoundedCornerShape(50.dp),
                                                contentPadding = PaddingValues(vertical = 14.dp)
                                        ) {
                                                Icon(
                                                        Icons.Filled.Add,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                        "Save & Add New",
                                                        fontWeight = FontWeight.SemiBold
                                                )
                                        }

                                        // Save button (right, filled primary)
                                        Button(
                                                onClick = {
                                                        if (saveTransaction() != null) {
                                                                onBack()
                                                        }
                                                },
                                                modifier = Modifier.weight(0.5f),
                                                shape = RoundedCornerShape(50.dp),
                                                contentPadding = PaddingValues(vertical = 14.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                                Icon(
                                                        Icons.Filled.Save,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                        if (currentTransactionId != null) "Update"
                                                        else "Save",
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }
                                }
                        }
                }
        ) { innerPadding ->
                val surfaceColor = MaterialTheme.colorScheme.surface
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        Column(
                                modifier =
                                        Modifier.fillMaxSize()
                                                .verticalScroll(rememberScrollState())
                                                .padding(horizontal = 16.dp)
                        ) {
                                // ─── Expense / Income Tab Switcher ───
                                Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        shape = RoundedCornerShape(50.dp),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant.copy(
                                                                                alpha = 0.5f
                                                                        )
                                                )
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(4.dp),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                                listOf(
                                                        TransactionType.EXPENSE to "Expense",
                                                        TransactionType.INCOME to "Income"
                                                )
                                                        .forEach { (type, label) ->
                                                                val isSelected =
                                                                        selectedType == type
                                                                Box(
                                                                        modifier =
                                                                                Modifier.weight(1f)
                                                                                        .clip(
                                                                                                RoundedCornerShape(
                                                                                                        50.dp
                                                                                                )
                                                                                        )
                                                                                        .background(
                                                                                                if (isSelected)
                                                                                                        MaterialTheme
                                                                                                                .colorScheme
                                                                                                                .primary
                                                                                                else
                                                                                                        Color.Transparent
                                                                                        )
                                                                                        .clickable {
                                                                                                if (selectedType !=
                                                                                                        type
                                                                                                ) {
                                                                                                        selectedType =
                                                                                                                type
                                                                                                        // Reset category when type changes
                                                                                                        val newCategories =
                                                                                                                if (type ==
                                                                                                                        TransactionType
                                                                                                                                .INCOME
                                                                                                                )
                                                                                                                        TransactionCategories
                                                                                                                                .INCOME_CATEGORIES
                                                                                                                else
                                                                                                                        TransactionCategories
                                                                                                                                .EXPENSE_CATEGORIES
                                                                                                        selectedCategory =
                                                                                                                newCategories
                                                                                                                        .firstOrNull()
                                                                                                                        ?: ""
                                                                                                }
                                                                                        }
                                                                                        .padding(vertical = 12.dp),
                                                                        contentAlignment = Alignment.Center
                                                                ) {
                                                                        Text(
                                                                                text = label,
                                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Black,
                                                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                                style = MaterialTheme.typography.titleSmall
                                                                        )
                                                                }
                                                        }
                                        }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // ─── Amount Input ───
                                Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                        Text(
                                                text = "AMOUNT",
                                                style = MaterialTheme.typography.labelMedium,
                                                color =
                                                        if (selectedType == TransactionType.EXPENSE)
                                                                ExpenseRed
                                                        else IncomeGreen,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))

                                        // Amount display: ₹ symbol stays right next to the number
                                        val amountTextStyle =
                                                MaterialTheme.typography.displayMedium.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                )
                                        Box(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                        ) {
                                                Row(
                                                        modifier =
                                                                Modifier.width(IntrinsicSize.Min),
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                text = "${currency.symbol}",
                                                                style = amountTextStyle,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurface
                                                        )
                                                        BasicTextField(
                                                                value = amount,
                                                                onValueChange = { newValue ->
                                                                        if (newValue.isEmpty() ||
                                                                                newValue.matches(
                                                                                        Regex(
                                                                                                "^\\d*\\.?\\d*$"
                                                                                        )
                                                                                )
                                                                        ) {
                                                                                amount = newValue
                                                                        }
                                                                },
                                                                textStyle = amountTextStyle,
                                                                keyboardOptions =
                                                                        KeyboardOptions(
                                                                                keyboardType =
                                                                                        KeyboardType
                                                                                                .Decimal,
                                                                                imeAction =
                                                                                        ImeAction
                                                                                                .Next
                                                                        ),
                                                                keyboardActions =
                                                                        KeyboardActions(
                                                                                onNext = {
                                                                                        nameFocusRequester
                                                                                                .requestFocus()
                                                                                }
                                                                        ),
                                                                singleLine = true,
                                                                cursorBrush =
                                                                        SolidColor(
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                        ),
                                                                modifier =
                                                                        Modifier.weight(1f)
                                                                                .focusRequester(
                                                                                        amountFocusRequester
                                                                                ),
                                                                decorationBox = { innerTextField ->
                                                                        if (amount.isEmpty()) {
                                                                                Text(
                                                                                        text = "0",
                                                                                        style =
                                                                                                amountTextStyle,
                                                                                        color =
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .onSurfaceVariant
                                                                                )
                                                                        }
                                                                        innerTextField()
                                                                }
                                                        )
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // ─── Name / "What was this for?" ───
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant.copy(
                                                                                alpha = 0.5f
                                                                        )
                                                )
                                ) {
                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .padding(
                                                                        horizontal = 16.dp,
                                                                        vertical = 0.dp
                                                                ),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Icon(
                                                        Icons.Filled.Description,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(22.dp),
                                                        tint = MaterialTheme.colorScheme.primary
                                                )
                                                TextField(
                                                        value = name,
                                                        onValueChange = { name = it },
                                                        placeholder = {
                                                                Text(
                                                                        "What was this for?",
                                                                        color =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .onSurfaceVariant
                                                                                        .copy(
                                                                                                alpha =
                                                                                                        0.6f
                                                                                        )
                                                                )
                                                        },
                                                        modifier =
                                                                Modifier.weight(1f)
                                                                        .focusRequester(nameFocusRequester)
                                                                        .offset(x = (-4).dp),
                                                        colors =
                                                                TextFieldDefaults.colors(
                                                                        focusedContainerColor = Color.Transparent,
                                                                        unfocusedContainerColor = Color.Transparent,
                                                                        focusedIndicatorColor = Color.Transparent,
                                                                        unfocusedIndicatorColor = Color.Transparent
                                                                ),
                                                        keyboardOptions =
                                                                KeyboardOptions(
                                                                        imeAction = ImeAction.Done,
                                                                        capitalization = KeyboardCapitalization.Sentences
                                                                ),
                                                        keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
                                                )
                                        }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // ─── Category Section (collapsible card) ───
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant.copy(
                                                                                alpha = 0.4f
                                                                        )
                                                )
                                ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                                // Header row
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .clickable {
                                                                                categoryExpanded =
                                                                                        !categoryExpanded
                                                                        }
                                                                        .padding(16.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                        Row(
                                                                verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                                Icon(
                                                                        Icons.Filled.Category,
                                                                        contentDescription = null,
                                                                        modifier =
                                                                                Modifier.size(
                                                                                        22.dp
                                                                                ),
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(
                                                                                        10.dp
                                                                                )
                                                                )
                                                                Column {
                                                                        Text(
                                                                                text = "Category",
                                                                                style = MaterialTheme.typography.titleSmall,
                                                                                fontWeight = FontWeight.Bold
                                                                        )
                                                                        if (selectedCategory
                                                                                        .isNotEmpty()
                                                                        ) {
                                                                                Row(
                                                                                        verticalAlignment =
                                                                                                Alignment
                                                                                                        .CenterVertically
                                                                                ) {
                                                                                        Icon(
                                                                                                getCategoryIcon(
                                                                                                        selectedCategory
                                                                                                ),
                                                                                                contentDescription =
                                                                                                        null,
                                                                                                modifier =
                                                                                                        Modifier.size(
                                                                                                                14.dp
                                                                                                        ),
                                                                                                tint =
                                                                                                        getCategoryColor(
                                                                                                                selectedCategory
                                                                                                        )
                                                                                        )
                                                                                        Spacer(
                                                                                                modifier =
                                                                                                        Modifier.width(
                                                                                                                4.dp
                                                                                                        )
                                                                                        )
                                                                                        Text(
                                                                                                text =
                                                                                                        selectedCategory,
                                                                                                style =
                                                                                                        MaterialTheme
                                                                                                                .typography
                                                                                                                .bodySmall,
                                                                                                color =
                                                                                                        getCategoryColor(
                                                                                                                selectedCategory
                                                                                                        ),
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .SemiBold
                                                                                        )
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                        Icon(
                                                                if (categoryExpanded)
                                                                        Icons.Filled.ExpandLess
                                                                else Icons.Filled.ExpandMore,
                                                                contentDescription = "Toggle",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                }

                                                // Category chips grid
                                                AnimatedVisibility(
                                                        visible = categoryExpanded,
                                                        enter = expandVertically(),
                                                        exit = shrinkVertically()
                                                ) {
                                                        Column(
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .padding(
                                                                                        start =
                                                                                                16.dp,
                                                                                        end = 16.dp,
                                                                                        bottom =
                                                                                                16.dp
                                                                                )
                                                        ) {
                                                                @OptIn(ExperimentalLayoutApi::class)
                                                                FlowRow(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .spacedBy(
                                                                                                8.dp
                                                                                        ),
                                                                        verticalArrangement =
                                                                                Arrangement
                                                                                        .spacedBy(
                                                                                                8.dp
                                                                                        )
                                                                ) {
                                                                        categories.forEach {
                                                                                        category ->
                                                                                val isSelected = selectedCategory == category
                                                                                val chipColor = getCategoryColor(category)
                                                                                val chipBg = if (isSelected) chipColor.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface

                                                                                Box(
                                                                                        modifier =
                                                                                                Modifier.clip(
                                                                                                        RoundedCornerShape(
                                                                                                                50.dp
                                                                                                        )
                                                                                                )
                                                                                                        .then(
                                                                                                                if (isSelected)
                                                                                                                        Modifier.border(
                                                                                                                                1.5.dp,
                                                                                                                                chipColor,
                                                                                                                                RoundedCornerShape(
                                                                                                                                        50.dp
                                                                                                                                )
                                                                                                                        )
                                                                                                                else
                                                                                                                        Modifier
                                                                                                        )
                                                                                                        .background(
                                                                                                                chipBg
                                                                                                        )
                                                                                                        .clickable {
                                                                                                                selectedCategory =
                                                                                                                        if (selectedCategory ==
                                                                                                                                category
                                                                                                                        )
                                                                                                                                ""
                                                                                                                        else
                                                                                                                                category
                                                                                                        }
                                                                                                        .padding(
                                                                                                                horizontal =
                                                                                                                        12.dp,
                                                                                                                vertical =
                                                                                                                        8.dp
                                                                                                        )
                                                                                ) {
                                                                                        Row(
                                                                                                verticalAlignment =
                                                                                                        Alignment
                                                                                                                .CenterVertically,
                                                                                                horizontalArrangement =
                                                                                                        Arrangement
                                                                                                                .spacedBy(
                                                                                                                        6.dp
                                                                                                                )
                                                                                        ) {
                                                                                                Icon(
                                                                                                        getCategoryIcon(
                                                                                                                category
                                                                                                        ),
                                                                                                        contentDescription =
                                                                                                                null,
                                                                                                        modifier =
                                                                                                                Modifier.size(
                                                                                                                        16.dp
                                                                                                                ),
                                                                                                        tint =
                                                                                                                chipColor
                                                                                                )
                                                                                                Text(
                                                                                                        text =
                                                                                                                category,
                                                                                                        style =
                                                                                                                MaterialTheme
                                                                                                                        .typography
                                                                                                                        .labelMedium,
                                                                                                        fontWeight =
                                                                                                                if (isSelected)
                                                                                                                        FontWeight
                                                                                                                                .Bold
                                                                                                                else
                                                                                                                        FontWeight
                                                                                                                                .Medium,
                                                                                                        color =
                                                                                                                if (isSelected)
                                                                                                                        chipColor
                                                                                                                else
                                                                                                                        MaterialTheme
                                                                                                                                .colorScheme
                                                                                                                                .onSurface,
                                                                                                        maxLines =
                                                                                                                1
                                                                                                )
                                                                                        }
                                                                                }
                                                                        }
                                                                        // "+ New" chip
                                                                        Box(
                                                                                modifier = Modifier
                                                                                        .clip(RoundedCornerShape(50.dp))
                                                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                                                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                                                                                        .clickable { showNewCategoryDialog = true }
                                                                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                                                        ) {
                                                                                Row(
                                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                                ) {
                                                                                        Icon(
                                                                                                Icons.Filled.Add,
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
                                                                }
                                                        }
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // ─── Payment Method Section (collapsible card) ───
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(24.dp),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant.copy(
                                                                                alpha = 0.4f
                                                                        )
                                                )
                                ) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                                // Header row
                                                Row(
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .clickable {
                                                                                paymentExpanded =
                                                                                        !paymentExpanded
                                                                        }
                                                                        .padding(16.dp),
                                                        horizontalArrangement =
                                                                Arrangement.SpaceBetween,
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Row(
                                                                verticalAlignment =
                                                                        Alignment.CenterVertically
                                                        ) {
                                                                Icon(
                                                                        Icons.Filled.Payment,
                                                                        contentDescription = null,
                                                                        modifier =
                                                                                Modifier.size(
                                                                                        22.dp
                                                                                ),
                                                                        tint =
                                                                                MaterialTheme
                                                                                        .colorScheme
                                                                                        .primary
                                                                )
                                                                Spacer(
                                                                        modifier =
                                                                                Modifier.width(
                                                                                        10.dp
                                                                                )
                                                                )
                                                                Column {
                                                                        Text(
                                                                                text =
                                                                                        "Payment Method",
                                                                                style =
                                                                                        MaterialTheme
                                                                                                .typography
                                                                                                .titleSmall,
                                                                                fontWeight =
                                                                                        FontWeight
                                                                                                .Bold
                                                                        )
                                                                        if (selectedPaymentMethod
                                                                                        .isNotEmpty()
                                                                        ) {
                                                                                val chipColor =
                                                                                        getPaymentChipColor(
                                                                                                selectedPaymentMethod
                                                                                        )
                                                                                Row(
                                                                                        verticalAlignment =
                                                                                                Alignment
                                                                                                        .CenterVertically
                                                                                ) {
                                                                                        when {
                                                                                                selectedPaymentMethod ==
                                                                                                        "GPay" ->
                                                                                                        Image(
                                                                                                                painter =
                                                                                                                        painterResource(
                                                                                                                                R.drawable
                                                                                                                                        .gpay
                                                                                                                        ),
                                                                                                                contentDescription =
                                                                                                                        "GPay",
                                                                                                                modifier =
                                                                                                                        Modifier.size(
                                                                                                                                14.dp
                                                                                                                        )
                                                                                                        )
                                                                                                selectedPaymentMethod ==
                                                                                                        "PhonePe" ->
                                                                                                        Image(
                                                                                                                painter =
                                                                                                                        painterResource(
                                                                                                                                R.drawable
                                                                                                                                        .phonepe
                                                                                                                        ),
                                                                                                                contentDescription =
                                                                                                                        "PhonePe",
                                                                                                                modifier =
                                                                                                                        Modifier.size(
                                                                                                                                14.dp
                                                                                                                        )
                                                                                                        )
                                                                                                selectedPaymentMethod
                                                                                                        .contains(
                                                                                                                "•"
                                                                                                        ) ->
                                                                                                        Icon(
                                                                                                                Icons.Default
                                                                                                                        .AccountBalance,
                                                                                                                contentDescription =
                                                                                                                        null,
                                                                                                                modifier =
                                                                                                                        Modifier.size(
                                                                                                                                14.dp
                                                                                                                        ),
                                                                                                                tint =
                                                                                                                        chipColor
                                                                                                        )
                                                                                                else ->
                                                                                                        Icon(
                                                                                                                getPaymentIcon(
                                                                                                                        selectedPaymentMethod
                                                                                                                ),
                                                                                                                contentDescription =
                                                                                                                        null,
                                                                                                                modifier =
                                                                                                                        Modifier.size(
                                                                                                                                14.dp
                                                                                                                        ),
                                                                                                                tint =
                                                                                                                        chipColor
                                                                                                        )
                                                                                        }
                                                                                        Spacer(
                                                                                                modifier =
                                                                                                        Modifier.width(
                                                                                                                4.dp
                                                                                                        )
                                                                                        )
                                                                                        Text(
                                                                                                text =
                                                                                                        selectedPaymentMethod,
                                                                                                style =
                                                                                                        MaterialTheme
                                                                                                                .typography
                                                                                                                .bodySmall,
                                                                                                color =
                                                                                                        chipColor,
                                                                                                fontWeight =
                                                                                                        FontWeight
                                                                                                                .SemiBold
                                                                                        )
                                                                                }
                                                                        }
                                                                }
                                                        }
                                                        Icon(
                                                                if (paymentExpanded)
                                                                        Icons.Filled.ExpandLess
                                                                else Icons.Filled.ExpandMore,
                                                                contentDescription = "Toggle",
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }

                                                // Payment chips
                                                AnimatedVisibility(
                                                        visible = paymentExpanded,
                                                        enter = expandVertically(),
                                                        exit = shrinkVertically()
                                                ) {
                                                        Column(
                                                                modifier =
                                                                        Modifier.fillMaxWidth()
                                                                                .padding(
                                                                                        start =
                                                                                                16.dp,
                                                                                        end = 16.dp,
                                                                                        bottom =
                                                                                                16.dp
                                                                                )
                                                        ) {
                                                                @OptIn(ExperimentalLayoutApi::class)
                                                                FlowRow(
                                                                        modifier =
                                                                                Modifier.fillMaxWidth(),
                                                                        horizontalArrangement =
                                                                                Arrangement
                                                                                        .spacedBy(
                                                                                                8.dp
                                                                                        ),
                                                                        verticalArrangement =
                                                                                Arrangement
                                                                                        .spacedBy(
                                                                                                8.dp
                                                                                        )
                                                                ) {
                                                                        val allPaymentMethods =
                                                                                remember(
                                                                                        bankAccountInfo,
                                                                                        customPaymentMethods
                                                                                ) {
                                                                                        val base = if (bankAccountInfo != null) {
                                                                                                listOf(bankAccountInfo!!) + TransactionCategories.PAYMENT_METHODS
                                                                                        } else {
                                                                                                TransactionCategories.PAYMENT_METHODS
                                                                                        }
                                                                                        base + customPaymentMethods.map { it.name }
                                                                                }
                                                                        allPaymentMethods.forEach {
                                                                                        method ->
                                                                                val isSelected =
                                                                                        selectedPaymentMethod ==
                                                                                                method
                                                                                val chipColor =
                                                                                        getPaymentChipColor(
                                                                                                method
                                                                                        )
                                                                                val chipBg =
                                                                                        if (isSelected)
                                                                                                chipColor
                                                                                                        .copy(
                                                                                                                alpha =
                                                                                                                        0.12f
                                                                                                        )
                                                                                        else
                                                                                                MaterialTheme
                                                                                                        .colorScheme
                                                                                                        .surface

                                                                                Box(
                                                                                        modifier =
                                                                                                Modifier.clip(
                                                                                                        RoundedCornerShape(
                                                                                                                50.dp
                                                                                                        )
                                                                                                )
                                                                                                        .then(
                                                                                                                if (isSelected)
                                                                                                                        Modifier.border(
                                                                                                                                1.5.dp,
                                                                                                                                chipColor,
                                                                                                                                RoundedCornerShape(
                                                                                                                                        50.dp
                                                                                                                                )
                                                                                                                        )
                                                                                                                else
                                                                                                                        Modifier
                                                                                                        )
                                                                                                        .background(
                                                                                                                chipBg
                                                                                                        )
                                                                                                        .clickable {
                                                                                                                selectedPaymentMethod =
                                                                                                                        method
                                                                                                        }
                                                                                                        .padding(
                                                                                                                horizontal =
                                                                                                                        12.dp,
                                                                                                                vertical =
                                                                                                                        8.dp
                                                                                                        )
                                                                                ) {
                                                                                        Row(
                                                                                                verticalAlignment =
                                                                                                        Alignment
                                                                                                                .CenterVertically,
                                                                                                horizontalArrangement =
                                                                                                        Arrangement
                                                                                                                .spacedBy(
                                                                                                                        6.dp
                                                                                                                )
                                                                                        ) {
                                                                                                when {
                                                                                                        method ==
                                                                                                                "GPay" ->
                                                                                                                Image(
                                                                                                                        painter =
                                                                                                                                painterResource(
                                                                                                                                        R.drawable
                                                                                                                                                .gpay
                                                                                                                                ),
                                                                                                                        contentDescription =
                                                                                                                                "GPay",
                                                                                                                        modifier =
                                                                                                                                Modifier.size(
                                                                                                                                        16.dp
                                                                                                                                )
                                                                                                                )
                                                                                                        method ==
                                                                                                                "PhonePe" ->
                                                                                                                Image(
                                                                                                                        painter =
                                                                                                                                painterResource(
                                                                                                                                        R.drawable
                                                                                                                                                .phonepe
                                                                                                                                ),
                                                                                                                        contentDescription =
                                                                                                                                "PhonePe",
                                                                                                                        modifier =
                                                                                                                                Modifier.size(
                                                                                                                                        16.dp
                                                                                                                                )
                                                                                                                )
                                                                                                        method.contains(
                                                                                                                "•"
                                                                                                        ) ->
                                                                                                                Icon(
                                                                                                                        Icons.Default
                                                                                                                                .AccountBalance,
                                                                                                                        contentDescription =
                                                                                                                                null,
                                                                                                                        modifier =
                                                                                                                                Modifier.size(
                                                                                                                                        16.dp
                                                                                                                                ),
                                                                                                                        tint =
                                                                                                                                if (isSelected)
                                                                                                                                        chipColor
                                                                                                                                else
                                                                                                                                        MaterialTheme
                                                                                                                                                .colorScheme
                                                                                                                                                .onSurfaceVariant
                                                                                                                )
                                                                                                        else ->
                                                                                                                Icon(
                                                                                                                        getPaymentIcon(
                                                                                                                                method
                                                                                                                        ),
                                                                                                                        contentDescription =
                                                                                                                                null,
                                                                                                                        modifier =
                                                                                                                                Modifier.size(
                                                                                                                                        16.dp
                                                                                                                                ),
                                                                                                                        tint =
                                                                                                                                chipColor
                                                                                                                )
                                                                                                }
                                                                                                Text(
                                                                                                        text =
                                                                                                                method,
                                                                                                        style =
                                                                                                                MaterialTheme
                                                                                                                        .typography
                                                                                                                        .labelMedium,
                                                                                                        fontWeight =
                                                                                                                if (isSelected)
                                                                                                                        FontWeight
                                                                                                                                .Bold
                                                                                                                else
                                                                                                                        FontWeight
                                                                                                                                .Medium,
                                                                                                        color =
                                                                                                                if (isSelected)
                                                                                                                        chipColor
                                                                                                                else
                                                                                                                        MaterialTheme
                                                                                                                                .colorScheme
                                                                                                                                .onSurface,
                                                                                                        maxLines =
                                                                                                                1
                                                                                                )
                                                                                        }
                                                                                }
                                                                        }
                                                                        // ── "+ New" chip (inside FlowRow, flows with other chips) ──
                                                                        Box(
                                                                                modifier = Modifier
                                                                                        .clip(RoundedCornerShape(50.dp))
                                                                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                                                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                                                                                        .clickable { showNewPaymentDialog = true }
                                                                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                                                        ) {
                                                                                Row(
                                                                                        verticalAlignment = Alignment.CenterVertically,
                                                                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                                ) {
                                                                                        Icon(
                                                                                                Icons.Filled.Add,
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

                                Spacer(modifier = Modifier.height(12.dp))

                                // ─── Date Section ───
                                Card(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .clip(RoundedCornerShape(24.dp))
                                                        .clickable { showDatePicker = true },
                                        shape = RoundedCornerShape(24.dp),
                                        colors =
                                                CardDefaults.cardColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme
                                                                        .surfaceVariant.copy(
                                                                                alpha = 0.4f
                                                                        )
                                                )
                                ) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Icon(
                                                                Icons.Filled.CalendarToday,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(22.dp),
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .primary
                                                        )
                                                        Spacer(modifier = Modifier.width(10.dp))
                                                        Text(
                                                                text = "Date",
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .titleSmall,
                                                                fontWeight = FontWeight.Bold
                                                        )
                                                }

                                                Row(
                                                        verticalAlignment =
                                                                Alignment.CenterVertically
                                                ) {
                                                        Text(
                                                                text =
                                                                        run {
                                                                                val today =
                                                                                        Calendar.getInstance()
                                                                                val isToday =
                                                                                        selectedDate
                                                                                                .get(
                                                                                                        Calendar.YEAR
                                                                                                ) ==
                                                                                                today.get(
                                                                                                        Calendar.YEAR
                                                                                                ) &&
                                                                                                selectedDate
                                                                                                        .get(
                                                                                                                Calendar.DAY_OF_YEAR
                                                                                                        ) ==
                                                                                                today.get(
                                                                                                        Calendar.DAY_OF_YEAR
                                                                                                )
                                                                                if (isToday)
                                                                                        "Today, ${timeFormat.format(selectedDate.time)}"
                                                                                else
                                                                                        "${dateFormat.format(selectedDate.time)}, ${timeFormat.format(selectedDate.time)}"
                                                                        },
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color =
                                                                        MaterialTheme.colorScheme
                                                                                .primary,
                                                                fontWeight = FontWeight.SemiBold
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                                Icons.Filled.ChevronRight,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(18.dp),
                                                                tint =
                                                                        MaterialTheme.colorScheme
                                                                                .onSurfaceVariant
                                                        )
                                                }
                                        }
                                }

                                Spacer(modifier = Modifier.height(40.dp))
                        }

                        // Edge fade at bottom
                        Box(
                                modifier =
                                        Modifier.align(Alignment.BottomCenter)
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .background(
                                                        Brush.verticalGradient(
                                                                colors =
                                                                        listOf(
                                                                                Color.Transparent,
                                                                                surfaceColor
                                                                        )
                                                        )
                                                )
                        )
                }
        }

        // ── Full-screen "Create" page overlays ───────────────────────────────
        if (showNewCategoryDialog) {
                CreateCustomItemScreen(
                        isCategory = true,
                        initialTypeIndex = if (selectedType == TransactionType.INCOME) 1 else 0,
                        existingNames = TransactionCategories.EXPENSE_CATEGORIES +
                                TransactionCategories.INCOME_CATEGORIES +
                                customCategories.map { it.name },
                        onConfirm = { itemName, iconKey, colorHex, itemType ->
                                val newItem = CustomItem(itemName, iconKey, colorHex, itemType)
                                registerCustomCategories(customCategories + newItem)
                                selectedCategory = itemName
                                coroutineScope.launch {
                                        customItemsRepo.addCustomCategory(newItem)
                                }
                                showNewCategoryDialog = false
                        },
                        onBack = { showNewCategoryDialog = false }
                )
        }

        if (showNewPaymentDialog) {
                CreateCustomItemScreen(
                        isCategory = false,
                        existingNames = TransactionCategories.PAYMENT_METHODS +
                                customPaymentMethods.map { it.name },
                        onConfirm = { itemName, iconKey, colorHex, itemType ->
                                val newItem = CustomItem(itemName, iconKey, colorHex, itemType)
                                registerCustomPaymentMethods(customPaymentMethods + newItem)
                                selectedPaymentMethod = itemName
                                coroutineScope.launch {
                                        customItemsRepo.addCustomPaymentMethod(newItem)
                                }
                                showNewPaymentDialog = false
                        },
                        onBack = { showNewPaymentDialog = false }
                )
        }

        // Date picker dialog
        if (showDatePicker) {
                DatePickerDialog(
                        selectedDate = selectedDate,
                        onDateSelected = {
                                selectedDate = it
                                showDatePicker = false
                                showTimePicker = true
                        },
                        onDismiss = { showDatePicker = false }
                )
        }

        // Time picker dialog
        if (showTimePicker) {
                TimePickerDialog(
                        selectedDate = selectedDate,
                        onTimeSelected = {
                                selectedDate = it
                                showTimePicker = false
                        },
                        onDismiss = { showTimePicker = false }
                )
        }

        // Delete confirmation dialog
        if (showDeleteConfirmDialog) {
                AlertDialog(
                        onDismissRequest = { showDeleteConfirmDialog = false },
                        icon = {
                                Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                )
                        },
                        title = { Text("Delete Transaction", fontWeight = FontWeight.Bold) },
                        text = {
                                Text(
                                        "Are you sure you want to delete this transaction? This cannot be undone."
                                )
                        },
                        confirmButton = {
                                Button(
                                        onClick = {
                                                currentTransactionId?.let { id ->
                                                        viewModel.deleteTransactionsByIds(
                                                                listOf(id)
                                                        )
                                                }
                                                showDeleteConfirmDialog = false
                                                onBack()
                                        },
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor =
                                                                MaterialTheme.colorScheme.error
                                                )
                                ) { Text("Delete", fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                                OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                                        Text("Cancel")
                                }
                        }
                )
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialog(
        selectedDate: Calendar,
        onDateSelected: (Calendar) -> Unit,
        onDismiss: () -> Unit
) {
        val datePickerState =
                rememberDatePickerState(initialSelectedDateMillis = selectedDate.timeInMillis)

        androidx.compose.material3.DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                        TextButton(
                                onClick = {
                                        datePickerState.selectedDateMillis?.let {
                                                val cal = Calendar.getInstance()
                                                cal.timeInMillis = it
                                                // Preserve time
                                                cal.set(
                                                        Calendar.HOUR_OF_DAY,
                                                        selectedDate.get(Calendar.HOUR_OF_DAY)
                                                )
                                                cal.set(
                                                        Calendar.MINUTE,
                                                        selectedDate.get(Calendar.MINUTE)
                                                )
                                                onDateSelected(cal)
                                        }
                                }
                        ) { Text("Done") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
                properties = DialogProperties(usePlatformDefaultWidth = false),
                modifier = Modifier.padding(horizontal = 16.dp)
        ) { DatePicker(state = datePickerState) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
        selectedDate: Calendar,
        onTimeSelected: (Calendar) -> Unit,
        onDismiss: () -> Unit
) {
        val timePickerState =
                rememberTimePickerState(
                        initialHour = selectedDate.get(Calendar.HOUR_OF_DAY),
                        initialMinute = selectedDate.get(Calendar.MINUTE),
                        is24Hour = false
                )

        AlertDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                        TextButton(
                                onClick = {
                                        val cal = selectedDate.clone() as Calendar
                                        cal.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                        cal.set(Calendar.MINUTE, timePickerState.minute)
                                        onTimeSelected(cal)
                                }
                        ) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
                title = { Text("Select Time") },
                text = {
                        Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                        ) { TimePicker(state = timePickerState) }
                }
        )
}
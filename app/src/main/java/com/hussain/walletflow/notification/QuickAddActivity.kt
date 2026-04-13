package com.hussain.walletflow.notification

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hussain.walletflow.R
import com.hussain.walletflow.data.CustomItemsRepository
import com.hussain.walletflow.data.Transaction
import com.hussain.walletflow.data.TransactionCategories
import com.hussain.walletflow.data.TransactionDatabase
import com.hussain.walletflow.data.TransactionType
import com.hussain.walletflow.ui.theme.TransactionTrackerTheme
import com.hussain.walletflow.utils.getCategoryColor
import com.hussain.walletflow.utils.getCategoryIcon
import com.hussain.walletflow.utils.getPaymentChipColor
import com.hussain.walletflow.utils.getPaymentIcon
import com.hussain.walletflow.utils.registerCustomCategories
import com.hussain.walletflow.utils.registerCustomPaymentMethods
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class QuickAddActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes = window.attributes.also { it.dimAmount = 0.6f }
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = android.graphics.Color.TRANSPARENT

        val txId    = intent.getLongExtra(SmsNotificationHelper.EXTRA_TRANSACTION_ID, -1L)
        val notifId = intent.getIntExtra(SmsNotificationHelper.EXTRA_NOTIFICATION_ID, -1)
        setContent {
            TransactionTrackerTheme {
                QuickAddSheet(
                    transactionId = txId,
                    notifId       = notifId,
                    onDone        = { finish() },
                    onCancel      = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun QuickAddSheet(
    transactionId: Long,
    notifId: Int,
    onDone: () -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    val customItemsRepo = remember { CustomItemsRepository(context) }
    val customCategories by customItemsRepo.customCategoriesFlow.collectAsState(initial = emptyList())
    val customPaymentMethods by customItemsRepo.customPaymentMethodsFlow.collectAsState(initial = emptyList())

    LaunchedEffect(customCategories) { registerCustomCategories(customCategories) }
    LaunchedEffect(customPaymentMethods) { registerCustomPaymentMethods(customPaymentMethods) }

    var transaction       by remember { mutableStateOf<Transaction?>(null) }
    var remark            by remember { mutableStateOf("") }
    var category          by remember { mutableStateOf("") }
    var payment           by remember { mutableStateOf("") }
    var isSaving          by remember { mutableStateOf(false) }
    var categoryExpanded  by remember { mutableStateOf(true) }
    var paymentExpanded   by remember { mutableStateOf(true) }
    var currencySymbol by remember { mutableStateOf("₹") }

    LaunchedEffect(Unit) {
        val activity = context as? QuickAddActivity ?: return@LaunchedEffect
        val i = activity.intent
        val tx = Transaction(
            id               = i.getLongExtra(SmsNotificationHelper.EXTRA_TRANSACTION_ID, -1L),
            date             = System.currentTimeMillis(),
            amount           = i.getDoubleExtra("tx_amount", 0.0),
            type             = TransactionType.valueOf(i.getStringExtra("tx_type") ?: "EXPENSE"),
            bankName         = i.getStringExtra("tx_bank") ?: "",
            accountLastFour  = i.getStringExtra("tx_account") ?: "",
            instrumentType   = i.getStringExtra("tx_instrument") ?: "UPI",
            remark           = i.getStringExtra("tx_remark") ?: "",
            category         = i.getStringExtra("tx_category") ?: "",
            paymentMethod    = i.getStringExtra("tx_payment") ?: "",
            originalSms      = i.getStringExtra("tx_original_sms") ?: "",
            isAddedToMonthly = false
        )
        transaction = tx
        currencySymbol = i.getStringExtra("tx_currency_symbol") ?: "₹"
//        remark   = tx.remark
        remark = when {
            tx.originalSms.isNotEmpty() && tx.originalSms != "Manual entry" && tx.originalSms != "TEST_SMS" ->
                tx.originalSms.replace("\n", " ").replace("\r", " ").trim()
            else -> tx.remark
        }
        category = tx.category
        payment  = tx.paymentMethod
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.92f)
                .clickable(enabled = false, onClick = {})
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            val tx = transaction
            if (tx == null) {
                Box(
                    Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                val typeString = if (tx.type == TransactionType.INCOME) "income" else "expense"
                val categories = if (tx.type == TransactionType.INCOME)
                    TransactionCategories.INCOME_CATEGORIES + customCategories.filter { it.type == "income" }.map { it.name }
                else
                    TransactionCategories.EXPENSE_CATEGORIES + customCategories.filter { it.type == "expense" }.map { it.name }

                val paymentMethods = TransactionCategories.PAYMENT_METHODS + customPaymentMethods.map { it.name }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // ── Drag handle ───────────────────────────────────────────
                    Box(
                        Modifier
                            .width(40.dp).height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(Modifier.height(12.dp))

                    // ── Header ────────────────────────────────────────────────
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ){
                        Text(
                            "Quick Add Transaction",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
//                        IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
//                            Icon(
//                                Icons.Default.Close,
//                                contentDescription = "Close",
//                                tint = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Amount card ───────────────────────────────────────────
                    val amtColor = if (tx.type == TransactionType.INCOME)
                        Color(0xFF43A047) else Color(0xFFE53935)
                    val amtLabel = if (tx.type == TransactionType.INCOME) "Credited" else "Debited"

                    Surface(
                        shape    = RoundedCornerShape(16.dp),
                        color    = amtColor.copy(alpha = 0.08f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "$currencySymbol${formatAmount(tx.amount)}",
                                    color      = amtColor,
                                    fontSize   = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "$amtLabel  •  ${tx.bankName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50.dp),
                                color = amtColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    tx.instrumentType,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = amtColor
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Remark ────────────────────────────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(24.dp),
                        colors   = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.EditNote,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                                tint     = MaterialTheme.colorScheme.primary
                            )
                            TextField(
                                value         = remark,
                                onValueChange = { remark = it },
                                placeholder   = {
                                    Text(
                                        "What was this for? ",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                colors   = TextFieldDefaults.colors(
                                    focusedContainerColor   = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor   = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                maxLines = 3
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Category card (collapsible) ───────────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(24.dp),
                        colors   = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Header row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { categoryExpanded = !categoryExpanded }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Category,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint     = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "Category",
                                            style      = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (category.isNotEmpty()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    getCategoryIcon(category),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp),
                                                    tint     = getCategoryColor(category)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    category,
                                                    style      = MaterialTheme.typography.bodySmall,
                                                    color      = getCategoryColor(category),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                                Icon(
                                    if (categoryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Chips
                            AnimatedVisibility(
                                visible = categoryExpanded,
                                enter   = expandVertically(),
                                exit    = shrinkVertically()
                            ) {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                                ) {
                                    categories.forEach { item ->
                                        val isSelected = category == item
                                        val chipColor  = getCategoryColor(item)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50.dp))
                                                .then(
                                                    if (isSelected)
                                                        Modifier.border(1.5.dp, chipColor, RoundedCornerShape(50.dp))
                                                    else Modifier
                                                )
                                                .background(
                                                    if (isSelected) chipColor.copy(alpha = 0.1f)
                                                    else MaterialTheme.colorScheme.surface
                                                )
                                                .clickable {
                                                    category = if (category == item) "" else item
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment     = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    getCategoryIcon(item),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint     = chipColor
                                                )
                                                Text(
                                                    item,
                                                    style      = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color      = if (isSelected) chipColor
                                                    else MaterialTheme.colorScheme.onSurface,
                                                    maxLines   = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // ── Payment method card (collapsible) ─────────────────────
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(24.dp),
                        colors   = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Header row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { paymentExpanded = !paymentExpanded }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Payment,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint     = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            "Payment Method",
                                            style      = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (payment.isNotEmpty()) {
                                            val chipColor = getPaymentChipColor(payment)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                when {
                                                    payment == "GPay" -> Image(
                                                        painter = painterResource(R.drawable.gpay),
                                                        contentDescription = "GPay",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    payment == "PhonePe" -> Image(
                                                        painter = painterResource(R.drawable.phonepe),
                                                        contentDescription = "PhonePe",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    else -> Icon(
                                                        getPaymentIcon(payment),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp),
                                                        tint     = chipColor
                                                    )
                                                }
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    payment,
                                                    style      = MaterialTheme.typography.bodySmall,
                                                    color      = chipColor,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }
                                    }
                                }
                                Icon(
                                    if (paymentExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Toggle",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Chips
                            AnimatedVisibility(
                                visible = paymentExpanded,
                                enter   = expandVertically(),
                                exit    = shrinkVertically()
                            ) {
                                FlowRow(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                                ) {
                                    paymentMethods.forEach { method ->
                                        val isSelected = payment == method
                                        val chipColor  = getPaymentChipColor(method)
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50.dp))
                                                .then(
                                                    if (isSelected)
                                                        Modifier.border(1.5.dp, chipColor, RoundedCornerShape(50.dp))
                                                    else Modifier
                                                )
                                                .background(
                                                    if (isSelected) chipColor.copy(alpha = 0.12f)
                                                    else MaterialTheme.colorScheme.surface
                                                )
                                                .clickable { payment = method }
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Row(
                                                verticalAlignment     = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                when {
                                                    method == "GPay" -> Image(
                                                        painter = painterResource(R.drawable.gpay),
                                                        contentDescription = "GPay",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    method == "PhonePe" -> Image(
                                                        painter = painterResource(R.drawable.phonepe),
                                                        contentDescription = "PhonePe",
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    else -> Icon(
                                                        getPaymentIcon(method),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp),
                                                        tint     = chipColor
                                                    )
                                                }
                                                Text(
                                                    method,
                                                    style      = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color      = if (isSelected) chipColor
                                                    else MaterialTheme.colorScheme.onSurface,
                                                    maxLines   = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Buttons ───────────────────────────────────────────────
                    Button(
                        onClick = {
                            isSaving = true
                            scope.launch {
                                val dao = TransactionDatabase.getDatabase(context).transactionDao()
                                withContext(Dispatchers.IO) {
                                    val updated = tx.copy(
                                        remark           = remark,
                                        category         = category,
                                        paymentMethod    = payment,
                                        isAddedToMonthly = true,
                                        originalSms      = ""
                                    )
                                    val exists = dao.getTransactionById(updated.id) != null
                                    if (exists) dao.update(updated) else dao.insert(updated)
                                }
                                SmsNotificationHelper.cancel(context, notifId)
                                onDone()
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(50.dp),
                        enabled  = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save & Add to Month", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick  = onCancel,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape    = RoundedCornerShape(50.dp)
                    ) { Text("Cancel") }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

private fun formatAmount(amount: Double): String =
    NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
        minimumFractionDigits = 2; maximumFractionDigits = 2
    }.format(amount)
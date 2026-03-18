package com.hussain.walletflow.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// ── Data model ────────────────────────────────────────────────────────────────
data class AccountChip(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val isAll: Boolean = false
)

data class PassbookFilter(
    val transactionType: String = "All",
    val selectedAccountIds: Set<String> = emptySet(),
    val dateRangeLabel: String? = null,
    val startDateMs: Long? = null,
    val endDateMs: Long? = null
) {
    val selectedAccountId: String
        get() = if (selectedAccountIds.isEmpty()) "all" else selectedAccountIds.first()
}

// ── Bottom Sheet ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    currentFilter: PassbookFilter,
    accounts: List<AccountChip>,
    onApply: (PassbookFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var draftType       by remember { mutableStateOf(currentFilter.transactionType) }
    val draftAccountIds = remember {
        mutableStateSetOf<String>().also { it.addAll(currentFilter.selectedAccountIds) }
    }
    var draftRangeLabel by remember { mutableStateOf(currentFilter.dateRangeLabel) }
    var draftStartMs    by remember { mutableStateOf(currentFilter.startDateMs) }
    var draftEndMs      by remember { mutableStateOf(currentFilter.endDateMs) }

    val datePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = draftStartMs,
        initialSelectedEndDateMillis   = draftEndMs
    )
    var showDatePicker by remember { mutableStateOf(false) }

    val uniqueAccounts = remember(accounts) {
        accounts.filter { !it.isAll }.distinctBy { it.id }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor   = MaterialTheme.colorScheme.surface,
        tonalElevation   = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Title ────────────────────────────────────────────────────────
            Text(
                text       = "Filter",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
            )

            // ── 1. Date Range ────────────────────────────────────────────────
            Surface(
                onClick        = { showDatePicker = true },
                modifier       = Modifier.fillMaxWidth(),
                shape          = RoundedCornerShape(14.dp),
                color          = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier              = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text  = draftRangeLabel ?: "Select Date Range",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (draftRangeLabel != null)
                            MaterialTheme.colorScheme.onSurface
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── 2. M3 Expressive connected ToggleButton group ────────────────
            // Uses ToggleButton with ButtonGroupDefaults.connected*ButtonShapes()
            // which gives the real M3 animated shape morph on press/select.
            val typeOptions = listOf("All", "Income", "Expense")

            @OptIn(ExperimentalMaterial3ExpressiveApi::class)
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                typeOptions.forEachIndexed { index, option ->
                    val isSelected = draftType == option
                    val shapes = when (index) {
                        0                      -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                        typeOptions.lastIndex  -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                        else                   -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                    }
                    ToggleButton(
                        checked         = isSelected,
                        onCheckedChange = { if (it) draftType = option },
                        modifier        = Modifier.weight(1f),
                        shapes          = shapes,
                        colors          = ToggleButtonDefaults.toggleButtonColors(
                            checkedContainerColor   = MaterialTheme.colorScheme.primary,
                            checkedContentColor     = MaterialTheme.colorScheme.onPrimary,
                            containerColor          = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor            = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Text(
                            text       = option,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize   = 14.sp
                        )
                    }
                }
            }

            // ── 3. Select account — wrapping chips ───────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text       = "Select account",
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color      = MaterialTheme.colorScheme.primary
                )

                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    AccountFilterChip(
                        icon       = Icons.Default.Wallet,
                        label      = "All",
                        isSelected = draftAccountIds.isEmpty(),
                        onClick    = { draftAccountIds.clear() }
                    )
                    uniqueAccounts.forEach { account ->
                        val isSelected = draftAccountIds.contains(account.id)
                        AccountFilterChip(
                            icon       = account.icon,
                            label      = account.label,
                            isSelected = isSelected,
                            onClick    = {
                                if (isSelected) draftAccountIds.remove(account.id)
                                else            draftAccountIds.add(account.id)
                            }
                        )
                    }
                }
            }

            // ── Action buttons ───────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick  = {
                        draftType = "All"
                        draftAccountIds.clear()
                        draftRangeLabel = null
                        draftStartMs    = null
                        draftEndMs      = null
                    },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Text("Clear Filters", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        onApply(
                            PassbookFilter(
                                transactionType    = draftType,
                                selectedAccountIds = draftAccountIds.toSet(),
                                dateRangeLabel     = draftRangeLabel,
                                startDateMs        = draftStartMs,
                                endDateMs          = draftEndMs
                            )
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Text("Done", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // ── M3 Date Range Picker — full Dialog so the calendar isn't clipped ────
    if (showDatePicker) {
        Dialog(
            onDismissRequest = { showDatePicker = false },
            properties       = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape         = RoundedCornerShape(28.dp),
                color         = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Column {
                    DateRangePicker(
                        state    = datePickerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        // Put Save / Cancel in the picker's own title slot
                        title = {
                            Row(
                                modifier              = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 8.dp, top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = { showDatePicker = false }) {
                                    Text("Cancel")
                                }
                                TextButton(
                                    onClick = {
                                        val startMs = datePickerState.selectedStartDateMillis
                                        val endMs   = datePickerState.selectedEndDateMillis
                                        if (startMs != null) {
                                            draftStartMs    = startMs
                                            draftEndMs      = endMs
                                            draftRangeLabel = buildDateRangeLabel(startMs, endMs)
                                        }
                                        showDatePicker = false
                                    },
                                    enabled = datePickerState.selectedStartDateMillis != null
                                ) {
                                    Text("Save", fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        headline = {
                            val startMs = datePickerState.selectedStartDateMillis
                            val endMs   = datePickerState.selectedEndDateMillis
                            val fmt     = remember { java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()) }
                            Text(
                                text = when {
                                    startMs == null              -> "Select start date"
                                    endMs == null                -> "${fmt.format(java.util.Date(startMs))} – Select end date"
                                    else                         -> "${fmt.format(java.util.Date(startMs))} – ${fmt.format(java.util.Date(endMs))}"
                                },
                                style    = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                                color    = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        showModeToggle = true
                    )
                }
            }
        }
    }
}

// ── Account chip ─────────────────────────────────────────────────────────────
@Composable
private fun AccountFilterChip(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val chipShape = RoundedCornerShape(50.dp)

    val bgColor by animateColorAsState(
        targetValue   = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label         = "chipBg_$label"
    )
    val contentColor by animateColorAsState(
        targetValue   = if (isSelected)
            MaterialTheme.colorScheme.onPrimaryContainer
        else
            MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label         = "chipContent_$label"
    )
    val borderColor by animateColorAsState(
        targetValue   = if (isSelected)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 500f),
        label         = "chipBorder_$label"
    )

    Row(
        modifier = Modifier
            .clip(chipShape)
            .background(bgColor)
            .border(1.dp, borderColor, chipShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            modifier           = Modifier.size(15.dp),
            tint               = contentColor
        )
        Text(
            text       = label,
            fontSize   = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color      = contentColor
        )
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────
private fun buildDateRangeLabel(startMs: Long, endMs: Long?): String {
    val fmt = java.text.SimpleDateFormat("d MMM", java.util.Locale.getDefault())
    return if (endMs != null && endMs != startMs)
        "${fmt.format(java.util.Date(startMs))} – ${fmt.format(java.util.Date(endMs))}"
    else
        fmt.format(java.util.Date(startMs))
}
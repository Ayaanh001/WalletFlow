package com.hussain.walletflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hussain.walletflow.data.CurrencyData
import com.hussain.walletflow.data.Transaction
import com.hussain.walletflow.data.TransactionType
import com.hussain.walletflow.data.UserPreferencesRepository
import com.hussain.walletflow.notification.SmsNotificationHelper
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.res.painterResource
import com.hussain.walletflow.ui.components.ClickableTile
import com.hussain.walletflow.ui.components.GroupSurface
import com.hussain.walletflow.ui.components.HapticSwitch
import com.hussain.walletflow.ui.components.SettingTile
import com.hussain.walletflow.ui.settings.SettingsImportBundle
import com.hussain.walletflow.ui.theme.ThemeMode
import com.hussain.walletflow.viewmodel.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun SettingsSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(bottom = 4.dp, start = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsAppearanceSection(viewModel: SettingsViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val haptic = LocalHapticFeedback.current

    SettingsSectionHeader("Appearance", Modifier.padding(top = 0.dp))
    ThemeCard(
        selectedTheme = themeMode,
        onThemeChange = { mode ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.setThemeMode(mode)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ThemeCard(
    selectedTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
            ) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Theme",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            val options = ThemeMode.entries
            val count = options.size

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                options.forEachIndexed { index, option ->
                    val isSelected = selectedTheme == option

                    val shapes =
                        when {
                            count == 1 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            index == count - 1 -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }

                    ToggleButton(
                        modifier = Modifier.weight(1f).height(44.dp),
                        checked = isSelected,
                        onCheckedChange = { if (it) onThemeChange(option) },
                        shapes = shapes,
                        colors =
                            ToggleButtonDefaults.toggleButtonColors(
                                checkedContainerColor = MaterialTheme.colorScheme.primary,
                                checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                    ) {
                        Icon(
                            imageVector =
                                when (option) {
                                    ThemeMode.AUTO -> Icons.Default.BrightnessAuto
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text =
                                when (option) {
                                    ThemeMode.AUTO -> "Auto"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
@Composable
fun SettingsProfileSection(
    userName: String,
    nameInput: TextFieldValue,
    onNameInputChange: (TextFieldValue) -> Unit,
    isEditingName: Boolean,
    onEditingNameChange: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    prefsRepository: UserPreferencesRepository,
    scope: CoroutineScope
) {
    SettingsSectionHeader("Profile", Modifier.padding(top = 4.dp))
    GroupSurface(count = 1) { _, shape ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    if (isEditingName) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = onNameInputChange,
                            placeholder = { Text("User name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).focusRequester(focusRequester),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions =
                            KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    scope.launch {
                                        prefsRepository.updateName(nameInput.text.trim())
                                    }
                                    onEditingNameChange(false)
                                }
                            )
                        )
                        LaunchedEffect(Unit) {
                            focusRequester.requestFocus()
                        }
                    } else {
                        Column {
                            Text(
                                text = userName.ifEmpty { "Not set" },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Your display name",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (isEditingName) {
                    IconButton(
                        onClick = {
                            scope.launch { prefsRepository.updateName(nameInput.text.trim()) }
                            onEditingNameChange(false)
                        }
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Save",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    IconButton(onClick = { onEditingNameChange(true) }) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit name",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsDebugSection(
    prefsRepository: UserPreferencesRepository,
    scope: CoroutineScope
) {
    val context = LocalContext.current
    SettingsSectionHeader("Debug", Modifier.padding(top = 4.dp))
    GroupSurface(count = 1) { _, shape ->
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = shape,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Test SMS Notification",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Fires a fake bank SMS notification to test the pipeline",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val currencyCode = prefsRepository.currencyFlow.first()
                            val currencySymbol = CurrencyData.currencies.find { it.code == currencyCode }?.symbol ?: currencyCode
                            val fakeExpense = Transaction(
                                id = 99991L,
                                date = System.currentTimeMillis(),
                                amount = 1234.50,
                                type = TransactionType.EXPENSE,
                                category = "Food",
                                bankName = "HDFC Bank",
                                accountLastFour = "8821",
                                instrumentType = "UPI",
                                remark = "Transaction with zomato@upi",
                                originalSms = "TEST_SMS",
                                paymentMethod = "",
                                isAddedToMonthly = false
                            )
                            SmsNotificationHelper.postTransactionNotification(context, fakeExpense, currencySymbol)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🔴  Test Expense Notification")
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val currencyCode = prefsRepository.currencyFlow.first()
                            val currencySymbol = CurrencyData.currencies.find { it.code == currencyCode }?.symbol ?: currencyCode
                            val fakeIncome = Transaction(
                                id = 99992L,
                                date = System.currentTimeMillis(),
                                amount = 50000.00,
                                type = TransactionType.INCOME,
                                category = "Salary",
                                bankName = "SBI",
                                accountLastFour = "4412",
                                instrumentType = "ACCOUNT",
                                remark = "Salary credited",
                                originalSms = "TEST_SMS",
                                paymentMethod = "",
                                isAddedToMonthly = false
                            )
                            SmsNotificationHelper.postTransactionNotification(context, fakeIncome, currencySymbol)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("🟢  Test Income Notification")
                }
            }
        }
    }
}

@Composable
fun SettingsPreferencesSection(
    selectedCurrency: String,
    onCurrencyClick: () -> Unit,
    deleteFromPassbook: Boolean,
    onDeleteFromPassbookChange: (Boolean) -> Unit
) {
    SettingsSectionHeader("Preferences", Modifier.padding(top = 4.dp))
    GroupSurface(count = 2) { index, shape ->
        when (index) {
            0 -> ClickableTile(
                title = "Set currency",
                subtitle = "Choose your preferred currency",
                onClick = onCurrencyClick,
                shape = shape,
                icon = Icons.Default.Payments,
                iconColor = androidx.compose.ui.graphics.Color(0xFF34A853), // Green
                trailing = {
                    val currencyObj = remember(selectedCurrency) {
                        CurrencyData.currencies.find { it.code == selectedCurrency }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = currencyObj?.let { "${it.symbol}  ${it.code}" } ?: selectedCurrency,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
            1 -> SettingTile(
                title = "Delete from Passbook",
                subtitle = if (deleteFromPassbook) "Transactions are removed after adding"
                else "Transactions are kept after adding",
                checked = deleteFromPassbook,
                onCheckedChange = onDeleteFromPassbookChange,
                shape = shape,
                icon = Icons.Default.Delete,
                iconColor = androidx.compose.ui.graphics.Color(0xFFEA4335) // Red
            )
        }
    }
}

@Composable
fun SettingsPrivacySection(
    appLockEnabled: Boolean,
    hideBalance: Boolean,
    hideIncome: Boolean,
    prefsRepository: UserPreferencesRepository,
    scope: CoroutineScope
) {
    SettingsSectionHeader("Privacy", Modifier.padding(top = 4.dp))
    GroupSurface(count = 3) { index, shape ->
        when (index) {
            0 -> SettingTile(
                title = "App Lock",
                subtitle = if (appLockEnabled) "Biometric lock enabled" else "Unlock with biometrics",
                checked = appLockEnabled,
                onCheckedChange = { scope.launch { prefsRepository.updateAppLockEnabled(it) } },
                shape = shape,
                icon = Icons.Default.Fingerprint,
                iconColor = androidx.compose.ui.graphics.Color(0xFF4285F4) // Blue
            )
            1 -> SettingTile(
                title = "Hide Available Balance",
                subtitle = if (hideBalance) "Tap balance to reveal for 5s" else "Balance visible on home",
                checked = hideBalance,
                onCheckedChange = { scope.launch { prefsRepository.updateHideBalance(it) } },
                shape = shape,
                icon = if (hideBalance) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                iconColor = androidx.compose.ui.graphics.Color(0xFF34A853) // Green
            )
            2 -> SettingTile(
                title = "Hide Income",
                subtitle = if (hideIncome) "Tap income to reveal for 5s" else "Income visible on home",
                checked = hideIncome,
                onCheckedChange = { scope.launch { prefsRepository.updateHideIncome(it) } },
                shape = shape,
                icon = if (hideIncome) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                iconColor = androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
            )
        }
    }
}

@Composable
fun SettingsDataSection(
    import: SettingsImportBundle,
    isExporting: Boolean,
    exportDone: String?,
    onExportClick: () -> Unit
) {
    SettingsSectionHeader("Data", Modifier.padding(top = 4.dp))
    GroupSurface(count = 2) { index, shape ->
        when (index) {
            0 -> ClickableTile(
                title = "Export Backup",
                subtitle = if (isExporting) "Exporting…" else exportDone ?: "Save all transactions as CSV",
                onClick = onExportClick,
                shape = shape,
                icon = if (isExporting) null else Icons.Default.FileUpload,
                iconColor = androidx.compose.ui.graphics.Color(0xFF2196F3), // Blue
                trailing = if (isExporting) {
                    {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                } else null
            )
            1 -> com.hussain.walletflow.ui.settings.ImportTransactionsCard(
                import = import,
                modifier = Modifier.clip(shape)
            )
        }
    }
}

@Composable
fun SettingsAboutSection() {
    val context = LocalContext.current
    SettingsSectionHeader("About", Modifier.padding(top = 4.dp))
    GroupSurface(count = 4) { index, shape ->
        when (index) {
            0 -> ClickableTile(
                title = "Ayaan Hussain",
                subtitle = "Developer",
                onClick = {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/Ayaanh001")
                        )
                    )
                },
                shape = shape,
                icon = androidx.compose.ui.res.painterResource(id = com.hussain.walletflow.R.drawable.ah_logo),
                trailing = null
            )
            1 -> ClickableTile(
                title = "GitHub",
                subtitle = "Source code repository",
                onClick = {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://github.com/Ayaanh001/WalletFlow")
                        )
                    )
                },
                shape = shape,
                icon = androidx.compose.ui.res.painterResource(id = com.hussain.walletflow.R.drawable.ic_github),
                iconContainerColor = androidx.compose.ui.graphics.Color.Black,
                trailing = null
            )
            2 -> ClickableTile(
                title = "Telegram",
                subtitle = "Community & Support",
                onClick = {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://t.me/Ahacd1")
                        )
                    )
                },
                shape = shape,
                icon = androidx.compose.ui.graphics.vector.ImageVector.vectorResource(id = com.hussain.walletflow.R.drawable.telegram),
                iconColor = androidx.compose.ui.graphics.Color(0xFF24A1DE),
                trailing = null
            )
            3 -> Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = shape,
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Version",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = com.hussain.walletflow.BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

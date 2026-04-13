package com.hussain.walletflow.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.hussain.walletflow.data.UserPreferencesRepository
import com.hussain.walletflow.ui.settings.ImportTransactionsCard
import com.hussain.walletflow.ui.settings.SettingsImportDialogs
import com.hussain.walletflow.ui.settings.rememberSettingsImport
import com.hussain.walletflow.utils.BackupExporter
import com.hussain.walletflow.viewmodel.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: TransactionViewModel,
    onNavigateToImport: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefsRepository = remember { UserPreferencesRepository(context) }
    val selectedCurrency by
        prefsRepository.currencyFlow.collectAsState(
            initial = UserPreferencesRepository.DEFAULT_CURRENCY
        )
    val userName by prefsRepository.nameFlow.collectAsState(initial = "")
    var nameInput by
        remember(userName) {
            mutableStateOf(TextFieldValue(userName, TextRange(userName.length)))
        }
    var isEditingName by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val deleteFromPassbook by
        prefsRepository.deleteFromPassbookFlow.collectAsState(initial = true)

    var showCurrencyDialog by remember { mutableStateOf(false) }
    val appLockEnabled by prefsRepository.appLockEnabledFlow.collectAsState(initial = false)
    val hideBalance by prefsRepository.hideBalanceFlow.collectAsState(initial = false)
    val hideIncome by prefsRepository.hideIncomeFlow.collectAsState(initial = false)
    var isExporting by remember { mutableStateOf(false) }
    var exportDone by remember { mutableStateOf<String?>(null) }

    val import = rememberSettingsImport(viewModel, onNavigateToImport)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    FilledIconButton(
                        modifier = Modifier.padding(12.dp),
                        onClick = onBack,
                        colors =
                            IconButtonDefaults.filledIconButtonColors(
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
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
            )
        }
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsProfileSection(
                userName = userName,
                nameInput = nameInput,
                onNameInputChange = { nameInput = it },
                isEditingName = isEditingName,
                onEditingNameChange = { isEditingName = it },
                focusRequester = focusRequester,
                prefsRepository = prefsRepository,
                scope = scope
            )

            SettingsDebugSection(
                prefsRepository = prefsRepository,
                scope = scope
            )

            SettingsSectionHeader("Preferences")
            SettingsCurrencyCard(selectedCurrency) { showCurrencyDialog = true }
            SettingsDeleteFromPassbookCard(deleteFromPassbook, prefsRepository, scope)

            SettingsPrivacySection(
                appLockEnabled = appLockEnabled,
                hideBalance = hideBalance,
                hideIncome = hideIncome,
                prefsRepository = prefsRepository,
                scope = scope
            )

            SettingsSectionHeader("Data", Modifier.padding(top = 4.dp))
            ImportTransactionsCard(import)

            SettingsExportCard(isExporting, exportDone) {
                isExporting = true
                scope.launch {
                    val result =
                        withContext(Dispatchers.IO) {
                            BackupExporter.exportToCsv(context)
                        }
                    isExporting = false
                    exportDone = result
                }
            }
        }
    }

    if (showCurrencyDialog) {
        SettingsDialog(onDismiss = { showCurrencyDialog = false })
    }

    SettingsImportDialogs(import)
}

@Composable
fun HapticSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    thumbContent: (@Composable () -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    Switch(
        checked = checked,
        onCheckedChange = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onCheckedChange(it)
        },
        thumbContent = thumbContent
    )
}
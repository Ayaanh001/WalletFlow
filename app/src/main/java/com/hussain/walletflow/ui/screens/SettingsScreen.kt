package com.hussain.walletflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hussain.walletflow.data.UserPreferencesRepository
import com.hussain.walletflow.ui.components.HapticSwitch
import com.hussain.walletflow.ui.settings.SettingsImportDialogs
import com.hussain.walletflow.ui.settings.rememberSettingsImport
import com.hussain.walletflow.utils.BackupExporter
import com.hussain.walletflow.viewmodel.SettingsViewModel
import com.hussain.walletflow.viewmodel.TransactionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: TransactionViewModel,
    onNavigateToImport: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
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

    val scrollState = rememberScrollState()
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier =
            Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier =
                Modifier.fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(top = statusBarPadding + 64.dp)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SettingsAppearanceSection(settingsViewModel)

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

            SettingsPreferencesSection(
                selectedCurrency = selectedCurrency,
                onCurrencyClick = { showCurrencyDialog = true },
                deleteFromPassbook = deleteFromPassbook,
                onDeleteFromPassbookChange = {
                    scope.launch { prefsRepository.updateDeleteFromPassbook(it) }
                }
            )

            SettingsPrivacySection(
                appLockEnabled = appLockEnabled,
                hideBalance = hideBalance,
                hideIncome = hideIncome,
                prefsRepository = prefsRepository,
                scope = scope
            )

            SettingsDataSection(
                import = import,
                isExporting = isExporting,
                exportDone = exportDone,
                onExportClick = {
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
            )

            SettingsAboutSection()
            
            Spacer(Modifier.height(32.dp))
        }

        // Sticky Header
        Surface(
            color = MaterialTheme.colorScheme.background,
            shadowElevation = if (scrollState.value > 0) 2.dp else 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier =
                    Modifier.fillMaxWidth()
                        .statusBarsPadding()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    if (showCurrencyDialog) {
        SettingsDialog(onDismiss = { showCurrencyDialog = false })
    }

    SettingsImportDialogs(import)
}
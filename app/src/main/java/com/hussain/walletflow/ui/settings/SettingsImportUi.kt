package com.hussain.walletflow.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.hussain.walletflow.viewmodel.TransactionViewModel
import kotlinx.coroutines.launch

/**
 * Holds import UI state and actions. Produced by [rememberSettingsImport].
 */
data class SettingsImportBundle(
    val isImporting: Boolean,
    val restoreMessage: String?,
    val showPasswordDialog: Boolean,
    val passwordInput: String,
    val passwordVisible: Boolean,
    val passwordError: Boolean,
    val onPickFile: () -> Unit,
    val onDismissRestoreMessage: () -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onTogglePasswordVisible: () -> Unit,
    val onUnlockClick: () -> Unit,
    val onDismissPasswordDialog: () -> Unit
)

@Composable
fun rememberSettingsImport(
    viewModel: TransactionViewModel,
    onNavigateToImport: () -> Unit
): SettingsImportBundle {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isImporting by remember { mutableStateOf(false) }
    var restoreMessage by remember { mutableStateOf<String?>(null) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }
    var pendingFileUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            isImporting = true
            scope.launch {
                when (val outcome = runSettingsImportPipeline(context, uri)) {
                    is SettingsImportPipelineResult.BackupRestored -> {
                        isImporting = false
                        restoreMessage = outcome.message
                    }
                    is SettingsImportPipelineResult.PasswordRequired -> {
                        isImporting = false
                        pendingFileUri = outcome.uri
                        passwordInput = ""
                        passwordError = false
                        showPasswordDialog = true
                    }
                    is SettingsImportPipelineResult.TransactionsParsed -> {
                        isImporting = false
                        viewModel.setPendingImport(outcome.transactions)
                        onNavigateToImport()
                    }
                }
            }
        }

    return SettingsImportBundle(
        isImporting = isImporting,
        restoreMessage = restoreMessage,
        showPasswordDialog = showPasswordDialog,
        passwordInput = passwordInput,
        passwordVisible = passwordVisible,
        passwordError = passwordError,
        onPickFile = {
            filePickerLauncher.launch(SettingsImportMimeTypes.OPEN_DOCUMENT_TYPES)
        },
        onDismissRestoreMessage = { restoreMessage = null },
        onPasswordChange = {
            passwordInput = it
            passwordError = false
        },
        onTogglePasswordVisible = { passwordVisible = !passwordVisible },
        onUnlockClick = click@{
            val uri = pendingFileUri ?: return@click
            isImporting = true
            scope.launch {
                val result = parseImportWithPassword(context, uri, passwordInput)
                isImporting = false
                when {
                    result.passwordRequired -> passwordError = true
                    else -> {
                        showPasswordDialog = false
                        pendingFileUri = null
                        passwordVisible = false
                        viewModel.setPendingImport(result.transactions)
                        onNavigateToImport()
                    }
                }
            }
        },
        onDismissPasswordDialog = {
            showPasswordDialog = false
            pendingFileUri = null
            passwordVisible = false
        }
    )
}

@Composable
fun ImportTransactionsCard(
    import: SettingsImportBundle,
    modifier: Modifier = Modifier
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(enabled = !import.isImporting) { import.onPickFile() },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (import.isImporting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        } else {
                            Icon(
                                Icons.Default.FileUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Column {
                    Text(
                        text = "Import Transactions",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text =
                            if (import.isImporting) "Parsing file…"
                            else "Import CSV, TXT, Excel (XLS/XLSX), or PDF",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!import.isImporting) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun SettingsImportDialogs(import: SettingsImportBundle) {
    if (import.restoreMessage != null) {
        AlertDialog(
            onDismissRequest = import.onDismissRestoreMessage,
            title = { Text("Backup Restored") },
            text = { Text(import.restoreMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = import.onDismissRestoreMessage) { Text("OK") }
            }
        )
    }

    if (import.showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                import.onDismissPasswordDialog()
            },
            icon = {
                Icon(
                    Icons.Default.FileUpload,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("Password Protected File") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "This file is password protected. Please enter the password to continue.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = import.passwordInput,
                        onValueChange = import.onPasswordChange,
                        label = { Text("Password") },
                        singleLine = true,
                        isError = import.passwordError,
                        supportingText =
                            if (import.passwordError) {
                                { Text("Incorrect password. Please try again.") }
                            } else null,
                        visualTransformation =
                            if (import.passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                        keyboardOptions =
                            KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                        trailingIcon = {
                            IconButton(onClick = import.onTogglePasswordVisible) {
                                Icon(
                                    imageVector =
                                        if (import.passwordVisible) Icons.Default.VisibilityOff
                                        else Icons.Default.Visibility,
                                    contentDescription =
                                        if (import.passwordVisible) "Hide password" else "Show password"
                                )
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = import.passwordInput.isNotEmpty() && !import.isImporting,
                    onClick = import.onUnlockClick
                ) {
                    if (import.isImporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Unlock")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = import.onDismissPasswordDialog) { Text("Cancel") }
            }
        )
    }
}

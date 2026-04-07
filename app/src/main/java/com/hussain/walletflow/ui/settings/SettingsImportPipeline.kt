package com.hussain.walletflow.ui.settings

import android.content.Context
import android.net.Uri
import com.hussain.walletflow.utils.BackupExporter
import com.hussain.walletflow.utils.FileImportParser
import com.hussain.walletflow.utils.ParsedTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** MIME types passed to [androidx.activity.result.contract.ActivityResultContracts.OpenDocument]. */
object SettingsImportMimeTypes {
    val OPEN_DOCUMENT_TYPES =
        arrayOf(
            "text/csv",
            "text/comma-separated-values",
            "text/plain",
            "application/pdf",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/octet-stream"
        )
}

sealed class SettingsImportPipelineResult {
    data class BackupRestored(val message: String) : SettingsImportPipelineResult()

    data class TransactionsParsed(val transactions: List<ParsedTransaction>) : SettingsImportPipelineResult()

    data class PasswordRequired(val uri: Uri) : SettingsImportPipelineResult()
}

/**
 * Tries WalletFlow backup restore first, then generic file import (CSV/TXT/Excel/PDF).
 * Runs blocking work on [Dispatchers.IO].
 */
suspend fun runSettingsImportPipeline(context: Context, uri: Uri): SettingsImportPipelineResult {
    val restoreResult =
        withContext(Dispatchers.IO) {
            BackupExporter.tryRestoreFromUri(context, uri)
        }
    if (restoreResult != null) {
        return SettingsImportPipelineResult.BackupRestored(restoreResult)
    }
    val parseResult =
        withContext(Dispatchers.IO) {
            FileImportParser.parseUri(context, uri)
        }
    return when {
        parseResult.passwordRequired -> SettingsImportPipelineResult.PasswordRequired(uri)
        else -> SettingsImportPipelineResult.TransactionsParsed(parseResult.transactions)
    }
}

suspend fun parseImportWithPassword(context: Context, uri: Uri, password: String) =
    withContext(Dispatchers.IO) {
        FileImportParser.parseUriWithPassword(context, uri, password)
    }

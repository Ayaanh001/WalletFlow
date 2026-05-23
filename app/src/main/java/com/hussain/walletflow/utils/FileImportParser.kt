package com.hussain.walletflow.utils

import android.content.Context
import android.net.Uri
import com.hussain.walletflow.data.TransactionType
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.poifs.crypt.Decryptor
import org.apache.poi.poifs.crypt.EncryptionInfo
import org.apache.poi.poifs.filesystem.POIFSFileSystem
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException

/** Wraps the result of parsing a file. */
data class ParseResult(
    val transactions: List<ParsedTransaction>,
    val passwordRequired: Boolean = false
)

data class ParsedTransaction(
    val date: Long,
    val amount: Double,
    val type: TransactionType,
    val narration: String,
    val paymentMethod: String,
    val category: String = "",       // preserved from WalletFlow backup CSV
    val rawLine: String = ""
)

object FileImportParser {

    // ──────────────────────────────────────────────────────────────────────────
    // Public entry point
    // ──────────────────────────────────────────────────────────────────────────

    private fun resolveFileName(context: Context, uri: Uri): String {
        var name = ""
        context.contentResolver.query(uri, null, null, null, null)?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst() && nameIndex >= 0) name = it.getString(nameIndex).lowercase()
        }
        return name.ifEmpty { uri.lastPathSegment?.lowercase() ?: "" }
    }

    fun parseUri(context: Context, uri: Uri): ParseResult {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val fileName = resolveFileName(context, uri)
        val isXlsx = fileName.endsWith(".xlsx") || mimeType.contains("spreadsheetml")
        val isXls = fileName.endsWith(".xls") || mimeType.contains("excel")
        val isExcel = isXlsx || isXls
        val isPdf = fileName.endsWith(".pdf") || mimeType.contains("pdf")

        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            when {
                isExcel -> parseExcel(inputStream, isXlsx)
                isPdf -> parsePdf(context, inputStream)
                else -> ParseResult(parseCsvOrTxt(inputStream))
            }
        }
            ?: ParseResult(emptyList())
    }

    fun parseUriWithPassword(context: Context, uri: Uri, password: String): ParseResult {
        val mimeType = context.contentResolver.getType(uri) ?: ""
        val fileName = resolveFileName(context, uri)
        val isXlsx = fileName.endsWith(".xlsx") || mimeType.contains("spreadsheetml")
        val isPdf = fileName.endsWith(".pdf") || mimeType.contains("pdf")

        return context.contentResolver.openInputStream(uri)?.use { inputStream ->
            when {
                isPdf -> parsePdf(context, inputStream, password)
                else -> parseExcel(inputStream, isXlsx, password)
            }
        }
            ?: ParseResult(emptyList())
    }

    // ──────────────────────────────────────────────────────────────────────────
    // CSV / TXT parser
    // ──────────────────────────────────────────────────────────────────────────

    private fun parseCsvOrTxt(inputStream: InputStream): List<ParsedTransaction> {
        val lines =
            BufferedReader(InputStreamReader(inputStream))
                .readLines()
                .map { it.trim() }
                .filter { it.isNotBlank() }

        return parseLines(lines, isPdf = false)
    }

    private fun parseLines(lines: List<String>, isPdf: Boolean): List<ParsedTransaction> {
        if (lines.size < 2) return emptyList()

        // ── 1. Find the header row ───────────────────────────────────────────
        val keywordSet = setOf(
            "date", "narration", "description", "particulars", "remarks",
            "withdrawal", "widthdrawl", "debit", "deposit", "credit", "amount", "amt",
            "dr", "cr", "txn", "value", "balance", "bal"
        )

        var headerIndex = -1
        var maxMatches = 0

        for (i in 0 until minOf(lines.size, 100)) {
            val lowerLine = lines[i].lowercase()
            var matches = 0
            for (kw in keywordSet) {
                if (lowerLine.contains(kw)) matches++
            }
            if (matches > maxMatches && matches >= 2) {
                maxMatches = matches
                headerIndex = i
            }
        }

        if (headerIndex < 0) return emptyList()

        val headerLine = lines[headerIndex]
        var delimiter = detectDelimiter(headerLine, isPdf)
        var headers = splitLine(headerLine, delimiter).map { it.trim().lowercase() }

        // For PDFs, if double-space splitting failed to find columns, try single space
        if (isPdf && headers.size <= 2 && delimiter == "  ") {
            val singleSpaceHeaders = splitLine(headerLine, " ").map { it.trim().lowercase() }
            if (singleSpaceHeaders.size > headers.size) {
                headers = singleSpaceHeaders
                delimiter = " "
            }
        }

        // ── 2. Map column indices ────────────────────────────────────────────
        val dateCol =
            headers.indexOfFirst { h ->
                h.contains("date") || h.contains("txn") || h.contains("value")
            }
        val narrationCol =
            headers.indexOfFirst { h ->
                h.contains("narration") ||
                        h.contains("description") ||
                        h.contains("particulars") ||
                        h.contains("remarks") ||
                        h.contains("remark") ||
                        h.contains("details")
            }

        // Identify Reference / Cheque column to EXCLUDE it from amount detection
        val refCol = headers.indexOfFirst { h ->
            val lower = h.lowercase()
            lower.contains("ref") || lower.contains("chq") || lower.contains("cheque") || lower.contains("reference") || lower.contains("vch")
        }

        val balanceCol = headers.indexOfFirst { h ->
            h.contains("balance") || h.contains("bal")
        }

        // ── WalletFlow backup-specific columns ───────────────────────────────
        val typeCol     = headers.indexOfFirst { h -> h == "type" }
        val categoryCol = headers.indexOfFirst { h -> h == "category" }
        val paymentCol  = headers.indexOfFirst { h -> h == "payment method" }
        val remarkCol   = headers.indexOfFirst { h -> h == "remark" }

        val isWalletFlowBackup = typeCol >= 0 && categoryCol >= 0

        val withdrawalCol =
            if (isWalletFlowBackup) -1
            else headers.indexOfFirst { h ->
                val lower = h.lowercase()
                (lower.contains("withdrawal") || lower.contains("widthdrawl") ||
                        lower.contains("debit") || lower == "dr") &&
                        !lower.contains("credit") && !lower.contains("balance") && !lower.contains("ref")
            }
        val depositCol =
            if (isWalletFlowBackup) -1
            else headers.indexOfFirst { h ->
                val lower = h.lowercase()
                (lower.contains("deposit") || lower.contains("credit") || lower == "cr") &&
                        !lower.contains("debit") && !lower.contains("balance") && !lower.contains("ref")
            }
        val singleAmountCol =
            if (withdrawalCol < 0 && depositCol < 0)
                headers.indexOfFirst { h ->
                    (h == "amount" || h == "amt") && !h.contains("balance") && !h.contains("ref")
                }
            else -1

        // ── 2.5 Try to find Opening Balance (for PDF math verification) ──────
        var runningBalance: Double? = null
        if (isPdf && balanceCol >= 0) {
            // 1. Look for explicit labels in the lines above the header
            for (j in 0 until headerIndex) {
                val line = lines[j].lowercase()
                if (line.contains("opening") || line.contains("brought") || line.contains("b/f") || line.contains("carried")) {
                    val parts = line.split(Regex("[^0-9,.]")).filter { it.isNotBlank() }
                    for (p in parts.reversed()) {
                        val b = parseAmount(p)
                        if (b != null) {
                            runningBalance = b
                            break
                        }
                    }
                }
                if (runningBalance != null) break
            }

            // 2. Fallback: Search backwards from header for ANY numeric value at the end of a line
            // This often picks up the "Balance" from the line preceding the first transaction.
            if (runningBalance == null) {
                for (j in (headerIndex - 1) downTo 0) {
                    val line = lines[j].trim()
                    if (line.isEmpty()) continue
                    // Extract the last "word" that looks like a number
                    val parts = line.split(Regex("\\s+"))
                    for (p in parts.reversed()) {
                        val b = parseAmount(p)
                        // Heuristic: Opening balances are usually reasonably large and have decimals
                        if (b != null && (p.contains(".") || b > 0)) {
                            runningBalance = b
                            break
                        }
                    }
                    if (runningBalance != null) break
                }
            }
        }

        // ── 3. Parse data rows ───────────────────────────────────────────────
        val results = mutableListOf<ParsedTransaction>()

        for (i in (headerIndex + 1) until lines.size) {
            val line = lines[i]
            if (line.isBlank()) continue

            var cells = splitLine(line, delimiter).map { it.trim().removeSurrounding("\"") }

            // Dynamic fallback for PDF lines that might have less spacing than the header
            if (isPdf && delimiter == "  ") {
                val spaceCells = splitLine(line, " ").map { it.trim().removeSurrounding("\"") }
                // If single space split gives more numeric columns, it's likely more accurate for PDFs
                val spaceNumCount = spaceCells.count { it.any { c -> c.isDigit() } && parseAmount(it) != null }
                val currentNumCount = cells.count { it.any { c -> c.isDigit() } && parseAmount(it) != null }
                if (spaceNumCount > currentNumCount || (spaceCells.size >= headers.size && cells.size < headers.size)) {
                    cells = spaceCells
                }
            }

            if (cells.size <= 1) continue

            val rawDate = if (dateCol >= 0 && dateCol < cells.size) cells[dateCol] else continue
            val parsedDate = parseDate(rawDate) ?: continue

            val narration = when {
                remarkCol >= 0 && remarkCol < cells.size && cells[remarkCol].isNotBlank() ->
                    cells[remarkCol]
                narrationCol >= 0 && narrationCol < cells.size -> cells[narrationCol]
                else -> ""
            }

            var amount: Double? = null
            var type: TransactionType = TransactionType.EXPENSE

            if (isWalletFlowBackup && singleAmountCol >= 0 && singleAmountCol < cells.size) {
                val amt = parseAmount(cells[singleAmountCol])
                if (amt != null) {
                    val typeStr = if (typeCol >= 0 && typeCol < cells.size)
                        cells[typeCol].trim().lowercase() else ""
                    val txType = when {
                        typeStr == "income"  -> TransactionType.INCOME
                        typeStr == "expense" -> TransactionType.EXPENSE
                        amt >= 0             -> TransactionType.INCOME
                        else                 -> TransactionType.EXPENSE
                    }
                    amount = if (amt < 0) -amt else amt
                    type = txType
                }
            } else if (singleAmountCol >= 0 && withdrawalCol < 0 && depositCol < 0
                    && singleAmountCol < cells.size) {
                val amt = parseAmount(cells[singleAmountCol])
                if (amt != null) {
                    amount = if (amt >= 0) amt else -amt
                    type = if (amt >= 0) TransactionType.INCOME else TransactionType.EXPENSE
                }
            } else {
                // PDF / Generic Table Logic
                
                // For PDF, we must be careful with merged cells. 
                // We split each cell by spaces internally to find multiple numbers.
                val rowNumbers = cells.flatMap { cell -> 
                    cell.split(Regex("\\s+")).mapNotNull { parseAmount(it) } 
                }

                val rowBalance = if (isPdf && balanceCol >= 0) {
                    // The balance is almost always the LAST numeric value on the line in bank PDFs
                    rowNumbers.lastOrNull()
                } else null

                // If we have a running balance, use math to be 100% sure
                if (isPdf && rowBalance != null && runningBalance != null) {
                    val diff = rowBalance - runningBalance
                    val absDiff = kotlin.math.abs(diff)
                    if (absDiff > 0.001) {
                        // Check if this difference matches any number in the row
                        // (This helps ignore the Ref No if it's also numeric)
                        val matchedAny = rowNumbers.any { kotlin.math.abs(it - absDiff) < 0.01 }
                        if (matchedAny) {
                            amount = absDiff
                            type = if (diff > 0) TransactionType.INCOME else TransactionType.EXPENSE
                            runningBalance = rowBalance
                        }
                    }
                }

                // Fallback if math didn't work (e.g. first row or math mismatch)
                if (amount == null) {
                    val withdrawal = if (withdrawalCol >= 0 && withdrawalCol < cells.size && withdrawalCol != refCol)
                        cells[withdrawalCol].split(Regex("\\s+")).mapNotNull { parseAmount(it) }.firstOrNull() else null
                    val deposit = if (depositCol >= 0 && depositCol < cells.size && depositCol != refCol)
                        cells[depositCol].split(Regex("\\s+")).mapNotNull { parseAmount(it) }.firstOrNull() else null

                    // Additional check for PDF: if the value we found is actually the balance, it's a false positive
                    val finalWithdrawal = if (withdrawal != null && (rowBalance == null || kotlin.math.abs(withdrawal - rowBalance) > 0.01)) withdrawal else null
                    val finalDeposit = if (deposit != null && (rowBalance == null || kotlin.math.abs(deposit - rowBalance) > 0.01)) deposit else null

                    when {
                        finalWithdrawal != null && finalWithdrawal > 0 -> {
                            amount = finalWithdrawal
                            type = TransactionType.EXPENSE
                        }
                        finalDeposit != null && finalDeposit > 0 -> {
                            amount = finalDeposit
                            type = TransactionType.INCOME
                        }
                        else -> {
                            // Last ditch for PDF first row: find the number that ISN'T the balance and ISN'T the Ref No
                            if (isPdf && rowBalance != null) {
                                val candidates = rowNumbers.filter { it != rowBalance }
                                if (candidates.size == 1) {
                                    amount = candidates[0]
                                    // Guess type based on row content or default to Expense
                                    type = TransactionType.EXPENSE 
                                } else if (candidates.size >= 2) {
                                    // If we have multiple candidates (e.g. Ref No and Amount)
                                    // The Amount is usually the one closer to the withdrawal/deposit columns
                                    // or just NOT the Ref No if we can identify it.
                                    val nonRefCandidates = candidates.filter { c -> 
                                        val idx = cells.indexOfFirst { it.contains(c.toString()) }
                                        idx != refCol
                                    }
                                    if (nonRefCandidates.isNotEmpty()) {
                                        amount = nonRefCandidates.last() // Amount usually comes after Ref No
                                        type = TransactionType.EXPENSE
                                    }
                                }
                            }
                        }
                    }
                    
                    // Update running balance for next row
                    if (isPdf && rowBalance != null) runningBalance = rowBalance
                }
            }

            if (amount == null || amount <= 0) continue

            val paymentMethod = when {
                paymentCol >= 0 && paymentCol < cells.size && cells[paymentCol].isNotBlank() ->
                    cells[paymentCol]
                else -> detectPaymentMethod(narration)
            }

            val category = if (categoryCol >= 0 && categoryCol < cells.size)
                cells[categoryCol] else ""

            results.add(
                ParsedTransaction(
                    date = parsedDate,
                    amount = amount,
                    type = type,
                    narration = narration.ifBlank { "Imported" },
                    paymentMethod = paymentMethod,
                    category = category,
                    rawLine = line
                )
            )
        }

        return results
    }

    private fun parsePdf(
        context: Context,
        inputStream: InputStream,
        password: String? = null
    ): ParseResult {
        PDFBoxResourceLoader.init(context)
        val document: PDDocument = try {
            if (password != null) {
                PDDocument.load(inputStream, password)
            } else {
                PDDocument.load(inputStream)
            }
        } catch (_: InvalidPasswordException) {
            return ParseResult(emptyList(), passwordRequired = true)
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("password") || msg.contains("encrypted")) {
                return ParseResult(emptyList(), passwordRequired = true)
            }
            return ParseResult(emptyList())
        }

        return try {
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true // Essential for multi-column PDFs
            val text = stripper.getText(document)
            document.close()

            val lines = text.split(Regex("\\r?\\n"))
                .map { it.trim() }
                .filter { it.isNotBlank() }

            ParseResult(parseLines(lines, isPdf = true))
        } catch (e: Exception) {
            e.printStackTrace()
            ParseResult(emptyList())
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Excel Parser (XLS / XLSX)
    // ──────────────────────────────────────────────────────────────────────────

    private fun parseExcel(
        inputStream: InputStream,
        isXlsx: Boolean,
        password: String? = null
    ): ParseResult {
        val results = mutableListOf<ParsedTransaction>()
        try {
            val workbook: Workbook = when {
                // ── Explicit password supplied ────────────────────────────────────
                password != null -> {
                    val fs = POIFSFileSystem(inputStream)
                    val encInfo = EncryptionInfo(fs)
                    val decryptor = Decryptor.getInstance(encInfo)
                    if (!decryptor.verifyPassword(password)) {
                        return ParseResult(emptyList(), passwordRequired = true)
                    }
                    XSSFWorkbook(decryptor.getDataStream(fs))
                }

                // ── XLSX (no password yet) ────────────────────────────────────────
                isXlsx -> {
                    // Buffer the stream so we can retry as OLE2 if the first
                    // attempt fails (encrypted xlsx is actually an OLE2 container)
                    val bytes = inputStream.readBytes()
                    try {
                        XSSFWorkbook(bytes.inputStream())
                    } catch (e: Exception) {
                        // If it looks like an OLE2 / encrypted file, flag it
                        try {
                            val fs = POIFSFileSystem(bytes.inputStream())
                            // Successfully parsed as OLE2 → check for encryption
                            if (fs.root.hasEntry(Decryptor.DEFAULT_POIFS_ENTRY)) {
                                return ParseResult(emptyList(), passwordRequired = true)
                            }
                            // OLE2 but not encrypted → try as legacy XLS
                            HSSFWorkbook(fs)
                        } catch (_: Exception) {
                            return ParseResult(emptyList(), passwordRequired = false)
                        }
                    }
                }

                // ── XLS ───────────────────────────────────────────────────────────
                else -> {
                    try {
                        HSSFWorkbook(inputStream)
                    } catch (e: Exception) {
                        val msg = e.message?.lowercase() ?: ""
                        if (msg.contains("password") || msg.contains("encrypt") ||
                            msg.contains("protected")) {
                            return ParseResult(emptyList(), passwordRequired = true)
                        }
                        return ParseResult(emptyList())
                    }
                }
            }

            // ── The rest of your existing sheet-parsing logic is UNCHANGED ────────
            val sheet = workbook.getSheetAt(0) ?: return ParseResult(emptyList())
            val evaluator = workbook.creationHelper.createFormulaEvaluator()

            var headerRowIndex = -1
            val keywordSet = setOf(
                "date", "narration", "description", "particulars", "remarks",
                "withdrawal", "debit", "deposit", "credit", "amount", "amt",
                "dr", "cr", "txn", "value"
            )

            for (r in 0 until minOf(sheet.physicalNumberOfRows, 100)) {
                val row = sheet.getRow(r) ?: continue
                var matchCount = 0
                for (c in 0 until row.lastCellNum) {
                    val cv = getCellValueAsString(row.getCell(c), evaluator).trim().lowercase()
                    if (keywordSet.any { kw -> cv.contains(kw) }) matchCount++
                }
                if (matchCount >= 2) {
                    headerRowIndex = r
                    break
                }
            }

            if (headerRowIndex == -1) return ParseResult(emptyList())

            val headerRow = sheet.getRow(headerRowIndex) ?: return ParseResult(emptyList())
            val firstCellNum = headerRow.firstCellNum.toInt().coerceAtLeast(0)
            val lastCellNum = headerRow.lastCellNum.toInt()
            val headers = (firstCellNum until lastCellNum).map { c ->
                getCellValueAsString(headerRow.getCell(c), evaluator).trim().lowercase()
            }

            val dateCol = headers.indexOfFirst { h ->
                h.contains("date") || h.contains("txn") || h.contains("value date")
            }
            val narrationCol = headers.indexOfFirst { h ->
                h.contains("narration") || h.contains("description") ||
                        h.contains("particulars") || h.contains("remarks") || h.contains("details")
            }
            val withdrawalCol = headers.indexOfFirst { h ->
                (h.contains("withdrawal") || h.contains("debit") || h == "dr") && !h.contains("credit")
            }
            val depositCol = headers.indexOfFirst { h ->
                (h.contains("deposit") || h.contains("credit") || h == "cr") && !h.contains("debit")
            }
            val singleAmountCol = if (withdrawalCol < 0 && depositCol < 0)
                headers.indexOfFirst { h -> h == "amount" || h == "amt" }
            else -1

            if (dateCol < 0) return ParseResult(emptyList())

            for (r in (headerRowIndex + 1)..sheet.lastRowNum) {
                val row = sheet.getRow(r) ?: continue

                fun cellAt(colIdx: Int): String {
                    val absCol = firstCellNum + colIdx
                    return getCellValueAsString(row.getCell(absCol), evaluator)
                }

                fun dateMsAt(colIdx: Int): Long? {
                    val absCol = firstCellNum + colIdx
                    val cell = row.getCell(absCol) ?: return null
                    if (cell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                        return cell.dateCellValue?.time
                    }
                    return parseDate(getCellValueAsString(cell, evaluator))
                }

                val allBlank = (firstCellNum until lastCellNum).all {
                    getCellValueAsString(row.getCell(it), evaluator).isBlank()
                }
                if (allBlank) continue

                val parsedDate = dateMsAt(dateCol) ?: continue
                val narration = if (narrationCol >= 0) cellAt(narrationCol) else ""

                val (amount, type) = when {
                    singleAmountCol >= 0 -> {
                        val amt = parseAmount(cellAt(singleAmountCol)) ?: continue
                        if (amt >= 0) Pair(amt, TransactionType.INCOME)
                        else Pair(-amt, TransactionType.EXPENSE)
                    }
                    else -> {
                        val withdrawal = if (withdrawalCol >= 0) parseAmount(cellAt(withdrawalCol)) else null
                        val deposit = if (depositCol >= 0) parseAmount(cellAt(depositCol)) else null
                        when {
                            withdrawal != null && withdrawal > 0 -> Pair(withdrawal, TransactionType.EXPENSE)
                            deposit != null && deposit > 0 -> Pair(deposit, TransactionType.INCOME)
                            else -> continue
                        }
                    }
                }

                if (amount <= 0) continue

                results.add(
                    ParsedTransaction(
                        date = parsedDate,
                        amount = amount,
                        type = type,
                        narration = narration.ifBlank { "Imported" },
                        paymentMethod = detectPaymentMethod(narration),
                        rawLine = (firstCellNum until lastCellNum).joinToString("|") {
                            getCellValueAsString(row.getCell(it), evaluator)
                        }
                    )
                )
            }
            workbook.close()

        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("password") || msg.contains("encrypt") || msg.contains("protected")) {
                return ParseResult(emptyList(), passwordRequired = true)
            }
            e.printStackTrace()
        }
        return ParseResult(results)
    }

    private fun getCellValueAsString(cell: Cell?, evaluator: FormulaEvaluator? = null): String {
        if (cell == null) return ""
        return when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            CellType.NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    val date = cell.dateCellValue
                    SimpleDateFormat("dd/MM/yyyy").format(date)
                } else {
                    // Handle numeric values smartly - remove trailing .0 if it's an integer
                    val num = cell.numericCellValue
                    if (num == num.toLong().toDouble()) num.toLong().toString() else num.toString()
                }
            }
            CellType.BOOLEAN -> cell.booleanCellValue.toString()
            CellType.FORMULA -> {
                try {
                    val cellValue = evaluator?.evaluate(cell)
                    when (cellValue?.cellType) {
                        CellType.STRING -> cellValue.stringValue
                        CellType.NUMERIC -> {
                            val num = cellValue.numberValue
                            if (num == num.toLong().toDouble()) num.toLong().toString()
                            else num.toString()
                        }
                        CellType.BOOLEAN -> cellValue.booleanValue.toString()
                        else -> cell.cellFormula // fallback to formula string
                    }
                } catch (e: Exception) {
                    cell.cellFormula // fallback to formula string
                }
            }
            else -> ""
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /** Split a line respecting quoted fields (e.g. "Smith, John",... ) */
    private fun splitLine(line: String, delimiter: String): List<String> {
        if (delimiter == "  ") {
            return line.split(Regex(" {2,}")).map { it.trim() }.filter { it.isNotBlank() }
        }
        if (delimiter == " ") {
            return line.split(Regex(" +")).map { it.trim() }.filter { it.isNotBlank() }
        }
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        val dChar = if (delimiter.isNotEmpty()) delimiter[0] else ','
        for (ch in line) {
            when {
                ch == '"' -> inQuotes = !inQuotes
                ch == dChar && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }

    private fun detectDelimiter(headerLine: String, isPdf: Boolean): String {
        val commaCount = headerLine.count { it == ',' }
        val semicolonCount = headerLine.count { it == ';' }
        val tabCount = headerLine.count { it == '\t' }
        val doubleSpaceCount = Regex(" {2,}").findAll(headerLine).count()

        return when {
            tabCount >= 2 -> "\t"
            commaCount >= 2 -> ","
            semicolonCount >= 2 -> ";"
            doubleSpaceCount >= 1 -> "  "
            isPdf -> "  " // Default for PDF if no obvious delimiter
            else -> ","
        }
    }

    private val DATE_FORMATS =
        listOf(
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "MM/dd/yyyy",
            "yyyy-MM-dd",
            "dd MMM yyyy",
            "dd-MMM-yyyy",
            "dd MMM yy",
            "dd-MMM-yy",
            "d/M/yy",
            "d/M/yyyy",
            "yyyyMMdd",
            "MM-dd-yyyy",
            "dd.MM.yyyy",
            "d-MMM-yyyy",
            "d-MMM-yy",
            "dd-MM-yy",
            "dd/MM/yy"
        )

    private fun parseDate(raw: String): Long? {
        val cleaned = raw.trim().removeSurrounding("\"")
        for (fmt in DATE_FORMATS) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.ENGLISH)
                sdf.isLenient = false
                val date = sdf.parse(cleaned)
                if (date != null) {
                    val cal = Calendar.getInstance()
                    cal.time = date
                    val year = cal.get(Calendar.YEAR)
                    // If year is 2 digits (e.g. 26), convert it to 2026
                    if (year < 100) {
                        cal.set(Calendar.YEAR, 2000 + year)
                        return cal.timeInMillis
                    }
                    return date.time
                }
            } catch (_: Exception) {}
        }
        return null
    }

    private fun parseAmount(raw: String): Double? {
        val cleaned = raw.trim().removeSurrounding("\"")
        if (cleaned.isEmpty()) return null
        
        // Handle parenthesis for negative numbers e.g. (100.00)
        var p = cleaned
        if (p.startsWith("(") && p.endsWith(")")) {
            p = "-" + p.substring(1, p.length - 1)
        }
        
        // Remove commas but KEEP the decimal point and minus sign
        // Also remove any spaces to handle "349 . 00" but note that our 
        // PDF logic already splits by spaces to avoid mashing.
        val pCleaned = p.replace(",", "").replace(" ", "").replace(Regex("[^0-9.\\-]"), "")
        
        // If multiple dots exist, Double parsing will fail correctly (prevent mashing)
        return pCleaned.toDoubleOrNull()
    }

    private fun detectPaymentMethod(narration: String): String {
        val upper = narration.uppercase()
        return when {
            upper.contains("UPI") -> "UPI"
            upper.contains("NEFT") -> "NEFT"
            upper.contains("IMPS") -> "IMPS"
            upper.contains("RTGS") -> "RTGS"
            upper.contains("ATM") -> "Cash"
            else -> ""
        }
    }
}
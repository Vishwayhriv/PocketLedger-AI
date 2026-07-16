package com.example.data

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.Inflater
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

// --- MERCHANT INTELLIGENCE ENGINE ---
object MerchantNormalizationEngine {
    private val commonSuffixes = listOf(
        "PVT", "LTD", "INC", "CO", "CORP", "LLC", "SERVICES", "INDIA", "USA", "UK",
        "PAY", "ONLINE", "WWW.", ".COM", ".NET", ".ORG", "PVT LTD", "PRIVATE LIMITED"
    )

    fun normalize(rawName: String, aliases: List<MerchantAliasEntity>): String {
        var clean = rawName.trim()
        if (clean.isEmpty()) return "Unknown Merchant"

        // Check if there is an exact or pattern-based user-defined alias first
        for (alias in aliases) {
            val regex = alias.pattern.toRegex(RegexOption.IGNORE_CASE)
            if (regex.containsMatchIn(clean)) {
                return alias.normalizedName
            }
        }

        // Apply automatic normalization
        clean = clean.uppercase()
        // Strip common suffixes / web additions
        for (suffix in commonSuffixes) {
            val suffixPattern = "\\b$suffix\\b".toRegex()
            clean = clean.replace(suffixPattern, "")
        }
        // Replace non-alphanumeric chars with spaces to clean up
        clean = clean.replace("[^A-Z0-9 ]".toRegex(), " ")
        clean = clean.replace("\\s+".toRegex(), " ").trim()

        // Capitalize words
        return clean.lowercase().split(" ").joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }.ifEmpty { "Unknown Merchant" }
    }
}

// --- SMART CATEGORY ENGINE ---
object SmartCategoryEngine {
    fun categorize(merchant: String, notes: String = ""): String {
        val textToMatch = "$merchant $notes".lowercase()

        return when {
            matchKeywords(textToMatch, listOf("salary", "payroll", "direct deposit", "employer", "wage", "stipend", "income")) -> "Salary"
            matchKeywords(textToMatch, listOf("atm", "cash out", "withdrawal")) -> "ATM"
            matchKeywords(textToMatch, listOf("cash", "hand cash")) -> "Cash"
            matchKeywords(textToMatch, listOf("swiggy", "zomato", "mcdonald", "starbucks", "dunkin", "burger king", "subway", "domino", "pizza", "cafe", "restaurant", "dining", "food", "eat", "grocery", "supermarket")) -> "Food"
            matchKeywords(textToMatch, listOf("uber", "ola", "lyft", "grab", "metro", "cab", "taxi", "train", "bus", "toll", "travel", "irctc", "makemytrip", "flight")) -> "Travel"
            matchKeywords(textToMatch, listOf("amazon", "flipkart", "myntra", "walmart", "target", "ebay", "best buy", "costco", "mall", "apparel", "zara", "nike", "clothing", "fashion", "shopping")) -> "Shopping"
            matchKeywords(textToMatch, listOf("electricity", "electric", "water", "power", "gas bill", "telecom", "at&t", "verizon", "comcast", "internet", "utility", "bills")) -> "Bills"
            matchKeywords(textToMatch, listOf("recharge", "mobile recharge", "dth recharge")) -> "Recharge"
            matchKeywords(textToMatch, listOf("rent", "landlord", "lease", "housing")) -> "Rent"
            matchKeywords(textToMatch, listOf("clinic", "hospital", "pharmacy", "cvs", "walgreens", "medical", "doctor", "dentist", "medicine", "health", "apollo")) -> "Medical"
            matchKeywords(textToMatch, listOf("vanguard", "fidelity", "stocks", "investment", "crypto", "coinbase", "broker", "mutual fund", "etf", "groww", "zerodha")) -> "Investment"
            matchKeywords(textToMatch, listOf("transfer", "venmo", "paypal", "wire", "zelle", "send money", "phonepe", "google pay", "gpay", "bhim", "upi")) -> "Transfer"
            matchKeywords(textToMatch, listOf("emi", "loan", "mortgage", "hdfc loan", "sbi home", "car loan")) -> "EMI"
            matchKeywords(textToMatch, listOf("fuel", "gas", "shell", "chevron", "petrol", "hpcl", "bpcl", "ioc")) -> "Fuel"
            matchKeywords(textToMatch, listOf("netflix", "spotify", "disney", "prime video", "hbo", "youtube premium", "ticket", "cinema", "movie", "game", "steam", "nintendo", "arcade", "entertainment")) -> "Entertainment"
            matchKeywords(textToMatch, listOf("university", "school", "college", "tuition", "coursera", "udemy", "book", "education", "course")) -> "Education"
            else -> "Other"
        }
    }

    private fun matchKeywords(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }
}

// --- STATEMENT IMPORT ENGINE ---
data class ParseResult(
    val parsedTransactions: List<TransactionEntity>,
    val duplicatesFound: Int,
    val errors: List<String>
)

object StatementImportEngine {
    private val dateFormats = listOf(
        "yyyy-MM-dd", "dd-MM-yyyy", "MM/dd/yyyy", "dd/MM/yyyy", "yyyy/MM/dd", "dd MMM yyyy", "dd-MMM-yyyy", "dd/MM/yy"
    )

    // Detect Bank Name from Content or Filename
    fun detectBank(content: String, fileName: String): String {
        val upper = (content + " " + fileName).uppercase()
        return when {
            upper.contains("SBI") || upper.contains("STATE BANK") -> "SBI"
            upper.contains("HDFC") -> "HDFC"
            upper.contains("ICICI") -> "ICICI"
            upper.contains("AXIS") -> "Axis Bank"
            upper.contains("KOTAK") -> "Kotak Mahindra"
            upper.contains("CANARA") -> "Canara Bank"
            upper.contains("UNION BANK") -> "Union Bank"
            upper.contains("BARODA") || upper.contains("BOB") -> "Bank of Baroda"
            upper.contains("PNB") || upper.contains("PUNJAB") -> "Punjab National Bank"
            upper.contains("INDIAN BANK") -> "Indian Bank"
            upper.contains("UPI") -> "UPI Export"
            else -> "General Heuristic"
        }
    }

    // PDF search-text extraction
    fun extractTextFromSearchablePdf(inputStream: InputStream): String {
        val decompressedText = StringBuilder()
        try {
            val bytes = inputStream.readBytes()
            var i = 0
            while (i < bytes.size - 6) {
                // Look for "stream" block
                if (bytes[i] == 's'.toByte() && bytes[i+1] == 't'.toByte() && bytes[i+2] == 'r'.toByte() && 
                    bytes[i+3] == 'e'.toByte() && bytes[i+4] == 'a'.toByte() && bytes[i+5] == 'm'.toByte()) {
                    
                    var streamStart = i + 6
                    while (streamStart < bytes.size && (bytes[streamStart] == '\r'.toByte() || bytes[streamStart] == '\n'.toByte())) {
                        streamStart++
                    }
                    
                    var streamEnd = -1
                    var j = streamStart
                    while (j < bytes.size - 9) {
                        if (bytes[j] == 'e'.toByte() && bytes[j+1] == 'n'.toByte() && bytes[j+2] == 'd'.toByte() && 
                            bytes[j+3] == 's'.toByte() && bytes[j+4] == 't'.toByte() && bytes[j+5] == 'r'.toByte() &&
                            bytes[j+6] == 'e'.toByte() && bytes[j+7] == 'a'.toByte() && bytes[j+8] == 'm'.toByte()) {
                            streamEnd = j
                            break
                        }
                        j++
                    }
                    
                    if (streamEnd != -1) {
                        val streamBytes = bytes.copyOfRange(streamStart, streamEnd)
                        val headerStart = (i - 150).coerceAtLeast(0)
                        val headerString = String(bytes.copyOfRange(headerStart, i))
                        val isFlate = headerString.contains("/FlateDecode")
                        
                        try {
                            val decompressed = if (isFlate) {
                                val decompressor = Inflater()
                                decompressor.setInput(streamBytes)
                                val bos = ByteArrayOutputStream(streamBytes.size)
                                val buf = ByteArray(1024)
                                while (!decompressor.finished()) {
                                    val count = decompressor.inflate(buf)
                                    if (count == 0) break
                                    bos.write(buf, 0, count)
                                }
                                decompressor.end()
                                bos.toByteArray()
                            } else {
                                streamBytes
                            }
                            
                            val text = parsePdfTextOperators(decompressed)
                            decompressedText.append(text).append("\n")
                        } catch (e: Exception) {
                            // Ignored stream error
                        }
                    }
                    i = j + 9
                } else {
                    i++
                }
            }
        } catch (e: Exception) {
            // Ignored file error
        }
        return decompressedText.toString()
    }

    private fun parsePdfTextOperators(bytes: ByteArray): String {
        val sb = StringBuilder()
        var i = 0
        while (i < bytes.size) {
            if (bytes[i] == '('.toByte()) {
                i++
                val textSegment = StringBuilder()
                var escaped = false
                while (i < bytes.size) {
                    val b = bytes[i]
                    if (escaped) {
                        textSegment.append(b.toChar())
                        escaped = false
                    } else if (b == '\\'.toByte()) {
                        escaped = true
                    } else if (b == ')'.toByte()) {
                        break
                    } else {
                        textSegment.append(b.toChar())
                    }
                    i++
                }
                val chunk = textSegment.toString()
                if (chunk.isNotEmpty() && chunk.any { it.isLetter() }) {
                    sb.append(chunk).append(" ")
                }
            } else {
                i++
            }
        }
        return sb.toString()
    }

    // XLSX Parser using lightweight android built-in zip streams and pull parser
    fun extractTextFromXlsx(inputStream: java.io.InputStream): String {
        val sb = StringBuilder()
        try {
            val zipInputStream = ZipInputStream(inputStream)
            var entry = zipInputStream.nextEntry
            val sharedStrings = mutableListOf<String>()
            val sheetsData = mutableListOf<String>()
            while (entry != null) {
                if (entry.name == "xl/sharedStrings.xml") {
                    sharedStrings.addAll(parseSharedStringsXml(zipInputStream))
                } else if (entry.name.startsWith("xl/worksheets/sheet") && entry.name.endsWith(".xml")) {
                    sheetsData.add(parseSheetXml(zipInputStream, sharedStrings))
                }
                entry = zipInputStream.nextEntry
            }
            zipInputStream.close()
            for (sheet in sheetsData) {
                sb.append(sheet)
            }
        } catch (e: Exception) {
            sb.append("Error parsing Excel: ${e.localizedMessage}")
        }
        return sb.toString()
    }

    private fun parseSharedStringsXml(inputStream: InputStream): List<String> {
        val strings = mutableListOf<String>()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType
            var currentString = StringBuilder()
            var insideT = false
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name == "t") {
                            insideT = true
                            currentString = StringBuilder()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (insideT) {
                            currentString.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "t") {
                            insideT = false
                            strings.add(currentString.toString())
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {}
        return strings
    }

    private fun parseSheetXml(inputStream: InputStream, sharedStrings: List<String>): String {
        val sb = StringBuilder()
        try {
            val parser = Xml.newPullParser()
            parser.setInput(inputStream, "UTF-8")
            var eventType = parser.eventType
            var currentCellRef = ""
            var currentCellType = ""
            var currentVal = StringBuilder()
            var insideV = false
            val currentRow = TreeMap<Int, String>()
            
            while (eventType != XmlPullParser.END_DOCUMENT) {
                val name = parser.name
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (name == "row") {
                            currentRow.clear()
                        } else if (name == "c") {
                            currentCellRef = parser.getAttributeValue(null, "r") ?: ""
                            currentCellType = parser.getAttributeValue(null, "t") ?: ""
                        } else if (name == "v") {
                            insideV = true
                            currentVal = StringBuilder()
                        }
                    }
                    XmlPullParser.TEXT -> {
                        if (insideV) {
                            currentVal.append(parser.text)
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "v") {
                            insideV = false
                        } else if (name == "c") {
                            val rawVal = currentVal.toString()
                            val value = if (currentCellType == "s") {
                                val idx = rawVal.toIntOrNull()
                                if (idx != null && idx in sharedStrings.indices) sharedStrings[idx] else rawVal
                            } else {
                                rawVal
                            }
                            val colIndex = excelColToIdx(currentCellRef)
                            currentRow[colIndex] = value
                        } else if (name == "row") {
                            if (currentRow.isNotEmpty()) {
                                val maxCol = currentRow.lastKey()
                                val rowList = List(maxCol + 1) { col -> currentRow[col] ?: "" }
                                sb.append(rowList.joinToString(",")).append("\n")
                            }
                        }
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {}
        return sb.toString()
    }

    private fun excelColToIdx(ref: String): Int {
        val colStr = ref.takeWhile { it.isLetter() }
        var idx = 0
        for (char in colStr) {
            idx = idx * 26 + (char - 'A' + 1)
        }
        return idx - 1
    }

    // Unified Robust Statement Parser
    fun parseStatement(
        rawContent: String,
        existingTransactions: List<TransactionEntity>,
        bankName: String = "General Heuristic"
    ): ParseResult {
        val lines = rawContent.split("\n")
        val parsed = mutableListOf<TransactionEntity>()
        var duplicatesCount = 0
        val errors = mutableListOf<String>()

        // 1. Column Header mapping heuristic
        var headerIndex = -1
        val columnsMap = mutableMapOf<String, Int>()

        val dateKeywords = listOf("date", "tran date", "txn date", "booking date", "value date", "trans date")
        val merchantKeywords = listOf("description", "narration", "remarks", "particulars", "payee", "details", "remark", "merchant")
        val debitKeywords = listOf("debit", "withdrawal", "dr", "withdraw", "withdrawal amt")
        val creditKeywords = listOf("credit", "deposit", "cr", "dep", "deposit amt")
        val amountKeywords = listOf("amount", "value", "tx amount", "txn amt")
        val balanceKeywords = listOf("balance", "bal", "closing bal")
        val refKeywords = listOf("ref", "reference", "cheque", "utr", "txn id", "chq")
        val upiKeywords = listOf("upi", "vpa")

        for ((idx, line) in lines.withIndex()) {
            val tokens = splitLine(line)
            if (tokens.size < 3) continue
            
            var matchCount = 0
            for ((tokIdx, token) in tokens.withIndex()) {
                val t = token.lowercase().trim()
                when {
                    dateKeywords.any { t == it || t.contains(it) } -> { columnsMap["date"] = tokIdx; matchCount++ }
                    merchantKeywords.any { t == it || t.contains(it) } -> { columnsMap["merchant"] = tokIdx; matchCount++ }
                    debitKeywords.any { t == it || t.contains(it) } -> { columnsMap["debit"] = tokIdx; matchCount++ }
                    creditKeywords.any { t == it || t.contains(it) } -> { columnsMap["credit"] = tokIdx; matchCount++ }
                    amountKeywords.any { t == it || t.contains(it) } -> { columnsMap["amount"] = tokIdx; matchCount++ }
                    balanceKeywords.any { t == it || t.contains(it) } -> { columnsMap["balance"] = tokIdx; matchCount++ }
                    refKeywords.any { t == it || t.contains(it) } -> { columnsMap["ref"] = tokIdx; matchCount++ }
                    upiKeywords.any { t == it || t.contains(it) } -> { columnsMap["upi"] = tokIdx; matchCount++ }
                }
            }
            if (matchCount >= 2 && columnsMap.containsKey("date") && columnsMap.containsKey("merchant")) {
                headerIndex = idx
                break
            }
        }

        val startRow = if (headerIndex != -1) headerIndex + 1 else 0
        for (i in startRow until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.startsWith("#") || line.uppercase().contains("TOTAL") || line.uppercase().contains("OPENING") || line.uppercase().contains("CLOSING")) continue

            val tokens = splitLine(line)
            if (tokens.size < 2) continue

            try {
                var date: Long = System.currentTimeMillis()
                var merchant = "Unknown Merchant"
                var amount = 0.0
                var notes = "Imported from $bankName"
                var paymentMethod = "Bank Statement"
                var upiId = ""
                var refNum = ""

                // Extract Date
                val dateIdx = columnsMap["date"] ?: 0
                val dateParsed = if (dateIdx < tokens.size) parseDate(tokens[dateIdx]) else null
                if (dateParsed == null) {
                    errors.add("Line ${i + 1}: Ignored because date is missing or malformed")
                    continue
                }
                date = dateParsed

                // Extract Merchant
                val merchantIdx = columnsMap["merchant"] ?: 1
                if (merchantIdx < tokens.size) {
                    merchant = tokens[merchantIdx].replace("\"", "").trim()
                }

                // Extract Amount
                val amtIdx = columnsMap["amount"]
                val debIdx = columnsMap["debit"]
                val credIdx = columnsMap["credit"]

                var isCredit = false

                if (amtIdx != null && amtIdx < tokens.size) {
                    amount = tokens[amtIdx].replace("[^\\d\\.-]".toRegex(), "").toDoubleOrNull() ?: 0.0
                    if (amount < 0.0) {
                        amount = abs(amount)
                        isCredit = false
                    } else {
                        val typeIdx = columnsMap["type"] ?: tokens.indexOfFirst { it.trim().lowercase() in listOf("cr", "dr") }
                        if (typeIdx != -1 && typeIdx < tokens.size) {
                            isCredit = tokens[typeIdx].trim().lowercase() == "cr"
                        }
                    }
                } else if (debIdx != null && credIdx != null) {
                    val debVal = if (debIdx < tokens.size) tokens[debIdx].replace("[^\\d\\.]".toRegex(), "").toDoubleOrNull() else null
                    val credVal = if (credIdx < tokens.size) tokens[credIdx].replace("[^\\d\\.]".toRegex(), "").toDoubleOrNull() else null
                    
                    if (credVal != null && credVal > 0.0) {
                        amount = credVal
                        isCredit = true
                    } else if (debVal != null && debVal > 0.0) {
                        amount = debVal
                        isCredit = false
                    }
                }

                if (amount <= 0.0 || amount.isNaN() || amount.isInfinite()) {
                    errors.add("Line ${i + 1}: Ignored invalid amount '$amount'")
                    continue
                }
                if (amount > 100000000.0) {
                    errors.add("Line ${i + 1}: Ignored because amount $amount exceeds maximum ₹10,00,00,000 limit")
                    continue
                }

                // Extract Ref / UPI
                val refIdx = columnsMap["ref"]
                if (refIdx != null && refIdx < tokens.size) {
                    refNum = tokens[refIdx].trim()
                }
                val upiIdx = columnsMap["upi"]
                if (upiIdx != null && upiIdx < tokens.size) {
                    upiId = tokens[upiIdx].trim()
                }

                val normalizedMerchant = MerchantNormalizationEngine.normalize(merchant, emptyList())
                val category = SmartCategoryEngine.categorize(normalizedMerchant, notes)

                if (upiId.isNotEmpty()) {
                    notes += " | UPI: $upiId"
                    paymentMethod = "UPI"
                }
                if (refNum.isNotEmpty()) {
                    notes += " | Ref: $refNum"
                }

                val finalCategory = if (isCredit) {
                    if (normalizedMerchant.lowercase().contains("salary") || normalizedMerchant.lowercase().contains("payroll") || amount > 15000) "Salary" else "Transfers"
                } else {
                    category
                }

                val tx = TransactionEntity(
                    merchant = normalizedMerchant,
                    amount = amount,
                    category = finalCategory,
                    date = date,
                    paymentMethod = paymentMethod,
                    notes = notes,
                    isCash = false
                )

                // Duplicate Check
                val isDuplicate = existingTransactions.any { existing ->
                    val cal1 = Calendar.getInstance().apply { timeInMillis = existing.date }
                    val cal2 = Calendar.getInstance().apply { timeInMillis = tx.date }
                    existing.merchant.lowercase() == tx.merchant.lowercase() &&
                            abs(existing.amount - tx.amount) < 0.01 &&
                            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
                }

                if (isDuplicate) {
                    duplicatesCount++
                } else {
                    parsed.add(tx)
                }
            } catch (e: Exception) {
                errors.add("Line ${i + 1}: ${e.localizedMessage}")
            }
        }

        if (parsed.isEmpty() && headerIndex == -1) {
            return parseStatementFallbackGeneric(lines, existingTransactions)
        }

        return ParseResult(parsed, duplicatesCount, errors)
    }

    private fun parseStatementFallbackGeneric(
        lines: List<String>,
        existingTransactions: List<TransactionEntity>
    ): ParseResult {
        val parsed = mutableListOf<TransactionEntity>()
        var duplicatesCount = 0
        val errors = mutableListOf<String>()

        for ((index, line) in lines.withIndex()) {
            val cleanLine = line.trim()
            if (cleanLine.isEmpty() || cleanLine.startsWith("#")) continue

            try {
                var date: Long = System.currentTimeMillis()
                var merchant = ""
                var amount = 0.0
                var category = "Others"
                val paymentMethod = "Bank Statement"
                val notes = "Heuristic Fallback"

                val delimiter = if (cleanLine.contains("\t")) "\t" else if (cleanLine.contains(";")) ";" else ","
                val parts = splitCsvLine(cleanLine, delimiter)
                if (parts.size < 2) continue

                val dateParsed = parseDate(parts[0])
                if (dateParsed == null) {
                    errors.add("Line ${index + 1}: Ignored because date is missing or malformed")
                    continue
                }
                date = dateParsed
                merchant = parts.getOrNull(1)?.replace("\"", "") ?: "Unknown Merchant"
                
                for (j in 2 until parts.size) {
                    val partClean = parts[j].replace("[^\\d\\.-]".toRegex(), "")
                    val amtVal = partClean.toDoubleOrNull()
                    if (amtVal != null && amtVal != 0.0) {
                        amount = abs(amtVal)
                        break
                    }
                }

                if (amount <= 0.0 || amount.isNaN() || amount.isInfinite()) {
                    errors.add("Line ${index + 1}: Ignored invalid amount '$amount'")
                    continue
                }
                if (amount > 100000000.0) {
                    errors.add("Line ${index + 1}: Ignored because amount $amount exceeds maximum ₹10,00,00,000 limit")
                    continue
                }

                val normalizedMerchant = MerchantNormalizationEngine.normalize(merchant, emptyList())
                category = SmartCategoryEngine.categorize(normalizedMerchant, notes)

                val tx = TransactionEntity(
                    merchant = normalizedMerchant,
                    amount = amount,
                    category = category,
                    date = date,
                    paymentMethod = paymentMethod,
                    notes = notes
                )

                val isDuplicate = existingTransactions.any { existing ->
                    val cal1 = Calendar.getInstance().apply { timeInMillis = existing.date }
                    val cal2 = Calendar.getInstance().apply { timeInMillis = tx.date }
                    existing.merchant.lowercase() == tx.merchant.lowercase() &&
                            existing.amount == tx.amount &&
                            cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
                }

                if (isDuplicate) {
                    duplicatesCount++
                } else {
                    parsed.add(tx)
                }
            } catch (e: Exception) {
                errors.add("Line ${index + 1}: ${e.localizedMessage}")
            }
        }
        return ParseResult(parsed, duplicatesCount, errors)
    }

    private fun splitLine(line: String): List<String> {
        val delimiter = if (line.contains("\t")) "\t" else if (line.contains(";")) ";" else ","
        return splitCsvLine(line, delimiter)
    }

    private fun splitCsvLine(line: String, delimiter: String): List<String> {
        val result = mutableListOf<String>()
        var currentToken = StringBuilder()
        var insideQuotes = false
        for (char in line) {
            if (char == '\"') {
                insideQuotes = !insideQuotes
            } else if (char.toString() == delimiter && !insideQuotes) {
                result.add(currentToken.toString().trim())
                currentToken = StringBuilder()
            } else {
                currentToken.append(char)
            }
        }
        result.add(currentToken.toString().trim())
        return result
    }

    private fun parseDate(dateStr: String): Long? {
        val clean = dateStr.trim().replace("\"", "")
        for (format in dateFormats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.US)
                sdf.isLenient = false
                return sdf.parse(clean)?.time
            } catch (e: Exception) {}
        }
        return null
    }
}

// --- GHOST SUBSCRIPTION HUNTER ---
data class SubscriptionCandidate(
    val merchant: String,
    val amount: Double,
    val interval: String, // "Monthly" or "Yearly"
    val confidence: Float, // 0.0 to 1.0
    val lastBilled: Long,
    val transactionIds: List<Int>,
    val explanation: String
)

object GhostSubscriptionHunter {
    fun detectSubscriptions(transactions: List<TransactionEntity>): List<SubscriptionCandidate> {
        val groupedByMerchant = transactions.groupBy { it.merchant.lowercase() }
        val candidates = mutableListOf<SubscriptionCandidate>()

        for ((merchantKey, txs) in groupedByMerchant) {
            if (txs.size < 2) continue
            val sortedTxs = txs.sortedBy { it.date }

            // Group transactions by approximate amount (within 1% threshold)
            val amountGroups = mutableListOf<MutableList<TransactionEntity>>()
            for (tx in sortedTxs) {
                var added = false
                for (group in amountGroups) {
                    val averageAmount = group.map { it.amount }.average()
                    if (abs(tx.amount - averageAmount) / averageAmount <= 0.02) {
                        group.add(tx)
                        added = true
                        break
                    }
                }
                if (!added) {
                    amountGroups.add(mutableListOf(tx))
                }
            }

            for (group in amountGroups) {
                if (group.size < 2) continue

                // Check average interval in days
                val intervals = mutableListOf<Long>()
                for (i in 1 until group.size) {
                    val diff = group[i].date - group[i - 1].date
                    intervals.add(diff)
                }

                val avgIntervalDays = intervals.average() / (1000 * 60 * 60 * 24)

                var intervalString = ""
                var confidence = 0.5f
                var reason = ""

                // Monthly interval (27 to 33 days)
                if (avgIntervalDays in 26.0..35.0) {
                    intervalString = "Monthly"
                    confidence = if (group.size >= 3) 0.9f else 0.7f
                    reason = "Billed consistently every month (${group.size} cycles detected)."
                }
                // Yearly interval (350 to 380 days)
                else if (avgIntervalDays in 340.0..385.0) {
                    intervalString = "Yearly"
                    confidence = if (group.size >= 2) 0.85f else 0.6f
                    reason = "Billed annually."
                } else if (avgIntervalDays in 12.0..16.0) {
                    intervalString = "Bi-Weekly"
                    confidence = 0.65f
                    reason = "Billed every two weeks."
                } else if (avgIntervalDays in 6.0..8.0) {
                    intervalString = "Weekly"
                    confidence = 0.60f
                    reason = "Billed weekly."
                }

                if (intervalString.isNotEmpty()) {
                    val displayMerchant = group.first().merchant
                    candidates.add(
                        SubscriptionCandidate(
                            merchant = displayMerchant,
                            amount = group.last().amount,
                            interval = intervalString,
                            confidence = confidence,
                            lastBilled = group.last().date,
                            transactionIds = group.map { it.id },
                            explanation = reason
                        )
                    )
                }
            }
        }
        return candidates.sortedByDescending { it.confidence }
    }
}

// --- MICRO LEAK DETECTOR ---
data class MicroLeak(
    val merchant: String,
    val category: String,
    val count: Int,
    val averageAmount: Double,
    val monthlyTotal: Double,
    val estimatedAnnualCost: Double,
    val potentialSavings: String
)

object MicroLeakDetector {
    fun detectLeaks(transactions: List<TransactionEntity>): List<MicroLeak> {
        val leakMerchants = listOf("Starbucks", "Dunkin", "Uber", "Swiggy", "Zomato", "Zomato Pay", "Cafe", "Tea", "Coffee", "Cab", "Taxi", "McDonalds", "Burger King", "KFC")
        val grouped = transactions.groupBy { it.merchant }
        val leaks = mutableListOf<MicroLeak>()

        for ((merchant, txs) in grouped) {
            // Find repeat transactions of smaller value (< 15.0 USD or < 500 INR depending on app scale)
            val isLeakMerchant = leakMerchants.any { merchant.lowercase().contains(it.lowercase()) }
            val avgAmount = txs.map { it.amount }.average()

            // If it's small recurring or high frequency small purchases
            if ((isLeakMerchant && txs.size >= 3) || (avgAmount < 25.0 && txs.size >= 4)) {
                val totalAmount = txs.map { it.amount }.sum()

                // Calculate timespan to find actual monthly rate
                val dates = txs.map { it.date }
                val minDate = dates.minOrNull() ?: System.currentTimeMillis()
                val maxDate = dates.maxOrNull() ?: System.currentTimeMillis()
                val diffDays = (maxDate - minDate) / (1000 * 60 * 60 * 24)
                val monthsSpan = if (diffDays < 30) 1.0 else diffDays / 30.0

                val monthlyTotal = totalAmount / monthsSpan
                val annualCost = monthlyTotal * 12.0

                if (monthlyTotal > 0.0) {
                    leaks.add(
                        MicroLeak(
                            merchant = merchant,
                            category = txs.first().category,
                            count = txs.size,
                            averageAmount = avgAmount,
                            monthlyTotal = monthlyTotal,
                            estimatedAnnualCost = annualCost,
                            potentialSavings = "By reducing ${merchant} visits by 50%, you could save \$${String.format("%.2f", annualCost / 2.0)} annually."
                        )
                    )
                }
            }
        }
        return leaks.sortedByDescending { it.monthlyTotal }
    }
}

// --- FINANCIAL HEALTH SCORE ---
data class HealthScore(
    val score: Int,
    val grade: String, // "A+", "A", "B", "C", "F"
    val suggestions: List<String>,
    val achievements: List<String>
)

object FinancialHealthScore {
    fun calculate(
        transactions: List<TransactionEntity>,
        salaryRecords: List<SalaryRecordEntity>
    ): HealthScore {
        if (transactions.isEmpty()) {
            return HealthScore(
                score = 70,
                grade = "B",
                suggestions = listOf("Add transactions to get an accurate financial health score.", "Import your first bank statement."),
                achievements = listOf("Initialized Wallet!")
            )
        }

        val totalIncome = salaryRecords.map { it.amount }.sum()
        // Approximate monthly income
        val monthlyIncome = if (salaryRecords.isEmpty()) {
            // Fallback: Use Transactions marked "Salary"
            val salaryTxs = transactions.filter { it.category == "Salary" }
            if (salaryTxs.isEmpty()) 3000.0 else salaryTxs.map { it.amount }.sum()
        } else {
            totalIncome / salaryRecords.size.coerceAtLeast(1)
        }

        // Calculate typical monthly expenses (excluding Salary category)
        val nonSalaryExpenses = transactions.filter { it.category != "Salary" }
        val totalExpenses = nonSalaryExpenses.map { it.amount }.sum()
        val dates = transactions.map { it.date }
        val minDate = dates.minOrNull() ?: System.currentTimeMillis()
        val maxDate = dates.maxOrNull() ?: System.currentTimeMillis()
        val diffDays = (maxDate - minDate) / (1000 * 60 * 60 * 24)
        val monthsSpan = if (diffDays < 30) 1.0 else diffDays / 30.0
        val monthlyExpense = totalExpenses / monthsSpan

        // Calculate Metrics
        val savingsRate = if (monthlyIncome > 0.0) {
            ((monthlyIncome - monthlyExpense) / monthlyIncome) * 100.0
        } else {
            0.0
        }

        // Subscriptions score impact
        val subscriptions = GhostSubscriptionHunter.detectSubscriptions(transactions)
        val totalSubscriptionCost = subscriptions.sumOf { it.amount }
        val subscriptionLoad = if (monthlyIncome > 0.0) (totalSubscriptionCost / monthlyIncome) * 100.0 else 0.0

        // Cash Ratio
        val cashSpent = transactions.filter { it.paymentMethod == "Cash" || it.isCash }.sumOf { it.amount }
        val cashRatio = if (totalExpenses > 0) (cashSpent / totalExpenses) * 100.0 else 0.0

        // Score formulation
        var baseScore = 75

        // Savings Rate effect (up to +15 or -25)
        baseScore += when {
            savingsRate >= 30.0 -> 15
            savingsRate >= 20.0 -> 10
            savingsRate >= 10.0 -> 5
            savingsRate < 0.0 -> -20
            else -> 0
        }

        // Subscription load effect (up to -15)
        baseScore -= when {
            subscriptionLoad > 15.0 -> 15
            subscriptionLoad > 10.0 -> 10
            subscriptionLoad > 5.0 -> 5
            else -> 0
        }

        // Cash ratio check (low cash = better digital traceability = positive score)
        baseScore += when {
            cashRatio < 10.0 -> 5
            cashRatio > 40.0 -> -10
            else -> 0
        }

        // Clamp score between 0 and 100
        val finalScore = baseScore.coerceIn(10, 100)

        val grade = when {
            finalScore >= 90 -> "A+"
            finalScore >= 80 -> "A"
            finalScore >= 70 -> "B"
            finalScore >= 60 -> "C"
            else -> "F"
        }

        val suggestions = mutableListOf<String>()
        val achievements = mutableListOf<String>()

        if (savingsRate >= 20.0) {
            achievements.add("Super Saver: Savings rate is above 20%.")
        } else if (savingsRate > 0.0) {
            suggestions.add("Aim to save at least 20% of your income. Currently saving ${String.format("%.1f", savingsRate)}%.")
        } else {
            suggestions.add("Alert: You are spending more than you earn! Reduce luxury purchases.")
        }

        if (subscriptionLoad > 10.0) {
            suggestions.add("Subscriptions consume ${String.format("%.1f", subscriptionLoad)}% of income. Audit recurring bills.")
        } else {
            achievements.add("Subscription Master: Low recurring payment burden.")
        }

        if (cashRatio > 30.0) {
            suggestions.add("High cash usage (${String.format("%.1f", cashRatio)}%). Try using digital transactions for better tracking.")
        } else {
            achievements.add("Paperless Tracker: Excellent digital footprint.")
        }

        if (finalScore >= 85) {
            achievements.add("Golden Health: Masterful control of budget.")
        }

        return HealthScore(finalScore, grade, suggestions, achievements)
    }
}

// --- PLAY STORE COMPLIANT NOTIFICATION TRANSACTION DETECTOR ENGINE ---
object NotificationTransactionParser {
    data class ParsedNotificationTx(
        val amount: Double,
        val merchant: String,
        val isIncome: Boolean,
        val paymentMethod: String,
        val currency: String = "₹",
        val bankName: String? = null,
        val upiRef: String? = null,
        val balance: Double? = null,
        val timestamp: Long? = null
    )

    fun isSupportedApp(packageName: String): Boolean {
        val pkg = packageName.lowercase()
        return pkg.contains("com.google.android.apps.nbu.paisa.user") || // Google Pay
               pkg.contains("phonepe") || // PhonePe
               pkg.contains("paytm") || // Paytm
               pkg.contains("com.dreamplug.androidapp") || pkg.contains("cred") || // CRED
               pkg.contains("npci.upiapp") || // BHIM UPI
               pkg.contains("amazon") || // Amazon Pay
               pkg.contains("whatsapp") || // WhatsApp
               pkg.contains("mobikwik") || // Mobikwik
               pkg.contains("freecharge") || // Freecharge
               pkg.contains("axis.mobile") || pkg.contains("axismobile") || pkg.contains("axisbank") || // Axis Bank
               pkg.contains("hdfc") || // HDFC Bank
               pkg.contains("icici") || // ICICI Bank
               pkg.contains("sbi") || pkg.contains("statebank") || // SBI
               pkg.contains("kotak") || // Kotak Bank
               pkg.contains("canarabank") || pkg.contains("canara") || // Canara Bank
               pkg.contains("unionbank") || // Union Bank
               pkg.contains("indianbank") || // Indian Bank
               pkg.contains("idfcfirstbank") || pkg.contains("idfc") || // IDFC First Bank
               pkg.contains("bankofbaroda") || pkg.contains("bob") || // Bank of Baroda
               pkg.contains("pnb") || pkg.contains("punjabnationalbank") || // Punjab National Bank
               pkg.contains("federalbank") || // Federal Bank
               pkg.contains("aubank") || pkg.contains("aupower") || // AU Small Finance Bank
               pkg.contains("yesbank") || // Yes Bank
               pkg.contains("airtel") || pkg.contains("airtelmoney") // Airtel Payments Bank
    }

    fun parseDateTime(text: String): Long? {
        try {
            val lowerText = text.lowercase(Locale.US)
            val calendar = Calendar.getInstance()
            var year: Int? = null
            var month: Int? = null
            var day: Int? = null
            var hour: Int? = null
            var minute: Int? = null
            var isPm = false
            var hasPm = false
            var hasTime = false
            var hasDate = false

            // 1. Time parsing (e.g. 10:45 PM or 10:45pm)
            val timePmRegex = "\\b(\\d{1,2}):(\\d{2})\\s*(am|pm)\\b".toRegex()
            val pmMatch = timePmRegex.find(lowerText)
            if (pmMatch != null) {
                hour = pmMatch.groupValues[1].toIntOrNull()
                minute = pmMatch.groupValues[2].toIntOrNull()
                val ampm = pmMatch.groupValues[3]
                isPm = ampm == "pm"
                hasPm = true
                hasTime = true
            } else {
                val timeRegex = "\\b(\\d{2}):(\\d{2})\\b".toRegex()
                val simpleMatch = timeRegex.find(lowerText)
                if (simpleMatch != null) {
                    hour = simpleMatch.groupValues[1].toIntOrNull()
                    minute = simpleMatch.groupValues[2].toIntOrNull()
                    hasTime = true
                }
            }

            val months = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")

            // 2. Date parsing
            // Pattern A: 16 Jul 2026 or 16-Jul-2026
            val dateTextRegex = "\\b(\\d{1,2})[- ]+(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[- ]+(\\d{4})\\b".toRegex()
            val matchA = dateTextRegex.find(lowerText)
            if (matchA != null) {
                day = matchA.groupValues[1].toIntOrNull()
                val monthStr = matchA.groupValues[2]
                month = months.indexOf(monthStr)
                year = matchA.groupValues[3].toIntOrNull()
                hasDate = true
            } else {
                // Pattern B: 16/07/2026 or 16-07-2026
                val dateNumericRegex = "\\b(\\d{1,2})[-/](\\d{1,2})[-/](\\d{4})\\b".toRegex()
                val matchB = dateNumericRegex.find(lowerText)
                if (matchB != null) {
                    day = matchB.groupValues[1].toIntOrNull()
                    val monthNum = matchB.groupValues[2].toIntOrNull()
                    if (monthNum != null) {
                        month = monthNum - 1
                    }
                    year = matchB.groupValues[3].toIntOrNull()
                    hasDate = true
                } else {
                    // Pattern C: 16 Jul or 16-Jul
                    val dateNoYearRegex = "\\b(\\d{1,2})[- ]+(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\\b".toRegex()
                    val matchC = dateNoYearRegex.find(lowerText)
                    if (matchC != null) {
                        day = matchC.groupValues[1].toIntOrNull()
                        val monthStr = matchC.groupValues[2]
                        month = months.indexOf(monthStr)
                        year = calendar.get(Calendar.YEAR)
                        hasDate = true
                    }
                }
            }

            if (!hasTime && !hasDate) {
                return null
            }

            if (hasDate && day != null && month != null && year != null) {
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
            }
            if (hasTime && hour != null && minute != null) {
                if (hasPm) {
                    if (isPm) {
                        calendar.set(Calendar.HOUR_OF_DAY, if (hour == 12) 12 else hour + 12)
                    } else {
                        calendar.set(Calendar.HOUR_OF_DAY, if (hour == 12) 0 else hour)
                    }
                } else {
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                }
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            } else {
                calendar.set(Calendar.HOUR_OF_DAY, 12)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }

            return calendar.timeInMillis
        } catch (e: Exception) {
            return null
        }
    }

    fun parse(title: String?, text: String?, packageName: String): ParsedNotificationTx? {
        // Must be a supported financial app
        if (!isSupportedApp(packageName)) {
            return null
        }

        val fullText = "${title ?: ""} ${text ?: ""}"
        val lowerText = fullText.lowercase()

        // ONLY DETECT FINANCIAL TRANSACTIONS
        val paymentKeywords = listOf(
            "credited", "debited", "paid", "received", "upi", "bank transfer", "sent", 
            "payment successful", "payment received", "received from", "paid to", "spent", 
            "withdrawn", "deposit", "credited to account", "debited from account", "transaction successful"
        )

        val ignoreKeywords = listOf(
            "otp", "verification code", "one-time password", "code:", "offers", "cashback", "advertisement", 
            "recharge reminder", "festival", "coupon", "marketing", "social", "chat", "email", "missed call", 
            "download", "promo", "discount"
        )

        // Must contain at least one payment keyword
        val hasPaymentKeyword = paymentKeywords.any { lowerText.contains(it) }
        if (!hasPaymentKeyword) {
            return null
        }

        // Must NOT contain any ignore keywords
        val hasIgnoreKeyword = ignoreKeywords.any { lowerText.contains(it) }
        if (hasIgnoreKeyword) {
            return null
        }

        // Search for amount
        val cleanText = fullText.replace(",", "") // remove commas for easier regex match
        val amountRegex = "(?:Rs\\.?|INR|₹|RS|USD|\\$|EUR|€|GBP|£)\\s*([\\d]+(?:\\.\\d{1,2})?)".toRegex(RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(cleanText) ?: return null
        val amount = amountMatch.groupValues[1].toDoubleOrNull() ?: return null

        if (amount <= 0.0) return null

        // Detect currency symbol
        val currencySymbol = when {
            cleanText.contains("₹") || cleanText.contains("INR", ignoreCase = true) || cleanText.contains("Rs", ignoreCase = true) -> "₹"
            cleanText.contains("$") || cleanText.contains("USD", ignoreCase = true) -> "$"
            cleanText.contains("€") || cleanText.contains("EUR", ignoreCase = true) -> "€"
            cleanText.contains("£") || cleanText.contains("GBP", ignoreCase = true) -> "£"
            else -> "₹"
        }

        // Detect if income or expense
        val isIncome = cleanText.contains("received", ignoreCase = true) || 
                       cleanText.contains("credited", ignoreCase = true) ||
                       cleanText.contains("deposit", ignoreCase = true) ||
                       cleanText.contains("refund", ignoreCase = true)

        // Try to find the merchant
        var merchant = ""
        
        // Known names / Specific rules
        val knownMerchants = listOf(
            "Swiggy", "Zomato", "Amazon", "Flipkart", "Blinkit", "Uber", "Ola", 
            "Netflix", "Spotify", "Jio", "Airtel", "Myntra", "BigBasket", "DMart", 
            "IRCTC", "BookMyShow", "Google Play", "Steam"
        )
        for (m in knownMerchants) {
            if (lowerText.contains(m.lowercase())) {
                merchant = m
                break
            }
        }

        if (merchant.isEmpty()) {
            // Patterns like "Paid ₹50 to Starbucks" -> "Starbucks"
            val toPattern = "(?:paid|sent|transfer to|spent|to)\\s+(?:Rs\\.?|INR|₹|RS|\\$|€|£)?\\s*[\\d.]+\\s+to\\s+([^\\n\\.]+)".toRegex(RegexOption.IGNORE_CASE)
            val atPattern = "(?:paid|spent|at)\\s+(?:Rs\\.?|INR|₹|RS|\\$|€|£)?\\s*[\\d.]+\\s+at\\s+([^\\n\\.]+)".toRegex(RegexOption.IGNORE_CASE)
            val fromPattern = "(?:received|credited|from)\\s+(?:Rs\\.?|INR|₹|RS|\\$|€|£)?\\s*[\\d.]+\\s+from\\s+([^\\n\\.]+)".toRegex(RegexOption.IGNORE_CASE)
            
            val toMatch = toPattern.find(cleanText)
            val atMatch = atPattern.find(cleanText)
            val fromMatch = fromPattern.find(cleanText)
    
            if (toMatch != null) {
                merchant = toMatch.groupValues[1].trim()
            } else if (atMatch != null) {
                merchant = atMatch.groupValues[1].trim()
            } else if (fromMatch != null) {
                merchant = fromMatch.groupValues[1].trim()
            }
    
            if (merchant.isEmpty()) {
                // General backup extraction
                val backupTo = "to\\s+([^\\n\\.,]+)".toRegex(RegexOption.IGNORE_CASE).find(cleanText)
                val backupAt = "at\\s+([^\\n\\.,]+)".toRegex(RegexOption.IGNORE_CASE).find(cleanText)
                val backupFrom = "from\\s+([^\\n\\.,]+)".toRegex(RegexOption.IGNORE_CASE).find(cleanText)
                
                if (backupTo != null) {
                    merchant = backupTo.groupValues[1].trim()
                } else if (backupAt != null) {
                    merchant = backupAt.groupValues[1].trim()
                } else if (backupFrom != null) {
                    merchant = backupFrom.groupValues[1].trim()
                }
            }
        }

        // Clean up merchant name
        if (merchant.isNotEmpty()) {
            val cleanRegex = "\\b(?:using|via|on|ref|a/c|balance|bank|card|wallet|from|to|at)\\b.*".toRegex(RegexOption.IGNORE_CASE)
            merchant = merchant.replace(cleanRegex, "").trim()
            merchant = merchant.replace("[^a-zA-Z0-9\\s]".toRegex(), "").trim()
            
            val words = merchant.split("\\s+".toRegex())
            if (words.size > 4) {
                merchant = words.take(4).joinToString(" ")
            }
        }

        if (merchant.isEmpty()) {
            merchant = if (isIncome) "Income Source" else "Merchant Partner"
        }

        // Detect payment method (UPI, Card, Bank Transfer, etc.)
        val paymentMethod = when {
            cleanText.contains("upi", ignoreCase = true) -> "UPI"
            cleanText.contains("card", ignoreCase = true) || cleanText.contains("visa", ignoreCase = true) || cleanText.contains("mastercard", ignoreCase = true) -> "Card"
            cleanText.contains("bank", ignoreCase = true) || cleanText.contains("a/c", ignoreCase = true) || cleanText.contains("account", ignoreCase = true) -> "Bank Transfer"
            else -> "UPI"
        }

        // Try to extract Bank Name
        val bankName = when {
            cleanText.contains("axis", ignoreCase = true) -> "Axis Bank"
            cleanText.contains("hdfc", ignoreCase = true) -> "HDFC Bank"
            cleanText.contains("icici", ignoreCase = true) -> "ICICI Bank"
            cleanText.contains("sbi", ignoreCase = true) || cleanText.contains("state bank", ignoreCase = true) -> "SBI"
            cleanText.contains("kotak", ignoreCase = true) -> "Kotak Bank"
            cleanText.contains("canara", ignoreCase = true) -> "Canara Bank"
            cleanText.contains("union bank", ignoreCase = true) -> "Union Bank"
            cleanText.contains("idfc", ignoreCase = true) -> "IDFC First Bank"
            cleanText.contains("baroda", ignoreCase = true) || cleanText.contains("bob", ignoreCase = true) -> "Bank of Baroda"
            cleanText.contains("pnb", ignoreCase = true) || cleanText.contains("punjab national", ignoreCase = true) -> "PNB"
            cleanText.contains("federal", ignoreCase = true) -> "Federal Bank"
            cleanText.contains("aubank", ignoreCase = true) || cleanText.contains("au small", ignoreCase = true) -> "AU Bank"
            cleanText.contains("yes bank", ignoreCase = true) -> "Yes Bank"
            cleanText.contains("airtel", ignoreCase = true) -> "Airtel Payments Bank"
            else -> null
        }

        // Try to extract UPI Reference Number
        val upiRegex = "\\b(?:ref|upi ref|txn|id)\\s*[:\\- ]?\\s*([\\d]{12})\\b".toRegex(RegexOption.IGNORE_CASE)
        val upiMatch = upiRegex.find(cleanText)
        val upiRef = upiMatch?.groupValues?.get(1)

        // Try to extract available balance
        var balance: Double? = null
        val balanceRegex = "(?:available\\s+balance|avl\\s+bal|balance|current\\s+balance)\\s*(?::|Rs\\.?|INR|₹|RS|\\$|€|£)?\\s*([\\d]+(?:\\.\\d{1,2})?)".toRegex(RegexOption.IGNORE_CASE)
        val balanceMatch = balanceRegex.find(cleanText)
        if (balanceMatch != null) {
            balance = balanceMatch.groupValues[1].toDoubleOrNull()
        }

        val parsedTimestamp = parseDateTime(fullText)

        return ParsedNotificationTx(
            amount = amount,
            merchant = merchant,
            isIncome = isIncome,
            paymentMethod = paymentMethod,
            currency = currencySymbol,
            bankName = bankName,
            upiRef = upiRef,
            balance = balance,
            timestamp = parsedTimestamp
        )
    }
}

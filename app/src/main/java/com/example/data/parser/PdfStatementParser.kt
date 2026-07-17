@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "REDUNDANT_INITIALIZER", "UNCHECKED_CAST")
package com.example.data.parser

import java.io.InputStream
import java.util.Date
import java.util.regex.Pattern

/**
 * Parses searchable bank statements in PDF format using Apache PDFBox.
 * Uses reflection to decouple compile-time dependency on Apache PDFBox.
 */
class PdfStatementParser(
    private val bankDetector: BankDetector,
    private val merchantNormalizer: MerchantNormalizer,
    private val categoryEngine: CategoryEngine,
    private val duplicateDetector: DuplicateDetector
) {

    private class PatternInfo(
        val pattern: Pattern,
        val dateGroup: Int,
        val descGroup: Int,
        val isDualColumn: Boolean,
        val amountGroup: Int = -1,
        val debitGroup: Int = -1,
        val creditGroup: Int = -1,
        val utrGroup: Int = -1,
        val txnIdGroup: Int = -1
    )

    private fun isValidUniqueId(id: String?): Boolean {
        if (id == null) return false
        val trimmed = id.trim().lowercase()
        if (trimmed.isEmpty()) return false
        val nonUniquePlaceholders = setOf("null", "n/a", "na", "n.a.", "-", "0", "0.0", "0.00", "nil", "none", "unknown", "undefined", "payment", "upi")
        return !nonUniquePlaceholders.contains(trimmed)
    }

    private fun getPatternsForBank(bankName: String): List<PatternInfo> {
        val list = mutableListOf<PatternInfo>()

        when (bankName) {
            ParserConstants.BANK_PHONEPE -> {
                list.add(PatternInfo(
                    pattern = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+(.*?)\\s+([0-9,.]+)\\s+([a-zA-Z0-9]+)(?:\\s+(?:SUCCESS|FAILED|PENDING))?"),
                    dateGroup = 1,
                    descGroup = 2,
                    isDualColumn = false,
                    amountGroup = 3,
                    utrGroup = 4
                ))
                list.add(PatternInfo(
                    pattern = Pattern.compile("^(\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}(?:\\s+\\d{2}:\\d{2}:\\d{2})?)\\s+(.*?)\\s+([0-9,.]+)\\s+([a-zA-Z0-9]+)(?:\\s+(?:SUCCESS|FAILED|PENDING))?"),
                    dateGroup = 1,
                    descGroup = 2,
                    isDualColumn = false,
                    amountGroup = 3,
                    utrGroup = 4
                ))
            }
            ParserConstants.BANK_GPAY -> {
                list.add(PatternInfo(
                    pattern = Pattern.compile("^([a-zA-Z]{3,}\\s+\\d{1,2},\\s+\\d{4})\\s+(To|From|Paid to|Received from)\\s+(.*?)\\s+[₹$]?([0-9,.]+)"),
                    dateGroup = 1,
                    descGroup = 3,
                    isDualColumn = false,
                    amountGroup = 4
                ))
                list.add(PatternInfo(
                    pattern = Pattern.compile("^(\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4})\\s+(To|From|Paid to|Received from)\\s+(.*?)\\s+[₹$]?([0-9,.]+)"),
                    dateGroup = 1,
                    descGroup = 3,
                    isDualColumn = false,
                    amountGroup = 4
                ))
                list.add(PatternInfo(
                    pattern = Pattern.compile("^([a-zA-Z]{3,}\\s+\\d{1,2},\\s+\\d{4})\\s+(.*?)\\s+[₹$]?([0-9,.]+)"),
                    dateGroup = 1,
                    descGroup = 2,
                    isDualColumn = false,
                    amountGroup = 3
                ))
            }
            ParserConstants.BANK_PAYTM -> {
                list.add(PatternInfo(
                    pattern = Pattern.compile("^(\\d{2}/\\d{2}/\\d{4}\\s+\\d{2}:\\d{2})\\s+(.*?)\\s+(?:TxnID:?\\s*\\d+)?\\s*([-+]?[₹$]?[0-9,.]+)"),
                    dateGroup = 1,
                    descGroup = 2,
                    isDualColumn = false,
                    amountGroup = 3
                ))
                list.add(PatternInfo(
                    pattern = Pattern.compile("^(\\d{2}/\\d{2}/\\d{4})\\s+(.*?)\\s+([-+]?[₹$]?[0-9,.]+)"),
                    dateGroup = 1,
                    descGroup = 2,
                    isDualColumn = false,
                    amountGroup = 3
                ))
            }
            ParserConstants.BANK_SBI -> {
                list.add(PatternInfo(
                    pattern = Pattern.compile("^(\\d{2}-[a-zA-Z]{3}-\\d{4})\\s+(.*?)\\s+([0-9,.]+)\\s+([0-9,.]+)"),
                    dateGroup = 1,
                    descGroup = 2,
                    isDualColumn = true,
                    debitGroup = 3,
                    creditGroup = 4
                ))
                list.add(PatternInfo(
                    pattern = Pattern.compile("^(\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4})\\s+(.*?)\\s+([0-9,.]+)\\s+([0-9,.]+)"),
                    dateGroup = 1,
                    descGroup = 2,
                    isDualColumn = true,
                    debitGroup = 3,
                    creditGroup = 4
                ))
            }
            ParserConstants.BANK_HDFC -> {
                list.add(PatternInfo(
                    pattern = Pattern.compile("^(\\d{2}/\\d{2}/\\d{2,4})\\s+(.*?)\\s+([0-9,.]+)\\s+([0-9,.]+)"),
                    dateGroup = 1,
                    descGroup = 2,
                    isDualColumn = true,
                    debitGroup = 3,
                    creditGroup = 4
                ))
                list.add(PatternInfo(
                    pattern = Pattern.compile("^(\\d{2}-\\d{2}-\\d{2,4})\\s+(.*?)\\s+([0-9,.]+)\\s+([0-9,.]+)"),
                    dateGroup = 1,
                    descGroup = 2,
                    isDualColumn = true,
                    debitGroup = 3,
                    creditGroup = 4
                ))
            }
            ParserConstants.BANK_ICICI -> {
                list.add(PatternInfo(
                    pattern = Pattern.compile("^(\\d{2}/\\d{2}/\\d{2,4})\\s+(.*?)\\s+([0-9,.]+)\\s+([0-9,.]+)"),
                    dateGroup = 1,
                    descGroup = 2,
                    isDualColumn = true,
                    debitGroup = 3,
                    creditGroup = 4
                ))
                list.add(PatternInfo(
                    pattern = Pattern.compile("^(\\d{2}/\\d{2}/\\d{2,4})\\s+(.*?)\\s+([0-9,.]+)\\s*(Dr|Cr|Debit|Credit)?"),
                    dateGroup = 1,
                    descGroup = 2,
                    isDualColumn = false,
                    amountGroup = 3
                ))
            }
            ParserConstants.BANK_AXIS -> {
                list.add(PatternInfo(
                    pattern = Pattern.compile("^(\\d{2}-\\d{2}-\\d{2,4})\\s+(.*?)\\s+([0-9,.]+)\\s+([0-9,.]+)"),
                    dateGroup = 1,
                    descGroup = 2,
                    isDualColumn = true,
                    debitGroup = 3,
                    creditGroup = 4
                ))
                list.add(PatternInfo(
                    pattern = Pattern.compile("^(\\d{2}-\\d{2}-\\d{2,4})\\s+(.*?)\\s+([0-9,.]+)"),
                    dateGroup = 1,
                    descGroup = 2,
                    isDualColumn = false,
                    amountGroup = 3
                ))
            }
        }
        return list
    }

    private fun getGenericPatterns(): List<PatternInfo> {
        val list = mutableListOf<PatternInfo>()
        list.add(PatternInfo(
            pattern = Pattern.compile("^(\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4})\\s+(.*?)\\s+([0-9,.]+)"),
            dateGroup = 1,
            descGroup = 2,
            isDualColumn = false,
            amountGroup = 3
        ))
        list.add(PatternInfo(
            pattern = Pattern.compile("^(\\d{1,2}\\s+[a-zA-Z]{3,}\\s+\\d{2,4}|[a-zA-Z]{3,}\\s+\\d{1,2},\\s+\\d{2,4})\\s+(.*?)\\s+([0-9,.]+)"),
            dateGroup = 1,
            descGroup = 2,
            isDualColumn = false,
            amountGroup = 3
        ))
        list.add(PatternInfo(
            pattern = Pattern.compile("^(\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4})\\s+(.*?)\\s+([0-9,.]+)\\s+([0-9,.]+)"),
            dateGroup = 1,
            descGroup = 2,
            isDualColumn = true,
            debitGroup = 3,
            creditGroup = 4
        ))
        list.add(PatternInfo(
            pattern = Pattern.compile("^(\\d{1,2}\\s+[a-zA-Z]{3,}\\s+\\d{2,4}|[a-zA-Z]{3,}\\s+\\d{1,2},\\s+\\d{2,4})\\s+(.*?)\\s+([0-9,.]+)\\s+([0-9,.]+)"),
            dateGroup = 1,
            descGroup = 2,
            isDualColumn = true,
            debitGroup = 3,
            creditGroup = 4
        ))
        list.add(PatternInfo(
            pattern = Pattern.compile("^(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2})\\s+(.*?)\\s+[-+]?[₹$]?([0-9,.]+)"),
            dateGroup = 1,
            descGroup = 2,
            isDualColumn = false,
            amountGroup = 3
        ))
        list.add(PatternInfo(
            pattern = Pattern.compile("^(\\d{1,2}[-/.]\\d{1,2}[-/.]\\d{2,4}|\\d{4}-\\d{2}-\\d{2})\\s+(.*?)\\s+[-+]?[₹$]?([0-9,.]+)"),
            dateGroup = 1,
            descGroup = 2,
            isDualColumn = false,
            amountGroup = 3
        ))
        return list
    }

    /**
     * Parse a PDF statement.
     */
    fun parse(
        inputStream: InputStream,
        existingTxns: List<Any> = emptyList()
    ): Any { // Returns ParseResult
        if (!PdfReflection.isAvailable()) {
            return createParseResult(emptyList(), 0, 0, "PDF parsing is not supported (Apache PDFBox library missing from classpath)")
        }

        try {
            println("[PdfStatementParser] Verifying PDF text extraction...")
            val fullText = PdfReflection.extractText(inputStream)
            val textLength = fullText?.length ?: 0
            println("[PdfStatementParser] Extracted text length: $textLength")

            if (fullText.isNullOrBlank()) {
                val reason = PdfReflection.lastExtractionError ?: "The PDF is empty or text could not be extracted."
                println("[PdfStatementParser] Extraction failed: $reason")
                return createParseResult(emptyList(), 0, 0, reason)
            }

            println("[PdfStatementParser] PDFBox extracted text successfully. First 500 chars:")
            println(fullText.take(Math.min(500, fullText.length)))

            // 1. Detect Bank
            val detectedBank = bankDetector.detectFromText(fullText)
            println("[PdfStatementParser] Detected bank: $detectedBank")

            // 2. Parse based on bank template
            return parseTextByBankTemplate(fullText, detectedBank, existingTxns)

        } catch (e: Exception) {
            println("[PdfStatementParser] PDF parsing error: ${e.localizedMessage}")
            return createParseResult(emptyList(), 0, 0, "PDF parsing error: ${e.localizedMessage}")
        }
    }

    private fun parseTextByBankTemplate(
        fullText: String,
        bankName: String,
        existingTxns: List<Any>
    ): Any {
        val transactions = mutableListOf<Any>()
        var unparsedCount = 0
        var duplicateCount = 0
        var skippedCount = 0

        val lines = fullText.split("\\r?\\n".toRegex())
        val totalLines = lines.size
        println("[PdfStatementParser] Parsing text using template: $bankName. Total lines: $totalLines")
        println("[PdfStatementParser] Regex selected: multi-layout patterns for bank $bankName with fallback to Generic patterns")

        var matchedLinesCount = 0

        println("[PdfStatementParser] LINE BY LINE ANALYSIS START")

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.isBlank()) {
                skippedCount++
                continue
            }

            println("LINE:")
            println(trimmedLine)

            // Standard page headers/footers to skip
            if (trimmedLine.contains("Page", ignoreCase = true) ||
                trimmedLine.contains("Balance", ignoreCase = true) && trimmedLine.contains("Date", ignoreCase = true) ||
                trimmedLine.contains("Generated:", ignoreCase = true) ||
                trimmedLine.contains("Statement for Account Holders", ignoreCase = true)) {
                println("MATCH:")
                println("false")
                println("Skipped line (recognized as header/footer or ignorable metadata)")
                skippedCount++
                continue
            }

            // Define regex configurations per bank
            val patternsToTry = mutableListOf<PatternInfo>()
            patternsToTry.addAll(getPatternsForBank(bankName))
            patternsToTry.addAll(getGenericPatterns())

            var matched = false
            var matchReason = "Pattern mismatch"
            var parsedTxn: Any? = null
            var parsedDate: Date? = null
            var normalizedMerchant = ""
            var amount = 0.0
            var utr: String? = null
            var transactionId: String? = null
            var type = ParserConstants.TXN_TYPE_DEBIT

            for (patternInfo in patternsToTry) {
                val matcher = patternInfo.pattern.matcher(trimmedLine)
                if (matcher.find()) {
                    try {
                        val dateStr = matcher.group(patternInfo.dateGroup)
                        val descStr = matcher.group(patternInfo.descGroup).trim()

                        parsedDate = ParserUtils.parseDate(dateStr)
                        if (parsedDate == null) {
                            matchReason = "Failed to parse date string: '$dateStr'"
                            continue
                        }

                        // Extract amount based on single vs dual columns
                        if (patternInfo.isDualColumn) {
                            val debitStr = matcher.group(patternInfo.debitGroup)
                            val creditStr = matcher.group(patternInfo.creditGroup)

                            val debitVal = if (!debitStr.isNullOrBlank()) debitStr.replace("[,\\s]".toRegex(), "").toDoubleOrNull() ?: 0.0 else 0.0
                            val creditVal = if (!creditStr.isNullOrBlank()) creditStr.replace("[,\\s]".toRegex(), "").toDoubleOrNull() ?: 0.0 else 0.0

                            if (debitVal > 0.0) {
                                amount = ParserUtils.parseAmount(debitStr)
                                type = ParserConstants.TXN_TYPE_DEBIT
                            } else if (creditVal > 0.0) {
                                amount = ParserUtils.parseAmount(creditStr)
                                type = ParserConstants.TXN_TYPE_CREDIT
                            } else {
                                matchReason = "Dual column matched, but both debit and credit values were zero/empty."
                                continue // Middle balances row
                            }
                        } else {
                            val amtStr = matcher.group(patternInfo.amountGroup)
                            val rawAmt = ParserUtils.parseAmount(amtStr)
                            amount = Math.abs(rawAmt)
                            // Heuristic: check if raw text indicates debit/credit, or fallback
                            val lowerLine = trimmedLine.lowercase()
                            type = when {
                                lowerLine.contains("received") || lowerLine.contains("credit") || lowerLine.contains("cr") || rawAmt > 0 -> ParserConstants.TXN_TYPE_CREDIT
                                else -> ParserConstants.TXN_TYPE_DEBIT
                            }
                        }

                        normalizedMerchant = merchantNormalizer.normalize(descStr)
                        val category = categoryEngine.predictCategory(normalizedMerchant, descStr)

                        // Optional fields like UTR
                        utr = if (patternInfo.utrGroup != -1) matcher.group(patternInfo.utrGroup) else null
                        transactionId = if (patternInfo.txnIdGroup != -1) matcher.group(patternInfo.txnIdGroup) else utr // Fallback to UTR as transactionId

                        val validUtr = if (isValidUniqueId(utr)) utr else null
                        val validTxnId = if (isValidUniqueId(transactionId)) transactionId else null

                        parsedTxn = createTransactionEntity(
                            date = parsedDate,
                            rawNarrative = descStr,
                            merchantName = normalizedMerchant,
                            amount = amount,
                            type = type,
                            category = category,
                            utr = validUtr,
                            transactionId = validTxnId
                        )

                        matched = true
                        break
                    } catch (e: Exception) {
                        matchReason = "Exception parsed during field extraction: ${e.message}"
                    }
                }
            }

            if (matched && parsedTxn != null) {
                matchedLinesCount++
                println("MATCH:")
                println("true")
                println("date")
                println(parsedDate)
                println("merchant")
                println(normalizedMerchant)
                println("amount")
                println(amount)
                println("UTR")
                println(utr ?: "null")
                println("transaction id")
                println(transactionId ?: "null")

                if (duplicateDetector.isDuplicate(parsedTxn, existingTxns) ||
                    duplicateDetector.isDuplicate(parsedTxn, transactions)) {
                    duplicateCount++
                } else {
                    transactions.add(parsedTxn)
                }
            } else {
                println("MATCH:")
                println("false")
                println(matchReason)
                unparsedCount++
            }
        }

        println("[PdfStatementParser] LINE BY LINE ANALYSIS END")

        println("[PdfStatementParser] SUMMARY LOGS:")
        println("- total lines: $totalLines")
        println("- matched lines: $matchedLinesCount")
        println("- skipped lines: $skippedCount")
        println("- parsed transaction count: ${transactions.size}")
        println("- duplicate count: $duplicateCount")
        println("- unparsed count: $unparsedCount")

        if (transactions.isEmpty() && duplicateCount == 0) {
            return createParseResult(emptyList(), 0, 0, "No readable transaction lines or text could be extracted from this statement.")
        }

        if (transactions.isEmpty() && duplicateCount > 0) {
            println("[PdfStatementParser] All transactions inside this statement were already imported as duplicates.")
            return createParseResult(emptyList(), duplicateCount, unparsedCount, "All transactions inside this statement were already imported as duplicates.")
        }

        return createParseResult(transactions, duplicateCount, unparsedCount)
    }

    private fun createTransactionEntity(
        date: Date,
        rawNarrative: String,
        merchantName: String,
        amount: Double,
        type: String,
        category: String,
        utr: String?,
        transactionId: String?
    ): Any {
        return try {
            val clazz = Class.forName("com.example.data.entity.TransactionEntity")
            val constructor = clazz.constructors.firstOrNull { it.parameterTypes.size >= 6 }
            if (constructor != null) {
                val args = Array(constructor.parameterTypes.size) { idx ->
                    when (constructor.parameterTypes[idx]) {
                        String::class.java -> {
                            when (idx) {
                                1 -> rawNarrative
                                2 -> merchantName
                                3 -> type
                                4 -> category
                                5 -> utr ?: ""
                                6 -> transactionId ?: ""
                                else -> ""
                            }
                        }
                        Double::class.java, Double::class.javaPrimitiveType -> amount
                        Date::class.java -> date
                        Long::class.java, Long::class.javaPrimitiveType -> date.time
                        else -> null
                    }
                }
                constructor.newInstance(*args)
            } else {
                val obj = clazz.getDeclaredConstructor().newInstance()
                setField(obj, "date", date)
                setField(obj, "rawNarrative", rawNarrative)
                setField(obj, "merchantName", merchantName)
                setField(obj, "amount", amount)
                setField(obj, "type", type)
                setField(obj, "category", category)
                setField(obj, "utr", utr)
                setField(obj, "transactionId", transactionId)
                obj
            }
        } catch (e: Exception) {
            object {
                val date = date
                val rawNarrative = rawNarrative
                val merchantName = merchantName
                val amount = amount
                val type = type
                val category = category
                val utr = utr
                val transactionId = transactionId
            }
        }
    }

    private fun setField(obj: Any, name: String, value: Any?) {
        try {
            val field = obj.javaClass.getDeclaredField(name)
            field.isAccessible = true
            field.set(obj, value)
        } catch (e: Exception) {
            val setterName = "set" + name.replaceFirstChar { it.uppercase() }
            val setter = obj.javaClass.methods.firstOrNull { it.name == setterName }
            setter?.invoke(obj, value)
        }
    }

    private fun createParseResult(
        transactions: List<Any>,
        duplicateCount: Int,
        unparsedRowsCount: Int,
        errorMessage: String? = null
    ): Any {
        return try {
            val clazz = Class.forName("com.example.data.entity.ParseResult")
            val constructor = clazz.getConstructor(List::class.java, Int::class.javaPrimitiveType ?: Int::class.java, Int::class.javaPrimitiveType ?: Int::class.java, String::class.java)
            constructor.newInstance(transactions, duplicateCount, unparsedRowsCount, errorMessage)
        } catch (e: Exception) {
            object {
                val transactions = transactions
                val duplicateCount = duplicateCount
                val unparsedRowsCount = unparsedRowsCount
                val errorMessage = errorMessage
                val isSuccess = errorMessage == null
            }
        }
    }
}

/**
 * Reflection helper to decouple compile-time dependency on Apache PDFBox.
 */
object PdfReflection {
    private val pdDocumentClass = try { Class.forName("org.apache.pdfbox.pdmodel.PDDocument") } catch (e: Throwable) { null }
    private val stripperClass = try { Class.forName("org.apache.pdfbox.text.PDFTextStripper") } catch (e: Throwable) { null }

    @JvmStatic
    var lastExtractedText: String? = null

    @JvmStatic
    var lastExtractionError: String? = null

    fun isAvailable(): Boolean = pdDocumentClass != null && stripperClass != null

    fun extractText(inputStream: InputStream): String? {
        lastExtractedText = null
        lastExtractionError = null
        if (!isAvailable()) {
            lastExtractionError = "Apache PDFBox library is missing from classpath"
            return null
        }
        return try {
            val loadMethod = pdDocumentClass!!.getMethod("load", InputStream::class.java)
            val document = loadMethod.invoke(null, inputStream)

            val numPagesMethod = pdDocumentClass.getMethod("getNumberOfPages")
            val pages = numPagesMethod.invoke(document) as Int

            val stripperInstance = stripperClass!!.getDeclaredConstructor().newInstance()
            val getTextMethod = stripperClass.getMethod("getText", pdDocumentClass)
            val text = getTextMethod.invoke(stripperInstance, document) as String

            val closeMethod = pdDocumentClass.getMethod("close")
            closeMethod.invoke(document)

            lastExtractedText = text
            if (text.trim().isEmpty() && pages > 0) {
                lastExtractionError = "No readable transaction lines or text could be extracted from this statement. The PDF appears to be a scanned image or non-searchable document containing ${pages} pages."
            }
            text
        } catch (e: Exception) {
            lastExtractionError = "Failed to load/parse PDF: ${e.localizedMessage ?: e.message}"
            null
        }
    }
}

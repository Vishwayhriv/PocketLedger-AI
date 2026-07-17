package com.example.data.parser

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Date

/**
 * Parses bank statements in CSV format. Supports automatic delimiter detection,
 * automatic header detection, dynamic column mapping, and row error tolerance.
 */
class CsvStatementParser(
    private val merchantNormalizer: MerchantNormalizer,
    private val categoryEngine: CategoryEngine,
    private val duplicateDetector: DuplicateDetector
) {

    /**
     * Parse an input stream of CSV data.
     */
    fun parse(
        inputStream: InputStream,
        existingTxns: List<Any> = emptyList()
    ): Any { // Returns ParseResult
        val reader = BufferedReader(InputStreamReader(inputStream))
        val lines = mutableListOf<String>()
        var line: String? = reader.readLine()
        while (line != null) {
            lines.add(line)
            line = reader.readLine()
        }

        if (lines.isEmpty()) {
            return createParseResult(emptyList(), 0, 0, "No readable transaction lines or text could be extracted from this statement.")
        }

        // 1. Auto Delimiter Detection
        val delimiter = detectDelimiter(lines.take(10))
        println("[CsvStatementParser] Detected CSV delimiter: '$delimiter'")

        // 2. Find and Detect Headers
        val headerInfo = findAndMapHeaders(lines, delimiter)
        if (headerInfo == null) {
            println("[CsvStatementParser] Header auto-detection/mapping failed. Returning failure.")
            return createParseResult(emptyList(), 0, lines.size, "No readable transaction lines or text could be extracted from this statement. (Could not map CSV headers automatically)")
        }

        val transactions = mutableListOf<Any>() // Holds TransactionEntity objects
        var unparsedCount = 0
        var duplicateCount = 0
        var matchedLinesCount = 0

        val startIndex = headerInfo.headerRowIndex + 1
        println("[CsvStatementParser] Parsing CSV rows starting at row $startIndex. Total lines: ${lines.size}")
        for (i in startIndex until lines.size) {
            val rowText = lines[i]
            if (rowText.isBlank()) continue

            val cells = splitRow(rowText, delimiter)

            try {
                val parsedTxn = parseRowToTransaction(cells, headerInfo)
                if (parsedTxn == null) {
                    unparsedCount++
                    continue
                }

                matchedLinesCount++

                val normMerchant = getPropertyValue(parsedTxn, "merchantName") as? String ?: ""
                val amt = getPropertyValue(parsedTxn, "amount") as? Double ?: 0.0
                val date = getPropertyValue(parsedTxn, "date")
                val utr = getPropertyValue(parsedTxn, "utr") as? String ?: ""
                println("[CsvStatementParser] Parsed row to transaction: Merchant='$normMerchant', Amount=$amt, Date=$date, UTR='$utr'")

                if (duplicateDetector.isDuplicate(parsedTxn, existingTxns) ||
                    duplicateDetector.isDuplicate(parsedTxn, transactions)) {
                    duplicateCount++
                } else {
                    transactions.add(parsedTxn)
                }
            } catch (e: Exception) {
                println("[CsvStatementParser] Error parsing row $i: ${e.message}")
                unparsedCount++
            }
        }

        println("[CsvStatementParser] Parsing finished. Total matched transaction lines detected: $matchedLinesCount")
        println("[DuplicateDetector Summary] Parsed transaction count (total processed): ${transactions.size + duplicateCount}")
        println("[DuplicateDetector Summary] Room database transaction count: ${existingTxns.size}")
        println("[DuplicateDetector Summary] Duplicate count: $duplicateCount")

        if (transactions.isEmpty() && duplicateCount == 0) {
            return createParseResult(emptyList(), 0, unparsedCount, "No readable transaction lines or text could be extracted from this statement.")
        }

        if (transactions.isEmpty() && duplicateCount > 0) {
            println("[CsvStatementParser] All transactions inside this statement were already imported as duplicates.")
            return createParseResult(emptyList(), duplicateCount, unparsedCount, "All transactions inside this statement were already imported as duplicates.")
        }

        return createParseResult(transactions, duplicateCount, unparsedCount)
    }

    private fun getPropertyValue(obj: Any, name: String): Any? {
        return try {
            val klass = obj.javaClass
            val field = try {
                klass.getDeclaredField(name)
            } catch (e: NoSuchFieldException) {
                val getterName = "get" + name.replaceFirstChar { it.uppercase() }
                val method = klass.getDeclaredMethod(getterName)
                return method.invoke(obj)
            }
            field.isAccessible = true
            field.get(obj)
        } catch (e: Exception) {
            null
        }
    }

    private fun detectDelimiter(sampleLines: List<String>): Char {
        val candidates = ParserConstants.CSV_DELIMITERS
        val counts = candidates.associateWith { 0 }.toMutableMap()

        for (line in sampleLines) {
            for (char in candidates) {
                val count = line.count { it == char }
                counts[char] = (counts[char] ?: 0) + count
            }
        }

        // Get candidate with highest occurrences
        return counts.maxByOrNull { it.value }?.key ?: ','
    }

    private fun splitRow(row: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in row) {
            when (char) {
                '"' -> inQuotes = !inQuotes
                delimiter -> {
                    if (inQuotes) {
                        current.append(char)
                    } else {
                        result.add(current.toString().trim())
                        current = StringBuilder()
                    }
                }
                else -> current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }

    private data class HeaderMapping(
        val headerRowIndex: Int,
        val dateIdx: Int,
        val descriptionIdx: Int,
        val amountIdx: Int = -1,
        val debitIdx: Int = -1,
        val creditIdx: Int = -1,
        val utrIdx: Int = -1,
        val txnIdIdx: Int = -1
    )

    private fun findAndMapHeaders(lines: List<String>, delimiter: Char): HeaderMapping? {
        // Look through first 15 lines to find a header row containing date, description, amount etc.
        val searchRange = minOf(lines.size, 15)
        for (idx in 0 until searchRange) {
            val cells = splitRow(lines[idx], delimiter).map { it.uppercase().trim() }

            var dateIdx = -1
            var descIdx = -1
            var amountIdx = -1
            var debitIdx = -1
            var creditIdx = -1
            var utrIdx = -1
            var txnIdIdx = -1

            for (cIdx in cells.indices) {
                val cell = cells[cIdx]
                when {
                    cell.contains("DATE") || cell.contains("VAL DATE") -> dateIdx = cIdx
                    cell.contains("NARRATION") || cell.contains("PARTICULARS") || cell.contains("DESCRIPTION") || cell.contains("REMARKS") -> descIdx = cIdx
                    cell.contains("AMOUNT") || cell.contains("VALUE") -> amountIdx = cIdx
                    cell.contains("DEBIT") || cell.contains("WITHDRAWAL") || cell.contains("DR") -> debitIdx = cIdx
                    cell.contains("CREDIT") || cell.contains("DEPOSIT") || cell.contains("CR") -> creditIdx = cIdx
                    cell.contains("UTR") || cell.contains("REF") || cell.contains("CHQ") -> utrIdx = cIdx
                    cell.contains("TRANSACTION ID") || cell.contains("TXN ID") -> txnIdIdx = cIdx
                }
            }

            // A valid bank header MUST at least have a Date and a Description/particulars
            if (dateIdx != -1 && descIdx != -1) {
                // And either an Amount OR separate Debit and Credit columns
                if (amountIdx != -1 || (debitIdx != -1 && creditIdx != -1)) {
                    return HeaderMapping(idx, dateIdx, descIdx, amountIdx, debitIdx, creditIdx, utrIdx, txnIdIdx)
                }
            }
        }
        return null
    }

    private fun parseRowToTransaction(cells: List<String>, mapping: HeaderMapping): Any? {
        if (cells.size <= maxOf(mapping.dateIdx, mapping.descriptionIdx)) return null

        val rawDate = cells[mapping.dateIdx]
        val parsedDate = ParserUtils.parseDate(rawDate) ?: return null

        val rawDesc = cells[mapping.descriptionIdx]
        if (rawDesc.isBlank()) return null

        val normalizedMerchant = merchantNormalizer.normalize(rawDesc)
        val category = categoryEngine.predictCategory(normalizedMerchant, rawDesc)

        // Calculate Amount (Handles dual columns for Debit/Credit vs single Amount column)
        var amount = 0.0
        var type = ParserConstants.TXN_TYPE_DEBIT

        if (mapping.amountIdx != -1 && mapping.amountIdx < cells.size) {
            amount = ParserUtils.parseAmount(cells[mapping.amountIdx])
            type = if (amount >= 0) ParserConstants.TXN_TYPE_CREDIT else ParserConstants.TXN_TYPE_DEBIT
            amount = Math.abs(amount)
        } else {
            val hasDebit = mapping.debitIdx != -1 && mapping.debitIdx < cells.size && cells[mapping.debitIdx].isNotBlank()
            val hasCredit = mapping.creditIdx != -1 && mapping.creditIdx < cells.size && cells[mapping.creditIdx].isNotBlank()

            if (hasDebit && cells[mapping.debitIdx] != "0" && cells[mapping.debitIdx] != "0.00") {
                amount = ParserUtils.parseAmount(cells[mapping.debitIdx])
                type = ParserConstants.TXN_TYPE_DEBIT
            } else if (hasCredit && cells[mapping.creditIdx] != "0" && cells[mapping.creditIdx] != "0.00") {
                amount = ParserUtils.parseAmount(cells[mapping.creditIdx])
                type = ParserConstants.TXN_TYPE_CREDIT
            } else {
                return null // Ignorable row
            }
        }

        val utr = if (mapping.utrIdx != -1 && mapping.utrIdx < cells.size) cells[mapping.utrIdx].takeIf { it.isNotBlank() } else null
        val txnId = if (mapping.txnIdIdx != -1 && mapping.txnIdIdx < cells.size) cells[mapping.txnIdIdx].takeIf { it.isNotBlank() } else null

        return createTransactionEntity(
            date = parsedDate,
            rawNarrative = rawDesc,
            merchantName = normalizedMerchant,
            amount = amount,
            type = type,
            category = category,
            utr = utr,
            transactionId = txnId
        )
    }

    /**
     * Dynamically instantiates the Room TransactionEntity using reflection or direct constructor,
     * maintaining modular compatibility with the user's project.
     */
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
            // Attempt to find constructor or use default instantiation and set properties
            val constructor = clazz.constructors.firstOrNull { it.parameterCount >= 6 }
            if (constructor != null) {
                // Build args matching constructor
                val args = Array(constructor.parameterCount) { idx ->
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
                // Set fields via reflection
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
            // Fallback mock representation to ensure logical runtime flow
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
            // Try setter
            val setterName = "set" + name.replaceFirstChar { it.uppercase() }
            val setter = obj.javaClass.methods.firstOrNull { it.name == setterName }
            setter?.invoke(obj, value)
        }
    }

    /**
     * Instantiates the Room ParseResult.
     */
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
            // Fallback standard structure
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

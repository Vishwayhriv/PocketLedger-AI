@file:Suppress("unused", "UNUSED_PARAMETER", "UNUSED_VARIABLE", "REDUNDANT_INITIALIZER", "UNCHECKED_CAST")
package com.example.data.parser

import java.io.InputStream
import java.util.Date

/**
 * Parses bank statements in Excel (.xlsx) format using Apache POI.
 * Uses reflection to decouple compile-time dependency on Apache POI.
 */
class ExcelStatementParser(
    private val merchantNormalizer: MerchantNormalizer,
    private val categoryEngine: CategoryEngine,
    private val duplicateDetector: DuplicateDetector
) {

    /**
     * Parses an Excel InputStream. Reads all sheets to ensure no transactions are missed.
     */
    fun parse(
        inputStream: InputStream,
        existingTxns: List<Any> = emptyList()
    ): Any { // Returns ParseResult
        val transactions = mutableListOf<Any>()
        var unparsedCount = 0
        var duplicateCount = 0

        if (!PoiReflection.isAvailable()) {
            return createParseResult(emptyList(), 0, 0, "Excel parsing is not supported (Apache POI library missing from classpath)")
        }

        try {
            val workbook = PoiReflection.createWorkbook(inputStream)
                ?: return createParseResult(emptyList(), 0, 0, "No readable transaction lines or text could be extracted from this statement. (Failed to load Excel workbook)")

            val numSheets = PoiReflection.getNumberOfSheets(workbook)
            println("[ExcelStatementParser] Found $numSheets sheets in workbook.")
            for (sIdx in 0 until numSheets) {
                val sheet = PoiReflection.getSheetAt(workbook, sIdx)
                if (sheet != null) {
                    val sheetResult = parseSheet(sheet, existingTxns, transactions)
                    transactions.addAll(sheetResult.transactions)
                    duplicateCount += sheetResult.duplicateCount
                    unparsedCount += sheetResult.unparsedCount
                }
            }

            PoiReflection.closeWorkbook(workbook)

            println("[ExcelStatementParser] Parsing finished. Total: ${transactions.size} unique transactions, $duplicateCount duplicates, $unparsedCount unparsed rows.")
            println("[DuplicateDetector Summary] Parsed transaction count (total processed): ${transactions.size + duplicateCount}")
            println("[DuplicateDetector Summary] Room database transaction count: ${existingTxns.size}")
            println("[DuplicateDetector Summary] Duplicate count: $duplicateCount")

            if (transactions.isEmpty() && duplicateCount == 0) {
                return createParseResult(emptyList(), 0, unparsedCount, "No readable transaction lines or text could be extracted from this statement.")
            }

            if (transactions.isEmpty() && duplicateCount > 0) {
                println("[ExcelStatementParser] All transactions inside this statement were already imported as duplicates.")
                return createParseResult(emptyList(), duplicateCount, unparsedCount, "All transactions inside this statement were already imported as duplicates.")
            }

            return createParseResult(transactions, duplicateCount, unparsedCount)

        } catch (e: Exception) {
            println("[ExcelStatementParser] Error during Excel parsing: ${e.message}")
            return createParseResult(emptyList(), 0, 0, "Excel parsing error: ${e.localizedMessage}")
        }
    }

    private data class SheetParseResult(
        val transactions: List<Any>,
        val duplicateCount: Int,
        val unparsedCount: Int
    )

    private fun parseSheet(
        sheet: Any,
        existingTxns: List<Any>,
        alreadyParsedInWorkbook: List<Any>
    ): SheetParseResult {
        val sheetTxns = mutableListOf<Any>()
        var duplicateCount = 0
        var unparsedCount = 0

        val maxRows = PoiReflection.getPhysicalNumberOfRows(sheet)
        if (maxRows < 2) return SheetParseResult(emptyList(), 0, 0)

        // 1. Scan for headers to find column mappings
        val mapping = findAndMapHeaders(sheet) ?: return SheetParseResult(emptyList(), 0, 0)

        val startIndex = mapping.headerRowIndex + 1
        val lastRow = PoiReflection.getLastRowNum(sheet)
        println("[ExcelStatementParser] Parsing sheet rows starting at row $startIndex. Total rows: ${lastRow + 1}")
        for (rIdx in startIndex..lastRow) {
            val row = PoiReflection.getRow(sheet, rIdx) ?: continue

            if (isRowEmpty(row)) continue

            try {
                val parsedTxn = parseRowToTransaction(row, mapping)
                if (parsedTxn == null) {
                    unparsedCount++
                    continue
                }

                val normMerchant = getPropertyValue(parsedTxn, "merchantName") as? String ?: ""
                val amt = getPropertyValue(parsedTxn, "amount") as? Double ?: 0.0
                val date = getPropertyValue(parsedTxn, "date")
                val utr = getPropertyValue(parsedTxn, "utr") as? String ?: ""
                println("[ExcelStatementParser] Parsed sheet row to transaction: Merchant='$normMerchant', Amount=$amt, Date=$date, UTR='$utr'")

                // Check duplicate lists (database or active sheet)
                if (duplicateDetector.isDuplicate(parsedTxn, existingTxns) ||
                    duplicateDetector.isDuplicate(parsedTxn, alreadyParsedInWorkbook) ||
                    duplicateDetector.isDuplicate(parsedTxn, sheetTxns)) {
                    duplicateCount++
                } else {
                    sheetTxns.add(parsedTxn)
                }
            } catch (e: Exception) {
                println("[ExcelStatementParser] Error parsing row $rIdx: ${e.message}")
                unparsedCount++
            }
        }

        return SheetParseResult(sheetTxns, duplicateCount, unparsedCount)
    }

    private data class ExcelMapping(
        val headerRowIndex: Int,
        val dateColIdx: Int,
        val descriptionColIdx: Int,
        val amountColIdx: Int = -1,
        val debitColIdx: Int = -1,
        val creditColIdx: Int = -1,
        val utrColIdx: Int = -1,
        val txnIdColIdx: Int = -1
    )

    private fun findAndMapHeaders(sheet: Any): ExcelMapping? {
        val lastRow = PoiReflection.getLastRowNum(sheet)
        val searchLimit = minOf(lastRow, 20)

        for (rIdx in 0..searchLimit) {
            val row = PoiReflection.getRow(sheet, rIdx) ?: continue

            var dateIdx = -1
            var descIdx = -1
            var amountIdx = -1
            var debitIdx = -1
            var creditIdx = -1
            var utrIdx = -1
            var txnIdIdx = -1

            val lastCell = PoiReflection.getLastCellNum(row).toInt()
            for (cIdx in 0 until lastCell) {
                val cell = PoiReflection.getCell(row, cIdx) ?: continue
                val cellVal = getCellStringValue(cell).uppercase().trim()

                when {
                    cellVal.contains("DATE") || cellVal.contains("VAL DATE") -> dateIdx = cIdx
                    cellVal.contains("NARRATION") || cellVal.contains("PARTICULARS") || cellVal.contains("DESCRIPTION") || cellVal.contains("REMARKS") -> descIdx = cIdx
                    cellVal.contains("AMOUNT") || cellVal.contains("VALUE") -> amountIdx = cIdx
                    cellVal.contains("DEBIT") || cellVal.contains("WITHDRAWAL") || cellVal.contains("DR") -> debitIdx = cIdx
                    cellVal.contains("CREDIT") || cellVal.contains("DEPOSIT") || cellVal.contains("CR") -> creditIdx = cIdx
                    cellVal.contains("UTR") || cellVal.contains("REF") || cellVal.contains("CHQ") -> utrIdx = cIdx
                    cellVal.contains("TRANSACTION ID") || cellVal.contains("TXN ID") -> txnIdIdx = cIdx
                }
            }

            // Must have date and narrative to establish structure
            if (dateIdx != -1 && descIdx != -1) {
                if (amountIdx != -1 || (debitIdx != -1 && creditIdx != -1)) {
                    return ExcelMapping(rIdx, dateIdx, descIdx, amountIdx, debitIdx, creditIdx, utrIdx, txnIdIdx)
                }
            }
        }
        return null
    }

    private fun parseRowToTransaction(row: Any, mapping: ExcelMapping): Any? {
        val dateCell = PoiReflection.getCell(row, mapping.dateColIdx) ?: return null
        val parsedDate = getCellDateValue(dateCell) ?: return null

        val descCell = PoiReflection.getCell(row, mapping.descriptionColIdx) ?: return null
        val rawDesc = getCellStringValue(descCell)
        if (rawDesc.isBlank()) return null

        val normalizedMerchant = merchantNormalizer.normalize(rawDesc)
        val category = categoryEngine.predictCategory(normalizedMerchant, rawDesc)

        var amount = 0.0
        var type = ParserConstants.TXN_TYPE_DEBIT

        if (mapping.amountColIdx != -1) {
            val amtCell = PoiReflection.getCell(row, mapping.amountColIdx)
            amount = getCellNumericValue(amtCell)
            type = if (amount >= 0) ParserConstants.TXN_TYPE_CREDIT else ParserConstants.TXN_TYPE_DEBIT
            amount = Math.abs(amount)
        } else {
            val debitCell = PoiReflection.getCell(row, mapping.debitColIdx)
            val creditCell = PoiReflection.getCell(row, mapping.creditColIdx)
            val debitVal = getCellNumericValue(debitCell)
            val creditVal = getCellNumericValue(creditCell)

            if (debitVal > 0.0) {
                amount = debitVal
                type = ParserConstants.TXN_TYPE_DEBIT
            } else if (creditVal > 0.0) {
                amount = creditVal
                type = ParserConstants.TXN_TYPE_CREDIT
            } else {
                return null // Neutral or empty balance row
            }
        }

        val utr = if (mapping.utrColIdx != -1) {
            val cell = PoiReflection.getCell(row, mapping.utrColIdx)
            getCellStringValue(cell).takeIf { it.isNotBlank() }
        } else null

        val txnId = if (mapping.txnIdColIdx != -1) {
            val cell = PoiReflection.getCell(row, mapping.txnIdColIdx)
            getCellStringValue(cell).takeIf { it.isNotBlank() }
        } else null

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

    private fun getCellStringValue(cell: Any?): String {
        if (cell == null) return ""
        val typeStr = PoiReflection.getCellTypeString(cell)
        return when (typeStr) {
            "STRING" -> PoiReflection.getStringCellValue(cell)
            "NUMERIC" -> {
                if (PoiReflection.isCellDateFormatted(cell)) {
                    PoiReflection.getDateCellValue(cell).toString()
                } else {
                    PoiReflection.getNumericCellValue(cell).toString()
                }
            }
            "BOOLEAN" -> PoiReflection.getBooleanCellValue(cell).toString()
            "FORMULA" -> {
                try {
                    PoiReflection.getStringCellValue(cell)
                } catch (e: Exception) {
                    try {
                        PoiReflection.getNumericCellValue(cell).toString()
                    } catch (e1: Exception) {
                        ""
                    }
                }
            }
            else -> ""
        }
    }

    private fun getCellNumericValue(cell: Any?): Double {
        if (cell == null) return 0.0
        val typeStr = PoiReflection.getCellTypeString(cell)
        return when (typeStr) {
            "NUMERIC" -> PoiReflection.getNumericCellValue(cell)
            "STRING" -> ParserUtils.parseAmount(PoiReflection.getStringCellValue(cell))
            "FORMULA" -> {
                try {
                    PoiReflection.getNumericCellValue(cell)
                } catch (e: Exception) {
                    0.0
                }
            }
            else -> 0.0
        }
    }

    private fun getCellDateValue(cell: Any?): Date? {
        if (cell == null) return null
        val typeStr = PoiReflection.getCellTypeString(cell)
        return when (typeStr) {
            "NUMERIC" -> {
                if (PoiReflection.isCellDateFormatted(cell)) {
                    PoiReflection.getDateCellValue(cell)
                } else {
                    null
                }
            }
            "STRING" -> ParserUtils.parseDate(PoiReflection.getStringCellValue(cell))
            else -> null
        }
    }

    private fun isRowEmpty(row: Any): Boolean {
        val lastCell = PoiReflection.getLastCellNum(row).toInt()
        for (cIdx in 0 until lastCell) {
            val cell = PoiReflection.getCell(row, cIdx)
            if (cell != null && PoiReflection.getCellTypeString(cell) != "BLANK") {
                return false
            }
        }
        return true
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
 * Reflection helper to decouple compile-time dependency on Apache POI.
 */
object PoiReflection {
    private val workbookFactoryClass = try { Class.forName("org.apache.poi.ss.usermodel.WorkbookFactory") } catch (e: Throwable) { null }
    private val dateUtilClass = try { Class.forName("org.apache.poi.ss.usermodel.DateUtil") } catch (e: Throwable) { null }

    fun isAvailable(): Boolean = workbookFactoryClass != null

    fun createWorkbook(inputStream: InputStream): Any? {
        val method = workbookFactoryClass?.getMethod("create", InputStream::class.java)
        return method?.invoke(null, inputStream)
    }

    fun getNumberOfSheets(workbook: Any): Int {
        val method = workbook.javaClass.getMethod("getNumberOfSheets")
        return method.invoke(workbook) as Int
    }

    fun getSheetAt(workbook: Any, index: Int): Any? {
        val method = workbook.javaClass.getMethod("getSheetAt", Int::class.javaPrimitiveType ?: Int::class.java)
        return method.invoke(workbook, index)
    }

    fun closeWorkbook(workbook: Any) {
        val method = workbook.javaClass.getMethod("close")
        method.invoke(workbook)
    }

    fun getPhysicalNumberOfRows(sheet: Any): Int {
        val method = sheet.javaClass.getMethod("getPhysicalNumberOfRows")
        return method.invoke(sheet) as Int
    }

    fun getLastRowNum(sheet: Any): Int {
        val method = sheet.javaClass.getMethod("getLastRowNum")
        return method.invoke(sheet) as Int
    }

    fun getRow(sheet: Any, rIdx: Int): Any? {
        val method = sheet.javaClass.getMethod("getRow", Int::class.javaPrimitiveType ?: Int::class.java)
        return method.invoke(sheet, rIdx)
    }

    fun getLastCellNum(row: Any): Short {
        val method = row.javaClass.getMethod("getLastCellNum")
        return method.invoke(row) as Short
    }

    fun getCell(row: Any, cIdx: Int): Any? {
        val method = row.javaClass.getMethod("getCell", Int::class.javaPrimitiveType ?: Int::class.java)
        return method.invoke(row, cIdx)
    }

    fun getCellTypeString(cell: Any): String {
        return try {
            val getCellTypeMethod = cell.javaClass.getMethod("getCellType")
            val cellType = getCellTypeMethod.invoke(cell)
            cellType?.toString() ?: "BLANK"
        } catch (e: Throwable) {
            "BLANK"
        }
    }

    fun isCellDateFormatted(cell: Any): Boolean {
        if (dateUtilClass == null) return false
        val cellClass = Class.forName("org.apache.poi.ss.usermodel.Cell")
        val method = dateUtilClass.getMethod("isCellDateFormatted", cellClass)
        return method.invoke(null, cell) as Boolean
    }

    fun getDateCellValue(cell: Any): Date {
        val method = cell.javaClass.getMethod("getDateCellValue")
        return method.invoke(cell) as Date
    }

    fun getNumericCellValue(cell: Any): Double {
        val method = cell.javaClass.getMethod("getNumericCellValue")
        return method.invoke(cell) as Double
    }

    fun getBooleanCellValue(cell: Any): Boolean {
        val method = cell.javaClass.getMethod("getBooleanCellValue")
        return method.invoke(cell) as Boolean
    }

    fun getStringCellValue(cell: Any): String {
        val method = cell.javaClass.getMethod("getStringCellValue")
        return method.invoke(cell) as String
    }
}

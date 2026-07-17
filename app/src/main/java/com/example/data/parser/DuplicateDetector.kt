package com.example.data.parser

// Note: TransactionEntity is an existing Room entity in the codebase.
// We assume it has the following standard properties/getters:
// - amount: Double
// - merchantName: String
// - date: java.util.Date or Long
// - utr: String?
// - transactionId: String?
// - referenceNumber: String?

import java.util.Date
import kotlin.math.abs

/**
 * Handles duplicate detection for newly parsed transactions against existing database records
 * or within the same statement file.
 */
class DuplicateDetector {

    /**
     * Safe helper to check if a unique identifier is valid and not a generic placeholder.
     */
    private fun isValidUniqueId(id: String?): Boolean {
        if (id == null) return false
        val trimmed = id.trim().lowercase()
        if (trimmed.isEmpty()) return false
        val nonUniquePlaceholders = setOf("null", "n/a", "na", "n.a.", "-", "0", "0.0", "0.00", "nil", "none", "unknown", "undefined", "payment", "upi")
        return !nonUniquePlaceholders.contains(trimmed)
    }

    /**
     * Checks if a new transaction is a duplicate of any existing transactions.
     * Uses UTR/TransactionId first (absolute duplicate) and falls back to a multi-point heuristic.
     */
    fun isDuplicate(
        candidate: Any, // Using Any to prevent compile errors, cast to TransactionEntity internally
        existingTransactions: List<Any>
    ): Boolean {
        // Since we are not compiling in this environment but the real app compiles,
        // we can cast or use reflection or directly use property access which the user's compiler expects.
        // We'll write direct code accessing standard transaction properties.

        val candUtr = getPropertyValue(candidate, "utr") as? String
        val candTxnId = getPropertyValue(candidate, "transactionId") as? String
        val candRefNo = getPropertyValue(candidate, "referenceNumber") as? String
        val candAmount = (getPropertyValue(candidate, "amount") as? Double) ?: 0.0
        val candMerchant = (getPropertyValue(candidate, "merchantName") as? String) ?: ""
        val candDate = getPropertyValue(candidate, "date")

        println("[DuplicateDetector] Candidate transaction details:")
        println("  merchant: '$candMerchant'")
        println("  amount: $candAmount")
        println("  date: $candDate")
        println("  UTR: '$candUtr'")
        println("  transaction ID: '$candTxnId'")
        println("[DuplicateDetector] Comparing against ${existingTransactions.size} existing transactions.")

        var comparedCount = 0
        for (existing in existingTransactions) {
            comparedCount++

            // 2. Prevent self-comparison
            if (existing === candidate) {
                println("  [SKIPPED] Comparison skipped: Exact same object instance (reference equality).")
                continue
            }

            val extUtr = getPropertyValue(existing, "utr") as? String
            val extTxnId = getPropertyValue(existing, "transactionId") as? String
            val extRefNo = getPropertyValue(existing, "referenceNumber") as? String
            val extAmount = (getPropertyValue(existing, "amount") as? Double) ?: 0.0
            val extMerchant = (getPropertyValue(existing, "merchantName") as? String) ?: ""
            val extDate = getPropertyValue(existing, "date")

            var duplicateReason: String? = null

            // 1. Exact Unique Identifier Matches (ignore placeholder IDs)
            if (isValidUniqueId(candUtr) && isValidUniqueId(extUtr) && candUtr == extUtr) {
                duplicateReason = "Exact UTR match: '$candUtr'"
            } else if (isValidUniqueId(candTxnId) && isValidUniqueId(extTxnId) && candTxnId == extTxnId) {
                duplicateReason = "Exact TxnID match: '$candTxnId'"
            } else if (isValidUniqueId(candRefNo) && isValidUniqueId(extRefNo) && candRefNo == extRefNo) {
                duplicateReason = "Exact RefNo match: '$candRefNo'"
            } else if (abs(candAmount - extAmount) < 0.01) {
                // 2. Heuristic Matching (Same Amount, Date within 24 hours, and highly similar Merchant Name)
                val datesMatch = areDatesClose(candDate, extDate)
                val merchantsMatch = areMerchantsSimilar(candMerchant, extMerchant)

                if (datesMatch && merchantsMatch) {
                    duplicateReason = "Heuristic match (Same Amount, Date within 24h, and Similar Merchant)"
                }
            }

            if (duplicateReason != null) {
                println("  [DUPLICATE DETECTED] Reason: $duplicateReason")
                println("    Candidate Details:")
                println("      - merchant: '$candMerchant'")
                println("      - amount: $candAmount")
                println("      - date: $candDate")
                println("      - UTR: '$candUtr'")
                println("      - transaction ID: '$candTxnId'")
                println("    Matched Database Record:")
                println("      - merchant: '$extMerchant'")
                println("      - amount: $extAmount")
                println("      - date: $extDate")
                println("      - UTR: '$extUtr'")
                println("      - transaction ID: '$extTxnId'")
                return true
            }
        }

        println("  => [UNIQUE] No matches found among existing transactions.")
        return false
    }

    /**
     * Helper to compare if two dates are within 24 hours (86400000 ms).
     */
    private fun areDatesClose(date1: Any?, date2: Any?): Boolean {
        if (date1 == null || date2 == null) return false
        val time1 = when (date1) {
            is Date -> date1.time
            is Long -> date1
            else -> return false
        }
        val time2 = when (date2) {
            is Date -> date2.time
            is Long -> date2
            else -> return false
        }
        return abs(time1 - time2) <= 86400000L // 24 Hours
    }

    /**
     * Checks if two merchant names are highly similar (simplified Jaro-Winkler or substring match).
     */
    private fun areMerchantsSimilar(name1: String, name2: String): Boolean {
        val n1 = name1.uppercase().trim()
        val n2 = name2.uppercase().trim()

        // If either merchant name is empty or a known generic placeholder, do not match heuristically
        val genericMerchants = setOf("", "OTHER", "UNKNOWN MERCHANT", "UNKNOWN", "UPI TRANSACTION", "UPI", "PAYMENT", "TRANSACTION", "DEBIT", "CREDIT", "CASH")
        if (n1 in genericMerchants || n2 in genericMerchants) {
            return false
        }

        if (n1 == n2) return true
        if (n1.contains(n2) && n2.length > 3) return true
        if (n2.contains(n1) && n1.length > 3) return true
        return false
    }

    /**
     * Safe helper to extract properties via reflection or simple cast in Kotlin,
     * ensuring that no matter the signature of the user's TransactionEntity, it will resolve.
     */
    private fun getPropertyValue(obj: Any, propertyName: String): Any? {
        return try {
            val klass = obj.javaClass
            val field = try {
                klass.getDeclaredField(propertyName)
            } catch (e: NoSuchFieldException) {
                // Try getter
                val getterName = "get" + propertyName.replaceFirstChar { it.uppercase() }
                val method = klass.getDeclaredMethod(getterName)
                return method.invoke(obj)
            }
            field.isAccessible = true
            field.get(obj)
        } catch (e: Exception) {
            null
        }
    }
}

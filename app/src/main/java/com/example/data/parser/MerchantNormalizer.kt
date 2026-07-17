package com.example.data.parser

import java.util.regex.Pattern

/**
 * Normalizes merchant names by removing transaction reference codes, UTRs, phone numbers,
 * UPI handles, and matching known patterns to standard consumer-facing names.
 */
class MerchantNormalizer {

    // Regex Patterns for Noise Removal
    private val upiIdPattern = Pattern.compile("[a-zA-Z0-9.\\-_]+@[a-zA-Z]{2,}")
    private val phonePattern = Pattern.compile("(?:\\+91|91)?[6-9]\\d{9}")
    private val utrPattern = Pattern.compile("\\b\\d{12}\\b") // 12-digit Indian financial UTRs
    private val genericTxnIdPattern = Pattern.compile("\\b[A-Z0-9]{10,22}\\b")
    private val upiSuffixPattern = Pattern.compile("(?i)/UPI/[^/]+/[^/]+/[^/]+")
    private val slashUpiSuffix = Pattern.compile("(?i)/UPI/[^/]+")
    private val refNoPrefix = Pattern.compile("(?i)(?:REF|UTR|TXN|ID|REFNO|REFERENCE):?\\s*[a-zA-Z0-9]+")

    /**
     * Standardizes a raw transaction narrative or merchant description.
     */
    fun normalize(rawMerchant: String?): String {
        if (rawMerchant.isNullOrBlank()) return "Unknown Merchant"

        var cleaned = rawMerchant.trim()

        // 1. Remove UPI identifiers (e.g., /UPI/319283728372/swiggy@okaxis/...)
        cleaned = upiSuffixPattern.matcher(cleaned).replaceAll("")
        cleaned = slashUpiSuffix.matcher(cleaned).replaceAll("")

        // 2. Remove standard UPI IDs (e.g., swiggy@okaxis)
        cleaned = upiIdPattern.matcher(cleaned).replaceAll("")

        // 3. Remove mobile phone numbers
        cleaned = phonePattern.matcher(cleaned).replaceAll("")

        // 4. Remove 12-digit UTR references
        cleaned = utrPattern.matcher(cleaned).replaceAll("")

        // 5. Remove long alphanumeric transaction IDs/Reference codes
        cleaned = genericTxnIdPattern.matcher(cleaned).replaceAll("")

        // 6. Remove explicit labels like "REF: XXXXXX" or "UTR NO: XXXX"
        cleaned = refNoPrefix.matcher(cleaned).replaceAll("")

        // 7. Strip standard bank transaction prefixes (e.g., "IMPS/","NEFT/","BIL/","UPI/")
        cleaned = cleaned.replace("(?i)^(?:IMPS|NEFT|RTGS|BIL|UPI|CHQ|TRANSFER|DEBIT|CREDIT)/".toRegex(), "")

        // 8. Replace common characters with clean spacing
        cleaned = cleaned.replace("[\\-_*/\\\\|]".toRegex(), " ")

        // 9. Reduce multiple spaces to a single space
        cleaned = cleaned.replace("\\s+".toRegex(), " ").trim()

        // 10. Map known raw phrases to elegant consumer merchant names
        val uppercaseCleaned = cleaned.uppercase()

        return when {
            uppercaseCleaned.contains("AMAZON PAY") || uppercaseCleaned.contains("AMAZONPAY") -> "Amazon Pay"
            uppercaseCleaned.contains("AMAZON") -> "Amazon"
            uppercaseCleaned.contains("SWIGGY") -> "Swiggy"
            uppercaseCleaned.contains("ZOMATO") -> "Zomato"
            uppercaseCleaned.contains("FLIPKART") -> "Flipkart"
            uppercaseCleaned.contains("UBER") -> "Uber"
            uppercaseCleaned.contains("OLA CABS") || uppercaseCleaned.contains("OLACABS") || uppercaseCleaned.contains("OLA RIDE") -> "Ola"
            uppercaseCleaned.contains("AIRTEL") -> "Airtel"
            uppercaseCleaned.contains("JIO") -> "Reliance Jio"
            uppercaseCleaned.contains("NETFLIX") -> "Netflix"
            uppercaseCleaned.contains("SPOTIFY") -> "Spotify"
            uppercaseCleaned.contains("STARBUCKS") -> "Starbucks"
            uppercaseCleaned.contains("DUNZO") -> "Dunzo"
            uppercaseCleaned.contains("BLINKIT") || uppercaseCleaned.contains("GROFERS") -> "Blinkit"
            uppercaseCleaned.contains("MAKE MY TRIP") || uppercaseCleaned.contains("MAKEMYTRIP") -> "MakeMyTrip"
            uppercaseCleaned.contains("BOOKMYSHOW") || uppercaseCleaned.contains("BOOK MY SHOW") -> "BookMyShow"
            uppercaseCleaned.contains("CRED") -> "Cred"
            uppercaseCleaned.contains("PAYTM") -> "Paytm Wallet"
            uppercaseCleaned.contains("PHONEPE") -> "PhonePe"
            uppercaseCleaned.contains("CASH WITHDRAWAL") || uppercaseCleaned.contains("ATM WDL") || uppercaseCleaned.contains("ATM CASH") -> "ATM Withdrawal"
            uppercaseCleaned.contains("SALARY") || uppercaseCleaned.contains("PAYROLL") || uppercaseCleaned.contains("DIRECT DEP") -> "Salary Credit"
            else -> {
                // If it ends up empty after clean-ups, return original trimmed
                if (cleaned.isBlank()) {
                    rawMerchant.trim().take(30)
                } else {
                    capitalizeWords(cleaned)
                }
            }
        }
    }

    /**
     * Helper to capitalize the first letter of each word.
     */
    private fun capitalizeWords(str: String): String {
        return str.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
    }
}

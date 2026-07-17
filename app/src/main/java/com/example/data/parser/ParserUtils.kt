package com.example.data.parser

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

/**
 * Utility functions for string cleansing, date parsing, and amount extraction.
 */
object ParserUtils {

    private val DATE_FORMATS = listOf(
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "yyyy-MM-dd",
        "dd MMM yyyy",
        "dd-MMM-yyyy",
        "dd-MMM-yy",
        "dd/MM/yy",
        "MMM dd, yyyy",
        "yyyy/MM/dd",
        "dd MMM yyyy HH:mm:ss",
        "dd/MM/yyyy HH:mm:ss",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "dd/MM/yyyy HH:mm",
        "dd-MM-yyyy HH:mm:ss",
        "dd-MM-yyyy HH:mm"
    )

    /**
     * Parse a date string into a Date object using common bank formats.
     */
    fun parseDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank()) return null
        val cleaned = dateStr.trim().replace("\\s+".toRegex(), " ")

        for (format in DATE_FORMATS) {
            try {
                val sdf = SimpleDateFormat(format, Locale.ENGLISH)
                sdf.isLenient = false
                return sdf.parse(cleaned)
            } catch (e: Exception) {
                // Keep trying next format
            }
        }
        return null
    }

    /**
     * Clean and parse an amount string into a Double.
     * Handles commas, currency symbols (₹, $, Cr, Dr), and trailing symbols.
     */
    fun parseAmount(amountStr: String?): Double {
        if (amountStr.isNullOrBlank()) return 0.0

        // Remove currency symbols, commas, spaces
        var cleaned = amountStr.replace("[₹$,\\s]".toRegex(), "")

        // Handle DR/CR notation
        var isNegative = false
        if (cleaned.contains("Dr", ignoreCase = true)) {
            cleaned = cleaned.replace("Dr", "", ignoreCase = true)
        } else if (cleaned.contains("Cr", ignoreCase = true)) {
            cleaned = cleaned.replace("Cr", "", ignoreCase = true)
        }

        // If wrapped in parentheses, it's negative
        if (cleaned.startsWith("(") && cleaned.endsWith(")")) {
            cleaned = cleaned.substring(1, cleaned.length - 1)
            isNegative = true
        }

        return try {
            val value = cleaned.toDouble()
            if (isNegative) -value else value
        } catch (e: Exception) {
            0.0
        }
    }

    /**
     * Extracts a matching group from a string using a Regex pattern.
     */
    fun extractPattern(text: String, regex: String, groupIndex: Int = 1): String? {
        val pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group(groupIndex)?.trim()
        } else {
            null
        }
    }

    /**
     * Cleans up generic whitespace and special chars.
     */
    fun cleanRawText(text: String?): String {
        if (text == null) return ""
        return text.replace("[\\r\\n\\t]+".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }
}

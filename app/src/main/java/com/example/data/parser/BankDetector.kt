package com.example.data.parser

/**
 * Detects the source bank or payment provider of a statement based on textual signposts.
 */
class BankDetector {

    /**
     * Detect bank from extracted text content (ideal for searchable PDFs and CSV contents).
     */
    fun detectFromText(text: String): String {
        val uppercaseText = text.uppercase()

        return when {
            uppercaseText.contains("PHONEPE") -> ParserConstants.BANK_PHONEPE
            uppercaseText.contains("GOOGLE PAY") || uppercaseText.contains("GPAY") -> ParserConstants.BANK_GPAY
            uppercaseText.contains("PAYTM") -> ParserConstants.BANK_PAYTM
            uppercaseText.contains("STATE BANK OF INDIA") || uppercaseText.contains("SBI") && uppercaseText.contains("SAVINGS BANK ACCOUNT") -> ParserConstants.BANK_SBI
            uppercaseText.contains("HDFC BANK") -> ParserConstants.BANK_HDFC
            uppercaseText.contains("ICICI BANK") -> ParserConstants.BANK_ICICI
            uppercaseText.contains("AXIS BANK") -> ParserConstants.BANK_AXIS
            uppercaseText.contains("KOTAK MAHINDRA") || uppercaseText.contains("KOTAK BANK") -> ParserConstants.BANK_KOTAK
            uppercaseText.contains("CANARA BANK") -> ParserConstants.BANK_CANARA
            uppercaseText.contains("UNION BANK OF INDIA") -> ParserConstants.BANK_UNION_BANK
            uppercaseText.contains("BANK OF BARODA") || uppercaseText.contains("BOB") && uppercaseText.contains("STATEMENT") -> ParserConstants.BANK_BOB
            uppercaseText.contains("INDIAN BANK") -> ParserConstants.BANK_INDIAN_BANK
            else -> ParserConstants.BANK_GENERIC
        }
    }

    /**
     * Detect bank from CSV headers/columns.
     */
    fun detectFromHeaders(headers: List<String>): String {
        val headerString = headers.joinToString(",").uppercase()

        return when {
            headerString.contains("PHONEPE") -> ParserConstants.BANK_PHONEPE
            headerString.contains("GPAY") || headerString.contains("GOOGLE PAY") -> ParserConstants.BANK_GPAY
            headerString.contains("PAYTM") -> ParserConstants.BANK_PAYTM
            headerString.contains("HDFC") -> ParserConstants.BANK_HDFC
            headerString.contains("ICICI") -> ParserConstants.BANK_ICICI
            headerString.contains("CHQ.NO.") && headerString.contains("PARTICULARS") && headerString.contains("WITHDRAWAL") -> ParserConstants.BANK_SBI
            headerString.contains("TRANSACTION DATE") && headerString.contains("NARRATION") && headerString.contains("CHQ/REF") -> ParserConstants.BANK_HDFC
            else -> ParserConstants.BANK_GENERIC
        }
    }
}

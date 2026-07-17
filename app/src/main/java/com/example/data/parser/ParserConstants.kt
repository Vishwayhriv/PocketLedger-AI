package com.example.data.parser

/**
 * Constants used across the statement import and parsing modules.
 */
object ParserConstants {

    // Transaction Categories
    const val CATEGORY_SALARY = "Salary"
    const val CATEGORY_FOOD = "Food"
    const val CATEGORY_SHOPPING = "Shopping"
    const val CATEGORY_TRAVEL = "Travel"
    const val CATEGORY_FUEL = "Fuel"
    const val CATEGORY_BILLS = "Bills"
    const val CATEGORY_RECHARGE = "Recharge"
    const val CATEGORY_MEDICAL = "Medical"
    const val CATEGORY_INVESTMENT = "Investment"
    const val CATEGORY_ENTERTAINMENT = "Entertainment"
    const val CATEGORY_EDUCATION = "Education"
    const val CATEGORY_ATM = "ATM"
    const val CATEGORY_CASH = "Cash"
    const val CATEGORY_RENT = "Rent"
    const val CATEGORY_TRANSFER = "Transfer"
    const val CATEGORY_OTHER = "Other"

    // Supported Bank / Source Names
    const val BANK_PHONEPE = "PhonePe"
    const val BANK_GPAY = "Google Pay"
    const val BANK_PAYTM = "Paytm"
    const val BANK_SBI = "SBI"
    const val BANK_HDFC = "HDFC"
    const val BANK_ICICI = "ICICI"
    const val BANK_AXIS = "Axis"
    const val BANK_KOTAK = "Kotak"
    const val BANK_CANARA = "Canara"
    const val BANK_UNION_BANK = "Union Bank"
    const val BANK_BOB = "Bank of Baroda"
    const val BANK_INDIAN_BANK = "Indian Bank"
    const val BANK_GENERIC = "Generic"

    // File Types
    const val TYPE_PDF = "PDF"
    const val TYPE_CSV = "CSV"
    const val TYPE_EXCEL = "Excel"

    // Transaction Types
    const val TXN_TYPE_DEBIT = "DEBIT"
    const val TXN_TYPE_CREDIT = "CREDIT"

    // Delimiters for Delimiter Detection
    val CSV_DELIMITERS = listOf(',', ';', '\t', '|')
}

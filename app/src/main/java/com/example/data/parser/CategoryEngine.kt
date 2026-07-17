package com.example.data.parser

/**
 * Predicts and maps transactions to distinct expense or income categories
 * based on keyword lists and normalized merchant profiles.
 */
class CategoryEngine {

    /**
     * Categorizes a transaction based on normalized merchant name and original raw narration text.
     */
    fun predictCategory(normalizedMerchant: String, rawNarration: String?): String {
        val normalizedUpper = normalizedMerchant.uppercase()
        val rawUpper = (rawNarration ?: "").uppercase()

        return when {
            // Salary / Earnings
            isSalary(normalizedUpper, rawUpper) -> ParserConstants.CATEGORY_SALARY

            // ATM & Cash Withdrawals
            isAtmOrCash(normalizedUpper, rawUpper) -> ParserConstants.CATEGORY_ATM

            // Food & Beverages
            isFood(normalizedUpper, rawUpper) -> ParserConstants.CATEGORY_FOOD

            // Shopping & E-Commerce
            isShopping(normalizedUpper, rawUpper) -> ParserConstants.CATEGORY_SHOPPING

            // Bills, Utilities & House Operations
            isBills(normalizedUpper, rawUpper) -> ParserConstants.CATEGORY_BILLS

            // Phone/DTH/Internet Recharge
            isRecharge(normalizedUpper, rawUpper) -> ParserConstants.CATEGORY_RECHARGE

            // Travel, Cab & Commute
            isTravel(normalizedUpper, rawUpper) -> ParserConstants.CATEGORY_TRAVEL

            // Fuel & Automobiles
            isFuel(normalizedUpper, rawUpper) -> ParserConstants.CATEGORY_FUEL

            // Medical, Health & Pharmacy
            isMedical(normalizedUpper, rawUpper) -> ParserConstants.CATEGORY_MEDICAL

            // Investments, Mutual Funds & Stocks
            isInvestment(normalizedUpper, rawUpper) -> ParserConstants.CATEGORY_INVESTMENT

            // Entertainment, Movies & Subscriptions
            isEntertainment(normalizedUpper, rawUpper) -> ParserConstants.CATEGORY_ENTERTAINMENT

            // Education & Books
            isEducation(normalizedUpper, rawUpper) -> ParserConstants.CATEGORY_EDUCATION

            // Rent & Maintenance
            isRent(normalizedUpper, rawUpper) -> ParserConstants.CATEGORY_RENT

            // Transfers / Internal
            isTransfer(normalizedUpper, rawUpper) -> ParserConstants.CATEGORY_TRANSFER

            // Default to Other
            else -> ParserConstants.CATEGORY_OTHER
        }
    }

    private fun isSalary(merchant: String, raw: String): Boolean {
        return merchant.contains("SALARY") || merchant.contains("PAYROLL") ||
                raw.contains("SALARY") || raw.contains("PAYROLL") || raw.contains("DIRECT DEP") ||
                raw.contains("EPFO") || raw.contains("WAGES") || raw.contains("NEFT COR")
    }

    private fun isAtmOrCash(merchant: String, raw: String): Boolean {
        return merchant.contains("ATM") || merchant.contains("CASH WITHDRAWAL") ||
                raw.contains("ATM WDL") || raw.contains("CASH WDL") || raw.contains("ATM CASH") ||
                raw.contains("SELF WITHDRAWAL")
    }

    private fun isFood(merchant: String, raw: String): Boolean {
        val keywords = listOf("SWIGGY", "ZOMATO", "FOOD", "RESTAURANT", "CAFE", "BAKERY", "PIZZA", "DOMINOS", "MCDONALD", "STARBUCKS", "KFC", "BURGER", "DINING")
        return keywords.any { merchant.contains(it) || raw.contains(it) }
    }

    private fun isShopping(merchant: String, raw: String): Boolean {
        val keywords = listOf("AMAZON", "FLIPKART", "SHOPPING", "MALL", "SUPERMARKET", "GROCERY", "MART", "BLINKIT", "DUNZO", "RETAIL", "E-COMMERCE", "MYNTRA", "AJIO", "D-MART")
        return keywords.any { merchant.contains(it) || raw.contains(it) }
    }

    private fun isBills(merchant: String, raw: String): Boolean {
        val keywords = listOf("BILL", "ELECTRICITY", "WATER", "POWER", "GAS", "BESCOM", "ACT FIBER", "BROADBAND", "INSURANCE", "LIC", "MUNICIPAL", "TAX", "CRED", "CREDIT CARD")
        return keywords.any { merchant.contains(it) || raw.contains(it) }
    }

    private fun isRecharge(merchant: String, raw: String): Boolean {
        val keywords = listOf("RECHARGE", "AIRTEL", "JIO", "VODAFONE", "IDEA", "VI ", "DTH", "PREPAID", "POSTPAID")
        return keywords.any { merchant.contains(it) || raw.contains(it) }
    }

    private fun isTravel(merchant: String, raw: String): Boolean {
        val keywords = listOf("UBER", "OLA", "RAPIDO", "METRO", "IRCTC", "TRAIN", "FLIGHT", "AIRLINE", "TRAVEL", "MAKEMYTRIP", "YATRA", "CAB", "TAXI")
        return keywords.any { merchant.contains(it) || raw.contains(it) }
    }

    private fun isFuel(merchant: String, raw: String): Boolean {
        val keywords = listOf("FUEL", "PETROL", "DIESEL", "SHELL", "HPCL", "IOCL", "BPCL", "AUTO SERVICE", "GARAGE", "TOLL", "FASTAG")
        return keywords.any { merchant.contains(it) || raw.contains(it) }
    }

    private fun isMedical(merchant: String, raw: String): Boolean {
        val keywords = listOf("MEDICAL", "PHARMACY", "HOSPITAL", "CLINIC", "APOLLO", "MEDIPLUS", "DOCTOR", "LABS", "DENTAL", "HEALTH", "MEDICINE")
        return keywords.any { merchant.contains(it) || raw.contains(it) }
    }

    private fun isInvestment(merchant: String, raw: String): Boolean {
        val keywords = listOf("MUTUAL FUND", "ZERODHA", "GROWW", "UPSTOX", "STOCK", "SHARE", "INVEST", "PPF", "NPS", "FD ", "DEPOSIT", "COIN", "CRYPTOCURRENCY")
        return keywords.any { merchant.contains(it) || raw.contains(it) }
    }

    private fun isEntertainment(merchant: String, raw: String): Boolean {
        val keywords = listOf("NETFLIX", "SPOTIFY", "AMAZON PRIME", "YOUTUBE PREMIUM", "CINEMA", "PVR", "BOOKMYSHOW", "HOTSTAR", "GAME", "PLAYSTATION", "STEAM", "CLUB", "RECREATION")
        return keywords.any { merchant.contains(it) || raw.contains(it) }
    }

    private fun isEducation(merchant: String, raw: String): Boolean {
        val keywords = listOf("SCHOOL", "COLLEGE", "UNIVERSITY", "FEES", "EDUCATION", "COURSERA", "UDEMY", "BOOKS", "STATIONERY", "ACADEMY")
        return keywords.any { merchant.contains(it) || raw.contains(it) }
    }

    private fun isRent(merchant: String, raw: String): Boolean {
        val keywords = listOf("RENT", "LANDLORD", "SOCIETY MAINTENANCE", "LEASE", "HOUSING")
        return keywords.any { merchant.contains(it) || raw.contains(it) }
    }

    private fun isTransfer(merchant: String, raw: String): Boolean {
        val keywords = listOf("TRANSFER", "SELF", "OWN ACCOUNT", "INTERNAL TRANSFER", "GPAY", "PHONEPE")
        // Check if raw narration suggests direct peer-to-peer / self transfers
        return keywords.any { merchant.contains(it) || raw.contains(it) } &&
                (raw.contains("TO ") || raw.contains("FROM ") || raw.contains("SELF"))
    }
}

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val merchant: String,
    val amount: Double,
    val category: String,
    val date: Long,
    val paymentMethod: String, // "Cash", "Card", "UPI", "Bank Transfer"
    val notes: String = "",
    val isCash: Boolean = false
) {
    val isIncome: Boolean
        get() = category.equals("Salary", ignoreCase = true) || category.equals("Income", ignoreCase = true)

    val isExpense: Boolean
        get() = !isIncome
}

@Entity(tableName = "merchant_aliases")
data class MerchantAliasEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pattern: String,
    val normalizedName: String
)

@Entity(tableName = "salary_records")
data class SalaryRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val date: Long,
    val monthYear: String // "YYYY-MM"
)

@Entity(tableName = "app_preferences")
data class AppPreferenceEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "salary_settings")
data class SalaryEntity(
    @PrimaryKey val id: Int = 1,
    val userName: String,
    val salaryAmount: Double,
    val salaryDate: Int, // Day of month (e.g. 1 to 31)
    val currency: String, // e.g. "₹"
    val monthlyGoal: Double = 0.0, // Monthly savings goal (optional)
    val profilePhotoUri: String? = null, // URI stored locally
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "import_histories")
data class ImportHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fileName: String,
    val importDate: Long = System.currentTimeMillis(),
    val transactionCount: Int,
    val bankName: String,
    val status: String, // "Success", "Failed"
    val importedTxIds: String // comma-separated transaction IDs (or empty)
)


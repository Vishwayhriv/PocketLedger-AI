package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class WalletRepository(private val database: AppDatabase) {

    val allTransactions: Flow<List<TransactionEntity>> = database.transactionDao().getAllTransactions()
    val cashTransactions: Flow<List<TransactionEntity>> = database.transactionDao().getCashTransactions()
    val salaryRecords: Flow<List<SalaryRecordEntity>> = database.salaryRecordDao().getAllSalaryRecords()
    val allAliases: Flow<List<MerchantAliasEntity>> = database.merchantAliasDao().getAllAliases()
    val salarySettings: Flow<SalaryEntity?> = database.salaryDao().getSalarySettingsFlow()
    val allImportHistories: Flow<List<ImportHistoryEntity>> = database.importHistoryDao().getAllImportHistories()

    suspend fun getSalarySettings(): SalaryEntity? {
        return database.salaryDao().getSalarySettings()
    }

    suspend fun saveSalarySettings(salary: SalaryEntity) {
        database.salaryDao().insertOrUpdateSalary(salary)
    }

    suspend fun insertTransaction(
        merchant: String,
        amount: Double,
        category: String?,
        date: Long,
        paymentMethod: String,
        notes: String,
        isCash: Boolean
    ): Long {
        val aliases = allAliases.first()
        val normalizedMerchant = MerchantNormalizationEngine.normalize(merchant, aliases)
        val determinedCategory = category ?: SmartCategoryEngine.categorize(normalizedMerchant, notes)

        val entity = TransactionEntity(
            merchant = normalizedMerchant,
            amount = amount,
            category = determinedCategory,
            date = date,
            paymentMethod = paymentMethod,
            notes = notes,
            isCash = isCash
        )
        return database.transactionDao().insertTransaction(entity)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        database.transactionDao().updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.transactionDao().deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        database.transactionDao().deleteById(id)
    }

    suspend fun addMerchantAlias(pattern: String, normalizedName: String) {
        database.merchantAliasDao().insertAlias(
            MerchantAliasEntity(pattern = pattern, normalizedName = normalizedName)
        )
    }

    suspend fun addSalaryRecord(amount: Double, date: Long, monthYear: String) {
        // Delete previous salary record for this month to avoid duplicates
        database.salaryRecordDao().deleteSalaryForMonth(monthYear)
        database.salaryRecordDao().insertSalary(
            SalaryRecordEntity(amount = amount, date = date, monthYear = monthYear)
        )
        // Also insert it as a transaction under "Salary" category for visual charts consistency
        insertTransaction(
            merchant = "Monthly Salary Received",
            amount = amount,
            category = "Salary",
            date = date,
            paymentMethod = "Bank Transfer",
            notes = "Salary Record for $monthYear",
            isCash = false
        )
    }

    suspend fun parseAndSaveStatement(rawContent: String): ParseResult {
        val existing = allTransactions.first()
        val result = StatementImportEngine.parseStatement(rawContent, existing)
        
        for (tx in result.parsedTransactions) {
            database.transactionDao().insertTransaction(tx)
        }
        return result
    }

    suspend fun getPreference(key: String): String? {
        return database.appPreferenceDao().getPreference(key)?.value
    }

    suspend fun savePreference(key: String, value: String) {
        database.appPreferenceDao().savePreference(AppPreferenceEntity(key, value))
    }

    suspend fun insertImportHistory(history: ImportHistoryEntity): Long {
        return database.importHistoryDao().insertImportHistory(history)
    }

    suspend fun deleteImportHistoryById(id: Int) {
        database.importHistoryDao().deleteById(id)
    }

    suspend fun clearAllData() {
        database.transactionDao().deleteAllTransactions()
        database.merchantAliasDao().deleteAllAliases()
        database.salaryRecordDao().deleteAllSalaryRecords()
        database.appPreferenceDao().deleteAllPreferences()
        database.salaryDao().deleteSalarySettings()
        database.importHistoryDao().deleteAllImportHistories()
    }
}

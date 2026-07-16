package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isCash = 1 ORDER BY date DESC")
    fun getCashTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Int)
}

@Dao
interface MerchantAliasDao {
    @Query("SELECT * FROM merchant_aliases")
    fun getAllAliases(): Flow<List<MerchantAliasEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: MerchantAliasEntity)

    @Query("DELETE FROM merchant_aliases")
    suspend fun deleteAllAliases()
}

@Dao
interface SalaryRecordDao {
    @Query("SELECT * FROM salary_records ORDER BY date DESC")
    fun getAllSalaryRecords(): Flow<List<SalaryRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalary(salary: SalaryRecordEntity)

    @Query("DELETE FROM salary_records WHERE monthYear = :monthYear")
    suspend fun deleteSalaryForMonth(monthYear: String)

    @Query("DELETE FROM salary_records")
    suspend fun deleteAllSalaryRecords()
}

@Dao
interface AppPreferenceDao {
    @Query("SELECT * FROM app_preferences WHERE `key` = :prefKey")
    suspend fun getPreference(prefKey: String): AppPreferenceEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePreference(preference: AppPreferenceEntity)

    @Query("DELETE FROM app_preferences")
    suspend fun deleteAllPreferences()
}

@Dao
interface SalaryDao {
    @Query("SELECT * FROM salary_settings WHERE id = 1 LIMIT 1")
    fun getSalarySettingsFlow(): Flow<SalaryEntity?>

    @Query("SELECT * FROM salary_settings WHERE id = 1 LIMIT 1")
    suspend fun getSalarySettings(): SalaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSalary(salary: SalaryEntity)

    @Query("DELETE FROM salary_settings")
    suspend fun deleteSalarySettings()
}

@Dao
interface ImportHistoryDao {
    @Query("SELECT * FROM import_histories ORDER BY importDate DESC")
    fun getAllImportHistories(): Flow<List<ImportHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportHistory(history: ImportHistoryEntity): Long

    @Delete
    suspend fun deleteImportHistory(history: ImportHistoryEntity)

    @Query("DELETE FROM import_histories WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM import_histories")
    suspend fun deleteAllImportHistories()
}

@Database(
    entities = [
        TransactionEntity::class,
        MerchantAliasEntity::class,
        SalaryRecordEntity::class,
        AppPreferenceEntity::class,
        SalaryEntity::class,
        ImportHistoryEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun merchantAliasDao(): MerchantAliasDao
    abstract fun salaryRecordDao(): SalaryRecordDao
    abstract fun appPreferenceDao(): AppPreferenceDao
    abstract fun salaryDao(): SalaryDao
    abstract fun importHistoryDao(): ImportHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pocketledger_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

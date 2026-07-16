package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: WalletRepository
    private val salaryUseCase: SalaryUseCase
    
    val salarySettings: StateFlow<SalaryEntity?>

    // --- SMART APP RATING STATE ---
    private val _showRatingPrompt = MutableStateFlow(false)
    val showRatingPrompt: StateFlow<Boolean> = _showRatingPrompt.asStateFlow()

    fun forceShowRatingPrompt() {
        _showRatingPrompt.value = true
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = WalletRepository(database)
        salaryUseCase = SalaryUseCase(repository)
        
        salarySettings = salaryUseCase.salarySettingsFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
        
        // Check if onboarding/first run and set flag
        viewModelScope.launch {
            val hasLaunched = repository.getPreference("has_launched") != null
            if (!hasLaunched) {
                repository.savePreference("has_launched", "true")
            }
            
            // Increment launch count
            val launchesStr = repository.getPreference("launch_count") ?: "0"
            val newLaunches = launchesStr.toInt() + 1
            repository.savePreference("launch_count", newLaunches.toString())
            
            // Set first launch date if missing
            val firstLaunchStr = repository.getPreference("first_launch_date")
            if (firstLaunchStr == null) {
                repository.savePreference("first_launch_date", System.currentTimeMillis().toString())
            }
            
            // Load user preferences
            val savedCurrency = repository.getPreference("pref_currency") ?: "$"
            _currencySymbol.value = savedCurrency
            
            val savedDarkMode = repository.getPreference("pref_dark_mode") ?: "true"
            _darkModeEnabled.value = savedDarkMode == "true"
            
            val completed = repository.getPreference("onboarding_completed") == "true"
            _onboardingCompleted.value = completed

            val isDemo = repository.getPreference("demo_mode_active") == "true"
            _demoModeActive.value = isDemo
            
            val savedLanguage = repository.getPreference("pref_language") ?: "en"
            _appLanguage.value = savedLanguage

            // Load AI preferences
            val aiEnabledStr = repository.getPreference("pref_ai_enabled") ?: "true"
            _isAiEnabled.value = aiEnabledStr == "true"

            val autoDetectEnabledStr = repository.getPreference("pref_auto_detect_enabled") ?: "false"
            _isAutoDetectEnabled.value = autoDetectEnabledStr == "true"

            val consentStateStr = repository.getPreference("pref_auto_detect_consent_state") ?: "not_prompted"
            _consentState.value = consentStateStr

            val premiumStr = repository.getPreference("pref_premium_enabled") ?: "false"
            _isPremiumUser.value = premiumStr == "true"

            val usageCountStr = repository.getPreference("pref_ai_usage_count") ?: "0"
            val usageDateStr = repository.getPreference("pref_ai_usage_date") ?: ""
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

            if (usageDateStr == todayStr) {
                _aiDailyUsageCount.value = usageCountStr.toIntOrNull() ?: 0
                _aiCoachUsageCount.value = (repository.getPreference("pref_ai_coach_count") ?: "0").toIntOrNull() ?: 0
                _aiStoryUsageCount.value = (repository.getPreference("pref_ai_story_count") ?: "0").toIntOrNull() ?: 0
                _aiRoastUsageCount.value = (repository.getPreference("pref_ai_roast_count") ?: "0").toIntOrNull() ?: 0
                _aiAdviceUsageCount.value = (repository.getPreference("pref_ai_advice_count") ?: "0").toIntOrNull() ?: 0
                _importBankUsageCount.value = (repository.getPreference("pref_import_bank_count") ?: "0").toIntOrNull() ?: 0
                _importCsvUsageCount.value = (repository.getPreference("pref_import_csv_count") ?: "0").toIntOrNull() ?: 0
                _importPdfUsageCount.value = (repository.getPreference("pref_import_pdf_count") ?: "0").toIntOrNull() ?: 0
            } else {
                _aiDailyUsageCount.value = 0
                _aiCoachUsageCount.value = 0
                _aiStoryUsageCount.value = 0
                _aiRoastUsageCount.value = 0
                _aiAdviceUsageCount.value = 0
                _importBankUsageCount.value = 0
                _importCsvUsageCount.value = 0
                _importPdfUsageCount.value = 0

                repository.savePreference("pref_ai_usage_count", "0")
                repository.savePreference("pref_ai_coach_count", "0")
                repository.savePreference("pref_ai_story_count", "0")
                repository.savePreference("pref_ai_roast_count", "0")
                repository.savePreference("pref_ai_advice_count", "0")
                repository.savePreference("pref_import_bank_count", "0")
                repository.savePreference("pref_import_csv_count", "0")
                repository.savePreference("pref_import_pdf_count", "0")
                repository.savePreference("pref_ai_usage_date", todayStr)
            }

            val cachedStory = repository.getPreference("cached_ai_story") ?: ""
            _aiStoryState.value = cachedStory

            val cachedRoast = repository.getPreference("cached_ai_roast") ?: ""
            _aiRoastState.value = cachedRoast

            val cachedAdvice = repository.getPreference("cached_ai_advice") ?: ""
            _aiAdviceState.value = cachedAdvice

            val dirtyStr = repository.getPreference("pref_data_dirty") ?: "true"
            _isDataDirty.value = dirtyStr == "true"
            
            // Check if we should trigger rating prompt
            evaluateRatingPrompt()
        }
    }

    // --- REVENUE FLOWS ---
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val detectedBalance: StateFlow<String?> = repository.allTransactions
        .map { _ ->
            repository.getPreference("pref_detected_balance")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val cashTransactions: StateFlow<List<TransactionEntity>> = repository.cashTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val salaryRecords: StateFlow<List<SalaryRecordEntity>> = repository.salaryRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- DERIVED FINANCIAL ANALYTICS ---
    val subscriptionCandidates: StateFlow<List<SubscriptionCandidate>> = allTransactions
        .map { GhostSubscriptionHunter.detectSubscriptions(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val microLeaks: StateFlow<List<MicroLeak>> = allTransactions
        .map { MicroLeakDetector.detectLeaks(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val healthScore: StateFlow<HealthScore> = combine(allTransactions, salaryRecords) { txs, salaries ->
        FinancialHealthScore.calculate(txs, salaries)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HealthScore(70, "B", emptyList(), emptyList()))

    // --- DYNAMIC AI STORY & STATS ---
    private val _aiStoryState = MutableStateFlow<String>("")
    val aiStoryState: StateFlow<String> = _aiStoryState.asStateFlow()

    private val _aiStoryLoading = MutableStateFlow(false)
    val aiStoryLoading: StateFlow<Boolean> = _aiStoryLoading.asStateFlow()

    // --- AI AND PREMIUM SYSTEM FLOWS ---
    private val _isAiEnabled = MutableStateFlow(true)
    val isAiEnabled: StateFlow<Boolean> = _isAiEnabled.asStateFlow()

    private val _isAutoDetectEnabled = MutableStateFlow(false)
    val isAutoDetectEnabled: StateFlow<Boolean> = _isAutoDetectEnabled.asStateFlow()

    private val _consentState = MutableStateFlow("not_prompted")
    val consentState: StateFlow<String> = _consentState.asStateFlow()

    private val _isPremiumUser = MutableStateFlow(false)
    val isPremiumUser: StateFlow<Boolean> = _isPremiumUser.asStateFlow()

    val billingManager = com.example.monetization.BillingManager(application, viewModelScope) { enabled ->
        _isPremiumUser.value = enabled
        viewModelScope.launch {
            repository.savePreference("pref_premium_enabled", enabled.toString())
        }
    }

    private val _aiCoachUsageCount = MutableStateFlow(0)
    val aiCoachUsageCount: StateFlow<Int> = _aiCoachUsageCount.asStateFlow()

    private val _aiStoryUsageCount = MutableStateFlow(0)
    val aiStoryUsageCount: StateFlow<Int> = _aiStoryUsageCount.asStateFlow()

    private val _aiRoastUsageCount = MutableStateFlow(0)
    val aiRoastUsageCount: StateFlow<Int> = _aiRoastUsageCount.asStateFlow()

    private val _aiAdviceUsageCount = MutableStateFlow(0)
    val aiAdviceUsageCount: StateFlow<Int> = _aiAdviceUsageCount.asStateFlow()

    private val _importBankUsageCount = MutableStateFlow(0)
    val importBankUsageCount: StateFlow<Int> = _importBankUsageCount.asStateFlow()

    private val _importCsvUsageCount = MutableStateFlow(0)
    val importCsvUsageCount: StateFlow<Int> = _importCsvUsageCount.asStateFlow()

    private val _importPdfUsageCount = MutableStateFlow(0)
    val importPdfUsageCount: StateFlow<Int> = _importPdfUsageCount.asStateFlow()

    private val _aiDailyUsageCount = MutableStateFlow(0)
    val aiDailyUsageCount: StateFlow<Int> = _aiDailyUsageCount.asStateFlow()

    private val _isDataDirty = MutableStateFlow(true)
    val isDataDirty: StateFlow<Boolean> = _isDataDirty.asStateFlow()

    private val _aiRoastState = MutableStateFlow("")
    val aiRoastState: StateFlow<String> = _aiRoastState.asStateFlow()

    private val _aiAdviceState = MutableStateFlow("")
    val aiAdviceState: StateFlow<String> = _aiAdviceState.asStateFlow()

    private val _aiRoastLoading = MutableStateFlow(false)
    val aiRoastLoading: StateFlow<Boolean> = _aiRoastLoading.asStateFlow()

    private val _aiAdviceLoading = MutableStateFlow(false)
    val aiAdviceLoading: StateFlow<Boolean> = _aiAdviceLoading.asStateFlow()

    private val _aiCoachChatHistory = MutableStateFlow<List<Pair<String, Boolean>>>(emptyList())
    val aiCoachChatHistory: StateFlow<List<Pair<String, Boolean>>> = _aiCoachChatHistory.asStateFlow()

    private val _aiCoachLoading = MutableStateFlow(false)
    val aiCoachLoading: StateFlow<Boolean> = _aiCoachLoading.asStateFlow()

    // --- PREFERENCES & CONFIGURATIONS ---
    private val _currencySymbol = MutableStateFlow("$")
    val currencySymbol: StateFlow<String> = _currencySymbol.asStateFlow()

    private val _darkModeEnabled = MutableStateFlow(true)
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(false)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _demoModeActive = MutableStateFlow(false)
    val demoModeActive: StateFlow<Boolean> = _demoModeActive.asStateFlow()

    private val _appLanguage = MutableStateFlow("en")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    // --- UI TRANSACTION TEMP STATE ---
    private val _importResult = MutableStateFlow<ParseResult?>(null)
    val importResult: StateFlow<ParseResult?> = _importResult.asStateFlow()

    private val _lastImportSummary = MutableStateFlow<ImportSummary?>(null)
    val lastImportSummary: StateFlow<ImportSummary?> = _lastImportSummary.asStateFlow()

    val allImportHistories: StateFlow<List<ImportHistoryEntity>> = repository.allImportHistories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _whatChangedInsight = MutableStateFlow<List<String>>(emptyList())
    val whatChangedInsight: StateFlow<List<String>> = _whatChangedInsight.asStateFlow()

    private val _importLoadingState = MutableStateFlow(false)
    val importLoadingState: StateFlow<Boolean> = _importLoadingState.asStateFlow()

    private val _importErrorMessage = MutableStateFlow<String?>(null)
    val importErrorMessage: StateFlow<String?> = _importErrorMessage.asStateFlow()

    fun clearImportError() {
        _importErrorMessage.value = null
    }



    fun saveSalarySettings(
        userName: String,
        salaryAmount: Double,
        salaryDate: Int,
        currency: String,
        monthlyGoal: Double,
        profilePhotoUri: String? = null
    ) {
        viewModelScope.launch {
            salaryUseCase.saveSalarySettings(
                userName = userName,
                salaryAmount = salaryAmount,
                salaryDate = salaryDate,
                currency = currency,
                monthlyGoal = monthlyGoal,
                profilePhotoUri = profilePhotoUri
            )
            _currencySymbol.value = currency
            
            val cal = Calendar.getInstance()
            val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
            repository.addSalaryRecord(salaryAmount, cal.timeInMillis, currentMonthYear)
            
            repository.savePreference("onboarding_completed", "true")
            _onboardingCompleted.value = true
            
            triggerAiStoryRefresh()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            repository.savePreference("onboarding_completed", "true")
            _onboardingCompleted.value = true
        }
    }

    fun startDemoMode() {
        viewModelScope.launch {
            repository.savePreference("demo_mode_active", "true")
            _demoModeActive.value = true
            prepopulateSampleData()
            repository.savePreference("onboarding_completed", "true")
            _onboardingCompleted.value = true
            triggerAiStoryRefresh()
        }
    }

    fun startFreshMode() {
        viewModelScope.launch {
            repository.savePreference("demo_mode_active", "false")
            _demoModeActive.value = false
            repository.savePreference("onboarding_completed", "true")
            _onboardingCompleted.value = true
            triggerAiStoryRefresh()
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            repository.savePreference("onboarding_completed", "false")
            _onboardingCompleted.value = false
        }
    }

    fun setCurrency(symbol: String) {
        viewModelScope.launch {
            repository.savePreference("pref_currency", symbol)
            _currencySymbol.value = symbol
            
            val existing = repository.getSalarySettings()
            if (existing != null) {
                repository.saveSalarySettings(existing.copy(currency = symbol))
            }
        }
    }

    fun setLanguage(languageCode: String) {
        viewModelScope.launch {
            repository.savePreference("pref_language", languageCode)
            _appLanguage.value = languageCode
        }
    }

    fun toggleTheme() {
        viewModelScope.launch {
            val current = _darkModeEnabled.value
            _darkModeEnabled.value = !current
            repository.savePreference("pref_dark_mode", (!current).toString())
        }
    }

    // --- ACTIONS ---
    fun addCashTransaction(
        amount: Double,
        merchant: String,
        category: String,
        date: Long,
        paymentMethod: String,
        notes: String,
        isCash: Boolean
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                merchant = merchant,
                amount = amount,
                category = category,
                date = date,
                paymentMethod = paymentMethod,
                notes = notes,
                isCash = isCash
            )
            incrementMeaningfulActions()
            triggerAiStoryRefresh()
        }
    }

    fun deleteTransaction(id: Int) {
        viewModelScope.launch {
            repository.deleteTransactionById(id)
            triggerAiStoryRefresh()
        }
    }

    fun addSalary(amount: Double, date: Long, monthYear: String) {
        viewModelScope.launch {
            repository.addSalaryRecord(amount, date, monthYear)
            triggerAiStoryRefresh()
        }
    }

    fun importStatementData(rawText: String) {
        viewModelScope.launch {
            val result = repository.parseAndSaveStatement(rawText)
            _importResult.value = result
            incrementMeaningfulActions()
            triggerAiStoryRefresh()
        }
    }

    fun parseStatementFile(context: android.content.Context, uri: android.net.Uri, fileName: String) {
        viewModelScope.launch {
            _importLoadingState.value = true
            _importErrorMessage.value = null
            _importResult.value = null
            _whatChangedInsight.value = emptyList()

            try {
                val extension = fileName.substringAfterLast('.', "").lowercase()
                val isPremium = _isPremiumUser.value
                if (!isPremium) {
                    if (extension == "pdf" && !canImportPdf()) {
                        _importErrorMessage.value = "LIMIT_REACHED:You've used today's free limit for PDF imports. Upgrade to PocketLedger Premium for unlimited AI insights and unlimited imports."
                        _importLoadingState.value = false
                        return@launch
                    } else if (extension == "csv" && !canImportCsv()) {
                        _importErrorMessage.value = "LIMIT_REACHED:You've used today's free limit for CSV imports. Upgrade to PocketLedger Premium for unlimited AI insights and unlimited imports."
                        _importLoadingState.value = false
                        return@launch
                    } else if (extension != "pdf" && extension != "csv" && !canImportBank()) {
                        _importErrorMessage.value = "LIMIT_REACHED:You've used today's free limit for bank statement imports. Upgrade to PocketLedger Premium for unlimited AI insights and unlimited imports."
                        _importLoadingState.value = false
                        return@launch
                    }
                }

                val contentResolver = context.contentResolver
                
                // File check validations (corruption, size limits)
                val pfd = try {
                    contentResolver.openFileDescriptor(uri, "r")
                } catch (e: Exception) {
                    null
                }
                
                val fileSize = pfd?.statSize ?: 0L
                pfd?.close()

                if (fileSize == 0L && extension != "txt" && extension != "csv") {
                    _importErrorMessage.value = "Selected file is empty or inaccessible."
                    _importLoadingState.value = false
                    return@launch
                }
                if (fileSize > 10 * 1024 * 1024) { // 10MB Limit
                    _importErrorMessage.value = "Selected file exceeds maximum 10MB size limit."
                    _importLoadingState.value = false
                    return@launch
                }

                val textContent = contentResolver.openInputStream(uri)?.use { stream ->
                    when (extension) {
                        "xlsx" -> StatementImportEngine.extractTextFromXlsx(stream)
                        "pdf" -> StatementImportEngine.extractTextFromSearchablePdf(stream)
                        else -> { // csv, txt, general
                            stream.bufferedReader().use { it.readText() }
                        }
                    }
                }

                if (textContent.isNullOrBlank()) {
                    _importErrorMessage.value = "No readable transaction lines or text could be extracted from this statement."
                    _importLoadingState.value = false
                    return@launch
                }

                val existing = repository.allTransactions.first()
                val bankName = StatementImportEngine.detectBank(textContent, fileName)
                val result = StatementImportEngine.parseStatement(textContent, existing, bankName)

                if (result.parsedTransactions.isEmpty()) {
                    _importErrorMessage.value = "Parsed zero new transactions. (Found ${result.duplicatesFound} duplicate transactions)."
                }

                _importResult.value = result
                calculateWhatChanged(result.parsedTransactions, existing)

            } catch (e: Exception) {
                _importErrorMessage.value = "Import failed: ${e.localizedMessage ?: "Invalid or corrupted file format."}"
            } finally {
                _importLoadingState.value = false
            }
        }
    }

    fun autoImportStatementFile(context: android.content.Context, uri: android.net.Uri, fileName: String) {
        viewModelScope.launch {
            _importLoadingState.value = true
            _importErrorMessage.value = null
            _importResult.value = null
            _lastImportSummary.value = null

            try {
                val extension = fileName.substringAfterLast('.', "").lowercase()
                val isPremium = _isPremiumUser.value
                if (!isPremium) {
                    if (extension == "pdf" && !canImportPdf()) {
                        _importErrorMessage.value = "LIMIT_REACHED:You've used today's free limit for PDF imports. Upgrade to PocketLedger Premium for unlimited AI insights and unlimited imports."
                        _importLoadingState.value = false
                        return@launch
                    } else if (extension == "csv" && !canImportCsv()) {
                        _importErrorMessage.value = "LIMIT_REACHED:You've used today's free limit for CSV imports. Upgrade to PocketLedger Premium for unlimited AI insights and unlimited imports."
                        _importLoadingState.value = false
                        return@launch
                    } else if (extension != "pdf" && extension != "csv" && !canImportBank()) {
                        _importErrorMessage.value = "LIMIT_REACHED:You've used today's free limit for bank statement imports. Upgrade to PocketLedger Premium for unlimited AI insights and unlimited imports."
                        _importLoadingState.value = false
                        return@launch
                    }
                }

                val contentResolver = context.contentResolver
                
                val pfd = try {
                    contentResolver.openFileDescriptor(uri, "r")
                } catch (e: Exception) {
                    null
                }
                val fileSize = pfd?.statSize ?: 0L
                pfd?.close()

                if (fileSize == 0L && extension != "txt" && extension != "csv") {
                    _importErrorMessage.value = "Selected file is empty or inaccessible."
                    _importLoadingState.value = false
                    return@launch
                }

                val textContent = contentResolver.openInputStream(uri)?.use { stream ->
                    when (extension) {
                        "xlsx" -> StatementImportEngine.extractTextFromXlsx(stream)
                        "pdf" -> StatementImportEngine.extractTextFromSearchablePdf(stream)
                        else -> {
                            stream.bufferedReader().use { it.readText() }
                        }
                    }
                }

                if (textContent.isNullOrBlank()) {
                    _importErrorMessage.value = "No readable transaction lines or text could be extracted from this statement."
                    _importLoadingState.value = false
                    return@launch
                }

                val existing = repository.allTransactions.first()
                val bankName = StatementImportEngine.detectBank(textContent, fileName)
                val result = StatementImportEngine.parseStatement(textContent, existing, bankName)

                if (result.parsedTransactions.isEmpty()) {
                    _importErrorMessage.value = "All transactions inside this statement were already imported as duplicates."
                    _importLoadingState.value = false
                    return@launch
                }

                val insertedIds = mutableListOf<Long>()
                for (tx in result.parsedTransactions) {
                    val id = repository.insertTransaction(
                        merchant = tx.merchant,
                        amount = tx.amount,
                        category = tx.category,
                        date = tx.date,
                        paymentMethod = tx.paymentMethod,
                        notes = tx.notes,
                        isCash = tx.isCash
                    )
                    insertedIds.add(id)
                }

                val history = ImportHistoryEntity(
                    fileName = fileName,
                    importDate = System.currentTimeMillis(),
                    transactionCount = result.parsedTransactions.size,
                    bankName = bankName,
                    status = "Success",
                    importedTxIds = insertedIds.joinToString(",")
                )
                repository.insertImportHistory(history)

                val income = result.parsedTransactions.filter { it.category in listOf("Salary", "Transfers") }.sumOf { it.amount }
                val expenses = result.parsedTransactions.filter { it.category !in listOf("Salary", "Transfers") }.sumOf { it.amount }
                val savings = maxOf(0.0, income - expenses)
                
                val subsCount = maxOf(1, GhostSubscriptionHunter.detectSubscriptions(result.parsedTransactions).size)
                val leaksCount = maxOf(2, MicroLeakDetector.detectLeaks(result.parsedTransactions).size)
                val score = if (income > 0) {
                    val ratio = savings / income
                    minOf(99, maxOf(45, (60 + ratio * 40).toInt()))
                } else {
                    88
                }

                _lastImportSummary.value = ImportSummary(
                    fileName = fileName,
                    transactionCount = result.parsedTransactions.size,
                    income = if (income > 0) income else (expenses * 1.5),
                    expenses = expenses,
                    savings = if (income > 0) savings else (expenses * 0.5),
                    subscriptionsCount = subsCount,
                    microLeaksCount = leaksCount,
                    financialScore = score
                )

                _importResult.value = result
                incrementMeaningfulActions()
                triggerAiStoryRefresh()

                if (extension == "pdf") {
                    incrementImportPdfUsage()
                } else if (extension == "csv") {
                    incrementImportCsvUsage()
                } else {
                    incrementImportBankUsage()
                }

            } catch (e: Exception) {
                _importErrorMessage.value = "Import failed: ${e.localizedMessage ?: "Invalid or corrupted file format."}"
            } finally {
                _importLoadingState.value = false
            }
        }
    }

    fun clearLastImportSummary() {
        _lastImportSummary.value = null
    }

    fun confirmSelectedImport(
        fileName: String,
        bankName: String,
        selectedTransactions: List<TransactionEntity>
    ) {
        viewModelScope.launch {
            if (selectedTransactions.isEmpty()) return@launch
            
            val insertedIds = mutableListOf<Long>()
            for (tx in selectedTransactions) {
                val id = repository.insertTransaction(
                    merchant = tx.merchant,
                    amount = tx.amount,
                    category = tx.category,
                    date = tx.date,
                    paymentMethod = tx.paymentMethod,
                    notes = tx.notes,
                    isCash = tx.isCash
                )
                insertedIds.add(id)
            }

            // Create and save import history
            val history = ImportHistoryEntity(
                fileName = fileName,
                importDate = System.currentTimeMillis(),
                transactionCount = selectedTransactions.size,
                bankName = bankName,
                status = "Success",
                importedTxIds = insertedIds.joinToString(",")
            )
            repository.insertImportHistory(history)
            
            // Clear current preview state
            _importResult.value = null
            
            incrementMeaningfulActions()
            triggerAiStoryRefresh()

            val extension = fileName.substringAfterLast('.', "").lowercase()
            if (extension == "pdf") {
                incrementImportPdfUsage()
            } else if (extension == "csv") {
                incrementImportCsvUsage()
            } else {
                incrementImportBankUsage()
            }
        }
    }

    fun deleteImportHistory(history: ImportHistoryEntity) {
        viewModelScope.launch {
            repository.deleteImportHistoryById(history.id)
            if (history.importedTxIds.isNotEmpty()) {
                val ids = history.importedTxIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                for (id in ids) {
                    repository.deleteTransactionById(id.toInt())
                }
            }
            triggerAiStoryRefresh()
        }
    }

    fun calculateWhatChanged(newTxs: List<TransactionEntity>, existingTxs: List<TransactionEntity>) {
        val insights = mutableListOf<String>()
        if (newTxs.isEmpty() || existingTxs.isEmpty()) {
            _whatChangedInsight.value = listOf("Not enough historical data to compare changes yet.")
            return
        }
        
        val newByCat = newTxs.groupBy { it.category }.mapValues { it.value.sumOf { it.amount } }
        val oldByCat = existingTxs.groupBy { it.category }.mapValues { it.value.sumOf { it.amount } }
        
        // 1. Food spending comparison
        val newFood = newByCat["Food"] ?: 0.0
        val oldFood = oldByCat["Food"] ?: 0.0
        if (newFood > 0 && oldFood > 0) {
            val diff = newFood - oldFood
            if (diff > 0) {
                insights.add("📈 Food spending increased by ${formatIndianCurrency(diff, currencySymbol.value)} compared to history.")
            } else if (diff < 0) {
                insights.add("📉 Food spending decreased by ${formatIndianCurrency(kotlin.math.abs(diff), currencySymbol.value)} compared to history.")
            }
        }
        
        // 2. Travel spending comparison
        val newTravel = newByCat["Travel"] ?: 0.0
        val oldTravel = oldByCat["Travel"] ?: 0.0
        if (newTravel > 0 && oldTravel > 0) {
            val diff = newTravel - oldTravel
            if (diff > 0) {
                insights.add("🚕 Travel spending increased by ${formatIndianCurrency(diff, currencySymbol.value)}.")
            } else if (diff < 0) {
                insights.add("🚕 Travel spending decreased by ${formatIndianCurrency(kotlin.math.abs(diff), currencySymbol.value)}.")
            }
        }
        
        // 3. Savings / Net cash comparison
        val newIncome = newTxs.filter { it.category == "Salary" }.sumOf { it.amount }
        val newExpense = newTxs.filter { it.category != "Salary" }.sumOf { it.amount }
        val newSavings = (newIncome - newExpense).coerceAtLeast(0.0)
        
        val oldIncome = existingTxs.filter { it.category == "Salary" }.sumOf { it.amount }
        val oldExpense = existingTxs.filter { it.category != "Salary" }.sumOf { it.amount }
        val oldSavings = (oldIncome - oldExpense).coerceAtLeast(0.0)
        
        if (newSavings > oldSavings) {
            insights.add("💰 Net savings cycle improved by ${formatIndianCurrency(newSavings - oldSavings, currencySymbol.value)}!")
        } else if (newSavings < oldSavings && oldSavings > 0) {
            insights.add("⚠️ Net savings cycle decreased by ${formatIndianCurrency(oldSavings - newSavings, currencySymbol.value)}.")
        }
        
        // 4. Subscriptions comparison
        val oldSubs = GhostSubscriptionHunter.detectSubscriptions(existingTxs).map { it.merchant.lowercase() }.toSet()
        val newSubs = GhostSubscriptionHunter.detectSubscriptions(existingTxs + newTxs)
        val brandNewSubs = newSubs.filter { it.merchant.lowercase() !in oldSubs }
        if (brandNewSubs.isNotEmpty()) {
            insights.add("👻 A new recurring subscription was detected for '${brandNewSubs.first().merchant}' (${brandNewSubs.first().interval}).")
        }
        
        if (insights.isEmpty()) {
            insights.add("✅ Spending patterns are stable. Keep up the disciplined budgeting!")
        }
        _whatChangedInsight.value = insights
    }

    fun clearImportResult() {
        _importResult.value = null
    }

    fun buildFinancialSummary(): String {
        val transactionsList = allTransactions.value
        val salariesList = salaryRecords.value
        val score = healthScore.value.score
        val activeSalary = salarySettings.value?.salaryAmount ?: 0.0
        val displayCurrency = salarySettings.value?.currency ?: currencySymbol.value

        val cal = Calendar.getInstance()
        val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)
        val currentMonthSalary = if (activeSalary > 0.0) activeSalary else (salariesList.firstOrNull { it.monthYear == currentMonthYear }?.amount ?: 3000.0)

        val nonSalaryTxsThisMonth = transactionsList.filter {
            val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
            txCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH) &&
                    txCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                    it.category != "Salary"
        }

        val totalSpentThisMonth = nonSalaryTxsThisMonth.sumOf { it.amount }
        val moneyRemaining = (currentMonthSalary - totalSpentThisMonth).coerceAtLeast(0.0)

        val categories = nonSalaryTxsThisMonth.filter { it.category != "Salary" }
            .groupBy { it.category }
            .mapValues { it.value.sumOf { tx -> tx.amount } }
            .entries.sortedByDescending { it.value }
            .take(3)
            .joinToString(", ") { "${it.key}: $displayCurrency${String.format("%.0f", it.value)}" }

        val topMerchant = nonSalaryTxsThisMonth.filter { it.category != "Salary" }
            .groupBy { it.merchant }
            .maxByOrNull { it.value.size }?.key ?: "None"

        val subs = subscriptionCandidates.value.take(2).joinToString(", ") { "${it.merchant}: $displayCurrency${String.format("%.0f", it.amount)}" }
        val leaksList = microLeaks.value.take(2).joinToString(", ") { "${it.merchant}: $displayCurrency${String.format("%.0f", it.monthlyTotal)}" }

        return """
            Salary: $displayCurrency${String.format("%.0f", currentMonthSalary)}
            Spent: $displayCurrency${String.format("%.0f", totalSpentThisMonth)}
            Remaining: $displayCurrency${String.format("%.0f", moneyRemaining)}
            Categories: $categories
            Top Merchant: $topMerchant
            Subscriptions: $subs
            Leaks: $leaksList
            Score: $score
        """.trimIndent()
    }

    fun canUseAiCoach(): Boolean {
        if (!_isAiEnabled.value) return false
        if (_isPremiumUser.value) return true
        return _aiCoachUsageCount.value < 2
    }

    fun canUseAiStory(): Boolean {
        if (!_isAiEnabled.value) return false
        if (_isPremiumUser.value) return true
        return _aiStoryUsageCount.value < 2
    }

    fun canUseAiRoast(): Boolean {
        if (!_isAiEnabled.value) return false
        if (_isPremiumUser.value) return true
        return _aiRoastUsageCount.value < 2
    }

    fun canUseAiAdvice(): Boolean {
        if (!_isAiEnabled.value) return false
        if (_isPremiumUser.value) return true
        return _aiAdviceUsageCount.value < 2
    }

    fun canImportBank(): Boolean {
        if (_isPremiumUser.value) return true
        return _importBankUsageCount.value < 2
    }

    fun canImportCsv(): Boolean {
        if (_isPremiumUser.value) return true
        return _importCsvUsageCount.value < 2
    }

    fun canImportPdf(): Boolean {
        if (_isPremiumUser.value) return true
        return _importPdfUsageCount.value < 2
    }

    private suspend fun incrementAiCoachUsage() {
        if (_isPremiumUser.value) return
        val newCount = _aiCoachUsageCount.value + 1
        _aiCoachUsageCount.value = newCount
        repository.savePreference("pref_ai_coach_count", newCount.toString())
        updateUsageDate()
    }

    private suspend fun incrementAiStoryUsage() {
        if (_isPremiumUser.value) return
        val newCount = _aiStoryUsageCount.value + 1
        _aiStoryUsageCount.value = newCount
        repository.savePreference("pref_ai_story_count", newCount.toString())
        updateUsageDate()
    }

    private suspend fun incrementAiRoastUsage() {
        if (_isPremiumUser.value) return
        val newCount = _aiRoastUsageCount.value + 1
        _aiRoastUsageCount.value = newCount
        repository.savePreference("pref_ai_roast_count", newCount.toString())
        updateUsageDate()
    }

    private suspend fun incrementAiAdviceUsage() {
        if (_isPremiumUser.value) return
        val newCount = _aiAdviceUsageCount.value + 1
        _aiAdviceUsageCount.value = newCount
        repository.savePreference("pref_ai_advice_count", newCount.toString())
        updateUsageDate()
    }

    suspend fun incrementImportBankUsage() {
        if (_isPremiumUser.value) return
        val newCount = _importBankUsageCount.value + 1
        _importBankUsageCount.value = newCount
        repository.savePreference("pref_import_bank_count", newCount.toString())
        updateUsageDate()
    }

    suspend fun incrementImportCsvUsage() {
        if (_isPremiumUser.value) return
        val newCount = _importCsvUsageCount.value + 1
        _importCsvUsageCount.value = newCount
        repository.savePreference("pref_import_csv_count", newCount.toString())
        updateUsageDate()
    }

    suspend fun incrementImportPdfUsage() {
        if (_isPremiumUser.value) return
        val newCount = _importPdfUsageCount.value + 1
        _importPdfUsageCount.value = newCount
        repository.savePreference("pref_import_pdf_count", newCount.toString())
        updateUsageDate()
    }

    private suspend fun updateUsageDate() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        repository.savePreference("pref_ai_usage_date", todayStr)
    }

    fun generateAiStory(force: Boolean = false) {
        if (allTransactions.value.isEmpty()) {
            _aiStoryState.value = "Import a bank statement or add your first transaction."
            return
        }
        if (!canUseAiStory()) {
            _aiStoryState.value = "LIMIT_REACHED:You've used today's free limit. Upgrade to PocketLedger Premium for unlimited AI insights and unlimited imports."
            return
        }
        viewModelScope.launch {
            _aiStoryLoading.value = true
            try {
                if (!force) {
                    val cached = repository.getPreference("cached_ai_story") ?: ""
                    val dirty = repository.getPreference("pref_data_dirty") ?: "true"
                    if (cached.isNotEmpty() && dirty == "false") {
                        _aiStoryState.value = cached
                        return@launch
                    }
                }

                val summary = buildFinancialSummary()
                val displayCurrency = salarySettings.value?.currency ?: currencySymbol.value
                val story = GeminiService.generateFinancialStory(summary, displayCurrency)
                _aiStoryState.value = story
                repository.savePreference("cached_ai_story", story)
                repository.savePreference("pref_data_dirty", "false")
                _isDataDirty.value = false
                incrementAiStoryUsage()
            } catch (e: Exception) {
                _aiStoryState.value = "Failed to create story: ${e.localizedMessage}"
            } finally {
                _aiStoryLoading.value = false
            }
        }
    }

    fun generateSpendingRoast(force: Boolean = false) {
        if (!canUseAiRoast()) {
            _aiRoastState.value = "LIMIT_REACHED:You've used today's free limit. Upgrade to PocketLedger Premium for unlimited AI insights and unlimited imports."
            return
        }
        viewModelScope.launch {
            _aiRoastLoading.value = true
            try {
                if (!force) {
                    val cached = repository.getPreference("cached_ai_roast") ?: ""
                    val dirty = repository.getPreference("pref_data_dirty") ?: "true"
                    if (cached.isNotEmpty() && dirty == "false") {
                        _aiRoastState.value = cached
                        return@launch
                    }
                }

                val summary = buildFinancialSummary()
                val displayCurrency = salarySettings.value?.currency ?: currencySymbol.value
                val roast = GeminiService.generateSpendingRoast(summary, displayCurrency)
                _aiRoastState.value = roast
                repository.savePreference("cached_ai_roast", roast)
                repository.savePreference("pref_data_dirty", "false")
                _isDataDirty.value = false
                incrementAiRoastUsage()
            } catch (e: Exception) {
                _aiRoastState.value = "Failed to roast spending: ${e.localizedMessage}"
            } finally {
                _aiRoastLoading.value = false
            }
        }
    }

    fun generateSavingsAdvice(force: Boolean = false) {
        if (!canUseAiAdvice()) {
            _aiAdviceState.value = "LIMIT_REACHED:You've used today's free limit. Upgrade to PocketLedger Premium for unlimited AI insights and unlimited imports."
            return
        }
        viewModelScope.launch {
            _aiAdviceLoading.value = true
            try {
                if (!force) {
                    val cached = repository.getPreference("cached_ai_advice") ?: ""
                    val dirty = repository.getPreference("pref_data_dirty") ?: "true"
                    if (cached.isNotEmpty() && dirty == "false") {
                        _aiAdviceState.value = cached
                        return@launch
                    }
                }

                val summary = buildFinancialSummary()
                val displayCurrency = salarySettings.value?.currency ?: currencySymbol.value
                val advice = GeminiService.generateSavingsAdvice(summary, displayCurrency)
                _aiAdviceState.value = advice
                repository.savePreference("cached_ai_advice", advice)
                repository.savePreference("pref_data_dirty", "false")
                _isDataDirty.value = false
                incrementAiAdviceUsage()
            } catch (e: Exception) {
                _aiAdviceState.value = "Failed to generate savings advice: ${e.localizedMessage}"
            } finally {
                _aiAdviceLoading.value = false
            }
        }
    }

    fun askCoachQuestion(question: String) {
        if (question.isBlank()) return
        if (!canUseAiCoach()) {
            viewModelScope.launch {
                val currentHistory = _aiCoachChatHistory.value.toMutableList()
                currentHistory.add(Pair(question, true))
                currentHistory.add(Pair("LIMIT_REACHED:You've used today's free limit. Upgrade to PocketLedger Premium for unlimited AI insights and unlimited imports.", false))
                _aiCoachChatHistory.value = currentHistory
            }
            return
        }
        viewModelScope.launch {
            _aiCoachLoading.value = true
            val currentHistory = _aiCoachChatHistory.value.toMutableList()
            currentHistory.add(Pair(question, true))
            _aiCoachChatHistory.value = currentHistory

            try {
                val summary = buildFinancialSummary()
                val displayCurrency = salarySettings.value?.currency ?: currencySymbol.value
                val answer = GeminiService.askFinancialCoach(summary, currentHistory, question, displayCurrency)
                val updatedHistory = _aiCoachChatHistory.value.toMutableList()
                updatedHistory.add(Pair(answer, false))
                if (updatedHistory.size > 12) {
                    _aiCoachChatHistory.value = updatedHistory.takeLast(12)
                } else {
                    _aiCoachChatHistory.value = updatedHistory
                }
                incrementAiCoachUsage()
            } catch (e: Exception) {
                val updatedHistory = _aiCoachChatHistory.value.toMutableList()
                updatedHistory.add(Pair("PocketLedger AI Coach offline error: ${e.localizedMessage}", false))
                _aiCoachChatHistory.value = updatedHistory
            } finally {
                _aiCoachLoading.value = false
            }
        }
    }

    fun clearCoachChat() {
        _aiCoachChatHistory.value = emptyList()
    }

    fun toggleAiEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isAiEnabled.value = enabled
            repository.savePreference("pref_ai_enabled", enabled.toString())
        }
    }

    fun toggleAutoDetectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _isAutoDetectEnabled.value = enabled
            repository.savePreference("pref_auto_detect_enabled", enabled.toString())
        }
    }

    fun setConsentState(state: String) {
        viewModelScope.launch {
            _consentState.value = state
            repository.savePreference("pref_auto_detect_consent_state", state)
            if (state == "enabled") {
                toggleAutoDetectEnabled(true)
            } else if (state == "skipped") {
                toggleAutoDetectEnabled(false)
            }
        }
    }

    fun buyPremium(activity: android.app.Activity, productId: String) {
        billingManager.launchBillingFlow(activity, productId)
    }

    fun restorePurchases() {
        billingManager.restorePurchases()
    }

    fun grantRewardedAiRequest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _aiCoachUsageCount.value = (_aiCoachUsageCount.value - 1).coerceAtLeast(0)
            _aiStoryUsageCount.value = (_aiStoryUsageCount.value - 1).coerceAtLeast(0)
            _aiRoastUsageCount.value = (_aiRoastUsageCount.value - 1).coerceAtLeast(0)
            _aiAdviceUsageCount.value = (_aiAdviceUsageCount.value - 1).coerceAtLeast(0)
            
            repository.savePreference("pref_ai_coach_count", _aiCoachUsageCount.value.toString())
            repository.savePreference("pref_ai_story_count", _aiStoryUsageCount.value.toString())
            repository.savePreference("pref_ai_roast_count", _aiRoastUsageCount.value.toString())
            repository.savePreference("pref_ai_advice_count", _aiAdviceUsageCount.value.toString())
            
            onSuccess()
        }
    }

    fun grantRewardedImport(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _importBankUsageCount.value = (_importBankUsageCount.value - 1).coerceAtLeast(0)
            _importCsvUsageCount.value = (_importCsvUsageCount.value - 1).coerceAtLeast(0)
            _importPdfUsageCount.value = (_importPdfUsageCount.value - 1).coerceAtLeast(0)
            
            repository.savePreference("pref_import_bank_count", _importBankUsageCount.value.toString())
            repository.savePreference("pref_import_csv_count", _importCsvUsageCount.value.toString())
            repository.savePreference("pref_import_pdf_count", _importPdfUsageCount.value.toString())
            
            onSuccess()
        }
    }

    fun clearAiCache() {
        viewModelScope.launch {
            _aiStoryState.value = ""
            _aiRoastState.value = ""
            _aiAdviceState.value = ""
            _aiCoachChatHistory.value = emptyList()
            repository.savePreference("cached_ai_story", "")
            repository.savePreference("cached_ai_roast", "")
            repository.savePreference("cached_ai_advice", "")
            repository.savePreference("pref_data_dirty", "true")
            _isDataDirty.value = true
        }
    }

    private fun triggerAiStoryRefresh() {
        viewModelScope.launch {
            repository.savePreference("pref_data_dirty", "true")
            _isDataDirty.value = true
            _aiStoryState.value = ""
            _aiRoastState.value = ""
            _aiAdviceState.value = ""
        }
    }

    suspend fun clearAllData() {
        repository.clearAllData()
        _aiStoryState.value = ""
        repository.savePreference("demo_mode_active", "false")
        _demoModeActive.value = false
        repository.savePreference("onboarding_completed", "false")
        _onboardingCompleted.value = false
    }

    private suspend fun prepopulateSampleData() {
        val cal = Calendar.getInstance()
        val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)

        // 1. Add salary record
        repository.addSalaryRecord(4200.0, cal.timeInMillis - (12 * 24 * 60 * 60 * 1000L), currentMonthYear)

        // 2. Add some mock transactions to showcase leaks and subscriptions
        val mockData = listOf(
            // Subscriptions (repeated Netflix & Spotify)
            Triple("Netflix India", 14.99, "Entertainment"),
            Triple("Spotify Music", 9.99, "Entertainment"),
            Triple("Netflix.com", 14.99, "Entertainment"),
            Triple("Spotify US", 9.99, "Entertainment"),
            
            // Micro leaks (repeated Starbucks coffee & Uber)
            Triple("Starbucks Coffee", 5.50, "Food"),
            Triple("Starbucks Coffee", 6.20, "Food"),
            Triple("Starbucks", 5.80, "Food"),
            Triple("Starbucks Coffee", 5.50, "Food"),
            
            Triple("Uber Trip", 12.50, "Travel"),
            Triple("Uber Trip", 14.20, "Travel"),
            Triple("Uber Ride", 11.80, "Travel"),
            
            // Large single expenses
            Triple("Walmart Supercenter", 184.50, "Shopping"),
            Triple("Whole Foods", 142.30, "Food"),
            Triple("Metropolitan Power", 115.00, "Bills"),
            Triple("AT&T Mobile", 75.00, "Bills"),
            Triple("Aetna Medical Care", 45.00, "Medical"),
            Triple("Vanguard Index Fund", 250.00, "Investment")
        )

        // Stagger these over the last 10 days
        for ((index, item) in mockData.withIndex()) {
            val daysAgo = (index % 10) + 1
            val txDate = cal.timeInMillis - (daysAgo * 24 * 60 * 60 * 1000L)
            
            repository.insertTransaction(
                merchant = item.first,
                amount = item.second,
                category = item.third,
                date = txDate,
                paymentMethod = if (item.third == "Food" && index % 3 == 0) "Cash" else "Card",
                notes = "Auto-generated sample transaction",
                isCash = item.third == "Food" && index % 3 == 0
            )
        }
    }

    // --- SMART APP RATING METHODS ---
    fun evaluateRatingPrompt() {
        viewModelScope.launch {
            val hasRated = repository.getPreference("has_rated_app") == "true"
            if (hasRated) {
                _showRatingPrompt.value = false
                return@launch
            }
            
            val lastPromptDateStr = repository.getPreference("last_prompt_date")
            if (lastPromptDateStr != null) {
                val lastPromptDate = lastPromptDateStr.toLongOrNull() ?: 0L
                val daysSinceLastPrompt = (System.currentTimeMillis() - lastPromptDate) / (1000 * 60 * 60 * 24)
                if (daysSinceLastPrompt < 30) {
                    _showRatingPrompt.value = false
                    return@launch
                }
            }
            
            val firstLaunchDateStr = repository.getPreference("first_launch_date")
            val firstLaunchDate = firstLaunchDateStr?.toLongOrNull() ?: System.currentTimeMillis()
            val daysUsed = (System.currentTimeMillis() - firstLaunchDate) / (1000 * 60 * 60 * 24)
            val meetsDays = daysUsed >= 7
            
            val launchesStr = repository.getPreference("launch_count") ?: "0"
            val launchCount = launchesStr.toInt()
            val meetsLaunches = launchCount >= 5
            
            val actionsStr = repository.getPreference("meaningful_actions_count") ?: "0"
            val actionsCount = actionsStr.toInt()
            val meetsActions = actionsCount >= 1
            
            val shouldShow = (meetsDays || meetsLaunches) && meetsActions
            _showRatingPrompt.value = shouldShow
        }
    }

    fun onUserRated(stars: Int) {
        viewModelScope.launch {
            repository.savePreference("has_rated_app", "true")
            _showRatingPrompt.value = false
        }
    }

    fun onUserDismissedRating(maybeLater: Boolean) {
        viewModelScope.launch {
            if (maybeLater) {
                repository.savePreference("last_prompt_date", System.currentTimeMillis().toString())
            } else {
                repository.savePreference("has_rated_app", "true") // Treat as never show again
            }
            _showRatingPrompt.value = false
        }
    }

    fun incrementMeaningfulActions() {
        viewModelScope.launch {
            val countStr = repository.getPreference("meaningful_actions_count") ?: "0"
            val newCount = countStr.toInt() + 1
            repository.savePreference("meaningful_actions_count", newCount.toString())
            evaluateRatingPrompt()
        }
    }

    // --- DATA UTILITIES: EXPORT & BACKUP ---
    fun exportLedgerData(context: android.content.Context) {
        viewModelScope.launch {
            val transactionsList = allTransactions.value
            val csvBuilder = java.lang.StringBuilder("ID,Merchant,Amount,Category,Date,PaymentMethod,Notes,IsCash\n")
            for (tx in transactionsList) {
                val cleanedMerchant = tx.merchant.replace("\"", "\"\"")
                val cleanedNotes = tx.notes.replace("\"", "\"\"")
                val cleanedCategory = tx.category.replace("\"", "\"\"")
                csvBuilder.append("${tx.id},\"${cleanedMerchant}\",${tx.amount},\"${cleanedCategory}\",${tx.date},\"${tx.paymentMethod}\",\"${cleanedNotes}\",${tx.isCash}\n")
            }
            val sendIntent: android.content.Intent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, csvBuilder.toString())
                type = "text/csv"
            }
            val shareIntent = android.content.Intent.createChooser(sendIntent, "Export Ledger CSV")
            shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        }
    }

    fun backupData(context: android.content.Context) {
        viewModelScope.launch {
            val txs = allTransactions.value
            val srs = salaryRecords.value
            val sSet = salarySettings.value
            
            val backupBuilder = java.lang.StringBuilder()
            backupBuilder.append("POCKETLEDGER_BACKUP_V1\n")
            backupBuilder.append("USER_NAME:${sSet?.userName ?: ""}\n")
            backupBuilder.append("SALARY_AMOUNT:${sSet?.salaryAmount ?: 0.0}\n")
            backupBuilder.append("SALARY_DATE:${sSet?.salaryDate ?: 1}\n")
            backupBuilder.append("CURRENCY:${sSet?.currency ?: ""}\n")
            backupBuilder.append("GOAL:${sSet?.monthlyGoal ?: 0.0}\n")
            
            backupBuilder.append("--- TRANSACTIONS ---\n")
            for (tx in txs) {
                backupBuilder.append("${tx.merchant}|${tx.amount}|${tx.category}|${tx.date}|${tx.paymentMethod}|${tx.notes}|${tx.isCash}\n")
            }
            
            val sendIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                putExtra(android.content.Intent.EXTRA_TEXT, backupBuilder.toString())
                type = "text/plain"
            }
            val shareIntent = android.content.Intent.createChooser(sendIntent, "PocketLedger Backup Data")
            shareIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        }
    }

    fun restoreData(context: android.content.Context, backupText: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                if (!backupText.startsWith("POCKETLEDGER_BACKUP_V1")) {
                    android.widget.Toast.makeText(context, "Invalid backup format", android.widget.Toast.LENGTH_SHORT).show()
                    return@launch
                }
                
                repository.clearAllData()
                val lines = backupText.split("\n")
                
                var userName = ""
                var salaryAmount = 0.0
                var salaryDate = 1
                var currency = "$"
                var goal = 0.0
                
                var parsingTransactions = false
                
                for (line in lines) {
                    if (line.isBlank()) continue
                    if (line.startsWith("USER_NAME:")) {
                        userName = line.substringAfter("USER_NAME:")
                    } else if (line.startsWith("SALARY_AMOUNT:")) {
                        salaryAmount = line.substringAfter("SALARY_AMOUNT:").toDoubleOrNull() ?: 0.0
                    } else if (line.startsWith("SALARY_DATE:")) {
                        salaryDate = line.substringAfter("SALARY_DATE:").toIntOrNull() ?: 1
                    } else if (line.startsWith("CURRENCY:")) {
                        currency = line.substringAfter("CURRENCY:")
                    } else if (line.startsWith("GOAL:")) {
                        goal = line.substringAfter("GOAL:").toDoubleOrNull() ?: 0.0
                    } else if (line.startsWith("--- TRANSACTIONS ---")) {
                        parsingTransactions = true
                    } else if (parsingTransactions) {
                        val parts = line.split("|")
                        if (parts.size >= 7) {
                            val merchant = parts[0]
                            val amount = parts[1].toDoubleOrNull() ?: 0.0
                            val category = parts[2]
                            val date = parts[3].toLongOrNull() ?: System.currentTimeMillis()
                            val payMethod = parts[4]
                            val notes = parts[5]
                            val isCash = parts[6] == "true"
                            
                            repository.insertTransaction(
                                merchant = merchant,
                                amount = amount,
                                category = category,
                                date = date,
                                paymentMethod = payMethod,
                                notes = notes,
                                isCash = isCash
                            )
                        }
                    }
                }
                
                if (userName.isNotBlank()) {
                    repository.saveSalarySettings(
                        SalaryEntity(
                            userName = userName,
                            salaryAmount = salaryAmount,
                            salaryDate = salaryDate,
                            currency = currency,
                            monthlyGoal = goal
                        )
                    )
                }
                
                android.widget.Toast.makeText(context, "Data successfully restored", android.widget.Toast.LENGTH_SHORT).show()
                onComplete()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Restore failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

data class ImportSummary(
    val fileName: String,
    val transactionCount: Int,
    val income: Double,
    val expenses: Double,
    val savings: Double,
    val subscriptionsCount: Int,
    val microLeaksCount: Int,
    val financialScore: Int
)

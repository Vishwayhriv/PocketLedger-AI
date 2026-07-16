package com.example.data

import kotlinx.coroutines.flow.Flow

class SalaryUseCase(private val repository: WalletRepository) {
    val salarySettingsFlow: Flow<SalaryEntity?> = repository.salarySettings

    suspend fun getSalarySettings(): SalaryEntity? = repository.getSalarySettings()

    suspend fun saveSalarySettings(
        userName: String,
        salaryAmount: Double,
        salaryDate: Int,
        currency: String,
        monthlyGoal: Double,
        profilePhotoUri: String?
    ) {
        val existing = repository.getSalarySettings()
        val settings = SalaryEntity(
            id = 1,
            userName = userName,
            salaryAmount = salaryAmount,
            salaryDate = salaryDate,
            currency = currency,
            monthlyGoal = monthlyGoal,
            profilePhotoUri = profilePhotoUri ?: existing?.profilePhotoUri,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        repository.saveSalarySettings(settings)
        
        // Storing general currency setting globally as well so standard components can fetch it
        repository.savePreference("pref_currency", currency)
    }
}

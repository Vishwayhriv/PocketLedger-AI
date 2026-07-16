package com.example.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.NotificationTransactionParser
import com.example.data.TransactionEntity
import com.example.data.SmartCategoryEngine
import com.example.data.AppPreferenceEntity
import com.example.ui.formatIndianCurrency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs

class TransactionNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return
        
        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        
        val fullText = text ?: bigText ?: ""

        serviceScope.launch {
            try {
                val database = AppDatabase.getDatabase(applicationContext)
                
                // Check if notification detection is enabled in preferences
                val isEnabled = database.appPreferenceDao().getPreference("pref_auto_detect_enabled")?.value == "true"
                if (!isEnabled) {
                    return@launch
                }

                // Parse the notification
                val parsed = NotificationTransactionParser.parse(title, fullText, packageName) ?: return@launch

                // 1. DUPLICATE DETECTION
                val recentTransactions = database.transactionDao().getAllTransactions().first()
                val currentTime = System.currentTimeMillis()
                val txDate = parsed.timestamp ?: currentTime
                
                val isDuplicate = recentTransactions.any { tx ->
                    val matchesRef = parsed.upiRef != null && tx.notes.contains(parsed.upiRef)
                    val isTxIncome = tx.category.equals("Salary", ignoreCase = true) || tx.category.equals("Income", ignoreCase = true)
                    val matchesType = parsed.isIncome == isTxIncome
                    val matchesDetails = tx.merchant.equals(parsed.merchant, ignoreCase = true) &&
                            abs(tx.amount - parsed.amount) < 0.01 &&
                            abs(tx.date - txDate) < 24 * 60 * 60 * 1000L &&
                            matchesType
                    val matchesBank = parsed.bankName == null || tx.notes.contains(parsed.bankName, ignoreCase = true)
                    matchesRef || (matchesDetails && matchesBank)
                }

                if (isDuplicate) {
                    return@launch
                }

                // Store balance in Room if extracted
                if (parsed.balance != null) {
                    database.appPreferenceDao().savePreference(AppPreferenceEntity("pref_detected_balance", parsed.balance.toString()))
                }

                // 2. CATEGORIZATION & INSERTION
                val category = if (parsed.isIncome) {
                    "Salary" // Default category for detected deposits
                } else {
                    SmartCategoryEngine.categorize(parsed.merchant, parsed.merchant)
                }

                val refString = if (parsed.upiRef != null) "Ref: ${parsed.upiRef} | " else ""
                val bankString = if (parsed.bankName != null) "Bank: ${parsed.bankName} | " else ""
                val entity = TransactionEntity(
                    merchant = parsed.merchant,
                    amount = parsed.amount,
                    category = category,
                    date = txDate,
                    paymentMethod = parsed.paymentMethod,
                    notes = "${refString}${bankString}Auto-detected via ${packageName.substringAfterLast('.')}",
                    isCash = false
                )

                val id = database.transactionDao().insertTransaction(entity)
                
                // Mark data as dirty for AI Insights refresh
                database.appPreferenceDao().savePreference(AppPreferenceEntity("pref_data_dirty", "true"))

                // 3. EMIT EVENT FOR SNACKBAR
                val formattedAmount = formatIndianCurrency(parsed.amount, parsed.currency)
                val appFriendlyName = when {
                    packageName.contains("paisa.user") -> "Google Pay"
                    packageName.contains("phonepe") -> "PhonePe"
                    packageName.contains("paytm") -> "Paytm"
                    packageName.contains("cred") -> "CRED"
                    else -> parsed.bankName ?: packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                }
                
                val snackbarMessage = if (parsed.isIncome) {
                    "$formattedAmount received from $appFriendlyName"
                } else {
                    "$formattedAmount paid to ${parsed.merchant}"
                }

                NotificationEventBus.emitEvent(snackbarMessage, id)

            } catch (e: Exception) {
                // Ignore silently, never crash
                Log.e("TxNotificationListener", "Error in notification listener: ${e.message}", e)
            }
        }
    }
}

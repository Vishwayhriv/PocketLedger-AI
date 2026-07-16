package com.example.ui.screens

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.*
import com.example.ui.theme.DynamicGreen
import com.example.ui.theme.DynamicRed
import com.example.ui.theme.DynamicBlue
import com.example.ui.theme.DynamicOrange
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToCashEntry: () -> Unit,
    onNavigateToTransactions: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToLeaks: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSalarySetup: () -> Unit,
    onNavigateToJourney: () -> Unit,
    onNavigateToCoach: () -> Unit,
    onNavigateToStatementImport: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val scope = rememberCoroutineScope()
    val demoModeActive by viewModel.demoModeActive.collectAsState()
    val salaryRecords by viewModel.salaryRecords.collectAsState()
    val health by viewModel.healthScore.collectAsState()
    val currencySymbol by viewModel.currencySymbol.collectAsState()
    val aiStory by viewModel.aiStoryState.collectAsState()
    val aiStoryLoading by viewModel.aiStoryLoading.collectAsState()
    val aiRoast by viewModel.aiRoastState.collectAsState()
    val aiRoastLoading by viewModel.aiRoastLoading.collectAsState()
    val aiAdvice by viewModel.aiAdviceState.collectAsState()
    val aiAdviceLoading by viewModel.aiAdviceLoading.collectAsState()
    val isAiEnabled by viewModel.isAiEnabled.collectAsState()
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()
    val subscriptions by viewModel.subscriptionCandidates.collectAsState()
    val leaks by viewModel.microLeaks.collectAsState()
    val salarySettings by viewModel.salarySettings.collectAsState()
    val showRatingPrompt by viewModel.showRatingPrompt.collectAsState()

    val isAutoDetectEnabled by viewModel.isAutoDetectEnabled.collectAsState()
    val consentState by viewModel.consentState.collectAsState()
    val detectedBalance by viewModel.detectedBalance.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    var isNotificationAccessGranted by remember { mutableStateOf(false) }
    var showNotificationPermissionRationale by remember { mutableStateOf(false) }

    val postNotificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setConsentState("enabled")
        if (!isGranted) {
            showNotificationPermissionRationale = true
        } else {
            try {
                val intent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                context.startActivity(intent)
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Search for Notification Access in System Settings", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }
    
    if (showNotificationPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showNotificationPermissionRationale = false },
            title = { Text("Notification Permission Required") },
            text = { Text("Pocket Ledger AI needs notification permission to immediately alert you when a transaction is detected in the background, updating your dashboard, reports, and safe-to-spend instantly.") },
            confirmButton = {
                Button(
                    onClick = {
                        showNotificationPermissionRationale = false
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            postNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationPermissionRationale = false }) {
                    Text("Dismiss")
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    
    val lifecycleOwnerForNotif = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwnerForNotif) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isNotificationAccessGranted = isNotificationServiceEnabled(context)
            }
        }
        lifecycleOwnerForNotif.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwnerForNotif.lifecycle.removeObserver(observer)
        }
    }

    if (consentState == "not_prompted") {
        HomeScreenNotificationConsentDialog(
            onEnable = {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    postNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    viewModel.setConsentState("enabled")
                    try {
                        val intent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(context, "Search for Notification Access in System Settings", android.widget.Toast.LENGTH_LONG).show()
                    }
                }
            },
            onMaybeLater = {
                viewModel.setConsentState("maybe_later")
            },
            onSkip = {
                viewModel.setConsentState("skipped")
            }
        )
    }

    // --- SMART RATING DIALOG OVERLAY ---
    if (showRatingPrompt) {
        val context = androidx.compose.ui.platform.LocalContext.current
        var ratingStars by remember { mutableStateOf(5) }
        var showDirectFeedback by remember { mutableStateOf(false) }
        var feedbackText by remember { mutableStateOf("") }
        
        AlertDialog(
            onDismissRequest = { viewModel.onUserDismissedRating(maybeLater = true) },
            title = {
                Text(
                    text = "Enjoying PocketLedger AI?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "We strive to offer a clean, premium personal financial companion. Your review helps us improve!",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    
                    if (!showDirectFeedback) {
                        Text(
                            text = "Rate your experience:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            for (i in 1..5) {
                                Icon(
                                    imageVector = if (i <= ratingStars) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Star $i",
                                    tint = if (i <= ratingStars) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clickable { ratingStars = i }
                                        .padding(2.dp)
                                )
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = feedbackText,
                            onValueChange = { feedbackText = it },
                            placeholder = { Text("What can we do better? Your feedback will be sent privately to our developers.") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ratingStars == 5 && !showDirectFeedback) {
                            viewModel.onUserRated(5)
                            val reviewManager = com.google.android.play.core.review.ReviewManagerFactory.create(context)
                            val request = reviewManager.requestReviewFlow()
                            request.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    val reviewInfo = task.result
                                    val activity = (context as? android.app.Activity) ?: (context as? android.content.ContextWrapper)?.baseContext as? android.app.Activity
                                    if (activity != null) {
                                        val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                                        flow.addOnCompleteListener {
                                            // Flow completed
                                        }
                                    } else {
                                        // Fallback: Open Play Store page
                                        try {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${context.packageName}"))
                                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                                            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(intent)
                                        }
                                    }
                                } else {
                                    // Fallback: Open Play Store page
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("market://details?id=${context.packageName}"))
                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                                        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }
                                }
                            }
                        } else if (!showDirectFeedback) {
                            showDirectFeedback = true
                        } else {
                            viewModel.onUserRated(ratingStars)
                            val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                                data = android.net.Uri.parse("mailto:support@pocketledger.ai")
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "PocketLedger AI Low Rating Feedback ($ratingStars Stars)")
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "User Rating: $ratingStars Stars\n" +
                                    "Private Feedback: $feedbackText\n\n" +
                                    "Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}\n" +
                                    "Android Version: ${android.os.Build.VERSION.RELEASE}"
                                )
                            }
                            try {
                                val chooser = android.content.Intent.createChooser(intent, "Send Feedback")
                                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(chooser)
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Feedback sent successfully", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (showDirectFeedback) "Submit" else "Rate Now")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { viewModel.onUserDismissedRating(maybeLater = true) }) {
                        Text("Maybe Later")
                    }
                    TextButton(onClick = { viewModel.onUserDismissedRating(maybeLater = false) }) {
                        Text("No Thanks")
                    }
                }
            }
        )
    }

    // --- CALENDAR & CYCLE CALCULATIONS ---
    val cal = Calendar.getInstance()
    val currentDay = cal.get(Calendar.DAY_OF_MONTH)
    val totalDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time)

    val activeSalary = if (transactions.isEmpty()) 0.0 else (salarySettings?.salaryAmount ?: 75000.0)
    val activeCreditDay = salarySettings?.salaryDate ?: 1
    val displayCurrency = salarySettings?.currency ?: currencySymbol

    val currentMonthSalary = if (transactions.isEmpty()) 0.0 else (if (activeSalary > 0.0) activeSalary else (salaryRecords.firstOrNull { it.monthYear == currentMonthYear }?.amount ?: 3000.0))

    val nonSalaryTxsThisMonth = if (transactions.isEmpty()) emptyList() else transactions.filter {
        val txCal = Calendar.getInstance().apply { timeInMillis = it.date }
        txCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH) &&
                txCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
                !it.category.equals("Salary", ignoreCase = true) && !it.category.equals("Income", ignoreCase = true)
    }

    val totalSpentThisMonth = if (transactions.isEmpty()) 0.0 else nonSalaryTxsThisMonth.sumOfAmount()
    val moneyRemaining = if (transactions.isEmpty()) 0.0 else (currentMonthSalary - totalSpentThisMonth).coerceAtLeast(0.0)

    val todayStartCal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val todaySpent = if (transactions.isEmpty()) 0.0 else transactions.filter {
        it.date >= todayStartCal.timeInMillis && !it.category.equals("Salary", ignoreCase = true) && !it.category.equals("Income", ignoreCase = true)
    }.sumOfAmount()

    val todayIncome = if (transactions.isEmpty()) 0.0 else transactions.filter {
        it.date >= todayStartCal.timeInMillis && (it.category.equals("Salary", ignoreCase = true) || it.category.equals("Income", ignoreCase = true))
    }.sumOfAmount()

    val todayTxCount = if (transactions.isEmpty()) 0 else transactions.count {
        it.date >= todayStartCal.timeInMillis
    }

    val remainingPercent = if (currentMonthSalary > 0) ((moneyRemaining / currentMonthSalary) * 100).toInt() else 0

    val daysRemaining = if (currentDay <= activeCreditDay) {
        (activeCreditDay - currentDay).coerceAtLeast(1)
    } else {
        ((totalDaysInMonth - currentDay) + activeCreditDay).coerceAtLeast(1)
    }

    // Safe Daily Spend = Money Remaining / Days left in month
    val dailySafeSpend = if (transactions.isEmpty()) 0.0 else (moneyRemaining / daysRemaining).coerceAtLeast(0.0)

    // Current Cycle Day (e.g. Day 17)
    val cycleDay = if (currentDay >= activeCreditDay) {
        (currentDay - activeCreditDay + 1)
    } else {
        (totalDaysInMonth - activeCreditDay + currentDay + 1)
    }

    // --- DYNAMIC LOCAL-TIME BASED GREETING STATE ---
    var currentDateTime by remember {
        mutableStateOf(Calendar.getInstance())
    }

    // Refresh greeting and date whenever the screen is resumed
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                currentDateTime = Calendar.getInstance()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Periodic updater to support midnight passing or real-time local updates
    LaunchedEffect(Unit) {
        while (true) {
            currentDateTime = Calendar.getInstance()
            kotlinx.coroutines.delay(10000) // update every 10 seconds
        }
    }

    val localHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
    val greeting = when (localHour) {
        in 5..11 -> "Good Morning"
        in 12..16 -> "Good Afternoon"
        in 17..20 -> "Good Evening"
        else -> "Good Night"
    }
    val greetingEmoji = when (localHour) {
        in 5..11 -> "👋"
        in 12..16 -> "☀️"
        in 17..20 -> "🌆"
        else -> "🌙"
    }

    // Dynamic motivational compliments based on actual stats
    val savingsRate = if (currentMonthSalary > 0) ((moneyRemaining / currentMonthSalary) * 100).toInt() else 0
    val motivationCompliment = remember(savingsRate, leaks) {
        when {
            savingsRate > 40 -> "Discipline level: masterclass. You're saving a superb $savingsRate% of your income! Keep pushing!"
            savingsRate > 20 -> "Awesome pacing! You are well within the safe zone, saving over 20%. Keep it up!"
            savingsRate > 0 -> "Solid progress. You have positive savings this month. Watch out for micro leaks to save even more!"
            else -> "Budget alert: You've exceeded your salary cycle. It is time to audit subscriptions and cut micro expenditures."
        }
    }

    // Determine if today is Payday
    val isPayday = currentDay == activeCreditDay

    // Animated values for ring progress
    val progressPercent = if (currentMonthSalary > 0) (moneyRemaining / currentMonthSalary).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progressPercent,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCashEntry,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .testTag("add_cash_fab")
                    .padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = stringResource(id = R.string.action_cash_entry),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Dynamic Greeting and Date
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val displayName = salarySettings?.userName
                    val greetingText = if (!displayName.isNullOrBlank()) {
                        "$greeting, $displayName $greetingEmoji"
                    } else {
                        "$greeting $greetingEmoji"
                    }
                    Text(
                        text = greetingText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val formattedDate = remember(currentDateTime) {
                        val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
                        formatter.format(currentDateTime.time)
                    }
                    Text(
                        text = formattedDate,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Salary Cycle Day $cycleDay",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "  •  ",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "$daysRemaining Days Until Salary",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Insights Card Section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToReports() },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "📊", fontSize = 18.sp)
                                Text(
                                    text = "Insights",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "Daily Summary",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Today's Spend",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatIndianCurrency(todaySpent, displayCurrency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (todaySpent > dailySafeSpend) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(modifier = Modifier.weight(1.1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Today's Income",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = formatIndianCurrency(todayIncome, displayCurrency),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = com.example.ui.theme.DynamicGreen,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(modifier = Modifier.weight(0.9f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Today Txs",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$todayTxCount txs",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Remaining %",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "$remainingPercent%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f))
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        val quickTip = when {
                            todaySpent > dailySafeSpend -> "💡 Tip: You're over your daily budget. Try to defer non-essential spend."
                            todaySpent == 0.0 -> "💡 Tip: Great job! No spend recorded today yet. Perfect for boosting savings."
                            remainingPercent < 15 -> "💡 Tip: Low remaining balance! Focus on core needs only for the rest of the cycle."
                            else -> "💡 Tip: Pacing is steady. Consider rounding up expenses to build an offline buffer."
                        }
                        Text(
                            text = quickTip,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Demo Mode Visual Tag/Banner
            if (demoModeActive) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Demo Mode Active",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Wipe sample data and start fresh anytime.",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        viewModel.clearAllData()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Start Fresh", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Payday Mode Celebration Banner
            if (isPayday) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Celebration,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Happy Payday! 🎉",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Your salary of ${formatIndianCurrency(currentMonthSalary, displayCurrency)} has landed! This is the perfect moment to build your savings plan first.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Suggested goals for this cycle:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Transfer savings first (Target: ${formatIndianCurrency(currentMonthSalary * 0.2, displayCurrency)})", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Audit subscription candidates before auto-renewal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                        }
                    }
                }
            }

            // Welcome setup wizard or Redesigned Hero Salary Card
            item {
                if (salarySettings == null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp)),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(id = R.string.welcome_title),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.setup_subtitle),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onNavigateToSalarySetup,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(id = R.string.settings_wizard), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Upgraded premium hero layout with interactive values & circular tracking
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.82f)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1.3f)) {
                                Text(
                                    text = "REMAINING BALANCE",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatIndianCurrency(moneyRemaining, displayCurrency),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )

                                if (!detectedBalance.isNullOrEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "AUTO-DETECTED BANK BAL: " + formatIndianCurrency(detectedBalance?.toDoubleOrNull() ?: 0.0, displayCurrency),
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Column {
                                        Text(
                                            text = "MONTHLY SALARY",
                                            fontSize = 9.sp,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = formatIndianCurrency(currentMonthSalary, displayCurrency),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "CYCLE TIMELINE",
                                            fontSize = 9.sp,
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.SemiBold,
                                            letterSpacing = 0.5.sp
                                        )
                                        Text(
                                            text = "Day $cycleDay / $totalDaysInMonth",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "$daysRemaining days until next payday",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Circular Progress Ring showing remaining percentage
                            Box(
                                modifier = Modifier
                                    .weight(0.7f)
                                    .padding(start = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier.size(84.dp),
                                    color = Color(0xFF10B981),
                                    strokeWidth = 8.dp,
                                    trackColor = Color.White.copy(alpha = 0.15f),
                                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${(animatedProgress * 100).toInt()}%",
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Text(
                                        text = "Left",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Daily Safe Spend & Alert
            item {
                if (salarySettings != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "DAILY SAFE TO SPEND",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Text(
                                    text = formatIndianCurrency(dailySafeSpend, displayCurrency),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF10B981)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Spent Today:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = formatIndianCurrency(todaySpent, displayCurrency),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (todaySpent > dailySafeSpend) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Warning if spent exceeds limit
                            if (todaySpent > dailySafeSpend && dailySafeSpend > 0) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                ) {
                                    Row {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = "Daily safe spend exceeded!",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = "You've spent ${formatIndianCurrency(todaySpent - dailySafeSpend, displayCurrency)} over your daily threshold. Consider throttling secondary expenses.",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error,
                                                lineHeight = 15.sp,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Coach Motivation Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = motivationCompliment,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Feature 2: Redesigned Beautiful Money Story & AI Hub
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PocketLedger AI Hub",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    if (isPremiumUser) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("PREMIUM", color = Color(0xFFF59E0B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                
                if (!isAiEnabled) {
                    GlassCard {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.SpeakerNotesOff, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "AI Ledger Coach is Currently Disabled",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Turn on the dynamic offline sandbox intelligence in your App Settings.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { viewModel.toggleAiEnabled(true) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Enable AI Sandbox Engine")
                            }
                        }
                    }
                } else {
                    var selectedAiTab by remember { mutableStateOf(0) } // 0: Story, 1: Roast, 2: Advice
                    
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Custom Glass Tab Buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("Story 📖", "Roast 🔥", "Advice 💡").forEachIndexed { index, label ->
                                val isSelected = selectedAiTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { selectedAiTab = index }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        GlassCard {
                            val onSurface = MaterialTheme.colorScheme.onSurface
                            
                            when (selectedAiTab) {
                                0 -> { // Story Tab
                                    if (aiStory.isEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Ready to compile your interactive local Story?",
                                                color = onSurface.copy(alpha = 0.7f),
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            )
                                            Button(
                                                onClick = { viewModel.generateAiStory(force = true) },
                                                enabled = !aiStoryLoading,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                if (aiStoryLoading) {
                                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                } else {
                                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Generate Story")
                                                }
                                            }
                                        }
                                    } else {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("This Month's Story", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black, fontSize = 13.sp)
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(text = aiStory, color = onSurface, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.height(14.dp))
                                            
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { viewModel.generateAiStory(force = true) },
                                                    enabled = !aiStoryLoading,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("Re-compile Story", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = onNavigateToCoach,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Ask Coach", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                                1 -> { // Roast Tab
                                    if (aiRoast.isEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Want a humorous, lighthearted spending audit?",
                                                color = onSurface.copy(alpha = 0.7f),
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            )
                                            Button(
                                                onClick = { viewModel.generateSpendingRoast(force = true) },
                                                enabled = !aiRoastLoading,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                if (aiRoastLoading) {
                                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                } else {
                                                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Roast My Spending")
                                                }
                                            }
                                        }
                                    } else {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Spending Roast 🔥", color = Color(0xFFEF4444), fontWeight = FontWeight.Black, fontSize = 13.sp)
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(text = aiRoast, color = onSurface, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.height(14.dp))
                                            
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { viewModel.generateSpendingRoast(force = true) },
                                                    enabled = !aiRoastLoading,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("Re-roast", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = onNavigateToCoach,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Ask Coach", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                                2 -> { // Advice Tab
                                    if (aiAdvice.isEmpty()) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "Formulate 3 precise tactical local savings action points?",
                                                color = onSurface.copy(alpha = 0.7f),
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(bottom = 12.dp)
                                            )
                                            Button(
                                                onClick = { viewModel.generateSavingsAdvice(force = true) },
                                                enabled = !aiAdviceLoading,
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                if (aiAdviceLoading) {
                                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                } else {
                                                    Icon(Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Formulate Advice")
                                                }
                                            }
                                        }
                                    } else {
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Savings Advice 💡", color = Color(0xFFFBBF24), fontWeight = FontWeight.Black, fontSize = 13.sp)
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(text = aiAdvice, color = onSurface, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
                                            Spacer(modifier = Modifier.height(14.dp))
                                            
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                    onClick = { viewModel.generateSavingsAdvice(force = true) },
                                                    enabled = !aiAdviceLoading,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Text("Re-compute", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Button(
                                                    onClick = onNavigateToCoach,
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Ask Coach", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Feature 3 Launch Card: "My Financial Journey"
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToJourney),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "My Financial Journey",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Open your interactive month-by-month financial history ledger.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                lineHeight = 14.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // Feature 4 & 10: Financial Wins & Streaks Section
            item {
                Text(
                    text = "Financial Achievements & Streaks",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                GlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        val daysLogged = transactions.map {
                            val tCal = Calendar.getInstance().apply { timeInMillis = it.date }
                            tCal.get(Calendar.DAY_OF_YEAR)
                        }.distinct().size

                        // Streak Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(com.example.ui.theme.DynamicOrange.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = com.example.ui.theme.DynamicOrange, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Current Streak: $daysLogged Days Active",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Consistent transaction logging protects your sandbox ledger.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }

                        // Savings Win Row
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(com.example.ui.theme.DynamicGreen.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = com.example.ui.theme.DynamicGreen, modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (savingsRate >= 20) "Savings Shield Level: MASTER" else "Savings Shield Level: ACTIVE",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "You've successfully secured a positive savings factor of $savingsRate%.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                        }
                    }
                }
            }

            // Ghost Subscriptions & Leaks Quick Toggles
            item {
                val onSurface = MaterialTheme.colorScheme.onSurface
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Subscriptions Panel
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNavigateToSubscriptions),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Ghost Bills", color = onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${subscriptions.size} Detected", color = onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Click to view details", color = onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }

                    // Micro Leaks Panel
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onNavigateToLeaks),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = com.example.ui.theme.DynamicRed, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Micro Leaks", color = onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${leaks.size} Detected", color = onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                            Text("Click to audit", color = onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                    }
                }
            }

            // Smart Bank Statement Import Quick Action Card
            item {
                val onSurface = MaterialTheme.colorScheme.onSurface
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onNavigateToStatementImport)
                        .testTag("import_statement_quick_action"),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Description,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Smart Bank Statement Import", color = onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("Import bank statements offline (CSV, PDF, TXT, Excel)", color = onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = onSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Warning Banner if Auto Detect is enabled but notification permission is missing
            if (isAutoDetectEnabled && !isNotificationAccessGranted) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Automatic transaction detection is disabled.",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                            Text(
                                text = "Enable it in Settings to automatically add supported financial transactions locally and securely.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                            )
                            Button(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Search for Notification Access in System Settings", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Enable Now", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Recent Transactions header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.ledger_title),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "See All",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(onClick = onNavigateToTransactions)
                    )
                }
            }

            // Feature 8: Dynamic Beautiful Empty State for Transactions
            val recentTxs = transactions.take(4)
            if (recentTxs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No transactions yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Start tracking your finances.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = onNavigateToCashEntry,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .testTag("add_first_transaction_empty_state_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add First Transaction", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                
                                OutlinedButton(
                                    onClick = onNavigateToStatementImport,
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .testTag("import_statement_empty_state_button")
                                ) {
                                    Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import Statement", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            } else {
                items(recentTxs) { tx ->
                    TransactionRow(
                        tx = tx,
                        currency = displayCurrency,
                        onDelete = { viewModel.deleteTransaction(tx.id) }
                    )
                }
            }

            // Visual balance spacer
            item {
                Spacer(modifier = Modifier.height(20.dp))
                com.example.monetization.AdMobBannerAd(isPremium = isPremiumUser)
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun StoryMetricRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = color
        )
    }
}

@Composable
fun HomeScreenNotificationConsentDialog(
    onEnable: () -> Unit,
    onMaybeLater: () -> Unit,
    onSkip: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onMaybeLater,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Top header / Logo section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Enable Automatic Transaction Detection",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Middle Info Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "PocketLedger AI can automatically detect financial transaction notifications to help build your transaction history.",
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Detail points
                    val points = listOf(
                        "🔍 Smart Extraction" to "The app processes only relevant financial notifications (Paytm, GPay, PhonePe, Cred, etc.).",
                        "🔒 Privacy-First & Offline" to "Notification data is processed locally on your device. It is never uploaded.",
                        "⚙️ Full Control" to "You can disable automatic detection or revoke permissions at any time in Settings.",
                        "🛡️ Secure & Safe" to "No banking passwords, OTPs, or account credentials are collected."
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        points.forEach { (title, desc) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                                )
                                Column {
                                    Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                                    Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                                }
                            }
                        }
                    }
                }

                // Bottom Buttons Section
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onEnable,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Enable", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onMaybeLater,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Maybe Later", fontSize = 14.sp)
                        }
                        
                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Text("Skip", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}

private fun isNotificationServiceEnabled(context: android.content.Context): Boolean {
    val pkgName = context.packageName
    val flat = android.provider.Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    if (!flat.isNullOrEmpty()) {
        val names = flat.split(":")
        for (name in names) {
            val cn = android.content.ComponentName.unflattenFromString(name)
            if (cn != null && cn.packageName == pkgName) {
                return true
            }
        }
    }
    return false
}

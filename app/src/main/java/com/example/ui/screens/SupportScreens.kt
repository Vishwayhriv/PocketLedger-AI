package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.ui.GlassCard
import com.example.ui.MainViewModel

// --- PRIVACY POLICY SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy Policy", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                Text(
                    text = "Your Privacy is Absolute",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    text = "PocketLedger AI is built from the ground up as an offline-first financial assistant. Your peace of mind and data security are our highest priorities.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("What is Stored locally", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• Cash Transactions: All manual logs.\n" +
                            "• Statements: Text from imported files is parsed and saved locally.\n" +
                            "• Auto-Detected Transactions: Financial notifications parsed locally.\n" +
                            "• Preferences: Currencies, themes, and rating dismissals.\n" +
                            "• Profile Info: Your name and net salary parameters.",
                            fontSize = 13.sp, lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Notification Access & Auto-Detection", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• Optional Access: Notification access is entirely optional and is only used to build your ledger automatically.\n" +
                            "• Financial Only: The app processes only relevant financial transaction notifications and ignores social, messaging, and other personal alerts.\n" +
                            "• Local Processing: Notification data is read and parsed strictly on your physical device. Raw notification contents, phone numbers, OTPs, or passwords are never uploaded.\n" +
                            "• Easy Revocation: You can disable Notification Access or revoke permissions at any time directly through Settings.",
                            fontSize = 13.sp, lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("What is NOT Collected", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• Bank Credentials: We never ask for password or SMS scraping access.\n" +
                            "• Personal Telemetry: No server logs of your transactions.\n" +
                            "• No Cloud Storing: All records reside securely inside the SQLite Room database on your physical device.",
                            fontSize = 13.sp, lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("On-Device AI Processing", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Our advanced budget storytelling uses Gemini AI. To provide fully offline safety, AI stories are strictly bound to secure device contexts. No external servers parse your transactions for tracking or profile targeted advertising.",
                            fontSize = 13.sp, lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("User Rights & Data Control", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "You maintain complete command over your data. At any moment, you can purge the entire local sandbox database and reset configurations back to zero directly in the settings dashboard.",
                            fontSize = 13.sp, lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Contact Support Email: support@pocketledger.ai",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }
    }
}

// --- TERMS OF USE SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Terms & Conditions", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            item {
                Text(
                    text = "Terms of Service",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    text = "Please read these terms carefully before utilizing PocketLedger AI.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                )
            }

            val termsList = listOf(
                "Acceptable Use" to "PocketLedger AI is a personal budgeting tool. You must not exploit local parsing features to systematically process third-party client banking histories for commercial bookkeeping.",
                "Disclaimer of Financial Advice" to "PocketLedger AI provides structural mathematical overviews and descriptive story summaries. The compiled budget scores and suggestions DO NOT constitute certified investment, legal, tax, or professional financial advice.",
                "AI Limitation Disclaimer" to "Analytical reports and micro-leak identifications are processed through advanced predictive algorithms. While we strive for accuracy, AI summaries may occasionally misclassify custom local receipts or statement layouts.",
                "Data Responsibility" to "As an offline-first solution, all records reside entirely on your device. We hold no responsibility for transaction history loss due to device hardware failures, operating system crashes, or unbacked factory resets."
            )

            items(termsList) { (title, description) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(description, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    }
                }
            }

            item {
                Text(
                    text = "Copyright © 2026 PocketLedger AI. All Rights Reserved.",
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// --- ABOUT SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About PocketLedger AI", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(20.dp))
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(52.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("PocketLedger AI", fontSize = 26.sp, fontWeight = FontWeight.Black)
                Text("Your Smart Offline Salary Companion", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "An offline-first personal financial manager utilizing dynamic, conversational storytelling to trace salary cycle run-times, hunt silent background subscriptions, and isolate micro cost leaks without breaching sandbox privacy boundaries.",
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("App Version", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Build Number", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("${BuildConfig.VERSION_CODE}", fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Developer Name", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text("PocketLedger Devs", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Open Source Licenses",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            // Show open source notice or just alert
                        }
                        .padding(8.dp)
                )
                Text(
                    text = "This app is built with 100% open-source components including Jetpack Compose, Room Database, and Kotlin Coroutines.",
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// --- CONTACT SUPPORT SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactSupportScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit
) {
    val context = LocalContext.current
    
    val menuItems = listOf(
        Triple("Email Support", "Get personalized assistance via email", Icons.Default.Email) to "email",
        Triple("Report Bug", "Notify us of a glitch or error in the app", Icons.Default.BugReport) to "bug_report",
        Triple("Feature Request", "Suggest a capability or visual enhancement", Icons.Default.Lightbulb) to "feature_request",
        Triple("Help Center & FAQ", "Browse detailed offline guidance & solutions", Icons.Default.HelpCenter) to "help_center",
        Triple("Submit Private Feedback", "Rate your offline experience with us", Icons.Default.Feedback) to "feedback",
        Triple("Privacy Policy", "Read our complete data sandbox guidelines", Icons.Default.Security) to "privacy_policy",
        Triple("Terms of Service", "Examine unacceptable use clauses & disclaimers", Icons.Default.Gavel) to "terms",
        Triple("About App", "Inspect version indexes and developer specifications", Icons.Default.Info) to "about"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Contact Support", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "How can we assist you?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Explore FAQs or contact our offline support module directly.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(menuItems) { (itemInfo, route) ->
                val (title, subtitle, icon) = itemInfo
                GlassCard(
                    onClick = {
                        if (route == "email") {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:support@pocketledger.ai")
                                putExtra(Intent.EXTRA_SUBJECT, "PocketLedger AI Help Request")
                            }
                            try {
                                context.startActivity(Intent.createChooser(intent, "Send Email"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                            }
                        } else if (route == "feature_request") {
                            onNavigate("feedback")
                        } else {
                            onNavigate(route)
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(subtitle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
                            }
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// --- HELP CENTER & FAQ SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpCenterScreen(onBack: () -> Unit) {
    val faqs = listOf(
        "How Automatic Detection Works?" to "PocketLedger AI utilizes Android's Notification Listener Service to automatically detect incoming financial transaction alerts (like Google Pay, PhonePe, Paytm, or banking apps). It processes these notifications entirely locally on your device, extracts the transaction details, and automatically adds them to your offline ledger.",
        "How to disable Notification Access?" to "You can disable automatic detection at any time. Simply open your device's System Settings, search for 'Notification Access' (or 'Device & App Notifications'), locate PocketLedger AI, and turn the switch off. Alternatively, you can toggle the 'Automatic Transaction Detection' switch off within the app's Settings screen.",
        "How to import statements?" to "To import a statement, navigate to the Home screen and click the 'Import PDF/TXT' quick action card. Paste raw bank notification text or transaction lists directly, and the offline parser will isolate dates, merchants, and categories safely.",
        "How to edit salary or payday?" to "Open the Settings screen and tap on 'Salary Setup Wizard'. This launches the local profiles form, letting you update your name, payday day, currency, monthly saving goals, and salary cap.",
        "How to add cash expenses?" to "At any time on the Home screen, tap the floating '+' (FAB) button. Fill out the merchant name, date, category, and notes to safely store local cash receipts in the timeline.",
        "How to change language?" to "Visit the Settings panel, find the 'App Language' picker card, and tap it to switch the entire user interface to हिन्दी, తెలుగు, தமிழ், ಕನ್ನಡ, etc.",
        "How to delete all ledger data?" to "Go to the Settings page, navigate into the 'Privacy & Sandbox' section, scroll down to the 'Danger Zone', and confirm deletion to permanently wipe all local database rows.",
        "How to backup my budgets?" to "Since we are fully offline, we do not store your budgets on our servers. You can back up your transactions by clicking 'Export Data' in the Settings menu to share or save a secure local ledger JSON."
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Frequently Asked Questions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Learn how PocketLedger AI processes your offline transactions.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            items(faqs) { (question, answer) ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = question,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f),
                                color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                        if (expanded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = answer,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// --- FEEDBACK SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var feedbackText by remember { mutableStateOf("") }
    var ratingStars by remember { mutableStateOf(5) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Submit Feedback", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Enjoying PocketLedger AI?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Your review tells our developers how we can keep improving offline financial story engines.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

            Spacer(modifier = Modifier.height(8.dp))
            Text("Rate your overall experience:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            // Star Selection Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= ratingStars) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star $i",
                        tint = if (i <= ratingStars) Color(0xFFFBBF24) else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        modifier = Modifier
                            .size(48.dp)
                            .clickable { ratingStars = i }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = feedbackText,
                onValueChange = { feedbackText = it },
                placeholder = { Text("Write your thoughts or feature requests here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support@pocketledger.ai")
                        putExtra(Intent.EXTRA_SUBJECT, "PocketLedger AI User Feedback ($ratingStars Stars)")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "User Rating: $ratingStars Stars\n" +
                            "Feedback: $feedbackText\n\n" +
                            "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                            "Android Version: ${Build.VERSION.RELEASE}\n" +
                            "App Version: ${BuildConfig.VERSION_NAME}"
                        )
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Send Email"))
                        Toast.makeText(context, "Feedback mail draft created", Toast.LENGTH_SHORT).show()
                        onBack()
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = feedbackText.isNotBlank()
            ) {
                Text("Send Feedback Email", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- BUG REPORT SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Report a Bug", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Encountered a glitch?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Let us know what went wrong so we can squash it in the next update.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Auto-Collected Telemetry Info:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    Text("• Device: ${Build.MANUFACTURER} ${Build.MODEL}", fontSize = 11.sp)
                    Text("• Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})", fontSize = 11.sp)
                    Text("• App Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})", fontSize = 11.sp)
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Explain what happened and how to trigger the glitch...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:support@pocketledger.ai")
                        putExtra(Intent.EXTRA_SUBJECT, "PocketLedger AI Bug Report")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Bug Description:\n$description\n\n" +
                            "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                            "Android Version: ${Build.VERSION.RELEASE}\n" +
                            "App Version: ${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
                        )
                    }
                    try {
                        context.startActivity(Intent.createChooser(intent, "Send Bug Report"))
                        Toast.makeText(context, "Bug report mail drafted", Toast.LENGTH_SHORT).show()
                        onBack()
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email client found", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                enabled = description.isNotBlank()
            ) {
                Text("Submit Bug Report Email", fontWeight = FontWeight.Bold)
            }
        }
    }
}

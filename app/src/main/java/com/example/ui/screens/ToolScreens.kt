package com.example.ui.screens

import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.ParseResult
import com.example.ui.BarChart
import com.example.ui.GlassCard
import com.example.ui.MainViewModel
import com.example.ui.MerchantAvatar
import com.example.ui.formatIndianCurrency
import com.example.ui.sumOfAmount
import com.example.ui.sumOfBigDecimal
import com.example.ui.theme.DynamicGreen
import com.example.ui.theme.DynamicRed
import com.example.ui.theme.DynamicBlue
import com.example.ui.theme.DynamicOrange
import java.text.SimpleDateFormat
import java.util.*

// --- STATEMENT IMPORT WIZARD ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatementImportScreen(
    viewModel: MainViewModel,
    onNavigateToPremium: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var rawText by remember { mutableStateOf("") }
    val importResult by viewModel.importResult.collectAsState()
    val importLoadingState by viewModel.importLoadingState.collectAsState()
    val importErrorMessage by viewModel.importErrorMessage.collectAsState()

    val filePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = getFileName(context, it)
            viewModel.autoImportStatementFile(context, it, fileName)
        }
    }

    val demoStatement = """
        2026-07-10,Netflix Premium,15.99,Entertainment,Card,Monthly billing
        2026-07-11,Uber Trip,14.50,Travel,UPI,Work commute
        2026-07-11,Starbucks,5.20,Food,Cash,Morning brew
        2026-07-12,Starbucks Coffee,5.80,Food,Cash,With team
        2026-07-12,Whole Foods,84.20,Food,Card,Weekly grocery
        2026-07-13,Amazon US,45.00,Shopping,Card,Books and items
    """.trimIndent()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.action_statement_import), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
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
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Zero Cloud Collection. Import your bank statement file or paste plain-text ledger data below to parse, categorize, and store transactions fully offline on-device.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }

            // OPTION 1: Upload Bank Statement File
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("upload_file_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Upload Bank Statement File",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = "Supports PDF, CSV, TXT, or Excel statement files. Safe & secure on-device extraction.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // PDF Picker
                            Button(
                                onClick = { filePickerLauncher.launch("application/pdf") },
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("import_pdf_button"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "📄 PDF",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }

                            // CSV/Excel/Any Picker
                            Button(
                                onClick = { filePickerLauncher.launch("*/*") },
                                colors = ButtonDefaults.filledTonalButtonColors(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("import_csv_excel_button"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "📊 CSV / Excel",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Section Separator Or Paste Text
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    Text(
                        text = " OR PASTE LEDGER ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                }
            }

            // Loading state feedback
            if (importLoadingState) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Extracting & analyzing statement transactions on-device...",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Error message feedback
            if (importErrorMessage != null) {
                val isLimitReached = importErrorMessage?.startsWith("LIMIT_REACHED:") == true
                val displayErrorText = if (isLimitReached) {
                    importErrorMessage?.substringAfter("LIMIT_REACHED:")
                } else {
                    importErrorMessage
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLimitReached) 
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            else 
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isLimitReached)
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            else
                                MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isLimitReached) Icons.Default.Lock else Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (isLimitReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isLimitReached) "Limit Reached" else "Import Issue",
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLimitReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = displayErrorText ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )

                            if (isLimitReached) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { onNavigateToPremium() },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Upgrade Now", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val activity = context as? android.app.Activity
                                            if (activity != null) {
                                                com.example.monetization.AdMobManager.showRewardedAd(activity) {
                                                    viewModel.grantRewardedImport {
                                                        viewModel.clearImportError()
                                                        android.widget.Toast.makeText(context, "1 Free Import granted! Try uploading again.", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Watch Video", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }

                            TextButton(
                                onClick = { viewModel.clearImportError() },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Clear", color = if (isLimitReached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("Paste CSV or text lines here...\nFormat: Date, Merchant, Amount, Category, PaymentMethod, Notes", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("import_statement_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Quick Demo Paste Trigger
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = { rawText = demoStatement }) {
                        Text("Paste Demo Statement", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    TextButton(onClick = { rawText = "" }) {
                        Text("Clear", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                    }
                }
            }

            // Parse Action Button
            item {
                Button(
                    onClick = {
                        if (rawText.trim().isNotEmpty()) {
                            viewModel.importStatementData(rawText)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("import_statement_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(16.dp),
                    enabled = rawText.trim().isNotEmpty()
                ) {
                    Text("Parse & Merge Ledger", fontSize = 16.sp, fontWeight = FontWeight.Black)
                }
            }

            // Diagnostic Results
            if (importResult != null) {
                item {
                    val res = importResult!!
                    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, outlineColor),
                        modifier = Modifier.padding(vertical = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("PARSE REPORT", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                             ) {
                                Text("Imported:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                Text("${res.parsedTransactions.size} transactions", color = DynamicGreen, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Skipped:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                Text("${res.errors.size} invalid rows", color = DynamicRed, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Duplicates:", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                Text("${res.duplicatesFound} ignored", color = DynamicOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            if (res.errors.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Errors found in statement lines:", color = DynamicRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                res.errors.take(3).forEach { err ->
                                    Text("- $err", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), fontSize = 11.sp)
                                }
                                if (res.errors.size > 3) {
                                    Text("- ... and ${res.errors.size - 3} more line errors.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 10.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    viewModel.clearImportResult()
                                    rawText = ""
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        try {
            if (cursor != null && cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        } finally {
            cursor?.close()
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "statement.pdf"
}

// --- GHOST SUBSCRIPTION HUNTER SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GhostSubscriptionsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val subscriptions by viewModel.subscriptionCandidates.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.hunter_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.hunter_subtitle),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (subscriptions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Celebration, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(id = R.string.hunter_empty), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(subscriptions) { sub ->
                        val sdf = remember { SimpleDateFormat("dd MMM yyyy", Locale.US) }
                        val formattedDate = sdf.format(Date(sub.lastBilled))
                        val onSurface = MaterialTheme.colorScheme.onSurface

                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MerchantAvatar(name = sub.merchant)
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sub.merchant, color = onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text(
                                        text = "${sub.interval} cycle • Confidence: ${(sub.confidence * 100).toInt()}%",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Text(
                                    text = formatIndianCurrency(sub.amount, currency),
                                    color = onSurface,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 10.dp))

                            Text(
                                text = "Diagnosis: ${sub.explanation}",
                                color = onSurface.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Text(
                                text = "Last observed payment: $formattedDate.",
                                color = onSurface.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- MICRO LEAK DETECTOR SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MicroLeaksScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val leaks by viewModel.microLeaks.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.leak_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.leak_subtitle),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                fontSize = 13.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            if (leaks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(stringResource(id = R.string.leak_empty), color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(leaks) { leak ->
                        val onSurface = MaterialTheme.colorScheme.onSurface
                        GlassCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MerchantAvatar(name = leak.merchant)
                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(leak.merchant, color = onSurface, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    Text(
                                        text = "Frequent Outflow Trigger",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Stats Grid Table
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Purchases Count:", color = onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                                    Text("${leak.count} times", color = onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Average Spend:", color = onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                                    Text(formatIndianCurrency(leak.averageAmount, currency), color = onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Monthly Spend:", color = onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                                    Text(formatIndianCurrency(leak.monthlyTotal, currency), color = DynamicRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Yearly Spend:", color = onSurface.copy(alpha = 0.6f), fontSize = 12.sp)
                                    Text(formatIndianCurrency(leak.estimatedAnnualCost, currency), color = DynamicRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Potential Saving Banner
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DynamicGreen.copy(alpha = 0.12f))
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Savings,
                                        contentDescription = null,
                                        tint = DynamicGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = "POTENTIAL SAVING",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = DynamicGreen,
                                            letterSpacing = 0.5.sp
                                        )
                                        val savingText = if (currency == "₹") {
                                            "By reducing ${leak.merchant} expenses by 50%, you could save ${formatIndianCurrency(leak.estimatedAnnualCost / 2.0, currency)} annually."
                                        } else {
                                            "You could save ${formatIndianCurrency(leak.estimatedAnnualCost / 2.0, currency)} annually."
                                        }
                                        Text(
                                            text = savingText,
                                            color = DynamicGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}// --- REPORTS SCREEN WITH CHARTS ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()

    var selectedPeriod by remember { mutableStateOf("This Month") }
    val periods = listOf("This Month", "Last 3 Months", "All Time")

    val filteredTxs = remember(transactions, selectedPeriod) {
        val nowCal = Calendar.getInstance()
        transactions.filter { tx ->
            when (selectedPeriod) {
                "This Month" -> {
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    txCal.get(Calendar.MONTH) == nowCal.get(Calendar.MONTH) &&
                            txCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR)
                }
                "Last 3 Months" -> {
                    val limit = System.currentTimeMillis() - (90L * 24L * 60L * 60L * 1000L)
                    tx.date >= limit
                }
                else -> true
            }
        }
    }

    // Process category amounts
    val categoryTotals = remember(filteredTxs) {
        filteredTxs.filter { it.category != "Salary" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOfAmount() }
            .toList()
            .sortedByDescending { it.second }
    }

    // Process digital vs cash amounts
    val paymentTotals = remember(filteredTxs) {
        val groups = filteredTxs.filter { it.category != "Salary" }
            .groupBy { if (it.isCash || it.paymentMethod == "Cash" || it.isCash == true) "Cash" else "Digital" }
        val cashSum = groups["Cash"]?.sumOfAmount() ?: 0.0
        val digitalSum = groups["Digital"]?.sumOfAmount() ?: 0.0
        Pair(cashSum, digitalSum)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.reports_title), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(id = R.string.reports_subtitle),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 13.sp
                )
            }

            // Period Selector Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    periods.forEach { period ->
                        val isSelected = selectedPeriod == period
                        InputChip(
                            selected = isSelected,
                            onClick = { selectedPeriod = period },
                            label = { Text(period) },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                labelColor = MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }

            // Category Bar Chart Section
            item {
                GlassCard {
                    Text(
                        text = stringResource(id = R.string.reports_category_breakdown).uppercase(Locale.getDefault()),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (categoryTotals.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(stringResource(id = R.string.reports_empty), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
                        }
                    } else {
                        BarChart(
                            categories = categoryTotals.map { it.first },
                            values = categoryTotals.map { it.second },
                            currency = currency,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Smart Recommendation for Top Spent Category
            if (categoryTotals.isNotEmpty()) {
                val topCat = categoryTotals.first()
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TipsAndUpdates,
                                contentDescription = null,
                                tint = DynamicOrange,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Smart Advisory Insight",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Your top expenditure is '${topCat.first}' with ${formatIndianCurrency(topCat.second, currency)}. Trimming this area by 15% would rescue ${formatIndianCurrency(topCat.second * 0.15, currency)} for your savings cycle.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Cash vs Digital Section
            item {
                GlassCard {
                    Text(
                        text = "CASH VS DIGITAL RATIO",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val cash = paymentTotals.first
                    val digital = paymentTotals.second
                    val total = cash + digital
                    val onSurface = MaterialTheme.colorScheme.onSurface

                    if (total == 0.0) {
                        Text(stringResource(id = R.string.reports_empty), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 12.sp)
                    } else {
                        val cashPercent = (cash / total * 100).toInt()
                        val digitalPercent = 100 - cashPercent

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Cash spent: ${formatIndianCurrency(cash, currency)}", color = onSurface, fontSize = 14.sp)
                                Text("Digital: ${formatIndianCurrency(digital, currency)}", color = onSurface, fontSize = 14.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("$cashPercent% Cash", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("$digitalPercent% Digital", color = DynamicGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        val cashColor = MaterialTheme.colorScheme.primary
                        val digitalColor = DynamicGreen
                        // Custom Ratio bar drawn on Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(16.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ) {
                            val cashFraction = (cash / total).toFloat()
                            val cashWidth = size.width * cashFraction
                            val digitalWidth = size.width - cashWidth

                            // Cash segment
                            drawRect(
                                color = cashColor,
                                size = Size(cashWidth, size.height)
                            )

                            // Digital segment
                            drawRect(
                                color = digitalColor,
                                topLeft = Offset(cashWidth, 0f),
                                size = Size(digitalWidth, size.height)
                            )
                        }
                    }
                }
            }

            // Income vs Expense comparison Card
            item {
                GlassCard {
                    Text(
                        text = "INCOME VS EXPENSE TREND",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    val income = filteredTxs.filter { it.category == "Salary" }.sumOfAmount()
                    val expense = filteredTxs.filter { it.category != "Salary" }.sumOfAmount()
                    val onSurface = MaterialTheme.colorScheme.onSurface

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Income", color = onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                            Text(formatIndianCurrency(income, currency), color = DynamicGreen, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Expense", color = onSurface.copy(alpha = 0.5f), fontSize = 11.sp)
                            Text(formatIndianCurrency(expense, currency), color = DynamicRed, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
                com.example.monetization.AdMobBannerAd(isPremium = isPremiumUser)
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

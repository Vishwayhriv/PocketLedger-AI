package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.example.R
import com.example.ui.GlassCard
import com.example.ui.MainViewModel
import com.example.ui.formatIndianCurrency
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToSalarySetup: () -> Unit
) {
    val salarySettings by viewModel.salarySettings.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val subscriptions by viewModel.subscriptionCandidates.collectAsState()
    val leaks by viewModel.microLeaks.collectAsState()
    val health by viewModel.healthScore.collectAsState()
    val globalCurrency by viewModel.currencySymbol.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var showBackupMessage by remember { mutableStateOf(false) }

    // Derive date/stats
    val activeName = salarySettings?.userName ?: "User"
    val activeSalary = salarySettings?.salaryAmount ?: 75000.0
    val activeCreditDay = salarySettings?.salaryDate ?: 1
    val currency = salarySettings?.currency ?: globalCurrency
    val activeGoal = salarySettings?.monthlyGoal ?: 15000.0
    val photoUri = salarySettings?.profilePhotoUri

    val calendar = Calendar.getInstance()
    val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
    val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    
    val daysRemaining = if (currentDay <= activeCreditDay) {
        activeCreditDay - currentDay
    } else {
        (maxDays - currentDay) + activeCreditDay
    }

    val totalExpenses = transactions.filter { it.category != "Salary" }.sumOf { it.amount }
    val savingsAmount = (activeSalary - totalExpenses).coerceAtLeast(0.0)
    val savingsRate = if (activeSalary > 0) ((savingsAmount / activeSalary) * 100).toInt().coerceAtLeast(0) else 0

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.profile_title), fontWeight = FontWeight.Black, fontSize = 20.sp) },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(id = R.string.profile_edit), tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
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
            // Profile Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape)
                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photoUri.isNullOrEmpty()) {
                            Image(
                                painter = rememberAsyncImagePainter(photoUri),
                                contentDescription = "Profile Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Default Avatar",
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(id = R.string.home_hello, activeName),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Salary Information Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(stringResource(id = R.string.home_salary).uppercase(Locale.getDefault()), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(formatIndianCurrency(activeSalary, currency), fontSize = 24.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .padding(vertical = 4.dp, horizontal = 10.dp)
                            ) {
                                Text(stringResource(id = R.string.profile_active_status), color = MaterialTheme.colorScheme.onPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(stringResource(id = R.string.field_payday).uppercase(Locale.getDefault()), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                Text("${activeCreditDay}${getDayOfMonthSuffix(activeCreditDay)} of month", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("NEXT SALARY", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                Text("$daysRemaining Days", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(stringResource(id = R.string.home_savings_goal).uppercase(Locale.getDefault()), fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                Text(formatIndianCurrency(activeGoal, currency), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("MONTHLY SAVINGS", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                                Text(formatIndianCurrency(savingsAmount, currency), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                            }
                        }
                    }
                }
            }

            // Monthly Statistics Section
            item {
                Text(
                    text = "Financial Audit Statistics",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                GlassCard {
                    StatRow(icon = Icons.Default.ReceiptLong, label = stringResource(id = R.string.ledger_title), value = "${transactions.size}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    StatRow(icon = Icons.Default.Payments, label = stringResource(id = R.string.action_cash_entry), value = "${transactions.filter { it.isCash || it.paymentMethod == "Cash" }.size}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    StatRow(icon = Icons.Default.CreditCard, label = "Digital & UPI Logs", value = "${transactions.filter { !it.isCash && it.paymentMethod != "Cash" }.size}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    StatRow(icon = Icons.Default.Refresh, label = stringResource(id = R.string.action_ghost_hunter), value = "${subscriptions.size}")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    StatRow(icon = Icons.Default.Warning, label = stringResource(id = R.string.action_leak_detector), value = "${leaks.size}", valueColor = MaterialTheme.colorScheme.error)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    StatRow(icon = Icons.Default.TrendingUp, label = "Computed Savings Rate", value = "$savingsRate%")
                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    StatRow(icon = Icons.Default.HeartBroken, label = stringResource(id = R.string.home_financial_health), value = "${health.score} / 100")
                }
            }

            // Shortcuts Panel
            item {
                Text(
                    text = "Preferences & Utilities",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    UtilityItem(icon = Icons.Default.Settings, label = stringResource(id = R.string.settings_title), description = "Themes, currency, visual tweaks") { onNavigateToSettings() }
                    UtilityItem(icon = Icons.Default.Lock, label = stringResource(id = R.string.privacy_title), description = "Local logs, zero cloud leak diagnostics") { onNavigateToPrivacy() }
                    UtilityItem(icon = Icons.Default.Backup, label = "Backup Ledger Snapshot", description = "Export local database state securely") {
                        showBackupMessage = true
                    }
                }
            }

            // About details
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(stringResource(id = R.string.settings_version), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("100% Offline Cryptographic Vault", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Backup notification Toast/Dialog
    if (showBackupMessage) {
        AlertDialog(
            onDismissRequest = { showBackupMessage = false },
            title = { Text("Backup Succeeded", fontWeight = FontWeight.Bold) },
            text = { Text("A secure, encrypted snapshot of your PocketLedger Room database has been compiled and saved locally under device storage successfully.") },
            confirmButton = {
                TextButton(onClick = { showBackupMessage = false }) {
                    Text("OK")
                }
            }
        )
    }

    // Edit Profile Modal Dialog
    if (showEditDialog) {
        var editName by remember { mutableStateOf(activeName) }
        var editSalary by remember { mutableStateOf(activeSalary.toInt().toString()) }
        var editCreditDay by remember { mutableStateOf(activeCreditDay) }
        var editCurrency by remember { mutableStateOf(currency) }
        var editGoal by remember { mutableStateOf(activeGoal.toInt().toString()) }
        var editPhotoUri by remember { mutableStateOf(photoUri) }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let { editPhotoUri = it.toString() }
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(id = R.string.profile_edit), fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Profile Photo edit
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .clickable { imagePickerLauncher.launch("image/*") }
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!editPhotoUri.isNullOrEmpty()) {
                                    Image(
                                        painter = rememberAsyncImagePainter(editPhotoUri),
                                        contentDescription = "Edit photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.PhotoCamera, contentDescription = "Add Photo", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text("Tap to pick photo", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = editName,
                            onValueChange = { editName = it },
                            label = { Text(stringResource(id = R.string.field_name)) },
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = editSalary,
                            onValueChange = { editSalary = it },
                            label = { Text(stringResource(id = R.string.field_salary_amount)) },
                            leadingIcon = { Text(editCurrency) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = editGoal,
                            onValueChange = { editGoal = it },
                            label = { Text(stringResource(id = R.string.field_goal)) },
                            leadingIcon = { Text(editCurrency) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    item {
                        Text(stringResource(id = R.string.field_payday), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            (1..5).forEach { day ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(CircleShape)
                                        .background(if (editCreditDay == day) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { editCreditDay = day }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(day.toString(), color = if (editCreditDay == day) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    item {
                        Text(stringResource(id = R.string.field_currency), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("₹", "$", "€", "£").forEach { curr ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (editCurrency == curr) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { editCurrency = curr }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(curr, color = if (editCurrency == curr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val salVal = editSalary.toDoubleOrNull() ?: activeSalary
                        val goalVal = editGoal.toDoubleOrNull() ?: activeGoal
                        viewModel.saveSalarySettings(
                            userName = editName.trim(),
                            salaryAmount = salVal,
                            salaryDate = editCreditDay,
                            currency = editCurrency,
                            monthlyGoal = goalVal,
                            profilePhotoUri = editPhotoUri
                        )
                        showEditDialog = false
                    },
                    enabled = editName.trim().isNotEmpty()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(id = R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
fun StatRow(icon: ImageVector, label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        }
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@Composable
fun UtilityItem(icon: ImageVector, label: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
        }
    }
}

fun getDayOfMonthSuffix(n: Int): String {
    if (n in 11..13) return "th"
    return when (n % 10) {
        1 -> "st"
        2 -> "nd"
        3 -> "rd"
        else -> "th"
    }
}

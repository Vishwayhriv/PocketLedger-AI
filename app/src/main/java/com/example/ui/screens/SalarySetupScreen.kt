package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.MainViewModel
import com.example.ui.theme.CurrencyVisualTransformation

fun filterNumericInput(input: String): String {
    val trimmed = input.trim()
    var dotCount = 0
    return trimmed.filter { char ->
        if (char == '.') {
            dotCount++
            dotCount <= 1
        } else {
            char.isDigit()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalarySetupScreen(
    viewModel: MainViewModel,
    onFinished: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    
    // Form fields
    var name by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("₹") }
    var salaryAmount by remember { mutableStateOf("") }
    var salaryDate by remember { mutableStateOf(1) }
    var savingsGoal by remember { mutableStateOf("") }

    val savedSettings by viewModel.salarySettings.collectAsState()

    // Prepopulate if settings already exist
    LaunchedEffect(savedSettings) {
        savedSettings?.let {
            if (name.isEmpty()) name = it.userName
            if (salaryAmount.isEmpty()) salaryAmount = it.salaryAmount.toInt().toString()
            salaryDate = it.salaryDate
            if (savingsGoal.isEmpty()) savingsGoal = it.monthlyGoal.toInt().toString()
            currency = it.currency
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(id = R.string.setup_title), fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Step indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepDot(active = step >= 1, label = "Profile")
                Spacer(modifier = Modifier.width(16.dp))
                HorizontalDivider(
                    modifier = Modifier.width(32.dp),
                    thickness = 2.dp,
                    color = if (step >= 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                StepDot(active = step >= 2, label = "Salary")
                Spacer(modifier = Modifier.width(16.dp))
                HorizontalDivider(
                    modifier = Modifier.width(32.dp),
                    thickness = 2.dp,
                    color = if (step >= 3) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.width(16.dp))
                StepDot(active = step >= 3, label = "Goal")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Step Content Box
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                when (step) {
                    1 -> Step1Profile(
                        name = name,
                        onNameChange = { name = it },
                        selectedCurrency = currency,
                        onCurrencySelected = { currency = it }
                    )
                    2 -> Step2Salary(
                        salary = salaryAmount,
                        onSalaryChange = { salaryAmount = it },
                        selectedDay = salaryDate,
                        onDaySelected = { salaryDate = it },
                        currency = currency
                    )
                    3 -> Step3Goal(
                        goal = savingsGoal,
                        onGoalChange = { savingsGoal = it },
                        currency = currency,
                        salary = salaryAmount
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (step > 1) {
                    OutlinedButton(
                        onClick = { step-- },
                        modifier = Modifier
                            .height(56.dp)
                            .weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.dialog_cancel),
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2
                        )
                    }
                }

                Button(
                    onClick = {
                        if (step < 3) {
                            if (step == 1 && name.trim().isEmpty()) {
                                // Validate step 1
                            } else if (step == 2 && salaryAmount.trim().isEmpty()) {
                                // Validate step 2
                            } else {
                                step++
                            }
                        } else {
                            // Finish and Save
                            val salaryVal = (salaryAmount.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100000000.0)
                            val goalVal = (savingsGoal.toDoubleOrNull() ?: 0.0).coerceIn(0.0, 100000000.0)
                            viewModel.saveSalarySettings(
                                userName = name.trim(),
                                salaryAmount = salaryVal,
                                salaryDate = salaryDate,
                                currency = currency,
                                monthlyGoal = goalVal
                            )
                            onFinished()
                        }
                    },
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    enabled = when (step) {
                        1 -> name.trim().isNotEmpty()
                        2 -> {
                            val sal = salaryAmount.trim().toDoubleOrNull() ?: 0.0
                            salaryAmount.trim().isNotEmpty() && sal > 0 && sal <= 100000000.0
                        }
                        else -> {
                            val salDouble = salaryAmount.trim().toDoubleOrNull() ?: 0.0
                            val goalDouble = savingsGoal.trim().toDoubleOrNull() ?: 0.0
                            savingsGoal.trim().isNotEmpty() && goalDouble >= 0 && goalDouble <= salDouble && salDouble > 0 && goalDouble <= 100000000.0
                        }
                    }
                ) {
                    Text(
                        text = if (step == 3) stringResource(id = R.string.button_save_profile) else "Continue",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
fun StepDot(active: Boolean, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            if (active) {
                Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
    }
}

@Composable
fun Step1Profile(
    name: String,
    onNameChange: (String) -> Unit,
    selectedCurrency: String,
    onCurrencySelected: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(id = R.string.setup_subtitle), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text(stringResource(id = R.string.field_name)) },
            placeholder = { Text("e.g. Vishwa") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(stringResource(id = R.string.field_currency), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val currencies = listOf("₹", "$", "€", "£", "¥")
            currencies.forEach { curr ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (selectedCurrency == curr) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .clickable { onCurrencySelected(curr) }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = curr,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedCurrency == curr) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun Step2Salary(
    salary: String,
    onSalaryChange: (String) -> Unit,
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
    currency: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(id = R.string.field_salary_amount), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = salary,
            onValueChange = { onSalaryChange(filterNumericInput(it)) },
            label = { Text(stringResource(id = R.string.field_salary_amount)) },
            leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            visualTransformation = CurrencyVisualTransformation(currency),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(stringResource(id = R.string.field_payday), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        // Grid day selection
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp)
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items((1..31).toList()) { day ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(
                                if (selectedDay == day) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { onDaySelected(day) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedDay == day) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Step3Goal(
    goal: String,
    onGoalChange: (String) -> Unit,
    currency: String,
    salary: String
) {
    val salDouble = salary.toDoubleOrNull() ?: 0.0
    val goalDouble = goal.toDoubleOrNull() ?: 0.0
    val savingsPercent = if (salDouble > 0) ((goalDouble / salDouble) * 100).toInt() else 0
    val isGoalInvalid = goalDouble > salDouble

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(stringResource(id = R.string.field_goal), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = goal,
            onValueChange = { onGoalChange(filterNumericInput(it)) },
            label = { Text(stringResource(id = R.string.field_goal)) },
            leadingIcon = { Icon(Icons.Default.TrackChanges, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            singleLine = true,
            isError = isGoalInvalid,
            visualTransformation = CurrencyVisualTransformation(currency),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (isGoalInvalid) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Savings goal cannot be greater than your monthly salary. Please enter a valid amount.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        if (!isGoalInvalid && goalDouble > 0 && salDouble > 0) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "You are aiming to save $savingsPercent% of your monthly income. Outstanding!",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

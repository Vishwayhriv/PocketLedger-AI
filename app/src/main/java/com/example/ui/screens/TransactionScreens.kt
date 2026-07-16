package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.TransactionEntity
import com.example.ui.MainViewModel
import com.example.ui.formatIndianCurrency
import com.example.ui.theme.CurrencyVisualTransformation
import com.example.ui.theme.LocalDarkTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// --- UTILS FOR PREMIUM CATEGORY STYLE ---
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun getCategoryEmoji(category: String): String {
    return when (category.lowercase(Locale.ROOT).trim()) {
        "food" -> "🍔"
        "shopping" -> "🛒"
        "rent" -> "🏠"
        "bills" -> "⚡"
        "travel" -> "🚗"
        "entertainment" -> "🎬"
        "health", "medical" -> "🏥"
        "education" -> "🎓"
        "office" -> "💼"
        "emi" -> "💳"
        "gift" -> "🎁"
        "salary", "income" -> "💰"
        "subscriptions", "upi" -> "📱"
        "cash" -> "💵"
        else -> "📦"
    }
}

fun getCategoryColor(category: String): Color {
    return when (category.lowercase(Locale.ROOT).trim()) {
        "food" -> Color(0xFFF97316) // Orange
        "shopping" -> Color(0xFFEC4899) // Pink
        "rent" -> Color(0xFF3B82F6) // Blue
        "bills" -> Color(0xFFEAB308) // Yellow
        "travel" -> Color(0xFF14B8A6) // Teal
        "entertainment" -> Color(0xFF8B5CF6) // Purple
        "health", "medical" -> Color(0xFFEF4444) // Red
        "education" -> Color(0xFF10B981) // Emerald
        "office" -> Color(0xFF64748B) // Slate
        "emi" -> Color(0xFF6366F1) // Indigo
        "gift" -> Color(0xFFD946EF) // Fuchsia
        "salary" -> Color(0xFF10B981) // Green
        "subscriptions" -> Color(0xFF06B6D4) // Cyan
        else -> Color(0xFF94A3B8) // Light slate
    }
}

fun getCategoryBgColor(category: String, isDark: Boolean): Color {
    val baseColor = getCategoryColor(category)
    return if (isDark) {
        baseColor.copy(alpha = 0.15f)
    } else {
        baseColor.copy(alpha = 0.12f)
    }
}

// --- PREMIUM CUSTOM CATEGORY CHIP ---
@Composable
fun CategoryCircleItem(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
        label = "category_scale"
    )
    val isDark = LocalDarkTheme.current
    val baseColor = getCategoryColor(label)
    val circleBg = if (isSelected) {
        baseColor
    } else {
        if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    val shadowElevation = if (isSelected) 6.dp else 2.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .padding(4.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .size(56.dp)
                .shadow(shadowElevation, CircleShape, clip = false)
                .clip(CircleShape)
                .background(circleBg)
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape
                )
        ) {
            Text(
                text = emoji,
                fontSize = 26.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
            maxLines = 1
        )
    }
}

// --- PREMIUM CUSTOM CATEGORY CHIP ---
@Composable
fun PremiumCategoryChip(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = LocalDarkTheme.current
    val baseColor = getCategoryColor(label)
    
    val containerColor = if (isSelected) {
        baseColor.copy(alpha = 0.15f)
    } else {
        if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    
    val borderColor = if (isSelected) baseColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val contentColor = if (isSelected) baseColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, fontSize = 16.sp)
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- CUSTOM ANIMATED SEGMENTED CONTROL ---
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val bg = if (isDark) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val isSelected = selectedOption == option
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "segment_bg"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                label = "segment_text"
            )
            
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(containerColor)
                    .clickable { onOptionSelected(option) }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = option,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// --- PREMIUM TRANSACTION CARD REDESIGN ---
@Composable
fun PremiumTransactionCard(
    tx: TransactionEntity,
    currency: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalDarkTheme.current
    val isSalary = tx.category.equals("Salary", ignoreCase = true)
    
    val cardBg = MaterialTheme.colorScheme.surface
    
    val formattedDate = remember(tx.date) {
        val cal = Calendar.getInstance()
        val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
        if (cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
            cal.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)) {
            "Today"
        } else {
            val yesterday = Calendar.getInstance().apply { add(Calendar.DATE, -1) }
            if (yesterday.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)) {
                "Yesterday"
            } else {
                SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(tx.date))
            }
        }
    }

    // Soft, premium, non-dominant color scheme
    val indicatorColor = if (isSalary) Color(0xFF10B981) else Color(0xFFF43F5E).copy(alpha = 0.6f)
    val amountColor = if (isSalary) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurface
    val amountSign = if (isSalary) "+" else "-"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp), clip = false),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp, 
            color = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color(0xFFE2E8F0).copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min), // to align the indicator height with the card
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Elegant vertical indicator strip instead of loud colors
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(indicatorColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon with a modern, softer background
                val emoji = getCategoryEmoji(tx.category)
                val iconBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color(0xFFF1F5F9)
                
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconBg)
                ) {
                    Text(text = emoji, fontSize = 20.sp)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tx.merchant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tx.category,
                            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = " • ",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            fontSize = 11.sp
                        )
                        Text(
                            text = tx.paymentMethod,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$amountSign${formatIndianCurrency(tx.amount, currency)}",
                        color = amountColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

// --- REDESIGNED LEDGER SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionTimelineScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToCashEntry: (String) -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val salaries by viewModel.salaryRecords.collectAsState()
    val salarySettings by viewModel.salarySettings.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("All") }
    val recentSearches = remember { mutableStateListOf("UPI", "Cash", "Swiggy", "Salary") }
    var showAddTransactionSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Dynamic Calculations
    val totalSalaryIncome = remember(salaries, salarySettings) {
        if (salaries.isNotEmpty()) {
            salaries.sumOf { it.amount }
        } else {
            salarySettings?.salaryAmount ?: 0.0
        }
    }
    val totalExpense = remember(transactions) {
        transactions.filter { !it.category.equals("Salary", ignoreCase = true) }.sumOf { it.amount }
    }
    val totalSavings = totalSalaryIncome - totalExpense

    val filteredTransactions = remember(transactions, searchQuery, activeFilter) {
        transactions.filter { tx ->
            val matchQuery = if (searchQuery.isEmpty()) {
                true
            } else {
                val amtStr = tx.amount.toString()
                val dateSdf = SimpleDateFormat("dd MMMM yyyy MMMM EEEE dd/MM/yyyy", Locale.US)
                val formattedDateStr = dateSdf.format(Date(tx.date))
                
                tx.merchant.contains(searchQuery, ignoreCase = true) ||
                tx.notes.contains(searchQuery, ignoreCase = true) ||
                tx.category.contains(searchQuery, ignoreCase = true) ||
                tx.paymentMethod.contains(searchQuery, ignoreCase = true) ||
                amtStr.contains(searchQuery) ||
                formattedDateStr.contains(searchQuery, ignoreCase = true)
            }
            
            val matchFilter = when (activeFilter) {
                "All" -> true
                "Income" -> tx.category.equals("Salary", ignoreCase = true)
                "Expense" -> !tx.category.equals("Salary", ignoreCase = true)
                else -> tx.category.equals(activeFilter, ignoreCase = true)
            }
            
            matchQuery && matchFilter
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.ledger_title), fontWeight = FontWeight.Black, fontSize = 22.sp) },
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddTransactionSheet = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Transaction") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            // Modern Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it 
                    if (it.isNotEmpty() && !recentSearches.contains(it) && recentSearches.size < 6) {
                        recentSearches.add(0, it)
                    }
                },
                placeholder = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Search transactions... ", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 14.sp)
                        Text("🎙", fontSize = 14.sp)
                    }
                },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_ledger_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = if (LocalDarkTheme.current) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(28.dp),
                singleLine = true
            )

            // Recent Searches Row
            if (recentSearches.isNotEmpty() && searchQuery.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Recent:",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentSearches.toList()) { search ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { searchQuery = search }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = search,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Modern, Compact Monthly Summary Bar
            val isDark = LocalDarkTheme.current
            val summaryBarBg = if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = summaryBarBg),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Income
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("INCOME", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(formatIndianCurrency(totalSalaryIncome, currency), color = com.example.ui.theme.DynamicGreen, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }

                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))

                    // Expenses
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("EXPENSES", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(formatIndianCurrency(totalExpense, currency), color = com.example.ui.theme.DynamicRed, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }

                    // Divider
                    Box(modifier = Modifier.width(1.dp).height(24.dp).background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))

                    // Saved
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("NET SAVED", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(formatIndianCurrency(totalSavings, currency), color = if (totalSavings >= 0) com.example.ui.theme.DynamicBlue else com.example.ui.theme.DynamicRed, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pill Filters
            val filterOptions = listOf("All", "Income", "Expense", "Food", "Travel", "Shopping", "Bills", "Subscriptions")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filterOptions) { filter ->
                    val isSelected = activeFilter == filter
                    val containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        if (LocalDarkTheme.current) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    }
                    val contentColor = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    }
                    
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(containerColor)
                            .clickable { activeFilter = filter }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter,
                            color = contentColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Transaction list header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${filteredTransactions.size} transactions",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (filteredTransactions.isNotEmpty()) {
                    Text(
                        text = "Sum: ${formatIndianCurrency(filteredTransactions.sumOf { it.amount }, currency)}",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Timeline List
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No matching transactions found",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredTransactions, key = { it.id }) { tx ->
                        SwipeToDismissBox(
                            state = rememberSwipeToDismissBoxState(
                                confirmValueChange = { dismissValue ->
                                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                        viewModel.deleteTransaction(tx.id)
                                        true
                                    } else {
                                        false
                                    }
                                }
                            ),
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFEF4444))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.White
                                    )
                                }
                            },
                            content = {
                                PremiumTransactionCard(
                                    tx = tx,
                                    currency = currency,
                                    onDelete = { viewModel.deleteTransaction(tx.id) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            enableDismissFromStartToEnd = false
                        )
                    }
                }
            }
        }
    }

    // Floating Action Button Options Sheet
    if (showAddTransactionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddTransactionSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Add New Transaction",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                val options = listOf(
                    Triple("Expense", Icons.Default.RemoveCircle, Color(0xFFEF4444)),
                    Triple("Income", Icons.Default.AddCircle, Color(0xFF10B981)),
                    Triple("Transfer", Icons.Default.SwapHoriz, Color(0xFF3B82F6)),
                    Triple("Cash", Icons.Default.Money, Color(0xFF10B981))
                )

                options.forEach { (label, icon, color) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                showAddTransactionSheet = false
                                onNavigateToCashEntry(label)
                            }
                            .background(color.copy(alpha = 0.08f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(label, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// --- BEAUTIFUL MODERN CATEGORY CHIP ---
@Composable
fun TransactionCategoryChip(
    label: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val baseColor = getCategoryColor(label)
    val isDark = LocalDarkTheme.current
    
    val containerColor = if (isSelected) {
        baseColor.copy(alpha = 0.15f)
    } else {
        if (isDark) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    
    val borderColor = if (isSelected) {
        baseColor
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
    }
    
    val contentColor = if (isSelected) {
        baseColor
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = emoji,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = if (label.length > 10) 10.sp else 12.sp,
                lineHeight = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 2,
                overflow = TextOverflow.Clip,
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
    }
}

// --- REDESIGNED CASH ENTRY SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CashEntryScreen(
    viewModel: MainViewModel,
    initialType: String = "Expense",
    onBack: () -> Unit
) {
    var amountStr by remember { mutableStateOf("") }
    var merchant by remember { mutableStateOf("") }
    var selectedCategory by remember(initialType) { 
        mutableStateOf(if (initialType.equals("Income", ignoreCase = true)) "Salary" else "Food") 
    }
    var notes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var selectedPaymentMethod by remember(initialType) { 
        mutableStateOf(if (initialType.equals("Cash", ignoreCase = true)) "Cash" else "UPI") 
    }
    var showDatePicker by remember { mutableStateOf(false) }

    val currency by viewModel.currencySymbol.collectAsState()
    val isPremiumUser by viewModel.isPremiumUser.collectAsState()

    val categoriesWithEmojis = listOf(
        "Food" to "🍔",
        "Shopping" to "🛒",
        "Rent" to "🏠",
        "Bills" to "⚡",
        "Travel" to "🚗",
        "Entertainment" to "🎬",
        "Medical" to "🏥",
        "Education" to "🎓",
        "Investment" to "📈",
        "Salary" to "💰",
        "Fuel" to "⛽",
        "EMI" to "💳",
        "Transfer" to "🔄",
        "ATM" to "🏪",
        "Recharge" to "📱",
        "Cash" to "💵",
        "Other" to "📦"
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val todayFormatted = remember(selectedDate) {
        val cal = Calendar.getInstance()
        val txCal = Calendar.getInstance().apply { timeInMillis = selectedDate }
        val isToday = cal.get(Calendar.YEAR) == txCal.get(Calendar.YEAR) &&
                      cal.get(Calendar.DAY_OF_YEAR) == txCal.get(Calendar.DAY_OF_YEAR)
        val dayLabel = if (isToday) "Today" else "Selected Date"
        dayLabel to SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(selectedDate))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Record Ledger", fontWeight = FontWeight.Black) },
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
        bottomBar = {
            Surface(
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val parsedAmount = amountStr.toDoubleOrNull()
                            if (parsedAmount == null || parsedAmount <= 0 || parsedAmount.isNaN() || parsedAmount.isInfinite()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Please enter a valid amount")
                                }
                                return@Button
                            }
                            if (parsedAmount > 100000000.0) {
                                scope.launch {
                                    snackbarHostState.showSnackbar("Invalid amount: cannot exceed ₹10,00,00,000")
                                }
                                return@Button
                            }
                            
                            val finalMerchant = merchant.trim().ifBlank { "Cash Expense" }

                            viewModel.addCashTransaction(
                                amount = parsedAmount,
                                merchant = finalMerchant,
                                category = selectedCategory,
                                date = selectedDate,
                                paymentMethod = selectedPaymentMethod,
                                notes = notes.trim(),
                                isCash = selectedPaymentMethod == "Cash"
                            )

                            val activity = context as? android.app.Activity
                            if (activity != null) {
                                com.example.monetization.AdMobManager.trackCompletedTransaction(activity, isPremiumUser)
                            }

                            onBack()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .testTag("save_cash_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Save Expense", fontSize = 16.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Screen Header: Expense Title & Elegant Date
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val headerTitle = when (initialType) {
                    "Expense" -> "Record Expense"
                    "Income" -> "Record Income"
                    "Transfer" -> "Record Transfer"
                    "Cash" -> "Record Cash Expense"
                    else -> "Record Transaction"
                }
                Text(
                    text = headerTitle,
                    fontWeight = FontWeight.Black,
                    fontSize = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clickable { showDatePicker = true }
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = todayFormatted.first,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = todayFormatted.second,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Change Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Amount Input Field
            Column {
                Text(
                    text = "Amount",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = filterNumericInput(it) },
                    placeholder = { Text("0.00", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    visualTransformation = CurrencyVisualTransformation(currency),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .testTag("cash_amount_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                // Quick Grid Amount Buttons
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val chunkedAmounts = listOf(
                        listOf(50.0, 100.0, 200.0),
                        listOf(500.0, 1000.0, 2000.0)
                    )
                    chunkedAmounts.forEach { rowAmounts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowAmounts.forEach { amt ->
                                Button(
                                    onClick = {
                                        val currentVal = amountStr.toDoubleOrNull() ?: 0.0
                                        val nextVal = currentVal + amt
                                        amountStr = if (nextVal % 1.0 == 0.0) nextVal.toInt().toString() else String.format("%.2f", nextVal)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                ) {
                                    Text(
                                        text = "+${amt.toInt()}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Category Horizontal Grid Selection
            Column {
                Text(
                    text = "Category",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))
                
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val chunks = categoriesWithEmojis.chunked(3)
                    chunks.forEach { chunk ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chunk.forEach { (catName, emoji) ->
                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    TransactionCategoryChip(
                                        label = catName,
                                        emoji = emoji,
                                        isSelected = selectedCategory == catName,
                                        onClick = { selectedCategory = catName }
                                    )
                                }
                            }
                            // If the last chunk is not full, fill with spacer weight
                            if (chunk.size < 3) {
                                val missing = 3 - chunk.size
                                for (i in 0 until missing) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // Merchant Input
            Column {
                Text(
                    text = "Merchant",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    placeholder = { Text("e.g. Swiggy, Starbucks", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Merchant Icon",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .testTag("cash_merchant_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(20.dp),
                    singleLine = true
                )
            }

            // Payment Method Segmented Control
            Column {
                Text(
                    text = "Payment Method",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                SegmentedControl(
                    options = listOf("Cash", "UPI", "Card", "Bank"),
                    selectedOption = selectedPaymentMethod,
                    onOptionSelected = { selectedPaymentMethod = it }
                )
            }

            // Notes Input (Optional)
            Column {
                Text(
                    text = "Notes (Optional)",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = { Text("Add payment details...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = false,
                    maxLines = 5
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            selectedDate = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.GlassCard
import com.example.ui.MainViewModel
import com.example.ui.formatIndianCurrency
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyFinancialJourneyScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val transactions by viewModel.allTransactions.collectAsState()
    val salaryRecords by viewModel.salaryRecords.collectAsState()
    val salarySettings by viewModel.salarySettings.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    // Group transactions by Month-Year (e.g. "July 2026")
    val chapters = remember(transactions, salaryRecords, salarySettings) {
        val sdfMonthYear = SimpleDateFormat("MMMM yyyy", Locale.US)
        val sdfKey = SimpleDateFormat("yyyy-MM", Locale.US)

        val allMonths = mutableSetOf<String>()
        transactions.forEach {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            allMonths.add(sdfMonthYear.format(cal.time))
        }
        salaryRecords.forEach {
            try {
                val parsed = SimpleDateFormat("yyyy-MM", Locale.US).parse(it.monthYear)
                if (parsed != null) {
                    allMonths.add(sdfMonthYear.format(parsed))
                }
            } catch (e: Exception) {}
        }
        
        if (allMonths.isEmpty()) {
            val currentCal = Calendar.getInstance()
            allMonths.add(sdfMonthYear.format(currentCal.time))
        }

        allMonths.toList().sortedWith { m1, m2 ->
            try {
                val d1 = sdfMonthYear.parse(m1) ?: Date()
                val d2 = sdfMonthYear.parse(m2) ?: Date()
                d2.compareTo(d1) // reverse chronological order (latest first)
            } catch (e: Exception) {
                0
            }
        }.map { monthName ->
            val monthDate = sdfMonthYear.parse(monthName) ?: Date()
            val cal = Calendar.getInstance().apply { time = monthDate }
            val currentMonth = cal.get(Calendar.MONTH)
            val currentYear = cal.get(Calendar.YEAR)

            val monthTxs = transactions.filter {
                val tCal = Calendar.getInstance().apply { timeInMillis = it.date }
                tCal.get(Calendar.MONTH) == currentMonth && tCal.get(Calendar.YEAR) == currentYear
            }

            // Find corresponding salary amount
            val monthKey = sdfKey.format(monthDate)
            val monthSalaryRecord = salaryRecords.find { it.monthYear == monthKey }?.amount
            val salary = monthSalaryRecord ?: salarySettings?.salaryAmount ?: 3000.0

            val bills = monthTxs.filter { it.category.equals("Bills", ignoreCase = true) }.sumOf { it.amount }
            val shopping = monthTxs.filter { it.category.equals("Shopping", ignoreCase = true) }.sumOf { it.amount }
            val food = monthTxs.filter { it.category.equals("Food", ignoreCase = true) }.sumOf { it.amount }
            val travel = monthTxs.filter { it.category.equals("Travel", ignoreCase = true) }.sumOf { it.amount }
            val totalSpent = monthTxs.filter { it.category != "Salary" }.sumOf { it.amount }
            val savings = (salary - totalSpent).coerceAtLeast(0.0)

            JourneyChapter(
                monthName = monthName,
                salary = salary,
                bills = bills,
                shopping = shopping,
                food = food,
                travel = travel,
                savings = savings,
                totalSpent = totalSpent
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Financial Journey", fontWeight = FontWeight.Black) },
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Every month is a chapter in your personal book of financial control. Review your dynamic monthly growth step-by-step.",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }

            if (chapters.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(60.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Your journey begins today! Log your first transaction.", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f), fontSize = 14.sp)
                        }
                    }
                }
            } else {
                items(chapters) { ch ->
                    var isExpanded by remember { mutableStateOf(true) }

                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Month Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isExpanded = !isExpanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = ch.monthName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (ch.savings > 0) "Saved: ${formatIndianCurrency(ch.savings, currency)}" else "Expenses logged",
                                        fontSize = 12.sp,
                                        color = if (ch.savings > 0) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }

                            if (isExpanded) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                Spacer(modifier = Modifier.height(16.dp))

                                // Chapter Timeline
                                Column {
                                    JourneyTimelineNode(
                                        icon = Icons.Default.Payments,
                                        title = "Salary Credited",
                                        subtitle = "Base income configured for this cycle",
                                        value = "+${formatIndianCurrency(ch.salary, currency)}",
                                        color = Color(0xFF10B981),
                                        isFirst = true
                                    )

                                    JourneyTimelineNode(
                                        icon = Icons.Default.Receipt,
                                        title = "Utility Bills",
                                        subtitle = "Power, rent, phone, regular dues",
                                        value = if (ch.bills > 0) "-${formatIndianCurrency(ch.bills, currency)}" else "No bill payments",
                                        color = if (ch.bills > 0) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )

                                    JourneyTimelineNode(
                                        icon = Icons.Default.ShoppingBag,
                                        title = "Shopping",
                                        subtitle = "E-commerce, apparel, tech, items",
                                        value = if (ch.shopping > 0) "-${formatIndianCurrency(ch.shopping, currency)}" else "No shopping logs",
                                        color = if (ch.shopping > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )

                                    JourneyTimelineNode(
                                        icon = Icons.Default.Restaurant,
                                        title = "Food & Delivery",
                                        subtitle = "Groceries, cafes, food orders",
                                        value = if (ch.food > 0) "-${formatIndianCurrency(ch.food, currency)}" else "No food purchases",
                                        color = if (ch.food > 0) Color(0xFFEC4899) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )

                                    JourneyTimelineNode(
                                        icon = Icons.Default.DirectionsCar,
                                        title = "Travel & Transit",
                                        subtitle = "Commutes, cabs, fuel, parking",
                                        value = if (ch.travel > 0) "-${formatIndianCurrency(ch.travel, currency)}" else "No travel logs",
                                        color = if (ch.travel > 0) Color(0xFF0EA5E9) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )

                                    JourneyTimelineNode(
                                        icon = Icons.Default.Savings,
                                        title = "Computed Savings",
                                        subtitle = "Retained balance for wealth-building",
                                        value = formatIndianCurrency(ch.savings, currency),
                                        color = Color(0xFF10B981)
                                    )

                                    JourneyTimelineNode(
                                        icon = Icons.Default.AutoAwesome,
                                        title = "AI Reflection",
                                        subtitle = if (ch.savings > ch.salary * 0.3) {
                                            "Excellent discipline! Saving over 30% of income is a major cornerstone of financial freedom."
                                        } else if (ch.savings > 0) {
                                            "A positive savings rate of ${((ch.savings/ch.salary)*100).toInt()}% keeps you ahead of micro leaks. Reduce food or shopping to hit 20% savings."
                                        } else {
                                            "Your spending exceeded your salary. Run an audit on your micro leaks to recover cash balance next month!"
                                        },
                                        value = "Active Coach",
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    JourneyTimelineNode(
                                        icon = Icons.Default.Stars,
                                        title = "Achievements",
                                        subtitle = when {
                                            ch.savings > ch.salary * 0.3 -> "🏆 Ultra Saver: Saved >30% of salary!"
                                            ch.totalSpent == 0.0 -> "🔥 Ledger Init: Setup the ledger successfully!"
                                            ch.food < ch.salary * 0.15 -> "🎯 Dining discipline: Food spending kept low"
                                            else -> "✅ Consistent: Kept transactions recorded"
                                        },
                                        value = "Awarded",
                                        color = Color(0xFFF59E0B)
                                    )

                                    JourneyTimelineNode(
                                        icon = Icons.Default.Flag,
                                        title = "Upcoming Goals",
                                        subtitle = when {
                                            ch.savings > 0 -> "Aim to increase savings rate by 5% and eliminate one subscription."
                                            else -> "Target keeping Food spending below ${formatIndianCurrency(ch.salary * 0.15, currency)} next month."
                                        },
                                        value = "Target Set",
                                        color = Color(0xFF8B5CF6),
                                        isLast = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun JourneyTimelineNode(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    value: String,
    color: Color,
    isFirst: Boolean = false,
    isLast: Boolean = false
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Vertical Timeline Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .weight(if (isFirst) 0.05f else 1f)
                    .background(if (isFirst) Color.Transparent else onSurface.copy(alpha = 0.12f))
            )

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f))
                    .border(1.5.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(12.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(1.5.dp)
                    .weight(if (isLast) 0.05f else 1f)
                    .background(if (isLast) Color.Transparent else onSurface.copy(alpha = 0.12f))
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Text & Value Column
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = onSurface
                )
                Text(
                    text = value,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    color = if (value.startsWith("+")) Color(0xFF10B981) else if (value.startsWith("-")) Color(0xFFEF4444) else onSurface
                )
            }
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = onSurface.copy(alpha = 0.6f),
                lineHeight = 15.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

data class JourneyChapter(
    val monthName: String,
    val salary: Double,
    val bills: Double,
    val shopping: Double,
    val food: Double,
    val travel: Double,
    val savings: Double,
    val totalSpent: Double
)

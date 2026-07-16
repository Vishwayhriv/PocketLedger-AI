package com.example.ui

import kotlin.math.abs
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransactionEntity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import java.text.SimpleDateFormat
import java.util.*

// --- PREMIUM GRADIENTS ---
object WalletGradients {
    val PremiumDark: Brush
        @Composable
        get() = Brush.verticalGradient(
            colors = listOf(MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.surface)
        )
    val GlassCard: Brush
        @Composable
        get() = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
            )
        )
    val NeonGreen: Brush
        @Composable
        get() = Brush.horizontalGradient(
            colors = listOf(com.example.ui.theme.DynamicGreen, com.example.ui.theme.DynamicGreen.copy(alpha = 0.7f))
        )
    val NeonRed: Brush
        @Composable
        get() = Brush.horizontalGradient(
            colors = listOf(com.example.ui.theme.DynamicRed, com.example.ui.theme.DynamicRed.copy(alpha = 0.7f))
        )
    val NeonBlue: Brush
        @Composable
        get() = Brush.horizontalGradient(
            colors = listOf(com.example.ui.theme.DynamicBlue, com.example.ui.theme.DynamicBlue.copy(alpha = 0.7f))
        )
}

// --- GLASSMORPHIC CARD ---
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = com.example.ui.theme.LocalDarkTheme.current
    val containerBg = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    
    val baseModifier = modifier
        .clip(RoundedCornerShape(24.dp))
        .background(containerBg)
        .border(1.dp, borderColor, RoundedCornerShape(24.dp))
        .padding(20.dp)

    if (onClick != null) {
        Column(
            modifier = baseModifier.clickable(onClick = onClick),
            content = content
        )
    } else {
        Column(
            modifier = baseModifier,
            content = content
        )
    }
}

// --- DYNAMIC HEALTH RING ---
@Composable
fun HealthScoreRing(
    score: Int,
    grade: String,
    size: Dp = 100.dp,
    strokeWidth: Dp = 8.dp
) {
    val animatedScore = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animatedScore.animateTo(
            targetValue = score.toFloat(),
            animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow)
        )
    }

    val onSurface = MaterialTheme.colorScheme.onSurface
    val trackColor = onSurface.copy(alpha = 0.1f)

    val green = com.example.ui.theme.DynamicGreen
    val orange = com.example.ui.theme.DynamicOrange
    val red = com.example.ui.theme.DynamicRed
    val primaryColor = MaterialTheme.colorScheme.primary

    val color = when {
        score >= 85 -> green
        score >= 70 -> primaryColor
        score >= 50 -> orange
        else -> red
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Background arc
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // Colored gauge arc
            val sweep = (animatedScore.value / 100f) * 360f
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedScore.value.toInt()}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = onSurface
            )
            Text(
                text = "Grade $grade",
                fontSize = 11.sp,
                color = onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// --- MERCHANT ICON AVATAR ---
fun getMerchantOrCategoryIcon(name: String, category: String?): androidx.compose.ui.graphics.vector.ImageVector? {
    val lowerName = name.lowercase().trim()
    val lowerCat = (category ?: "").lowercase().trim()

    return when {
        lowerName.contains("swiggy") || lowerName.contains("zomato") || lowerCat == "food" -> Icons.Default.Fastfood
        lowerName.contains("netflix") || lowerName.contains("spotify") || lowerCat == "entertainment" -> Icons.Default.Tv
        lowerName.contains("uber") || lowerName.contains("ola") || lowerCat == "travel" -> Icons.Default.DirectionsCar
        lowerName.contains("salary") || lowerCat == "salary" -> Icons.Default.Payments
        lowerName.contains("fuel") || lowerName.contains("petrol") || lowerCat == "fuel" -> Icons.Default.LocalGasStation
        lowerName.contains("medical") || lowerName.contains("hospital") || lowerCat == "medical" -> Icons.Default.LocalHospital
        lowerName.contains("recharge") || lowerCat == "recharge" -> Icons.Default.PhoneAndroid
        lowerName.contains("shopping") || lowerCat == "shopping" -> Icons.Default.ShoppingCart
        lowerName.contains("bill") || lowerCat == "bills" -> Icons.Default.FlashOn
        lowerName.contains("investment") || lowerCat == "investment" -> Icons.Default.TrendingUp
        else -> null
    }
}

@Composable
fun MerchantAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    category: String? = null
) {
    val cleanName = name.trim().ifEmpty { "?" }
    
    // Choose dynamic pairs consistently based on initial letter - professional dynamic Material You contrast pairings
    val dynamicPairs = listOf(
        MaterialTheme.colorScheme.primary to MaterialTheme.colorScheme.onPrimary,
        MaterialTheme.colorScheme.secondary to MaterialTheme.colorScheme.onSecondary,
        MaterialTheme.colorScheme.tertiary to MaterialTheme.colorScheme.onTertiary,
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
        com.example.ui.theme.DynamicGreen to Color.White,
        com.example.ui.theme.DynamicBlue to Color.White,
        com.example.ui.theme.DynamicOrange to Color.White,
        com.example.ui.theme.DynamicRed to Color.White
    )
    val colorIndex = abs(cleanName.hashCode() % dynamicPairs.size)
    val (avatarBg, avatarTextColor) = dynamicPairs[colorIndex]

    val icon = getMerchantOrCategoryIcon(cleanName, category)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarBg)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = avatarTextColor,
                modifier = Modifier.size(size * 0.5f)
            )
        } else {
            val initial = cleanName.first().uppercaseChar()
            Text(
                text = initial.toString(),
                color = avatarTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

fun getCategoryEmoji(category: String): String {
    return when (category.lowercase(java.util.Locale.ROOT).trim()) {
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

// --- DYNAMIC BAR CHART ---
@Composable
fun BarChart(
    categories: List<String>,
    values: List<Double>,
    modifier: Modifier = Modifier,
    currency: String = "$"
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    if (categories.isEmpty() || values.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("No Chart Data available", color = onSurface.copy(alpha = 0.5f), fontSize = 14.sp)
        }
        return
    }

    val maxValue = values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
    val totalSum = values.sumOfBigDecimal().coerceAtLeast(1.0)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        categories.zip(values).forEach { (category, value) ->
            val percentage = (value / totalSum) * 100.0
            val fraction = (value / maxValue).toFloat().coerceIn(0f, 1f)
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category with Icon/Emoji
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        ) {
                            Text(
                                text = if (category.equals("Salary", ignoreCase = true)) "💼" else getCategoryEmoji(category),
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = category,
                            color = onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Value and Percentage (High Contrast & Clear spacing)
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = formatIndianCurrency(value, currency),
                            color = onSurface,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", percentage)}%",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar spanning full container width
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(onSurface.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                    )
                }
            }
        }
    }
}

// --- TIMELINE ELEMENT ---
@Composable
fun TimelineNode(
    title: String,
    amount: String,
    date: String,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    indicatorColor: Color? = null
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outlineColor = onSurface.copy(alpha = 0.15f)
    val resolvedIndicatorColor = indicatorColor ?: com.example.ui.theme.DynamicGreen

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Vertical line with node
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            // Upper line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(if (isFirst) 0.1f else 1f)
                    .background(if (isFirst) Color.Transparent else outlineColor)
            )

            // Circle node
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(resolvedIndicatorColor)
                    .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )

            // Lower line
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .weight(if (isLast) 0.1f else 1f)
                    .background(if (isLast) Color.Transparent else outlineColor)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp, start = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = amount,
                    color = onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp
                )
            }
            Text(
                text = date,
                color = onSurface.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    }
}

// --- TRANSACTION ROW ---
@Composable
fun TransactionRow(
    tx: TransactionEntity,
    currency: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.US) }
    val formattedDate = sdf.format(Date(tx.date))

    val isSalary = tx.category == "Salary"
    val color = if (isSalary) com.example.ui.theme.DynamicGreen else com.example.ui.theme.DynamicRed
    val sign = if (isSalary) "+" else "-"
    val onSurface = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(onSurface.copy(alpha = 0.04f))
            .padding(12.dp)
            .testTag("transaction_row_${tx.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MerchantAvatar(name = tx.merchant, category = tx.category)

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.merchant,
                color = onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(onSurface.copy(alpha = 0.06f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tx.category,
                        color = onSurface.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tx.paymentMethod,
                    color = onSurface.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$sign${formatIndianCurrency(tx.amount, currency)}",
                color = color,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp
            )
            Text(
                text = formattedDate,
                color = onSurface.copy(alpha = 0.4f),
                fontSize = 10.sp
            )
        }
    }
}

// --- FINANCIAL UTILS FOR OVERFLOW & PRECISION ---
fun formatIndianCurrency(amount: Double, currencySymbol: String = "₹"): String {
    if (amount.isNaN() || amount.isInfinite()) return "${currencySymbol}0.00"
    
    // Safety clamp (₹10,00,00,000 maximum transaction as per requirement, totals can be higher but let's clamp format for extreme safety)
    val absAmt = Math.abs(amount)
    if (absAmt > 10000000000.0) {
        return "${if (amount < 0) "-" else ""}${currencySymbol}10,00,00,00,000.00"
    }

    if (currencySymbol != "₹") {
        return "${if (amount < 0) "-" else ""}${currencySymbol}${String.format(java.util.Locale.US, "%,.2f", absAmt)}"
    }

    val isNegative = amount < 0
    val parts = String.format(java.util.Locale.US, "%.2f", absAmt).split(".")
    val integerPart = parts[0]
    val decimalPart = parts[1]
    
    val len = integerPart.length
    val formattedInteger = if (len <= 3) {
        integerPart
    } else {
        val lastThree = integerPart.substring(len - 3)
        val rest = integerPart.substring(0, len - 3)
        val restFormatted = StringBuilder()
        var i = rest.length - 1
        var count = 0
        while (i >= 0) {
            if (count == 2) {
                restFormatted.append(',')
                count = 0
            }
            restFormatted.append(rest[i])
            count++
            i--
        }
        restFormatted.reverse().toString() + "," + lastThree
    }
    
    return (if (isNegative) "-" else "") + currencySymbol + formattedInteger + "." + decimalPart
}

fun Iterable<com.example.data.TransactionEntity>.sumOfAmount(): Double {
    return this.fold(java.math.BigDecimal.ZERO) { acc, tx ->
        acc.add(java.math.BigDecimal.valueOf(tx.amount))
    }.toDouble()
}

fun Iterable<Double>.sumOfBigDecimal(): Double {
    return this.fold(java.math.BigDecimal.ZERO) { acc, d ->
        acc.add(java.math.BigDecimal.valueOf(d))
    }.toDouble()
}


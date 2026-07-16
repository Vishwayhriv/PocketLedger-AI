package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

// Indigo Brand Colors
val IndigoPrimary = Color(0xFF4F46E5)
val IndigoSecondary = Color(0xFF6366F1)
val IndigoAccent = Color(0xFF8B5CF6)

// Slate Neutral Colors
val Slate50 = Color(0xFFF8FAFC)
val Slate100 = Color(0xFFF1F5F9)
val Slate200 = Color(0xFFE2E8F0)
val Slate300 = Color(0xFFCBD5E1)
val Slate400 = Color(0xFF94A3B8)
val Slate500 = Color(0xFF64748B)
val Slate600 = Color(0xFF475569)
val Slate700 = Color(0xFF334155)
val Slate800 = Color(0xFF1E293B)
val Slate900 = Color(0xFF0F172A)
val Slate950 = Color(0xFF020617)

// Professional Polish Backgrounds
val PolishLightBg = Color(0xFFF3F5FA)
val PolishDarkBg = Color(0xFF0B0F19)

// Accent Indicator Colors
val EmeraldGreen = Color(0xFF10B981)
val CoralRed = Color(0xFFEF4444)
val AmberWarning = Color(0xFFF59E0B)

// Dynamic theme-adaptive colors
val DynamicGreen: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF34D399) else Color(0xFF059669)

val DynamicRed: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFFF87171) else Color(0xFFDC2626)

val DynamicBlue: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFF60A5FA) else Color(0xFF2563EB)

val DynamicOrange: Color
    @Composable
    get() = if (LocalDarkTheme.current) Color(0xFFFBBF24) else Color(0xFFD97706)

// Currency visual transformation (with Indian Numbering System formatting support)
class CurrencyVisualTransformation(private val symbol: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(AnnotatedString(""), OffsetMapping.Identity)
        }
        
        val parts = raw.split('.')
        val wholeStr = parts[0]
        val decimalStr = if (parts.size > 1) "." + parts[1] else ""
        
        val formattedWhole = if (symbol.contains("₹") || symbol.isEmpty()) {
            // Default to Indian numbering or standard format
            formatIndianWholeNumberString(wholeStr)
        } else {
            formatStandardWholeNumberString(wholeStr)
        }
        
        val transformedString = symbol + formattedWhole + decimalStr
        
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var transformedIdx = symbol.length
                val originalToTransformedMap = IntArray(raw.length + 1)
                originalToTransformedMap[0] = transformedIdx
                
                var transformedWholeIdx = 0
                for (i in 0 until raw.length) {
                    val char = raw[i]
                    if (char == '.') {
                        originalToTransformedMap[i + 1] = transformedIdx + 1
                        transformedIdx++
                    } else {
                        while (transformedWholeIdx < formattedWhole.length && formattedWhole[transformedWholeIdx] == ',') {
                            transformedIdx++
                            transformedWholeIdx++
                        }
                        transformedIdx++
                        transformedWholeIdx++
                        originalToTransformedMap[i + 1] = transformedIdx
                    }
                }
                return originalToTransformedMap.getOrElse(offset) { transformedString.length }
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= symbol.length) return 0
                var originalIdx = 0
                for (transformedIdx in symbol.length until offset.coerceAtMost(transformedString.length)) {
                    val char = transformedString[transformedIdx]
                    if (char != ',') {
                        originalIdx++
                    }
                }
                return originalIdx.coerceAtMost(raw.length)
            }
        }
        
        return TransformedText(AnnotatedString(transformedString), offsetMapping)
    }
}

fun formatIndianWholeNumberString(wholeStr: String): String {
    if (wholeStr.length <= 3) return wholeStr
    val lastThree = wholeStr.substring(wholeStr.length - 3)
    val remaining = wholeStr.substring(0, wholeStr.length - 3)
    val sb = StringBuilder()
    var count = 0
    for (i in remaining.length - 1 downTo 0) {
        sb.append(remaining[i])
        count++
        if (count == 2 && i > 0) {
            sb.append(',')
            count = 0
        }
    }
    return sb.reverse().toString() + "," + lastThree
}

fun formatStandardWholeNumberString(wholeStr: String): String {
    if (wholeStr.length <= 3) return wholeStr
    val sb = StringBuilder()
    var count = 0
    for (i in wholeStr.length - 1 downTo 0) {
        sb.append(wholeStr[i])
        count++
        if (count == 3 && i > 0) {
            sb.append(',')
            count = 0
        }
    }
    return sb.reverse().toString()
}

// Default theme backward-compatibility placeholders (if needed by standard resources)
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

package com.erpnext.pos.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

actual class DecimalFormatter actual constructor() {
    actual fun format(value: Double, decimals: Int, includeSeparator: Boolean): String {
        val pattern = buildString {
            if (includeSeparator) append("#,##")
            append("0")
            if (decimals > 0) append(".")
            repeat(decimals) { append("0") }
        }
        // Usar Locale.US para consistencia o Locale.getDefault() para adaptarse al sistema
        val df = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))
        return df.format(value)
    }
}

actual fun formatDoubleToString(value: Double, decimals: Int): String {
    // Simple, sin separadores de miles, siempre con punto decimal
    return String.format(Locale.US, "%.${decimals}f", value)
}
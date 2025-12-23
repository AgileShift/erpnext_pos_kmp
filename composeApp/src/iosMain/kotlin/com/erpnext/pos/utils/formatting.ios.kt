package com.erpnext.pos.utils

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle
import platform.Foundation.NSNumberFormatterNoStyle
import platform.Foundation.NSString
import platform.Foundation.stringWithFormat


actual class DecimalFormatter actual constructor() {
    private val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
    }

    actual fun format(
        value: Double,
        decimals: Int,
        includeSeparator: Boolean
    ): String {
        formatter.numberStyle =
            if (includeSeparator) NSNumberFormatterDecimalStyle else NSNumberFormatterNoStyle
        formatter.minimumFractionDigits = decimals.toULong()
        formatter.maximumFractionDigits = decimals.toULong()
        return formatter.stringFromNumber(NSNumber(value)) ?: value.toString()
    }
}

actual fun formatDoubleToString(value: Double, decimals: Int): String {
    return NSString.stringWithFormat(format = "%.${decimals}f", value)
}
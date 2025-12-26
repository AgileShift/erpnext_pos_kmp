package com.erpnext.pos.utils

expect class DecimalFormatter() {
    fun format(value: Double, decimals: Int, includeSeparator: Boolean = false): String
}

expect fun formatDoubleToString(value: Double, decimals: Int): String
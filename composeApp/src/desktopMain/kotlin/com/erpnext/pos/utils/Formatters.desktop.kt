package com.erpnext.pos.utils

actual fun formatAmount(symbol: String, amount: Double): String {
    return symbol + "%.2f".format(amount)
}
package com.erpnext.pos.utils

fun String.toCurrencySymbol(): String {
    return when (this) {
        "NIO" -> "C$"
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        else -> ""
    }
}
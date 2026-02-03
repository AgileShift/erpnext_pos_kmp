package com.erpnext.pos.utils

private val POS_INVOICE_PREFIXES = listOf(
    "ACC-PSINV-",
    "PSINV-",
    "POSINV-",
    "POS-"
)

fun isLikelyPosInvoiceName(name: String?): Boolean {
    val normalized = name?.trim()?.uppercase().orEmpty()
    if (normalized.isBlank()) return false
    return POS_INVOICE_PREFIXES.any { normalized.startsWith(it) }
}

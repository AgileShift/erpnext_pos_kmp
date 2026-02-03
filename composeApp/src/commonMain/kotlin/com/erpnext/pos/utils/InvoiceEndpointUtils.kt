package com.erpnext.pos.utils

private val POS_INVOICE_PREFIXES = listOf(
    "ACC-PSINV-",
    "PSINV-",
    "POSINV-",
    "POS-"
)

private val SALES_INVOICE_PREFIXES = listOf(
    "FACT-",
    "SINV-",
    "SI-"
)

fun isLikelyPosInvoiceName(name: String?): Boolean {
    val normalized = name?.trim()?.uppercase().orEmpty()
    if (normalized.isBlank()) return false
    return POS_INVOICE_PREFIXES.any { normalized.startsWith(it) }
}

fun isLikelySalesInvoiceName(name: String?): Boolean {
    val normalized = name?.trim()?.uppercase().orEmpty()
    if (normalized.isBlank()) return false
    return SALES_INVOICE_PREFIXES.any { normalized.startsWith(it) }
}

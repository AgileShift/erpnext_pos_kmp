package com.erpnext.pos.domain.usecases

/**
 * Resolves whether a return should affect inventory stock.
 *
 * If the UI does not provide an explicit selection, we fallback to invoice nature: POS invoices
 * default to stock impact, non-POS invoices default to accounting-only return.
 */
internal fun resolveReturnStockImpact(
    isPosInvoice: Boolean,
    explicitSelection: Boolean?,
): Boolean = explicitSelection ?: isPosInvoice

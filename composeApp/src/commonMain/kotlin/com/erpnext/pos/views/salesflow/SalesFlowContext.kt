package com.erpnext.pos.views.salesflow

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SalesFlowSource {
    Customer,
    Quotation,
    SalesOrder,
    DeliveryNote
}

data class SalesFlowContext(
    val customerId: String? = null,
    val customerName: String? = null,
    val sourceType: SalesFlowSource? = null,
    val sourceId: String? = null
) {
    fun withSource(sourceType: SalesFlowSource, sourceId: String?): SalesFlowContext {
        return copy(
            sourceType = sourceType,
            sourceId = sourceId?.trim().orEmpty().ifBlank { null }
        )
    }

    fun withCustomer(customerId: String?, customerName: String?): SalesFlowContext {
        return copy(
            customerId = customerId?.trim().orEmpty().ifBlank { null },
            customerName = customerName?.trim().orEmpty().ifBlank { null }
        )
    }

    fun sourceLabel(): String? {
        return when (sourceType) {
            SalesFlowSource.Customer -> "Customer"
            SalesFlowSource.Quotation -> "Quotation"
            SalesFlowSource.SalesOrder -> "Sales Order"
            SalesFlowSource.DeliveryNote -> "Delivery Note"
            null -> null
        }
    }
}

class SalesFlowContextStore {
    private val _context = MutableStateFlow<SalesFlowContext?>(null)
    val context = _context.asStateFlow()

    fun set(context: SalesFlowContext?) {
        _context.value = context
    }

    fun clear() {
        _context.value = null
    }

    fun consume(): SalesFlowContext? {
        val current = _context.value
        _context.value = null
        return current
    }
}

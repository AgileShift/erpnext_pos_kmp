package com.erpnext.pos.domain.sync

enum class SyncDocType(val value: String) {
    CUSTOMER("Customer"),
    QUOTATION("Quotation"),
    SALES_ORDER("Sales Order"),
    DELIVERY_NOTE("Delivery Note"),
    PAYMENT_ENTRY("Payment Entry"),
    SALES_INVOICE("Sales Invoice")
}

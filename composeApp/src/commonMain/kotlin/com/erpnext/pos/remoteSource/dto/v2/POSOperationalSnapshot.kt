package com.erpnext.pos.remoteSource.dto.v2

data class POSOperationalSnapshot(
    val context: POSContextSnapshot,
    val configuration: POSConfigurationSnapshot,
    val catalog: CatalogSnapshot,
    val customers: List<CustomerSnapshot>,
    val invoices: List<InvoiceSnapshot>,
    val sync: SyncStatusSnapshot
)
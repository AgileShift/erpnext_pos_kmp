package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "sales_invoice_payments",
    indices = [
        Index(value = ["instanceId", "companyId", "invoiceId"]),
        Index(value = ["invoiceId"])
    ]
)
data class SalesInvoicePaymentEntity(
    var paymentId: String,
    var invoiceId: String,
    var paymentMode: String,
    var amount: Double
) : BaseEntity()

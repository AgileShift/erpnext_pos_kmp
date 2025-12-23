package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "sales_invoice_items",
    indices = [
        Index(value = ["instanceId", "companyId", "invoiceId"]),
        Index(value = ["invoiceId"])
    ]
)
class SalesInvoiceItemEntity(
    var invoiceId: String,
    var rowId: Int,
    var itemCode: String,
    var itemName: String,
    var qty: Double,
    var uom: String,
    var rate: Double,
    var amount: Double,
    var warehouse: String,
    var priceListRate: Double
) : BaseEntity()
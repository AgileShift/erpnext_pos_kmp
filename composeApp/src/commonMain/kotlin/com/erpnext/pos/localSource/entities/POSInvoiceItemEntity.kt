package com.erpnext.pos.localSource.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabPOSInvoiceItem")
data class POSInvoiceItemEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var invoiceName: String,
    var itemCode: String,
    var itemName: String,
    var qty: Double,
    var rate: Double,
    var amount: Double,
    var discountPercentage: Double? = null,
    var discountAmount: Double? = null
)
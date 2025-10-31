package com.erpnext.pos.localSource.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabPOSInvoice")
data class POSInvoiceEntity(
    @PrimaryKey(autoGenerate = false)
    var name: String,
    var customer: String,
    var customerName: String,
    var postingDate: String,
    var dueDate: String,
    var status: String,
    var total: Double,
    var outstandingAmount: Double? = null
)
package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class CustomerEntity(
    @ColumnInfo(name = "id")
    @PrimaryKey(autoGenerate = false)
    var name: String,
    var customerName: String,
    var territory: String?,
    var email: String?,
    var mobileNo: String?,
    var customerType: String,
    var creditLimit: Double? = null,
    var currentBalance: Double,
    var totalPendingAmount: Double,  // Sum outstanding_amount
    var pendingInvoicesCount: Int,
    var availableCredit: Double,
    var address: String? = null  // Formatted
)
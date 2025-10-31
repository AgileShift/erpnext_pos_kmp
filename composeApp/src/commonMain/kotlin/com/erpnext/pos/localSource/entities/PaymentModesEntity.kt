package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabPaymentModes")
data class PaymentModesEntity(
    @PrimaryKey(autoGenerate = false)
    var name: String,
    var default: Boolean,
    @ColumnInfo("mode_of_payment")
    var modeOfPayment: String,
)
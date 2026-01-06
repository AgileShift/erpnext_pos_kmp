package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.time.Clock

@Entity(tableName = "tabPaymentTerm")
data class PaymentTermEntity(
    @PrimaryKey
    @ColumnInfo(name = "payment_term_name")
    val name: String,

    @ColumnInfo(name = "invoice_portion")
    val invoicePortion: Double? = null,

    @ColumnInfo(name = "mode_of_payment")
    val modeOfPayment: String? = null,

    @ColumnInfo(name = "due_date_based_on")
    val dueDateBasedOn: String? = null,

    @ColumnInfo(name = "credit_days")
    val creditDays: Int? = null,

    @ColumnInfo(name = "credit_months")
    val creditMonths: Int? = null,

    @ColumnInfo(name = "discount_type")
    val discountType: String? = null,

    @ColumnInfo(name = "discount")
    val discount: Double? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "discount_validity")
    val discountValidity: Int? = null,

    @ColumnInfo(name = "discount_validity_based_on")
    val discountValidityBasedOn: String? = null,

    @ColumnInfo(name = "last_synced_at")
    val lastSyncedAt: Long = Clock.System.now().toEpochMilliseconds()
)

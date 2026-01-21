package com.erpnext.pos.localSource.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Entity(
    tableName = "tabSalesInvoicePayment",
    foreignKeys = [
        ForeignKey(
            entity = SalesInvoiceEntity::class,
            parentColumns = ["invoice_name"],
            childColumns = ["parent_invoice"],
            onDelete = ForeignKey.Companion.CASCADE,
            onUpdate = ForeignKey.Companion.CASCADE
        )
    ],
    indices = [
        Index(value = ["parent_invoice"])
    ]
)
data class POSInvoicePaymentEntity(

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,

    @ColumnInfo(name = "parent_invoice")
    var parentInvoice: String, // invoice_name del padre

    @ColumnInfo(name = "mode_of_payment")
    var modeOfPayment: String, // Cash, Card, Transfer...

    @ColumnInfo(name = "amount")
    var amount: Double = 0.0,

    @ColumnInfo(name = "entered_amount", defaultValue = "0.0")
    var enteredAmount: Double = 0.0,
    @ColumnInfo(name = "payment_currency")
    var paymentCurrency: String? = null,
    @ColumnInfo(name = "exchange_rate", defaultValue = "1.0")
    var exchangeRate: Double = 1.0,

    @ColumnInfo(name = "payment_reference")
    var paymentReference: String? = null, // No. de recibo o transacción
    @ColumnInfo(name = "payment_date")
    var paymentDate: String? = null,
    @ColumnInfo(name = "pos_opening_entry")
    var posOpeningEntry: String? = null,
    @ColumnInfo(name = "sync_status", defaultValue = "Pending")
    var syncStatus: String = "Pending",

    @ColumnInfo(name = "created_at")
    var createdAt: Long = Clock.System.now().toEpochMilliseconds(),
    @ColumnInfo(name = "last_synced_at")
    override var lastSyncedAt: Long? =  Clock.System.now().toEpochMilliseconds()
): SyncableEntity

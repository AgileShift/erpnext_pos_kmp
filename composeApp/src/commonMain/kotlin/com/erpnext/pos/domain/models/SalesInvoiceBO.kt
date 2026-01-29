package com.erpnext.pos.domain.models

import androidx.room.ColumnInfo

data class SalesInvoiceBO(
    val invoiceId: String,
    val customerId: String,
    val customer: String?,
    val customerPhone: String?,
    val postingDate: String,
    val dueDate: String?,
    val outstandingAmount: Double,
    val netTotal: Double,
    val total: Double,
    val paidAmount: Double,
    val baseTotal: Double? = null,
    val baseNetTotal: Double? = null,
    val baseTotalTaxesAndCharges: Double? = null,
    val baseGrandTotal: Double? = null,
    val baseRoundingAdjustment: Double? = null,
    val baseRoundedTotal: Double? = null,
    val baseDiscountAmount: Double? = null,
    val basePaidAmount: Double? = null,
    val baseChangeAmount: Double? = null,
    val baseWriteOffAmount: Double? = null,
    val baseOutstandingAmount: Double? = null,
    val isPos: Boolean? = false,
    val docStatus: Int,
    val currency: String?,
    val status: String?,
    val syncStatus: String? = "Pending",
    val conversionRate: Double? = null,
    val customExchangeRate: Double? = null,
    val items: List<SalesInvoiceItemsBO> = emptyList(),
    val payments: List<SalesInvoicePaymentsBO> = emptyList(),
    val partyAccountCurrency: String? = null
)

data class SalesInvoiceItemsBO(
    var itemCode: String,
    var itemName: String? = null,
    var description: String? = null,
    var uom: String? = "Unit",
    var qty: Double = 1.0,
    var rate: Double = 0.0,
    var amount: Double = 0.0,
    var netRate: Double = 0.0,
    var netAmount: Double = 0.0
)

data class SalesInvoicePaymentsBO(
    var modeOfPayment: String,
    var amount: Double = 0.0,
    var paymentReference: String? = null,
    var paymentDate: String? = null,
)

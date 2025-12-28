package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class SyncStatus(status: String) {
    Pending("Pending"),
    Synced("Synced"),
    Failed("Failed")
}

enum class DocStatus(status: String) {
    Draft("Draft"),
    Unpaid("Unpaid"),
    Paid("Paid"),
    Overdue("Overdue")
}

@Serializable
data class SalesInvoiceDto(
    val name: String? = null,
    val customer: String,
    @SerialName("customer_name")
    val customerName: String,
    @SerialName("contact_mobile")
    val customerPhone: String?,
    val company: String,
    @SerialName("posting_date")
    val postingDate: String,
    @SerialName("due_date")
    val dueDate: String? = null,
    val status: String? = null,
    @SerialName("grand_total")
    val grandTotal: Double = 0.0,
    @SerialName("outstanding_amount")
    val outstandingAmount: Double? = 0.0,
    @SerialName("total_taxes_and_charges")
    val totalTaxesAndCharges: Double? = 0.0,
    @SerialName("net_total")
    val netTotal: Double,
    @SerialName("paid_amount")
    val paidAmount: Double = 0.0,
    val items: List<SalesInvoiceItemDto> = emptyList(),
    @SerialName("payments")
    val payments: List<SalesInvoicePaymentDto> = emptyList(),
    @SerialName("payment_schedule")
    val paymentSchedule: List<SalesInvoicePaymentScheduleDto> = emptyList(),
    @SerialName("payment_terms")
    val paymentTerms: String? = null,
    val remarks: String? = null,
    @SerialName("custom_payment_currency")
    val customPaymentCurrency: String? = null,
    @SerialName("custom_exchange_rate")
    val customExchangeRate: Double? = null,
    @SerialName("posa_delivery_charges")
    val posaDeliveryCharges: String? = null,
    @SerialName("is_pos")
    @Serializable(IntAsBooleanSerializer::class)
    val isPos: Boolean = true,
    val doctype: String = "Sales Invoice",
    @SerialName("pos_profile")
    val posProfile: String? = null,
    @SerialName("party_account_currency")
    val currency: String? = null,
)

@Serializable
data class SalesInvoicePaymentScheduleDto(
    @SerialName("payment_term")
    val paymentTerm: String,
    @SerialName("invoice_portion")
    val invoicePortion: Double,
    @SerialName("due_date")
    val dueDate: String,
    @SerialName("mode_of_payment")
    val modeOfPayment: String? = null
)

@Serializable
data class SalesInvoiceItemDto(
    @SerialName("item_code")
    val itemCode: String,
    @SerialName("item_name")
    val itemName: String? = null,
    val description: String? = null,
    val qty: Double,
    val rate: Double,
    val amount: Double,
    @SerialName("discount_percentage")
    val discountPercentage: Double? = null,
    val warehouse: String? = null,
    @SerialName("income_account")
    val incomeAccount: String? = null,
    @SerialName("cost_center")
    val costCenter: String? = null
)

@Serializable
data class InvoiceTax(
    @SerialName("charge_type") val chargeType: String = "On Net Total",
    @SerialName("account_head") val accountHead: String,
    val rate: Double,
    @SerialName("description") val description: String? = null
)

@Serializable
data class SalesInvoicePaymentDto(
    @SerialName("mode_of_payment")
    val modeOfPayment: String,
    val amount: Double,
    @SerialName("payment_reference")
    val paymentReference: String? = null,
    val type: String? = "Receive"
)

package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SalesInvoiceDto(
    val name: String? = null,
    val customer: String,
    @SerialName("customer_name")
    val customerName: String,
    @SerialName("contact_mobile")
    val customerPhone: String? = null,
    val company: String,
    @SerialName("posting_date")
    val postingDate: String,
    @SerialName("due_date")
    val dueDate: String? = null,
    val status: String? = null,
    @SerialName("grand_total")
    val grandTotal: Double = 0.0,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("outstanding_amount")
    val outstandingAmount: Double? = null,
    @SerialName("total_taxes_and_charges")
    val totalTaxesAndCharges: Double? = 0.0,
    @SerialName("total")
    val total: Double? = null,
    @SerialName("net_total")
    val netTotal: Double,
    @SerialName("rounded_total")
    val roundedTotal: Double? = null,
    @SerialName("rounding_adjustment")
    val roundingAdjustment: Double? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("disable_rounded_total")
    @Serializable(IntAsBooleanSerializer::class)
    val disableRoundedTotal: Boolean? = null,
    @SerialName("discount_amount")
    val discountAmount: Double? = null,
    @SerialName("apply_discount_on")
    val applyDiscountOn: String? = null,
    @SerialName("additional_discount_percentage")
    val additionalDiscountPercentage: Double? = null,
    @SerialName("coupon_code")
    val couponCode: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("paid_amount")
    val paidAmount: Double? = null,
    @SerialName("change_amount")
    val changeAmount: Double? = null,
    @SerialName("write_off_amount")
    val writeOffAmount: Double? = null,
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
    @SerialName("is_pos")
    @Serializable(IntAsBooleanSerializer::class)
    val isPos: Boolean = false,
    @SerialName("update_stock")
    @Serializable(with = IntAsBooleanSerializer::class)
    val updateStock: Boolean = false,
    val doctype: String = "Sales Invoice",
    @SerialName("pos_profile")
    val posProfile: String? = null,
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("conversion_rate")
    val conversionRate: Double? = null,
    @SerialName("party_account_currency")
    val partyAccountCurrency: String? = null,
    @SerialName("return_against")
    val returnAgainst: String? = null,
    @SerialName("pos_opening_entry")
    val posOpeningEntry: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("debit_to")
    val debitTo: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("docstatus")
    val docStatus: Int? = null,
    @SerialName("is_return")
    val isReturn: Int = 0,

    // Response
    @SerialName("base_grand_total")
    val baseGrandTotal: Double? = null,
    @SerialName("base_total")
    val baseTotal: Double? = null,
    @SerialName("base_net_total")
    val baseNetTotal: Double? = null,
    @SerialName("base_total_taxes_and_charges")
    val baseTotalTaxesAndCharges: Double? = null,
    @SerialName("base_rounding_adjustment")
    val baseRoundingAdjustment: Double? = null,
    @SerialName("base_rounded_total")
    val baseRoundedTotal: Double? = null,
    @SerialName("base_discount_amount")
    val baseDiscountAmount: Double? = null,
    @SerialName("base_paid_amount")
    val basePaidAmount: Double? = null,
    @SerialName("base_change_amount")
    val baseChangeAmount: Double? = null,
    @SerialName("base_write_off_amount")
    val baseWriteOffAmount: Double? = null,
)

@Serializable
data class SalesInvoicePaymentScheduleDto(
    @SerialName("payment_term")
    val paymentTerm: String? = null,
    @SerialName("invoice_portion")
    val invoicePortion: Double,
    @SerialName("due_date")
    val dueDate: String,
    @SerialName("mode_of_payment")
    val modeOfPayment: String? = null
)

@OptIn(ExperimentalSerializationApi::class)
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
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    @SerialName("income_account")
    val incomeAccount: String? = null,
    @SerialName("sales_order")
    val salesOrder: String? = null,
    @SerialName("so_detail")
    val salesOrderItem: String? = null,
    @SerialName("delivery_note")
    val deliveryNote: String? = null,
    @SerialName("dn_detail")
    val deliveryNoteItem: String? = null,
    @SerialName("cost_center")
    val costCenter: String? = null
)

@Serializable
data class SalesInvoicePaymentDto(
    @SerialName("mode_of_payment")
    val modeOfPayment: String,
    val amount: Double,
    val account: String? = null,
    @SerialName("payment_reference")
    val paymentReference: String? = null,
    val type: String? = "Receive"
)

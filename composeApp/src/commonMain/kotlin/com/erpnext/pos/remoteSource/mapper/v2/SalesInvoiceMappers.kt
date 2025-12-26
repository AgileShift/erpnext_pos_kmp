package com.erpnext.pos.remoteSource.mapper.v2

import com.erpnext.pos.localSource.entities.v2.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoicePaymentEntity
import com.erpnext.pos.localSource.relations.v2.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.dto.v2.SalesInvoiceCreateDto
import com.erpnext.pos.remoteSource.dto.v2.SalesInvoiceItemCreateDto
import com.erpnext.pos.remoteSource.dto.v2.SalesInvoicePaymentCreateDto

fun SalesInvoiceWithItemsAndPayments.toCreateDto(
    remoteCustomerId: String
): SalesInvoiceCreateDto {
    return SalesInvoiceCreateDto(
        company = invoice.company,
        customerId = remoteCustomerId,
        postingDate = invoice.postingDate,
        postingTime = invoice.postingTime,
        dueDate = invoice.dueDate,
        customerName = invoice.customerName,
        territory = invoice.territory,
        isPos = invoice.isPos,
        updateStock = invoice.updateStock,
        setWarehouse = invoice.setWarehouse,
        sellingPriceList = invoice.priceList,
        currency = invoice.currency,
        conversionRate = invoice.conversionRate,
        namingSeries = invoice.namingSeries,
        disableRoundedTotal = invoice.disableRoundedTotal,
        roundedTotal = invoice.roundedTotal,
        totalTaxesAndCharges = invoice.totalTaxesAndCharges,
        grandTotal = invoice.grandTotal,
        items = items.map { it.toCreateDto() },
        payments = payments.map { it.toCreateDto() }
    )
}

fun SalesInvoiceItemEntity.toCreateDto() = SalesInvoiceItemCreateDto(
    itemCode = itemCode,
    qty = qty,
    rate = rate,
    uom = uom,
    warehouse = warehouse,
    priceListRate = priceListRate
)

fun SalesInvoicePaymentEntity.toCreateDto() = SalesInvoicePaymentCreateDto(
    modeOfPayment = paymentMode,
    amount = amount
)

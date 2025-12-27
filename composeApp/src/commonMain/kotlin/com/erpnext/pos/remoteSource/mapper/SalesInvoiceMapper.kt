@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.remoteSource.mapper

import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.POSProfileEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.dto.BalanceDetailsDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceItemDto
import com.erpnext.pos.remoteSource.dto.SalesInvoicePaymentDto
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// ------------------------------------------------------
// 🔹 Local → Remoto (para enviar al API ERPNext)
// ------------------------------------------------------

fun SalesInvoiceWithItemsAndPayments.toDto(): SalesInvoiceDto {
    return SalesInvoiceDto(
        name = invoice.invoiceName,
        customer = invoice.customer,
        company = invoice.company,
        postingDate = invoice.postingDate,
        dueDate = invoice.dueDate,
        currency = invoice.currency,
        status = invoice.status,
        grandTotal = invoice.grandTotal,
        outstandingAmount = invoice.outstandingAmount,
        totalTaxesAndCharges = invoice.taxTotal,
        items = items.map { it.toDto(invoice) },
        payments = payments.map { it.toDto() },
        remarks = invoice.remarks,
        isPos = true,
        doctype = "Sales Invoice",
        customerName = invoice.customerName ?: "CST",
        customerPhone = invoice.customerPhone,
        netTotal = invoice.netTotal,
        paidAmount = invoice.paidAmount,
        posProfile = invoice.posProfile,
    )
}

fun SalesInvoiceItemEntity.toDto(parent: SalesInvoiceEntity): SalesInvoiceItemDto {
    return SalesInvoiceItemDto(
        itemCode = itemCode,
        itemName = itemName,
        description = description,
        qty = qty,
        rate = rate,
        amount = amount,
        discountPercentage = discountPercentage.takeIf { it != 0.0 },
        warehouse = warehouse ?: parent.warehouse,
        incomeAccount = parent.incomeAccount,
        costCenter = parent.costCenter
    )
}

fun POSInvoicePaymentEntity.toDto(): SalesInvoicePaymentDto {
    return SalesInvoicePaymentDto(
        modeOfPayment = modeOfPayment,
        amount = amount,
        type = "Receive"
    )
}

fun BalanceDetailsEntity.toDto(): BalanceDetailsDto {
    return BalanceDetailsDto(
        modeOfPayment = this.modeOfPayment,
        openingAmount = this.openingAmount,
        closingAmount = this.closingAmount
    )
}

fun List<BalanceDetailsEntity>.toDto(): List<BalanceDetailsDto> {
    return this.map { it.toDto() }
}

// ------------------------------------------------------
// 🔹 Remoto → Local (para guardar respuesta del servidor)
// ------------------------------------------------------

fun List<SalesInvoiceDto>.toEntities(): List<SalesInvoiceWithItemsAndPayments> {
    return this.map { it.toEntity() }
}

fun SalesInvoiceDto.toEntity(): SalesInvoiceWithItemsAndPayments {
    val now = Clock.System.now().toEpochMilliseconds()

    val invoiceEntity = SalesInvoiceEntity(
        invoiceName = name,
        customer = customer,
        customerPhone = customerPhone,
        customerName = customerName,
        company = company,
        postingDate = postingDate,
        dueDate = dueDate,
        currency = currency ?: "NIO",
        netTotal = items.sumOf { it.amount },
        taxTotal = totalTaxesAndCharges ?: 0.0,
        grandTotal = grandTotal,
        outstandingAmount = outstandingAmount ?: 0.0,
        status = status ?: "Draft",
        syncStatus = "Synced",
        docstatus = if (status == "Submitted" || status == "Paid") 1 else 0,
        modeOfPayment = payments.firstOrNull()?.modeOfPayment,
        createdAt = now,
        modifiedAt = now,
        remarks = remarks
    )

    val itemsEntity = items.map { dto ->
        SalesInvoiceItemEntity(
            parentInvoice = name ?: "",
            itemCode = dto.itemCode,
            itemName = dto.itemName,
            description = dto.description,
            qty = dto.qty,
            rate = dto.rate,
            amount = dto.amount,
            discountPercentage = dto.discountPercentage ?: 0.0,
            warehouse = dto.warehouse,
            incomeAccount = dto.incomeAccount,
            costCenter = dto.costCenter,
            createdAt = now,
            modifiedAt = now
        )
    }

    val paymentsEntity = payments.map { dto ->
        POSInvoicePaymentEntity(
            parentInvoice = name ?: "",
            modeOfPayment = dto.modeOfPayment,
            amount = dto.amount,
            createdAt = now
        )
    }

    return SalesInvoiceWithItemsAndPayments(
        invoice = invoiceEntity,
        items = itemsEntity,
        payments = paymentsEntity
    )
}

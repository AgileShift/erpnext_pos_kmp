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
    val invoiceName = invoice.invoiceName?.takeUnless { it.startsWith("LOCAL-") }
    val hasOutstanding = invoice.outstandingAmount > 0.0001
    val paymentsToSend = payments.filter { it.amount > 0.0 }
    val shouldSendAsPos = invoice.isPos && paymentsToSend.isNotEmpty() && !hasOutstanding
    val resolvedPayments = if (shouldSendAsPos) {
        paymentsToSend.map { it.toDto() }
    } else {
        emptyList()
    }
    val resolvedIsPos = shouldSendAsPos
    val resolvedDocType = if (shouldSendAsPos) "POS Invoice" else "Sales Invoice"

    return SalesInvoiceDto(
        name = invoiceName,
        customer = invoice.customer,
        company = invoice.company,
        postingDate = invoice.postingDate,
        dueDate = invoice.dueDate,
        currency = invoice.currency,
        debitTo = invoice.debitTo,
        partyAccountCurrency = invoice.partyAccountCurrency,
        status = invoice.status,
        grandTotal = invoice.grandTotal,
        outstandingAmount = invoice.outstandingAmount,
        totalTaxesAndCharges = invoice.taxTotal,
        items = items.map { it.toDto(invoice) },
        payments = resolvedPayments,
        remarks = invoice.remarks,
        isPos = resolvedIsPos,
        doctype = resolvedDocType,
        customerName = invoice.customerName ?: "CST",
        customerPhone = invoice.customerPhone,
        netTotal = invoice.netTotal,
        paidAmount = invoice.paidAmount,
        posProfile = invoice.profileId,
        docStatus = 0
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
    )
}

fun POSInvoicePaymentEntity.toDto(): SalesInvoicePaymentDto {
    return SalesInvoicePaymentDto(
        modeOfPayment = modeOfPayment,
        amount = amount,
        paymentReference = paymentReference,
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
    val resolvedIsPos = doctype.equals("POS Invoice", ignoreCase = true) || isPos
    fun resolveBaseAmount(amount: Double?, baseAmount: Double?): Double? {
        baseAmount?.let { return it }
        val rate = conversionRate?.takeIf { it > 0.0 } ?: return amount
        return amount?.let { it * rate }
    }

    // Se asegura que la factura local conserve los montos pagados recibidos del servidor.
    val headerWarehouse = items.firstOrNull { !it.warehouse.isNullOrBlank() }?.warehouse

    val invoiceEntity = SalesInvoiceEntity(
        invoiceName = name,
        profileId = posProfile,
        customer = customer,
        customerPhone = customerPhone,
        customerName = customerName,
        company = company,
        debitTo = debitTo,
        postingDate = postingDate,
        dueDate = dueDate,
        currency = currency ?: "NIO" ,
        partyAccountCurrency = partyAccountCurrency,
        conversionRate = conversionRate,
        customExchangeRate = customExchangeRate,
        netTotal = netTotal,
        taxTotal = totalTaxesAndCharges ?: 0.0,
        grandTotal = grandTotal,
        outstandingAmount = outstandingAmount ?: 0.0,
        baseTotal = resolveBaseAmount(total ?: netTotal, baseTotal),
        baseNetTotal = resolveBaseAmount(netTotal, baseNetTotal),
        baseTotalTaxesAndCharges = resolveBaseAmount(totalTaxesAndCharges, baseTotalTaxesAndCharges),
        baseGrandTotal = resolveBaseAmount(grandTotal, baseGrandTotal),
        baseRoundingAdjustment = resolveBaseAmount(roundingAdjustment, baseRoundingAdjustment),
        baseRoundedTotal = resolveBaseAmount(roundedTotal, baseRoundedTotal),
        baseDiscountAmount = resolveBaseAmount(discountAmount, baseDiscountAmount),
        basePaidAmount = resolveBaseAmount(paidAmount, basePaidAmount),
        baseChangeAmount = resolveBaseAmount(changeAmount, baseChangeAmount),
        baseWriteOffAmount = resolveBaseAmount(writeOffAmount, baseWriteOffAmount),
        baseOutstandingAmount = baseOutstandingAmount ?: outstandingAmount,
        // El paid_amount remoto es la fuente de verdad para reconciliación y BI.
        paidAmount = paidAmount,
        status = status ?: "Draft",
        syncStatus = "Synced",
        docstatus = resolveDocStatus(status, docStatus),
        isReturn = isReturn == 1,
        modeOfPayment = payments.firstOrNull()?.modeOfPayment,
        isPos = resolvedIsPos,
        createdAt = now,
        modifiedAt = now,
        remarks = remarks,
        posOpeningEntry = posOpeningEntry,
        warehouse = headerWarehouse
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
            enteredAmount = dto.amount,
            // La moneda real del pago es la moneda de la factura; party_account_currency suele ser USD.
            paymentCurrency = currency ?: partyAccountCurrency,
            paymentReference = dto.paymentReference,
            syncStatus = "Synced",
            createdAt = now
        )
    }

    return SalesInvoiceWithItemsAndPayments(
        invoice = invoiceEntity,
        items = itemsEntity,
        payments = paymentsEntity
    )
}

fun SalesInvoiceEntity.toDto(): SalesInvoiceDto {
    return SalesInvoiceDto(
        name = invoiceName,
        customer = customer,
        customerName = customerName ?: "",
        customerPhone = customerPhone,
        company = company,
        postingDate = postingDate,
        dueDate = dueDate,
        status = status,
        grandTotal = grandTotal,
        outstandingAmount = outstandingAmount,
        totalTaxesAndCharges = taxTotal,
        total = netTotal,
        netTotal = netTotal,
        paidAmount = paidAmount,
        items = emptyList(),
        payments = emptyList(),
        remarks = remarks,
        isPos = isPos,
        doctype = if (isPos) "POS Invoice" else "Sales Invoice",
        updateStock = true,
        posProfile = profileId,
        currency = currency,
        conversionRate = conversionRate,
        partyAccountCurrency = partyAccountCurrency,
        customExchangeRate = customExchangeRate,
        debitTo = debitTo,
        docStatus = docstatus,
        baseTotal = baseTotal,
        baseNetTotal = baseNetTotal,
        baseTotalTaxesAndCharges = baseTotalTaxesAndCharges,
        baseGrandTotal = baseGrandTotal,
        baseRoundingAdjustment = baseRoundingAdjustment,
        baseRoundedTotal = baseRoundedTotal,
        baseDiscountAmount = baseDiscountAmount,
        basePaidAmount = basePaidAmount,
        baseChangeAmount = baseChangeAmount,
        baseWriteOffAmount = baseWriteOffAmount,
        baseOutstandingAmount = baseOutstandingAmount,
    )
}

private fun resolveDocStatus(status: String?, docStatus: Int?): Int {
    val normalized = status?.lowercase()?.trim()
    return docStatus?.coerceIn(0, 2) ?: when (normalized) {
        "paid", "submitted" -> 1
        "cancelled", "return" -> 2
        else -> 0
    }
}

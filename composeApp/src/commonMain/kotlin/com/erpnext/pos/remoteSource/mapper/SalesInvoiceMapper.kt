@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.remoteSource.mapper

import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.dto.BalanceDetailsDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceItemDto
import com.erpnext.pos.remoteSource.dto.SalesInvoicePaymentDto
import com.erpnext.pos.utils.roundToCurrency
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// ------------------------------------------------------
// 🔹 Local → Remoto (para enviar al API ERPNext)
// ------------------------------------------------------

fun SalesInvoiceWithItemsAndPayments.toDto(): SalesInvoiceDto {
    val invoiceName = invoice.invoiceName?.takeUnless { it.startsWith("LOCAL-") }
    val resolvedPayments = payments.map { it.toDto() }
    val resolvedDocType = "Sales Invoice"
    val resolvedIsPos = invoice.isPos ||
        !invoice.profileId.isNullOrBlank() ||
        !invoice.posOpeningEntry.isNullOrBlank()

    return SalesInvoiceDto(
        name = invoiceName,
        customer = invoice.customer,
        company = invoice.company,
        postingDate = invoice.postingDate,
        dueDate = invoice.dueDate,
        currency = invoice.currency,
        conversionRate = invoice.conversionRate,
        debitTo = invoice.debitTo,
        partyAccountCurrency = invoice.partyAccountCurrency,
        customExchangeRate = invoice.customExchangeRate,
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
        posOpeningEntry = invoice.posOpeningEntry,
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

// ------------------------------------------------------
// 🔹 Remoto → Local (para guardar respuesta del servidor)
// ------------------------------------------------------

fun List<SalesInvoiceDto>.toEntities(): List<SalesInvoiceWithItemsAndPayments> {
    return this.map { it.toEntity() }
}

fun SalesInvoiceDto.toEntity(): SalesInvoiceWithItemsAndPayments {
    val now = Clock.System.now().toEpochMilliseconds()
    val resolvedIsPos = isPos
    fun resolveBaseAmount(amount: Double?, baseAmount: Double?): Double? {
        if (amount == null) return baseAmount
        val rate = conversionRate?.takeIf { it > 0.0 && it != 1.0 }
        if (baseAmount != null && rate != null) {
            val approxEqual = kotlin.math.abs(baseAmount - amount) <= 0.0001
            if (approxEqual) return amount * rate
        }
        if (rate == null) return baseAmount
        return baseAmount ?: (amount * rate)
    }

    val financials = resolveInvoiceFinancialsFromRemote(
        grandTotal = grandTotal,
        paidAmount = paidAmount,
        outstandingAmount = outstandingAmount,
        payments = payments,
        isReturn = isReturn,
        status = status
    )

    // Se asegura que la factura local conserve los montos pagados recibidos del servidor.
    val headerWarehouse = items.firstOrNull { !it.warehouse.isNullOrBlank() }?.warehouse

    val invoiceEntity = SalesInvoiceEntity(
        invoiceName = name,
        profileId = posProfile?.takeIf { it.isNotBlank() },
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
        outstandingAmount = financials.outstandingAmount,
        baseTotal = resolveBaseAmount(total ?: netTotal, baseTotal),
        baseNetTotal = resolveBaseAmount(netTotal, baseNetTotal),
        baseTotalTaxesAndCharges = resolveBaseAmount(totalTaxesAndCharges, baseTotalTaxesAndCharges),
        baseGrandTotal = resolveBaseAmount(grandTotal, baseGrandTotal),
        baseRoundingAdjustment = resolveBaseAmount(roundingAdjustment, baseRoundingAdjustment),
        baseRoundedTotal = resolveBaseAmount(roundedTotal, baseRoundedTotal),
        baseDiscountAmount = resolveBaseAmount(discountAmount, baseDiscountAmount),
        basePaidAmount = resolveBaseAmount(financials.paidAmount, basePaidAmount),
        baseChangeAmount = resolveBaseAmount(changeAmount, baseChangeAmount),
        baseWriteOffAmount = resolveBaseAmount(writeOffAmount, baseWriteOffAmount),
        // El paid_amount remoto es la fuente de verdad para reconciliación y BI.
        paidAmount = financials.paidAmount,
        status = status ?: "Draft",
        syncStatus = "Synced",
        docstatus = resolveDocStatus(status, docStatus),
        isReturn = isReturn == 1,
        modeOfPayment = payments.firstOrNull()?.modeOfPayment,
        isPos = resolvedIsPos,
        createdAt = now,
        modifiedAt = now,
        remarks = remarks,
        posOpeningEntry = posOpeningEntry?.takeIf { it.isNotBlank() },
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
        doctype = "Sales Invoice",
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

private data class ResolvedInvoiceFinancials(
    val paidAmount: Double,
    val outstandingAmount: Double
)

private fun resolveInvoiceFinancialsFromRemote(
    grandTotal: Double,
    paidAmount: Double?,
    outstandingAmount: Double?,
    payments: List<SalesInvoicePaymentDto>,
    isReturn: Int,
    status: String?
): ResolvedInvoiceFinancials {
    val tolerance = 0.01
    val total = grandTotal.coerceAtLeast(0.0)
    val paidFromField = (paidAmount ?: 0.0).coerceAtLeast(0.0)
    val paidFromPayments = payments.sumOf { it.amount }.coerceAtLeast(0.0)
    val paidMissing = paidFromField <= tolerance && paidFromPayments <= tolerance

    var paidResolved = when {
        paidFromField > tolerance -> paidFromField
        paidFromPayments > tolerance -> paidFromPayments
        else -> 0.0
    }.coerceAtMost(total)

    val outstandingProvided = outstandingAmount
        ?.coerceAtLeast(0.0)
        ?.coerceAtMost(total)
    var outstandingResolved = outstandingProvided ?: (total - paidResolved).coerceAtLeast(0.0)
    val inferredPaidFromOutstanding = (total - outstandingResolved).coerceAtLeast(0.0)
    val statusSuggestsPayment = status?.trim()?.lowercase() in setOf(
        "paid",
        "partly paid",
        "partly paid and discounted",
        "credit note issued"
    )

    if (outstandingProvided != null &&
        inferredPaidFromOutstanding > tolerance &&
        (paidMissing || statusSuggestsPayment ||
            kotlin.math.abs((total - paidResolved) - outstandingResolved) > tolerance)
    ) {
        paidResolved = inferredPaidFromOutstanding
    }

    if (outstandingProvided == null &&
        paidMissing &&
        isReturn != 1 &&
        total > 0.0 &&
        outstandingResolved < total - tolerance
    ) {
        paidResolved = 0.0
        outstandingResolved = total
    }

    if (outstandingProvided == null) {
        outstandingResolved = (total - paidResolved).coerceAtLeast(0.0)
    } else {
        val expectedOutstanding = (total - paidResolved).coerceAtLeast(0.0)
        if (kotlin.math.abs(expectedOutstanding - outstandingResolved) > tolerance) {
            paidResolved = inferredPaidFromOutstanding
            outstandingResolved = outstandingProvided
        }
    }

    paidResolved = roundToCurrency(paidResolved.coerceIn(0.0, total))
    outstandingResolved = roundToCurrency(outstandingResolved.coerceIn(0.0, total))
    if (kotlin.math.abs((total - paidResolved) - outstandingResolved) > tolerance) {
        paidResolved = roundToCurrency((total - outstandingResolved).coerceAtLeast(0.0))
    }

    return ResolvedInvoiceFinancials(
        paidAmount = paidResolved,
        outstandingAmount = outstandingResolved
    )
}

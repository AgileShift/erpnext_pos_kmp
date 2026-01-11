@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.utils

import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryCreateDto
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryReferenceCreateDto
import com.erpnext.pos.utils.oauth.CurrencySpec
import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.coerceAtLeastZero
import com.erpnext.pos.utils.oauth.isZero
import com.erpnext.pos.utils.oauth.minOfBd
import com.erpnext.pos.utils.oauth.moneyScale
import com.erpnext.pos.utils.oauth.roundCashIfNeeded
import com.erpnext.pos.utils.oauth.safeDiv
import com.erpnext.pos.utils.oauth.safeMul
import com.erpnext.pos.utils.oauth.toDouble
import com.erpnext.pos.utils.toErpDateTime
import com.erpnext.pos.views.POSContext
import com.erpnext.pos.views.billing.PaymentLine
import kotlin.time.Clock
import kotlin.math.pow
import kotlin.time.ExperimentalTime


fun normalizeCurrency(code: String?): String? {
    val normalized = code?.trim()?.uppercase()
    return normalized?.takeIf { it.isNotBlank() }
}

fun resolvePaymentCurrencyForMode(
    modeOfPayment: String,
    invoiceCurrency: String,
    paymentModeCurrencyByMode: Map<String, String>?,
    paymentModeDetails: Map<String, ModeOfPaymentEntity>
): String {
    val invoice = normalizeCurrency(invoiceCurrency) ?: "USD"
    val modeDefinition = paymentModeDetails[modeOfPayment]
    normalizeCurrency(modeDefinition?.currency)?.let { return it }
    normalizeCurrency(paymentModeCurrencyByMode?.get(modeOfPayment))?.let { return it }
    return invoice
}

fun buildPaymentModeDetailMap(definitions: List<ModeOfPaymentEntity>): Map<String, ModeOfPaymentEntity> {
    return definitions.fold(mutableMapOf<String, ModeOfPaymentEntity>()) { acc, definition ->
        acc[definition.modeOfPayment] = definition
        acc[definition.name] = definition
        acc
    }
}

fun buildPaymentModeCurrencyMap(definitions: List<ModeOfPaymentEntity>): Map<String, String> {
    return definitions.fold(mutableMapOf<String, String>()) { acc, definition ->
        definition.currency?.trim()?.uppercase()?.takeIf { it.isNotBlank() }?.let { currency ->
            acc[definition.modeOfPayment] = currency
            acc[definition.name] = currency
        }
        acc
    }
}

// -----------------------------------------------------------------------------
// Payment helpers (reusables para Billing, Customer, etc.)
// -----------------------------------------------------------------------------

suspend fun resolveRateToInvoiceCurrency(
    api: APIService,
    paymentCurrency: String,
    invoiceCurrency: String,
    cache: Map<String, Double>,
    rateResolver: (suspend (fromCurrency: String?, toCurrency: String?) -> Double?)? = null
): Double {
    val pay = normalizeCurrency(paymentCurrency) ?: return 1.0
    val inv = normalizeCurrency(invoiceCurrency) ?: return 1.0
    if (pay == inv) return 1.0

    cache[pay]?.takeIf { it > 0.0 }?.let { return it }

    rateResolver?.invoke(pay, inv)?.takeIf { it > 0.0 }?.let { return it }

    val direct = api.getExchangeRate(fromCurrency = pay, toCurrency = inv)?.takeIf { it > 0.0 }
    if (direct != null) return direct

    val reverse = api.getExchangeRate(fromCurrency = inv, toCurrency = pay)
        ?.takeIf { it > 0.0 }
        ?.let { 1 / it }

    return reverse ?: error("No se pudo resolver tasa $pay -> $inv")
}

suspend fun resolveRateToInvoiceCurrency(
    paymentCurrency: String,
    invoiceCurrency: String,
    cache: Map<String, Double>,
    rateResolver: suspend (fromCurrency: String, toCurrency: String) -> Double?
): Double {
    val pay = normalizeCurrency(paymentCurrency) ?: return 1.0
    val inv = normalizeCurrency(invoiceCurrency) ?: return 1.0
    if (pay == inv) return 1.0

    cache[pay]?.takeIf { it > 0.0 }?.let { return it }

    val direct = rateResolver(pay, inv)?.takeIf { it > 0.0 }
    if (direct != null) return direct

    val reverse = rateResolver(inv, pay)?.takeIf { it > 0.0 }?.let { 1 / it }
    return reverse ?: error("No se pudo resolver tasa $pay -> $inv")
}

suspend fun resolveRateToInvoiceCurrency(
    api: APIService,
    fromCurrency: String?,
    toCurrency: String?,
): Double? {
    val from = normalizeCurrency(fromCurrency) ?: return null
    val to = normalizeCurrency(toCurrency) ?: return null
    if (from == to) return 1.0

    val directRate = api.getExchangeRate(fromCurrency = from, toCurrency = to)?.takeIf { it > 0.0 }
    if (directRate != null) return directRate

    val reverseRate = api.getExchangeRate(fromCurrency = to, toCurrency = from)
        ?.takeIf { it > 0.0 }
        ?.let { 1 / it }

    return reverseRate
}

/**
 * Resuelve una tasa de cambio entre monedas.
 * - Intenta directo: from -> to
 * - Si falla, intenta inverso: to -> from y lo invierte
 */
suspend fun resolveExchangeRateBetween(
    api: APIService,
    fromCurrency: String?,
    toCurrency: String?,
): Double? {
    val from = normalizeCurrency(fromCurrency) ?: return null
    val to = normalizeCurrency(toCurrency) ?: return null
    if (from == to) return 1.0

    val directRate = api.getExchangeRate(fromCurrency = from, toCurrency = to)?.takeIf { it > 0.0 }
    if (directRate != null) return directRate

    val reverseRate = api.getExchangeRate(fromCurrency = to, toCurrency = from)
        ?.takeIf { it > 0.0 }
        ?.let { 1 / it }

    return reverseRate
}

data class PaymentStatus(
    val paidAmount: Double,
    val outstandingAmount: Double,
    val status: String
)

/**
 * Lógica de estado de pago. Para no acoplarlo a Billing, recibe una función `round`.
 */
fun resolvePaymentStatus(
    total: Double,
    paymentLines: List<PaymentLine>,
    round: (Double) -> Double
): PaymentStatus {
    val paidAmount = round(paymentLines.sumOf { it.baseAmount })
    val roundedTotal = round(total)
    val outstandingAmount = round((roundedTotal - paidAmount).coerceAtLeast(0.0))
    val status =
        if (outstandingAmount == roundedTotal) "Unpaid"
        else if ((roundedTotal - outstandingAmount) > 0.0) "Partly Paid"
        else "Paid"
    return PaymentStatus(paidAmount, outstandingAmount, status)
}

fun requiresReference(mode: POSPaymentModeOption?): Boolean {
    val type = mode?.type?.trim().orEmpty()
    return type.equals("Bank", ignoreCase = true) ||
            type.equals("Card", ignoreCase = true) ||
            mode?.modeOfPayment?.contains("bank", ignoreCase = true) == true ||
            mode?.modeOfPayment?.contains("card", ignoreCase = true) == true
}

fun buildCurrencySpecs(): Map<String, CurrencySpec> {
    // Ajusta aquí si agregas más monedas/escala por país.
    return mapOf(
        "NIO" to CurrencySpec(code = "NIO", minorUnits = 2, cashScale = 0),
        "USD" to CurrencySpec(code = "USD", minorUnits = 2, cashScale = 2),
    )
}

fun buildLocalPayments(
    invoiceId: String,
    postingDate: String,
    paymentLines: List<PaymentLine>
): List<POSInvoicePaymentEntity> {
    return paymentLines.map { line ->
        POSInvoicePaymentEntity(
            parentInvoice = invoiceId,
            modeOfPayment = line.modeOfPayment,
            amount = line.baseAmount,
            enteredAmount = line.enteredAmount,
            paymentCurrency = line.currency,
            exchangeRate = line.exchangeRate,
            paymentReference = line.referenceNumber?.takeIf { it.isNotBlank() },
            paymentDate = postingDate
        )
    }
}

data class InvoiceReceivableAmounts(
    val receivableCurrency: String,
    val totalRc: Double,
    val outstandingRc: Double
)

fun resolveInvoiceAmountsInReceivable(
    created: SalesInvoiceDto,
): InvoiceReceivableAmounts {
    val invoiceCurrency = (created.currency ?: "").trim().uppercase()
    val receivableCurrency = (created.partyAccountCurrency ?: created.currency ?: "")
        .trim().uppercase()

    if (receivableCurrency.isBlank()) {
        error("No se pudo resolver party_account_currency de la factura creada.")
    }

    val invoiceTotalInv = created.grandTotal
    val invoiceOutstandingInv = created.outstandingAmount ?: invoiceTotalInv

    // Caso simple: invoice currency == receivable currency
    if (invoiceCurrency.isNotBlank() && invoiceCurrency.equals(
            receivableCurrency,
            ignoreCase = true
        )
    ) {
        return InvoiceReceivableAmounts(
            receivableCurrency = receivableCurrency,
            totalRc = invoiceTotalInv,
            outstandingRc = invoiceOutstandingInv
        )
    }

    val baseTotal = created.baseGrandTotal
    if (baseTotal == null) {
        val paidRc = created.paidAmount
        val totalRc = if (paidRc > 0.0) paidRc + invoiceOutstandingInv else invoiceOutstandingInv
        return InvoiceReceivableAmounts(
            receivableCurrency = receivableCurrency,
            totalRc = totalRc,
            outstandingRc = invoiceOutstandingInv
        )
    }

    if (invoiceTotalInv <= 0.0) {
        error("grand_total inválido para inferir tasa invoice->receivable (grand_total=$invoiceTotalInv).")
    }

    // rate: (receivable per 1 invoice)
    val rateInvToRc = baseTotal / invoiceTotalInv
    if (rateInvToRc <= 0.0) {
        error("Tasa invoice->receivable inválida. base_grand_total=$baseTotal, grand_total=$invoiceTotalInv")
    }

    val totalRc = invoiceTotalInv * rateInvToRc
    val outstandingRc = invoiceOutstandingInv * rateInvToRc

    return InvoiceReceivableAmounts(
        receivableCurrency = receivableCurrency,
        totalRc = totalRc,
        outstandingRc = outstandingRc
    )
}

fun isSameMoneyAmount(a: Double, b: Double, decimals: Int): Boolean {
    val factor = decimals.toDouble().pow(10.0)
    val ra = kotlin.math.round(a * factor) / factor
    val rb = kotlin.math.round(b * factor) / factor
    return ra == rb
}

fun resolvePaymentToReceivableRate(
    paymentCurrency: String?,
    invoiceCurrency: String?,
    receivableCurrency: String?,
    paymentToInvoiceRate: Double?,
    invoiceToReceivableRate: Double?
): Double? {
    val pay = normalizeCurrency(paymentCurrency) ?: return null
    val rc = normalizeCurrency(receivableCurrency) ?: return null
    if (pay.equals(rc, ignoreCase = true)) return 1.0

    val inv = normalizeCurrency(invoiceCurrency) ?: rc
    val payToInv = paymentToInvoiceRate?.takeIf { it > 0.0 }
    val invToRc = invoiceToReceivableRate?.takeIf { it > 0.0 }

    return when {
        pay.equals(inv, ignoreCase = true) -> invToRc
        inv.equals(rc, ignoreCase = true) -> payToInv
        payToInv != null && invToRc != null -> payToInv * invToRc
        else -> null
    }
}

suspend fun buildPaymentEntryDto(
    api: APIService,
    line: PaymentLine,
    context: POSContext,
    customer: CustomerBO,
    postingDate: String,
    invoiceId: String,
    invoiceTotalRc: Double,
    outstandingRc: Double,
    paidFromAccount: String?,
    partyAccountCurrency: String?,
    invoiceCurrency: String?,
    invoiceToReceivableRate: Double?,
    exchangeRateByCurrency: Map<String, Double>,
    currencySpecs: Map<String, CurrencySpec>,
    paymentModeDetails: Map<String, ModeOfPaymentEntity>,
): PaymentEntryCreateDto {

    val paidFromResolved = paidFromAccount?.takeIf { it.isNotBlank() }
        ?: error("paidFromAccount requerido")

    val modeDefinition = paymentModeDetails[line.modeOfPayment]
    val paidToResolved = modeDefinition?.account?.takeIf { it.isNotBlank() }
        ?: error("Modo de pago ${line.modeOfPayment} sin cuenta configurada")

    val receivableCurrency = (partyAccountCurrency?.takeIf { it.isNotBlank() }
        ?: error("party_account_currency requerido"))
        .trim().uppercase()

    val paidToCurrency = (modeDefinition.currency?.takeIf { it.isNotBlank() }
        ?: line.currency.takeIf { it.isNotBlank() }
        ?: error("No se pudo resolver moneda de paid_to"))
        .trim().uppercase()
    val invoiceCurrencyResolved = normalizeCurrency(invoiceCurrency)
        ?: normalizeCurrency(line.currency)
        ?: receivableCurrency

    val rcSpec = currencySpecs[receivableCurrency]
        ?: CurrencySpec(code = receivableCurrency, minorUnits = 2, cashScale = 2)
    val toSpec = currencySpecs[paidToCurrency]
        ?: CurrencySpec(code = paidToCurrency, minorUnits = 2, cashScale = 2)

    val outstanding = bd(outstandingRc)
        .moneyScale(rcSpec.minorUnits)
        .coerceAtLeastZero()

    if (outstanding.isZero()) {
        error("Factura $invoiceId sin saldo pendiente en $receivableCurrency")
    }

    val entered = roundCashIfNeeded(bd(line.enteredAmount), toSpec)

    // Caso 1: paid_to == receivable
    if (paidToCurrency.equals(receivableCurrency, ignoreCase = true)) {
        val roundingTolerance = maxOf(
            1.0 / rcSpec.minorUnits.toDouble().pow(10.0),
            0.05
        )
        val allocated = if (entered.toDouble(2) + roundingTolerance >= outstanding.toDouble(2)) {
            outstanding
        } else {
            minOfBd(entered, outstanding).moneyScale(rcSpec.minorUnits)
        }

        return PaymentEntryCreateDto(
            company = context.company,
            postingDate = postingDate,
            paymentType = "Receive",
            partyType = "Customer",
            docStatus = 1,
            partyId = customer.name,
            modeOfPayment = line.modeOfPayment,
            paidAmount = allocated.toDouble(rcSpec.minorUnits),
            receivedAmount = allocated.toDouble(rcSpec.minorUnits),
            paidFrom = paidFromResolved,
            paidTo = paidToResolved,
            paidToAccountCurrency = paidToCurrency,
            sourceExchangeRate = null,
            targetExchangeRate = null,
            referenceNo = line.referenceNumber?.takeIf { it.isNotBlank() },
            referenceDate = if (!line.referenceNumber.isNullOrEmpty())
                Clock.System.now().toEpochMilliseconds().toErpDateTime()
            else null,
            references = listOf(
                PaymentEntryReferenceCreateDto(
                    referenceDoctype = "Sales Invoice",
                    referenceName = invoiceId,
                    totalAmount = bd(invoiceTotalRc).moneyScale(rcSpec.minorUnits)
                        .toDouble(rcSpec.minorUnits),
                    outstandingAmount = outstanding.toDouble(rcSpec.minorUnits),
                    allocatedAmount = allocated.toDouble(rcSpec.minorUnits)
                )
            )
        )
    }

    // Caso 2: paid_to != receivable
    val rateFromInputs = resolvePaymentToReceivableRate(
        paymentCurrency = paidToCurrency,
        invoiceCurrency = invoiceCurrencyResolved,
        receivableCurrency = receivableCurrency,
        paymentToInvoiceRate = line.exchangeRate,
        invoiceToReceivableRate = invoiceToReceivableRate
    )
    val rateDouble = rateFromInputs
        ?: resolveExchangeRateBetween(
            api = api,
            fromCurrency = paidToCurrency,
            toCurrency = receivableCurrency,
        )?.takeIf { it > 0.0 }
        ?: error("No se pudo resolver tasa $paidToCurrency -> $receivableCurrency")

    val rate = bd(rateDouble)

    val roundingTolerance = maxOf(
        1.0 / rcSpec.minorUnits.toDouble().pow(10.0),
        0.05
    )
    val deliveredRc = entered.safeMul(rate).moneyScale(rcSpec.minorUnits)
    val allocatedRc = if (deliveredRc.toDouble(2) + roundingTolerance >= outstanding.toDouble(2)) {
        outstanding
    } else {
        minOfBd(deliveredRc, outstanding).moneyScale(rcSpec.minorUnits)
    }

    val receivedEffective = allocatedRc
        .safeDiv(rate, scale = 8)
        .let { roundCashIfNeeded(it, toSpec) }

    return PaymentEntryCreateDto(
        company = context.company,
        postingDate = postingDate,
        paymentType = "Receive",
        partyType = "Customer",
        docStatus = 1,
        partyId = customer.name,
        modeOfPayment = line.modeOfPayment,
        paidAmount = allocatedRc.toDouble(rcSpec.minorUnits),
        receivedAmount = receivedEffective.toDouble(toSpec.cashScale.coerceAtMost(toSpec.minorUnits)),
        paidFrom = paidFromResolved,
        paidTo = paidToResolved,
        paidToAccountCurrency = paidToCurrency,
        sourceExchangeRate = 1.0,
        targetExchangeRate = rate.toDouble(6),
        referenceNo = line.referenceNumber?.takeIf { it.isNotBlank() },
        referenceDate = if (!line.referenceNumber.isNullOrEmpty())
            Clock.System.now().toEpochMilliseconds().toErpDateTime()
        else null,
        references = listOf(
            PaymentEntryReferenceCreateDto(
                referenceDoctype = "Sales Invoice",
                referenceName = invoiceId,
                totalAmount = bd(invoiceTotalRc).moneyScale(rcSpec.minorUnits)
                    .toDouble(rcSpec.minorUnits),
                outstandingAmount = outstanding.toDouble(rcSpec.minorUnits),
                allocatedAmount = allocatedRc.toDouble(rcSpec.minorUnits)
            )
        )
    )
}

/**
 * Convierte una PaymentLine a baseAmount sin acoplarla a Billing: recibe `round`.
 */
fun PaymentLine.toBaseAmount(round: (Double) -> Double): PaymentLine {
    return copy(baseAmount = round(enteredAmount * exchangeRate))
}

/**
 * Variante para casos donde el caller no tiene (o no quiere inyectar) un [APIService].
 * Reutiliza la misma lógica de [buildPaymentEntryDto], pero obtiene la tasa con un resolver externo.
 *
 * Regla de negocio: el resolver debe retornar la tasa `from -> to`.
 */
suspend fun buildPaymentEntryDtoWithRateResolver(
    rateResolver: suspend (fromCurrency: String?, toCurrency: String?) -> Double?,
    line: PaymentLine,
    context: POSContext,
    customer: CustomerBO,
    postingDate: String,
    invoiceId: String,
    invoiceTotalRc: Double,
    outstandingRc: Double,
    paidFromAccount: String?,
    partyAccountCurrency: String?,
    invoiceCurrency: String?,
    invoiceToReceivableRate: Double?,
    exchangeRateByCurrency: Map<String, Double>,
    currencySpecs: Map<String, CurrencySpec>,
    paymentModeDetails: Map<String, ModeOfPaymentEntity>,
): PaymentEntryCreateDto {
    // Reusa buildPaymentEntryDto internamente creando un API "virtual" via resolver.
    // Para mantener el comportamiento de cache de resolveRateToInvoiceCurrency, intentamos cache local primero.

    // Re-implementamos la parte de resolución de tasa con el resolver.
    val paidFromResolved = paidFromAccount?.takeIf { it.isNotBlank() }
        ?: error("paidFromAccount requerido")

    val modeDefinition = paymentModeDetails[line.modeOfPayment]
    val paidToResolved = modeDefinition?.account?.takeIf { it.isNotBlank() }
        ?: error("Modo de pago ${line.modeOfPayment} sin cuenta configurada")

    val receivableCurrency = (partyAccountCurrency?.takeIf { it.isNotBlank() }
        ?: error("party_account_currency requerido"))
        .trim().uppercase()

    val paidToCurrency = (modeDefinition.currency?.takeIf { it.isNotBlank() }
        ?: line.currency.takeIf { it.isNotBlank() }
        ?: error("No se pudo resolver moneda de paid_to"))
        .trim().uppercase()

    val rcSpec = currencySpecs[receivableCurrency]
        ?: CurrencySpec(code = receivableCurrency, minorUnits = 2, cashScale = 2)
    val toSpec = currencySpecs[paidToCurrency]
        ?: CurrencySpec(code = paidToCurrency, minorUnits = 2, cashScale = 2)

    val outstanding = bd(outstandingRc)
        .moneyScale(rcSpec.minorUnits)
        .coerceAtLeastZero()

    if (outstanding.isZero()) {
        error("Factura $invoiceId sin saldo pendiente en $receivableCurrency")
    }

    val entered = roundCashIfNeeded(bd(line.enteredAmount), toSpec)

    // Caso 1: paid_to == receivable
    if (paidToCurrency.equals(receivableCurrency, ignoreCase = true)) {
        val roundingTolerance = maxOf(
            1.0 / rcSpec.minorUnits.toDouble().pow(10.0),
            0.05
        )
        val allocated = if (entered.toDouble(2) + roundingTolerance >= outstanding.toDouble(2)) {
            outstanding
        } else {
            minOfBd(entered, outstanding).moneyScale(rcSpec.minorUnits)
        }

        return PaymentEntryCreateDto(
            company = context.company,
            postingDate = postingDate,
            paymentType = "Receive",
            partyType = "Customer",
            docStatus = 1,
            partyId = customer.name,
            modeOfPayment = line.modeOfPayment,
            paidAmount = allocated.toDouble(rcSpec.minorUnits),
            receivedAmount = allocated.toDouble(rcSpec.minorUnits),
            paidFrom = paidFromResolved,
            paidTo = paidToResolved,
            paidToAccountCurrency = paidToCurrency,
            sourceExchangeRate = null,
            targetExchangeRate = null,
            referenceNo = line.referenceNumber?.takeIf { it.isNotBlank() },
            referenceDate = if (!line.referenceNumber.isNullOrEmpty())
                Clock.System.now().toEpochMilliseconds().toErpDateTime()
            else null,
            references = listOf(
                PaymentEntryReferenceCreateDto(
                    referenceDoctype = "Sales Invoice",
                    referenceName = invoiceId,
                    totalAmount = bd(invoiceTotalRc).moneyScale(rcSpec.minorUnits)
                        .toDouble(rcSpec.minorUnits),
                    outstandingAmount = outstanding.toDouble(rcSpec.minorUnits),
                    allocatedAmount = allocated.toDouble(rcSpec.minorUnits)
                )
            )
        )
    }

    val invoiceCurrencyResolved = normalizeCurrency(invoiceCurrency)
        ?: normalizeCurrency(line.currency)
        ?: receivableCurrency
    // Caso 2: paid_to != receivable
    val rateFromInputs = resolvePaymentToReceivableRate(
        paymentCurrency = paidToCurrency,
        invoiceCurrency = invoiceCurrencyResolved,
        receivableCurrency = receivableCurrency,
        paymentToInvoiceRate = line.exchangeRate,
        invoiceToReceivableRate = invoiceToReceivableRate
    )
    // Priorizamos caché solo cuando invoice == receivable (misma base de tasa).
    val cachedRate = if (invoiceCurrencyResolved.equals(receivableCurrency, ignoreCase = true)) {
        exchangeRateByCurrency[paidToCurrency]?.takeIf { it > 0.0 }
    } else {
        null
    }
    val rateDouble = rateFromInputs
        ?: cachedRate
        ?: rateResolver(paidToCurrency, receivableCurrency)
            ?.takeIf { it > 0.0 }
        ?: error("No se pudo resolver tasa $paidToCurrency -> $receivableCurrency")

    val rate = bd(rateDouble)

    val roundingTolerance = maxOf(
        1.0 / rcSpec.minorUnits.toDouble().pow(10.0),
        0.05
    )
    val deliveredRc = entered.safeMul(rate).moneyScale(rcSpec.minorUnits)
    val allocatedRc = if (deliveredRc.toDouble(2) + roundingTolerance >= outstanding.toDouble(2)) {
        outstanding
    } else {
        minOfBd(deliveredRc, outstanding).moneyScale(rcSpec.minorUnits)
    }

    val receivedEffective = allocatedRc
        .safeDiv(rate, scale = 8)
        .let { roundCashIfNeeded(it, toSpec) }

    return PaymentEntryCreateDto(
        company = context.company,
        postingDate = postingDate,
        paymentType = "Receive",
        partyType = "Customer",
        docStatus = 1,
        partyId = customer.name,
        modeOfPayment = line.modeOfPayment,
        paidAmount = allocatedRc.toDouble(rcSpec.minorUnits),
        receivedAmount = receivedEffective.toDouble(toSpec.cashScale.coerceAtMost(toSpec.minorUnits)),
        paidFrom = paidFromResolved,
        paidTo = paidToResolved,
        paidToAccountCurrency = paidToCurrency,
        sourceExchangeRate = 1.0,
        targetExchangeRate = rate.toDouble(6),
        referenceNo = line.referenceNumber?.takeIf { it.isNotBlank() },
        referenceDate = if (!line.referenceNumber.isNullOrEmpty())
            Clock.System.now().toEpochMilliseconds().toErpDateTime()
        else null,
        references = listOf(
            PaymentEntryReferenceCreateDto(
                referenceDoctype = "Sales Invoice",
                referenceName = invoiceId,
                totalAmount = bd(invoiceTotalRc).moneyScale(rcSpec.minorUnits)
                    .toDouble(rcSpec.minorUnits),
                outstandingAmount = outstanding.toDouble(rcSpec.minorUnits),
                allocatedAmount = allocatedRc.toDouble(rcSpec.minorUnits)
            )
        )
    )
}

/**
 * Helper puro (sin I/O) para resolver tasa entre monedas a partir de un mapa de tasas base.
 * `baseRates` debe ser: baseCurrency -> { currency -> rate }.
 */
fun resolveRateBetweenFromBaseRates(
    fromCurrency: String,
    toCurrency: String,
    baseCurrency: String,
    baseRates: Map<String, Double>
): Double? {
    val from = fromCurrency.trim().uppercase()
    val to = toCurrency.trim().uppercase()
    val base = baseCurrency.trim().uppercase()
    if (from == to) return 1.0

    val baseToFrom = if (from == base) 1.0 else baseRates[from]
    val baseToTo = if (to == base) 1.0 else baseRates[to]
    if (baseToFrom == null || baseToTo == null || baseToFrom <= 0.0 || baseToTo <= 0.0) return null
    return baseToTo / baseToFrom
}

fun formatPendingDual(
    pendingInBase: Double,
    posBaseCurrency: String,
    customerCurrency: String?,
    baseRates: Map<String, Double>?
): Pair<String, String?> {
    val base = normalizeCurrency(posBaseCurrency) ?: "USD"
    val customer = normalizeCurrency(customerCurrency) ?: base

    val baseSymbol = base.toCurrencySymbol().ifBlank { base }
    val baseLabel = formatAmount(baseSymbol, pendingInBase)

    if (customer.equals(base, ignoreCase = true)) {
        return baseLabel to null
    }

    val customerSymbol = customer.toCurrencySymbol().ifBlank { customer }

    val rate = if (baseRates.isNullOrEmpty()) {
        null
    } else {
        resolveRateBetweenFromBaseRates(
            fromCurrency = base,
            toCurrency = customer,
            baseCurrency = base,
            baseRates = baseRates
        )
    }

    val customerLabel = rate?.let { r ->
        formatAmount(customerSymbol, pendingInBase * r)
    } ?: "$customerSymbol --"

    return baseLabel to customerLabel
}

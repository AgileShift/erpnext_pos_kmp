@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.utils

import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.remoteSource.api.APIService
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
import com.erpnext.pos.views.POSContext
import com.erpnext.pos.views.billing.PaymentLine
import kotlin.time.Clock
import kotlin.math.pow
import kotlin.time.ExperimentalTime


fun normalizeCurrency(code: String?): String {
    val normalized = code?.trim()?.uppercase()
    return normalized?.takeIf { it.isNotBlank() } ?: "NIO"
}

fun resolvePaymentCurrencyForMode(
    modeOfPayment: String,
    paymentModeDetails: Map<String, ModeOfPaymentEntity>,
    preferredCurrency: String? = null,
    invoiceCurrency: String? = null
): String {
    val preferred = preferredCurrency?.trim()?.uppercase()
    if (!preferred.isNullOrBlank()) return preferred
    val modeDefinition = paymentModeDetails[modeOfPayment]
    val fromMode = modeDefinition?.currency?.trim()?.uppercase()
    if (!fromMode.isNullOrBlank()) return fromMode
    val inferred = inferCurrencyFromModeName(modeOfPayment)
    if (!inferred.isNullOrBlank()) return inferred
    return normalizeCurrency(invoiceCurrency)
}

private fun inferCurrencyFromModeName(modeOfPayment: String): String? {
    val upper = modeOfPayment.trim().uppercase()
    return when {
        upper.contains("USD") || upper.contains("DOLAR") -> "USD"
        upper.contains("NIO") || upper.contains("CORDO") -> "NIO"
        else -> null
    }
}

fun buildPaymentModeDetailMap(definitions: List<ModeOfPaymentEntity>): Map<String, ModeOfPaymentEntity> {
    return definitions.fold(mutableMapOf()) { acc, definition ->
        acc[definition.modeOfPayment] = definition
        acc[definition.name] = definition
        acc
    }
}

suspend fun resolveRateToInvoiceCurrency(
    paymentCurrency: String,
    invoiceCurrency: String,
    cache: Map<String, Double>,
    rateResolver: suspend (fromCurrency: String, toCurrency: String) -> Double?
): Double {
    val pay = normalizeCurrency(paymentCurrency)
    val inv = normalizeCurrency(invoiceCurrency)
    if (pay == inv) return 1.0

    cache[pay]?.takeIf { it > 0.0 }?.let { return it }

    val direct = rateResolver(pay, inv)?.takeIf { it > 0.0 }
    if (direct != null) return direct

    val reverse = rateResolver(inv, pay)?.takeIf { it > 0.0 }?.let { 1 / it }
    return reverse ?: error("No se pudo resolver tasa $pay -> $inv")
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
    val from = normalizeCurrency(fromCurrency)
    val to = normalizeCurrency(toCurrency)
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
        // USA
        "NIO" to CurrencySpec(code = "NIO", minorUnits = 2, cashScale = 0),
        "USD" to CurrencySpec(code = "USD", minorUnits = 0, cashScale = 0),
        // LATAM (continental)
        "MXN" to CurrencySpec(code = "MXN", minorUnits = 2, cashScale = 2),
        "GTQ" to CurrencySpec(code = "GTQ", minorUnits = 2, cashScale = 2),
        "HNL" to CurrencySpec(code = "HNL", minorUnits = 2, cashScale = 2),
        "CRC" to CurrencySpec(code = "CRC", minorUnits = 2, cashScale = 2),
        "PAB" to CurrencySpec(code = "PAB", minorUnits = 2, cashScale = 2),
        "BZD" to CurrencySpec(code = "BZD", minorUnits = 2, cashScale = 2),
        "ARS" to CurrencySpec(code = "ARS", minorUnits = 2, cashScale = 2),
        "BOB" to CurrencySpec(code = "BOB", minorUnits = 2, cashScale = 2),
        "BRL" to CurrencySpec(code = "BRL", minorUnits = 2, cashScale = 2),
        "CLP" to CurrencySpec(code = "CLP", minorUnits = 0, cashScale = 0),
        "COP" to CurrencySpec(code = "COP", minorUnits = 2, cashScale = 2),
        "PEN" to CurrencySpec(code = "PEN", minorUnits = 2, cashScale = 2),
        "PYG" to CurrencySpec(code = "PYG", minorUnits = 0, cashScale = 0),
        "UYU" to CurrencySpec(code = "UYU", minorUnits = 2, cashScale = 2),
        "VES" to CurrencySpec(code = "VES", minorUnits = 2, cashScale = 2),
        "GYD" to CurrencySpec(code = "GYD", minorUnits = 2, cashScale = 2),
        "SRD" to CurrencySpec(code = "SRD", minorUnits = 2, cashScale = 2),
    )
}

fun resolveMinorUnits(
    currency: String?,
    specs: Map<String, CurrencySpec> = buildCurrencySpecs()
): Int {
    val code = normalizeCurrency(currency)
    return specs[code]?.minorUnits ?: 2
}

fun resolveMinorUnitTolerance(
    currency: String?,
    specs: Map<String, CurrencySpec> = buildCurrencySpecs()
): Double {
    val units = resolveMinorUnits(currency, specs)
    return 1.0 / 10.0.pow(units)
}

fun buildLocalPayments(
    invoiceId: String,
    postingDate: String,
    paymentLines: List<PaymentLine>,
    posOpeningEntry: String?,
    remotePaymentEntries: Map<String, String?> = emptyMap()
): List<POSInvoicePaymentEntity> {
    return paymentLines.map { line ->
        val reference = line.referenceNumber?.takeIf { it.isNotBlank() }
        val remoteEntry = reference?.let { remotePaymentEntries[it] }
        POSInvoicePaymentEntity(
            parentInvoice = invoiceId,
            modeOfPayment = line.modeOfPayment,
            amount = line.baseAmount,
            enteredAmount = line.enteredAmount,
            paymentCurrency = line.currency,
            exchangeRate = line.exchangeRate,
            paymentReference = reference,
            remotePaymentEntry = remoteEntry,
            paymentDate = postingDate,
            posOpeningEntry = posOpeningEntry,
            syncStatus = if (remoteEntry != null) "Synced" else "Pending"
        )
    }
}

data class InvoiceReceivableAmounts(
    val receivableCurrency: String,
    val totalRc: Double,
    val outstandingRc: Double
)
fun resolvePaymentToReceivableRate(
    paymentCurrency: String?,
    invoiceCurrency: String?,
    receivableCurrency: String?,
    paymentToInvoiceRate: Double?,
    invoiceToReceivableRate: Double?
): Double? {
    val pay = normalizeCurrency(paymentCurrency)
    val rc = normalizeCurrency(receivableCurrency)
    if (pay.equals(rc, ignoreCase = true)) return 1.0

    val inv = normalizeCurrency(invoiceCurrency)
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
    currencySpecs: Map<String, CurrencySpec>,
    paymentModeDetails: Map<String, ModeOfPaymentEntity>,
    referenceDoctype: String = "Sales Invoice"
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
    val rcSpec = currencySpecs[receivableCurrency]
        ?: CurrencySpec(code = receivableCurrency, minorUnits = 2, cashScale = 2)
    val toSpec = currencySpecs[paidToCurrency]
        ?: CurrencySpec(code = paidToCurrency, minorUnits = 2, cashScale = 2)
    val invoiceCurrencyResolved = normalizeCurrency(invoiceCurrency)
    val rateInvToRc = invoiceToReceivableRate?.takeIf { it > 0.0 }

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
    val outstandingAdjusted = bd(outstandingRc - roundingTolerance)
        .moneyScale(rcSpec.minorUnits)
        .coerceAtLeastZero()
    val allocated = if (entered.toDouble(2) + roundingTolerance >= outstanding.toDouble(2)) {
        outstandingAdjusted
    } else {
        minOfBd(entered, outstandingAdjusted).moneyScale(rcSpec.minorUnits)
    }
        return PaymentEntryCreateDto(
            company = context.company,
            postingDate = postingDate,
            paymentType = "Receive",
            partyType = "Customer",
            docStatus = 0,
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
                    referenceDoctype = referenceDoctype,
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
    val outstandingAdjusted2 = bd(outstandingRc - roundingTolerance)
        .moneyScale(rcSpec.minorUnits)
        .coerceAtLeastZero()
    val allocatedRc = if (deliveredRc.toDouble(2) + roundingTolerance >= outstanding.toDouble(2)) {
        outstandingAdjusted2
    } else {
        minOfBd(deliveredRc, outstandingAdjusted2).moneyScale(rcSpec.minorUnits)
    }
    val receivedEffective = allocatedRc
        .safeDiv(rate, scale = 8)
        .let { roundCashIfNeeded(it, toSpec) }

    return PaymentEntryCreateDto(
        company = context.company,
        postingDate = postingDate,
        paymentType = "Receive",
        partyType = "Customer",
        docStatus = 0,
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
                referenceDoctype = referenceDoctype,
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
    referenceDoctype: String = "Sales Invoice",
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
    val outstandingAdjusted = bd(outstandingRc - roundingTolerance)
        .moneyScale(rcSpec.minorUnits)
        .coerceAtLeastZero()
    val allocated = if (entered.toDouble(2) + roundingTolerance >= outstanding.toDouble(2)) {
        outstandingAdjusted
    } else {
        minOfBd(entered, outstandingAdjusted).moneyScale(rcSpec.minorUnits)
    }

        return PaymentEntryCreateDto(
            company = context.company,
            postingDate = postingDate,
            paymentType = "Receive",
            partyType = "Customer",
            docStatus = 0,
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
                    referenceDoctype = referenceDoctype,
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
    val outstandingAdjusted2 = bd(outstandingRc - roundingTolerance)
        .moneyScale(rcSpec.minorUnits)
        .coerceAtLeastZero()
    val allocatedRc = if (deliveredRc.toDouble(2) + roundingTolerance >= outstanding.toDouble(2)) {
        outstandingAdjusted2
    } else {
        minOfBd(deliveredRc, outstandingAdjusted2).moneyScale(rcSpec.minorUnits)
    }

    val receivedEffective = allocatedRc
        .safeDiv(rate, scale = 8)
        .let { roundCashIfNeeded(it, toSpec) }

    return PaymentEntryCreateDto(
        company = context.company,
        postingDate = postingDate,
        paymentType = "Receive",
        partyType = "Customer",
        docStatus = 0,
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
                referenceDoctype = referenceDoctype,
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
    return baseToFrom / baseToTo
}

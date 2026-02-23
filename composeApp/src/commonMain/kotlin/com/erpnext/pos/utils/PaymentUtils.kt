@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.utils

import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.PaymentEntryCreateDto
import com.erpnext.pos.remoteSource.dto.PaymentEntryReferenceCreateDto
import com.erpnext.pos.utils.oauth.CurrencySpec
import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.coerceAtLeastZero
import com.erpnext.pos.utils.oauth.isZero
import com.erpnext.pos.utils.oauth.minOfBd
import com.erpnext.pos.utils.oauth.moneyScale
import com.erpnext.pos.utils.oauth.moneyScaleDown
import com.erpnext.pos.utils.oauth.roundCashIfNeeded
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
    val base = mapOf(
        // USA
        "NIO" to CurrencySpec(code = "NIO", minorUnits = 2, cashScale = 2),
        "USD" to CurrencySpec(code = "USD", minorUnits = 2, cashScale = 2),
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
    return base.mapValues { (code, spec) ->
        val precision = CurrencyPrecisionResolver.precisionFor(code)
        val cashScale = CurrencyPrecisionResolver.cashScaleFor(code)
        spec.copy(minorUnits = precision, cashScale = cashScale)
    }
}

fun resolveCurrencySpec(
    currency: String?,
    specs: Map<String, CurrencySpec> = buildCurrencySpecs()
): CurrencySpec {
    val code = normalizeCurrency(currency)
    val precision = CurrencyPrecisionResolver.precisionFor(code)
    val cashScale = CurrencyPrecisionResolver.cashScaleFor(code)
    return specs[code]?.copy(minorUnits = precision, cashScale = cashScale)
        ?: CurrencySpec(code = code, minorUnits = precision, cashScale = cashScale)
}

private fun minorUnitEpsilon(minorUnits: Int): Double {
    val units = minorUnits.coerceAtLeast(0)
    return if (units == 0) 0.5 else 1.0 / (2.0 * 10.0.pow(units.toDouble()))
}

fun resolveMinorUnits(
    currency: String?,
    specs: Map<String, CurrencySpec> = buildCurrencySpecs()
): Int {
    val code = normalizeCurrency(currency)
    return specs[code]?.minorUnits ?: CurrencyPrecisionResolver.precisionFor(code)
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

    val enteredCurrency = line.currency.takeIf { it.isNotBlank() }
        ?.trim()?.uppercase()
        ?: error("No se pudo resolver moneda del pago ingresado")
    val paidToCurrency = (modeDefinition.currency?.takeIf { it.isNotBlank() }
        ?: enteredCurrency
        ?: error("No se pudo resolver moneda de paid_to"))
        .trim().uppercase()
    val rcSpec = resolveCurrencySpec(receivableCurrency, currencySpecs)
    val enteredSpec = resolveCurrencySpec(enteredCurrency, currencySpecs)
    val accountSpec = resolveCurrencySpec(paidToCurrency, currencySpecs)
    val invoiceCurrencyResolved = normalizeCurrency(invoiceCurrency)

    val outstanding = bd(outstandingRc)
        .moneyScaleDown(rcSpec.minorUnits)
        .coerceAtLeastZero()

    if (outstanding.isZero()) {
        error("Factura $invoiceId sin saldo pendiente en $receivableCurrency")
    }

    val entered = roundCashIfNeeded(bd(line.enteredAmount), enteredSpec)
    val roundingTolerance = minorUnitEpsilon(rcSpec.minorUnits)

    val rateFromInputsToReceivable = resolvePaymentToReceivableRate(
        paymentCurrency = enteredCurrency,
        invoiceCurrency = invoiceCurrencyResolved,
        receivableCurrency = receivableCurrency,
        paymentToInvoiceRate = line.exchangeRate,
        invoiceToReceivableRate = invoiceToReceivableRate
    )
    val rateEnteredToReceivable = rateFromInputsToReceivable
        ?: resolveExchangeRateBetween(
            api = api,
            fromCurrency = enteredCurrency,
            toCurrency = receivableCurrency,
        )?.takeIf { it > 0.0 }
        ?: error("No se pudo resolver tasa $enteredCurrency -> $receivableCurrency")

    val deliveredRc = entered.safeMul(bd(rateEnteredToReceivable)).moneyScaleDown(rcSpec.minorUnits)
    val outstandingValue = outstanding.toDouble(rcSpec.minorUnits)
    val allocatedRc = if (deliveredRc.toDouble(rcSpec.minorUnits) + roundingTolerance >= outstandingValue) {
        outstanding
    } else {
        minOfBd(deliveredRc, outstanding).moneyScaleDown(rcSpec.minorUnits)
    }

    val receivedEffective = when {
        paidToCurrency.equals(receivableCurrency, ignoreCase = true) -> allocatedRc
        enteredCurrency.equals(paidToCurrency, ignoreCase = true) -> entered
        invoiceCurrencyResolved.equals(paidToCurrency, ignoreCase = true) && line.exchangeRate > 0.0 ->
            entered.safeMul(bd(line.exchangeRate)).moneyScaleDown(accountSpec.minorUnits)
        else -> {
            val rateEnteredToAccount = resolveExchangeRateBetween(
                api = api,
                fromCurrency = enteredCurrency,
                toCurrency = paidToCurrency
            )?.takeIf { it > 0.0 }
                ?: error("No se pudo resolver tasa $enteredCurrency -> $paidToCurrency")
            entered.safeMul(bd(rateEnteredToAccount)).moneyScaleDown(accountSpec.minorUnits)
        }
    }

    return PaymentEntryCreateDto(
        company = context.company,
        postingDate = postingDate,
        paymentType = "Receive",
        partyType = "Customer",
        docStatus = 0,
        partyId = customer.name,
        modeOfPayment = line.modeOfPayment,
        paidAmount = allocatedRc.toDouble(rcSpec.minorUnits),
        receivedAmount = receivedEffective.toDouble(accountSpec.cashScale.coerceAtMost(accountSpec.minorUnits)),
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
                totalAmount = bd(invoiceTotalRc).moneyScaleDown(rcSpec.minorUnits)
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

    val enteredCurrency = line.currency.takeIf { it.isNotBlank() }
        ?.trim()?.uppercase()
        ?: error("No se pudo resolver moneda del pago ingresado")
    val paidToCurrency = (modeDefinition.currency?.takeIf { it.isNotBlank() }
        ?: enteredCurrency
        ?: error("No se pudo resolver moneda de paid_to"))
        .trim().uppercase()

    val rcSpec = resolveCurrencySpec(receivableCurrency, currencySpecs)
    val enteredSpec = resolveCurrencySpec(enteredCurrency, currencySpecs)
    val accountSpec = resolveCurrencySpec(paidToCurrency, currencySpecs)

    val outstanding = bd(outstandingRc)
        .moneyScaleDown(rcSpec.minorUnits)
        .coerceAtLeastZero()

    if (outstanding.isZero()) {
        error("Factura $invoiceId sin saldo pendiente en $receivableCurrency")
    }

    val entered = roundCashIfNeeded(bd(line.enteredAmount), enteredSpec)
    val roundingTolerance = minorUnitEpsilon(rcSpec.minorUnits)

    val invoiceCurrencyResolved = normalizeCurrency(invoiceCurrency)
    val rateFromInputsToReceivable = resolvePaymentToReceivableRate(
        paymentCurrency = enteredCurrency,
        invoiceCurrency = invoiceCurrencyResolved,
        receivableCurrency = receivableCurrency,
        paymentToInvoiceRate = line.exchangeRate,
        invoiceToReceivableRate = invoiceToReceivableRate
    )
    // Priorizamos caché solo cuando invoice == receivable (misma base de tasa).
    val cachedRateToReceivable = if (invoiceCurrencyResolved.equals(receivableCurrency, ignoreCase = true)) {
        exchangeRateByCurrency[enteredCurrency]?.takeIf { it > 0.0 }
    } else {
        null
    }
    val rateEnteredToReceivable = rateFromInputsToReceivable
        ?: cachedRateToReceivable
        ?: rateResolver(enteredCurrency, receivableCurrency)
            ?.takeIf { it > 0.0 }
        ?: error("No se pudo resolver tasa $enteredCurrency -> $receivableCurrency")

    val deliveredRc = entered.safeMul(bd(rateEnteredToReceivable)).moneyScaleDown(rcSpec.minorUnits)
    val outstandingValue = outstanding.toDouble(rcSpec.minorUnits)
    val allocatedRc = if (deliveredRc.toDouble(rcSpec.minorUnits) + roundingTolerance >= outstandingValue) {
        outstanding
    } else {
        minOfBd(deliveredRc, outstanding).moneyScaleDown(rcSpec.minorUnits)
    }

    val receivedEffective = when {
        paidToCurrency.equals(receivableCurrency, ignoreCase = true) -> allocatedRc
        enteredCurrency.equals(paidToCurrency, ignoreCase = true) -> entered
        invoiceCurrencyResolved.equals(paidToCurrency, ignoreCase = true) && line.exchangeRate > 0.0 ->
            entered.safeMul(bd(line.exchangeRate)).moneyScaleDown(accountSpec.minorUnits)
        else -> {
            val cachedToAccount = if (invoiceCurrencyResolved.equals(paidToCurrency, ignoreCase = true)) {
                exchangeRateByCurrency[enteredCurrency]?.takeIf { it > 0.0 }
            } else {
                null
            }
            val rateEnteredToAccount = cachedToAccount
                ?: rateResolver(enteredCurrency, paidToCurrency)?.takeIf { it > 0.0 }
                ?: error("No se pudo resolver tasa $enteredCurrency -> $paidToCurrency")
            entered.safeMul(bd(rateEnteredToAccount)).moneyScaleDown(accountSpec.minorUnits)
        }
    }

    return PaymentEntryCreateDto(
        company = context.company,
        postingDate = postingDate,
        paymentType = "Receive",
        partyType = "Customer",
        docStatus = 0,
        partyId = customer.name,
        modeOfPayment = line.modeOfPayment,
        paidAmount = allocatedRc.toDouble(rcSpec.minorUnits),
        receivedAmount = receivedEffective.toDouble(accountSpec.cashScale.coerceAtMost(accountSpec.minorUnits)),
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
                totalAmount = bd(invoiceTotalRc).moneyScaleDown(rcSpec.minorUnits)
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

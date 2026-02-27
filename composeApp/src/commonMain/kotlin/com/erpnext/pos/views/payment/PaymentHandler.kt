package com.erpnext.pos.views.payment

import com.erpnext.pos.data.repositories.ExchangeRateRepository
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.usecases.CreatePaymentEntryInput
import com.erpnext.pos.domain.usecases.CreatePaymentEntryUseCase
import com.erpnext.pos.domain.usecases.SaveInvoicePaymentsInput
import com.erpnext.pos.domain.usecases.SaveInvoicePaymentsUseCase
import com.erpnext.pos.domain.utils.UUIDGenerator
import com.erpnext.pos.localSource.datasources.InvoiceLocalSource
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.utils.InvoiceReceivableAmounts
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.buildCurrencySpecs
import com.erpnext.pos.utils.buildLocalPayments
import com.erpnext.pos.utils.buildPaymentEntryDto
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.oauth.CurrencySpec
import com.erpnext.pos.utils.resolveMinorUnitTolerance
import com.erpnext.pos.utils.resolvePaymentCurrencyForMode
import com.erpnext.pos.utils.resolvePaymentToReceivableRate
import com.erpnext.pos.utils.resolveRateToInvoiceCurrency
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.utils.toBaseAmount
import com.erpnext.pos.views.POSContext
import com.erpnext.pos.views.billing.PaymentLine
import kotlinx.coroutines.flow.first
import kotlin.math.pow
import kotlin.math.round

class PaymentHandler(
    private val api: APIService,
    private val createPaymentEntryUseCase: CreatePaymentEntryUseCase,
    private val saveInvoicePaymentsUseCase: SaveInvoicePaymentsUseCase,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val invoiceLocalSource: InvoiceLocalSource,
    private val networkMonitor: NetworkMonitor
) {

    data class PaymentLineResult(
        val line: PaymentLine,
        val exchangeRateByCurrency: Map<String, Double>
    )

    data class PaymentRegistrationResult(
        val remotePaymentsSucceeded: Boolean,
        val invoiceNameForLocal: String,
        val outstandingRemaining: Double?
    )

    suspend fun resolvePaymentLine(
        line: PaymentLine,
        invoiceCurrencyInput: String,
        paymentModeDetails: Map<String, ModeOfPaymentEntity>,
        exchangeRateByCurrency: Map<String, Double>,
        round: (Double) -> Double
    ): PaymentLineResult {
        val invoiceCurrency = normalizeCurrency(invoiceCurrencyInput)
        val paymentCurrency = resolvePaymentCurrencyForMode(
            modeOfPayment = line.modeOfPayment,
            paymentModeDetails = paymentModeDetails,
            preferredCurrency = line.currency,
            invoiceCurrency = invoiceCurrency
        )

        val resolvedRate = resolveRateToInvoiceCurrency(
            paymentCurrency = paymentCurrency,
            invoiceCurrency = invoiceCurrency,
            cache = exchangeRateByCurrency,
            rateResolver = { from, to ->
                exchangeRateRepository.getLocalRate(from, to)
            }
        )
        val rate = if (paymentCurrency.equals(invoiceCurrency, ignoreCase = true)) 1.0 else resolvedRate

        val resolvedReference = line.referenceNumber?.takeIf { it.isNotBlank() }
            ?: "POSPAY-${UUIDGenerator().newId()}"

        val fixed = line.copy(
            currency = paymentCurrency,
            exchangeRate = rate,
            referenceNumber = resolvedReference
        ).toBaseAmount(round)

        val payKey = paymentCurrency.uppercase()
        val invKey = invoiceCurrency.uppercase()
        val newCache = exchangeRateByCurrency
            .plus(invKey to 1.0)
            .plus(payKey to rate)

        return PaymentLineResult(fixed, newCache)
    }

    suspend fun registerPayments(
        paymentLines: List<PaymentLine>,
        createdInvoice: SalesInvoiceDto?,
        invoiceNameForLocal: String,
        postingDate: String,
        context: POSContext,
        customer: CustomerBO,
        exchangeRateByCurrency: Map<String, Double>,
        paymentModeDetails: Map<String, ModeOfPaymentEntity>,
        posOpeningEntry: String?
    ): PaymentRegistrationResult {
        if (paymentLines.isEmpty()) {
            return PaymentRegistrationResult(
                remotePaymentsSucceeded = false,
                invoiceNameForLocal = invoiceNameForLocal,
                outstandingRemaining = null
            )
        }

        val currencySpecs = buildCurrencySpecs()
        val companyCurrency = normalizeCurrency(context.companyCurrency)
        var resolvedReceivableCurrency = normalizeCurrency(createdInvoice?.partyAccountCurrency)
        var resolvedInvoiceCurrency = normalizeCurrency(createdInvoice?.currency)

        val rateInvToRc =
            com.erpnext.pos.utils.CurrencyService.resolveInvoiceToReceivableRateUnified(
                invoiceCurrency = resolvedInvoiceCurrency,
                receivableCurrency = resolvedReceivableCurrency,
                conversionRate = createdInvoice?.conversionRate,
                customExchangeRate = createdInvoice?.customExchangeRate,
                posCurrency = context.currency,
                posExchangeRate = context.exchangeRate,
                rateResolver = { from, to ->
                    resolveRateBetweenCurrencies(
                        fromCurrency = from,
                        toCurrency = to,
                        context = context
                    )
                }
            )?.takeIf { it > 0.0 }

        var receivableAmounts: InvoiceReceivableAmounts? = createdInvoice?.let {
            val invoiceTotalInv = it.grandTotal
            val invoiceOutstandingInv = it.outstandingAmount
            val totalRc = when {
                resolvedInvoiceCurrency.equals(resolvedReceivableCurrency, ignoreCase = true) ->
                    invoiceTotalInv

                resolvedReceivableCurrency.equals(companyCurrency, ignoreCase = true) &&
                    it.baseGrandTotal != null && it.baseGrandTotal > 0.0 -> it.baseGrandTotal

                rateInvToRc != null -> invoiceTotalInv * rateInvToRc
                else -> invoiceTotalInv
            }
            val outstandingRc = when {
                resolvedInvoiceCurrency.equals(resolvedReceivableCurrency, ignoreCase = true) ->
                    invoiceOutstandingInv ?: totalRc

                resolvedReceivableCurrency.equals(companyCurrency, ignoreCase = true) &&
                    it.baseGrandTotal != null -> {
                    val basePaid = it.basePaidAmount ?: 0.0
                    (it.baseGrandTotal - basePaid).coerceAtLeast(0.0)
                }

                rateInvToRc != null -> {
                    val invOutstanding = invoiceOutstandingInv ?: invoiceTotalInv
                    (invOutstanding * rateInvToRc).coerceAtLeast(0.0)
                }

                else -> invoiceOutstandingInv ?: totalRc
            }
            InvoiceReceivableAmounts(
                receivableCurrency = resolvedReceivableCurrency,
                totalRc = totalRc,
                outstandingRc = outstandingRc
            )
        }
        var remotePaymentsSucceeded = false
        var remainingOutstandingRc: Double? = receivableAmounts?.outstandingRc
            ?: createdInvoice?.outstandingAmount?.takeIf { it > 0.0 }
        if (paymentLines.isNotEmpty() && ((remainingOutstandingRc ?: 0.0) <= 0.0)) {
            remainingOutstandingRc = receivableAmounts?.totalRc?.takeIf { it > 0.0 }
                ?: createdInvoice?.grandTotal?.takeIf { it > 0.0 }
        }

        val isOnline = networkMonitor.isConnected.first()
        val remoteEntryByReference = mutableMapOf<String, String?>()

        if (createdInvoice != null) {
            val resolvedInvoiceName = createdInvoice.name ?: invoiceNameForLocal
            resolvedInvoiceCurrency = normalizeCurrency(createdInvoice.currency)
            resolvedReceivableCurrency = normalizeCurrency(createdInvoice.partyAccountCurrency)

            val rateInvToRcResolved =
                com.erpnext.pos.utils.CurrencyService.resolveInvoiceToReceivableRateUnified(
                    invoiceCurrency = resolvedInvoiceCurrency,
                    receivableCurrency = resolvedReceivableCurrency,
                    conversionRate = createdInvoice.conversionRate,
                    customExchangeRate = createdInvoice.customExchangeRate,
                    posCurrency = context.currency,
                    posExchangeRate = context.exchangeRate,
                    rateResolver = { from, to ->
                        resolveRateBetweenCurrencies(
                            fromCurrency = from,
                            toCurrency = to,
                            context = context
                        )
                    }
                )?.takeIf { it > 0.0 }

            val remoteGrandTotal = createdInvoice.grandTotal
            val remoteOutstanding = createdInvoice.outstandingAmount ?: remoteGrandTotal

            val totalRc = when {
                resolvedReceivableCurrency.equals(resolvedInvoiceCurrency, ignoreCase = true) ->
                    remoteGrandTotal

                resolvedReceivableCurrency.equals(companyCurrency, ignoreCase = true) &&
                    (createdInvoice.baseGrandTotal) != null &&
                        createdInvoice.baseGrandTotal > 0.0 ->
                    createdInvoice.baseGrandTotal

                rateInvToRcResolved != null -> createdInvoice.grandTotal * rateInvToRcResolved
                else -> remoteGrandTotal
            }

            val outstandingRcResolved = when {
                resolvedReceivableCurrency.equals(resolvedInvoiceCurrency, ignoreCase = true) ->
                    remoteOutstanding

                resolvedReceivableCurrency.equals(companyCurrency, ignoreCase = true) &&
                    (createdInvoice.baseGrandTotal) != null -> {
                    val baseTotal = createdInvoice.baseGrandTotal
                    val basePaid = createdInvoice.basePaidAmount ?: 0.0
                    (baseTotal - basePaid).coerceAtLeast(0.0)
                }

                rateInvToRcResolved != null -> {
                    val converted = (remoteOutstanding * rateInvToRcResolved).coerceAtLeast(0.0)
                    val rcTolerance = resolveMinorUnitTolerance(resolvedReceivableCurrency, currencySpecs)
                    val outstandingLooksAlreadyReceivable =
                        kotlin.math.abs(remoteOutstanding - totalRc) <= (rcTolerance * 2)
                    if (outstandingLooksAlreadyReceivable) remoteOutstanding.coerceAtLeast(0.0)
                    else converted
                }

                else -> remoteOutstanding
            }

            receivableAmounts = InvoiceReceivableAmounts(
                receivableCurrency = resolvedReceivableCurrency,
                totalRc = totalRc,
                outstandingRc = outstandingRcResolved
            )
            remainingOutstandingRc = receivableAmounts.outstandingRc
            if (paymentLines.isNotEmpty() && (remainingOutstandingRc <= 0.0)) {
                remainingOutstandingRc = receivableAmounts.totalRc.takeIf { it > 0.0 }
            }

            if (isOnline) {
                val paidFrom = resolvePaidFromAccount(
                    invoice = createdInvoice,
                    invoiceNameForLocal = invoiceNameForLocal,
                    context = context,
                    customer = customer,
                    receivableCurrency = resolvedReceivableCurrency
                )
                var remotePaymentFailed = false
                val cacheForReceivable = if (resolvedInvoiceCurrency.equals(
                        resolvedReceivableCurrency,
                        ignoreCase = true
                    )
                ) {
                    exchangeRateByCurrency.toMutableMap()
                } else {
                    mutableMapOf()
                }

                if (paidFrom.isNullOrBlank()) {
                    remotePaymentFailed = true
                } else {
                    paymentLines.forEach { line ->
                        val outstandingForEntry = (remainingOutstandingRc
                            ?: receivableAmounts.totalRc).coerceAtLeast(0.0)
                        if (outstandingForEntry <= 0.0) return@forEach
                        val paymentCurrency = normalizeCurrency(line.currency)
                        if (!paymentCurrency.equals(resolvedReceivableCurrency, ignoreCase = true)) {
                            if (cacheForReceivable[paymentCurrency] == null) {
                                resolveRateBetweenCurrencies(
                                    fromCurrency = paymentCurrency,
                                    toCurrency = resolvedReceivableCurrency,
                                    context = context
                                )?.takeIf { it > 0.0 }?.let { rate ->
                                    cacheForReceivable[paymentCurrency] = rate
                                }
                            }
                        }

                        val adjustedLine = capPaymentLineToOutstanding(
                            line = line,
                            remainingOutstandingRc = outstandingForEntry,
                            receivableCurrency = resolvedReceivableCurrency,
                            paidToCurrency = paymentCurrency,
                            invoiceCurrency = resolvedInvoiceCurrency,
                            rateInvToRc = rateInvToRcResolved,
                            cacheForReceivable = cacheForReceivable,
                            context = context,
                            currencySpecs = currencySpecs
                        )

                        val paymentEntry = buildPaymentEntryDto(
                            api = api,
                            line = adjustedLine,
                            context = context,
                            customer = customer,
                            postingDate = postingDate,
                            invoiceId = createdInvoice.name ?: resolvedInvoiceName,
                            invoiceTotalRc = receivableAmounts.totalRc,
                            outstandingRc = outstandingForEntry,
                            paidFromAccount = paidFrom,
                            partyAccountCurrency = resolvedReceivableCurrency,
                            invoiceCurrency = resolvedInvoiceCurrency,
                            invoiceToReceivableRate = rateInvToRcResolved,
                            currencySpecs = currencySpecs,
                            paymentModeDetails = paymentModeDetails,
                            referenceDoctype = "Sales Invoice"
                        )

                        val paymentResult = runCatching {
                            createPaymentEntryUseCase(CreatePaymentEntryInput(paymentEntry))
                        }
                        if (paymentResult.isFailure) {
                            remotePaymentFailed = true
                        } else {
                            val createdPaymentName = paymentResult.getOrNull()
                            adjustedLine.referenceNumber?.takeIf { it.isNotBlank() }?.let { ref ->
                                remoteEntryByReference[ref] = createdPaymentName
                            }
                        }

                        val allocated = paymentEntry.references.firstOrNull()?.allocatedAmount ?: 0.0
                        remainingOutstandingRc =
                            roundToCurrency((outstandingForEntry - allocated).coerceAtLeast(0.0))
                    }
                }

                if (!remotePaymentFailed) {
                    remotePaymentsSucceeded = true
                }
            }
        }

        val localOutstandingRc = remainingOutstandingRc
            ?: receivableAmounts?.outstandingRc
            ?: createdInvoice?.outstandingAmount?.takeIf { it > 0.0 }
            ?: createdInvoice?.grandTotal
        val localOutstandingInv = localOutstandingRc?.let {
            com.erpnext.pos.utils.CurrencyService.amountReceivableToInvoice(it, rateInvToRc)
        }
        var remainingLocalInv = localOutstandingInv
        val adjustedLines = paymentLines.map { line ->
            val localAmountInv = line.baseAmount
            if (remainingLocalInv == null) return@map line.copy(baseAmount = localAmountInv)
            val allocatedInv = minOf(localAmountInv, remainingLocalInv)
            remainingLocalInv = (remainingLocalInv - allocatedInv).coerceAtLeast(0.0)
            line.copy(baseAmount = allocatedInv)
        }

        val localPayments = buildLocalPayments(
            invoiceNameForLocal,
            postingDate,
            adjustedLines,
            posOpeningEntry,
            remotePaymentEntries = remoteEntryByReference
        )
        saveInvoicePaymentsUseCase(
            SaveInvoicePaymentsInput(
                invoiceName = invoiceNameForLocal,
                payments = localPayments
            )
        )

        return PaymentRegistrationResult(
            remotePaymentsSucceeded = remotePaymentsSucceeded,
            invoiceNameForLocal = invoiceNameForLocal,
            outstandingRemaining = remainingOutstandingRc
        )
    }

    private suspend fun resolvePaidFromAccount(
        invoice: SalesInvoiceDto,
        invoiceNameForLocal: String,
        context: POSContext,
        customer: CustomerBO,
        receivableCurrency: String?
    ): String? {
        invoice.debitTo?.takeIf { it.isNotBlank() }?.let { return it }

        invoice.name?.takeIf { it.isNotBlank() }?.let { remoteName ->
            invoiceLocalSource.getInvoiceByName(remoteName)
                ?.invoice
                ?.debitTo
                ?.takeIf { it.isNotBlank() }
                ?.let { return it }
        }

        invoiceLocalSource.getInvoiceByName(invoiceNameForLocal)
            ?.invoice
            ?.debitTo
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        return invoiceLocalSource.findRecentDebitTo(
            company = context.company,
            customer = customer.name,
            partyAccountCurrency = receivableCurrency
        )
    }

    private suspend fun capPaymentLineToOutstanding(
        line: PaymentLine,
        remainingOutstandingRc: Double?,
        receivableCurrency: String?,
        paidToCurrency: String?,
        invoiceCurrency: String?,
        rateInvToRc: Double?,
        cacheForReceivable: Map<String, Double>,
        context: POSContext,
        currencySpecs: Map<String, CurrencySpec>
    ): PaymentLine {
        val outstanding = remainingOutstandingRc?.takeIf { it > 0.0 } ?: return line
        val rc = normalizeCurrency(receivableCurrency)
        val pay = normalizeCurrency(paidToCurrency)
        if (rc.isBlank() || pay.isBlank()) return line

        val paySpec = currencySpecs[pay] ?: CurrencySpec(code = pay, minorUnits = 2, cashScale = 2)
        val maxEntered = if (pay.equals(rc, ignoreCase = true)) {
            outstanding
        } else {
            val rate = resolvePaymentToReceivableRate(
                paymentCurrency = pay,
                invoiceCurrency = normalizeCurrency(invoiceCurrency),
                receivableCurrency = rc,
                paymentToInvoiceRate = line.exchangeRate,
                invoiceToReceivableRate = rateInvToRc
            ) ?: cacheForReceivable[pay] ?: resolveRateBetweenCurrencies(
                fromCurrency = pay,
                toCurrency = rc,
                context = context
            )
            if (rate == null || rate <= 0.0) return line
            outstanding / rate
        }

        val scale = paySpec.cashScale.coerceAtMost(paySpec.minorUnits)
        val capped = roundToScale(maxEntered, scale)
        if (line.enteredAmount <= capped + 0.000001) return line

        return line.copy(
            enteredAmount = capped,
            baseAmount = roundToCurrency(capped * line.exchangeRate)
        )
    }

    private fun roundToScale(value: Double, scale: Int): Double {
        if (!value.isFinite()) return value
        if (scale <= 0) return round(value)
        val factor = 10.0.pow(scale.toDouble())
        return round(value * factor) / factor
    }

    private suspend fun resolveRateBetweenCurrencies(
        fromCurrency: String,
        toCurrency: String,
        context: POSContext
    ): Double? {
        val from = normalizeCurrency(fromCurrency)
        val to = normalizeCurrency(toCurrency)
        if (from.equals(to, ignoreCase = true)) return 1.0

        exchangeRateRepository.getLocalRate(from, to)?.takeIf { it > 0.0 }?.let { return it }
        exchangeRateRepository.getLocalRate(to, from)?.takeIf { it > 0.0 }?.let { return 1 / it }

        val ctxCurrency = normalizeCurrency(context.currency)
        val ctxRate = context.exchangeRate
        if (ctxRate > 0.0) {
            if (from.equals(ctxCurrency, true) && to.equals("USD", true)) {
                return 1 / ctxRate
            }
            if (from.equals("USD", true) && to.equals(ctxCurrency, true)) {
                return ctxRate
            }
        }
        return null
    }

}

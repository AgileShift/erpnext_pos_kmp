package com.erpnext.pos.views.payment

import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.usecases.CreatePaymentEntryInput
import com.erpnext.pos.domain.usecases.CreatePaymentEntryUseCase
import com.erpnext.pos.domain.usecases.SaveInvoicePaymentsInput
import com.erpnext.pos.domain.usecases.SaveInvoicePaymentsUseCase
import com.erpnext.pos.data.repositories.ExchangeRateRepository
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.views.POSContext
import com.erpnext.pos.views.billing.PaymentLine
import com.erpnext.pos.domain.utils.UUIDGenerator
import com.erpnext.pos.utils.InvoiceReceivableAmounts
import com.erpnext.pos.utils.buildCurrencySpecs
import com.erpnext.pos.utils.buildLocalPayments
import com.erpnext.pos.utils.buildPaymentEntryDto
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.resolvePaymentCurrencyForMode
import com.erpnext.pos.utils.resolveRateToInvoiceCurrency
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.utils.toBaseAmount
import kotlinx.coroutines.flow.first

class PaymentHandler(
    private val api: APIService,
    private val createPaymentEntryUseCase: CreatePaymentEntryUseCase,
    private val saveInvoicePaymentsUseCase: SaveInvoicePaymentsUseCase,
    private val exchangeRateRepository: ExchangeRateRepository,
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
        paymentModeCurrencyByMode: Map<String, String>,
        paymentModeDetails: Map<String, ModeOfPaymentEntity>,
        exchangeRateByCurrency: Map<String, Double>,
        round: (Double) -> Double
    ): PaymentLineResult {
        val invoiceCurrency = normalizeCurrency(invoiceCurrencyInput) ?: "USD"

        val paymentCurrency = resolvePaymentCurrencyForMode(
            modeOfPayment = line.modeOfPayment,
            invoiceCurrency = invoiceCurrency,
            paymentModeCurrencyByMode = paymentModeCurrencyByMode,
            paymentModeDetails = paymentModeDetails
        )

        val rate = resolveRateToInvoiceCurrency(
            paymentCurrency = paymentCurrency,
            invoiceCurrency = invoiceCurrency,
            cache = exchangeRateByCurrency,
            rateResolver = { from, to ->
                exchangeRateRepository.getLocalRate(from, to)
            }
        )

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
        baseAmountCurrency: String?,
        posOpeningEntry: String?
    ): PaymentRegistrationResult {
        if (paymentLines.isEmpty()) {
            return PaymentRegistrationResult(
                remotePaymentsSucceeded = false,
                invoiceNameForLocal = invoiceNameForLocal,
                outstandingRemaining = null
            )
        }

        val isPosInvoice =
            createdInvoice?.doctype?.equals("POS Invoice", ignoreCase = true) == true
        val currencySpecs = buildCurrencySpecs()
        var resolvedReceivableCurrency = normalizeCurrency(createdInvoice?.partyAccountCurrency)
            ?: normalizeCurrency(context.partyAccountCurrency)
            ?: normalizeCurrency(createdInvoice?.currency)
            ?: normalizeCurrency(context.currency)
            ?: "USD"
        var resolvedInvoiceCurrency = normalizeCurrency(createdInvoice?.currency)
            ?: normalizeCurrency(context.currency)
            ?: "USD"

        val rateInvToRc = when {
            resolvedInvoiceCurrency.equals(resolvedReceivableCurrency, ignoreCase = true) -> 1.0
            createdInvoice?.conversionRate != null && createdInvoice.conversionRate > 0.0 ->
                createdInvoice.conversionRate
            createdInvoice?.customExchangeRate != null && createdInvoice.customExchangeRate > 0.0 ->
                createdInvoice.customExchangeRate
            createdInvoice?.baseGrandTotal != null && createdInvoice.grandTotal > 0.0 ->
                createdInvoice.baseGrandTotal / createdInvoice.grandTotal
            else -> resolveRateBetweenCurrencies(
                fromCurrency = resolvedInvoiceCurrency,
                toCurrency = resolvedReceivableCurrency,
                context = context
            )
        }?.takeIf { it > 0.0 }

        var receivableAmounts: InvoiceReceivableAmounts? = createdInvoice?.let {
            val invoiceTotalInv = it.grandTotal
            val invoiceOutstandingInv = it.outstandingAmount
            val totalRc = when {
                resolvedInvoiceCurrency.equals(resolvedReceivableCurrency, ignoreCase = true) ->
                    invoiceTotalInv
                it.baseGrandTotal != null && it.baseGrandTotal > 0.0 -> it.baseGrandTotal
                rateInvToRc != null -> invoiceTotalInv * rateInvToRc
                else -> invoiceTotalInv
            }
            val outstandingRc = when {
                resolvedInvoiceCurrency.equals(resolvedReceivableCurrency, ignoreCase = true) ->
                    invoiceOutstandingInv ?: totalRc
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

        val isOnline = networkMonitor.isConnected.first()

        if (createdInvoice != null && isOnline && !isPosInvoice) {
            val resolvedInvoice = if (createdInvoice.debitTo.isNullOrBlank() &&
                !createdInvoice.name.isNullOrBlank()
            ) {
                runCatching { api.getSalesInvoiceByName(createdInvoice.name!!) }
                    .getOrElse { createdInvoice }
            } else {
                createdInvoice
            }
            resolvedInvoiceCurrency = normalizeCurrency(resolvedInvoice.currency)
                ?: resolvedInvoiceCurrency
            resolvedReceivableCurrency = normalizeCurrency(resolvedInvoice.partyAccountCurrency)
                ?: resolvedReceivableCurrency

            val rateInvToRcResolved = when {
                resolvedInvoiceCurrency.equals(resolvedReceivableCurrency, ignoreCase = true) -> 1.0
                resolvedInvoice.conversionRate != null && resolvedInvoice.conversionRate > 0.0 ->
                    resolvedInvoice.conversionRate
                resolvedInvoice.customExchangeRate != null && resolvedInvoice.customExchangeRate > 0.0 ->
                    resolvedInvoice.customExchangeRate
                resolvedInvoice.baseGrandTotal != null && resolvedInvoice.grandTotal > 0.0 ->
                    resolvedInvoice.baseGrandTotal / resolvedInvoice.grandTotal
                else -> resolveRateBetweenCurrencies(
                    fromCurrency = resolvedInvoiceCurrency,
                    toCurrency = resolvedReceivableCurrency,
                    context = context
                )
            }?.takeIf { it > 0.0 }

            val totalRcResolved = when {
                resolvedReceivableCurrency.equals(resolvedInvoiceCurrency, ignoreCase = true) ->
                    resolvedInvoice.grandTotal
                resolvedInvoice.baseGrandTotal != null && resolvedInvoice.baseGrandTotal > 0.0 ->
                    resolvedInvoice.baseGrandTotal
                rateInvToRcResolved != null -> resolvedInvoice.grandTotal * rateInvToRcResolved
                else -> resolvedInvoice.grandTotal
            }

            val outstandingRcResolved = when {
                resolvedReceivableCurrency.equals(resolvedInvoiceCurrency, ignoreCase = true) ->
                    resolvedInvoice.outstandingAmount ?: resolvedInvoice.grandTotal
                resolvedInvoice.baseGrandTotal != null -> {
                    val basePaid = resolvedInvoice.basePaidAmount ?: 0.0
                    (resolvedInvoice.baseGrandTotal - basePaid).coerceAtLeast(0.0)
                }
                rateInvToRcResolved != null -> {
                    val invOutstanding =
                        resolvedInvoice.outstandingAmount ?: resolvedInvoice.grandTotal
                    (invOutstanding * rateInvToRcResolved).coerceAtLeast(0.0)
                }
                else -> resolvedInvoice.outstandingAmount ?: resolvedInvoice.grandTotal
            }

            receivableAmounts = InvoiceReceivableAmounts(
                receivableCurrency = resolvedReceivableCurrency,
                totalRc = totalRcResolved,
                outstandingRc = outstandingRcResolved
            )
            remainingOutstandingRc = receivableAmounts?.outstandingRc

            val paidFrom = resolvedInvoice.debitTo
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

            paymentLines.forEach { line ->
                if (remainingOutstandingRc != null && remainingOutstandingRc <= 0.0) return@forEach
                val paidToCurrency = normalizeCurrency(
                    paymentModeDetails[line.modeOfPayment]?.currency
                ) ?: normalizeCurrency(line.currency) ?: resolvedInvoiceCurrency
                if (!paidToCurrency.equals(resolvedReceivableCurrency, ignoreCase = true)) {
                    if (cacheForReceivable[paidToCurrency] == null) {
                        resolveRateBetweenCurrencies(
                            fromCurrency = paidToCurrency,
                            toCurrency = resolvedReceivableCurrency,
                            context = context
                        )?.takeIf { it > 0.0 }?.let { rate ->
                            cacheForReceivable[paidToCurrency] = rate
                        }
                    }
                }

                val paymentEntry = buildPaymentEntryDto(
                    api = api,
                    line = line,
                    context = context,
                    customer = customer,
                    postingDate = postingDate,
                    invoiceId = resolvedInvoice.name ?: invoiceNameForLocal,
                    invoiceTotalRc = receivableAmounts?.totalRc ?: resolvedInvoice.grandTotal,
                    outstandingRc = remainingOutstandingRc ?: resolvedInvoice.grandTotal,
                    paidFromAccount = paidFrom,
                    partyAccountCurrency = resolvedReceivableCurrency,
                    invoiceCurrency = resolvedInvoiceCurrency,
                    invoiceToReceivableRate = rateInvToRcResolved,
                    exchangeRateByCurrency = cacheForReceivable,
                    currencySpecs = currencySpecs,
                    paymentModeDetails = paymentModeDetails
                )

                val paymentResult = runCatching {
                    createPaymentEntryUseCase(CreatePaymentEntryInput(paymentEntry))
                }
                if (paymentResult.isFailure) {
                    remotePaymentFailed = true
                }

                val allocated = paymentEntry.references.firstOrNull()?.allocatedAmount ?: 0.0
                remainingOutstandingRc = remainingOutstandingRc?.let { curr ->
                    roundToCurrency((curr - allocated).coerceAtLeast(0.0))
                }
            }

            if (!remotePaymentFailed) {
                remotePaymentsSucceeded = true
            }
        } else if (createdInvoice != null && isPosInvoice) {
            remotePaymentsSucceeded = true
        }

        val localOutstandingStart = receivableAmounts?.outstandingRc
            ?: createdInvoice?.outstandingAmount?.takeIf { it > 0.0 }
            ?: createdInvoice?.grandTotal
        var remainingLocal = localOutstandingStart
        val adjustedLines = paymentLines.map { line ->
            val localAmount = if (rateInvToRc != null) {
                roundToCurrency(line.baseAmount * rateInvToRc)
            } else {
                line.baseAmount
            }
            if (remainingLocal == null) return@map line.copy(baseAmount = localAmount)
            val allocated = minOf(localAmount, remainingLocal!!)
            remainingLocal = (remainingLocal!! - allocated).coerceAtLeast(0.0)
            line.copy(baseAmount = allocated)
        }

        val localPayments = buildLocalPayments(
            invoiceNameForLocal,
            postingDate,
            adjustedLines,
            posOpeningEntry
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

    private suspend fun resolveRateBetweenCurrencies(
        fromCurrency: String,
        toCurrency: String,
        context: POSContext
    ): Double? {
        val from = normalizeCurrency(fromCurrency) ?: return null
        val to = normalizeCurrency(toCurrency) ?: return null
        if (from.equals(to, ignoreCase = true)) return 1.0

        exchangeRateRepository.getLocalRate(from, to)?.takeIf { it > 0.0 }?.let { return it }
        exchangeRateRepository.getLocalRate(to, from)?.takeIf { it > 0.0 }?.let { return 1 / it }

        val ctxCurrency = normalizeCurrency(context.currency)
        val ctxRate = context.exchangeRate
        if (ctxRate > 0.0 && ctxCurrency != null) {
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

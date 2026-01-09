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
        invoiceCurrencyInput: String?,
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
                exchangeRateRepository.getRate(from, to)
            }
        )

        val fixed = line.copy(
            currency = paymentCurrency,
            exchangeRate = rate
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
        paymentModeDetails: Map<String, ModeOfPaymentEntity>
    ): PaymentRegistrationResult {
        if (paymentLines.isEmpty()) {
            return PaymentRegistrationResult(
                remotePaymentsSucceeded = false,
                invoiceNameForLocal = invoiceNameForLocal,
                outstandingRemaining = null
            )
        }

        val currencySpecs = buildCurrencySpecs()
        var remotePaymentsSucceeded = false
        var remainingOutstandingRc: Double? = createdInvoice?.outstandingAmount?.takeIf { it > 0.0 }
            ?: createdInvoice?.grandTotal

        val isOnline = networkMonitor.isConnected.first()

        if (createdInvoice != null && isOnline) {
            val paidFrom = createdInvoice.debitTo
            var remotePaymentFailed = false

            paymentLines.forEach { line ->
                if (remainingOutstandingRc != null && remainingOutstandingRc <= 0.0) return@forEach

                val paymentEntry = buildPaymentEntryDto(
                    api = api,
                    line = line,
                    context = context,
                    customer = customer,
                    postingDate = postingDate,
                    invoiceId = createdInvoice.name ?: invoiceNameForLocal,
                    invoiceTotalRc = createdInvoice.grandTotal,
                    outstandingRc = remainingOutstandingRc ?: createdInvoice.grandTotal,
                    paidFromAccount = paidFrom,
                    partyAccountCurrency = createdInvoice.partyAccountCurrency,
                    exchangeRateByCurrency = exchangeRateByCurrency,
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
        }

        val localPayments = buildLocalPayments(invoiceNameForLocal, postingDate, paymentLines)
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
}

package com.erpnext.pos.sync

import com.erpnext.pos.data.repositories.ExchangeRateRepository
import com.erpnext.pos.data.repositories.CustomerSyncRepository
import com.erpnext.pos.data.repositories.ClosingEntrySyncRepository
import com.erpnext.pos.data.repositories.OpeningEntrySyncRepository
import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.domain.usecases.CreatePaymentEntryInput
import com.erpnext.pos.domain.usecases.CreatePaymentEntryUseCase
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.datasources.InvoiceLocalSource
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.utils.buildCurrencySpecs
import com.erpnext.pos.utils.buildPaymentModeDetailMap
import com.erpnext.pos.utils.buildPaymentEntryDtoWithRateResolver
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.billing.PaymentLine
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class LegacyPushSyncManager(
    private val invoiceRepository: SalesInvoiceRepository,
    private val invoiceLocalSource: InvoiceLocalSource,
    private val modeOfPaymentDao: ModeOfPaymentDao,
    private val paymentEntryUseCase: CreatePaymentEntryUseCase,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val openingEntrySyncRepository: OpeningEntrySyncRepository,
    private val closingEntrySyncRepository: ClosingEntrySyncRepository,
    private val customerSyncRepository: CustomerSyncRepository,
    private val cashBoxManager: CashBoxManager
) : PushSyncRunner {

    override suspend fun runPushQueue(ctx: SyncContext, onDocType: (String) -> Unit): Boolean {
        return try {
            var hasChanges = false
            onDocType("Aperturas pendientes")
            val openingsSynced = openingEntrySyncRepository.pushPending()
            hasChanges = hasChanges || openingsSynced
            onDocType("Clientes pendientes")
            val customersSynced = customerSyncRepository.pushPending()
            hasChanges = hasChanges || customersSynced
            onDocType("Facturas locales")
            val pending = invoiceRepository.getPendingSyncInvoices()
            if (pending.isNotEmpty()) {
                invoiceRepository.syncPendingInvoices()
                hasChanges = true
            }
            onDocType("Pagos pendientes")
            val paymentsSynced = pushPendingPayments()
            hasChanges = hasChanges || paymentsSynced
            onDocType("Cierres pendientes")
            val closingsSynced = closingEntrySyncRepository.pushPending()
            hasChanges || closingsSynced
        } catch (e: Throwable) {
            AppSentry.capture(e, "LegacyPushSyncManager: invoices push failed")
            AppLogger.warn("LegacyPushSyncManager: invoices push failed", e)
            throw e
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun pushPendingPayments(): Boolean {
        val pendingPayments = invoiceLocalSource.getPendingPayments()
        if (pendingPayments.isEmpty()) return false
        val context = cashBoxManager.getContext() ?: return false
        val modeDefinitions = runCatching { modeOfPaymentDao.getAllModes(context.company) }
            .getOrElse { emptyList() }
        val paymentModeDetails = buildPaymentModeDetailMap(modeDefinitions)
        val currencySpecs = buildCurrencySpecs()
        val exchangeRateCache = mutableMapOf<String, Double>()
        val now = Clock.System.now().toEpochMilliseconds()

        var hasChanges = false
        val failedIds = mutableListOf<Int>()

        pendingPayments.forEach { payment ->
            val invoiceWrapper = invoiceLocalSource.getInvoiceByName(payment.parentInvoice)
                ?: return@forEach
            val invoice = invoiceWrapper.invoice
            val invoiceName = invoice.invoiceName ?: return@forEach
            if (invoiceName.startsWith("LOCAL-", ignoreCase = true)) return@forEach
            if (!invoice.syncStatus.equals("Synced", ignoreCase = true)) return@forEach
            if (invoice.isPos) {
                invoiceLocalSource.updatePaymentSyncStatus(payment.id, "Synced", now)
                hasChanges = true
                return@forEach
            }

            val paidFrom = invoice.debitTo?.takeIf { it.isNotBlank() } ?: return@forEach
            val receivableCurrency = normalizeCurrency(invoice.partyAccountCurrency)
                ?: normalizeCurrency(invoice.currency)
                ?: "USD"
            val invoiceCurrency = normalizeCurrency(invoice.currency) ?: receivableCurrency
            val invoiceToReceivableRate = com.erpnext.pos.utils.CurrencyService.resolveInvoiceToReceivableRate(
                invoiceCurrency = invoiceCurrency,
                receivableCurrency = receivableCurrency,
                conversionRate = invoice.conversionRate,
                customExchangeRate = null
            )

            val enteredAmount = payment.enteredAmount.takeIf { it > 0.0 } ?: payment.amount
            val paymentCurrency = normalizeCurrency(payment.paymentCurrency)
                ?: normalizeCurrency(invoice.currency)
                ?: receivableCurrency
            val resolvedReference = payment.paymentReference?.takeIf { it.isNotBlank() }
                ?: "POSPAY-LCL-${invoiceName}-${payment.id}"

            val line = PaymentLine(
                modeOfPayment = payment.modeOfPayment,
                enteredAmount = enteredAmount,
                currency = paymentCurrency,
                exchangeRate = payment.exchangeRate.takeIf { it > 0.0 } ?: 1.0,
                baseAmount = 0.0,
                referenceNumber = resolvedReference
            )

            val customer = CustomerBO(
                name = invoice.customer,
                customerName = invoice.customerName ?: invoice.customer,
                customerType = "",
                currency = receivableCurrency
            )

            val paymentEntry = runCatching {
                buildPaymentEntryDtoWithRateResolver(
                    rateResolver = { from, to ->
                        exchangeRateRepository.getRate(from ?: "", to ?: "")
                    },
                    line = line,
                    context = context,
                    customer = customer,
                    postingDate = payment.paymentDate ?: invoice.postingDate,
                    invoiceId = invoiceName,
                    invoiceTotalRc = invoice.paidAmount + invoice.outstandingAmount,
                    outstandingRc = invoice.outstandingAmount,
                    paidFromAccount = paidFrom,
                    partyAccountCurrency = receivableCurrency,
                    invoiceCurrency = invoiceCurrency,
                    invoiceToReceivableRate = invoiceToReceivableRate,
                    exchangeRateByCurrency = exchangeRateCache,
                    currencySpecs = currencySpecs,
                    paymentModeDetails = paymentModeDetails
                )
            }.getOrNull()

            if (paymentEntry == null) {
                failedIds += payment.id
                return@forEach
            }

            runCatching {
                paymentEntryUseCase(CreatePaymentEntryInput(paymentEntry))
            }.onFailure { err ->
                AppSentry.capture(err, "LegacyPushSyncManager: payment ${payment.id} failed")
                AppLogger.warn("LegacyPushSyncManager: payment ${payment.id} failed", err)
                failedIds += payment.id
            }.onSuccess {
                invoiceLocalSource.updatePaymentSyncStatus(payment.id, "Synced", now)
                hasChanges = true
            }
        }

        if (failedIds.isNotEmpty()) {
            failedIds.forEach { paymentId ->
                invoiceLocalSource.updatePaymentSyncStatus(paymentId, "Failed", now)
            }
        }

        return hasChanges
    }
}

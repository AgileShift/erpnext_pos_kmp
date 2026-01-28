package com.erpnext.pos.views.reconciliation

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.data.repositories.PosProfilePaymentMethodLocalRepository
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.datasources.InvoiceLocalSource
import com.erpnext.pos.sync.LegacyPushSyncManager
import com.erpnext.pos.sync.SyncContextProvider
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.localSource.dao.ShiftPaymentRow
import com.erpnext.pos.sync.PushSyncRunner
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.utils.parseErpDateTimeToEpochMillis
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.views.POSContext
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalTime::class)
class ReconciliationViewModel(
    private val cashBoxManager: CashBoxManager,
    private val salesInvoiceDao: SalesInvoiceDao,
    private val invoiceLocalSource: InvoiceLocalSource,
    private val paymentMethodLocalRepository: PosProfilePaymentMethodLocalRepository,
    private val legacyPushSyncManager: PushSyncRunner,
    private val syncContextProvider: SyncContextProvider,
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor
) : BaseViewModel() {
    private val _stateFlow = MutableStateFlow<ReconciliationState>(ReconciliationState.Loading)
    val stateFlow: StateFlow<ReconciliationState> = _stateFlow.asStateFlow()
    private val _closeState = MutableStateFlow(CloseCashboxState())
    val closeState: StateFlow<CloseCashboxState> = _closeState.asStateFlow()

    init {
        loadShiftSummary()
    }

    private fun loadShiftSummary() {
        viewModelScope.launch {
            _stateFlow.update { ReconciliationState.Loading }
            val summary = runCatching { buildShiftSummary() }
                .onFailure { error ->
                    _stateFlow.update {
                        ReconciliationState.Error(
                            error.message ?: "Unable to load reconciliation data."
                        )
                    }
                }
                .getOrNull()
            if (summary == null) {
                _stateFlow.update { ReconciliationState.Empty }
            } else {
                _stateFlow.update { ReconciliationState.Success(summary) }
            }
        }
    }

    fun reload() {
        loadShiftSummary()
    }

    fun closeCashbox(countedByMode: Map<String, Double>) {
        if (_closeState.value.isClosing) return
        _closeState.update { it.copy(isClosing = true, errorMessage = null) }
        executeUseCase(
            action = {
                val syncPrepared = prepareForClose()
                if (!syncPrepared) return@executeUseCase
                val summary = runCatching { buildShiftSummary() }.getOrNull()
                if (summary != null) {
                    updateClosingAmounts(summary, countedByMode)
                }
                cashBoxManager.closeCashBox()
                _closeState.update { it.copy(isClosing = false, isClosed = true) }
                loadShiftSummary()
            },
            exceptionHandler = { error ->
                _closeState.update {
                    it.copy(
                        isClosing = false,
                        errorMessage = error.message ?: "Failed to close the cashbox."
                    )
                }
            }
        )
    }

    private suspend fun prepareForClose(): Boolean {
        val isOnline = networkMonitor.isConnected.first()
        if (!isOnline) {
            _closeState.update {
                it.copy(
                    isSyncing = false,
                    syncMessage = "No hay conexión para sincronizar antes del cierre.",
                    isClosing = false,
                    errorMessage = "Necesitas conexión para cerrar la caja."
                )
            }
            return false
        }
        _closeState.update {
            it.copy(
                isSyncing = true,
                syncMessage = "Sincronizando facturas y pagos...",
                errorMessage = null
            )
        }
        val syncSuccess = waitForSyncCompletion()
        _closeState.update {
            it.copy(isSyncing = false, syncMessage = null)
        }
        if (!syncSuccess) {
            _closeState.update {
                it.copy(
                    isClosing = false,
                    errorMessage = "No se pudo sincronizar completamente con el servidor."
                )
            }
            return false
        }
        val pending = hasPendingLocalDocs()
        if (pending) {
            _closeState.update {
                it.copy(
                    isClosing = false,
                    errorMessage = "Aún hay facturas o pagos locales pendientes de sincronizar."
                )
            }
            return false
        }
        return true
    }

    private suspend fun waitForSyncCompletion(): Boolean {
        val currentState = syncManager.state.value
        if (currentState !is SyncState.SYNCING) {
            syncManager.fullSync()
        }
        val finalState = syncManager.state
            .filter { it !is SyncState.SYNCING }
            .first()
        return finalState is SyncState.SUCCESS
    }

    private suspend fun hasPendingLocalDocs(): Boolean {
        val pendingInvoices = salesInvoiceDao.getPendingSyncInvoices()
        if (pendingInvoices.isNotEmpty()) return true
        val pendingPayments = invoiceLocalSource.getPendingPayments()
        return pendingPayments.isNotEmpty()
    }

    private suspend fun buildShiftSummary(): ReconciliationSummaryUi? {
        val context = cashBoxManager.getContext() ?: cashBoxManager.initializeContext()
        ?: return null
        val activeCashbox = cashBoxManager.getActiveCashboxWithDetails() ?: return null
        val openingByMode = activeCashbox.details.associate { it.modeOfPayment to it.openingAmount }
        val startMillis = parseErpDateTimeToEpochMillis(activeCashbox.cashbox.periodStartDate)
            ?: return null
        val endMillis = activeCashbox.cashbox.periodEndDate?.let {
            parseErpDateTimeToEpochMillis(it)
        } ?: Clock.System.now().toEpochMilliseconds()
        val openingEntryId = activeCashbox.cashbox.openingEntryId
        val invoices = if (!openingEntryId.isNullOrBlank()) {
            salesInvoiceDao.getInvoicesForOpeningEntry(openingEntryId)
        } else {
            salesInvoiceDao.getInvoicesForShift(
                profileId = context.profileName,
                startMillis = startMillis,
                endMillis = endMillis
            )
        }
        val pendingSubmitCount = invoices.count { it.docstatus == 0 }
        val posCurrency = normalizeCurrency(context.currency)
        val rateCache = mutableMapOf<String, Double>()
        val paymentRows = if (!openingEntryId.isNullOrBlank()) {
            salesInvoiceDao.getPaymentsForOpeningEntry(openingEntryId)
        } else {
            salesInvoiceDao.getShiftPayments(
                profileId = context.profileName,
                startMillis = startMillis,
                endMillis = endMillis
            )
        }
        val resolvedMethods =
            paymentMethodLocalRepository.getMethodsForProfile(context.profileName)
        val modeCurrency = resolvedMethods
            .mapNotNull { method ->
                val currency = method.currency?.takeIf { it.isNotBlank() }
                    ?.let { normalizeCurrency(it) }
                currency?.let { method.mopName to it }
            }
            .toMap()
        val cashMethodsByCurrency =
            paymentMethodLocalRepository.getCashMethodsGroupedByCurrency(
                context.profileName,
                context.currency
            )
        val cashModeCurrency = cashMethodsByCurrency.flatMap { (currency, methods) ->
            val normalized = normalizeCurrency(currency) ?: currency
            methods.map { it.mopName to normalized }
        }.toMap()
        val cashCurrencies = cashMethodsByCurrency.keys.map {
            normalizeCurrency(it) ?: it.uppercase()
        }.distinct().sorted()
        val paymentsByMode =
            aggregatePaymentsByMode(invoices, paymentRows, posCurrency, modeCurrency, rateCache)
        val cashModes = resolveCashModes(context, openingByMode)
        val paymentsCashByCurrency =
            aggregateCashByCurrency(
                paymentRows,
                invoices,
                posCurrency,
                cashModes,
                modeCurrency,
                rateCache
            )
        val paymentsByCurrency =
            aggregatePaymentsByCurrency(invoices, paymentRows, posCurrency, modeCurrency)
        val availableModes = context.paymentModes.mapNotNull { mode ->
            mode.modeOfPayment.takeIf { it.isNotBlank() }
        }
        // Construye el esperado por modo (apertura + pagos del turno).
        val expectedByMode = buildExpectedByMode(openingByMode, paymentsByMode, availableModes)
        val openingByCurrency =
            mapOpeningByCurrency(openingByMode, posCurrency, modeCurrency)
        // El total esperado en caja solo debe contemplar modos de efectivo y en moneda POS.
        val expectedTotal = roundToCurrency(
            (openingByCurrency[posCurrency.uppercase()] ?: 0.0) +
                (paymentsCashByCurrency[posCurrency.uppercase()] ?: 0.0)
        )
        val cashPaymentsTotal = roundToCurrency(
            paymentsCashByCurrency[posCurrency.uppercase()] ?: 0.0
        )
        val paymentsTotal = roundToCurrency(
            ((paymentsByCurrency[posCurrency.uppercase()] ?: 0.0) - cashPaymentsTotal)
                .coerceAtLeast(0.0)
        )
        val symbol = context.allowedCurrencies
            .firstOrNull { it.code.equals(context.currency, ignoreCase = true) }
            ?.symbol
        val nonCashByCurrency = subtractCurrencyMaps(paymentsByCurrency, paymentsCashByCurrency)
        val currencySet =
            (openingByCurrency.keys + paymentsByCurrency.keys).map { it.uppercase() }.toSet()
        // Totales de crédito basados en facturas del turno (pagos parciales y pendientes).
        val creditTotals = aggregateCreditTotals(invoices, posCurrency, rateCache)
        val expensesByCurrency =
            convertAmountForCurrencies(0.0, posCurrency, currencySet, rateCache)
        return ReconciliationSummaryUi(
            posProfile = context.profileName,
            openingEntryId = activeCashbox.cashbox.openingEntryId.orEmpty(),
            cashierName = context.cashier.firstName,
            periodStart = activeCashbox.cashbox.periodStartDate,
            periodEnd = activeCashbox.cashbox.periodEndDate,
            openingAmount = roundToCurrency(openingByMode.values.sum()),
            openingDetails = activeCashbox.details.map {
                OpeningBalanceDetailUi(it.modeOfPayment, it.openingAmount)
            },
            openingByMode = openingByMode,
            paymentsByMode = paymentsByMode,
            expectedByMode = expectedByMode,
            cashModes = cashModes,
            salesTotal = cashPaymentsTotal,
            paymentsTotal = paymentsTotal,
            expensesTotal = 0.0,
            expectedTotal = expectedTotal,
            pendingSubmitCount = pendingSubmitCount,
            currency = context.currency,
            currencySymbol = symbol,
            invoiceCount = invoices.size,
            cashByCurrency = paymentsCashByCurrency,
            openingCashByCurrency = openingByCurrency,
            paymentsByCurrency = paymentsByCurrency,
            salesByCurrency = paymentsCashByCurrency,
            nonCashPaymentsByCurrency = nonCashByCurrency,
            creditPartialTotal = creditTotals.partialTotal,
            creditPendingTotal = creditTotals.pendingTotal,
            creditPartialByCurrency = creditTotals.partialByCurrency,
            creditPendingByCurrency = creditTotals.pendingByCurrency,
            expensesByCurrency = expensesByCurrency,
            cashCurrencies = cashCurrencies,
            cashModeCurrency = cashModeCurrency
        )
    }

    private suspend fun aggregateCashByCurrency(
        rows: List<ShiftPaymentRow>,
        invoices: List<com.erpnext.pos.localSource.entities.SalesInvoiceEntity>,
        posCurrency: String,
        cashModes: Set<String>,
        modeCurrency: Map<String, String>,
        rateCache: MutableMap<String, Double>
    ): Map<String, Double> {
        // Mapa de facturas para resolver totales y calcular vuelto real por invoice.
        val invoicesByName = invoices.associateBy { it.invoiceName }
        val totals = mutableMapOf<String, Double>()
        val cashRowsByInvoice = mutableMapOf<String, MutableList<ShiftPaymentRow>>()
        val nonCashBaseByInvoice = mutableMapOf<String, Double>()
        rows.filter { cashModes.contains(it.modeOfPayment) }.forEach { row ->
            val payCurrency = resolvePaymentCurrency(row, posCurrency, modeCurrency)
            val normalizedCurrency = payCurrency.uppercase()
            // Para efectivo sumamos lo recibido (entered_amount) si existe.
            val amountInPayCurrency = resolvePaymentAmount(row, payCurrency)
            totals[normalizedCurrency] = (totals[normalizedCurrency] ?: 0.0) + amountInPayCurrency
            cashRowsByInvoice.getOrPut(row.invoiceName) { mutableListOf() }.add(row)
        }
        rows.filterNot { cashModes.contains(it.modeOfPayment) }.forEach { row ->
            // Pagos no efectivo en base (POS) para descontar del total de la factura.
            nonCashBaseByInvoice[row.invoiceName] =
                (nonCashBaseByInvoice[row.invoiceName] ?: 0.0) + row.amount
        }
        // Calculamos vuelto por factura para restarlo en la moneda donde realmente se entregó.
        cashRowsByInvoice.forEach { (invoiceName, cashRows) ->
            val invoice = invoicesByName[invoiceName] ?: return@forEach
            val invoiceCurrency = normalizeCurrency(invoice.currency)
                ?: normalizeCurrency(invoice.partyAccountCurrency)
                ?: posCurrency
            val invoiceTotal = invoice.grandTotal
            val nonCashPaid = nonCashBaseByInvoice[invoiceName] ?: 0.0
            val cashDue = (invoiceTotal - nonCashPaid).coerceAtLeast(0.0)
            val cashPaidBase = cashRows.sumOf { it.amount }
            val changeBase = (cashPaidBase - cashDue).takeIf { it > 0.0 } ?: 0.0
            if (changeBase <= 0.0) return@forEach
            val changeCurrency = resolveChangeCurrency(cashRows, posCurrency, modeCurrency)
            val changeInCurrency = if (changeCurrency.equals(invoiceCurrency, ignoreCase = true)) {
                changeBase
            } else {
                val key = "${invoiceCurrency.uppercase()}->${changeCurrency.uppercase()}"
                val rate = rateCache.getOrPut(key) {
                    cashBoxManager.resolveExchangeRateBetween(
                        invoiceCurrency,
                        changeCurrency,
                        allowNetwork = false
                    ) ?: 1.0
                }
                changeBase * rate
            }
            val changeKey = changeCurrency.uppercase()
            totals[changeKey] = (totals[changeKey] ?: 0.0) - changeInCurrency
        }
        return totals.mapValues { roundToCurrency(it.value) }
    }

    private fun resolveChangeCurrency(
        cashRows: List<ShiftPaymentRow>,
        posCurrency: String,
        modeCurrency: Map<String, String>
    ): String {
        val totalsByCurrency = mutableMapOf<String, Double>()
        cashRows.forEach { row ->
            val currency = resolvePaymentCurrency(row, posCurrency, modeCurrency)
            val amount = resolvePaymentAmount(row, currency)
            totalsByCurrency[currency] = (totalsByCurrency[currency] ?: 0.0) + amount
        }
        return totalsByCurrency.maxByOrNull { it.value }?.key ?: posCurrency
    }

    private fun aggregatePaymentsByCurrency(
        invoices: List<com.erpnext.pos.localSource.entities.SalesInvoiceEntity>,
        rows: List<ShiftPaymentRow>,
        posCurrency: String,
        modeCurrency: Map<String, String>
    ): Map<String, Double> {
        val totals = mutableMapOf<String, Double>()
        val paymentsByInvoice = mutableMapOf<String, Double>()
        val invoiceByName = invoices.associateBy { it.invoiceName }
        rows.forEach { row ->
            val payCurrency = resolvePaymentCurrency(row, posCurrency, modeCurrency)
            val normalizedCurrency = payCurrency.uppercase()
            val amount = resolvePaymentAmount(row, payCurrency)
            totals[normalizedCurrency] = (totals[normalizedCurrency] ?: 0.0) + amount

            val invoice = invoiceByName[row.invoiceName]
            val receivableCurrency = normalizeCurrency(invoice?.partyAccountCurrency)
                ?: normalizeCurrency(row.partyAccountCurrency)
                ?: normalizeCurrency(invoice?.currency)
                ?: normalizeCurrency(row.invoiceCurrency)
                ?: posCurrency
            val invoiceCurrency = normalizeCurrency(invoice?.currency)
                ?: normalizeCurrency(row.invoiceCurrency)
                ?: receivableCurrency
            val rateInvToRc = com.erpnext.pos.utils.CurrencyService.resolveInvoiceToReceivableRate(
                invoiceCurrency = invoiceCurrency,
                receivableCurrency = receivableCurrency,
                conversionRate = invoice?.conversionRate,
                customExchangeRate = invoice?.customExchangeRate
            )
            val rowReceivable = com.erpnext.pos.utils.CurrencyService.amountInvoiceToReceivable(
                row.amount,
                rateInvToRc
            )
            paymentsByInvoice[row.invoiceName] =
                (paymentsByInvoice[row.invoiceName] ?: 0.0) + rowReceivable
        }

        invoices.forEach { invoice ->
            val invoiceName = invoice.invoiceName ?: return@forEach
            val paidAmount = invoice.paidAmount
            if (paidAmount <= 0.0) return@forEach
            val captured = paymentsByInvoice[invoiceName] ?: 0.0
            val delta = paidAmount - captured
            if (delta > 0.005) {
                val currency = normalizeCurrency(invoice.partyAccountCurrency)
                    ?: normalizeCurrency(invoice.currency)
                    ?: posCurrency
                val code = currency.uppercase()
                totals[code] = (totals[code] ?: 0.0) + delta
            }
        }

        return totals.mapValues { roundToCurrency(it.value) }
    }

    private fun resolvePaymentCurrency(
        row: ShiftPaymentRow,
        fallback: String,
        modeCurrency: Map<String, String>
    ): String {
        return normalizeCurrency(row.paymentCurrency)
            ?: resolveModeCurrency(row.modeOfPayment, modeCurrency, fallback)
    }

    private fun resolvePaymentAmount(row: ShiftPaymentRow, paymentCurrency: String): Double {
        val hasEntered = row.enteredAmount > 0.0
        if (row.paymentCurrency != null && hasEntered) return row.enteredAmount
        if (row.exchangeRate > 0.0 && !paymentCurrency.isBlank()) {
            // amount está en moneda de factura, exchangeRate es pago -> factura.
            return row.amount / row.exchangeRate
        }
        return row.amount
    }

    private fun resolveModeCurrency(
        mode: String,
        modeCurrency: Map<String, String>,
        fallback: String
    ): String {
        return normalizeCurrency(modeCurrency[mode]) ?: fallback.uppercase()
    }

    private fun mapOpeningByCurrency(
        openingByMode: Map<String, Double>,
        posCurrency: String,
        modeCurrency: Map<String, String>
    ): Map<String, Double> {
        val acc = mutableMapOf<String, Double>()
        openingByMode.forEach { (mode, amount) ->
            val currency = resolveModeCurrency(mode, modeCurrency, posCurrency)
            acc[currency] = (acc[currency] ?: 0.0) + amount
        }
        return acc.mapValues { roundToCurrency(it.value) }
    }

    private fun subtractCurrencyMaps(
        totalByCurrency: Map<String, Double>,
        subtractByCurrency: Map<String, Double>
    ): Map<String, Double> {
        val result = totalByCurrency.toMutableMap()
        subtractByCurrency.forEach { (code, amount) ->
            result[code] = roundToCurrency((result[code] ?: 0.0) - amount)
        }
        return result.mapValues { roundToCurrency(it.value) }
    }

    private suspend fun convertAmountForCurrencies(
        amount: Double,
        sourceCurrency: String,
        currencies: Set<String>,
        rateCache: MutableMap<String, Double>
    ): Map<String, Double> {
        if (currencies.isEmpty()) return emptyMap()
        return currencies.associateWith { target ->
            if (target.equals(sourceCurrency, ignoreCase = true)) {
                roundToCurrency(amount)
            } else {
                val key = "${sourceCurrency.uppercase()}->${target.uppercase()}"
                val rate = rateCache.getOrPut(key) {
                    cashBoxManager.resolveExchangeRateBetween(
                        sourceCurrency,
                        target,
                        allowNetwork = false
                    ) ?: 1.0
                }
                roundToCurrency(amount * rate)
            }
        }
    }

    private suspend fun aggregatePaymentsByMode(
        invoices: List<com.erpnext.pos.localSource.entities.SalesInvoiceEntity>,
        rows: List<ShiftPaymentRow>,
        posCurrency: String,
        modeCurrency: Map<String, String>,
        rateCache: MutableMap<String, Double>
    ): Map<String, Double> {
        val totals = mutableMapOf<String, Double>()
        val paymentsByInvoice = mutableMapOf<String, Double>()
        val invoiceByName = invoices.associateBy { it.invoiceName }
        rows.forEach { row ->
            val payCurrency = resolvePaymentCurrency(row, posCurrency, modeCurrency)
            val amount = resolvePaymentAmount(row, payCurrency)
            totals[row.modeOfPayment] = (totals[row.modeOfPayment] ?: 0.0) + amount
            val invoice = invoiceByName[row.invoiceName]
            val receivableCurrency = normalizeCurrency(invoice?.partyAccountCurrency)
                ?: normalizeCurrency(row.partyAccountCurrency)
                ?: normalizeCurrency(invoice?.currency)
                ?: normalizeCurrency(row.invoiceCurrency)
                ?: posCurrency
            val invoiceCurrency = normalizeCurrency(invoice?.currency)
                ?: normalizeCurrency(row.invoiceCurrency)
                ?: receivableCurrency
            val rateInvToRc = com.erpnext.pos.utils.CurrencyService.resolveInvoiceToReceivableRate(
                invoiceCurrency = invoiceCurrency,
                receivableCurrency = receivableCurrency,
                conversionRate = invoice?.conversionRate,
                customExchangeRate = invoice?.customExchangeRate
            )
            val rowReceivable = com.erpnext.pos.utils.CurrencyService.amountInvoiceToReceivable(
                row.amount,
                rateInvToRc
            )
            paymentsByInvoice[row.invoiceName] =
                (paymentsByInvoice[row.invoiceName] ?: 0.0) + rowReceivable
        }
        invoices.forEach { invoice ->
            val invoiceName = invoice.invoiceName ?: return@forEach
            val paidAmount = invoice.paidAmount
            if (paidAmount <= 0.0) return@forEach
            val invoiceCurrency = normalizeCurrency(invoice.partyAccountCurrency)
                ?: normalizeCurrency(invoice.currency)
                ?: posCurrency
            val captured = paymentsByInvoice[invoiceName] ?: 0.0
            val delta = paidAmount - captured
            if (delta > 0.005) {
                val mode =
                    invoice.modeOfPayment?.takeIf { it.isNotBlank() } ?: UNASSIGNED_PAYMENT_MODE
                val targetCurrency = resolveModeCurrency(mode, modeCurrency, invoiceCurrency)
                val adjusted = if (targetCurrency.equals(invoiceCurrency, ignoreCase = true)) {
                    delta
                } else {
                    val key = "${invoiceCurrency.uppercase()}->${targetCurrency.uppercase()}"
                    val rate = rateCache.getOrPut(key) {
                        cashBoxManager.resolveExchangeRateBetween(
                            invoiceCurrency,
                            targetCurrency,
                            allowNetwork = false
                        )
                            ?: 1.0
                    }
                    delta * rate
                }
                totals[mode] = (totals[mode] ?: 0.0) + adjusted
            }
        }
        return totals.mapValues { roundToCurrency(it.value) }
    }

    private fun resolveCashModes(
        context: POSContext,
        openingByMode: Map<String, Double>
    ): Set<String> {
        val direct = context.paymentModes.filter { mode ->
            mode.type?.equals("Cash", ignoreCase = true) == true ||
                    mode.modeOfPayment.contains("cash", ignoreCase = true) ||
                    mode.modeOfPayment.contains("efectivo", ignoreCase = true)
        }.map { it.modeOfPayment }
        if (direct.isNotEmpty()) return direct.toSet()
        val fallback = openingByMode.keys.filter { mode ->
            mode.contains("cash", ignoreCase = true) || mode.contains("efectivo", ignoreCase = true)
        }
        return if (fallback.isNotEmpty()) fallback.toSet() else openingByMode.keys
    }

    private suspend fun updateClosingAmounts(
        summary: ReconciliationSummaryUi,
        countedByMode: Map<String, Double>
    ) {
        val activeCashbox = cashBoxManager.getActiveCashboxWithDetails() ?: return
        val closingByMode = summary.expectedByMode.toMutableMap()
        countedByMode.forEach { (mode, counted) ->
            closingByMode[mode] = roundToCurrency(counted)
        }
        cashBoxManager.updateClosingAmounts(activeCashbox.cashbox.localId, closingByMode)
    }

    private fun buildExpectedByMode(
        openingByMode: Map<String, Double>,
        paymentsByMode: Map<String, Double>,
        availableModes: List<String>
    ): Map<String, Double> {
        val expected = openingByMode.toMutableMap()
        availableModes.forEach { mode ->
            if (!expected.containsKey(mode)) {
                expected[mode] = 0.0
            }
        }
        paymentsByMode.forEach { (mode, amount) ->
            expected[mode] = roundToCurrency((expected[mode] ?: 0.0) + amount)
        }
        return expected.mapValues { roundToCurrency(it.value) }
    }

    // Contenedor de totales de crédito calculados desde facturas del turno.
    private data class CreditTotals(
        val partialTotal: Double,
        val pendingTotal: Double,
        val partialByCurrency: Map<String, Double>,
        val pendingByCurrency: Map<String, Double>
    )

    private suspend fun aggregateCreditTotals(
        invoices: List<com.erpnext.pos.localSource.entities.SalesInvoiceEntity>,
        posCurrency: String,
        rateCache: MutableMap<String, Double>
    ): CreditTotals {
        // Separamos pagos parciales (paid) y pendientes (outstanding) por moneda.
        val partialByCurrency = mutableMapOf<String, Double>()
        val pendingByCurrency = mutableMapOf<String, Double>()
        var partialTotal = 0.0
        var pendingTotal = 0.0
        invoices.forEach { invoice ->
            // Solo consideramos facturas con saldo pendiente para crédito.
            val outstanding = invoice.outstandingAmount
            if (outstanding <= 0.0) return@forEach
            val paid = invoice.paidAmount.coerceAtLeast(0.0)
            // Resolvemos moneda de la factura para el desglose.
            val invoiceCurrency = normalizeCurrency(invoice.partyAccountCurrency)
                ?: normalizeCurrency(invoice.currency)
            val currencyKey = invoiceCurrency.uppercase()
            pendingByCurrency[currencyKey] = (pendingByCurrency[currencyKey] ?: 0.0) + outstanding
            if (paid > 0.0) {
                partialByCurrency[currencyKey] = (partialByCurrency[currencyKey] ?: 0.0) + paid
            }
            // Convertimos a moneda del POS para los totales globales.
            val rate = if (invoiceCurrency.equals(posCurrency, ignoreCase = true)) {
                1.0
            } else {
                val key = "${invoiceCurrency.uppercase()}->$posCurrency"
                rateCache.getOrPut(key) {
                    cashBoxManager.resolveExchangeRateBetween(
                        invoiceCurrency,
                        posCurrency,
                        allowNetwork = false
                    ) ?: 1.0
                }
            }
            pendingTotal += outstanding * rate
            if (paid > 0.0) {
                partialTotal += paid * rate
            }
        }
        // Redondeamos para mantener consistencia visual y contable.
        return CreditTotals(
            partialTotal = roundToCurrency(partialTotal),
            pendingTotal = roundToCurrency(pendingTotal),
            partialByCurrency = partialByCurrency.mapValues { roundToCurrency(it.value) },
            pendingByCurrency = pendingByCurrency.mapValues { roundToCurrency(it.value) }
        )
    }
}

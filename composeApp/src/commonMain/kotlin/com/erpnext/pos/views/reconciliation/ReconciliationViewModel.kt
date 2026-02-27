package com.erpnext.pos.views.reconciliation

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.auth.SessionRefresher
import com.erpnext.pos.data.repositories.PosProfilePaymentMethodLocalRepository
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.sync.PushSyncRunner
import com.erpnext.pos.sync.SyncContextProvider
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.localSource.dao.ShiftPaymentRow
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.utils.CurrencyService
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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(ExperimentalTime::class)
class ReconciliationViewModel(
    private val cashBoxManager: CashBoxManager,
    private val salesInvoiceDao: SalesInvoiceDao,
    private val paymentMethodLocalRepository: PosProfilePaymentMethodLocalRepository,
    private val pushSyncRunner: PushSyncRunner,
    private val syncContextProvider: SyncContextProvider,
    private val networkMonitor: NetworkMonitor,
    private val sessionRefresher: SessionRefresher,
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
                    AppLogger.info(
                        "cashbox-close-log: profile=${summary.posProfile} opening=${summary.openingEntryId} " +
                                "openingByMode=${summary.openingByMode} expectedByMode=${summary.expectedByMode} countedByMode=$countedByMode"
                    )
                }
                cashBoxManager.closeCashBox()
                if (cashBoxManager.cashboxState.value) {
                    error("No se pudo cerrar la caja. Intenta nuevamente.")
                }
                _closeState.update { it.copy(isClosing = false, isClosed = true) }
                AppLogger.info("cashbox-close-log: cierre completado")
                loadShiftSummary()
            },
            exceptionHandler = { error ->
                AppLogger.warn("Reconciliation: closeCashbox failed", error)
                _closeState.update {
                    it.copy(
                        isClosing = false,
                        errorMessage = error.message ?: "Failed to close the cashbox."
                    )
                }
            },
            loadingMessage = "Cerrando caja..."
        )
    }

    private suspend fun prepareForClose(): Boolean {
        _closeState.update { it.copy(isSyncing = false, syncMessage = null, errorMessage = null) }
        val isOnline = networkMonitor.isConnected.firstOrNull() == true
        if (!isOnline) {
            return true
        }
        if (!sessionRefresher.ensureValidSession()) {
            AppLogger.warn(
                "Reconciliation: sesión no válida durante pre-cierre; se continuará en modo offline-first."
            )
            return true
        }
        val syncContext = syncContextProvider.buildContext()
        if (syncContext == null) {
            AppLogger.warn(
                "Reconciliation: contexto de sync no disponible; se continuará cierre local pendiente de sync."
            )
            return true
        }
        return runCatching {
            _closeState.update {
                it.copy(
                    isSyncing = true,
                    syncMessage = "Sincronizando pendientes antes del cierre...",
                    errorMessage = null
                )
            }
            val pushReport = pushSyncRunner.runPushQueue(syncContext) { docType ->
                _closeState.update {
                    it.copy(
                        isSyncing = true,
                        syncMessage = "Sincronizando: $docType"
                    )
                }
            }
            if (pushReport.hasConflicts) {
                AppLogger.warn(
                    "Reconciliation: push detectó ${pushReport.conflictCount} conflicto(s) remotos antes del cierre."
                )
            }
            _closeState.update { it.copy(isSyncing = false, syncMessage = null, errorMessage = null) }
            true
        }.getOrElse { error ->
            AppLogger.warn("Reconciliation: prepareForClose sync failed", error)
            _closeState.update { it.copy(isSyncing = false, syncMessage = null, errorMessage = null) }
            true
        }
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
        // Nos quedamos solo con pagos válidos para arqueo diario.
        val paymentRowsFiltered = paymentRows.filter { it.enteredAmount > 0.0 || it.amount > 0.0 }
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
            val normalized = normalizeCurrency(currency)
            methods.map { it.mopName to normalized }
        }.toMap()
        val cashCurrencies = cashMethodsByCurrency.keys.map {
            normalizeCurrency(it)
        }.distinct().sorted()
        val paymentsByMode =
            aggregatePaymentsByMode(
                invoices,
                paymentRowsFiltered,
                posCurrency,
                modeCurrency,
                rateCache
            )
        val cashModes = resolveCashModes(context, openingByMode)
        val paymentsCashByCurrency =
            aggregateCashByCurrency(
                paymentRowsFiltered,
                invoices,
                posCurrency,
                cashModes,
                rateCache
            )
        val paymentsByCurrency =
            aggregatePaymentsByCurrency(invoices, paymentRowsFiltered, posCurrency)
        val availableModes = context.paymentModes.mapNotNull { mode ->
            mode.modeOfPayment.takeIf { it.isNotBlank() }
        }
        // Construye el esperado por modo (apertura + pagos del turno).
        val expectedByMode = buildExpectedByMode(openingByMode, paymentsByMode, availableModes)
        val openingByCurrency =
            mapOpeningByCurrency(openingByMode, modeCurrency)
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
        val creditTotals = aggregateCreditTotals(
            invoices = invoices,
            paymentRows = paymentRowsFiltered,
            cashModes = cashModes,
            posCurrency = posCurrency,
            rateCache = rateCache
        )
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
        invoices: List<SalesInvoiceEntity>,
        posCurrency: String,
        cashModes: Set<String>,
        rateCache: MutableMap<String, Double>
    ): Map<String, Double> {
        // Mapa de facturas para resolver totales y calcular vuelto real por invoice.
        val invoicesByName = invoices.associateBy { it.invoiceName }
        val totals = mutableMapOf<String, Double>()
        val cashRowsByInvoice = mutableMapOf<String, MutableList<ShiftPaymentRow>>()
        val nonCashBaseByInvoice = mutableMapOf<String, Double>()
        rows.filter { isCashModeName(it.modeOfPayment) || cashModes.contains(it.modeOfPayment) }
            .forEach { row ->
                val payCurrency = resolvePaymentCurrency(row)
                val normalizedCurrency = payCurrency.uppercase()
                // Para efectivo sumamos lo recibido (entered_amount) si existe.
                val amountInPayCurrency = resolvePaymentAmount(row, payCurrency)
                totals[normalizedCurrency] =
                    (totals[normalizedCurrency] ?: 0.0) + amountInPayCurrency
                cashRowsByInvoice.getOrPut(row.invoiceName) { mutableListOf() }.add(row)
            }
        rows.filterNot { isCashModeName(it.modeOfPayment) || cashModes.contains(it.modeOfPayment) }
            .forEach { row ->
                // Pagos no efectivo en base (POS) para descontar del total de la factura.
                nonCashBaseByInvoice[row.invoiceName] =
                    (nonCashBaseByInvoice[row.invoiceName] ?: 0.0) + row.amount
            }
        // Calculamos vuelto por factura para restarlo en la moneda donde realmente se entregó.
        cashRowsByInvoice.forEach { (invoiceName, cashRows) ->
            val invoice = invoicesByName[invoiceName] ?: return@forEach
            val invoiceCurrency = normalizeCurrency(invoice.currency)
            val invoiceTotal = invoice.grandTotal
            val nonCashPaid = nonCashBaseByInvoice[invoiceName] ?: 0.0
            val cashDue = (invoiceTotal - nonCashPaid).coerceAtLeast(0.0)
            val cashPaidBase = cashRows.sumOf { it.amount }
            val changeBase = (cashPaidBase - cashDue).takeIf { it > 0.0 } ?: 0.0
            if (changeBase <= 0.0) return@forEach
            val changeCurrency = resolveChangeCurrency(cashRows, posCurrency)
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
    ): String {
        val dominantRow = cashRows.maxByOrNull { it.amount }
        return dominantRow?.let { resolvePaymentCurrency(it) }
            ?: posCurrency
    }

    private suspend fun aggregatePaymentsByCurrency(
        invoices: List<SalesInvoiceEntity>,
        rows: List<ShiftPaymentRow>,
        posCurrency: String,
    ): Map<String, Double> {
        val totals = mutableMapOf<String, Double>()
        val paymentsByInvoice = mutableMapOf<String, Double>()
        val invoiceByName = invoices.associateBy { it.invoiceName }
        rows.forEach { row ->
            val payCurrency = resolvePaymentCurrency(row)
            val normalizedCurrency = payCurrency.uppercase()
            val amount = resolvePaymentAmount(row, payCurrency)
            totals[normalizedCurrency] = (totals[normalizedCurrency] ?: 0.0) + amount

            val invoice = invoiceByName[row.invoiceName]
            val receivableCurrency = normalizeCurrency(invoice?.partyAccountCurrency)
            val invoiceCurrency = normalizeCurrency(invoice?.currency)
            val rateInvToRc = CurrencyService.resolveInvoiceToReceivableRateUnified(
                invoiceCurrency = invoiceCurrency,
                receivableCurrency = receivableCurrency,
                conversionRate = invoice?.conversionRate,
                customExchangeRate = invoice?.customExchangeRate,
                posCurrency = posCurrency,
                posExchangeRate = cashBoxManager.getContext()?.exchangeRate,
                rateResolver = { from, to ->
                    cashBoxManager.resolveExchangeRateBetween(from, to, allowNetwork = false)
                }
            )
            val rowReceivable = CurrencyService.amountInvoiceToReceivable(
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
                val code = currency.uppercase()
                totals[code] = (totals[code] ?: 0.0) + delta
            }
        }

        return totals.mapValues { roundToCurrency(it.value) }
    }

    // Prioriza la moneda capturada en el pago; si falta, usa la del modo; último recurso la del POS.
    private fun resolvePaymentCurrency(
        row: ShiftPaymentRow,
    ): String {
        val fromRow = normalizeCurrency(row.paymentCurrency)
        return fromRow.uppercase()
    }

    private fun resolvePaymentAmount(row: ShiftPaymentRow, paymentCurrency: String): Double {
        // entered_amount es el monto en la moneda efectivamente recibida.
        if (row.enteredAmount > 0.0) return row.enteredAmount

        if (row.exchangeRate > 0.0 && paymentCurrency.isNotBlank()) {
            // amount está en moneda de factura, exchangeRate es pago -> factura.
            return row.amount / row.exchangeRate
        }
        return row.amount
    }

    private fun resolveModeCurrency(
        mode: String,
        modeCurrency: Map<String, String>,
    ): String {
        return normalizeCurrency(modeCurrency[mode])
    }

    private fun mapOpeningByCurrency(
        openingByMode: Map<String, Double>,
        modeCurrency: Map<String, String>
    ): Map<String, Double> {
        val acc = mutableMapOf<String, Double>()
        openingByMode.forEach { (mode, amount) ->
            val currency = resolveModeCurrency(mode, modeCurrency)
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
        invoices: List<SalesInvoiceEntity>,
        rows: List<ShiftPaymentRow>,
        posCurrency: String,
        modeCurrency: Map<String, String>,
        rateCache: MutableMap<String, Double>
    ): Map<String, Double> {
        val totals = mutableMapOf<String, Double>()
        val paymentsByInvoice = mutableMapOf<String, Double>()
        val invoiceByName = invoices.associateBy { it.invoiceName }
        rows.forEach { row ->
            val payCurrency = resolvePaymentCurrency(row)
            val amount = resolvePaymentAmount(row, payCurrency)
            totals[row.modeOfPayment] = (totals[row.modeOfPayment] ?: 0.0) + amount
            val invoice = invoiceByName[row.invoiceName]
            val receivableCurrency = normalizeCurrency(invoice?.partyAccountCurrency)
            val invoiceCurrency = normalizeCurrency(invoice?.currency)
            val rateInvToRc =
                CurrencyService.resolveInvoiceToReceivableRateUnified(
                    invoiceCurrency = invoiceCurrency,
                    receivableCurrency = receivableCurrency,
                    conversionRate = invoice?.conversionRate,
                    customExchangeRate = invoice?.customExchangeRate,
                    posCurrency = posCurrency,
                    posExchangeRate = cashBoxManager.getContext()?.exchangeRate,
                    rateResolver = { from, to ->
                        cashBoxManager.resolveExchangeRateBetween(from, to, allowNetwork = false)
                    }
                )
            val rowReceivable = CurrencyService.amountInvoiceToReceivable(
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
            val captured = paymentsByInvoice[invoiceName] ?: 0.0
            val delta = paidAmount - captured
            if (delta > 0.005) {
                val mode =
                    invoice.modeOfPayment?.takeIf { it.isNotBlank() } ?: UNASSIGNED_PAYMENT_MODE
                val targetCurrency = resolveModeCurrency(mode, modeCurrency)
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
            mode.type?.equals(
                "Cash",
                ignoreCase = true
            ) == true || isCashModeName(mode.modeOfPayment)
        }.map { it.modeOfPayment }
        if (direct.isNotEmpty()) return direct.toSet()
        val fallback = openingByMode.keys.filter { mode -> isCashModeName(mode) }
        return if (fallback.isNotEmpty()) fallback.toSet() else openingByMode.keys
    }

    private fun isCashModeName(mode: String?): Boolean {
        if (mode.isNullOrBlank()) return false
        val normalized = mode.trim()
        return normalized.contains("cash", ignoreCase = true) ||
                normalized.contains("efectivo", ignoreCase = true)
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
        invoices: List<SalesInvoiceEntity>,
        paymentRows: List<ShiftPaymentRow>,
        cashModes: Set<String>,
        posCurrency: String,
        rateCache: MutableMap<String, Double>
    ): CreditTotals {
        // Separamos pagos parciales (paid) y pendientes (outstanding) por moneda.
        val partialByCurrency = mutableMapOf<String, Double>()
        val pendingByCurrency = mutableMapOf<String, Double>()
        var partialTotal = 0.0
        var pendingTotal = 0.0

        // Pagos parciales en efectivo por moneda real recibida
        val cashPartialsByCurrency = paymentRows
            .filter { cashModes.contains(it.modeOfPayment) }
            .groupBy { resolvePaymentCurrency(it) }
            .mapValues { entry -> entry.value.sumOf { resolvePaymentAmount(it, entry.key) } }

        invoices.forEach { invoice ->
            // Solo consideramos facturas con saldo pendiente para crédito.
            val outstanding = invoice.outstandingAmount
            if (outstanding <= 0.0) return@forEach
            val receivableCurrency = normalizeCurrency(invoice.partyAccountCurrency)
            val currencyKey = receivableCurrency.uppercase()
            pendingByCurrency[currencyKey] = (pendingByCurrency[currencyKey] ?: 0.0) + outstanding

            // Totales globales en moneda POS (convierte pendiente a POS usando moneda de cuenta).
            val rate = if (receivableCurrency.equals(posCurrency, ignoreCase = true)) {
                1.0
            } else {
                val key = "${receivableCurrency.uppercase()}->$posCurrency"
                rateCache.getOrPut(key) {
                    cashBoxManager.resolveExchangeRateBetween(
                        receivableCurrency,
                        posCurrency,
                        allowNetwork = false
                    ) ?: 1.0
                }
            }
            pendingTotal += outstanding * rate
        }

        // Pagos parciales: solo efectivo, en la moneda real del pago.
        cashPartialsByCurrency.forEach { (code, amount) ->
            partialByCurrency[code.uppercase()] = roundToCurrency(amount)
            // Convertimos cada parcial a moneda POS para el total global.
            val rate = if (code.equals(posCurrency, ignoreCase = true)) 1.0 else {
                val key = "${code.uppercase()}->$posCurrency"
                rateCache.getOrPut(key) {
                    cashBoxManager.resolveExchangeRateBetween(
                        code,
                        posCurrency,
                        allowNetwork = false
                    )
                        ?: 1.0
                }
            }
            partialTotal += amount * rate
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

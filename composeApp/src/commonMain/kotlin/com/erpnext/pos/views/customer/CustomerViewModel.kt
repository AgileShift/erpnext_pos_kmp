package com.erpnext.pos.views.customer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.models.CompanyBO
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.domain.usecases.CancelSalesInvoiceInput
import com.erpnext.pos.domain.usecases.CancelSalesInvoiceUseCase
import com.erpnext.pos.domain.usecases.CheckCustomerCreditUseCase
import com.erpnext.pos.domain.usecases.CreateCustomerInput
import com.erpnext.pos.domain.usecases.CreateCustomerUseCase
import com.erpnext.pos.domain.usecases.CustomerCreditInput
import com.erpnext.pos.domain.usecases.CustomerInvoiceHistoryInput
import com.erpnext.pos.domain.usecases.CustomerQueryInput
import com.erpnext.pos.domain.usecases.DownloadSalesInvoicePdfUseCase
import com.erpnext.pos.domain.usecases.FetchCustomerDetailUseCase
import com.erpnext.pos.domain.usecases.FetchCustomerGroupsLocalUseCase
import com.erpnext.pos.domain.usecases.FetchCustomerInvoicesLocalForPeriodUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersLocalWithStateUseCase
import com.erpnext.pos.domain.usecases.FetchOutstandingInvoicesLocalForCustomerUseCase
import com.erpnext.pos.domain.usecases.FetchPaymentTermsLocalUseCase
import com.erpnext.pos.domain.usecases.FetchSalesInvoiceLocalUseCase
import com.erpnext.pos.domain.usecases.FetchSalesInvoiceWithItemsUseCase
import com.erpnext.pos.domain.usecases.FetchTerritoriesLocalUseCase
import com.erpnext.pos.domain.usecases.InvoiceCancellationAction
import com.erpnext.pos.domain.usecases.PartialReturnInput
import com.erpnext.pos.domain.usecases.PartialReturnUseCase
import com.erpnext.pos.domain.usecases.PushPendingCustomersUseCase
import com.erpnext.pos.domain.usecases.RebuildCustomerSummariesUseCase
import com.erpnext.pos.domain.utils.UUIDGenerator
import com.erpnext.pos.localSource.dao.CompanyDao
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.dao.PosProfilePaymentMethodDao
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.localSource.preferences.ReturnPolicyPreferences
import com.erpnext.pos.remoteSource.mapper.toDto
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.buildPaymentModeDetailMap
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.openPdfFile
import com.erpnext.pos.utils.parseErpDateTimeToEpochMillis
import com.erpnext.pos.utils.resolvePaymentToReceivableRate
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.utils.savePdfFileAs
import com.erpnext.pos.utils.sharePdfFile
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.toErpDate
import com.erpnext.pos.utils.toErpDateTime
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.billing.PaymentLine
import com.erpnext.pos.views.payment.PaymentHandler
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

@OptIn(FlowPreview::class, ExperimentalTime::class)
/**
 * Customer view consumes local-only sources:
 * - Customers from CustomerLocalSource via FetchCustomersLocalWithStateUseCase.
 * - Outstanding invoices from SalesInvoiceDao via FetchOutstandingInvoicesLocalForCustomerUseCase.
 */
class CustomerViewModel(
    private val cashboxManager: CashBoxManager,
    private val fetchCustomersUseCase: FetchCustomersLocalWithStateUseCase,
    private val checkCustomerCreditUseCase: CheckCustomerCreditUseCase,
    private val rebuildCustomerSummariesUseCase: RebuildCustomerSummariesUseCase,
    private val fetchCustomerDetailUseCase: FetchCustomerDetailUseCase,
    private val fetchOutstandingInvoicesUseCase: FetchOutstandingInvoicesLocalForCustomerUseCase,
    private val fetchCustomerInvoicesForPeriodUseCase: FetchCustomerInvoicesLocalForPeriodUseCase,
    private val fetchSalesInvoiceLocalUseCase: FetchSalesInvoiceLocalUseCase,
    private val downloadSalesInvoicePdfUseCase: DownloadSalesInvoicePdfUseCase,
    private val fetchSalesInvoiceWithItemsUseCase: FetchSalesInvoiceWithItemsUseCase,
    private val modeOfPaymentDao: ModeOfPaymentDao,
    private val posProfilePaymentMethodDao: PosProfilePaymentMethodDao,
    private val paymentHandler: PaymentHandler,
    private val createCustomerUseCase: CreateCustomerUseCase,
    private val pushPendingCustomersUseCase: PushPendingCustomersUseCase,
    private val fetchCustomerGroupsUseCase: FetchCustomerGroupsLocalUseCase,
    private val fetchTerritoriesUseCase: FetchTerritoriesLocalUseCase,
    private val fetchPaymentTermsUseCase: FetchPaymentTermsLocalUseCase,
    private val companyDao: CompanyDao,
    private val cancelSalesInvoiceUseCase: CancelSalesInvoiceUseCase,
    private val partialReturnUseCase: PartialReturnUseCase,
    private val networkMonitor: NetworkMonitor,
    private val returnPolicyPreferences: ReturnPolicyPreferences,
) : BaseViewModel() {

    private val _stateFlow: MutableStateFlow<CustomerState> =
        MutableStateFlow(CustomerState.Loading)
    val stateFlow = _stateFlow

    private val _invoicesState = MutableStateFlow<CustomerInvoicesState>(CustomerInvoicesState.Idle)
    val invoicesState = _invoicesState
    private val _outstandingInvoicesPagingFlow =
        MutableStateFlow(flowOf(PagingData.empty<SalesInvoiceBO>()))
    val outstandingInvoicesPagingFlow = _outstandingInvoicesPagingFlow.asStateFlow()

    private val _paymentState = MutableStateFlow(CustomerPaymentState())
    val paymentState = _paymentState

    private val _dialogDataState = MutableStateFlow(CustomerDialogDataState())
    val dialogDataState = _dialogDataState.asStateFlow()

    private val _historyState =
        MutableStateFlow<CustomerInvoiceHistoryState>(CustomerInvoiceHistoryState.Idle)
    val historyState = _historyState
    private val _historyInvoicesPagingFlow =
        MutableStateFlow(flowOf(PagingData.empty<SalesInvoiceBO>()))
    val historyInvoicesPagingFlow = _historyInvoicesPagingFlow.asStateFlow()

    private val _historyMessage = MutableStateFlow<String?>(null)
    val historyMessage = _historyMessage
    private val _returnInfoMessage = MutableStateFlow<String?>(null)
    val returnInfoMessage = _returnInfoMessage

    private val _customerMessage = MutableStateFlow<String?>(null)
    val customerMessage = _customerMessage

    private val _customersPagingFlow =
        MutableStateFlow<Flow<PagingData<CustomerBO>>>(flowOf(PagingData.empty()))
    val customersPagingFlow = _customersPagingFlow.asStateFlow()

    private var didRebuildSummaries = false

    private val _historyActionBusy = MutableStateFlow(false)
    val historyActionBusy = _historyActionBusy.asStateFlow()

    private val _returnPolicy =
        MutableStateFlow(com.erpnext.pos.domain.models.ReturnPolicySettings())
    val returnPolicy = _returnPolicy.asStateFlow()

    private var historyCustomerId: String? = null
    private var outstandingCustomerId: String? = null

    private var paymentModeDetails: Map<String, ModeOfPaymentEntity> = emptyMap()
    private var customersJob: kotlinx.coroutines.Job? = null
    private val paymentIdCache: MutableMap<String, String> = mutableMapOf()
    private var paymentRateCacheCurrency: String? = null
    private var paymentRateCache: MutableMap<String, Double> = mutableMapOf()

    private fun buildPaymentState(
        isSubmitting: Boolean = false,
        errorMessage: String? = null,
        successMessage: String? = null,
        modeTypes: Map<String, ModeOfPaymentEntity>? = mapOf(),
        modeDetails: Map<String, String>? = mapOf(),
        paymentModesOverride: List<POSPaymentModeOption>? = null,
    ): CustomerPaymentState {
        val context = cashboxManager.getContext()
        return CustomerPaymentState(
            isSubmitting = isSubmitting,
            errorMessage = errorMessage,
            successMessage = successMessage,
            baseCurrency = context?.companyCurrency ?: (context?.currency ?: "USD"),
            partyAccountCurrency = context?.partyAccountCurrency ?: (context?.currency ?: "USD"),
            allowedCurrencies = context?.allowedCurrencies ?: emptyList(),
            paymentModes = paymentModesOverride ?: (context?.paymentModes ?: emptyList()),
            exchangeRate = context?.exchangeRate ?: 1.0,
            modeTypes = modeTypes,
            paymentModeCurrencyByMode = modeDetails,
        )
    }

    private suspend fun resolvePaymentModesForState(): List<POSPaymentModeOption> {
        val context = cashboxManager.getContext() ?: cashboxManager.initializeContext()
        val profileName = context?.profileName?.trim().orEmpty()
        if (profileName.isBlank()) return context?.paymentModes ?: emptyList()

        val resolved =
            runCatching { posProfilePaymentMethodDao.getResolvedMethodsForProfile(profileName) }
                .getOrElse { emptyList() }

        val mapped =
            resolved
                .asSequence()
                .filter { it.enabled && it.enabledInProfile }
                .map {
                    POSPaymentModeOption(
                        name = it.mopName,
                        modeOfPayment = it.mopName,
                        account = it.account,
                        currency = it.currency,
                        type = it.type,
                        allowInReturns = it.allowInReturns,
                    )
                }
                .distinctBy { it.modeOfPayment.uppercase() }
                .toList()

        return mapped.ifEmpty { (context?.paymentModes ?: emptyList()) }
    }

    private var searchFilter by mutableStateOf<String?>(null)
    private var selectedState by mutableStateOf<String?>(null)
    private val searchFlow = MutableStateFlow<String?>(null)
    private val stateFlowFilter = MutableStateFlow<String?>(null)

    init {
        preloadPaymentState()
        loadDialogData()
        viewModelScope.launch {
            returnPolicyPreferences.settings.collect { settings -> _returnPolicy.value = settings }
        }
        viewModelScope.launch {
            if (!didRebuildSummaries) {
                didRebuildSummaries = true
                runCatching { rebuildCustomerSummariesUseCase(Unit) }
            }
        }
        combine(searchFlow, stateFlowFilter) { q, s -> q to s }
            .debounce(250)
            .onEach { (q, s) -> fetchAllCustomers(q, s) }
            .launchIn(viewModelScope)

        fetchAllCustomers()
    }

    private fun loadDialogData() {
        viewModelScope.launch {
            val groups = runCatching { fetchCustomerGroupsUseCase() }.getOrElse { emptyList() }
            val territories = runCatching { fetchTerritoriesUseCase() }.getOrElse { emptyList() }
            val paymentTerms =
                runCatching { fetchPaymentTermsUseCase(null) }.getOrElse { emptyList() }
            val companies =
                runCatching {
                    companyDao.getAll().map {
                        CompanyBO(
                            company = it.companyName,
                            defaultCurrency = it.defaultCurrency,
                            country = it.country,
                            ruc = it.taxId,
                        )
                    }
                }
                    .getOrElse { emptyList() }
            _dialogDataState.value =
                CustomerDialogDataState(
                    customerGroups = groups,
                    territories = territories,
                    paymentTerms = paymentTerms,
                    companies = companies,
                )
        }
    }

    private fun preloadPaymentState() {
        viewModelScope.launch {
            val paymentModes = resolvePaymentModesForState()
            _paymentState.value =
                buildPaymentState(
                    modeTypes = paymentModeDetails,
                    modeDetails = emptyMap(),
                    paymentModesOverride = paymentModes,
                )
        }
    }

    fun fetchAllCustomers(query: String? = null, state: String? = null) {
        _stateFlow.value = CustomerState.Loading

        customersJob?.cancel()
        customersJob =
            executeUseCase(
                action = {
                    // pequeña espera para evitar parpadeos al cambiar filtros
                    delay(120)
                    val input = CustomerQueryInput(query, state)
                    _customersPagingFlow.value = fetchCustomersUseCase.invoke(input)
                    val totalCount = fetchCustomersUseCase.count(input)
                    val pendingCount = fetchCustomersUseCase.countPending(input)
                    _stateFlow.value =
                        if (totalCount == 0) {
                            CustomerState.Empty
                        } else {
                            CustomerState.Success(
                                totalCount = totalCount,
                                pendingCount = pendingCount
                            )
                        }
                },
                exceptionHandler = {
                    _stateFlow.value = CustomerState.Error(it.message ?: "Error al cargar clientes")
                },
                showLoading = false,
            )
    }

    fun onSearchQueryChanged(query: String?) {
        searchFilter = query
        searchFlow.value = query
    }

    fun onStateSelected(state: String?) {
        selectedState = state
        stateFlowFilter.value = state
    }

    fun onRefresh() {
        fetchAllCustomers(searchFilter, selectedState)
        historyCustomerId?.let { loadInvoiceHistory(it) }
        outstandingCustomerId?.let { loadOutstandingInvoices(it) }
    }

    fun checkCredit(customerId: String, amount: Double, onResult: (Boolean, String) -> Unit) {
        executeUseCase(
            action = {
                val isValid =
                    checkCustomerCreditUseCase.invoke(CustomerCreditInput(customerId, amount))
                val customer = fetchCustomerDetailUseCase.invoke(customerId)
                val message =
                    if (isValid) {
                        "Crédito suficiente: Disponible ${customer?.availableCredit}"
                    } else {
                        "Crédito insuficiente: Disponible ${customer?.availableCredit ?: 0.0}"
                    }
                onResult(isValid, message)
            },
            exceptionHandler = { onResult(false, it.message ?: "Error al validar credito") },
            loadingMessage = "Validando crédito...",
        )
    }

    fun loadOutstandingInvoices(
        customerId: String,
        successMessage: String? = null,
        errorMessage: String? = null,
    ) {
        outstandingCustomerId = customerId
        _invoicesState.value = CustomerInvoicesState.Loading

        viewModelScope.launch {
            val modeDefinitions =
                runCatching {
                    modeOfPaymentDao.getAllModes(
                        cashboxManager.getContext()?.company ?: ""
                    )
                }
                    .getOrElse { emptyList() }

            val modeTypes = modeDefinitions.associateBy { it.modeOfPayment }
            paymentModeDetails = buildPaymentModeDetailMap(modeDefinitions)

            val paymentModeCurrencyByMode = buildMap {
                modeDefinitions.forEach { def ->
                    val currency = def.currency?.trim()?.uppercase().orEmpty()
                    if (currency.isNotBlank()) {
                        put(def.modeOfPayment, currency)
                        put(def.name, currency)
                    }
                }
            }

            _paymentState.value =
                buildPaymentState(
                    modeTypes = modeTypes,
                    modeDetails = paymentModeCurrencyByMode,
                    successMessage = successMessage,
                    errorMessage = errorMessage,
                    paymentModesOverride = resolvePaymentModesForState(),
                )
        }

        executeUseCase(
            action = {
                _outstandingInvoicesPagingFlow.value =
                    fetchOutstandingInvoicesUseCase.invoke(customerId)

                val baseCurrency = normalizeCurrency(cashboxManager.getContext()?.companyCurrency)
                val exchangeRates = mutableMapOf<String, Double>()
                paymentState.value.allowedCurrencies
                    .map { normalizeCurrency(it.code) }
                    .distinct()
                    .forEach { currency ->
                        val resolved =
                            cashboxManager
                                .resolveExchangeRateBetween(
                                    fromCurrency = baseCurrency,
                                    toCurrency = currency,
                                    allowNetwork = false,
                                )
                                ?.takeIf { it > 0.0 }
                        if (resolved != null) exchangeRates[currency] = resolved
                    }

                _invoicesState.value =
                    CustomerInvoicesState.Success(
                        invoices = emptyList(),
                        exchangeRateByCurrency = exchangeRates,
                    )
                refreshPaymentModeDetails()
            },
            exceptionHandler = {
                _invoicesState.value =
                    CustomerInvoicesState.Error(
                        it.message ?: "No se pudieron cargar las facturas pendientes."
                    )
            },
            loadingMessage = "Cargando facturas pendientes...",
        )
    }

    fun clearOutstandingInvoices() {
        outstandingCustomerId = null
        _invoicesState.value = CustomerInvoicesState.Idle
        _outstandingInvoicesPagingFlow.value = flowOf(PagingData.empty())
        _paymentState.value = buildPaymentState()
    }

    fun registerPayment(
        customerId: String,
        invoiceId: String,
        modeOfPayment: String,
        enteredAmount: Double,
        enteredCurrency: String,
        referenceNumber: String,
    ) {
        if (modeOfPayment.isBlank()) {
            _paymentState.value = buildPaymentState(errorMessage = "Selecciona un modo de pago.")
            return
        }
        if (invoiceId.isBlank()) {
            _paymentState.value = buildPaymentState(errorMessage = "Selecciona una factura.")
            return
        }
        if (enteredAmount <= 0) {
            _paymentState.value = buildPaymentState(errorMessage = "Ingresa un monto válido.")
            return
        }

        _paymentState.value = buildPaymentState(isSubmitting = true)

        executeUseCase(
            action = {
                refreshPaymentModeDetails()

                val invoice =
                    fetchSalesInvoiceLocalUseCase(invoiceId)
                        ?: error("No se pudo cargar la factura local.")

                val context = cashboxManager.requireContext()
                val postingDate = getTimeMillis().toErpDateTime()

                val invoiceCurrency = normalizeCurrency(invoice.currency)
                val receivableCurrency = normalizeCurrency(invoice.partyAccountCurrency)

                val normalizedInvoiceCurrency = normalizeCurrency(invoiceCurrency)
                if (!normalizedInvoiceCurrency.equals(
                        paymentRateCacheCurrency,
                        ignoreCase = true
                    )
                ) {
                    paymentRateCacheCurrency = normalizedInvoiceCurrency
                    paymentRateCache = mutableMapOf(normalizedInvoiceCurrency to 1.0)
                }

                val customer =
                    fetchCustomerDetailUseCase.invoke(customerId)
                        ?: CustomerBO(
                            name = customerId,
                            customerName = "",
                            territory = "",
                            currency = receivableCurrency,
                            customerType = "",
                        )

                val cacheKey =
                    listOf(
                        invoiceId.trim(),
                        modeOfPayment.trim(),
                        enteredCurrency.trim().uppercase(),
                        roundToCurrency(enteredAmount).toString(),
                    )
                        .joinToString("|")
                val resolvedReference =
                    referenceNumber.takeIf { it.isNotBlank() }
                        ?: paymentIdCache.getOrPut(cacheKey) { "POSPAY-${UUIDGenerator().newId()}" }

                val line =
                    PaymentLine(
                        modeOfPayment = modeOfPayment,
                        currency = normalizeCurrency(enteredCurrency),
                        enteredAmount = enteredAmount,
                        baseAmount = 0.0,
                        referenceNumber = resolvedReference,
                        exchangeRate = 0.0,
                    )

                val paymentResolved =
                    paymentHandler.resolvePaymentLine(
                        line = line,
                        invoiceCurrencyInput = invoiceCurrency,
                        paymentModeDetails = paymentModeDetails,
                        exchangeRateByCurrency = paymentRateCache,
                        round = ::roundToCurrency,
                    )
                val fixedLine = paymentResolved.line
                val updatedCache = paymentResolved.exchangeRateByCurrency
                paymentRateCache = updatedCache.toMutableMap()

                val createdInvoiceDto =
                    invoice.toDto().copy(isPos = invoice.isPos, doctype = "Sales Invoice")
                val paymentResult =
                    paymentHandler.registerPayments(
                        paymentLines = listOf(fixedLine),
                        createdInvoice = createdInvoiceDto,
                        invoiceNameForLocal = invoice.invoiceName ?: invoiceId,
                        postingDate = postingDate,
                        context = context,
                        customer = customer,
                        exchangeRateByCurrency = updatedCache,
                        paymentModeDetails = paymentModeDetails,
                        posOpeningEntry =
                            cashboxManager.getActiveCashboxWithDetails()?.cashbox?.openingEntryId,
                    )

                val invoiceCurrencyResolved = normalizeCurrency(invoice.currency)
                val invoiceToReceivableRate =
                    when {
                        invoiceCurrencyResolved.equals(receivableCurrency, ignoreCase = true) -> 1.0
                        invoice.conversionRate != null && (invoice.conversionRate ?: 0.0) > 0.0 ->
                            invoice.conversionRate

                        else -> null
                    }

                val outstandingRc = invoice.outstandingAmount.coerceAtLeast(0.0)
                val fixedLineRc =
                    invoiceToReceivableRate?.let { fixedLine.baseAmount * it }
                        ?: fixedLine.baseAmount
                val appliedRc = fixedLineRc.coerceAtMost(outstandingRc)
                val changeRc = (fixedLineRc - appliedRc).takeIf { it > 0.0 }
                val paymentToReceivableRate =
                    resolvePaymentToReceivableRate(
                        paymentCurrency = fixedLine.currency,
                        invoiceCurrency = invoiceCurrencyResolved,
                        receivableCurrency = receivableCurrency,
                        paymentToInvoiceRate = fixedLine.exchangeRate,
                        invoiceToReceivableRate = invoiceToReceivableRate,
                    )
                val changeInPaymentCurrency =
                    changeRc?.let { change ->
                        paymentToReceivableRate?.takeIf { it > 0.0 }?.let { change / it }
                    }

                val baseCurrencyForChange = normalizeCurrency(enteredCurrency)
                val changeText =
                    changeInPaymentCurrency
                        ?.takeIf { it > 0.0 }
                        ?.let { change ->
                            val symbol =
                                baseCurrencyForChange.toCurrencySymbol()
                                    .ifBlank { baseCurrencyForChange }
                            "Cambio: $symbol ${formatDoubleToString(change, 2)}"
                        }

                val invoiceLabel = invoice.invoiceName?.takeIf { it.isNotBlank() } ?: invoiceId
                val baseMessage =
                    if (paymentResult.remotePaymentsSucceeded) {
                        "Pago registrado correctamente en factura $invoiceLabel."
                    } else {
                        "Pago guardado localmente para factura $invoiceLabel. Se sincronizará cuando haya conexión."
                    }

                val finalMessage = listOfNotNull(baseMessage, changeText).joinToString(" ")

                loadOutstandingInvoices(customerId, successMessage = finalMessage)
                // Forzamos refresco inmediato de la lista principal para reflejar nuevos saldos.
                fetchAllCustomers(searchFilter, selectedState)
            },
            exceptionHandler = {
                _paymentState.value =
                    buildPaymentState(
                        errorMessage =
                            it.message?.takeIf { msg -> msg.isNotBlank() }
                                ?: "No se pudo registrar el pago de la factura $invoiceId."
                    )
            },
            loadingMessage = "Registrando pago...",
        )
    }

    suspend fun loadInvoiceLocal(invoiceId: String): SalesInvoiceWithItemsAndPayments? {
        return fetchSalesInvoiceWithItemsUseCase(invoiceId)
    }

    fun downloadInvoicePdf(
        invoiceId: String,
        action: InvoicePdfActionOption = InvoicePdfActionOption.OPEN_NOW,
    ) {
        val normalized = invoiceId.trim()
        if (normalized.isBlank()) {
            _customerMessage.value = "Factura inválida para descargar PDF."
            return
        }

        executeUseCase(
            action = {
                val path = downloadSalesInvoicePdfUseCase(normalized)
                val fileName = path.substringAfterLast('/').substringAfterLast('\\')
                when (action) {
                    InvoicePdfActionOption.OPEN_NOW -> {
                        val opened = openPdfFile(path)
                        _customerMessage.value =
                            if (opened) {
                                "PDF listo y abierto: $fileName"
                            } else {
                                "PDF descargado: $path"
                            }
                    }

                    InvoicePdfActionOption.SHARE -> {
                        val shared = sharePdfFile(path)
                        _customerMessage.value =
                            if (shared) {
                                "PDF listo para compartir: $fileName"
                            } else {
                                "PDF descargado: $path"
                            }
                    }

                    InvoicePdfActionOption.SAVE_AS -> {
                        val target = savePdfFileAs(path, fileName)
                        _customerMessage.value =
                            target?.let { "PDF guardado en: $it" } ?: "PDF descargado: $path"
                    }
                }
            },
            exceptionHandler = {
                _customerMessage.value =
                    "No se pudo descargar el PDF de la factura $normalized: ${it.message ?: "error desconocido."}"
            },
            loadingMessage = "Generando y descargando PDF...",
        )
    }

    private suspend fun refreshPaymentModeDetails() {
        if (paymentModeDetails.isNotEmpty()) return
        val company = cashboxManager.requireContext().company
        val definitions =
            runCatching { modeOfPaymentDao.getAllModes(company) }.getOrElse { emptyList() }
        paymentModeDetails = buildPaymentModeDetailMap(definitions)
    }

    fun clearPaymentMessages() {
        _paymentState.value = _paymentState.value.copy(errorMessage = null, successMessage = null)
    }

    @OptIn(ExperimentalTime::class)
    fun loadInvoiceHistory(customerId: String) {
        historyCustomerId = customerId
        viewModelScope.launch {
            _historyState.value = CustomerInvoiceHistoryState.Loading
            try {
                val now = Clock.System.now()
                val start = now - 90.days
                val invoices =
                    fetchCustomerInvoicesForPeriodUseCase(
                        CustomerInvoiceHistoryInput(
                            customerId = customerId,
                            startDate = start.toEpochMilliseconds().toErpDate(),
                            endDate = now.toEpochMilliseconds().toErpDate(),
                        )
                    )
                _historyInvoicesPagingFlow.value = invoices
                _historyState.value = CustomerInvoiceHistoryState.Success(emptyList())
            } catch (e: Exception) {
                _historyState.value =
                    CustomerInvoiceHistoryState.Error(
                        e.message ?: "No se pudo obtener el historial de facturas."
                    )
            }
        }
    }

    fun clearInvoiceHistory() {
        historyCustomerId = null
        _historyState.value = CustomerInvoiceHistoryState.Idle
        _historyInvoicesPagingFlow.value = flowOf(PagingData.empty())
        _historyMessage.value = null
    }

    fun clearHistoryMessage() {
        _historyMessage.value = null
    }

    fun clearCustomerMessage() {
        _customerMessage.value = null
    }

    fun createCustomer(input: CreateCustomerInput) {
        viewModelScope.launch {
            try {
                createCustomerUseCase(input)
                _customerMessage.value =
                    "Cliente creado localmente. Se sincronizará en el próximo sync."
                val isOnline = networkMonitor.isConnected.first()
                if (isOnline) {
                    val pushed = pushPendingCustomersUseCase(Unit)
                    if (pushed) {
                        _customerMessage.value = "Cliente sincronizado correctamente."
                    }
                }
                fetchAllCustomers(searchFilter, selectedState)
            } catch (e: Exception) {
                _customerMessage.value = e.message ?: "No se pudo crear el cliente."
            }
        }
    }

    fun submitPartialReturn(
        invoiceId: String,
        reason: String?,
        refundModeOfPayment: String?,
        refundReferenceNo: String?,
        applyRefund: Boolean,
        affectInventory: Boolean,
        itemsToReturnByCode: Map<String, Double>,
    ) {
        viewModelScope.launch {
            _historyActionBusy.value = true
            try {
                val policyMessage =
                    validateReturnPolicy(
                        invoiceId = invoiceId,
                        isPartial = true,
                        applyRefund = applyRefund,
                        reason = reason,
                    )
                if (policyMessage != null) {
                    _historyMessage.value = policyMessage
                    return@launch
                }
                val filtered = itemsToReturnByCode.filterValues { it > 0.0 }
                if (filtered.isEmpty()) {
                    throw IllegalArgumentException("Selecciona al menos un artículo para devolver.")
                }
                val localInvoice = fetchSalesInvoiceWithItemsUseCase(invoiceId)
                val preview = buildPartialReturnPreview(localInvoice, filtered)
                if (applyRefund) {
                    ensureRefundFunds(
                        refundModeOfPayment = refundModeOfPayment,
                        preview = preview,
                        invoiceId = invoiceId,
                    )
                }
                val result =
                    partialReturnUseCase(
                        PartialReturnInput(
                            invoiceName = invoiceId,
                            itemsToReturnByCode = filtered,
                            reason = reason,
                            refundModeOfPayment = refundModeOfPayment,
                            refundReferenceNo = refundReferenceNo,
                            applyRefund = applyRefund,
                            affectInventory = affectInventory,
                        )
                    )
                if (applyRefund) {
                    applyRefundCashMovement(
                        refundModeOfPayment = refundModeOfPayment,
                        preview = preview,
                        invoiceId = invoiceId,
                        partial = true,
                    )
                }
                val message =
                    buildReturnPostMessage(
                        creditNoteName = result.creditNoteName,
                        preview = preview,
                        applyRefund = applyRefund,
                        isPartial = true,
                    )
                _historyMessage.value = message
                _returnInfoMessage.value = message
            } catch (e: Exception) {
                _historyMessage.value =
                    "No se pudo registrar el retorno parcial: ${e.message ?: "error desconocido."}"
            } finally {
                _historyActionBusy.value = false
                historyCustomerId?.let { loadInvoiceHistory(it) }
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    fun performInvoiceHistoryAction(
        invoiceId: String,
        action: InvoiceCancellationAction,
        reason: String?,
        refundModeOfPayment: String?,
        refundReferenceNo: String?,
        applyRefund: Boolean,
        affectInventory: Boolean,
    ) {
        viewModelScope.launch {
            val isOnline = networkMonitor.isConnected.first()
            if (!isOnline) {
                _historyMessage.value = "Se requiere conexión para completar esta acción."
                return@launch
            }
            _historyActionBusy.value = true
            try {
                val fullReturnPreview =
                    if (action == InvoiceCancellationAction.RETURN) {
                        buildFullReturnPreview(fetchSalesInvoiceLocalUseCase(invoiceId))
                    } else {
                        null
                    }
                if (action == InvoiceCancellationAction.RETURN) {
                    val policyMessage =
                        validateReturnPolicy(
                            invoiceId = invoiceId,
                            isPartial = false,
                            applyRefund = applyRefund,
                            reason = reason,
                        )
                    if (policyMessage != null) {
                        _historyMessage.value = policyMessage
                        return@launch
                    }
                }
                val trimmedReason = reason?.takeIf { it.isNotBlank() }
                if (action == InvoiceCancellationAction.RETURN && applyRefund) {
                    ensureRefundFunds(
                        refundModeOfPayment = refundModeOfPayment,
                        preview = fullReturnPreview,
                        invoiceId = invoiceId,
                    )
                }
                val result =
                    cancelSalesInvoiceUseCase(
                        CancelSalesInvoiceInput(
                            invoiceName = invoiceId,
                            action = action,
                            reason = trimmedReason,
                            refundModeOfPayment = refundModeOfPayment,
                            refundReferenceNo = refundReferenceNo,
                            applyRefund = applyRefund,
                            affectInventory = affectInventory,
                        )
                    )
                if (action == InvoiceCancellationAction.RETURN && applyRefund) {
                    applyRefundCashMovement(
                        refundModeOfPayment = refundModeOfPayment,
                        preview = fullReturnPreview,
                        invoiceId = invoiceId,
                        partial = false,
                    )
                }
                _historyMessage.value =
                    when (action) {
                        InvoiceCancellationAction.CANCEL -> "Factura $invoiceId cancelada."
                        InvoiceCancellationAction.RETURN ->
                            buildReturnPostMessage(
                                creditNoteName = result.creditNoteName,
                                preview = fullReturnPreview,
                                applyRefund = applyRefund,
                                isPartial = false,
                            )
                    }
                if (action == InvoiceCancellationAction.RETURN) {
                    _returnInfoMessage.value = _historyMessage.value
                }
            } catch (e: Exception) {
                _historyMessage.value =
                    "No se pudo completar la acción: ${e.message ?: "error desconocido."}"
            } finally {
                _historyActionBusy.value = false
                historyCustomerId?.let { loadInvoiceHistory(it) }
            }
        }
    }

    private suspend fun validateReturnPolicy(
        invoiceId: String,
        isPartial: Boolean,
        applyRefund: Boolean,
        reason: String?,
    ): String? {
        val policy = returnPolicyPreferences.get()
        if (isPartial && !policy.allowPartialReturns) {
            return "Los retornos parciales están deshabilitados por política."
        }
        if (!isPartial && !policy.allowFullReturns) {
            return "Los retornos totales están deshabilitados por política."
        }
        if (policy.requireReason && reason.isNullOrBlank()) {
            return "Debes indicar un motivo para el retorno."
        }
        if (applyRefund && !policy.allowRefunds) {
            return "Los reembolsos están deshabilitados por política."
        }
        if (policy.maxDaysAfterInvoice > 0) {
            val invoice = fetchSalesInvoiceLocalUseCase(invoiceId)
            val invoiceMillis = parseErpDateTimeToEpochMillis(invoice?.postingDate)
            val now = Clock.System.now().toEpochMilliseconds()
            if (invoiceMillis != null) {
                val days = ((now - invoiceMillis) / (24 * 60 * 60 * 1000.0)).toInt()
                if (days > policy.maxDaysAfterInvoice) {
                    return "El retorno excede el límite de ${policy.maxDaysAfterInvoice} días."
                }
            }
        }
        if (applyRefund && policy.requirePaidInvoiceForRefund) {
            val invoice = fetchSalesInvoiceLocalUseCase(invoiceId)
            val hasPayments = (invoice?.paidAmount ?: 0.0) > 0.0
            if (!hasPayments) {
                return "Solo se permite reembolso si la factura tiene pagos."
            }
        }
        return null
    }

    private data class ReturnPreview(
        val currency: String,
        val returnTotal: Double,
        val projectedOutstanding: Double?,
    )

    private fun buildPartialReturnPreview(
        invoice: SalesInvoiceWithItemsAndPayments?,
        requested: Map<String, Double>,
    ): ReturnPreview? {
        invoice ?: return null
        val requestedRemaining = requested.mapValues { it.value.coerceAtLeast(0.0) }.toMutableMap()
        var total = 0.0
        invoice.items.forEach { item ->
            val desired = (requestedRemaining[item.itemCode] ?: 0.0).coerceAtLeast(0.0)
            if (desired <= 0.0) return@forEach
            val soldQty = kotlin.math.abs(item.qty)
            val qtyToReturn = desired.coerceAtMost(soldQty)
            if (qtyToReturn <= 0.0) return@forEach
            requestedRemaining[item.itemCode] = (desired - qtyToReturn).coerceAtLeast(0.0)
            val perUnit = if (item.qty != 0.0) item.amount / item.qty else item.rate
            total += kotlin.math.abs(perUnit) * qtyToReturn
        }
        val outstanding = invoice.invoice.outstandingAmount.coerceAtLeast(0.0)
        return ReturnPreview(
            currency = normalizeCurrency(invoice.invoice.currency),
            returnTotal = roundToCurrency(total),
            projectedOutstanding = roundToCurrency((outstanding - total).coerceAtLeast(0.0)),
        )
    }

    private fun buildFullReturnPreview(
        invoice: com.erpnext.pos.localSource.entities.SalesInvoiceEntity?
    ): ReturnPreview? {
        invoice ?: return null
        val total = invoice.grandTotal.coerceAtLeast(0.0)
        val outstanding = invoice.outstandingAmount.coerceAtLeast(0.0)
        return ReturnPreview(
            currency = normalizeCurrency(invoice.currency),
            returnTotal = roundToCurrency(total),
            projectedOutstanding = roundToCurrency((outstanding - total).coerceAtLeast(0.0)),
        )
    }

    private suspend fun ensureRefundFunds(
        refundModeOfPayment: String?,
        preview: ReturnPreview?,
        invoiceId: String,
    ) {
        val mode = refundModeOfPayment?.trim().orEmpty()
        require(mode.isNotBlank()) {
            "Debes seleccionar modo de reembolso para devolver factura $invoiceId."
        }
        val amount = preview?.returnTotal?.coerceAtLeast(0.0) ?: 0.0
        require(amount >= 0.0) {
            "No se pudo calcular el monto a reembolsar para la factura $invoiceId."
        }
        val available = cashboxManager.getAvailableFundsForMode(mode)
        check(available + 0.009 >= amount) {
            "Fondos insuficientes para reembolso en $mode. Disponible: ${
                formatMoney(
                    preview?.currency ?: "NIO",
                    roundToCurrency(available)
                )
            }."
        }
    }

    private suspend fun applyRefundCashMovement(
        refundModeOfPayment: String?,
        preview: ReturnPreview?,
        invoiceId: String,
        partial: Boolean,
    ) {
        val mode = refundModeOfPayment?.trim().orEmpty()
        val amount = preview?.returnTotal?.coerceAtLeast(0.0) ?: 0.0
        if (mode.isBlank() || amount <= 0.0) return
        val label = if (partial) "reembolso parcial" else "reembolso total"
        cashboxManager.registerRefundOutflow(
            modeOfPayment = mode,
            amount = amount,
            note = "$label factura $invoiceId",
        )
    }

    private fun buildReturnPostMessage(
        creditNoteName: String?,
        preview: ReturnPreview?,
        applyRefund: Boolean,
        isPartial: Boolean,
    ): String {
        val base =
            when {
                !creditNoteName.isNullOrBlank() && isPartial ->
                    "Retorno parcial registrado como $creditNoteName."

                !creditNoteName.isNullOrBlank() -> "Retorno registrado como $creditNoteName."
                isPartial -> "Retorno parcial registrado."
                else -> "Retorno registrado."
            }
        val destination = if (applyRefund) "reembolso" else "crédito a favor"
        val projection =
            preview
                ?.let {
                    " Monto devuelto estimado: ${formatMoney(it.currency, it.returnTotal)}. " +
                            "Saldo estimado tras retorno: ${
                                formatMoney(
                                    it.currency,
                                    it.projectedOutstanding ?: 0.0,
                                )
                            }."
                }
                .orEmpty()
        val notice =
            " Nota: en ERPNext el saldo visible puede mantenerse temporalmente hasta la conciliación o cierre de caja."
        return "$base Se aplicó como $destination.$projection$notice"
    }

    private fun formatMoney(currency: String, amount: Double): String {
        val symbol = currency.toCurrencySymbol().ifBlank { currency }
        return "$symbol ${formatDoubleToString(amount, 2)}"
    }
}

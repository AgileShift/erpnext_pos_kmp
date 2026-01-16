package com.erpnext.pos.views.customer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.usecases.CheckCustomerCreditUseCase
import com.erpnext.pos.domain.usecases.CustomerCreditInput
import com.erpnext.pos.domain.usecases.CustomerQueryInput
import com.erpnext.pos.domain.usecases.FetchCustomerDetailUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersLocalWithStateUseCase
import com.erpnext.pos.domain.usecases.FetchOutstandingInvoicesLocalForCustomerUseCase
import com.erpnext.pos.domain.usecases.FetchSalesInvoiceLocalUseCase
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.remoteSource.mapper.toDto
import com.erpnext.pos.utils.buildPaymentModeDetailMap
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.resolvePaymentToReceivableRate
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.toErpDateTime
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.billing.PaymentLine
import com.erpnext.pos.views.payment.PaymentHandler
import com.erpnext.pos.domain.utils.UUIDGenerator
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
/**
 * Customer view consumes local-only sources:
 * - Customers from CustomerLocalSource via FetchCustomersLocalWithStateUseCase.
 * - Outstanding invoices from SalesInvoiceDao via FetchOutstandingInvoicesLocalForCustomerUseCase.
 */
class CustomerViewModel(
    private val cashboxManager: CashBoxManager,
    private val fetchCustomersUseCase: FetchCustomersLocalWithStateUseCase,
    private val checkCustomerCreditUseCase: CheckCustomerCreditUseCase,
    private val fetchCustomerDetailUseCase: FetchCustomerDetailUseCase,
    private val fetchOutstandingInvoicesUseCase: FetchOutstandingInvoicesLocalForCustomerUseCase,
    private val fetchSalesInvoiceLocalUseCase: FetchSalesInvoiceLocalUseCase,
    private val modeOfPaymentDao: ModeOfPaymentDao,
    private val paymentHandler: PaymentHandler
) : BaseViewModel() {

    private val _stateFlow: MutableStateFlow<CustomerState> =
        MutableStateFlow(CustomerState.Loading)
    val stateFlow = _stateFlow

    private val _invoicesState = MutableStateFlow<CustomerInvoicesState>(CustomerInvoicesState.Idle)
    val invoicesState = _invoicesState

    private val _paymentState = MutableStateFlow(CustomerPaymentState())
    val paymentState = _paymentState

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
        modeDetails: Map<String, String>? = mapOf()
    ): CustomerPaymentState {
        val context = cashboxManager.getContext()
        return CustomerPaymentState(
            isSubmitting = isSubmitting,
            errorMessage = errorMessage,
            successMessage = successMessage,
            baseCurrency = context?.currency ?: "USD",
            partyAccountCurrency = context?.partyAccountCurrency ?: (context?.currency ?: "USD"),
            allowedCurrencies = context?.allowedCurrencies ?: emptyList(),
            paymentModes = context?.paymentModes ?: emptyList(),
            exchangeRate = context?.exchangeRate ?: 1.0,
            modeTypes = modeTypes,
            paymentModeCurrencyByMode = modeDetails
        )
    }

    private var searchFilter by mutableStateOf<String?>(null)
    private var selectedState by mutableStateOf<String?>(null)
    private val searchFlow = MutableStateFlow<String?>(null)
    private val stateFlowFilter = MutableStateFlow<String?>(null)

    init {
        preloadPaymentState()
        combine(searchFlow, stateFlowFilter) { q, s -> q to s }
            .debounce(250)
            .onEach { (q, s) -> fetchAllCustomers(q, s) }
            .launchIn(viewModelScope)

        fetchAllCustomers()
    }

    private fun preloadPaymentState() {
        viewModelScope.launch {
            val context = cashboxManager.getContext() ?: return@launch
            _paymentState.value = buildPaymentState(
                modeTypes = paymentModeDetails,
                modeDetails = emptyMap()
            )
        }
    }

    fun fetchAllCustomers(query: String? = null, state: String? = null) {
        _stateFlow.value = CustomerState.Loading

        customersJob?.cancel()
        customersJob = executeUseCase(
            action = {
                // pequeña espera para evitar parpadeos al cambiar filtros
                delay(120)
                fetchCustomersUseCase.invoke(CustomerQueryInput(query, state))
                    .collectLatest { customers ->
                        _stateFlow.value = when {
                            customers.isEmpty() -> CustomerState.Empty
                            else -> CustomerState.Success(
                                customers,
                                customers.size,
                                customers.count { (it.pendingInvoices ?: 0) > 0 },
                            )
                        }
                    }
            },
            exceptionHandler = {
                _stateFlow.value =
                    CustomerState.Error(it.message ?: "Error al cargar clientes")
            },
            showLoading = false
        )
    }

    fun onSearchQueryChanged(query: String?) {
        searchFilter = query
        searchFlow.value = query
    }

    fun toDetails(customerId: String) {}

    fun onStateSelected(state: String?) {
        selectedState = state
        stateFlowFilter.value = state
    }

    fun onRefresh() {
        fetchAllCustomers(searchFilter, selectedState)
    }

    fun checkCredit(customerId: String, amount: Double, onResult: (Boolean, String) -> Unit) {
        executeUseCase(
            action = {
                val isValid =
                    checkCustomerCreditUseCase.invoke(CustomerCreditInput(customerId, amount))
                val customer = fetchCustomerDetailUseCase.invoke(customerId)
                val message = if (isValid) {
                    "Crédito suficiente: Disponible ${customer?.availableCredit}"
                } else {
                    "Crédito insuficiente: Disponible ${customer?.availableCredit ?: 0.0}"
                }
                onResult(isValid, message)
            },
            exceptionHandler = { onResult(false, it.message ?: "Error al validar credito") }
        )
    }

    fun loadOutstandingInvoices(
        customerId: String,
        successMessage: String? = null,
        errorMessage: String? = null
    ) {
        _invoicesState.value = CustomerInvoicesState.Loading

        viewModelScope.launch {
            val modeDefinitions =
                runCatching {
                    modeOfPaymentDao.getAllModes(
                        cashboxManager.getContext()?.company ?: ""
                    )
                }.getOrElse { emptyList() }

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

            _paymentState.value = buildPaymentState(
                modeTypes = modeTypes,
                modeDetails = paymentModeCurrencyByMode,
                successMessage = successMessage,
                errorMessage = errorMessage
            )
        }

        executeUseCase(
            action = {
                val invoices = fetchOutstandingInvoicesUseCase.invoke(customerId)

                val baseCurrency = normalizeCurrency(cashboxManager.getContext()?.currency) ?: "USD"

                // Pre-caché para que la UI no dispare resolveExchangeRateBetween repetidamente.
                val exchangeRates = mutableMapOf<String, Double>()
                invoices
                    .flatMap { invoice ->
                        listOfNotNull(
                            normalizeCurrency(invoice.currency),
                            normalizeCurrency(invoice.partyAccountCurrency)
                        )
                    }
                    .distinct()
                    .forEach { currency ->
                        val resolved = cashboxManager
                            .resolveExchangeRateBetween(
                                fromCurrency = baseCurrency,
                                toCurrency = currency
                            )
                            ?.takeIf { it > 0.0 }
                        if (resolved != null) exchangeRates[currency] = resolved
                    }

                _invoicesState.value = CustomerInvoicesState.Success(
                    invoices = invoices,
                    exchangeRateByCurrency = exchangeRates
                )
                refreshPaymentModeDetails()
            },
            exceptionHandler = {
                _invoicesState.value = CustomerInvoicesState.Error(
                    it.message ?: "No se pudieron cargar las facturas pendientes."
                )
            }
        )
    }

    fun clearOutstandingInvoices() {
        _invoicesState.value = CustomerInvoicesState.Idle
        _paymentState.value = buildPaymentState()
    }

    fun registerPayment(
        customerId: String,
        invoiceId: String,
        modeOfPayment: String,
        enteredAmount: Double,
        enteredCurrency: String,
        referenceNumber: String
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

                val invoice = fetchSalesInvoiceLocalUseCase(invoiceId)
                    ?: error("No se pudo cargar la factura local.")

                val context = cashboxManager.requireContext()
                val postingDate = getTimeMillis().toErpDateTime()

                val invoiceCurrency = normalizeCurrency(invoice.currency)
                    ?: normalizeCurrency(context.currency)
                    ?: "USD"
                val receivableCurrency = normalizeCurrency(invoice.partyAccountCurrency)
                    ?: normalizeCurrency(context.partyAccountCurrency)
                    ?: normalizeCurrency(invoice.currency)
                    ?: "USD"

                val normalizedInvoiceCurrency = normalizeCurrency(invoiceCurrency) ?: "USD"
                if (!normalizedInvoiceCurrency.equals(paymentRateCacheCurrency, ignoreCase = true)) {
                    paymentRateCacheCurrency = normalizedInvoiceCurrency
                    paymentRateCache = mutableMapOf(normalizedInvoiceCurrency to 1.0)
                }

                val customer = fetchCustomerDetailUseCase.invoke(customerId)
                    ?: CustomerBO(
                        name = customerId,
                        customerName = "",
                        territory = "",
                        currency = receivableCurrency,
                        customerType = "",
                    )

                val cacheKey = listOf(
                    invoiceId.trim(),
                    modeOfPayment.trim(),
                    enteredCurrency.trim().uppercase(),
                    roundToCurrency(enteredAmount).toString()
                ).joinToString("|")
                val resolvedReference = referenceNumber.takeIf { it.isNotBlank() }
                    ?: paymentIdCache.getOrPut(cacheKey) { "POSPAY-${UUIDGenerator().newId()}" }

                val line = PaymentLine(
                    modeOfPayment = modeOfPayment,
                    currency = normalizeCurrency(enteredCurrency)
                        ?: normalizeCurrency(context.currency) ?: "USD",
                    enteredAmount = enteredAmount,
                    baseAmount = 0.0,
                    referenceNumber = resolvedReference,
                    exchangeRate = 0.0
                )

                val paymentResolved = paymentHandler.resolvePaymentLine(
                    line = line,
                    invoiceCurrencyInput = invoiceCurrency,
                    paymentModeCurrencyByMode = _paymentState.value.paymentModeCurrencyByMode
                        ?: emptyMap(),
                    paymentModeDetails = paymentModeDetails,
                    exchangeRateByCurrency = paymentRateCache,
                    round = ::roundToCurrency
                )
                val fixedLine = paymentResolved.line
                val updatedCache = paymentResolved.exchangeRateByCurrency
                paymentRateCache = updatedCache.toMutableMap()

                val paymentResult = paymentHandler.registerPayments(
                    paymentLines = listOf(fixedLine),
                    createdInvoice = invoice.toDto(),
                    invoiceNameForLocal = invoice.invoiceName ?: invoiceId,
                    postingDate = postingDate,
                    context = context,
                    customer = customer,
                    exchangeRateByCurrency = updatedCache,
                    paymentModeDetails = paymentModeDetails,
                    baseAmountCurrency = invoiceCurrency
                )

                val invoiceCurrencyResolved = normalizeCurrency(invoice.currency)
                    ?: normalizeCurrency(context.currency)
                    ?: receivableCurrency
                val invoiceToReceivableRate = when {
                    invoiceCurrencyResolved.equals(receivableCurrency, ignoreCase = true) -> 1.0
                    invoice.conversionRate != null && (invoice.customExchangeRate ?: 0.0) > 0.0 ->
                        invoice.conversionRate
                    invoice.customExchangeRate != null && (invoice.customExchangeRate ?: 0.0) > 0.0 ->
                        invoice.customExchangeRate
                    else -> null
                }

                val outstandingRc = invoice.outstandingAmount.coerceAtLeast(0.0)
                val fixedLineRc = invoiceToReceivableRate?.let { fixedLine.baseAmount * it }
                    ?: fixedLine.baseAmount
                val appliedRc = fixedLineRc.coerceAtMost(outstandingRc)
                val changeRc = (fixedLineRc - appliedRc).takeIf { it > 0.0 }
                val paymentToReceivableRate = resolvePaymentToReceivableRate(
                    paymentCurrency = fixedLine.currency,
                    invoiceCurrency = invoiceCurrencyResolved,
                    receivableCurrency = receivableCurrency,
                    paymentToInvoiceRate = fixedLine.exchangeRate,
                    invoiceToReceivableRate = invoiceToReceivableRate
                )
                val changeInPaymentCurrency = changeRc?.let { change ->
                    paymentToReceivableRate?.takeIf { it > 0.0 }?.let { change / it }
                }

                val baseCurrencyForChange = normalizeCurrency(enteredCurrency)
                    ?: normalizeCurrency(context.currency) ?: "USD"
                val changeText = changeInPaymentCurrency?.takeIf { it > 0.0 }?.let { change ->
                    val symbol = baseCurrencyForChange.toCurrencySymbol().ifBlank { baseCurrencyForChange }
                    "Cambio: $symbol ${formatDoubleToString(change, 2)}"
                }

                val baseMessage = if (paymentResult.remotePaymentsSucceeded) {
                    "Pago registrado correctamente."
                } else {
                    "Pago registrado localmente. Se sincronizará cuando haya conexión."
                }

                val finalMessage = listOfNotNull(baseMessage, changeText).joinToString(" ")

                loadOutstandingInvoices(customerId, successMessage = finalMessage)
                // Forzamos refresco inmediato de la lista principal para reflejar nuevos saldos.
                fetchAllCustomers(searchFilter, selectedState)
            },
            exceptionHandler = {
                _paymentState.value = buildPaymentState(
                    errorMessage = it.message ?: "No se pudo registrar el pago."
                )
            }
        )
    }

    private suspend fun refreshPaymentModeDetails() {
        if (paymentModeDetails.isNotEmpty()) return
        val company = cashboxManager.requireContext().company
        val definitions = runCatching { modeOfPaymentDao.getAllModes(company) }
            .getOrElse { emptyList() }
        paymentModeDetails = buildPaymentModeDetailMap(definitions)
    }

    fun clearPaymentMessages() {
        _paymentState.value = _paymentState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}

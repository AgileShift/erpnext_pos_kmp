package com.erpnext.pos.views.customer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.usecases.CheckCustomerCreditUseCase
import com.erpnext.pos.domain.usecases.CreatePaymentEntryInput
import com.erpnext.pos.domain.usecases.CreatePaymentEntryUseCase
import com.erpnext.pos.domain.usecases.CustomerCreditInput
import com.erpnext.pos.domain.usecases.CustomerQueryInput
import com.erpnext.pos.domain.usecases.FetchCustomerDetailUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersUseCase
import com.erpnext.pos.domain.usecases.FetchOutstandingInvoicesForCustomerUseCase
import com.erpnext.pos.domain.usecases.FetchSalesInvoiceLocalUseCase
import com.erpnext.pos.domain.usecases.RegisterInvoicePaymentInput
import com.erpnext.pos.domain.usecases.RegisterInvoicePaymentUseCase
import com.erpnext.pos.domain.usecases.SyncSalesInvoiceFromRemoteUseCase
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.utils.buildCurrencySpecs
import com.erpnext.pos.utils.buildPaymentEntryDto
import com.erpnext.pos.utils.buildPaymentEntryDtoWithRateResolver
import com.erpnext.pos.utils.buildPaymentModeDetailMap
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.toErpDateTime
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.billing.PaymentLine
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@OptIn(FlowPreview::class)
class CustomerViewModel(
    private val cashboxManager: CashBoxManager,
    private val fetchCustomersUseCase: FetchCustomersUseCase,
    private val checkCustomerCreditUseCase: CheckCustomerCreditUseCase,
    private val fetchCustomerDetailUseCase: FetchCustomerDetailUseCase,
    private val fetchOutstandingInvoicesUseCase: FetchOutstandingInvoicesForCustomerUseCase,
    private val registerInvoicePaymentUseCase: RegisterInvoicePaymentUseCase,
    private val createPaymentEntryUseCase: CreatePaymentEntryUseCase,
    private val fetchSalesInvoiceLocalUseCase: FetchSalesInvoiceLocalUseCase,
    private val syncSalesInvoiceFromRemoteUseCase: SyncSalesInvoiceFromRemoteUseCase,
    private val modeOfPaymentDao: ModeOfPaymentDao
) : BaseViewModel() {

    private val _stateFlow: MutableStateFlow<CustomerState> =
        MutableStateFlow(CustomerState.Loading)
    val stateFlow = _stateFlow

    private val _invoicesState = MutableStateFlow<CustomerInvoicesState>(CustomerInvoicesState.Idle)
    val invoicesState = _invoicesState

    private val _paymentState = MutableStateFlow(CustomerPaymentState())
    val paymentState = _paymentState

    private var paymentModeDetails: Map<String, ModeOfPaymentEntity> = emptyMap()

    private fun buildPaymentState(
        isSubmitting: Boolean = false,
        errorMessage: String? = null,
        successMessage: String? = null
    ): CustomerPaymentState {
        val context = cashboxManager.getContext()
        return CustomerPaymentState(
            isSubmitting = isSubmitting,
            errorMessage = errorMessage,
            successMessage = successMessage,
            baseCurrency = context?.currency ?: "USD",
            allowedCurrencies = context?.allowedCurrencies ?: emptyList(),
            paymentModes = context?.paymentModes ?: emptyList(),
            exchangeRate = context?.exchangeRate ?: 1.0
        )
    }

    private var searchFilter by mutableStateOf<String?>(null)
    private var selectedState by mutableStateOf<String?>(null)
    private val searchFlow = MutableStateFlow<String?>(null)
    private val stateFlowFilter = MutableStateFlow<String?>(null)

    init {
        combine(searchFlow, stateFlowFilter) { q, s -> q to s }
            .debounce(250)
            .onEach { (q, s) -> fetchAllCustomers(q, s) }
            .launchIn(viewModelScope)

        fetchAllCustomers()
    }

    fun fetchAllCustomers(query: String? = null, state: String? = null) {
        _stateFlow.value = CustomerState.Loading

        executeUseCase(
            action = {
                // dejamos respirar a Compose una recomposición
                delay(200)

                fetchCustomersUseCase.invoke(CustomerQueryInput(query, state))
                    .collectLatest { customers ->
                        _stateFlow.value = when {
                            customers.isEmpty() -> CustomerState.Empty
                            else -> CustomerState.Success(
                                customers,
                                customers.size,
                                customers.count { (it.pendingInvoices ?: 0) > 0 }
                            )
                        }
                    }
            },
            exceptionHandler = {
                _stateFlow.value = CustomerState.Error(it.message ?: "Error al cargar clientes")
            }
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

    fun loadOutstandingInvoices(customerId: String) {
        _invoicesState.value = CustomerInvoicesState.Loading
        _paymentState.value = buildPaymentState()

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
        // NOTE: el Screen puede enviar baseAmount estimado, pero el VM recalcula y “cappea” usando PaymentUtils.
        baseAmount: Double
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
        if (baseAmount <= 0) {
            // Permite que el VM siga, porque el cálculo final se basa en enteredAmount + tasa.
            // Pero si te llega baseAmount 0 porque faltó tasa, el Screen debe bloquear el submit.
        }

        _paymentState.value = buildPaymentState(isSubmitting = true)

        executeUseCase(
            action = {
                refreshPaymentModeDetails()

                val invoice = fetchSalesInvoiceLocalUseCase(invoiceId)
                    ?: error("No se pudo cargar la factura local.")

                val context = cashboxManager.requireContext()
                val postingDate = getTimeMillis().toErpDateTime()

                val receivableCurrency = normalizeCurrency(invoice.partyAccountCurrency)
                    ?: normalizeCurrency(invoice.currency)
                    ?: normalizeCurrency(context.currency)
                    ?: "USD"

                val paidFromAccount = invoice.debitTo?.takeIf { it.isNotBlank() }
                    ?: error("No se pudo resolver la cuenta de débito (debit_to).")

                val currencySpecs = buildCurrencySpecs()

                // Para Customer, usamos el resolver ya existente (CashBoxManager), evitando acoplar el VM a APIService.
                val rateResolver: suspend (String?, String?) -> Double? = { from, to ->
                    cashboxManager.resolveExchangeRateBetween(
                        fromCurrency = from ?: "",
                        toCurrency = to ?: ""
                    )
                }

                // Construimos una PaymentLine “genérica” reutilizable (misma pieza que Billing).
                val line = PaymentLine(
                    modeOfPayment = modeOfPayment,
                    currency = normalizeCurrency(enteredCurrency)
                        ?: normalizeCurrency(context.currency) ?: "USD",
                    enteredAmount = enteredAmount,
                    // PaymentUtils no depende de baseAmount para el cálculo final.
                    baseAmount = 0.0,
                    referenceNumber = null,
                    exchangeRate = rateResolver(enteredCurrency, receivableCurrency) ?: 0.0
                )

                // Re-usamos la caché de exchange rates calculada en loadOutstandingInvoices para performance.
                val exchangeRateByCurrency = when (val invState = _invoicesState.value) {
                    is CustomerInvoicesState.Success -> invState.exchangeRateByCurrency
                    else -> emptyMap()
                }

                // Intentamos usar el Customer real (si está disponible) para consistencia.
                val customer = fetchCustomerDetailUseCase.invoke(customerId)
                    ?: CustomerBO(
                        name = customerId,
                        customerName = "",
                        territory = "",
                        currency = receivableCurrency,
                        customerType = "",
                    )

                // Construcción final del PaymentEntry coherente con Billing.
                val entry = buildPaymentEntryDtoWithRateResolver(
                    rateResolver = rateResolver,
                    line = line,
                    context = context,
                    customer = customer,
                    postingDate = postingDate,
                    invoiceId = invoice.invoiceName ?: invoiceId,
                    invoiceTotalRc = invoice.grandTotal,
                    outstandingRc = invoice.outstandingAmount,
                    paidFromAccount = paidFromAccount,
                    partyAccountCurrency = receivableCurrency,
                    exchangeRateByCurrency = exchangeRateByCurrency,
                    currencySpecs = currencySpecs,
                    paymentModeDetails = paymentModeDetails,
                )

                val remoteResult = runCatching {
                    createPaymentEntryUseCase(CreatePaymentEntryInput(entry))
                }

                if (remoteResult.isSuccess) {
                    runCatching { syncSalesInvoiceFromRemoteUseCase(invoiceId) }
                }

                // El monto “real” asignado es el que PaymentUtils calculó y cappeó.
                registerInvoicePaymentUseCase(
                    RegisterInvoicePaymentInput(
                        invoiceId = invoiceId,
                        modeOfPayment = modeOfPayment,
                        amount = entry.paidAmount
                    )
                )

                _paymentState.value = buildPaymentState(
                    successMessage = if (remoteResult.isFailure) {
                        "Pago registrado localmente. No se pudo sincronizar."
                    } else {
                        "Pago registrado correctamente."
                    }
                )
                loadOutstandingInvoices(customerId)
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
}

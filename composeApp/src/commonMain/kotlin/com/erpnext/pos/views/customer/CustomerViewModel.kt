package com.erpnext.pos.views.customer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.usecases.CheckCustomerCreditUseCase
import com.erpnext.pos.domain.usecases.CustomerCreditInput
import com.erpnext.pos.domain.usecases.CustomerQueryInput
import com.erpnext.pos.domain.usecases.FetchCustomerDetailUseCase
import com.erpnext.pos.domain.usecases.FetchCustomersUseCase
import com.erpnext.pos.domain.usecases.FetchOutstandingInvoicesForCustomerUseCase
import com.erpnext.pos.domain.usecases.RegisterInvoicePaymentInput
import com.erpnext.pos.domain.usecases.RegisterInvoicePaymentUseCase
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.FlowPreview
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
    private val registerInvoicePaymentUseCase: RegisterInvoicePaymentUseCase
) : BaseViewModel() {

    private val _stateFlow: MutableStateFlow<CustomerState> =
        MutableStateFlow(CustomerState.Loading)
    val stateFlow = _stateFlow

    private val _invoicesState = MutableStateFlow<CustomerInvoicesState>(CustomerInvoicesState.Idle)
    val invoicesState = _invoicesState

    private val _paymentState = MutableStateFlow(CustomerPaymentState())
    val paymentState = _paymentState

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
        combine(
            searchFlow,
            stateFlowFilter,
        ) { q, s -> q to s }
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
                kotlinx.coroutines.delay(120)

                fetchCustomersUseCase.invoke(CustomerQueryInput(query, state))
                    .collectLatest { customers ->
                        _stateFlow.value = when {
                            customers.isEmpty() -> CustomerState.Empty
                            else -> CustomerState.Success(customers)
                        }
                    }
            },
            exceptionHandler = {
                _stateFlow.value = CustomerState.Error(
                    it.message ?: "Error al cargar clientes"
                )
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

    private fun updateFilteredCustomers() {
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

    fun onRefresh() {
        fetchAllCustomers(searchFilter, selectedState)
    }

    fun loadOutstandingInvoices(customerId: String) {
        _invoicesState.value = CustomerInvoicesState.Loading
        _paymentState.value = buildPaymentState()
        executeUseCase(
            action = {
                val invoices = fetchOutstandingInvoicesUseCase.invoke(customerId)
                val baseCurrency =
                    cashboxManager.getContext()?.currency?.trim()?.uppercase().orEmpty()
                val exchangeRates = mutableMapOf<String, Double>()
                invoices.mapNotNull { it.currency?.trim()?.uppercase() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .forEach { currency ->
                        val resolved = if (baseCurrency.isNotBlank()) {
                            cashboxManager.resolveExchangeRateBetween(
                                fromCurrency = baseCurrency,
                                toCurrency = currency
                            )
                        } else {
                            null
                        }
                        if (resolved != null && resolved > 0.0) {
                            exchangeRates[currency] = resolved
                        }
                    }
                _invoicesState.value = CustomerInvoicesState.Success(
                    invoices = invoices,
                    exchangeRateByCurrency = exchangeRates
                )
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
        amount: Double
    ) {
        if (modeOfPayment.isBlank()) {
            _paymentState.value = buildPaymentState(errorMessage = "Selecciona un modo de pago.")
            return
        }
        if (invoiceId.isBlank()) {
            _paymentState.value = buildPaymentState(errorMessage = "Selecciona una factura.")
            return
        }
        if (amount <= 0) {
            _paymentState.value = buildPaymentState(errorMessage = "Ingresa un monto válido.")
            return
        }

        _paymentState.value = buildPaymentState(isSubmitting = true)
        executeUseCase(
            action = {
                registerInvoicePaymentUseCase(
                    RegisterInvoicePaymentInput(
                        invoiceId = invoiceId,
                        modeOfPayment = modeOfPayment,
                        amount = amount
                    )
                )
                _paymentState.value = buildPaymentState(
                    successMessage = "Pago registrado correctamente.",
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
}

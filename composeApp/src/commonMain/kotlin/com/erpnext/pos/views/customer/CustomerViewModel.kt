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
            exceptionHandler = { onResult(false, it.message ?: "Error") }
        )
    }

    fun onRefresh() {
        fetchAllCustomers(searchFilter, selectedState)
    }

    fun loadOutstandingInvoices(customerId: String) {
        _invoicesState.value = CustomerInvoicesState.Loading
        executeUseCase(
            action = {
                val invoices = fetchOutstandingInvoicesUseCase.invoke(customerId)
                _invoicesState.value = CustomerInvoicesState.Success(invoices)
            },
            exceptionHandler = {
                _invoicesState.value = CustomerInvoicesState.Error(
                    it.message ?: "Unable to load outstanding invoices."
                )
            }
        )
    }

    fun clearOutstandingInvoices() {
        _invoicesState.value = CustomerInvoicesState.Idle
        _paymentState.value = CustomerPaymentState()
    }

    fun registerPayment(
        customerId: String,
        invoiceId: String,
        modeOfPayment: String,
        amount: Double
    ) {
        if (modeOfPayment.isBlank()) {
            _paymentState.value = CustomerPaymentState(
                errorMessage = "Select a mode of payment."
            )
            return
        }
        if (invoiceId.isBlank()) {
            _paymentState.value = CustomerPaymentState(
                errorMessage = "Select an invoice."
            )
            return
        }
        if (amount <= 0) {
            _paymentState.value = CustomerPaymentState(
                errorMessage = "Enter a valid amount."
            )
            return
        }

        _paymentState.value = CustomerPaymentState(isSubmitting = true)
        executeUseCase(
            action = {
                registerInvoicePaymentUseCase(
                    RegisterInvoicePaymentInput(
                        invoiceId = invoiceId,
                        modeOfPayment = modeOfPayment,
                        amount = amount
                    )
                )
                _paymentState.value = CustomerPaymentState(
                    successMessage = "Payment registered successfully."
                )
                loadOutstandingInvoices(customerId)
            },
            exceptionHandler = {
                _paymentState.value = CustomerPaymentState(
                    errorMessage = it.message ?: "Unable to register payment."
                )
            }
        )
    }
}

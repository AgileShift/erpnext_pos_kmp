package com.erpnext.pos.views.paymententry

import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.usecases.RegisterInvoicePaymentInput
import com.erpnext.pos.domain.usecases.RegisterInvoicePaymentUseCase
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class PaymentEntryViewModel(
    private val registerPaymentUseCase: RegisterInvoicePaymentUseCase,
    private val navManager: NavigationManager
) : BaseViewModel() {

    private val _state = MutableStateFlow(PaymentEntryState())
    val state: StateFlow<PaymentEntryState> = _state

    fun setInvoiceId(invoiceId: String?) {
        _state.update { it.copy(invoiceId = invoiceId?.trim().orEmpty()) }
    }

    fun onInvoiceIdChanged(value: String) {
        _state.update { it.copy(invoiceId = value) }
    }

    fun onModeOfPaymentChanged(value: String) {
        _state.update { it.copy(modeOfPayment = value) }
    }

    fun onAmountChanged(value: String) {
        _state.update { it.copy(amount = value) }
    }

    fun onSubmit() {
        val current = _state.value
        val amount = current.amount.toDoubleOrNull()
        if (amount == null) {
            _state.update { it.copy(errorMessage = "Ingresa un monto válido.") }
            return
        }

        _state.update { it.copy(isSubmitting = true, errorMessage = null, successMessage = null) }

        executeUseCase(
            action = {
                registerPaymentUseCase(
                    RegisterInvoicePaymentInput(
                        invoiceId = current.invoiceId,
                        modeOfPayment = current.modeOfPayment,
                        amount = amount
                    )
                )
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        successMessage = "Pago registrado correctamente para la factura ${current.invoiceId}."
                    )
                }
            },
            exceptionHandler = { throwable ->
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = throwable.message
                            ?: "No se pudo registrar el pago para la factura ${current.invoiceId}."
                    )
                }
            },
            loadingMessage = "Registrando pago..."
        )
    }

    fun onBack() {
        navManager.navigateTo(NavRoute.NavigateUp)
    }
}

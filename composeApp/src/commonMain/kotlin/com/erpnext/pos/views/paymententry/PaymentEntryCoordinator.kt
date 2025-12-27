package com.erpnext.pos.views.paymententry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel

class PaymentEntryCoordinator(
    private val viewModel: PaymentEntryViewModel
) {
    val state = viewModel.state

    fun setInvoiceId(invoiceId: String?) = viewModel.setInvoiceId(invoiceId)
    fun onInvoiceIdChanged(value: String) = viewModel.onInvoiceIdChanged(value)
    fun onModeOfPaymentChanged(value: String) = viewModel.onModeOfPaymentChanged(value)
    fun onAmountChanged(value: String) = viewModel.onAmountChanged(value)
    fun onSubmit() = viewModel.onSubmit()
    fun onBack() = viewModel.onBack()
}

@Composable
fun rememberPaymentEntryCoordinator(): PaymentEntryCoordinator {
    val viewModel: PaymentEntryViewModel = koinViewModel()

    return remember(viewModel) {
        PaymentEntryCoordinator(viewModel)
    }
}

package com.erpnext.pos.views.paymententry

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.koin.compose.viewmodel.koinViewModel

class PaymentEntryCoordinator(
    private val viewModel: PaymentEntryViewModel
) {
    val state = viewModel.state

    fun resetFormState() = viewModel.resetFormState()
    fun setEntryType(entryType: PaymentEntryType) = viewModel.setEntryType(entryType)
    fun setInvoiceId(invoiceId: String?) = viewModel.setInvoiceId(invoiceId)
    fun onInvoiceIdChanged(value: String) = viewModel.onInvoiceIdChanged(value)
    fun onModeOfPaymentChanged(value: String) = viewModel.onModeOfPaymentChanged(value)
    fun onTargetModeOfPaymentChanged(value: String) = viewModel.onTargetModeOfPaymentChanged(value)
    fun onSourceAccountChanged(value: String) = viewModel.onSourceAccountChanged(value)
    fun onTargetAccountChanged(value: String) = viewModel.onTargetAccountChanged(value)
    fun onAmountChanged(value: String) = viewModel.onAmountChanged(value)
    fun onConceptChanged(value: String) = viewModel.onConceptChanged(value)
    fun onPartyChanged(value: String) = viewModel.onPartyChanged(value)
    fun onSupplierInvoiceToggled(invoiceName: String) = viewModel.onSupplierInvoiceToggled(invoiceName)
    fun onReferenceNoChanged(value: String) = viewModel.onReferenceNoChanged(value)
    fun onReferenceDateChanged(value: String) = viewModel.onReferenceDateChanged(value)
    fun onNotesChanged(value: String) = viewModel.onNotesChanged(value)
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

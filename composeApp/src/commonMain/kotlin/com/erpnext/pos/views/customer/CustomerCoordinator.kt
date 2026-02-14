package com.erpnext.pos.views.customer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.erpnext.pos.domain.usecases.InvoiceCancellationAction
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import org.koin.compose.viewmodel.koinViewModel

class CustomerCoordinator(
    val viewModel: CustomerViewModel
) {
    val screenStateFlow = viewModel.stateFlow
    val customersPagingFlow = viewModel.customersPagingFlow
    val outstandingInvoicesPagingFlow = viewModel.outstandingInvoicesPagingFlow
    val historyInvoicesPagingFlow = viewModel.historyInvoicesPagingFlow
    val invoicesState = viewModel.invoicesState
    val paymentState = viewModel.paymentState
    val historyState = viewModel.historyState
    val historyMessage = viewModel.historyMessage
    val historyActionBusy = viewModel.historyActionBusy
    val customerMessage = viewModel.customerMessage
    val dialogDataState = viewModel.dialogDataState
    val returnPolicy = viewModel.returnPolicy

    fun fetchAll() = viewModel.fetchAllCustomers()

    fun onSearchQueryChanged(input: String) = viewModel.onSearchQueryChanged(input)

    fun onTerritorySelected(territory: String?) = viewModel.onStateSelected(territory)

    fun checkCredit(customerId: String, amount: Double, onResult: (Boolean, String) -> Unit) =
        viewModel.checkCredit(customerId, amount, onResult)

    fun onRefresh() = viewModel.onRefresh()

    fun loadOutstandingInvoices(customerId: String) = viewModel.loadOutstandingInvoices(customerId)

    fun clearOutstandingInvoices() = viewModel.clearOutstandingInvoices()

    fun registerPayment(
        customerId: String,
        invoiceId: String,
        modeOfPayment: String,
        enteredAmount: Double,
        enteredCurrency: String,
        referenceNumber: String,
    ) = viewModel.registerPayment(
        customerId,
        invoiceId,
        modeOfPayment,
        enteredAmount,
        enteredCurrency,
        referenceNumber
    )

    fun clearPaymentMessages() = viewModel.clearPaymentMessages()
    fun downloadInvoicePdf(invoiceId: String) = viewModel.downloadInvoicePdf(invoiceId)
    fun loadInvoiceHistory(customerId: String) = viewModel.loadInvoiceHistory(customerId)
    fun clearInvoiceHistory() = viewModel.clearInvoiceHistory()
    fun clearInvoiceHistoryMessages() = viewModel.clearHistoryMessage()
    fun clearCustomerMessages() = viewModel.clearCustomerMessage()
    fun createCustomer(input: com.erpnext.pos.domain.usecases.CreateCustomerInput) =
        viewModel.createCustomer(input)
    fun performInvoiceHistoryAction(
        invoiceId: String,
        action: InvoiceCancellationAction,
        reason: String?,
        refundModeOfPayment: String?,
        refundReferenceNo: String?,
        applyRefund: Boolean
    ) = viewModel.performInvoiceHistoryAction(
        invoiceId,
        action,
        reason,
        refundModeOfPayment,
        refundReferenceNo,
        applyRefund
    )

    suspend fun loadInvoiceLocal(invoiceId: String): SalesInvoiceWithItemsAndPayments? =
        viewModel.loadInvoiceLocal(invoiceId)

    fun submitPartialReturn(
        invoiceId: String,
        reason: String?,
        refundModeOfPayment: String?,
        refundReferenceNo: String?,
        applyRefund: Boolean,
        itemsToReturnByCode: Map<String, Double>
    ) = viewModel.submitPartialReturn(
        invoiceId,
        reason,
        refundModeOfPayment,
        refundReferenceNo,
        applyRefund,
        itemsToReturnByCode
    )
}

@Composable
fun rememberCustomerCoordinator(): CustomerCoordinator {
    val viewModel: CustomerViewModel = koinViewModel()

    return remember(viewModel) {
        CustomerCoordinator(
            viewModel = viewModel
        )
    }
}

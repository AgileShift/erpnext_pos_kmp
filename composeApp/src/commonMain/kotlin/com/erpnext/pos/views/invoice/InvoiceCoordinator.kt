package com.erpnext.pos.views.invoice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.paging.PagingData
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.domain.usecases.InvoiceCancellationAction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.viewmodel.koinViewModel

class InvoiceCoordinator(
    private val viewModel: InvoiceViewModel
) {
    fun getInvoices(): Flow<PagingData<SalesInvoiceBO>> = viewModel.invoices

    fun onSearchQueryChanged(customerId: String) = viewModel.onSearchQueryChanged(customerId)
    fun onDateSelected(date: String) = viewModel.onDateSelected(date)
    fun onItemClick(invoiceId: String) = viewModel.onInvoiceSelected(invoiceId)
    fun goToBilling() = viewModel.goToBilling()

    fun onRefresh() {}
    fun onPrint() {}
    fun onInvoiceCancelRequested(
        invoiceId: String,
        action: InvoiceCancellationAction,
        reason: String?
    ) = viewModel.onInvoiceCancelRequested(invoiceId, action, reason)
    fun feedbackMessage(): StateFlow<String?> = viewModel.feedbackMessage
    fun clearFeedbackMessage() = viewModel.clearFeedbackMessage()
}

@Composable
fun rememberInvoiceCoordinator(): InvoiceCoordinator {
    val viewModel: InvoiceViewModel = koinViewModel()

    return remember(viewModel) {
        InvoiceCoordinator(
            viewModel = viewModel
        )
    }
}

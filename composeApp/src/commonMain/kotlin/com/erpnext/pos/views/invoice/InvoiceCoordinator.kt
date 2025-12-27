package com.erpnext.pos.views.invoice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.paging.PagingData
import com.erpnext.pos.domain.models.SalesInvoiceBO
import kotlinx.coroutines.flow.Flow
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

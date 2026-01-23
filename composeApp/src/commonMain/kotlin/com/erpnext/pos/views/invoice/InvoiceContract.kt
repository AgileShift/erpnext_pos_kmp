package com.erpnext.pos.views.invoice

import androidx.paging.PagingData
import com.erpnext.pos.domain.models.SalesInvoiceBO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import com.erpnext.pos.domain.usecases.InvoiceCancellationAction

sealed interface InvoiceState {
    data class Success(val invoices: Flow<PagingData<SalesInvoiceBO>>) : InvoiceState
    data class Error(val message: String) : InvoiceState
}

data class InvoiceAction(
    val getInvoices: () -> Flow<PagingData<SalesInvoiceBO>> = { emptyFlow() },
    val onSearchQueryChanged: (String) -> Unit = {},
    val onDateSelected: (String) -> Unit = { _ -> },
    val onRefresh: () -> Unit = {},
    val onPrint: () -> Unit = {},
    val onItemClick: (String) -> Unit = {},
    val onClearSearch: () -> Unit = {},
    val goToBilling: () -> Unit = {},
    val onInvoiceCancelRequested: (invoiceId: String, action: InvoiceCancellationAction, reason: String?) -> Unit = { _, _, _ -> },
    val feedbackMessage: StateFlow<String?> = MutableStateFlow(null),
    val onFeedbackCleared: () -> Unit = {}
)

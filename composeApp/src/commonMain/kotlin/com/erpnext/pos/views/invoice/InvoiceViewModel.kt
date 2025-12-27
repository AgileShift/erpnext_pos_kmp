package com.erpnext.pos.views.invoice

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.domain.usecases.FetchPendingInvoiceUseCase
import com.erpnext.pos.domain.usecases.PendingInvoiceInput
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class InvoiceViewModel(
    private val fetchPendingInvoiceUseCase: FetchPendingInvoiceUseCase,
    private val navManager: NavigationManager
) : BaseViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val dateFilter = MutableStateFlow<String?>("")

    // Este es el flujo de datos que la UI consumirá
    val invoices: Flow<PagingData<SalesInvoiceBO>> = combine(
        searchQuery.debounce(300),
        dateFilter.debounce(300)
    ) { query, date ->
        PendingInvoiceInput(
            query = query.trim(),
            date = date.takeIf { it?.isNotBlank() == true }
        )
    }
        .flatMapLatest { input ->
            fetchPendingInvoiceUseCase(input)
                .map { paging -> input.applyLocalSearch(paging) }
        }
        .cachedIn(viewModelScope)


    fun onSearchQueryChanged(text: String) {
        searchQuery.value = text
    }

    fun onDateSelected(date: String?) {
        dateFilter.value = date
    }

    fun goToBilling() {
        navManager.navigateTo(NavRoute.Billing)
    }

    fun onInvoiceSelected(invoiceId: String) {
        navManager.navigateTo(NavRoute.PaymentEntry(invoiceId))
    }

    private fun PendingInvoiceInput.applyLocalSearch(
        paging: PagingData<SalesInvoiceBO>
    ): PagingData<SalesInvoiceBO> {
        val q = query?.lowercase()?.trim()
        if (q.isNullOrEmpty()) return paging
        return paging.filter { invoice ->
            invoice.customer?.lowercase()?.contains(q) == true ||
                    invoice.customerPhone?.lowercase()?.contains(q) == true ||
                    invoice.invoiceId.lowercase().contains(q)
        }
    }
}

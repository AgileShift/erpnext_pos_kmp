package com.erpnext.pos.views.invoice

import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.filter
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.domain.usecases.CancelSalesInvoiceInput
import com.erpnext.pos.domain.usecases.CancelSalesInvoiceUseCase
import com.erpnext.pos.domain.usecases.FetchPendingInvoiceUseCase
import com.erpnext.pos.domain.usecases.FetchSalesInvoiceLocalUseCase
import com.erpnext.pos.domain.usecases.InvoiceCancellationAction
import com.erpnext.pos.domain.usecases.PendingInvoiceInput
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.localSource.preferences.ReturnPolicyPreferences
import com.erpnext.pos.utils.parseErpDateTimeToEpochMillis
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class InvoiceViewModel(
    private val fetchPendingInvoiceUseCase: FetchPendingInvoiceUseCase,
    private val cancelSalesInvoiceUseCase: CancelSalesInvoiceUseCase,
    private val fetchSalesInvoiceLocalUseCase: FetchSalesInvoiceLocalUseCase,
    private val returnPolicyPreferences: ReturnPolicyPreferences,
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

    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage


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

    fun onInvoiceCancelRequested(
        invoiceId: String,
        action: InvoiceCancellationAction,
        reason: String?
    ) {
        viewModelScope.launch {
            _feedbackMessage.value = null
            if (action == InvoiceCancellationAction.RETURN) {
                val policy = returnPolicyPreferences.get()
                if (!policy.allowFullReturns) {
                    _feedbackMessage.value = "Los retornos totales están deshabilitados por política."
                    return@launch
                }
                if (policy.requireReason && reason.isNullOrBlank()) {
                    _feedbackMessage.value = "Debes indicar un motivo para el retorno."
                    return@launch
                }
                if (policy.maxDaysAfterInvoice > 0) {
                    val invoice = fetchSalesInvoiceLocalUseCase(invoiceId)
                    val invoiceMillis = parseErpDateTimeToEpochMillis(invoice?.postingDate)
                    val now = Clock.System.now().toEpochMilliseconds()
                    if (invoiceMillis != null) {
                        val days = ((now - invoiceMillis) / (24 * 60 * 60 * 1000.0)).toInt()
                        if (days > policy.maxDaysAfterInvoice) {
                            _feedbackMessage.value =
                                "El retorno excede el límite de ${policy.maxDaysAfterInvoice} días."
                            return@launch
                        }
                    }
                }
            }
            val result = runCatching {
                cancelSalesInvoiceUseCase(
                    CancelSalesInvoiceInput(
                        invoiceName = invoiceId,
                        action = action,
                        reason = reason,
                        applyRefund = false
                    )
                )
            }
            result.onSuccess {
                _feedbackMessage.value = when (action) {
                    InvoiceCancellationAction.CANCEL -> "Factura $invoiceId cancelada."
                    InvoiceCancellationAction.RETURN ->
                        if (!it.creditNoteName.isNullOrBlank()) {
                            "Retorno registrado como ${it.creditNoteName}."
                        } else {
                            "Retorno registrado."
                        }
                }
            }
            result.onFailure {
                _feedbackMessage.value =
                    "No se pudo procesar la acción: ${it.message ?: "error desconocido."}"
            }
        }
    }

    fun clearFeedbackMessage() {
        _feedbackMessage.value = null
    }

    private fun PendingInvoiceInput.applyLocalSearch(
        paging: PagingData<SalesInvoiceBO>
    ): PagingData<SalesInvoiceBO> {
        val q = query?.lowercase()?.trim()
        return paging.filter { invoice ->
            val queryMatches = if (q.isNullOrEmpty()) {
                true
            } else {
                invoice.customer?.lowercase()?.contains(q) == true ||
                    invoice.customerPhone?.lowercase()?.contains(q) == true ||
                    invoice.invoiceId.lowercase().contains(q)
            }
            val dateValue = date?.trim()
            val dateMatches = if (dateValue.isNullOrEmpty()) {
                true
            } else {
                invoice.postingDate?.startsWith(dateValue) == true
            }
            queryMatches && dateMatches
        }
    }
}

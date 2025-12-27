package com.erpnext.pos.views.quotation

import com.erpnext.pos.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class QuotationViewModel : BaseViewModel() {
    private val _stateFlow = MutableStateFlow<QuotationState>(QuotationState.Ready)
    val stateFlow: StateFlow<QuotationState> = _stateFlow

    fun onRefresh() {
        _stateFlow.value = QuotationState.Ready
    }
}

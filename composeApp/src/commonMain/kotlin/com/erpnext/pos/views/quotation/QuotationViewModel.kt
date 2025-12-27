package com.erpnext.pos.views.quotation

import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class QuotationViewModel(
    private val navManager: NavigationManager
) : BaseViewModel() {
    private val _stateFlow = MutableStateFlow<QuotationState>(QuotationState.Ready)
    val stateFlow: StateFlow<QuotationState> = _stateFlow

    fun onRefresh() {
        _stateFlow.value = QuotationState.Ready
    }

    fun onBack() {
        navManager.navigateTo(NavRoute.NavigateUp)
    }
}

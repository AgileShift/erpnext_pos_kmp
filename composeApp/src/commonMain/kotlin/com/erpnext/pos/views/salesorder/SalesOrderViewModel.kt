package com.erpnext.pos.views.salesorder

import com.erpnext.pos.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SalesOrderViewModel : BaseViewModel() {
    private val _stateFlow = MutableStateFlow<SalesOrderState>(SalesOrderState.Ready)
    val stateFlow: StateFlow<SalesOrderState> = _stateFlow

    fun onRefresh() {
        _stateFlow.value = SalesOrderState.Ready
    }
}

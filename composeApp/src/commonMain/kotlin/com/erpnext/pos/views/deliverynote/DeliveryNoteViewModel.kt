package com.erpnext.pos.views.deliverynote

import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeliveryNoteViewModel(
    private val navManager: NavigationManager
) : BaseViewModel() {
    private val _stateFlow = MutableStateFlow<DeliveryNoteState>(DeliveryNoteState.Ready)
    val stateFlow: StateFlow<DeliveryNoteState> = _stateFlow

    fun onRefresh() {
        _stateFlow.value = DeliveryNoteState.Ready
    }

    fun onBack() {
        navManager.navigateTo(NavRoute.NavigateUp)
    }
}

package com.erpnext.pos.views.deliverynote

import com.erpnext.pos.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DeliveryNoteViewModel : BaseViewModel() {
    private val _stateFlow = MutableStateFlow<DeliveryNoteState>(DeliveryNoteState.Ready)
    val stateFlow: StateFlow<DeliveryNoteState> = _stateFlow

    fun onRefresh() {
        _stateFlow.value = DeliveryNoteState.Ready
    }
}

package com.erpnext.pos.views.deliverynote

sealed class DeliveryNoteState {
    data object Loading : DeliveryNoteState()
    data object Ready : DeliveryNoteState()
    data class Error(val message: String) : DeliveryNoteState()
}

data class DeliveryNoteAction(
    val onBack: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onCreateInvoice: (String?) -> Unit = {}
)

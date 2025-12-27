package com.erpnext.pos.views.quotation

sealed class QuotationState {
    data object Loading : QuotationState()
    data object Ready : QuotationState()
    data class Error(val message: String) : QuotationState()
}

data class QuotationAction(
    val onBack: () -> Unit = {},
    val onRefresh: () -> Unit = {}
)

package com.erpnext.pos.views.customer

import com.erpnext.pos.domain.models.CustomerBO

sealed class CustomerState {
    object Loading : CustomerState()
    object Empty : CustomerState()
    data class Success(val customers: List<CustomerBO>) : CustomerState()

    data class Error(val message: String) : CustomerState()
}

data class CustomerAction(
    val onSearchQueryChanged: (String) -> Unit = {},
    val onStateSelected: (String?) -> Unit = {},
    val onRefresh: () -> Unit = {},
    val checkCredit: (String, Double, (Boolean, String) -> Unit) -> Unit = { _, _, _ ->  },
    val fetchAll: () -> Unit = {},
    val toDetails: (String) -> Unit = {},
    val onViewPendingInvoices: (CustomerBO) -> Unit = {},
    val onCreateQuotation: (CustomerBO) -> Unit = {},
    val onCreateSalesOrder: (CustomerBO) -> Unit = {},
    val onCreateDeliveryNote: (CustomerBO) -> Unit = {},
    val onCreateInvoice: (CustomerBO) -> Unit = {},
    val onRegisterPayment: (CustomerBO) -> Unit = {}
)

package com.erpnext.pos.views.customer

import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.SalesInvoiceBO

sealed class CustomerState {
    object Loading : CustomerState()
    object Empty : CustomerState()
    data class Success(val customers: List<CustomerBO>) : CustomerState()

    data class Error(val message: String) : CustomerState()
}

sealed class CustomerInvoicesState {
    data object Idle : CustomerInvoicesState()
    data object Loading : CustomerInvoicesState()
    data class Success(val invoices: List<SalesInvoiceBO>) : CustomerInvoicesState()
    data class Error(val message: String) : CustomerInvoicesState()
}

data class CustomerPaymentState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

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
    val onRegisterPayment: (CustomerBO) -> Unit = {},
    val loadOutstandingInvoices: (CustomerBO) -> Unit = {},
    val clearOutstandingInvoices: () -> Unit = {},
    val registerPayment: (customerId: String, invoiceId: String, modeOfPayment: String, amount: Double) -> Unit =
        { _, _, _, _ -> }
)

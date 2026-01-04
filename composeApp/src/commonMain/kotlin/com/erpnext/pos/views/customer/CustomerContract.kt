package com.erpnext.pos.views.customer

import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.POSCurrencyOption
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.models.SalesInvoiceBO

sealed class CustomerState {
    object Loading : CustomerState()
    object Empty : CustomerState()
    data class Success(val customers: List<CustomerBO>, val totalCount: Int, val pendingCount: Int) : CustomerState()

    data class Error(val message: String) : CustomerState()
}

sealed class CustomerInvoicesState {
    data object Idle : CustomerInvoicesState()
    data object Loading : CustomerInvoicesState()
    data class Success(
        val invoices: List<SalesInvoiceBO>,
        val exchangeRateByCurrency: Map<String, Double> = emptyMap()
    ) : CustomerInvoicesState()

    data class Error(val message: String) : CustomerInvoicesState()
}

data class CustomerPaymentState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val baseCurrency: String = "USD",
    val allowedCurrencies: List<POSCurrencyOption> = emptyList(),
    val paymentModes: List<POSPaymentModeOption> = emptyList(),
    val exchangeRate: Double = 1.0
)

data class CustomerAction(
    val onSearchQueryChanged: (String) -> Unit = {},
    val onStateSelected: (String?) -> Unit = {},
    val onRefresh: () -> Unit = {},
    val checkCredit: (String, Double, (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
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
    val registerPayment: (
        customerId: String,
        invoiceId: String,
        modeOfPayment: String,
        enteredAmount: Double,
        enteredCurrency: String,
        baseAmount: Double
    ) -> Unit = { _, _, _, _, _, _ -> }
)

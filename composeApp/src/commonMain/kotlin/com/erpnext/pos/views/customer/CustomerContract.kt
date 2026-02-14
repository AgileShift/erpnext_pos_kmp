package com.erpnext.pos.views.customer

import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.POSCurrencyOption
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.domain.models.CustomerGroupBO
import com.erpnext.pos.domain.models.TerritoryBO
import com.erpnext.pos.domain.models.PaymentTermBO
import com.erpnext.pos.domain.usecases.InvoiceCancellationAction
import com.erpnext.pos.domain.usecases.CreateCustomerInput
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments

sealed class CustomerState {
    object Loading : CustomerState()
    object Empty : CustomerState()
    data class Success(val totalCount: Int, val pendingCount: Int) : CustomerState()

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

sealed class CustomerInvoiceHistoryState {
    object Idle : CustomerInvoiceHistoryState()
    object Loading : CustomerInvoiceHistoryState()
    data class Success(val invoices: List<SalesInvoiceBO>) : CustomerInvoiceHistoryState()
    data class Error(val message: String) : CustomerInvoiceHistoryState()
}

data class CustomerPaymentState(
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val baseCurrency: String = "USD",
    val partyAccountCurrency: String = "USD",
    val allowedCurrencies: List<POSCurrencyOption> = emptyList(),
    val paymentModes: List<POSPaymentModeOption> = emptyList(),
    val exchangeRate: Double = 1.0,
    val modeTypes: Map<String, ModeOfPaymentEntity>? = mapOf(),
    val paymentModeCurrencyByMode: Map<String, String>? = mapOf()
)

data class CustomerDialogDataState(
    val customerGroups: List<CustomerGroupBO> = emptyList(),
    val territories: List<TerritoryBO> = emptyList(),
    val paymentTerms: List<PaymentTermBO> = emptyList(),
    val companies: List<com.erpnext.pos.domain.models.CompanyBO> = emptyList()
)

data class CustomerAction(
    val onSearchQueryChanged: (String) -> Unit = {},
    val onStateSelected: (String?) -> Unit = {},
    val onRefresh: () -> Unit = {},
    val checkCredit: (String, Double, (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
    val fetchAll: () -> Unit = {},
    val onViewPendingInvoices: (CustomerBO) -> Unit = {},
    val onViewInvoiceHistory: (CustomerBO) -> Unit = {},
    val onCreateCustomer: (CreateCustomerInput) -> Unit = {},
    val onCreateQuotation: (CustomerBO) -> Unit = {},
    val onCreateSalesOrder: (CustomerBO) -> Unit = {},
    val onCreateDeliveryNote: (CustomerBO) -> Unit = {},
    val onCreateInvoice: (CustomerBO) -> Unit = {},
    val onRegisterPayment: (CustomerBO) -> Unit = {},
    val onDownloadInvoicePdf: (String) -> Unit = {},
    val loadOutstandingInvoices: (CustomerBO) -> Unit = {},
    val clearOutstandingInvoices: () -> Unit = {},
    val clearPaymentMessages: () -> Unit = {},
    val clearInvoiceHistory: () -> Unit = {},
    val clearInvoiceHistoryMessages: () -> Unit = {},
    val clearCustomerMessages: () -> Unit = {},

    val onInvoiceHistoryAction: (
        invoiceId: String,
        action: InvoiceCancellationAction,
        reason: String?,
        refundModeOfPayment: String?,
        refundReferenceNo: String?,
        applyRefund: Boolean
    ) -> Unit = { _, _, _, _, _, _ -> },

    val registerPayment: (
        customerId: String,
        invoiceId: String,
        modeOfPayment: String,
        enteredAmount: Double,
        enteredCurrency: String,
        referenceNumber: String,
    ) -> Unit = { _, _, _, _, _, _ -> },

    // --- NUEVO: para retorno parcial offline-first (leer local) ---
    val loadInvoiceLocal: suspend (invoiceId: String) -> SalesInvoiceWithItemsAndPayments? = { _ -> null },

    // --- NUEVO: submit retorno parcial (ViewModel lo implementa) ---
    val onInvoicePartialReturn: (
        invoiceId: String,
        reason: String?,
        refundModeOfPayment: String?,
        refundReferenceNo: String?,
        applyRefund: Boolean,
        itemsToReturnByCode: Map<String, Double>
    ) -> Unit = { _, _, _, _, _, _ -> }
)

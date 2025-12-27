package com.erpnext.pos.views.paymententry

data class PaymentEntryState(
    val invoiceId: String = "",
    val modeOfPayment: String = "",
    val amount: String = "",
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class PaymentEntryAction(
    val onInvoiceIdChanged: (String) -> Unit = {},
    val onModeOfPaymentChanged: (String) -> Unit = {},
    val onAmountChanged: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onBack: () -> Unit = {}
)

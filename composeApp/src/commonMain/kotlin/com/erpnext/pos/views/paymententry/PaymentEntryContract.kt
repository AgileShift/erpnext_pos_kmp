package com.erpnext.pos.views.paymententry

enum class PaymentEntryType(val value: String) {
    Receive("receive"),
    Pay("pay"),
    InternalTransfer("internal-transfer");

    companion object {
        fun from(value: String?): PaymentEntryType {
            return when (value) {
                "invoice-payment" -> Receive
                else -> entries.firstOrNull { it.value == value } ?: Receive
            }
        }
    }
}

data class PaymentEntryState(
    val entryType: PaymentEntryType = PaymentEntryType.Receive,
    val invoiceId: String = "",
    val modeOfPayment: String = "",
    val targetModeOfPayment: String = "",
    val sourceAccount: String = "",
    val targetAccount: String = "",
    val currencyCode: String = "USD",
    val amount: String = "",
    val concept: String = "",
    val party: String = "",
    val referenceNo: String = "",
    val referenceDate: String = "",
    val notes: String = "",
    val expenseAccount: String = "",
    val defaultReceivableAccount: String = "",
    val availableModes: List<String> = emptyList(),
    val accountOptions: List<String> = emptyList(),
    val partyOptions: List<String> = emptyList(),
    val isOnline: Boolean = true,
    val offlineModeEnabled: Boolean = false,
    val isSubmitting: Boolean = false,
    val referenceNoError: String? = null,
    val referenceDateError: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

data class PaymentEntryAction(
    val onInvoiceIdChanged: (String) -> Unit = {},
    val onModeOfPaymentChanged: (String) -> Unit = {},
    val onTargetModeOfPaymentChanged: (String) -> Unit = {},
    val onSourceAccountChanged: (String) -> Unit = {},
    val onTargetAccountChanged: (String) -> Unit = {},
    val onAmountChanged: (String) -> Unit = {},
    val onConceptChanged: (String) -> Unit = {},
    val onPartyChanged: (String) -> Unit = {},
    val onReferenceNoChanged: (String) -> Unit = {},
    val onReferenceDateChanged: (String) -> Unit = {},
    val onNotesChanged: (String) -> Unit = {},
    val onSubmit: () -> Unit = {},
    val onBack: () -> Unit = {}
)

package com.erpnext.pos.views.reconciliation

data class OpeningBalanceDetailUi(
    val modeOfPayment: String,
    val openingAmount: Double
)

sealed class ReconciliationState {
    object Loading : ReconciliationState()
    object Empty : ReconciliationState()
    data class Success(val summary: ReconciliationSummaryUi) : ReconciliationState()
    data class Error(val message: String) : ReconciliationState()
}

data class ReconciliationSummaryUi(
    val posProfile: String,
    val openingEntryId: String,
    val cashierName: String,
    val periodStart: String,
    val periodEnd: String?,
    val openingAmount: Double,
    val openingDetails: List<OpeningBalanceDetailUi>,
    val openingByMode: Map<String, Double>,
    val paymentsByMode: Map<String, Double>,
    val expectedByMode: Map<String, Double>,
    val cashModes: Set<String>,
    val salesTotal: Double,
    val paymentsTotal: Double,
    val expensesTotal: Double,
    val expectedTotal: Double,
    val pendingSubmitCount: Int = 0,
    val currency: String,
    val currencySymbol: String?,
    val invoiceCount: Int,
    val cashByCurrency: Map<String, Double> = emptyMap(),
    val openingCashByCurrency: Map<String, Double> = emptyMap(),
    val paymentsByCurrency: Map<String, Double> = emptyMap(),
    val salesByCurrency: Map<String, Double> = emptyMap(),
    val nonCashPaymentsByCurrency: Map<String, Double> = emptyMap(),
    // Total de créditos con pagos parciales, en moneda del POS.
    val creditPartialTotal: Double = 0.0,
    // Total de créditos pendientes, en moneda del POS.
    val creditPendingTotal: Double = 0.0,
    // Desglose por moneda de créditos con pagos parciales.
    val creditPartialByCurrency: Map<String, Double> = emptyMap(),
    // Desglose por moneda de créditos pendientes.
    val creditPendingByCurrency: Map<String, Double> = emptyMap(),
    val expensesByCurrency: Map<String, Double> = emptyMap(),
    val cashCurrencies: List<String> = emptyList(),
    val cashModeCurrency: Map<String, String> = emptyMap()
)

const val UNASSIGNED_PAYMENT_MODE = "__UNASSIGNED__"

enum class ReconciliationMode(val value: String) {
    Review("review"),
    Close("close");

    companion object {
        fun from(value: String?): ReconciliationMode {
            return entries.firstOrNull { it.value == value } ?: Review
        }
    }
}

data class CloseCashboxState(
    val isClosing: Boolean = false,
    val isClosed: Boolean = false,
    val errorMessage: String? = null
)

data class ReconciliationAction(
    val onBack: () -> Unit = {},
    val onConfirmClose: (Map<String, Double>) -> Unit = {},
    val onSaveDraft: () -> Unit = {},
    val onReload: () -> Unit = {}
)

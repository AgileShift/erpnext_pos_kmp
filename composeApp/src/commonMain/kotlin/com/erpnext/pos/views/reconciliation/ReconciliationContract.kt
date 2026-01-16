package com.erpnext.pos.views.reconciliation

data class ReconciliationSessionUi(
    val id: String,
    val posProfile: String,
    val openingEntry: String,
    val periodStart: String,
    val periodEnd: String,
    val closingAmount: Double,
    val pendingSync: Boolean
)

sealed class ReconciliationState {
    object Loading : ReconciliationState()
    object Empty : ReconciliationState()
    data class Success(val sessions: List<ReconciliationSessionUi>) : ReconciliationState()
    data class Error(val message: String) : ReconciliationState()
}

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
    val onConfirmClose: () -> Unit = {}
)

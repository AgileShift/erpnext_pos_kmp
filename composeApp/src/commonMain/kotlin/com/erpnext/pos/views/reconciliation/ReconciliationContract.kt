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

data class ReconciliationAction(
    val onBack: () -> Unit = {}
)

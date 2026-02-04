package com.erpnext.pos.sync

// 1. Definimos los estados posibles de la sincronización
sealed class SyncState {
    data object IDLE : SyncState()
    data class SYNCING(
        val message: String,
        val currentStep: Int? = null,
        val totalSteps: Int? = null
    ) : SyncState() {
        val progress: Float?
            get() = if (currentStep != null && totalSteps != null && totalSteps > 0) {
                currentStep.toFloat() / totalSteps.toFloat()
            } else {
                null
            }
    }
    data object SUCCESS : SyncState()
    data class ERROR(val message: String) : SyncState()
}

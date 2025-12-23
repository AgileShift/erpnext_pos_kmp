package com.erpnext.pos.sync

// 1. Definimos los estados posibles de la sincronización
sealed class SyncState {
    data object IDLE : SyncState()
    data class SYNCING(val message: String) : SyncState()
    data object SUCCESS : SyncState()
    data class ERROR(val message: String) : SyncState()
}
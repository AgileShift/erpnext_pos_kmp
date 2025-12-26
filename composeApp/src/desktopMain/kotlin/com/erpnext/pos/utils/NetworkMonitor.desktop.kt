package com.erpnext.pos.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

actual class NetworkMonitor {
    actual val isConnected: Flow<Boolean>
        get() = flowOf(true)
}
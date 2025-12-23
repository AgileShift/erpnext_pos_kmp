package com.erpnext.pos.utils

import kotlinx.coroutines.flow.Flow

expect class NetworkMonitor {
    val isConnected: Flow<Boolean>
}
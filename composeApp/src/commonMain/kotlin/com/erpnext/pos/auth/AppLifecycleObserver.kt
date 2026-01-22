package com.erpnext.pos.auth

import kotlinx.coroutines.flow.Flow

expect class AppLifecycleObserver() {
    val onResume: Flow<Unit>
}

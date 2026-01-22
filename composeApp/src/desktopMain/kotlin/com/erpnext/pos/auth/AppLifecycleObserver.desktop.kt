package com.erpnext.pos.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

actual class AppLifecycleObserver actual constructor() {
    private val events = MutableSharedFlow<Unit>(replay = 1)
    actual val onResume: Flow<Unit> = events.asSharedFlow()

    init {
        events.tryEmit(Unit)
    }
}

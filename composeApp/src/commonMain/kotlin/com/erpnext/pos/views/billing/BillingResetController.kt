package com.erpnext.pos.views.billing

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class BillingResetController {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun reset() {
        _events.tryEmit(Unit)
    }
}

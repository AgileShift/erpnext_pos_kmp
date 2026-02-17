package com.erpnext.pos.views.inventory

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class InventoryRefreshController {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun refresh() {
        _events.tryEmit(Unit)
    }
}

package com.erpnext.pos.utils.loading

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger

/**
 * Indicador de carga global muy simple basado en un contador de operaciones en vuelo.
 * No es suspendiente para evitar deadlocks; BaseViewModel lo invoca en try/finally.
 */
object LoadingIndicator {
    private val counter = AtomicInteger(0)
    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    fun start() {
        val now = counter.incrementAndGet()
        if (now > 0) _isLoading.value = true
    }

    fun stop() {
        val now = counter.updateAndGet { curr -> (curr - 1).coerceAtLeast(0) }
        _isLoading.value = now > 0
    }

    fun reset() {
        counter.set(0)
        _isLoading.value = false
    }
}

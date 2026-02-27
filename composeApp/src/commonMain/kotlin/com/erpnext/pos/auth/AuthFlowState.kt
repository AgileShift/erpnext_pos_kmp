package com.erpnext.pos.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Global guard for auth flows (OAuth). Prevents background session invalidation
 * while an interactive login is in progress.
 */
class AuthFlowState {
    private val _inProgress = MutableStateFlow(false)
    val inProgress: StateFlow<Boolean> = _inProgress

    fun begin() {
        _inProgress.value = true
    }

    fun end() {
        _inProgress.value = false
    }
}

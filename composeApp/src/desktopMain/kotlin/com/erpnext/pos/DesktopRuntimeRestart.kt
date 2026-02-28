package com.erpnext.pos

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object DesktopRuntimeRestart {
    private val _restartToken = MutableStateFlow(0)
    val restartToken = _restartToken.asStateFlow()

    fun requestRestart() {
        _restartToken.update { it + 1 }
    }
}

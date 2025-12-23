package com.erpnext.pos.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class NavigationManager(private val coroutineScope: CoroutineScope) {
    private val _navigationEvents = MutableSharedFlow<NavRoute>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    fun navigateTo(event: NavRoute) {
        coroutineScope.launch {
            _navigationEvents.emit(event)
        }
    }
}
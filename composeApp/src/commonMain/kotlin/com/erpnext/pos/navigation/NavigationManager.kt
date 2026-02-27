package com.erpnext.pos.navigation

import com.erpnext.pos.utils.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class NavigationManager(private val coroutineScope: CoroutineScope) {
    private val _navigationEvents = MutableSharedFlow<NavRoute>(
        replay = 1,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val navigationEvents = _navigationEvents.asSharedFlow()

    fun navigateTo(event: NavRoute) {
        AppLogger.info("NavigationManager.navigateTo -> enqueue ${event::class.simpleName}")
        if (_navigationEvents.tryEmit(event)) {
            AppLogger.info("NavigationManager.navigateTo -> emit done ${event::class.simpleName}")
            return
        }
        coroutineScope.launch {
            AppLogger.info("NavigationManager.navigateTo -> emit start ${event::class.simpleName}")
            _navigationEvents.emit(event)
            AppLogger.info("NavigationManager.navigateTo -> emit done ${event::class.simpleName}")
        }
    }
}

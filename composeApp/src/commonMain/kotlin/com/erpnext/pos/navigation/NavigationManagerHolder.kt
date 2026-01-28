package com.erpnext.pos.navigation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

object NavigationManagerHolder {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val instance: NavigationManager = NavigationManager(scope)
}

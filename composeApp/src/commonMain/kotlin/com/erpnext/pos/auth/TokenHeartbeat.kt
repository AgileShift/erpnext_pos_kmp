package com.erpnext.pos.auth

import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.utils.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Ejecuta un chequeo periódico de sesión para refrescar tokens antes de que expiren.
 * Respeta el modo offline: si no hay conexión, no intenta refrescar y deja que la app siga offline.
 */
class TokenHeartbeat(
    private val scope: CoroutineScope,
    private val sessionRefresher: SessionRefresher,
    private val networkMonitor: NetworkMonitor,
    private val generalPreferences: GeneralPreferences,
    private val lifecycleObserver: AppLifecycleObserver
) {
    fun start(intervalMinutes: Long = 5) {
        scope.launch {
            val offlineMode = generalPreferences.getOfflineMode()
            val isOnline = networkMonitor.isConnected.first()
            if (isOnline && !offlineMode) {
                sessionRefresher.ensureValidSession()
            }
            while (true) {
                delay(intervalMinutes * 60 * 1000)
                val offlineModeTick = generalPreferences.getOfflineMode()
                val currentlyOnline = networkMonitor.isConnected.first()
                if (currentlyOnline && !offlineModeTick) {
                    sessionRefresher.ensureValidSession()
                }
            }
        }

        scope.launch {
            lifecycleObserver.onResume.collectLatest {
                val offlineMode = generalPreferences.getOfflineMode()
                val currentlyOnline = networkMonitor.isConnected.first()
                if (currentlyOnline && !offlineMode) {
                    sessionRefresher.ensureValidSession()
                }
            }
        }
    }
}

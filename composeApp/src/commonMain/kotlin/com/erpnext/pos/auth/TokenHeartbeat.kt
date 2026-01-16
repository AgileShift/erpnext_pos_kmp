package com.erpnext.pos.auth

import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.remoteSource.oauth.TokenStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

/**
 * Ejecuta un chequeo periódico de sesión para refrescar tokens antes de que expiren.
 * Respeta el modo offline: si no hay conexión, no intenta refrescar y deja que la app siga offline.
 */
class TokenHeartbeat(
    private val scope: CoroutineScope,
    private val sessionRefresher: SessionRefresher,
    private val networkMonitor: NetworkMonitor,
    private val tokenStore: TokenStore
) {
    private var job: Job? = null

    fun start(intervalMinutes: Long = 5) {
        if (job != null) return
        job = scope.launch {
            while (true) {
                delay(intervalMinutes * 60 * 1000)
                val tokens = tokenStore.load()
                if (tokens == null) {
                    continue
                }
                val isOnline = networkMonitor.isConnected.first()
                if (isOnline) {
                    sessionRefresher.ensureValidSession()
                }
            }
        }
    }
}

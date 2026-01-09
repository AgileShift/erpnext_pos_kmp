package com.erpnext.pos.auth

import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.TokenUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SessionRefresher(
    private val tokenStore: TokenStore,
    private val apiService: APIService,
    private val navigationManager: NavigationManager,
    private val networkMonitor: NetworkMonitor
) {
    private val mutex = Mutex()
    private val refreshThresholdSeconds = 5 * 60L // 5 minutos

    suspend fun ensureValidSession(): Boolean = mutex.withLock {
        val isOnline = networkMonitor.isConnected.first()
        val tokens = tokenStore.load() ?: return if (isOnline) invalidateSession() else true

        val secondsLeft = secondsToExpiry(tokens.id_token)
        val isNearExpiry = secondsLeft != null && secondsLeft <= refreshThresholdSeconds

        if (!isOnline) {
            // Offline: permitimos seguir, refrescaremos al reconectar
            return true
        }

        if (TokenUtils.isValid(tokens.id_token) && !isNearExpiry) return true
        val refreshToken = tokens.refresh_token ?: return invalidateSession()

        return try {
            val refreshed = apiService.refreshToken(refreshToken)
            tokenStore.save(refreshed)
            true
        } catch (t: Throwable) {
            AppSentry.capture(t, "SessionRefresher.refresh failed")
            AppLogger.warn("SessionRefresher: refresh failed", t)
            invalidateSession()
        }
    }

    private suspend fun invalidateSession(): Boolean {
        tokenStore.clear()
        navigationManager.navigateTo(NavRoute.Login)
        return false
    }

    private fun secondsToExpiry(idToken: String?): Long? {
        if (idToken == null) return null
        val claims = TokenUtils.decodePayload(idToken) ?: return null
        val exp = claims["exp"]?.toString()?.toLongOrNull() ?: return null
        val now = kotlin.time.Clock.System.now().epochSeconds
        return exp - now
    }
}

package com.erpnext.pos.auth

import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.isRefreshTokenRejected
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.TokenUtils
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SessionRefresher(
    private val tokenStore: TokenStore,
    private val apiService: APIService,
    private val navigationManager: NavigationManager,
    private val networkMonitor: NetworkMonitor,
    private val cashBoxManager: Lazy<Any>? = null,
    private val authFlowState: AuthFlowState? = null
) {
    private val mutex = Mutex()
    private val refreshThresholdSeconds = 30 * 60L // 30 minutos
    private var invalidated = false

    suspend fun ensureValidSession(): Boolean = mutex.withLock {
        AppLogger.info("SessionRefresher.ensureValidSession start")
        if (authFlowState?.inProgress?.value == true) {
            AppLogger.info("SessionRefresher.ensureValidSession skipped (auth flow)")
            return true
        }
        val isOnline = networkMonitor.isConnected.first()
        val tokens = tokenStore.load() ?: return if (isOnline) invalidateSession() else true
        val idToken = tokens.id_token
        if (!idToken.isNullOrBlank() && !apiService.isIdTokenIssuerBoundToCurrentSite(idToken)) {
            val (issuer, site) = apiService.getIdTokenIssuerAndCurrentSite(idToken)
            AppLogger.warn(
                "SessionRefresher: token issuer mismatch, invalidating. issuer=$issuer currentSite=$site"
            )
            return invalidateSession()
        }

        val secondsLeft = secondsToExpiry(idToken)
        val isNearExpiry = secondsLeft != null && secondsLeft <= refreshThresholdSeconds

        if (!isOnline) {
            // Offline: permitimos seguir, refrescaremos al reconectar
            return true
        }

        if (TokenUtils.isValid(idToken) && !isNearExpiry) {
            AppLogger.info("SessionRefresher: token valid, skip refresh")
            invalidated = false
            return true
        }
        if (idToken.isNullOrBlank() && tokens.access_token.isNotBlank()) {
            AppLogger.info("SessionRefresher: id_token missing, using access token session")
            invalidated = false
            return true
        }
        val refreshToken = tokens.refresh_token?.takeIf { it.isNotBlank() }
            ?: return if (TokenUtils.isValid(idToken) ||
                (idToken.isNullOrBlank() && tokens.access_token.isNotBlank())
            ) {
                AppLogger.info("SessionRefresher: no refresh token, keep current session")
                true
            } else {
                invalidateSession()
            }

        return try {
            AppLogger.info(
                "SessionRefresher: refreshing token " +
                        "idTokenExpIn=${secondsLeft ?: -1}s " +
                        "refreshToken=${maskTokenForLogs(refreshToken)}"
            )
            val refreshed = apiService.refreshToken(refreshToken)
            tokenStore.save(refreshed)
            val refreshedIdToken = refreshed.id_token
            if (!refreshedIdToken.isNullOrBlank() &&
                !apiService.isIdTokenIssuerBoundToCurrentSite(refreshedIdToken)
            ) {
                val (issuer, site) = apiService.getIdTokenIssuerAndCurrentSite(refreshedIdToken)
                AppLogger.warn(
                    "SessionRefresher: refreshed token issuer mismatch, invalidating. issuer=$issuer currentSite=$site"
                )
                return invalidateSession()
            }
            invalidated = false
            true
        } catch (t: Throwable) {
            AppSentry.capture(t, "SessionRefresher.refresh failed")
            AppLogger.warn("SessionRefresher: refresh failed", t)
            if (isRefreshTokenRejected(t)) {
                AppLogger.warn("SessionRefresher: refresh rejected by server, invalidating")
                return invalidateSession()
            }
            if (TokenUtils.isValid(idToken)) {
                AppLogger.info("SessionRefresher: refresh failed but token still valid")
                invalidated = false
                true
            } else if (idToken.isNullOrBlank() && tokens.access_token.isNotBlank()) {
                AppLogger.info("SessionRefresher: refresh failed but access token is present")
                invalidated = false
                true
            } else {
                invalidateSession()
            }
        }
    }

    private suspend fun invalidateSession(): Boolean {
        if (invalidated) return false
        if (authFlowState?.inProgress?.value == true) {
            AppLogger.info("SessionRefresher.invalidateSession skipped (auth flow)")
            return false
        }
        invalidated = true
        AppLogger.warn("SessionRefresher: invalidating session -> Login")
        tokenStore.clear()
        (cashBoxManager?.value as? CashBoxManager)?.clearContext()
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

private fun maskTokenForLogs(value: String?): String {
    if (value.isNullOrBlank()) return "<empty>"
    return "len=${value.length} ${value.take(8)}...${value.takeLast(6)}"
}

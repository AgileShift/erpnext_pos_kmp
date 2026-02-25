package com.erpnext.pos.views.login

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.auth.InstanceSwitcher
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.navigation.AuthNavigator
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.TransientAuthStore
import com.erpnext.pos.remoteSource.oauth.buildAuthorizeRequest
import com.erpnext.pos.remoteSource.oauth.toOAuthConfig
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.TokenUtils
import com.erpnext.pos.utils.oauth.OAuthCallbackReceiver
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class LoginViewModel(
    private val authNavigator: AuthNavigator,
    private val oauthService: APIService,
    private val authStore: AuthInfoStore,
    private val navManager: NavigationManager,
    private val contextProvider: CashBoxManager,
    private val instanceSwitcher: InstanceSwitcher,
    private val transientAuthStore: TransientAuthStore,
    private val tokenStore: TokenStore
) : BaseViewModel() {

    private val _stateFlow: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.Loading)
    val stateFlow: StateFlow<LoginState> = _stateFlow.asStateFlow()

    fun doLogin(url: String) {
        AppLogger.info("LoginViewModel.doLogin -> $url")
        authNavigator.openAuthPage(url)
    }

    fun onAuthCodeReceived(code: String) {
        _stateFlow.update { LoginState.Loading }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppLogger.info("LoginViewModel.onAuthCodeReceived start")
                val oAuthConfigBase = authStore.loadAuthInfoByUrl().toOAuthConfig()
                val redirectUri = transientAuthStore.loadRedirectUri()
                val oAuthConfig = if (!redirectUri.isNullOrBlank()) {
                    oAuthConfigBase.copy(redirectUrl = redirectUri)
                } else {
                    oAuthConfigBase
                }
                val authRequest = buildAuthorizeRequest(oAuthConfig)
                AppLogger.info("LoginViewModel.onAuthCodeReceived -> exchanging code")
                val tokens = withTimeout(30_000) {
                    oauthService.exchangeCode(
                        oAuthConfig,
                        code,
                        authRequest.pkce,
                        authRequest.state,
                        authRequest.state
                    )
                }
                if (tokens != null) {
                    AppLogger.info("LoginViewModel.onAuthCodeReceived -> tokens OK")
                    isAuthenticated(tokens)
                } else {
                    AppLogger.warn("LoginViewModel.onAuthCodeReceived -> tokens null")
                    _stateFlow.update { LoginState.Error("Error durante la autenticación") }
                }
                transientAuthStore.clearRedirectUri()
                transientAuthStore.clearPkceVerifier()
                transientAuthStore.clearState()
            } catch (e: Exception) {
                AppLogger.warn("LoginViewModel.onAuthCodeReceived -> error", e)
                _stateFlow.update {
                    val message = if (e is kotlinx.coroutines.TimeoutCancellationException) {
                        "Tiempo de espera al autenticar. Intenta de nuevo."
                    } else {
                        e.message ?: "Error durante la autenticación"
                    }
                    LoginState.Error(message)
                }
                transientAuthStore.clearRedirectUri()
                transientAuthStore.clearPkceVerifier()
                transientAuthStore.clearState()
            }
        }
    }

    fun fetchSites() {
        _stateFlow.update { LoginState.Loading }
        viewModelScope.launch {
            AppLogger.info("LoginViewModel.fetchSites")
            val sites = authStore.loadAuthInfo()
                .map {
                    val displayName = it.name
                        .takeIf { value -> value.isNotBlank() }
                        ?: it.url
                            .removePrefix("https://")
                            .removePrefix("http://")
                            .substringBefore("/")
                            .ifBlank { it.url }
                    Site(it.url, displayName) //, it.lastUsedAt, it.isFavorite)
                }
                .sortedWith(
                    compareByDescending<Site> { it.isFavorite }
                        .thenByDescending { it.lastUsedAt ?: 0L }
                        .thenBy { it.name }
                )
            _stateFlow.update { LoginState.Success(sites) }
        }
    }

    fun onSiteSelected(site: Site) {
        _stateFlow.update { LoginState.Loading }
        viewModelScope.launch {
            val isDesktop = getPlatformName() == "Desktop"
            val receiver = if (isDesktop) OAuthCallbackReceiver() else null
            try {
                AppLogger.info("LoginViewModel.onSiteSelected -> ${site.url}")
                clearCurrentSessionBeforeSwitch()
                val loginInfo = oauthService.getLoginWithSite(site.url)
                authStore.saveAuthInfo(loginInfo)
                val oauthConfig = loginInfo.toOAuthConfig()
                val request = if (isDesktop) {
                    AppLogger.info("LoginViewModel.onSiteSelected -> starting OAuthCallbackReceiver")
                    val redirectUri = runCatching {
                        withTimeout(3000) {
                            receiver?.start(DESKTOP_REDIRECT_URI) ?: DESKTOP_REDIRECT_URI
                        }
                    }.onFailure {
                        AppLogger.warn(
                            "OAuthCallbackReceiver.start failed, using default redirect",
                            it
                        )
                    }.getOrElse { DESKTOP_REDIRECT_URI }
                    transientAuthStore.saveRedirectUri(redirectUri)
                    buildAuthorizeRequest(oauthConfig.copy(redirectUrl = redirectUri))
                } else {
                    buildAuthorizeRequest(oauthConfig)
                }
                AppLogger.info("LoginViewModel.onSiteSelected URL -> ${request.url}")
                doLogin(request.url)
                if (isDesktop) {
                    val code = runCatching {
                        AppLogger.info("LoginViewModel.onSiteSelected -> waiting auth code")
                        withTimeout(120_000) {
                            receiver?.awaitCode(request.state) ?: ""
                        }
                    }.getOrElse {
                        AppLogger.warn("LoginViewModel.onSiteSelected -> auth code timeout", it)
                        ""
                    }
                    if (code.isBlank()) {
                        _stateFlow.update {
                            LoginState.Error("No se recibió el código de autenticación.")
                        }
                    } else {
                        AppLogger.info("LoginViewModel.onSiteSelected -> auth code received")
                        onAuthCodeReceived(code)
                    }
                }
            } catch (e: Exception) {
                AppLogger.warn("LoginViewModel.onSiteSelected -> error", e)
                _stateFlow.update { LoginState.Error(e.message.toString()) }
            } finally {
                receiver?.stop()
            }
        }
    }

    fun onAddSite(url: String) {
        onSiteSelected(Site(url = url.trim(), name = url.trim()))
    }

    fun deleteSite(site: Site) {
        viewModelScope.launch {
            runCatching {
                authStore.deleteSite(site.url)
            }.onFailure { error ->
                AppLogger.warn("LoginViewModel.deleteSite -> error", error)
                _stateFlow.update {
                    LoginState.Error(
                        error.message ?: "No se pudo eliminar la instancia"
                    )
                }
            }.onSuccess {
                fetchSites()
            }
        }
    }

    fun onError(error: String) {
        _stateFlow.update { LoginState.Error(error) }
    }

    fun clear() {
        viewModelScope.launch {
            authStore.clearAuthInfo()
            tokenStore.clear()
        }
    }

    fun toggleFavorite(site: Site) {
        viewModelScope.launch {
            authStore.updateSiteMeta(site.url, isFavorite = !site.isFavorite)
            fetchSites()
        }
    }

    fun reset() = _stateFlow.update { LoginState.Success() }
        .also { fetchSites() }

    fun isAuthenticated(tokens: TokenResponse) {
        val isAuth = tokens.access_token.isNotBlank() &&
                (tokens.id_token.isNullOrBlank() || TokenUtils.isValid(tokens.id_token))
        _stateFlow.update { LoginState.Success() }
        if (isAuth) {
            viewModelScope.launch {
                instanceSwitcher.switchInstance(authStore.getCurrentSite())
                authStore.getCurrentSite()?.let { url ->
                    authStore.updateSiteMeta(
                        url = url,
                        lastUsedAt = Clock.System.now().toEpochMilliseconds()
                    )
                }
                navManager.navigateTo(NavRoute.Home)
            }
        } else {
            _stateFlow.update {
                LoginState.Error("Token inválido: no se pudo completar autenticación.")
            }
        }
    }

    private suspend fun clearCurrentSessionBeforeSwitch() {
        runCatching { tokenStore.clear() }
            .onFailure {
                AppLogger.warn(
                    "LoginViewModel.clearCurrentSessionBeforeSwitch token clear failed",
                    it
                )
            }
        contextProvider.clearContext()
        transientAuthStore.clearRedirectUri()
        transientAuthStore.clearPkceVerifier()
        transientAuthStore.clearState()
    }

    private companion object {
        const val DESKTOP_REDIRECT_URI = "http://127.0.0.1:8070/oauth2redirect"
    }
}

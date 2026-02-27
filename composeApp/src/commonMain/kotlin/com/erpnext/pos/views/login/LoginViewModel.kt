package com.erpnext.pos.views.login

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.auth.InstanceSwitcher
import com.erpnext.pos.auth.AuthFlowState
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.navigation.AuthNavigator
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.Pkce
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.TransientAuthStore
import com.erpnext.pos.remoteSource.oauth.buildAuthorizeRequest
import com.erpnext.pos.remoteSource.oauth.toOAuthConfig
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.TokenUtils
import com.erpnext.pos.utils.oauth.OAuthCallbackReceiver
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private val authFlowState: AuthFlowState,
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
                val pkceVerifier = transientAuthStore.loadPkceVerifier()
                    ?.takeIf { it.isNotBlank() }
                    ?: error("Falta PKCE verifier para completar autenticación.")
                val expectedState = transientAuthStore.loadState()
                    ?.takeIf { it.isNotBlank() }
                    ?: error("Falta estado OAuth para completar autenticación.")
                AppLogger.info(
                    "LoginViewModel.onAuthCodeReceived redirectUri -> ${oAuthConfig.redirectUrl}"
                )
                AppLogger.info("LoginViewModel.onAuthCodeReceived -> exchanging code")
                val tokens = withTimeout(30_000) {
                    oauthService.exchangeCode(
                        oAuthConfig,
                        code,
                        Pkce(verifier = pkceVerifier, challenge = ""),
                        expectedState,
                        expectedState
                    )
                }
                if (tokens != null) {
                    AppLogger.info("LoginViewModel.onAuthCodeReceived -> tokens OK")
                    isAuthenticated(tokens)
                    AppLogger.info("LoginViewModel.onAuthCodeReceived -> dispatched isAuthenticated")
                } else {
                    AppLogger.warn("LoginViewModel.onAuthCodeReceived -> tokens null")
                    _stateFlow.update { LoginState.Error("Error durante la autenticación") }
                    authFlowState.end()
                }
                transientAuthStore.clearRedirectUri()
                transientAuthStore.clearPkceVerifier()
                transientAuthStore.clearState()
                authFlowState.end()
            } catch (e: CancellationException) {
                AppLogger.warn("LoginViewModel.onAuthCodeReceived -> cancelled", e)
                _stateFlow.update { LoginState.Error("Autenticación cancelada.") }
                transientAuthStore.clearRedirectUri()
                transientAuthStore.clearPkceVerifier()
                transientAuthStore.clearState()
                authFlowState.end()
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
                authFlowState.end()
            }
        }
    }

    fun fetchSites() {
        _stateFlow.update { LoginState.Loading }
        viewModelScope.launch {
            AppLogger.info("LoginViewModel.fetchSites")
            val sites = authStore.loadAuthInfo()
                .map {
                    Site(it.url, it.company) //, it.lastUsedAt, it.isFavorite)
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
            var handedOffToCodeExchange = false
            try {
                authFlowState.begin()
                AppLogger.info("LoginViewModel.onSiteSelected -> ${site.url}")
                clearCurrentSessionBeforeSwitch()
                val loginInfo = oauthService.getLoginWithSite(site.url)
                authStore.saveAuthInfo(loginInfo)
                val oauthConfig = loginInfo.toOAuthConfig()
                AppLogger.info(
                    "LoginViewModel.onSiteSelected redirectUri -> ${oauthConfig.redirectUrl}"
                )
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
                transientAuthStore.savePkceVerifier(request.pkce.verifier)
                transientAuthStore.saveState(request.state)
                if (!isDesktop) {
                    transientAuthStore.saveRedirectUri(oauthConfig.redirectUrl)
                }
                AppLogger.info("LoginViewModel.onSiteSelected URL -> ${request.url}")
                doLogin(request.url)
                if (isDesktop) {
                    val code = runCatching {
                        AppLogger.info("LoginViewModel.onSiteSelected -> waiting auth code")
                        withTimeout(120_000) {
                            receiver?.awaitCode(request.state) ?: ""
                        }
                    }.getOrElse { error ->
                        if (error is CancellationException) throw error
                        AppLogger.warn("LoginViewModel.onSiteSelected -> auth code wait failed", error)
                        ""
                    }
                    if (code.isBlank()) {
                        _stateFlow.update {
                            LoginState.Error(
                                "Autenticación cancelada o expirada. Intenta de nuevo."
                            )
                        }
                    } else {
                        AppLogger.info("LoginViewModel.onSiteSelected -> auth code received")
                        handedOffToCodeExchange = true
                        onAuthCodeReceived(code)
                    }
                }
            } catch (e: CancellationException) {
                AppLogger.warn("LoginViewModel.onSiteSelected -> cancelled", e)
                _stateFlow.update { LoginState.Error("Inicio de sesión cancelado.") }
            } catch (e: Exception) {
                AppLogger.warn("LoginViewModel.onSiteSelected -> error", e)
                _stateFlow.update { LoginState.Error(e.message.toString()) }
            } finally {
                if (!handedOffToCodeExchange) {
                    transientAuthStore.clearRedirectUri()
                    transientAuthStore.clearPkceVerifier()
                    transientAuthStore.clearState()
                    authFlowState.end()
                }
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
        if (!isAuth) {
            _stateFlow.update {
                LoginState.Error("Token inválido: no se pudo completar autenticación.")
            }
            authFlowState.end()
            return
        }

        _stateFlow.update { LoginState.Loading }
        viewModelScope.launch {
            AppLogger.info("LoginViewModel.isAuthenticated -> start")
            runCatching {
                AppLogger.info("LoginViewModel.isAuthenticated -> switchInstance start")
                withTimeout(15_000) {
                    withContext(Dispatchers.Default) {
                        instanceSwitcher.switchInstance(authStore.getCurrentSite())
                    }
                }
                AppLogger.info("LoginViewModel.isAuthenticated -> switchInstance done")
            }.onFailure { error ->
                AppLogger.warn("LoginViewModel.isAuthenticated -> switchInstance failed", error)
                _stateFlow.update {
                    val message = if (error is kotlinx.coroutines.TimeoutCancellationException) {
                        "Cambio de instancia agotó el tiempo de espera. Intenta de nuevo."
                    } else {
                        error.message ?: "No se pudo cambiar de instancia."
                    }
                    LoginState.Error(message)
                }
                authFlowState.end()
                return@launch
            }

            AppLogger.info("LoginViewModel.isAuthenticated -> navigating Home")
            navManager.navigateTo(NavRoute.Home)

            authStore.getCurrentSite()?.let { url ->
                runCatching {
                    AppLogger.info("LoginViewModel.isAuthenticated -> updateSiteMeta start url=$url")
                    withTimeout(3_000) {
                        authStore.updateSiteMeta(
                            url = url,
                            lastUsedAt = Clock.System.now().toEpochMilliseconds()
                        )
                    }
                    AppLogger.info("LoginViewModel.isAuthenticated -> updateSiteMeta done url=$url")
                }.onFailure {
                    AppLogger.warn("LoginViewModel.isAuthenticated -> updateSiteMeta failed", it)
                }
            }
            AppLogger.info("LoginViewModel.isAuthenticated -> end")
            authFlowState.end()
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

package com.erpnext.pos.views.login

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.navigation.AuthNavigator
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.buildAuthorizeRequest
import com.erpnext.pos.remoteSource.oauth.toOAuthConfig
import com.erpnext.pos.utils.TokenUtils
import com.erpnext.pos.utils.AppLogger
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

class LoginViewModel(
    private val authNavigator: AuthNavigator,
    private val oauthService: APIService,
    private val authStore: AuthInfoStore,
    private val navManager: NavigationManager,
    private val contextProvider: CashBoxManager
) : BaseViewModel() {

    private val _stateFlow: MutableStateFlow<LoginState> = MutableStateFlow(LoginState.Loading)
    val stateFlow: StateFlow<LoginState> = _stateFlow.asStateFlow()

    init {
        viewModelScope.launch {
            contextProvider.initializeContext()
        }
    }

    fun doLogin(url: String) {
        AppLogger.info("LoginViewModel.doLogin -> $url")
        authNavigator.openAuthPage(url)
    }

    fun onAuthCodeReceived(code: String) {
        _stateFlow.update { LoginState.Loading }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                AppLogger.info("LoginViewModel.onAuthCodeReceived")
                val oAuthConfig = authStore.loadAuthInfoByUrl().toOAuthConfig()
                val authRequest = buildAuthorizeRequest(oAuthConfig)
                val tokens = oauthService.exchangeCode(
                    oAuthConfig,
                    code,
                    authRequest.pkce,
                    authRequest.state,
                    authRequest.state
                )
                if (tokens != null) {
                    AppLogger.info("LoginViewModel.onAuthCodeReceived -> tokens OK")
                    _stateFlow.update { LoginState.Authenticated(tokens) }
                } else {
                    AppLogger.warn("LoginViewModel.onAuthCodeReceived -> tokens null")
                    _stateFlow.update { LoginState.Error("Error durante la autenticación") }
                }
            } catch (e: Exception) {
                AppLogger.warn("LoginViewModel.onAuthCodeReceived -> error", e)
                _stateFlow.update {
                    LoginState.Error(
                        e.message ?: "Error durante la autenticación"
                    )
                }
            }
        }
    }

    fun fetchSites() {
        _stateFlow.update { LoginState.Loading }
        viewModelScope.launch {
            AppLogger.info("LoginViewModel.fetchSites")
            val sites = authStore.loadAuthInfo().map { Site(it.url, it.name) }
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
                val oauthConfig = authStore.loadAuthInfoByUrl(site.url).toOAuthConfig()
                val request = if (isDesktop) {
                    AppLogger.info("LoginViewModel.onSiteSelected -> starting OAuthCallbackReceiver")
                    val redirectUri = runCatching {
                        withTimeout(3000) {
                            receiver?.start(DESKTOP_REDIRECT_URI) ?: DESKTOP_REDIRECT_URI
                        }
                    }.onFailure {
                        AppLogger.warn("OAuthCallbackReceiver.start failed, using default redirect", it)
                    }.getOrElse { DESKTOP_REDIRECT_URI }
                    buildAuthorizeRequest(oauthConfig.copy(redirectUrl = redirectUri))
                } else {
                    buildAuthorizeRequest(oauthConfig)
                }
                AppLogger.info("LoginViewModel.onSiteSelected URL -> ${request.url}")
                doLogin(request.url)
                if (isDesktop) {
                    val code = receiver?.awaitCode(request.state) ?: ""
                    onAuthCodeReceived(code)
                }
            } catch (e: Exception) {
                AppLogger.warn("LoginViewModel.onSiteSelected -> error", e)
                _stateFlow.update { LoginState.Error(e.message.toString()) }
            } finally {
                receiver?.stop()
            }
        }
    }

    fun onAddSite(site: Site) {
        _stateFlow.update { LoginState.Loading }
        viewModelScope.launch {
            AppLogger.info("LoginViewModel.onAddSite -> ${site.url}")
            val loginInfo = oauthService.getLoginWithSite(site.url)
            authStore.saveAuthInfo(loginInfo)
            //val sites = authStore.loadAuthInfo().map { Site(it.url, it.name) }
            val oauthConfig = loginInfo.toOAuthConfig()
            val request = buildAuthorizeRequest(oauthConfig)
            doLogin(request.url)
            //_stateFlow.update { LoginState.Success(sites) }
        }
    }

    fun onError(error: String) {
        _stateFlow.update { LoginState.Error(error) }
    }

    fun reset() = _stateFlow.update { LoginState.Success() }

    fun isAuthenticated(tokens: TokenResponse) {
        val isAuth = TokenUtils.isValid(tokens.id_token)
        if (isAuth)
            navManager.navigateTo(NavRoute.Home)
        _stateFlow.update { LoginState.Success() }
    }

    private companion object {
        const val DESKTOP_REDIRECT_URI = "http://127.0.0.1:8070/oauth2redirect"
    }
}

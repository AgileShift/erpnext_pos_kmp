package com.erpnext.pos.views.splash

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.utils.TokenUtils
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SplashViewModel(
    private val navigationManager: NavigationManager,
    private val tokenStore: TokenStore,
    private val contextProvider: CashBoxManager,
    private val apiService: APIService
) : BaseViewModel() {

    private val _stateFlow: MutableStateFlow<SplashState> = MutableStateFlow(SplashState.Loading)
    val stateFlow: StateFlow<SplashState> = _stateFlow.asStateFlow()

    suspend fun initializeContext() = contextProvider.initializeContext()

    fun verifyToken() {
        _stateFlow.update { SplashState.Loading }
        viewModelScope.launch {
            val tokens = tokenStore.load()

            if (tokens == null) {
                // No hay sesión previa
                navigationManager.navigateTo(NavRoute.Login)
                _stateFlow.update { SplashState.InvalidToken }
                return@launch
            }

            val idToken = tokens.id_token
            val refreshToken = tokens.refresh_token

            if (idToken != null && TokenUtils.isValid(idToken)) {
                // Sesión válida, directo a Home
                navigationManager.navigateTo(NavRoute.Home)
                _stateFlow.update { SplashState.Success }
                return@launch
            }

            // id_token inválido o nulo -> intentamos refresh si hay refresh_token
            if (refreshToken.isNullOrEmpty()) {
                navigationManager.navigateTo(NavRoute.Login)
                _stateFlow.update { SplashState.InvalidToken }
                return@launch
            }

            try {
                val newTokens = apiService.refreshToken(refreshToken)
                tokenStore.save(newTokens)
                navigationManager.navigateTo(NavRoute.Home)
                _stateFlow.update { SplashState.Success }
            } catch (e: Exception) {
                // Refresh falló -> sesión inválida
                tokenStore.clear()
                navigationManager.navigateTo(NavRoute.Login)
                _stateFlow.update { SplashState.InvalidToken }
            }
        }
    }
}
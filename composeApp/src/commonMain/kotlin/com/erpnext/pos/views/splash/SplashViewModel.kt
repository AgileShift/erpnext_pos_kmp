package com.erpnext.pos.views.splash

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SplashViewModel(
    private val navigationManager: NavigationManager,
    private val tokenStore: TokenStore,
    private val contextProvider: CashBoxManager
) : BaseViewModel() {

    private val _stateFlow: MutableStateFlow<SplashState> = MutableStateFlow(SplashState.Loading)
    val stateFlow: StateFlow<SplashState> = _stateFlow.asStateFlow()

    suspend fun initializeContext() = contextProvider.initializeContext()

    fun verifyToken() {
        _stateFlow.update { SplashState.Loading }
        viewModelScope.launch {
            if (tokenStore.load() == null) {
                // No hay sesión previa
                navigationManager.navigateTo(NavRoute.Login)
                _stateFlow.update { SplashState.InvalidToken }
                return@launch
            }
            navigationManager.navigateTo(NavRoute.Home)
            _stateFlow.update { SplashState.Success }
        }
    }
}

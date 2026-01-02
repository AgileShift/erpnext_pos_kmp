package com.erpnext.pos.views.home

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.domain.usecases.FetchPosProfileInfoUseCase
import com.erpnext.pos.domain.usecases.FetchPosProfileUseCase
import com.erpnext.pos.domain.usecases.FetchUserInfoUseCase
import com.erpnext.pos.domain.usecases.LogoutUseCase
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.PaymentModeWithAmount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val fetchUserInfoUseCase: FetchUserInfoUseCase,
    private val fetchPosProfileUseCase: FetchPosProfileUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val fetchPosProfileInfoUseCase: FetchPosProfileInfoUseCase,
    private val contextManager: CashBoxManager,
    private val syncManager: SyncManager,
    private val syncPreferences: SyncPreferences,
    private val navManager: NavigationManager
) : BaseViewModel() {
    private val _stateFlow: MutableStateFlow<HomeState> = MutableStateFlow(HomeState.Loading)
    val stateFlow = _stateFlow.asStateFlow()

    val syncState: StateFlow<SyncState> = syncManager.state
    private val _syncSettings = MutableStateFlow(
        SyncSettings(autoSync = true, syncOnStartup = true, wifiOnly = false, lastSyncAt = null)
    )
    val syncSettings: StateFlow<SyncSettings> = _syncSettings.asStateFlow()

    private var userInfo: UserBO = UserBO()
    private var posProfiles: List<POSProfileSimpleBO> = emptyList()

    init {
        viewModelScope.launch {
            syncPreferences.settings.collectLatest { settings ->
                _syncSettings.value = settings
            }
        }
        viewModelScope.launch {
            isCashboxOpen().collectLatest {
                if (it && _syncSettings.value.syncOnStartup) {
                    startInitialSync()
                }
            }
        }
        loadInitialData()
    }

    fun startInitialSync() {
        if (!_syncSettings.value.autoSync) return
        executeUseCase(
            action = { syncManager.fullSync() },
            exceptionHandler = { it.printStackTrace() })
    }

    fun syncNow() {
        executeUseCase(
            action = { syncManager.fullSync() },
            exceptionHandler = { it.printStackTrace() })
    }

    fun loadInitialData() {
        _stateFlow.update { HomeState.Loading }
        executeUseCase(action = {
            userInfo = fetchUserInfoUseCase.invoke(null)
            posProfiles = fetchPosProfileUseCase.invoke(userInfo.email)
            _stateFlow.update { HomeState.POSProfiles(posProfiles, userInfo) }
        }, exceptionHandler = { e ->
            _stateFlow.update { HomeState.Error(e.message ?: "Error") }
        })
    }

    fun isCashboxOpen(): StateFlow<Boolean> = contextManager.cashboxState

    fun resetToInitialState() {
        _stateFlow.update { HomeState.POSProfiles(posProfiles, userInfo) }
    }

    fun logout() {
        executeUseCase(action = {
            logoutUseCase.invoke(null)
            _stateFlow.update { HomeState.Logout }
            navManager.navigateTo(NavRoute.Login)
        }, exceptionHandler = { it.printStackTrace() })
    }

    fun onError(error: String) {
        _stateFlow.update { HomeState.Error(error) }
    }

    fun onPosSelected(pos: POSProfileSimpleBO) {
        _stateFlow.update { HomeState.POSInfoLoading }
        executeUseCase(action = {
            val posProfileInfo = fetchPosProfileInfoUseCase(pos.name)
            _stateFlow.update { HomeState.POSInfoLoaded(posProfileInfo, posProfileInfo.currency) }
        }, exceptionHandler = { it.printStackTrace() })
    }

    fun openCashbox(entry: POSProfileSimpleBO, amounts: List<PaymentModeWithAmount>) {
        viewModelScope.launch {
            contextManager.openCashBox(entry, amounts)
        }
    }

    fun closeCashbox() {
        viewModelScope.launch {
            contextManager.closeCashBox()
        }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch {
            syncPreferences.setAutoSync(enabled)
        }
    }

    fun setSyncOnStartup(enabled: Boolean) {
        viewModelScope.launch {
            syncPreferences.setSyncOnStartup(enabled)
        }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch {
            syncPreferences.setWifiOnly(enabled)
        }
    }
}

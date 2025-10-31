package com.erpnext.pos.views.home

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.domain.models.POSProfileBO
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.domain.usecases.FetchPosProfileInfoUseCase
import com.erpnext.pos.domain.usecases.FetchPosProfileUseCase
import com.erpnext.pos.domain.usecases.FetchUserInfoUseCase
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.CashBoxState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val navManager: NavigationManager,
    private val fetchUserInfoUseCase: FetchUserInfoUseCase,
    private val fetchPosProfileUseCase: FetchPosProfileUseCase,
    private val fetchPosProfileInfoUseCase: FetchPosProfileInfoUseCase,
    private val cashboxManager: CashBoxManager
) : BaseViewModel() {
    private val _stateFlow: MutableStateFlow<HomeState> = MutableStateFlow(HomeState.Loading)
    val stateFlow = _stateFlow.asStateFlow()

    private var userInfo: UserBO = UserBO("", "", "", "", "", "", false)
    private var posProfiles: List<POSProfileSimpleBO> = emptyList()

    init {
        viewModelScope.launch {
            loadUserInfo()
            loadPOSProfile()
            isCashboxOpen()  // Suspend OK en coroutine
        }
        collectCashBoxState()
    }

    private fun collectCashBoxState() {
        viewModelScope.launch {
            cashboxManager.observeCashBoxState().collectLatest { state ->
                when (state) {
                    is CashBoxState.Opened -> {
                        _stateFlow.update { HomeState.CashboxState(true) }
                    }

                    is CashBoxState.Closed -> {
                        _stateFlow.update { HomeState.CashboxState(false) }
                    }
                }
            }
        }
    }

    suspend fun isCashboxOpen(): Boolean = cashboxManager.isCashBoxOpen()

    fun loadUserInfo() {
        _stateFlow.update { HomeState.Loading }
        executeUseCase(action = {
            userInfo = fetchUserInfoUseCase.invoke(null)
            _stateFlow.update { HomeState.Success(userInfo) }
        }, exceptionHandler = { e ->
            _stateFlow.update { HomeState.Error(e.message ?: "Error") }
        })
    }

    fun resetToInitialState() {
        _stateFlow.update { HomeState.POSProfiles(posProfiles) }
        _stateFlow.update { HomeState.Success(userInfo) }
    }

    fun loadPOSProfile() {
        _stateFlow.update { HomeState.Loading }
        executeUseCase(action = {
            posProfiles = fetchPosProfileUseCase.invoke(null)
            _stateFlow.update { HomeState.POSProfiles(posProfiles) }
        }, exceptionHandler = { e ->
            e.printStackTrace()
            _stateFlow.update { HomeState.Error(e.message ?: "Error") }
        })
    }

    fun logout() {}

    fun onError(error: String) {
        _stateFlow.update { HomeState.Error(error) }
    }

    fun onPosSelected(pos: POSProfileBO) {
        _stateFlow.update { HomeState.POSInfoLoading }
        executeUseCase(action = {
            val posProfileInfo = fetchPosProfileInfoUseCase(pos.name)
            _stateFlow.update { HomeState.POSInfoLoaded(posProfileInfo, pos.currency) }
        }, exceptionHandler = {})
    }

    fun openCashbox(entry: POSOpeningEntryDto) {
        viewModelScope.launch {
            cashboxManager.openCashBox(entry)
        }
    }

    fun closeCashbox() {
        viewModelScope.launch {
            cashboxManager.closeCashBox(userId = userInfo.username)  // Asume userInfo tiene username
        }
    }
}
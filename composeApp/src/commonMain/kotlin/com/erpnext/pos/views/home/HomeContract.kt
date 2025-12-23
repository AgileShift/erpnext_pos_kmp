package com.erpnext.pos.views.home

import com.erpnext.pos.domain.models.POSProfileBO
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.views.PaymentModeWithAmount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

sealed class HomeState {
    object Loading : HomeState()
    object POSInfoLoading : HomeState()
    data class POSInfoLoaded(val info: POSProfileBO, val currency: String) : HomeState()
    data class POSProfiles(val posProfiles: List<POSProfileSimpleBO>, val user: UserBO) :
        HomeState()

    data class Error(val message: String) : HomeState()
    object Logout : HomeState()
}

data class HomeAction(
    val sync: () -> Unit = {},
    val syncState: StateFlow<SyncState> = MutableStateFlow(SyncState.IDLE),
    val loadInitialData: () -> Unit = {},
    val initialState: () -> Unit = {},
    val openCashbox: (pos: POSProfileSimpleBO, amounts: List<PaymentModeWithAmount>) -> Unit = { _, _ -> },
    val onPosSelected: (pos: POSProfileSimpleBO) -> Unit = {},
    val closeCashbox: () -> Unit = {},
    val isCashboxOpen: () -> StateFlow<Boolean> = { MutableStateFlow(false) },
    val onLogout: () -> Unit = {},
    val onError: (error: String) -> Unit = {},
)
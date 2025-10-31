package com.erpnext.pos.views.home

import com.erpnext.pos.domain.models.POSProfileBO
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto

sealed class HomeState {
    object Loading : HomeState()
    data class Success(val user: UserBO) : HomeState()
    data class CashboxState(val isOpen: Boolean) : HomeState()
    object LoadingProfiles : HomeState()
    object POSInfoLoading : HomeState()
    data class POSInfoLoaded(val info: POSProfileBO, val currency: String) : HomeState()
    data class POSProfiles(val posProfiles: List<POSProfileSimpleBO>) : HomeState()
    data class Error(val message: String) : HomeState()
}

data class HomeAction(
    val loadUserInfo: () -> Unit = {},
    val loadPOSProfile: () -> Unit = {},
    val initialState: () -> Unit = {},
    val openCashbox: (pos: POSOpeningEntryDto) -> Unit = {},
    val onPosSelected: (pos: POSProfileBO) -> Unit = {},
    val closeCashbox: () -> Unit = {},
    val isCashboxOpen: suspend () -> Boolean = { false },
    val onLogout: () -> Unit = {},
    val onError: (error: String) -> Unit = {},
)
package com.erpnext.pos.views.home

import com.erpnext.pos.domain.models.POSProfileBO
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.localSource.preferences.SyncSettings
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
    val syncSettings: StateFlow<SyncSettings> = MutableStateFlow(
        SyncSettings(
            autoSync = true,
            syncOnStartup = true,
            wifiOnly = false,
            lastSyncAt = null,
            useTtl = false
        )
    ),
    val homeMetrics: StateFlow<HomeMetrics> = MutableStateFlow(HomeMetrics()),
    val openingState: StateFlow<CashboxOpeningProfileState> =
        MutableStateFlow(CashboxOpeningProfileState()),
    val loadInitialData: () -> Unit = {},
    val initialState: () -> Unit = {},
    val onPosSelected: (pos: POSProfileSimpleBO) -> Unit = {},
    val onLoadOpeningProfile: (profileId: String?) -> Unit = {},
    val onOpenCashbox: suspend (POSProfileSimpleBO, List<com.erpnext.pos.views.PaymentModeWithAmount>) -> Unit =
        { _, _ -> },
    val closeCashbox: () -> Unit = {},
    val isCashboxOpen: () -> StateFlow<Boolean> = { MutableStateFlow(false) },
    val onOpenSettings: () -> Unit = {},
    val onOpenReconciliation: () -> Unit = {},
    val onCloseCashbox: () -> Unit = {},
    val onLogout: () -> Unit = {},
    val onError: (error: String) -> Unit = {},
)

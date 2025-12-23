package com.erpnext.pos.views.home

import com.erpnext.pos.domain.usecases.FetchPosProfileUseCase
import com.erpnext.pos.domain.usecases.FetchUserInfoUseCase
import com.erpnext.pos.navigation.NavigationManager

class POSProfileViewModel(
    private val navManager: NavigationManager,
    private val fetchPosProfileUseCase: FetchPosProfileUseCase,
    private val fetchUserInfoUseCase: FetchUserInfoUseCase,
) {
}
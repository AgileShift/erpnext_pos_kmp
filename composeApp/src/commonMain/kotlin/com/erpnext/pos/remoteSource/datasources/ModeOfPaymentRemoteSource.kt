package com.erpnext.pos.remoteSource.datasources

import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.AccountDetailDto
import com.erpnext.pos.remoteSource.dto.ModeOfPaymentDetailDto
import com.erpnext.pos.remoteSource.dto.ModeOfPaymentDto

class ModeOfPaymentRemoteSource(
    private val api: APIService
) {
    suspend fun getActiveModes(): List<ModeOfPaymentDto> {
        return api.getActiveModeOfPayment()
    }

    suspend fun getModeDetail(name: String): ModeOfPaymentDetailDto? {
        return api.getModeOfPaymentDetail(name)
    }

    suspend fun getAccountDetail(name: String): AccountDetailDto? {
        return api.getAccountDetail(name)
    }
}

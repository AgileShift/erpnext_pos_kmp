package com.erpnext.pos.remoteSource.datasources

import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.PaymentModesDao
import com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.POSProfileDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto
import com.erpnext.pos.remoteSource.dto.POSProfileSimpleDto
import com.erpnext.pos.remoteSource.mapper.toEntity

class POSProfileRemoteSource(
    private val api: APIService,
    private val posProfileDao: POSProfileDao,
    private val paymentModesDao: PaymentModesDao
) {
    suspend fun getPOSProfile(assignedTo: String? = null): List<POSProfileSimpleDto> {
        return api.getPOSProfiles(assignedTo)
    }

    //TODO: Tengo que crear una tabla para poder guardar el estado actual de la caja, la informacion seria
    // status, profileId, warehouseId, userId
    suspend fun getPOSProfileDetails(profileId: String): POSProfileDto {
        val profiles = api.getPOSProfileDetails(profileId)
        posProfileDao.insertAll(listOf(profiles.toEntity()))
        paymentModesDao.insertAll(profiles.payments.toEntity(profileId))
        return profiles
    }


    suspend fun sync() {

    }
}
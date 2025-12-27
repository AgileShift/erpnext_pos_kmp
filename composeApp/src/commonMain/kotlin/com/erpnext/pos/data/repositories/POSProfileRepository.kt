package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.POSProfileBO
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.repositories.IPOSRepository
import com.erpnext.pos.localSource.datasources.POSProfileLocalSource
import com.erpnext.pos.remoteSource.datasources.POSProfileRemoteSource
import kotlinx.coroutines.flow.combine

//TODO: Siempre tenemos que agregar el LocalSource
class POSProfileRepository(
    private val remoteSource: POSProfileRemoteSource,
    private val localSource: POSProfileLocalSource,
) : IPOSRepository {
    override suspend fun getPOSProfiles(assignedTo: String?): List<POSProfileSimpleBO> {
        return remoteSource.getPOSProfile(assignedTo).toBO()
    }

    override suspend fun getPOSProfileDetails(profileId: String): POSProfileBO {
        return remoteSource.getPOSProfileDetails(profileId).toBO()
    }

    override suspend fun sync() {
    }
}
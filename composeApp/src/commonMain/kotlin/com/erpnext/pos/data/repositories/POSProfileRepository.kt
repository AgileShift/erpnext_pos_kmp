package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.POSProfileBO
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.repositories.IPOSRepository
import com.erpnext.pos.localSource.datasources.POSProfileLocalSource
import com.erpnext.pos.remoteSource.datasources.POSProfileRemoteSource
import com.erpnext.pos.utils.RepoTrace
import kotlinx.coroutines.flow.combine

//TODO: Siempre tenemos que agregar el LocalSource
class POSProfileRepository(
    private val remoteSource: POSProfileRemoteSource,
    private val localSource: POSProfileLocalSource,
) : IPOSRepository {
    override suspend fun getPOSProfiles(assignedTo: String?): List<POSProfileSimpleBO> {
        RepoTrace.breadcrumb("POSProfileRepository", "getPOSProfiles", "assignedTo=$assignedTo")
        return runCatching { remoteSource.getPOSProfile(assignedTo).toBO() }
            .getOrElse {
                RepoTrace.capture("POSProfileRepository", "getPOSProfiles", it)
                throw it
            }
    }

    override suspend fun getPOSProfileDetails(profileId: String): POSProfileBO {
        RepoTrace.breadcrumb("POSProfileRepository", "getPOSProfileDetails", profileId)
        return runCatching { remoteSource.getPOSProfileDetails(profileId).toBO() }
            .getOrElse {
                RepoTrace.capture("POSProfileRepository", "getPOSProfileDetails", it)
                throw it
            }
    }

    override suspend fun sync() {
    }
}

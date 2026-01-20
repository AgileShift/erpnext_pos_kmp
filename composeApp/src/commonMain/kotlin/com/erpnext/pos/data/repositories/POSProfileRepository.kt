package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.data.mappers.toSimpleBO
import com.erpnext.pos.domain.models.POSProfileBO
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.repositories.IPOSRepository
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.PosProfileLocalDao
import com.erpnext.pos.utils.RepoTrace

//TODO: Siempre tenemos que agregar el LocalSource
class POSProfileRepository(
    private val posProfileDao: POSProfileDao,
    private val posProfileLocalDao: PosProfileLocalDao
) : IPOSRepository {
    override suspend fun getPOSProfiles(assignedTo: String?): List<POSProfileSimpleBO> {
        RepoTrace.breadcrumb("POSProfileRepository", "getPOSProfiles", "assignedTo=$assignedTo")
        return runCatching { posProfileLocalDao.getAll().map { it.toSimpleBO() } }
            .getOrElse { error ->
                RepoTrace.capture("POSProfileRepository", "getPOSProfiles", error)
                throw error
            }
    }

    override suspend fun getPOSProfileDetails(profileId: String): POSProfileBO {
        RepoTrace.breadcrumb("POSProfileRepository", "getPOSProfileDetails", profileId)
        return runCatching { posProfileDao.getPOSProfile(profileId).toBO() }
            .getOrElse { error ->
                RepoTrace.capture("POSProfileRepository", "getPOSProfileDetails", error)
                throw error
            }
    }

    override suspend fun sync() {
    }
}

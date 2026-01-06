package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.DeliveryChargeBO
import com.erpnext.pos.localSource.datasources.DeliveryChargeLocalSource
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.RepoTrace

class DeliveryChargesRepository(
    private val api: APIService,
    private val localSource: DeliveryChargeLocalSource
) {
    suspend fun fetchDeliveryCharges(): List<DeliveryChargeBO> {
        RepoTrace.breadcrumb("DeliveryChargesRepository", "fetchDeliveryCharges")

        val localData = localSource.getAll().map { it.toBO() }

        val remoteResult = runCatching {
            api.fetchDeliveryCharges()
        }

        remoteResult.onSuccess { charges ->
            localSource.insertAll(charges.map { it.toEntity() })
        }

        return remoteResult.map { charges ->
            charges.map { it.toEntity().toBO() }
        }.getOrElse { error ->
            RepoTrace.capture("DeliveryChargesRepository", "fetchDeliveryCharges", error)
            localData
        }
    }
}

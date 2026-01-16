package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.DeliveryChargeBO
import com.erpnext.pos.localSource.datasources.DeliveryChargeLocalSource
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.RepoTrace
import io.ktor.client.plugins.ClientRequestException

class DeliveryChargesRepository(
    private val api: APIService,
    private val localSource: DeliveryChargeLocalSource
) {
    suspend fun fetchDeliveryCharges(): List<DeliveryChargeBO> {
        RepoTrace.breadcrumb("DeliveryChargesRepository", "fetchDeliveryCharges")

        val localData = localSource.getAll().map { it.toBO() }

        return runCatching { api.fetchDeliveryCharges() }
            .onSuccess { charges ->
                localSource.deleteAll()
                if (charges.isNotEmpty()) {
                    localSource.insertAll(charges.map { it.toEntity() })
                }
            }
            .map { charges -> charges.map { it.toEntity().toBO() } }
            .getOrElse { error ->
                if (error is ClientRequestException && error.response.status.value == 404) {
                    localSource.deleteAll()
                    return emptyList()
                }
                RepoTrace.capture("DeliveryChargesRepository", "fetchDeliveryCharges", error)
                localData
            }
    }
}

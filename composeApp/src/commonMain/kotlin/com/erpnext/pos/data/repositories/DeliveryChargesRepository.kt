package com.erpnext.pos.data.repositories

import com.erpnext.pos.domain.models.DeliveryChargeBO
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.utils.RepoTrace

class DeliveryChargesRepository(
    private val api: APIService
) {
    suspend fun fetchDeliveryCharges(): List<DeliveryChargeBO> {
        RepoTrace.breadcrumb("DeliveryChargesRepository", "fetchDeliveryCharges")
        return runCatching {
            api.fetchDeliveryCharges().map { charge ->
                DeliveryChargeBO(
                    label = charge.label,
                    defaultRate = charge.defaultRate
                )
            }
        }.getOrElse {
            RepoTrace.capture("DeliveryChargesRepository", "fetchDeliveryCharges", it)
            throw it
        }
    }
}

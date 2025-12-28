package com.erpnext.pos.data.repositories

import com.erpnext.pos.domain.models.DeliveryChargeBO
import com.erpnext.pos.remoteSource.api.APIService

class DeliveryChargesRepository(
    private val api: APIService
) {
    suspend fun fetchDeliveryCharges(): List<DeliveryChargeBO> {
        return api.fetchDeliveryCharges().map { charge ->
            DeliveryChargeBO(
                label = charge.label,
                defaultRate = charge.defaultRate
            )
        }
    }
}

package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.DeliveryChargesRepository
import com.erpnext.pos.domain.models.DeliveryChargeBO

class FetchDeliveryChargesLocalUseCase(
    private val repository: DeliveryChargesRepository
) : UseCase<Unit, List<DeliveryChargeBO>>() {
    override suspend fun useCaseFunction(input: Unit): List<DeliveryChargeBO> {
        return repository.getLocalDeliveryCharges()
    }
}

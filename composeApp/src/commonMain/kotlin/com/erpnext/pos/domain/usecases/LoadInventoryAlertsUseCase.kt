package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.InventoryAlertRepository
import com.erpnext.pos.views.home.InventoryAlert

data class InventoryAlertInput(
    val instanceId: String,
    val companyId: String,
    val warehouseId: String,
    val limit: Int = 20
)

class LoadInventoryAlertsUseCase(
    private val repository: InventoryAlertRepository
) : UseCase<InventoryAlertInput, List<InventoryAlert>>() {

    override suspend fun useCaseFunction(input: InventoryAlertInput): List<InventoryAlert> {
        return repository.loadAlerts(
            warehouseId = input.warehouseId,
            limit = input.limit
        )
    }
}

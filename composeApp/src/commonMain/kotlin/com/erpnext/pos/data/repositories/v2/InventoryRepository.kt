package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.repositories.v2.IInventoryRepository
import com.erpnext.pos.localSource.dao.v2.InventoryDao
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class InventoryRepository(
    private val inventoryDao: InventoryDao
) : IInventoryRepository {
    override suspend fun getStockByWarehouse(
        instanceId: String,
        companyId: String,
        warehouseId: String
    ): Map<String, Float> {
        return inventoryDao
            .getStockForWarehouse(instanceId, companyId, warehouseId)
            .associate { it.itemCode to it.actualQty }
    }

    override suspend fun adjustStockLocal(
        instanceId: String,
        companyId: String,
        warehouseId: String,
        itemId: String,
        deltaQty: Double
    ) {
        inventoryDao.updateStock(
            instanceId = instanceId,
            companyId = companyId,
            warehouseId = warehouseId,
            itemId = itemId,
            deltaQty = deltaQty,
            updatedAt = Clock.System.now().epochSeconds
        )
    }
}
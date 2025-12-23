package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Query

@Dao
interface InventoryDao {

    @Query(
        """
        SELECT itemId, actualQty, reservedQty, projectedQty
        FROM inventory_bins
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND warehouseId = :warehouseId
          AND is_deleted = 0
    """
    )
    suspend fun getStockForWarehouse(
        instanceId: String,
        companyId: String,
        warehouseId: String
    ): List<InventoryStockRow>

    @Query(
        """
        UPDATE inventory_bins
        SET actualQty = actualQty + :deltaQty,
            updated_at = :updatedAt
        WHERE instanceId = :instanceId
          AND companyId = :companyId
          AND warehouseId = :warehouseId
          AND itemId = :itemId
    """
    )
    suspend fun updateStock(
        instanceId: String,
        companyId: String,
        warehouseId: String,
        itemId: String,
        deltaQty: Double,
        updatedAt: Long
    )
}

data class InventoryStockRow(
    val itemCode: String,
    val actualQty: Float,
    val reservedQty: Float?,
    val projectedQty: Float?
)

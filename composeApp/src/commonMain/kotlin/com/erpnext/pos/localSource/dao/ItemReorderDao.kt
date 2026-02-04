package com.erpnext.pos.localSource.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.ItemReorderEntity

@Dao
interface ItemReorderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(rows: List<ItemReorderEntity>)

    @Query(
        """
        SELECT * FROM item_reorders
        WHERE warehouseId = :warehouseId
          AND is_deleted = 0
    """
    )
    suspend fun getByWarehouse(
        warehouseId: String
    ): List<ItemReorderEntity>

    @Query(
        """
        SELECT * FROM item_reorders
        WHERE warehouseId = :warehouseId
          AND itemId IN (:itemIds)
          AND is_deleted = 0
    """
    )
    suspend fun getByItems(
        warehouseId: String,
        itemIds: List<String>
    ): List<ItemReorderEntity>

    @Query(
        """
        DELETE FROM item_reorders
        WHERE warehouseId = :warehouseId
    """
    )
    suspend fun deleteByWarehouse(
        warehouseId: String
    )
}

package com.erpnext.pos.localSource.dao.v2

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.erpnext.pos.localSource.entities.v2.InventoryBinEntity
import com.erpnext.pos.localSource.entities.v2.ItemEntity
import com.erpnext.pos.localSource.entities.v2.ItemGroupEntity
import com.erpnext.pos.localSource.entities.v2.ItemPriceEntity

@Dao
interface CatalogDao {

    @Query("""
        SELECT *
        FROM item_groups
        WHERE instanceId = :instanceId AND companyId = :companyId
    """)
    suspend fun getAllItemGroups(instanceId: String, companyId: String): List<ItemGroupEntity>

    @Query("""
        SELECT *
        FROM items
        WHERE instanceId = :instanceId AND companyId = :companyId
          AND disabled = 0
    """)
    suspend fun getActiveItems(instanceId: String, companyId: String): List<ItemEntity>

    @Query("""
        SELECT *
        FROM item_prices
        WHERE instanceId = :instanceId AND companyId = :companyId
          AND priceList = :priceList
    """)
    suspend fun getItemPricesForPriceList(
        instanceId: String,
        companyId: String,
        priceList: String
    ): List<ItemPriceEntity>

    @Query("""
        SELECT *
        FROM inventory_bins
        WHERE instanceId = :instanceId AND companyId = :companyId
          AND warehouseId = :warehouseId
    """)
    suspend fun getInventoryForWarehouse(
        instanceId: String,
        companyId: String,
        warehouseId: String
    ): List<InventoryBinEntity>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItemGroups(itemGroups: List<ItemGroupEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItems(items: List<ItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItemPrices(prices: List<ItemPriceEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertInventoryBins(bins: List<InventoryBinEntity>)
}

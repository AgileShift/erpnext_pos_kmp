package com.erpnext.pos.domain.repositories.v2

import com.erpnext.pos.localSource.entities.v2.InventoryBinEntity
import com.erpnext.pos.localSource.entities.v2.ItemEntity
import com.erpnext.pos.localSource.entities.v2.ItemGroupEntity
import com.erpnext.pos.localSource.entities.v2.ItemPriceEntity

interface ICatalogRepository {
    suspend fun getCategories(instanceId: String, companyId: String): List<ItemGroupEntity>?
    suspend fun getItems(instanceId: String, companyId: String): List<ItemEntity>
    suspend fun getPrices(
        instanceId: String,
        companyId: String,
        priceList: String
    ): List<ItemPriceEntity>

    suspend fun getStock(
        instanceId: String,
        companyId: String,
        warehouseId: String
    ): List<InventoryBinEntity>
}
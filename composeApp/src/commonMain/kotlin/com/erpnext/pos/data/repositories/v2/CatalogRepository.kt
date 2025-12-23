package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.repositories.v2.ICatalogRepository
import com.erpnext.pos.localSource.dao.v2.CatalogDao

class CatalogRepository(
    private val catalogDao: CatalogDao
) : ICatalogRepository {

    override suspend fun getCategories(instanceId: String, companyId: String) =
        catalogDao.getAllItemGroups(instanceId, companyId)

    override suspend fun getItems(instanceId: String, companyId: String) =
        catalogDao.getActiveItems(instanceId, companyId)

    override suspend fun getPrices(
        instanceId: String,
        companyId: String,
        priceList: String
    ) = catalogDao.getItemPricesForPriceList(instanceId, companyId, priceList)

    override suspend fun getStock(
        instanceId: String,
        companyId: String,
        warehouseId: String
    ) = catalogDao.getInventoryForWarehouse(instanceId, companyId, warehouseId)
}

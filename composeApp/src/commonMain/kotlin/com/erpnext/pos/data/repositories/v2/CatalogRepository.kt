package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.repositories.v2.ICatalogRepository
import com.erpnext.pos.localSource.dao.v2.CatalogDao
import com.erpnext.pos.utils.RepoTrace

class CatalogRepository(
    private val catalogDao: CatalogDao
) : ICatalogRepository {

    override suspend fun getCategories(instanceId: String, companyId: String) =
        catalogDao.getAllItemGroups(instanceId, companyId).also {
            RepoTrace.breadcrumb("CatalogRepositoryV2", "getCategories")
        }

    override suspend fun getItems(instanceId: String, companyId: String) =
        catalogDao.getActiveItems(instanceId, companyId).also {
            RepoTrace.breadcrumb("CatalogRepositoryV2", "getItems")
        }

    override suspend fun getPrices(
        instanceId: String,
        companyId: String,
        priceList: String
    ) = catalogDao.getItemPricesForPriceList(instanceId, companyId, priceList).also {
        RepoTrace.breadcrumb("CatalogRepositoryV2", "getPrices")
    }

    override suspend fun getStock(
        instanceId: String,
        companyId: String,
        warehouseId: String
    ) = catalogDao.getInventoryForWarehouse(instanceId, companyId, warehouseId).also {
        RepoTrace.breadcrumb("CatalogRepositoryV2", "getStock")
    }
}

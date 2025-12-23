package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.cache.CatalogCache
import com.erpnext.pos.data.repositories.v2.CatalogRepository
import com.erpnext.pos.domain.usecases.UseCase
import com.erpnext.pos.remoteSource.dto.v2.CatalogSnapshot
import com.erpnext.pos.remoteSource.dto.v2.ItemCatalogDto

data class CatalogInput(
    val instanceId: String,
    val companyId: String,
    val priceList: String,
    val warehouseId: String
)

class LoadCatalogUseCase(
    private val catalogRepository: CatalogRepository,
    private val cache: CatalogCache
) : UseCase<CatalogInput, CatalogSnapshot>() {

    override suspend fun useCaseFunction(input: CatalogInput): CatalogSnapshot {
        val cacheKey =
            "${input.instanceId}|${input.companyId}|${input.priceList}|${input.warehouseId}"

        cache.get(cacheKey)?.let { return it }

        val categories = catalogRepository.getCategories(input.instanceId, input.companyId)
        val items = catalogRepository.getItems(input.instanceId, input.companyId)
        val prices = catalogRepository
            .getPrices(input.instanceId, input.companyId, input.priceList)
            .associate { it.itemId to it.rate }

        val stock = catalogRepository
            .getStock(input.instanceId, input.companyId, input.warehouseId)
            .associate { it.itemId to it.actualQty }

        val catalogItems = items.map {
            ItemCatalogDto(
                itemId = it.itemId,
                itemCode = it.itemCode,
                itemName = it.itemName,
                itemGroupId = it.itemGroup,
                imageUrl = it.imageUrl,
                salesUom = it.salesUom ?: "Unit",
                stockUom = it.stockUom ?: "Unit",
                allowNegativeStock = it.allowNegativeStock,
                disabled = it.disabled
            )
        }

        val snapshot = CatalogSnapshot(
            categories = categories,
            items = catalogItems,
            prices = prices,
            stock = stock
        )

        cache.put(cacheKey, snapshot)
        return snapshot
    }
}

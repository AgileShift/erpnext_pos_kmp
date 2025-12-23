package com.erpnext.pos.remoteSource.datasources

import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.ItemDto
import com.erpnext.pos.remoteSource.dto.CategoryDto
import com.erpnext.pos.remoteSource.dto.WarehouseItemDto

class InventoryRemoteSource(private val apiService: APIService) {

    suspend fun getItems(
        warehouseId: String? = null,
        priceList: String? = null,
        offset: Int? = null,
        pageSize: Int? = null
    ): List<WarehouseItemDto> {
        return apiService.getInventoryForWarehouse(warehouseId, priceList, offset, pageSize)
    }

    suspend fun getItemDetail(itemId: String): ItemDto {
        return apiService.getItemDetail(itemId)
    }

    suspend fun getCategories(): List<CategoryDto> {
        return apiService.getCategories()
    }
}

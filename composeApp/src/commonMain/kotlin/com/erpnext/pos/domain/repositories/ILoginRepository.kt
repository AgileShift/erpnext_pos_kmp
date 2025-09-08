package com.erpnext.pos.domain.repositories

import com.erpnext.pos.domain.models.CategoryBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.remoteSource.dto.CredentialsDto

interface IInventoryRepository {
    suspend fun getItems(): List<ItemBO>
    suspend fun getItemDetails(itemId: String): ItemBO
    suspend fun getCategories(): List<CategoryBO>
}
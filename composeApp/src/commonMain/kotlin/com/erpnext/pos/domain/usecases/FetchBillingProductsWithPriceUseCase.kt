package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.InventoryRepository
import com.erpnext.pos.domain.models.ItemBO
import kotlinx.coroutines.flow.Flow

class FetchBillingProductsWithPriceUseCase(
    private val repo: InventoryRepository
) : UseCase<String?, Flow<List<ItemBO>>>() {
    override suspend fun useCaseFunction(input: String?): Flow<List<ItemBO>> {
        return repo.getItems(input)
    }
}
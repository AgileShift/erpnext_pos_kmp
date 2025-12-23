package com.erpnext.pos.domain.usecases

import androidx.paging.PagingData
import com.erpnext.pos.data.repositories.InventoryRepository
import com.erpnext.pos.domain.models.ItemBO
import kotlinx.coroutines.flow.Flow

class FetchInventoryItemUseCase(
    private val repo: InventoryRepository
) : UseCase<String?, Flow<PagingData<ItemBO>>>() {
    override suspend fun useCaseFunction(input: String?): Flow<PagingData<ItemBO>> {
        return repo.getItemsPaged(input)
    }
}
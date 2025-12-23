package com.erpnext.pos.domain.usecases

import com.erpnext.pos.base.Resource
import com.erpnext.pos.data.repositories.InventoryRepository
import com.erpnext.pos.domain.models.CategoryBO
import kotlinx.coroutines.flow.Flow

class FetchCategoriesUseCase(
    private val repo: InventoryRepository
) : UseCase<Unit?, Flow<Resource<List<CategoryBO>>>>() {
    override suspend fun useCaseFunction(input: Unit?): Flow<Resource<List<CategoryBO>>> {
        return repo.getCategories()
    }
}
package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.localSource.datasources.InventoryLocalSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FetchBillingProductsLocalUseCase(
    private val localSource: InventoryLocalSource
) : UseCase<String?, Flow<List<ItemBO>>>() {
    override suspend fun useCaseFunction(input: String?): Flow<List<ItemBO>> {
        val query = input?.trim().orEmpty()
        val source = if (query.isEmpty()) {
            flowOf(localSource.getAll())
        } else {
            localSource.getAllFiltered(query)
        }
        return source.map { list -> list.map { it.toBO() } }
    }
}

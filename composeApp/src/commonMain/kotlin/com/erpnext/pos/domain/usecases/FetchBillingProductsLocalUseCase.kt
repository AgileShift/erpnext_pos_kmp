package com.erpnext.pos.domain.usecases

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.localSource.datasources.InventoryLocalSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class BillingProductsQueryInput(
    val query: String? = null,
    val category: String? = null
)

class FetchBillingProductsLocalUseCase(
    private val localSource: InventoryLocalSource
) : UseCase<BillingProductsQueryInput?, Flow<PagingData<ItemBO>>>() {
    override suspend fun useCaseFunction(input: BillingProductsQueryInput?): Flow<PagingData<ItemBO>> {
        val query = input?.query?.trim().orEmpty()
        val category = input?.category?.trim().orEmpty()
        return Pager(PagingConfig(pageSize = 40, prefetchDistance = 15)) {
            localSource.getPaged(search = query, category = category)
        }.flow.map { paging ->
            paging.map { it.toBO() }
        }
    }
}

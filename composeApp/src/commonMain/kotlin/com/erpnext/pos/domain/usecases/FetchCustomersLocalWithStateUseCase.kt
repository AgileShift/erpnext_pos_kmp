package com.erpnext.pos.domain.usecases

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.localSource.datasources.CustomerLocalSource
import com.erpnext.pos.remoteSource.mapper.toBO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Local-only customer lookup used by the Customer view.
 * Data sources:
 * - CustomerLocalSource (customers cached locally)
 */
class FetchCustomersLocalWithStateUseCase(
    private val localSource: CustomerLocalSource
) : UseCase<CustomerQueryInput?, Flow<PagingData<CustomerBO>>>() {
    override suspend fun useCaseFunction(input: CustomerQueryInput?): Flow<PagingData<CustomerBO>> {
        val query = input?.query?.trim().orEmpty()
        val state = input?.state?.trim().orEmpty()
        return Pager(PagingConfig(pageSize = 30, prefetchDistance = 10)) {
            localSource.getPaged(search = query, state = state)
        }.flow.map { paging ->
            paging.map { it.toBO() }
        }
    }

    suspend fun count(input: CustomerQueryInput?): Int {
        val query = input?.query?.trim().orEmpty()
        val state = input?.state?.trim().orEmpty()
        return localSource.countFiltered(search = query, state = state)
    }

    suspend fun countPending(input: CustomerQueryInput?): Int {
        val query = input?.query?.trim().orEmpty()
        val state = input?.state?.trim().orEmpty()
        return localSource.countPendingFiltered(search = query, state = state)
    }
}

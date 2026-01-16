package com.erpnext.pos.domain.usecases

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
) : UseCase<CustomerQueryInput?, Flow<List<CustomerBO>>>() {
    override suspend fun useCaseFunction(input: CustomerQueryInput?): Flow<List<CustomerBO>> {
        val query = input?.query?.trim().orEmpty()
        val state = input?.state?.trim().orEmpty()
        val source = when {
            state.isEmpty() && query.isEmpty() -> localSource.getAll()
            state.isEmpty() -> localSource.getAllFiltered(query)
            query.isEmpty() -> if (state == "Todos") {
                localSource.getAll()
            } else {
                localSource.getByCustomerState(state)
            }
            else -> localSource.getAll()
        }
        return source.map { list -> list.map { it.toBO() } }
    }
}

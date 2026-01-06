package com.erpnext.pos.domain.usecases

import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.localSource.datasources.CustomerLocalSource
import com.erpnext.pos.remoteSource.mapper.toBO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FetchCustomersLocalUseCase(
    private val localSource: CustomerLocalSource
) : UseCase<String?, Flow<List<CustomerBO>>>() {
    override suspend fun useCaseFunction(input: String?): Flow<List<CustomerBO>> {
        val query = input?.trim().orEmpty()
        val source = if (query.isEmpty()) {
            localSource.getAll()
        } else {
            localSource.getAllFiltered(query)
        }
        return source.map { list -> list.map { it.toBO() } }
    }
}

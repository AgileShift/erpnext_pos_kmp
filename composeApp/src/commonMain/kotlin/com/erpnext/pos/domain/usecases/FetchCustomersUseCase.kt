package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.CustomerRepository
import com.erpnext.pos.domain.models.CustomerBO
import kotlinx.coroutines.flow.Flow

data class CustomerQueryInput(val query: String? = null, val state: String? = null)
class FetchCustomersUseCase(
    private val repo: CustomerRepository
) : UseCase<CustomerQueryInput?, Flow<List<CustomerBO>>>() {
    override suspend fun useCaseFunction(input: CustomerQueryInput?): Flow<List<CustomerBO>> {
        return repo.getCustomers(input?.query, input?.state)
    }
}
package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.CustomerRepository

class RebuildCustomerSummariesUseCase(
    private val repository: CustomerRepository
) : UseCase<Unit, Unit>() {
    override suspend fun useCaseFunction(input: Unit) {
        repository.rebuildAllCustomerSummaries()
    }
}

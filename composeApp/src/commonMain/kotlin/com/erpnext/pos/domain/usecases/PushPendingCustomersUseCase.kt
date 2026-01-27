package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.CustomerSyncRepository

class PushPendingCustomersUseCase(
    private val repository: CustomerSyncRepository
) : UseCase<Unit, Boolean>() {
    override suspend fun useCaseFunction(input: Unit): Boolean {
        return repository.pushPending()
    }
}

package com.erpnext.pos.domain.usecases

import com.erpnext.pos.remoteSource.api.APIService

class LogoutUseCase(
    private val apiService: APIService
) : UseCase<Unit?, Unit>() {
    override suspend fun useCaseFunction(input: Unit?) {
        apiService.revoke()
    }
}
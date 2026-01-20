package com.erpnext.pos.domain.usecases

import com.erpnext.pos.domain.models.POSProfileBO
import com.erpnext.pos.domain.repositories.IPOSRepository

class FetchPosProfileInfoLocalUseCase(
    private val repository: IPOSRepository
) : UseCase<String, POSProfileBO>() {
    override suspend fun useCaseFunction(input: String): POSProfileBO {
        return repository.getPOSProfileDetails(input)
    }
}

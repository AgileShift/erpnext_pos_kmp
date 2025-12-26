package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.data.repositories.v2.ContextRepository
import com.erpnext.pos.domain.repositories.v2.ContextPullInput
import com.erpnext.pos.remoteSource.dto.v2.POSContextSnapshot

class LoadPosContextUseCase(
    private val contextRepository: ContextRepository
) {

    suspend operator fun invoke(
        instanceId: String,
        companyId: String,
        userId: String,
        posProfileId: String
    ): POSContextSnapshot {

        val existing = contextRepository.getContextSnapshot(
            instanceId,
            companyId,
            userId,
            posProfileId
        )
        if (existing != null) {
            return existing
        }

        contextRepository.pullContext(
            ContextPullInput(
                instanceId = instanceId,
                companyId = companyId,
                userId = userId,
                posProfileId = posProfileId
            )
        )

        return requireNotNull(
            contextRepository.getContextSnapshot(
                instanceId,
                companyId,
                userId,
                posProfileId
            )
        )
    }
}

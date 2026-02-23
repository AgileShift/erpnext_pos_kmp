package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.InternalTransferRepository
import com.erpnext.pos.remoteSource.dto.InternalTransferCreateDto
import com.erpnext.pos.remoteSource.dto.InternalTransferSubmitDto

data class CreateInternalTransferInput(
    val clientRequestId: String,
    val payload: InternalTransferCreateDto
)

class CreateInternalTransferUseCase(
    private val repository: InternalTransferRepository
) : UseCase<CreateInternalTransferInput, InternalTransferSubmitDto>() {
    override suspend fun useCaseFunction(input: CreateInternalTransferInput): InternalTransferSubmitDto {
        return repository.createInternalTransfer(input.clientRequestId, input.payload)
    }
}

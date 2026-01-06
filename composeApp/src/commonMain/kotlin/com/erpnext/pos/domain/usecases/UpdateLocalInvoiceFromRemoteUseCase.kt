package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto

data class UpdateLocalInvoiceFromRemoteInput(
    val localInvoiceName: String,
    val remoteInvoice: SalesInvoiceDto
)

class UpdateLocalInvoiceFromRemoteUseCase(
    private val repository: SalesInvoiceRepository
) : UseCase<UpdateLocalInvoiceFromRemoteInput, Unit>() {
    override suspend fun useCaseFunction(input: UpdateLocalInvoiceFromRemoteInput) {
        repository.updateLocalInvoiceFromRemote(input.localInvoiceName, input.remoteInvoice)
    }
}

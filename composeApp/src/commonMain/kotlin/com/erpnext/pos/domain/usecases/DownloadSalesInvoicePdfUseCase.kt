package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository

class DownloadSalesInvoicePdfUseCase(
    private val repository: SalesInvoiceRepository
) : UseCase<String, String>() {
    override suspend fun useCaseFunction(input: String): String {
        return repository.downloadInvoicePdf(input)
    }
}

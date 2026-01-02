package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.data.repositories.v2.SourceDocumentRepository
import com.erpnext.pos.domain.models.SourceDocumentOption
import com.erpnext.pos.views.salesflow.SalesFlowSource

data class LoadSourceDocumentsInput(
    val customerId: String,
    val sourceType: SalesFlowSource
)

class LoadSourceDocumentsUseCase(
    private val repository: SourceDocumentRepository
) {
    suspend operator fun invoke(input: LoadSourceDocumentsInput): List<SourceDocumentOption> {
        return repository.fetchDocumentsForCustomer(input.customerId, input.sourceType)
    }
}

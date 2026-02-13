package com.erpnext.pos.domain.usecases

import com.erpnext.pos.domain.models.SourceDocumentOption
import com.erpnext.pos.views.salesflow.SalesFlowSource

data class LoadSourceDocumentsInput(
    val customerId: String,
    val sourceType: SalesFlowSource
)

class LoadSourceDocumentsUseCase {
    suspend operator fun invoke(input: LoadSourceDocumentsInput): List<SourceDocumentOption> {
        return emptyList()
    }
}

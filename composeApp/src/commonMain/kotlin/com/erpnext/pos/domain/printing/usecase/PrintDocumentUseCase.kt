package com.erpnext.pos.domain.printing.usecase

import com.erpnext.pos.domain.printing.model.PrintDocument
import com.erpnext.pos.domain.printing.model.PrintExecutionResult
import com.erpnext.pos.domain.repositories.printing.IPrinterProfileRepository
import com.erpnext.pos.domain.usecases.UseCase
import com.erpnext.pos.printing.application.PrintOrchestrator

data class PrintDocumentInput(
    val profileId: String,
    val document: PrintDocument,
)

class PrintDocumentUseCase(
    private val profileRepository: IPrinterProfileRepository,
    private val orchestrator: PrintOrchestrator,
) : UseCase<PrintDocumentInput, Result<PrintExecutionResult>>() {
  override suspend fun useCaseFunction(input: PrintDocumentInput): Result<PrintExecutionResult> {
    val profile =
        profileRepository.getById(input.profileId)
            ?: return Result.failure(IllegalStateException("Printer profile not found."))
    return orchestrator.print(profile, input.document)
  }
}

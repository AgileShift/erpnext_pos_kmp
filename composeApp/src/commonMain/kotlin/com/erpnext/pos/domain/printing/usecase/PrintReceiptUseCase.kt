package com.erpnext.pos.domain.printing.usecase

import com.erpnext.pos.domain.printing.model.PrintExecutionResult
import com.erpnext.pos.domain.printing.model.ReceiptDocument
import com.erpnext.pos.domain.repositories.printing.IPrinterProfileRepository
import com.erpnext.pos.domain.usecases.UseCase
import com.erpnext.pos.printing.application.PrintOrchestrator

data class PrintReceiptInput(
    val profileId: String,
    val document: ReceiptDocument,
)

class PrintReceiptUseCase(
    private val profileRepository: IPrinterProfileRepository,
    private val orchestrator: PrintOrchestrator,
) : UseCase<PrintReceiptInput, Result<PrintExecutionResult>>() {
  override suspend fun useCaseFunction(input: PrintReceiptInput): Result<PrintExecutionResult> {
    val profile =
        profileRepository.getById(input.profileId)
            ?: return Result.failure(IllegalStateException("Printer profile not found."))

    return orchestrator.print(
        profile = profile,
        document = input.document,
    )
  }
}

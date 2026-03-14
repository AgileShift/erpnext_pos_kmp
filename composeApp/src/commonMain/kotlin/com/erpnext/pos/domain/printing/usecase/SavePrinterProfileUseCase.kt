package com.erpnext.pos.domain.printing.usecase

import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.repositories.printing.IPrinterProfileRepository
import com.erpnext.pos.domain.usecases.UseCase

class SavePrinterProfileUseCase(
    private val repository: IPrinterProfileRepository,
) : UseCase<PrinterProfile, Unit>() {
  override suspend fun useCaseFunction(input: PrinterProfile) {
    repository.save(input)
    if (input.isDefault) {
      repository.setDefault(input.id)
    }
  }
}

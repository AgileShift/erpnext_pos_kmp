package com.erpnext.pos.domain.printing.usecase

import com.erpnext.pos.domain.repositories.printing.IPrinterProfileRepository
import com.erpnext.pos.domain.usecases.UseCase

class SetDefaultPrinterUseCase(
    private val repository: IPrinterProfileRepository,
) : UseCase<String, Unit>() {
  override suspend fun useCaseFunction(input: String) {
    repository.setDefault(input)
  }
}

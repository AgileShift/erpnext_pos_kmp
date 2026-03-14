package com.erpnext.pos.domain.repositories.printing

import com.erpnext.pos.domain.printing.model.PrinterProfile
import kotlinx.coroutines.flow.Flow

interface IPrinterProfileRepository {
  fun observeProfiles(): Flow<List<PrinterProfile>>

  fun observeDefaultProfile(): Flow<PrinterProfile?>

  suspend fun getById(id: String): PrinterProfile?

  suspend fun getDefaultProfile(): PrinterProfile?

  suspend fun save(profile: PrinterProfile)

  suspend fun setDefault(id: String)

  suspend fun delete(id: String)
}

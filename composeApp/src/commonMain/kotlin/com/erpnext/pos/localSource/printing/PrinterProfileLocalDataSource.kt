package com.erpnext.pos.localSource.printing

import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.localSource.dao.PrinterProfileDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrinterProfileLocalDataSource(
    private val dao: PrinterProfileDao,
) {
  fun observeProfiles(): Flow<List<PrinterProfile>> = dao.observeProfiles().map { list ->
    list.map { it.toDomain() }
  }

  fun observeDefaultProfile(): Flow<PrinterProfile?> = dao.observeDefaultProfile().map { it?.toDomain() }

  suspend fun getById(id: String): PrinterProfile? = dao.getById(id)?.toDomain()

  suspend fun getDefaultProfile(): PrinterProfile? = dao.getDefault()?.toDomain()

  suspend fun save(profile: PrinterProfile) {
    if (profile.isDefault) {
      dao.clearDefaultFlag()
    }
    dao.upsert(profile.toEntity())
  }

  suspend fun setDefault(id: String) {
    dao.setDefault(id)
  }

  suspend fun delete(id: String) {
    dao.delete(id)
  }
}

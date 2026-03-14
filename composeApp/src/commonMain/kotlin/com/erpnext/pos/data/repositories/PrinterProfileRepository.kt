package com.erpnext.pos.data.repositories

import com.erpnext.pos.domain.printing.model.PrinterProfile
import com.erpnext.pos.domain.repositories.printing.IPrinterProfileRepository
import com.erpnext.pos.localSource.printing.PrinterProfileLocalDataSource
import kotlinx.coroutines.flow.Flow

class PrinterProfileRepository(
    private val localDataSource: PrinterProfileLocalDataSource,
) : IPrinterProfileRepository {

  override fun observeProfiles(): Flow<List<PrinterProfile>> = localDataSource.observeProfiles()

  override fun observeDefaultProfile(): Flow<PrinterProfile?> = localDataSource.observeDefaultProfile()

  override suspend fun getById(id: String): PrinterProfile? = localDataSource.getById(id)

  override suspend fun getDefaultProfile(): PrinterProfile? = localDataSource.getDefaultProfile()

  override suspend fun save(profile: PrinterProfile) = localDataSource.save(profile)

  override suspend fun setDefault(id: String) = localDataSource.setDefault(id)

  override suspend fun delete(id: String) = localDataSource.delete(id)
}

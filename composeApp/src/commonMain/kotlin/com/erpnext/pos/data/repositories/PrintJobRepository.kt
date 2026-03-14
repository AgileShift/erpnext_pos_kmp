package com.erpnext.pos.data.repositories

import com.erpnext.pos.domain.printing.model.PrintJob
import com.erpnext.pos.domain.repositories.printing.IPrintJobRepository
import com.erpnext.pos.localSource.printing.PrintJobLocalDataSource
import kotlinx.coroutines.flow.Flow

class PrintJobRepository(
    private val localDataSource: PrintJobLocalDataSource,
) : IPrintJobRepository {
  override fun observeJobs(): Flow<List<PrintJob>> = localDataSource.observeJobs()

  override suspend fun enqueue(job: PrintJob) = localDataSource.enqueue(job)

  override suspend fun update(job: PrintJob) = localDataSource.update(job)

  override suspend fun getPendingJobs(): List<PrintJob> = localDataSource.getPendingJobs()

  override suspend fun latestError(): PrintJob? = localDataSource.latestError()
}

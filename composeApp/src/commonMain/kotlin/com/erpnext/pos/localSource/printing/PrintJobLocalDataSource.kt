package com.erpnext.pos.localSource.printing

import com.erpnext.pos.domain.printing.model.PrintJob
import com.erpnext.pos.localSource.dao.PrintJobDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PrintJobLocalDataSource(
    private val dao: PrintJobDao,
) {
  fun observeJobs(): Flow<List<PrintJob>> = dao.observeJobs().map { jobs -> jobs.map { it.toDomain() } }

  suspend fun enqueue(job: PrintJob) {
    dao.upsert(job.toEntity())
  }

  suspend fun update(job: PrintJob) {
    dao.upsert(job.toEntity())
  }

  suspend fun getPendingJobs(): List<PrintJob> = dao.getPendingJobs().map { it.toDomain() }

  suspend fun latestError(): PrintJob? = dao.latestError()?.toDomain()
}

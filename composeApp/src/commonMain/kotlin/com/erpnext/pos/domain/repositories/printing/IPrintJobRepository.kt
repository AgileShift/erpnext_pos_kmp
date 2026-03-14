package com.erpnext.pos.domain.repositories.printing

import com.erpnext.pos.domain.printing.model.PrintJob
import kotlinx.coroutines.flow.Flow

interface IPrintJobRepository {
  fun observeJobs(): Flow<List<PrintJob>>

  suspend fun enqueue(job: PrintJob)

  suspend fun update(job: PrintJob)

  suspend fun getPendingJobs(): List<PrintJob>

  suspend fun latestError(): PrintJob?
}

package com.erpnext.pos.printing.queue

import com.erpnext.pos.domain.printing.model.PrintJobStatus
import com.erpnext.pos.domain.repositories.printing.IPrintJobRepository
import com.erpnext.pos.domain.repositories.printing.IPrinterProfileRepository
import com.erpnext.pos.printing.application.PrintOrchestrator
import com.erpnext.pos.printing.policy.PrintRetryPolicy

class PrintQueueProcessor(
    private val jobRepository: IPrintJobRepository,
    private val profileRepository: IPrinterProfileRepository,
    private val orchestrator: PrintOrchestrator,
    private val retryPolicy: PrintRetryPolicy
) {
    suspend fun processPendingJobs() {
        val jobs = jobRepository.getPendingJobs()

        jobs.forEach { job ->
            val profile = profileRepository.getById(job.profileId)
            if (profile == null) {
                jobRepository.update(
                    job.copy(
                        status = PrintJobStatus.FAILED,
                        lastError = "Perfil de impresora no encontrado."
                    )
                )
                return@forEach
            }

            jobRepository.update(job.copy(status = PrintJobStatus.PRINTING))

            val result = orchestrator.print(
                profile = profile,
                document = error("Queued job document replay is not yet implemented.")
            )

            if (result.isSuccess) {
                jobRepository.update(job.copy(status = PrintJobStatus.SUCCESS))
            } else {
                val attempts = job.attempts + 1
                val shouldRetry = retryPolicy.shouldRetry(attempts)

                jobRepository.update(
                    job.copy(
                        attempts = attempts,
                        status = if (shouldRetry) PrintJobStatus.RETRYING else PrintJobStatus.FAILED,
                        lastError = result.exceptionOrNull()?.message
                    )
                )
            }
        }
    }
}

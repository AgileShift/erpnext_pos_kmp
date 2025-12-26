package com.erpnext.pos.domain.usecases.v2.sync

import com.erpnext.pos.data.repositories.v2.CustomerRepository
import com.erpnext.pos.data.repositories.v2.SyncRepository
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.domain.sync.SyncDocType
import com.erpnext.pos.domain.sync.SyncUnitResult
import com.erpnext.pos.domain.usecases.UseCase
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.remoteSource.dto.v2.CustomerCreateDto
import com.erpnext.pos.remoteSource.sdk.v2.ERPDocType
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SyncCustomersUseCase(
    private val repository: CustomerRepository,
    private val api: APIServiceV2,
    private val syncRepository: SyncRepository
) : UseCase<SyncContext, SyncUnitResult>() {

    override suspend fun useCaseFunction(input: SyncContext): SyncUnitResult {
        val docType = SyncDocType.CUSTOMER.value
        syncRepository.setInProgress(input.instanceId, input.companyId, docType, true)
        return try {
            val pulled = repository.pull(input)
            syncRepository.markPullSuccess(input.instanceId, input.companyId, docType)

            val pending = repository.pushPending(input)
            var hadFailure = false
            pending.forEach { item ->
                try {
                    val response =
                        api.createDoc<CustomerCreateDto>(ERPDocType.Customer, item.payload)
                    repository.markCustomerSynced(
                        input.instanceId,
                        input.companyId,
                        item.localId,
                        response.name,
                        response.modified
                    )
                } catch (e: Throwable) {
                    repository.markFailed(input.instanceId, input.companyId, item.localId)
                    hadFailure = true
                }
            }

            if (hadFailure) {
                throw IllegalStateException("Customer sync had failures.")
            }

            if (pending.isNotEmpty()) {
                syncRepository.markPushSuccess(input.instanceId, input.companyId, docType)
            }

            syncRepository.refreshCounters(
                input.instanceId,
                input.companyId,
                docType,
                Clock.System.now().epochSeconds
            )
            SyncUnitResult(success = true, changed = pulled || pending.isNotEmpty())
        } catch (e: Throwable) {
            syncRepository.markFailure(input.instanceId, input.companyId, docType, e)
            SyncUnitResult(success = false, changed = false, error = e)
        } finally {
            syncRepository.setInProgress(input.instanceId, input.companyId, docType, false)
        }
    }
}

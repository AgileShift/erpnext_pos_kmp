package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.sync.PendingSync
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.localSource.dao.v2.PaymentEntryDao
import com.erpnext.pos.localSource.entities.v2.PaymentEntryEntity
import com.erpnext.pos.localSource.entities.v2.PaymentEntryReferenceEntity
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryCreateDto
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryReferenceCreateDto
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class PaymentEntryRepository(
    private val paymentEntryDao: PaymentEntryDao,
    private val customerRepository: CustomerRepository
) {

    suspend fun pull(ctx: SyncContext): Boolean {
        return true
    }

    suspend fun pushPending(ctx: SyncContext): List<PendingSync<PaymentEntryCreateDto>> {
        return buildPendingCreatePayloads(ctx.instanceId, ctx.companyId)
    }

    suspend fun insertPaymentEntryWithReferences(
        entry: PaymentEntryEntity,
        references: List<PaymentEntryReferenceEntity>
    ) {
        paymentEntryDao.insertPaymentEntryWithReferences(entry, references)
    }

    suspend fun getPendingPaymentEntriesWithReferences(
        instanceId: String,
        companyId: String
    ) = paymentEntryDao.getPendingPaymentEntriesWithReferences(instanceId, companyId)

    @OptIn(ExperimentalTime::class)
    suspend fun markSynced(
        instanceId: String,
        companyId: String,
        paymentEntryId: String,
        remoteName: String?,
        remoteModified: String?
    ) {
        val now = Clock.System.now().epochSeconds
        paymentEntryDao.updateSyncStatus(
            instanceId,
            companyId,
            paymentEntryId,
            syncStatus = "SYNCED",
            lastSyncedAt = now,
            updatedAt = now
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun markFailed(
        instanceId: String,
        companyId: String,
        paymentEntryId: String
    ) {
        val now = Clock.System.now().epochSeconds
        paymentEntryDao.updateSyncStatus(
            instanceId,
            companyId,
            paymentEntryId,
            syncStatus = "FAILED",
            lastSyncedAt = null,
            updatedAt = now
        )
    }

    suspend fun buildPendingCreatePayloads(
        instanceId: String,
        companyId: String
    ): List<PendingSync<PaymentEntryCreateDto>> {
        return paymentEntryDao.getPendingPaymentEntriesWithReferences(instanceId, companyId).map { snapshot ->
            val partyId = if (snapshot.paymentEntry.partyType == "Customer") {
                customerRepository.resolveRemoteCustomerId(
                    instanceId,
                    companyId,
                    snapshot.paymentEntry.partyId
                )
            } else {
                snapshot.paymentEntry.partyId
            }
            PendingSync(
                localId = snapshot.paymentEntry.paymentEntryId,
                payload = PaymentEntryCreateDto(
                    company = snapshot.paymentEntry.company,
                    postingDate = snapshot.paymentEntry.postingDate,
                    paymentType = snapshot.paymentEntry.paymentType,
                    partyType = snapshot.paymentEntry.partyType,
                    partyId = partyId,
                    modeOfPayment = snapshot.paymentEntry.modeOfPayment.takeIf { it.isNotBlank() },
                    paidAmount = snapshot.paymentEntry.paidAmount,
                    receivedAmount = snapshot.paymentEntry.receivedAmount,
                    references = snapshot.references.map { reference ->
                        PaymentEntryReferenceCreateDto(
                            referenceDoctype = reference.referenceDoctype,
                            referenceName = reference.referenceName,
                            totalAmount = reference.totalAmount,
                            outstandingAmount = reference.outstandingAmount,
                            allocatedAmount = reference.allocatedAmount
                        )
                    }
                )
            )
        }
    }
}

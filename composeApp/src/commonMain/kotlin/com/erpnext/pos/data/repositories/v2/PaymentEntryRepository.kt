package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.localSource.dao.v2.PaymentEntryDao
import com.erpnext.pos.localSource.entities.v2.PaymentEntryEntity
import com.erpnext.pos.localSource.entities.v2.PaymentEntryReferenceEntity
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryCreateDto
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryReferenceCreateDto

class PaymentEntryRepository(
    private val paymentEntryDao: PaymentEntryDao
) {

    suspend fun pull(ctx: SyncContext): Boolean {
        return true
    }

    suspend fun pushPending(ctx: SyncContext): List<PaymentEntryCreateDto> {
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

    suspend fun buildPendingCreatePayloads(
        instanceId: String,
        companyId: String
    ): List<PaymentEntryCreateDto> {
        return paymentEntryDao.getPendingPaymentEntriesWithReferences(instanceId, companyId).map { snapshot ->
            PaymentEntryCreateDto(
                company = snapshot.paymentEntry.company,
                postingDate = snapshot.paymentEntry.postingDate,
                paymentType = snapshot.paymentEntry.paymentType,
                partyType = snapshot.paymentEntry.partyType,
                partyId = snapshot.paymentEntry.partyId,
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
        }
    }
}

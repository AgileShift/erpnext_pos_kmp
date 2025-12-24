package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.data.repositories.v2.CustomerRepository
import com.erpnext.pos.data.repositories.v2.PaymentEntryRepository
import com.erpnext.pos.domain.utils.UUIDGenerator
import com.erpnext.pos.localSource.dao.SyncStatus
import com.erpnext.pos.localSource.entities.v2.PaymentEntryEntity
import com.erpnext.pos.localSource.entities.v2.PaymentEntryReferenceEntity
import com.erpnext.pos.utils.view.DateTimeProvider

data class CreatePaymentEntryOfflineInput(
    val instanceId: String,
    val companyId: String,
    val partyType: String,
    val partyId: String,
    val paymentType: String,
    val modeOfPayment: String?,
    val paidAmount: Double,
    val receivedAmount: Double,
    val references: List<CreatePaymentEntryReferenceInput> = emptyList()
)

data class CreatePaymentEntryReferenceInput(
    val referenceDoctype: String,
    val referenceName: String,
    val totalAmount: Double? = null,
    val outstandingAmount: Double? = null,
    val allocatedAmount: Double
)

class CreatePaymentEntryOfflineUseCase(
    private val customerRepository: CustomerRepository,
    private val paymentEntryRepository: PaymentEntryRepository,
    private val idGenerator: UUIDGenerator
) {

    suspend operator fun invoke(input: CreatePaymentEntryOfflineInput): String {
        require(input.paidAmount >= 0.0) { "Invalid paid amount" }
        require(input.receivedAmount >= 0.0) { "Invalid received amount" }

        val customerTerritory = if (input.partyType == "Customer") {
            val customer = requireNotNull(
                customerRepository.getCustomerDetail(
                    input.instanceId,
                    input.companyId,
                    input.partyId
                )
            ) { "Customer not found: ${input.partyId}" }

            require(!customer.customer.disabled) {
                "Customer is disabled"
            }
            customer.customer.territory
        } else {
            null
        }

        val localPaymentEntryId = "LOCAL-${idGenerator.newId()}"
        val postingDate = DateTimeProvider.todayDate()

        val unallocated = input.paidAmount - input.references.sumOf { it.allocatedAmount }

        val paymentEntry = PaymentEntryEntity(
            paymentEntryId = localPaymentEntryId,
            postingDate = postingDate,
            company = input.companyId,
            territory = customerTerritory,
            paymentType = input.paymentType,
            modeOfPayment = input.modeOfPayment ?: "",
            partyType = input.partyType,
            partyId = input.partyId,
            paidAmount = input.paidAmount,
            receivedAmount = input.receivedAmount,
            unallocatedAmount = unallocated,
            syncStatus = SyncStatus.PENDING
        ).apply {
            instanceId = input.instanceId
            companyId = input.companyId
        }

        val referenceEntities = input.references.map { reference ->
            PaymentEntryReferenceEntity(
                paymentEntryId = localPaymentEntryId,
                referenceDoctype = reference.referenceDoctype,
                referenceName = reference.referenceName,
                totalAmount = reference.totalAmount ?: 0.0,
                outstandingAmount = reference.outstandingAmount ?: 0.0,
                allocatedAmount = reference.allocatedAmount
            ).apply {
                instanceId = input.instanceId
                companyId = input.companyId
            }
        }

        paymentEntryRepository.insertPaymentEntryWithReferences(
            entry = paymentEntry,
            references = referenceEntities
        )

        return localPaymentEntryId
    }
}

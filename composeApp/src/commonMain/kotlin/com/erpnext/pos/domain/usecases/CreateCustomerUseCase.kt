package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.CustomerSyncRepository
import com.erpnext.pos.domain.models.CustomerCreatePayload
import com.erpnext.pos.localSource.entities.CustomerEntity
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.views.CashBoxManager
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class CreateCustomerInput(
    val customerName: String,
    val customerType: String,
    val customerGroup: String? = null,
    val territory: String? = null,
    val isInternalCustomer: Boolean = false,
    val internalCompany: String? = null,
    val taxId: String? = null,
    val taxCategory: String? = null,
    val email: String? = null,
    val mobileNo: String? = null,
    val phone: String? = null,
    val addressLine1: String? = null,
    val addressLine2: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    val creditLimit: Double? = null,
    val paymentTerms: String? = null,
    val notes: String? = null
)

data class CreateCustomerResult(
    val localId: String
)

@OptIn(ExperimentalTime::class)
class CreateCustomerUseCase(
    private val syncRepository: CustomerSyncRepository,
    private val cashBoxManager: CashBoxManager
) : UseCase<CreateCustomerInput, CreateCustomerResult>() {
    override suspend fun useCaseFunction(input: CreateCustomerInput): CreateCustomerResult {
        val context = cashBoxManager.requireContext()
        if (input.isInternalCustomer && input.internalCompany.isNullOrBlank()) {
            error("Selecciona la compañía para el cliente interno.")
        }
        val localId = "LOCAL-CUST-${com.erpnext.pos.domain.utils.UUIDGenerator().newId()}"
        val territory = input.territory?.ifBlank { null }
            ?: context.territory
            ?: context.route
            ?: "All Territories"
        val customerGroup = input.customerGroup?.ifBlank { null } ?: "All Customer Groups"
        val payload = CustomerCreatePayload(
            customerName = input.customerName.trim(),
            customerType = input.customerType,
            customerGroup = customerGroup,
            territory = territory,
            defaultCurrency = normalizeCurrency(context.currency),
            defaultPriceList = context.priceList,
            mobileNo = input.mobileNo,
            email = input.email,
            phone = input.phone,
            taxId = input.taxId,
            taxCategory = input.taxCategory,
            isInternalCustomer = input.isInternalCustomer,
            representsCompany = input.internalCompany?.ifBlank { null },
            creditLimit = input.creditLimit,
            paymentTerms = input.paymentTerms,
            notes = input.notes,
            address = CustomerCreatePayload.Address(
                line1 = input.addressLine1,
                line2 = input.addressLine2,
                city = input.city,
                state = input.state,
                country = input.country
            ),
            contact = CustomerCreatePayload.Contact(
                email = input.email,
                mobile = input.mobileNo,
                phone = input.phone
            )
        )
        val entity = CustomerEntity(
            name = localId,
            customerName = payload.customerName,
            territory = payload.territory,
            email = payload.email,
            mobileNo = payload.mobileNo ?: payload.phone,
            customerType = payload.customerType,
            creditLimit = payload.creditLimit,
            currentBalance = 0.0,
            totalPendingAmount = 0.0,
            pendingInvoicesCount = 0,
            availableCredit = payload.creditLimit,
            image = null,
            address = payload.address?.line1,
            state = "Sin Pendientes",
            lastSyncedAt = Clock.System.now().toEpochMilliseconds()
        )
        syncRepository.enqueueCustomer(entity, payload)
        return CreateCustomerResult(localId = localId)
    }
}

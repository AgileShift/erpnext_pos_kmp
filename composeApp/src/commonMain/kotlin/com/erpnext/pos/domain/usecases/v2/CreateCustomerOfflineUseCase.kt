package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.data.repositories.v2.CustomerRepository
import com.erpnext.pos.domain.utils.UUIDGenerator
import com.erpnext.pos.localSource.dao.SyncStatus
import com.erpnext.pos.localSource.entities.v2.CustomerAddressEntity
import com.erpnext.pos.localSource.entities.v2.CustomerContactEntity
import com.erpnext.pos.localSource.entities.v2.CustomerEntity

data class CreateCustomerOfflineInput(
    val instanceId: String,
    val companyId: String,
    val customerName: String,
    val customerType: String,
    val territoryId: String,
    val territoryName: String? = null,
    val customerGroup: String? = null,
    val defaultCurrency: String? = null,
    val defaultPriceList: String? = null,
    val mobileNo: String? = null,
    val creditLimit: Double? = null,
    val paymentTerms: String? = null,
    val contact: CreateCustomerContactInput? = null,
    val address: CreateCustomerAddressInput? = null
)

data class CreateCustomerContactInput(
    val firstName: String,
    val lastName: String? = null,
    val emailId: String? = null,
    val phone: String? = null,
    val mobileNo: String? = null,
    val isPrimary: Boolean = true
)

data class CreateCustomerAddressInput(
    val addressTitle: String,
    val addressType: String,
    val line1: String,
    val line2: String? = null,
    val city: String,
    val county: String? = null,
    val state: String? = null,
    val country: String? = null,
    val pinCode: String? = null,
    val isPrimary: Boolean = true,
    val isShipping: Boolean = true
)

class CreateCustomerOfflineUseCase(
    private val customerRepository: CustomerRepository,
    private val idGenerator: UUIDGenerator
) {

    suspend operator fun invoke(input: CreateCustomerOfflineInput): String {
        require(input.customerName.isNotBlank()) { "Customer name is required" }
        require(input.customerType.isNotBlank()) { "Customer type is required" }
        require(input.territoryId.isNotBlank()) { "Territory is required" }

        val localCustomerId = "LOCAL-${idGenerator.newId()}"

        val customer = CustomerEntity(
            customerId = localCustomerId,
            customerName = input.customerName,
            territoryId = input.territoryId,
            customerType = input.customerType,
            customerGroup = input.customerGroup,
            territory = input.territoryName ?: input.territoryId,
            creditLimit = input.creditLimit,
            paymentTerms = input.paymentTerms,
            defaultCurrency = input.defaultCurrency,
            defaultPriceList = input.defaultPriceList,
            primaryAddress = null,
            mobileNo = input.mobileNo,
            salesTeam = null,
            outstandingAmount = 0.0,
            overdueAmount = 0.0,
            unpaidInvoiceCount = 0,
            lastPaidDate = null,
            syncStatus = SyncStatus.PENDING
        ).apply {
            instanceId = input.instanceId
            companyId = input.companyId
        }

        val contactEntity = input.contact?.let { contact ->
            val contactId = "CONTACT-${idGenerator.newId()}"
            CustomerContactEntity(
                contactId = contactId,
                customerId = localCustomerId,
                firstName = contact.firstName,
                lastName = contact.lastName,
                fullName = listOfNotNull(contact.firstName, contact.lastName)
                    .joinToString(" ")
                    .ifBlank { contact.firstName },
                emailId = contact.emailId,
                phone = contact.phone,
                mobileNo = contact.mobileNo,
                isPrimary = contact.isPrimary,
                status = "Active"
            ).apply {
                instanceId = input.instanceId
                companyId = input.companyId
            }
        }

        val addressEntity = input.address?.let { address ->
            val addressId = "ADDRESS-${idGenerator.newId()}"
            CustomerAddressEntity(
                addressId = addressId,
                customerId = localCustomerId,
                addressTitle = address.addressTitle,
                addressType = address.addressType,
                line1 = address.line1,
                line2 = address.line2,
                city = address.city,
                county = address.county,
                state = address.state,
                country = address.country,
                pinCode = address.pinCode,
                isPrimary = address.isPrimary,
                isShipping = address.isShipping,
                disabled = false
            ).apply {
                instanceId = input.instanceId
                companyId = input.companyId
            }
        }

        customerRepository.insertCustomerWithDetails(
            customer = customer,
            contacts = listOfNotNull(contactEntity),
            addresses = listOfNotNull(addressEntity)
        )

        return localCustomerId
    }
}

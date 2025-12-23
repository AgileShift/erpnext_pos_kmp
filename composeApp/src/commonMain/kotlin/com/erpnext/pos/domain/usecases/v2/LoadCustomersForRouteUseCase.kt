package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.cache.CustomerCache
import com.erpnext.pos.domain.usecases.UseCase
import com.erpnext.pos.localSource.dao.v2.CustomerDao
import com.erpnext.pos.localSource.dao.v2.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.v2.CustomerAddressEntity
import com.erpnext.pos.localSource.entities.v2.CustomerContactEntity
import com.erpnext.pos.remoteSource.dto.v2.CustomerAddressDto
import com.erpnext.pos.remoteSource.dto.v2.CustomerContactDto
import com.erpnext.pos.remoteSource.dto.v2.CustomerCreditDto
import com.erpnext.pos.remoteSource.dto.v2.CustomerSnapshot
import kotlin.jvm.JvmName

data class LoadCustomersForRouteInput(
    val instanceId: String,
    val companyId: String,
    val territoryId: String
)

class LoadCustomersForRouteUseCase(
    private val customerDao: CustomerDao,
    private val salesInvoiceDao: SalesInvoiceDao,
    private val cache: CustomerCache
) : UseCase<LoadCustomersForRouteInput, List<CustomerSnapshot>>() {

    override suspend fun useCaseFunction(input: LoadCustomersForRouteInput): List<CustomerSnapshot> {
        val cacheKey = "${input.instanceId}|${input.companyId}|${input.territoryId}"

        cache.get(cacheKey)?.let { return it }

        val customers =
            customerDao.getCustomersForTerritory(
                instanceId = input.instanceId,
                companyId = input.companyId,
                territoryId = input.territoryId
            )

        val agingRows =
            salesInvoiceDao.getCustomerAgingForTerritory(
                input.instanceId,
                input.companyId,
                input.territoryId
            )

        val agingByCustomerId = agingRows.associateBy { it.customerId }

        return customers.map { customerWithRelations ->

            val customer = customerWithRelations.customer
            val aging = agingByCustomerId[customer.customerId]

            val outstanding = (aging?.outstanding ?: 0f).toDouble()
            val overdue = (aging?.overdue ?: 0f).toDouble()
            val lastPurchaseDate = aging?.lastPurchaseDate

            val creditLimit = customer.creditLimit?.toDouble() ?: 0.0

            val creditDto = CustomerCreditDto(
                creditLimit = creditLimit,
                outstanding = outstanding,
                overdue = overdue,
                availableCredit = creditLimit - outstanding
            )

            val snapshot = CustomerSnapshot(
                customerId = customer.customerId,
                customerName = customer.customerName,
                territoryId = customer.territoryId,
                mobileFallback = customer.mobileNo,
                credit = creditDto,
                primaryContact = customerWithRelations.contacts
                    .firstOrNull { it.isPrimary }
                    ?.toDto(),
                primaryAddress = customerWithRelations.addresses
                    .firstOrNull { it.isPrimary }
                    ?.toDto(),
                creditLimit = creditLimit,
                outstandingAmount = outstanding,
                overdueAmount = overdue,
                primaryPhone = customer.mobileNo,
                lastPurchaseDate = lastPurchaseDate
            )
            
            cache.put(cacheKey, listOf(snapshot))

            snapshot
        }
    }
}

fun CustomerContactEntity.toDto(): CustomerContactDto {
    return CustomerContactDto(
        name = this.fullName,
        phone = this.phone,
        mobile = this.mobileNo,
        email = this.emailId
    )
}

@JvmName("ListCustomerContactEntityToDto")
fun List<CustomerContactEntity>.toDto(): List<CustomerContactDto> {
    return this.map { it.toDto() }
}

fun CustomerAddressEntity.toDto(): CustomerAddressDto {
    return CustomerAddressDto(
        addressId = this.addressId,
        title = this.addressTitle,
        type = this.addressType,
        line1 = this.line1,
        line2 = this.line2,
        city = this.city,
        country = this.country ?: "Nicaragua",
    )
}

@JvmName("ListCustomerAddressEntityToDto")
fun List<CustomerAddressEntity>.toDto(): List<CustomerAddressDto> {
    return this.map { it.toDto() }
}
package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.repositories.v2.ICustomerRepository
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.localSource.dao.v2.CustomerDao
import com.erpnext.pos.localSource.entities.v2.CustomerAddressEntity
import com.erpnext.pos.localSource.entities.v2.CustomerContactEntity
import com.erpnext.pos.localSource.entities.v2.CustomerEntity
import com.erpnext.pos.remoteSource.dto.v2.CustomerCreateDto

class CustomerRepository(
    private val customerDao: CustomerDao
) : ICustomerRepository {

    suspend fun pull(ctx: SyncContext): Boolean {
        return true
    }

    suspend fun pushPending(ctx: SyncContext): List<CustomerCreateDto> {
        return buildPendingCreatePayloads(ctx.instanceId, ctx.companyId)
    }

    override suspend fun getCustomersForTerritory(
        instanceId: String,
        companyId: String,
        territoryId: String
    ) = customerDao.getCustomersWithContactsAndAddressesForTerritory(
        instanceId,
        companyId,
        territoryId
    )

    override suspend fun getCustomerDetail(
        instanceId: String,
        companyId: String,
        customerId: String
    ) = customerDao.getCustomerWithContactsAndAddresses(
        instanceId,
        companyId,
        customerId
    )

    suspend fun insertCustomerWithDetails(
        customer: CustomerEntity,
        contacts: List<CustomerContactEntity>,
        addresses: List<CustomerAddressEntity>
    ) {
        customerDao.insertCustomerWithDetails(customer, contacts, addresses)
    }

    suspend fun getPendingCustomersWithDetails(
        instanceId: String,
        companyId: String
    ) = customerDao.getPendingCustomersWithContactsAndAddresses(instanceId, companyId)

    suspend fun buildPendingCreatePayloads(
        instanceId: String,
        companyId: String
    ): List<CustomerCreateDto> {
        return customerDao.getPendingCustomersWithContactsAndAddresses(instanceId, companyId).map { snapshot ->
            CustomerCreateDto(
                customerName = snapshot.customer.customerName,
                customerType = snapshot.customer.customerType,
                territory = snapshot.customer.territory,
                customerGroup = snapshot.customer.customerGroup,
                defaultCurrency = snapshot.customer.defaultCurrency,
                defaultPriceList = snapshot.customer.defaultPriceList,
                mobileNo = snapshot.customer.mobileNo
            )
        }
    }
}

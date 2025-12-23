package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.repositories.v2.ICustomerRepository
import com.erpnext.pos.localSource.dao.v2.CustomerDao

class CustomerRepository(
    private val customerDao: CustomerDao
) : ICustomerRepository {

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
}

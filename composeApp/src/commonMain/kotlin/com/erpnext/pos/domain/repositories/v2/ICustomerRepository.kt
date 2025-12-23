package com.erpnext.pos.domain.repositories.v2

import com.erpnext.pos.localSource.relations.v2.CustomerWithContactsAndAddresses

interface ICustomerRepository {
    suspend fun getCustomersForTerritory(
        instanceId: String,
        companyId: String,
        territoryId: String
    ): List<CustomerWithContactsAndAddresses>

    suspend fun getCustomerDetail(
        instanceId: String,
        companyId: String,
        customerId: String
    ) : CustomerWithContactsAndAddresses?
}
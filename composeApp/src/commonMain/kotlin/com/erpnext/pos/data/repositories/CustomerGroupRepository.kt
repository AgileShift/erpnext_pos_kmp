package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.CustomerGroupBO
import com.erpnext.pos.localSource.datasources.CustomerGroupLocalSource
import com.erpnext.pos.utils.RepoTrace

class CustomerGroupRepository(
    private val localSource: CustomerGroupLocalSource
) {
    suspend fun getLocalCustomerGroups(): List<CustomerGroupBO> {
        RepoTrace.breadcrumb("CustomerGroupRepository", "getLocalCustomerGroups")
        return localSource.getAll().map { it.toBO() }
    }
}

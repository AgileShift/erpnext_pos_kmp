package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.CustomerGroupRepository
import com.erpnext.pos.domain.models.CustomerGroupBO

class FetchCustomerGroupsLocalUseCase(
    private val repository: CustomerGroupRepository
) {
    suspend operator fun invoke(): List<CustomerGroupBO> {
        return repository.getLocalCustomerGroups()
    }
}

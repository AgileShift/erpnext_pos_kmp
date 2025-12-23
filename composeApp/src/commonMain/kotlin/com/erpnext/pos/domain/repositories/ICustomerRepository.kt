package com.erpnext.pos.domain.repositories

import androidx.paging.PagingData
import com.erpnext.pos.base.Resource
import com.erpnext.pos.domain.models.CustomerBO
import kotlinx.coroutines.flow.Flow

interface ICustomerRepository {
    suspend fun getCustomers(search: String? = null, state: String? = null): Flow<List<CustomerBO>>
    suspend fun getCustomerByName(name: String): CustomerBO?
    suspend fun sync(): Flow<Resource<List<CustomerBO>>>
}
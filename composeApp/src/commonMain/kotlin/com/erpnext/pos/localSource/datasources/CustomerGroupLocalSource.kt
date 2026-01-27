package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.CustomerGroupDao
import com.erpnext.pos.localSource.entities.CustomerGroupEntity

interface ICustomerGroupLocalSource {
    suspend fun getAll(): List<CustomerGroupEntity>
    suspend fun insertAll(groups: List<CustomerGroupEntity>)
    suspend fun softDeleteMissing(names: List<String>)
    suspend fun hardDeleteDeletedMissing(names: List<String>)
    suspend fun softDeleteAll()
    suspend fun hardDeleteAllDeleted()
    suspend fun getOldest(): CustomerGroupEntity?
}

class CustomerGroupLocalSource(private val dao: CustomerGroupDao) : ICustomerGroupLocalSource {
    override suspend fun getAll(): List<CustomerGroupEntity> = dao.getAll()
    override suspend fun insertAll(groups: List<CustomerGroupEntity>) = dao.insertAll(groups)
    override suspend fun softDeleteMissing(names: List<String>) = dao.softDeleteNotIn(names)
    override suspend fun hardDeleteDeletedMissing(names: List<String>) = dao.hardDeleteDeletedNotIn(names)
    override suspend fun softDeleteAll() = dao.softDeleteAll()
    override suspend fun hardDeleteAllDeleted() = dao.hardDeleteAllDeleted()
    override suspend fun getOldest(): CustomerGroupEntity? = dao.getOldest()
}

package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.AddressDao
import com.erpnext.pos.localSource.entities.AddressEntity

interface IAddressLocalSource {
    suspend fun getAll(): List<AddressEntity>
    suspend fun insertAll(addresses: List<AddressEntity>)
    suspend fun softDeleteMissing(names: List<String>)
    suspend fun hardDeleteDeletedMissing(names: List<String>)
    suspend fun softDeleteAll()
    suspend fun hardDeleteAllDeleted()
    suspend fun getOldest(): AddressEntity?
}

class AddressLocalSource(private val dao: AddressDao) : IAddressLocalSource {
    override suspend fun getAll(): List<AddressEntity> = dao.getAll()
    override suspend fun insertAll(addresses: List<AddressEntity>) = dao.insertAll(addresses)
    override suspend fun softDeleteMissing(names: List<String>) = dao.softDeleteNotIn(names)
    override suspend fun hardDeleteDeletedMissing(names: List<String>) = dao.hardDeleteDeletedNotIn(names)
    override suspend fun softDeleteAll() = dao.softDeleteAll()
    override suspend fun hardDeleteAllDeleted() = dao.hardDeleteAllDeleted()
    override suspend fun getOldest(): AddressEntity? = dao.getOldest()
}

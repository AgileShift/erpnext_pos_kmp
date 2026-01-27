package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.ContactDao
import com.erpnext.pos.localSource.entities.ContactEntity

interface IContactLocalSource {
    suspend fun getAll(): List<ContactEntity>
    suspend fun insertAll(contacts: List<ContactEntity>)
    suspend fun softDeleteMissing(names: List<String>)
    suspend fun hardDeleteDeletedMissing(names: List<String>)
    suspend fun softDeleteAll()
    suspend fun hardDeleteAllDeleted()
    suspend fun getOldest(): ContactEntity?
}

class ContactLocalSource(private val dao: ContactDao) : IContactLocalSource {
    override suspend fun getAll(): List<ContactEntity> = dao.getAll()
    override suspend fun insertAll(contacts: List<ContactEntity>) = dao.insertAll(contacts)
    override suspend fun softDeleteMissing(names: List<String>) = dao.softDeleteNotIn(names)
    override suspend fun hardDeleteDeletedMissing(names: List<String>) = dao.hardDeleteDeletedNotIn(names)
    override suspend fun softDeleteAll() = dao.softDeleteAll()
    override suspend fun hardDeleteAllDeleted() = dao.hardDeleteAllDeleted()
    override suspend fun getOldest(): ContactEntity? = dao.getOldest()
}

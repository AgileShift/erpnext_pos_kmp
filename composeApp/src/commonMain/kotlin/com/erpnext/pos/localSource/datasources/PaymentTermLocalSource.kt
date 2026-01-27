package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.PaymentTermDao
import com.erpnext.pos.localSource.entities.PaymentTermEntity

interface IPaymentTermLocalSource {
    suspend fun getAll(): List<PaymentTermEntity>
    suspend fun insertAll(terms: List<PaymentTermEntity>)
    suspend fun softDeleteMissing(names: List<String>)
    suspend fun hardDeleteDeletedMissing(names: List<String>)
    suspend fun softDeleteAll()
    suspend fun hardDeleteAllDeleted()
    suspend fun getOldest(): PaymentTermEntity?
}

class PaymentTermLocalSource(private val dao: PaymentTermDao) : IPaymentTermLocalSource {
    override suspend fun getAll(): List<PaymentTermEntity> = dao.getAll()
    override suspend fun insertAll(terms: List<PaymentTermEntity>) = dao.insertAll(terms)
    override suspend fun softDeleteMissing(names: List<String>) = dao.softDeleteNotIn(names)
    override suspend fun hardDeleteDeletedMissing(names: List<String>) = dao.hardDeleteDeletedNotIn(names)
    override suspend fun softDeleteAll() = dao.softDeleteAll()
    override suspend fun hardDeleteAllDeleted() = dao.hardDeleteAllDeleted()
    override suspend fun getOldest(): PaymentTermEntity? = dao.getOldest()
}

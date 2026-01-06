package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.PaymentTermDao
import com.erpnext.pos.localSource.entities.PaymentTermEntity

interface IPaymentTermLocalSource {
    suspend fun getAll(): List<PaymentTermEntity>
    suspend fun insertAll(terms: List<PaymentTermEntity>)
    suspend fun deleteAll()
    suspend fun getOldest(): PaymentTermEntity?
}

class PaymentTermLocalSource(private val dao: PaymentTermDao) : IPaymentTermLocalSource {
    override suspend fun getAll(): List<PaymentTermEntity> = dao.getAll()
    override suspend fun insertAll(terms: List<PaymentTermEntity>) = dao.insertAll(terms)
    override suspend fun deleteAll() = dao.deleteAll()
    override suspend fun getOldest(): PaymentTermEntity? = dao.getOldest()
}

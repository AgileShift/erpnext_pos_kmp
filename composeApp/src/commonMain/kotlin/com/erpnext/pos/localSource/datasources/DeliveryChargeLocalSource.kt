package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.DeliveryChargeDao
import com.erpnext.pos.localSource.entities.DeliveryChargeEntity

interface IDeliveryChargeLocalSource {
    suspend fun getAll(): List<DeliveryChargeEntity>
    suspend fun insertAll(charges: List<DeliveryChargeEntity>)
    suspend fun deleteAll()
    suspend fun getOldest(): DeliveryChargeEntity?
}

class DeliveryChargeLocalSource(
    private val dao: DeliveryChargeDao
) : IDeliveryChargeLocalSource {
    override suspend fun getAll(): List<DeliveryChargeEntity> = dao.getAll()
    override suspend fun insertAll(charges: List<DeliveryChargeEntity>) = dao.insertAll(charges)
    override suspend fun deleteAll() = dao.deleteAll()
    override suspend fun getOldest(): DeliveryChargeEntity? = dao.getOldest()
}

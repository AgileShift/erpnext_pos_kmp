package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.DeliveryChargeDao
import com.erpnext.pos.localSource.entities.DeliveryChargeEntity

interface IDeliveryChargeLocalSource {
    suspend fun getAll(): List<DeliveryChargeEntity>
    suspend fun insertAll(charges: List<DeliveryChargeEntity>)
    suspend fun softDeleteMissing(labels: List<String>)
    suspend fun hardDeleteDeletedMissing(labels: List<String>)
    suspend fun softDeleteAll()
    suspend fun hardDeleteAllDeleted()
    suspend fun getOldest(): DeliveryChargeEntity?
}

class DeliveryChargeLocalSource(
    private val dao: DeliveryChargeDao
) : IDeliveryChargeLocalSource {
    override suspend fun getAll(): List<DeliveryChargeEntity> = dao.getAll()
    override suspend fun insertAll(charges: List<DeliveryChargeEntity>) = dao.insertAll(charges)
    override suspend fun softDeleteMissing(labels: List<String>) = dao.softDeleteNotIn(labels)
    override suspend fun hardDeleteDeletedMissing(labels: List<String>) = dao.hardDeleteDeletedNotIn(labels)
    override suspend fun softDeleteAll() = dao.softDeleteAll()
    override suspend fun hardDeleteAllDeleted() = dao.hardDeleteAllDeleted()
    override suspend fun getOldest(): DeliveryChargeEntity? = dao.getOldest()
}

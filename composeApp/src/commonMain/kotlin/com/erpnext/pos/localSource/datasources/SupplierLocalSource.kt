package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.SupplierDao
import com.erpnext.pos.localSource.entities.SupplierEntity

class SupplierLocalSource(private val dao: SupplierDao) {
    suspend fun getAllActive(): List<SupplierEntity> = dao.getAllActive()
    suspend fun insertAll(items: List<SupplierEntity>) = dao.insertAll(items)
    suspend fun softDeleteMissing(names: List<String>) = dao.softDeleteNotIn(names)
    suspend fun hardDeleteDeletedMissing(names: List<String>) = dao.hardDeleteDeletedNotIn(names)
    suspend fun softDeleteAll() = dao.softDeleteAll()
    suspend fun hardDeleteAllDeleted() = dao.hardDeleteAllDeleted()
}

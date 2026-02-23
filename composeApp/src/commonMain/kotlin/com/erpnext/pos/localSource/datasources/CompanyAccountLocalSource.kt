package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.CompanyAccountDao
import com.erpnext.pos.localSource.entities.CompanyAccountEntity

class CompanyAccountLocalSource(private val dao: CompanyAccountDao) {
    suspend fun getAllActive(): List<CompanyAccountEntity> = dao.getAllActive()
    suspend fun insertAll(items: List<CompanyAccountEntity>) = dao.insertAll(items)
    suspend fun softDeleteMissing(names: List<String>) = dao.softDeleteNotIn(names)
    suspend fun hardDeleteDeletedMissing(names: List<String>) = dao.hardDeleteDeletedNotIn(names)
    suspend fun softDeleteAll() = dao.softDeleteAll()
    suspend fun hardDeleteAllDeleted() = dao.hardDeleteAllDeleted()
}

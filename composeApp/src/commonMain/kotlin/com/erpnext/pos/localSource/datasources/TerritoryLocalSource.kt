package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.TerritoryDao
import com.erpnext.pos.localSource.entities.TerritoryEntity

interface ITerritoryLocalSource {
    suspend fun getAll(): List<TerritoryEntity>
    suspend fun insertAll(territories: List<TerritoryEntity>)
    suspend fun softDeleteMissing(names: List<String>)
    suspend fun hardDeleteDeletedMissing(names: List<String>)
    suspend fun softDeleteAll()
    suspend fun hardDeleteAllDeleted()
    suspend fun getOldest(): TerritoryEntity?
}

class TerritoryLocalSource(private val dao: TerritoryDao) : ITerritoryLocalSource {
    override suspend fun getAll(): List<TerritoryEntity> = dao.getAll()
    override suspend fun insertAll(territories: List<TerritoryEntity>) = dao.insertAll(territories)
    override suspend fun softDeleteMissing(names: List<String>) = dao.softDeleteNotIn(names)
    override suspend fun hardDeleteDeletedMissing(names: List<String>) = dao.hardDeleteDeletedNotIn(names)
    override suspend fun softDeleteAll() = dao.softDeleteAll()
    override suspend fun hardDeleteAllDeleted() = dao.hardDeleteAllDeleted()
    override suspend fun getOldest(): TerritoryEntity? = dao.getOldest()
}

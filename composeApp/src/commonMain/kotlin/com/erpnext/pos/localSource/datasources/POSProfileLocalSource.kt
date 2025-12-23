package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.POSClosingEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.entities.POSClosingEntryEntity
import com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
import kotlinx.coroutines.flow.Flow

interface IPOSProfileLocalSource {
    suspend fun getClosingEntryPendingSync(): Flow<List<POSClosingEntryEntity>>
    suspend fun getOpeningEntryPendingSync(): Flow<List<POSOpeningEntryEntity>>
}

class POSProfileLocalSource(
    private val openingDao: POSOpeningEntryDao,
    private val closingDao: POSClosingEntryDao
) : IPOSProfileLocalSource {
    override suspend fun getClosingEntryPendingSync(): Flow<List<POSClosingEntryEntity>> {
        return closingDao.getAllPendingSync()
    }

    override suspend fun getOpeningEntryPendingSync(): Flow<List<POSOpeningEntryEntity>> {
        return openingDao.getAllPendingSync()
    }
}
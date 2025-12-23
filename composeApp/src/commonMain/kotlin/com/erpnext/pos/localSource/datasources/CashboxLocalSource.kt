package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.POSClosingEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.entities.CustomerEntity
import com.erpnext.pos.localSource.entities.POSClosingEntryEntity
import com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
import kotlinx.coroutines.flow.Flow

interface ICashboxLocalSource {
    suspend fun insertOpeningEntry(entry: POSOpeningEntryEntity)
    suspend fun insertClosingEntry(entry: POSClosingEntryEntity)
    suspend fun getActiveOpeningEntry(): Flow<POSOpeningEntryEntity>
    suspend fun closeCashbox(entry: POSClosingEntryEntity)
    suspend fun count(): Int
    // suspend fun getPendingSyncEntries(): Flow<List<>>
}

class CashboxLocalSource(
    private val openingDao: POSOpeningEntryDao,
    private val closingDao: POSClosingEntryDao
): ICashboxLocalSource {
    override suspend fun insertOpeningEntry(entry: POSOpeningEntryEntity) {
        TODO("Not yet implemented")
    }

    override suspend fun insertClosingEntry(entry: POSClosingEntryEntity) {
        TODO("Not yet implemented")
    }

    override suspend fun getActiveOpeningEntry(): Flow<POSOpeningEntryEntity> {
        TODO("Not yet implemented")
    }

    override suspend fun closeCashbox(entry: POSClosingEntryEntity) {
        TODO("Not yet implemented")
    }

    override suspend fun count(): Int {
        TODO("Not yet implemented")
    }
}
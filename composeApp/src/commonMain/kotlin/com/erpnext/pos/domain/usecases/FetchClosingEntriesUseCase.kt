package com.erpnext.pos.domain.usecases

import com.erpnext.pos.localSource.dao.POSClosingEntryDao
import com.erpnext.pos.localSource.entities.POSClosingEntryEntity
import kotlinx.coroutines.flow.Flow

class FetchClosingEntriesUseCase(
    private val closingEntryDao: POSClosingEntryDao
) : UseCase<Unit, Flow<List<POSClosingEntryEntity>>>() {
    override suspend fun useCaseFunction(input: Unit): Flow<List<POSClosingEntryEntity>> {
        return closingEntryDao.getAll()
    }
}

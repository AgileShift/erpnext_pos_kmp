package com.erpnext.pos.data.repositories

import com.erpnext.pos.base.Resource
import com.erpnext.pos.localSource.datasources.ModeOfPaymentLocalSource
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.utils.RepoTrace
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface IModeOfPaymentRepository {
    suspend fun sync(ttlHours: Int = 0): Flow<Resource<List<ModeOfPaymentEntity>>>
}

@OptIn(ExperimentalTime::class)
class ModeOfPaymentRepository(
    private val localSource: ModeOfPaymentLocalSource,
    private val context: CashBoxManager
) : IModeOfPaymentRepository {

    override suspend fun sync(
        ttlHours: Int
    ): Flow<Resource<List<ModeOfPaymentEntity>>> {
        RepoTrace.breadcrumb("ModeOfPaymentRepository", "sync", "local-only")
        return flow {
            emit(Resource.Loading(emptyList()))
            val company = context.requireContext().company
            val cached = localSource.getAllModes(company)
            if (cached.isNotEmpty()) {
                emit(Resource.Success(cached))
                return@flow
            }
            val now = Clock.System.now().toEpochMilliseconds()
            emit(Resource.Success(cached.map { it.copy(lastSyncedAt = now) }))
        }
    }
}

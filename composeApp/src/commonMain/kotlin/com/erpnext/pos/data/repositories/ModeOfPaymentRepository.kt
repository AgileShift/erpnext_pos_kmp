package com.erpnext.pos.data.repositories

import com.erpnext.pos.base.Resource
import com.erpnext.pos.base.networkBoundResource
import com.erpnext.pos.localSource.datasources.ModeOfPaymentLocalSource
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.remoteSource.datasources.ModeOfPaymentRemoteSource
import com.erpnext.pos.sync.SyncTTL
import com.erpnext.pos.utils.RepoTrace
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

interface IModeOfPaymentRepository {
    suspend fun sync(ttlHours: Int = SyncTTL.DEFAULT_TTL_HOURS): Flow<Resource<List<ModeOfPaymentEntity>>>
}

@OptIn(ExperimentalTime::class)
class ModeOfPaymentRepository(
    private val remoteSource: ModeOfPaymentRemoteSource,
    private val localSource: ModeOfPaymentLocalSource,
    private val context: CashBoxManager
) : IModeOfPaymentRepository {

    override suspend fun sync(
        ttlHours: Int
    ): Flow<Resource<List<ModeOfPaymentEntity>>> {
        RepoTrace.breadcrumb("ModeOfPaymentRepository", "sync", "ttl=$ttlHours")
        delay(1000)
        val company = context.requireContext().company
        return networkBoundResource(
            query = { flowOf(localSource.getAllModes(company)) },
            fetch = {
                RepoTrace.breadcrumb("ModeOfPaymentRepository", "fetch")
                val now = Clock.System.now().toEpochMilliseconds()
                val activeModes = remoteSource.getActiveModes()
                val results = mutableListOf<ModeOfPaymentEntity>()
                activeModes.forEach { mode ->
                    val detail = remoteSource.getModeDetail(mode.name) ?: return@forEach
                    val account = detail.accounts.firstOrNull { it.company == company }
                        ?.defaultAccount
                    val accountDetail = account?.let { remoteSource.getAccountDetail(it) }
                    results.add(
                        ModeOfPaymentEntity(
                            name = detail.name,
                            modeOfPayment = detail.modeOfPayment,
                            company = company,
                            type = accountDetail?.accountType ?: detail.type ?: "Cash",
                            enabled = detail.enabled,
                            currency = accountDetail?.accountCurrency,
                            account = account,
                            lastSyncedAt = now
                        )
                    )
                }
                results
            },
            saveFetchResult = { results ->
                localSource.insertAllModes(results)
                localSource.deleteMissing(company, results.map { it.name })
            },
            shouldFetch = { cached ->
                cached.isEmpty() ||
                    SyncTTL.isExpired(localSource.getLastSyncedAt(company), ttlHours)
            },
            onFetchFailed = { RepoTrace.capture("ModeOfPaymentRepository", "sync", it) }
        )
    }
}

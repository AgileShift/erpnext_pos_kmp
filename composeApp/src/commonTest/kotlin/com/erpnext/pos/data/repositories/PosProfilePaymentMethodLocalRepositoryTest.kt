package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.dao.PosProfilePaymentMethodDao
import com.erpnext.pos.localSource.dao.ResolvedPaymentMethod
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.entities.PosProfilePaymentMethodEntity
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PosProfilePaymentMethodLocalRepositoryTest {
    @Test
    fun groupsCashMethodsByCurrency() = runBlocking {
        val dao = InMemoryPosProfilePaymentMethodDao().apply {
            seedModeOfPayments(
                listOf(
                    ModeOfPaymentEntity(
                        name = "CASH_NIO",
                        modeOfPayment = "CASH_NIO",
                        company = "ACME",
                        type = "Cash",
                        enabled = true,
                        currency = "NIO",
                        account = "1110",
                        lastSyncedAt = 1
                    ),
                    ModeOfPaymentEntity(
                        name = "CASH_USD",
                        modeOfPayment = "CASH_USD",
                        company = "ACME",
                        type = "Cash",
                        enabled = true,
                        currency = "USD",
                        account = "1120",
                        lastSyncedAt = 1
                    )
                )
            )
            upsertAll(
                listOf(
                    PosProfilePaymentMethodEntity(
                        profileId = "POS-A",
                        mopName = "CASH_NIO",
                        idx = 0,
                        isDefault = true,
                        allowInReturns = false,
                        enabledInProfile = true,
                        lastSyncedAt = 1
                    ),
                    PosProfilePaymentMethodEntity(
                        profileId = "POS-A",
                        mopName = "CASH_USD",
                        idx = 1,
                        isDefault = false,
                        allowInReturns = false,
                        enabledInProfile = true,
                        lastSyncedAt = 1
                    )
                )
            )
        }

        val repo = PosProfilePaymentMethodLocalRepository(dao)
        val grouped = repo.getCashMethodsGroupedByCurrency("POS-A")
        assertEquals(2, grouped.size)
        assertTrue(grouped.containsKey("NIO"))
        assertTrue(grouped.containsKey("USD"))
    }

    @Test
    fun keepsRelationsIsolatedPerProfileOnCleanup() = runBlocking {
        val dao = InMemoryPosProfilePaymentMethodDao().apply {
            seedModeOfPayments(
                listOf(
                    ModeOfPaymentEntity(
                        name = "CASH_NIO",
                        modeOfPayment = "CASH_NIO",
                        company = "ACME",
                        type = "Cash",
                        enabled = true,
                        currency = "NIO",
                        account = "1110",
                        lastSyncedAt = 1
                    ),
                    ModeOfPaymentEntity(
                        name = "CASH_USD",
                        modeOfPayment = "CASH_USD",
                        company = "ACME",
                        type = "Cash",
                        enabled = true,
                        currency = "USD",
                        account = "1120",
                        lastSyncedAt = 1
                    )
                )
            )
            upsertAll(
                listOf(
                    PosProfilePaymentMethodEntity(
                        profileId = "POS-A",
                        mopName = "CASH_NIO",
                        idx = 0,
                        isDefault = true,
                        allowInReturns = false,
                        enabledInProfile = true,
                        lastSyncedAt = 1
                    ),
                    PosProfilePaymentMethodEntity(
                        profileId = "POS-B",
                        mopName = "CASH_USD",
                        idx = 0,
                        isDefault = true,
                        allowInReturns = false,
                        enabledInProfile = true,
                        lastSyncedAt = 1
                    )
                )
            )
        }

        dao.deleteStaleForProfile("POS-A", listOf("CASH_NIO"))

        assertEquals(1, dao.countRelationsForProfile("POS-A"))
        assertEquals(1, dao.countRelationsForProfile("POS-B"))
    }

    @Test
    fun countsResolvedMethodsAfterModeDetails() = runBlocking {
        val dao = InMemoryPosProfilePaymentMethodDao().apply {
            seedModeOfPayments(
                listOf(
                    ModeOfPaymentEntity(
                        name = "CASH_NIO",
                        modeOfPayment = "CASH_NIO",
                        company = "ACME",
                        type = "Cash",
                        enabled = true,
                        currency = "NIO",
                        account = "1110",
                        lastSyncedAt = 1
                    )
                )
            )
            upsertAll(
                listOf(
                    PosProfilePaymentMethodEntity(
                        profileId = "POS-A",
                        mopName = "CASH_NIO",
                        idx = 0,
                        isDefault = true,
                        allowInReturns = false,
                        enabledInProfile = true,
                        lastSyncedAt = 1
                    )
                )
            )
        }

        assertEquals(1, dao.countResolvedForProfile("POS-A"))
    }
}

private class InMemoryPosProfilePaymentMethodDao : PosProfilePaymentMethodDao {
    private val relations = mutableListOf<PosProfilePaymentMethodEntity>()
    private val modes = mutableMapOf<String, ModeOfPaymentEntity>()

    fun seedModeOfPayments(items: List<ModeOfPaymentEntity>) {
        items.forEach { modes[it.name] = it }
    }

    override suspend fun upsertAll(items: List<PosProfilePaymentMethodEntity>) {
        items.forEach { item ->
            relations.removeAll { it.profileId == item.profileId && it.mopName == item.mopName }
            relations.add(item)
        }
    }

    override suspend fun getResolvedMethodsForProfile(profileId: String): List<ResolvedPaymentMethod> {
        return relations.filter { it.profileId == profileId }
            .sortedBy { it.idx }
            .mapNotNull { relation ->
                val mode = modes[relation.mopName] ?: return@mapNotNull null
                ResolvedPaymentMethod(
                    mopName = relation.mopName,
                    type = mode.type,
                    enabled = mode.enabled,
                    isDefault = relation.isDefault,
                    currency = mode.currency,
                    account = mode.account,
                    allowInReturns = relation.allowInReturns,
                    idx = relation.idx,
                    enabledInProfile = relation.enabledInProfile
                )
            }
    }

    override suspend fun countResolvedForProfile(profileId: String): Int {
        return getResolvedMethodsForProfile(profileId).size
    }

    override suspend fun countRelationsForProfile(profileId: String): Int {
        return relations.count { it.profileId == profileId }
    }

    override suspend fun deleteStaleForProfile(profileId: String, activeMops: List<String>) {
        relations.removeAll { it.profileId == profileId && it.mopName !in activeMops }
    }

    override suspend fun deleteAllForProfile(profileId: String) {
        relations.removeAll { it.profileId == profileId }
    }
}

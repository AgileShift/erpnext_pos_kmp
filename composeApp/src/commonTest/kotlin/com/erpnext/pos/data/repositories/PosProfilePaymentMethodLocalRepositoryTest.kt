package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.dao.PosProfilePaymentMethodDao
import com.erpnext.pos.localSource.dao.ResolvedPaymentMethod
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.entities.PosProfilePaymentMethodEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

class PosProfilePaymentMethodLocalRepositoryTest {
  @Test
  fun groupsCashMethodsByCurrency() = runBlocking {
    val dao =
        InMemoryPosProfilePaymentMethodDao().apply {
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
                      lastSyncedAt = 1,
                  ),
                  ModeOfPaymentEntity(
                      name = "CASH_USD",
                      modeOfPayment = "CASH_USD",
                      company = "ACME",
                      type = "Cash",
                      enabled = true,
                      currency = "USD",
                      account = "1120",
                      lastSyncedAt = 1,
                  ),
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
                      lastSyncedAt = 1,
                  ),
                  PosProfilePaymentMethodEntity(
                      profileId = "POS-A",
                      mopName = "CASH_USD",
                      idx = 1,
                      isDefault = false,
                      allowInReturns = false,
                      enabledInProfile = true,
                      lastSyncedAt = 1,
                  ),
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
    val dao =
        InMemoryPosProfilePaymentMethodDao().apply {
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
                      lastSyncedAt = 1,
                  ),
                  ModeOfPaymentEntity(
                      name = "CASH_USD",
                      modeOfPayment = "CASH_USD",
                      company = "ACME",
                      type = "Cash",
                      enabled = true,
                      currency = "USD",
                      account = "1120",
                      lastSyncedAt = 1,
                  ),
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
                      lastSyncedAt = 1,
                  ),
                  PosProfilePaymentMethodEntity(
                      profileId = "POS-B",
                      mopName = "CASH_USD",
                      idx = 0,
                      isDefault = true,
                      allowInReturns = false,
                      enabledInProfile = true,
                      lastSyncedAt = 1,
                  ),
              )
          )
        }

    dao.softDeleteStaleForProfile("POS-A", listOf("CASH_NIO"))
    dao.hardDeleteDeletedStaleForProfile("POS-A", listOf("CASH_NIO"))

    assertEquals(1, dao.countRelationsForProfile("POS-A"))
    assertEquals(1, dao.countRelationsForProfile("POS-B"))
  }

  @Test
  fun countsResolvedMethodsAfterModeDetails() = runBlocking {
    val dao =
        InMemoryPosProfilePaymentMethodDao().apply {
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
                      lastSyncedAt = 1,
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
                      lastSyncedAt = 1,
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

  override suspend fun getResolvedMethodsForProfile(
      profileId: String
  ): List<ResolvedPaymentMethod> {
    return relations
        .filter { it.profileId == profileId && !it.isDeleted }
        .sortedBy { it.idx }
        .mapNotNull { relation ->
          val mode = modes[relation.mopName] ?: return@mapNotNull null
          if (!mode.enabled) return@mapNotNull null
          ResolvedPaymentMethod(
              mopName = relation.mopName,
              type = mode.type,
              enabled = mode.enabled,
              isDefault = relation.isDefault,
              currency = mode.currency,
              account = mode.account,
              allowInReturns = relation.allowInReturns,
              idx = relation.idx,
              enabledInProfile = relation.enabledInProfile,
          )
        }
  }

  override suspend fun countResolvedForProfile(profileId: String): Int {
    return getResolvedMethodsForProfile(profileId).size
  }

  override suspend fun countRelationsForProfile(profileId: String): Int {
    return relations.count { it.profileId == profileId && !it.isDeleted }
  }

  override suspend fun countAllRelations(): Int {
    return relations.count { !it.isDeleted }
  }

  override suspend fun softDeleteStaleForProfile(profileId: String, activeMops: List<String>) {
    mutateRelations { relation ->
      if (
          relation.profileId == profileId && !relation.isDeleted && relation.mopName !in activeMops
      ) {
        relation.copy(isDeleted = true)
      } else {
        relation
      }
    }
  }

  override suspend fun hardDeleteDeletedStaleForProfile(
      profileId: String,
      activeMops: List<String>,
  ) {
    relations.removeAll { it.profileId == profileId && it.isDeleted && it.mopName !in activeMops }
  }

  override suspend fun softDeleteAllForProfile(profileId: String) {
    mutateRelations { relation ->
      if (relation.profileId == profileId && !relation.isDeleted) {
        relation.copy(isDeleted = true)
      } else {
        relation
      }
    }
  }

  override suspend fun hardDeleteAllDeletedForProfile(profileId: String) {
    relations.removeAll { it.profileId == profileId && it.isDeleted }
  }

  override suspend fun softDeleteForProfilesNotIn(profileIds: List<String>) {
    mutateRelations { relation ->
      if (!relation.isDeleted && relation.profileId !in profileIds) {
        relation.copy(isDeleted = true)
      } else {
        relation
      }
    }
  }

  override suspend fun hardDeleteDeletedForProfilesNotIn(profileIds: List<String>) {
    relations.removeAll { it.isDeleted && it.profileId !in profileIds }
  }

  override suspend fun softDeleteAllRelations() {
    mutateRelations { relation ->
      if (!relation.isDeleted) relation.copy(isDeleted = true) else relation
    }
  }

  override suspend fun hardDeleteAllDeletedRelations() {
    relations.removeAll { it.isDeleted }
  }

  private fun mutateRelations(
      transform: (PosProfilePaymentMethodEntity) -> PosProfilePaymentMethodEntity
  ) {
    val updated = relations.map(transform)
    relations.clear()
    relations.addAll(updated)
  }
}

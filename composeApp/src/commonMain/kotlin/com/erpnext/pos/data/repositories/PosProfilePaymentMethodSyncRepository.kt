package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.PosProfilePaymentMethodDao
import com.erpnext.pos.localSource.dao.PosProfileLocalDao
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.entities.PosProfileLocalEntity
import com.erpnext.pos.localSource.entities.PosProfilePaymentMethodEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.POSProfileDto
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.RepoTrace
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class PosProfilePaymentMethodSyncRepository(
    private val apiService: APIService,
    private val posProfileDao: POSProfileDao,
    private val posProfileLocalDao: PosProfileLocalDao,
    private val posProfilePaymentMethodDao: PosProfilePaymentMethodDao,
    private val modeOfPaymentDao: ModeOfPaymentDao
) {
    suspend fun syncProfiles(assignedTo: String?): List<PosProfileLocalEntity> {
        RepoTrace.breadcrumb("PosProfilePaymentMethodSyncRepository", "syncProfiles")
        val now = Clock.System.now().toEpochMilliseconds()
        val profiles = apiService.getPOSProfiles(assignedTo)
        val local = profiles.map { profile ->
            PosProfileLocalEntity(
                profileName = profile.profileName,
                company = profile.company,
                currency = profile.currency,
                lastSyncedAt = now
            )
        }
        posProfileLocalDao.upsertAll(local)
        val profileNames = local.map { it.profileName }
        if (profileNames.isEmpty()) {
            posProfileLocalDao.hardDeleteAllDeleted()
            posProfileLocalDao.softDeleteAll()
            posProfileDao.hardDeleteAllDeleted()
            posProfileDao.softDeleteAll()
            posProfilePaymentMethodDao.hardDeleteAllDeletedRelations()
            posProfilePaymentMethodDao.softDeleteAllRelations()
        } else {
            posProfileLocalDao.hardDeleteDeletedNotIn(profileNames)
            posProfileLocalDao.softDeleteNotIn(profileNames)
            posProfileDao.hardDeleteDeletedNotIn(profileNames)
            posProfileDao.softDeleteNotIn(profileNames)
            posProfilePaymentMethodDao.hardDeleteDeletedForProfilesNotIn(profileNames)
            posProfilePaymentMethodDao.softDeleteForProfilesNotIn(profileNames)
        }
        return local
    }

    suspend fun syncProfilesWithPayments(assignedTo: String?): List<PosProfileLocalEntity> {
        RepoTrace.breadcrumb("PosProfilePaymentMethodSyncRepository", "syncProfilesWithPayments")
        val profiles = syncProfiles(assignedTo)
        val failures = mutableListOf<String>()
        profiles.forEach { profile ->
            runCatching { syncProfilePayments(profile.profileName) }
                .onFailure {
                    AppLogger.warn("syncProfilePayments failed for ${profile.profileName}", it)
                    failures.add("${profile.profileName}: ${it.message ?: "unknown error"}")
                }
        }
        if (failures.isNotEmpty()) {
            throw IllegalStateException(
                "Failed syncing POS profile payments for ${failures.joinToString("; ")}"
            )
        }
        return profiles
    }

    suspend fun syncProfilePayments(profileId: String): POSProfileDto {
        RepoTrace.breadcrumb("PosProfilePaymentMethodSyncRepository", "syncProfilePayments", profileId)
        val now = Clock.System.now().toEpochMilliseconds()
        val profile = apiService.getPOSProfileDetails(profileId)
        posProfileDao.insertAll(listOf(profile.toEntity()))
        posProfileLocalDao.upsertAll(
            listOf(
                PosProfileLocalEntity(
                    profileName = profile.profileName,
                    company = profile.company,
                    currency = profile.currency,
                    lastSyncedAt = now
                )
            )
        )

        val paymentEntities = profile.payments.mapIndexed { index, payment ->
            PosProfilePaymentMethodEntity(
                profileId = profile.profileName,
                mopName = payment.modeOfPayment,
                idx = index,
                isDefault = payment.default,
                allowInReturns = payment.allowInReturns,
                enabledInProfile = true,
                lastSyncedAt = now
            )
        }
        posProfilePaymentMethodDao.upsertAll(paymentEntities)
        if (paymentEntities.isEmpty()) {
            posProfilePaymentMethodDao.hardDeleteAllDeletedForProfile(profile.profileName)
            posProfilePaymentMethodDao.softDeleteAllForProfile(profile.profileName)
        } else {
            val mopNames = paymentEntities.map { it.mopName }
            posProfilePaymentMethodDao.hardDeleteDeletedStaleForProfile(profile.profileName, mopNames)
            posProfilePaymentMethodDao.softDeleteStaleForProfile(profile.profileName, mopNames)
        }

        syncModeOfPaymentDetails(profile, now)
        return profile
    }

    private suspend fun syncModeOfPaymentDetails(
        profile: POSProfileDto,
        now: Long
    ) {
        val company = profile.company
        val resolved = profile.payments.mapNotNull { payment ->
            val modeName = payment.modeOfPayment.trim()
            if (modeName.isBlank()) return@mapNotNull null
            val account = payment.defaultAccount
                ?: payment.account
                ?: payment.accounts.firstOrNull { it.company == company }?.defaultAccount
                ?: payment.accounts.firstOrNull()?.defaultAccount
            val currency = payment.accountCurrency
                ?: payment.currency
            val type = payment.accountType
                ?: payment.modeOfPaymentType
                ?: "Cash"
            ModeOfPaymentEntity(
                name = payment.name.ifBlank { modeName },
                modeOfPayment = modeName,
                company = company,
                type = type,
                enabled = payment.enabled,
                currency = currency,
                account = account,
                lastSyncedAt = now
            )
        }
        if (resolved.isNotEmpty()) {
            modeOfPaymentDao.insertAllModes(resolved)
            val names = resolved.map { it.name }
            modeOfPaymentDao.hardDeleteDeletedNotIn(company, names)
            modeOfPaymentDao.softDeleteNotIn(company, names)
        } else {
            modeOfPaymentDao.hardDeleteAllDeletedForCompany(company)
            modeOfPaymentDao.softDeleteAllForCompany(company)
        }
    }
}

package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.PosProfilePaymentMethodDao
import com.erpnext.pos.localSource.dao.PosProfileLocalDao
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.entities.PosProfileLocalEntity
import com.erpnext.pos.localSource.entities.PosProfilePaymentMethodEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.PaymentModesDto
import com.erpnext.pos.remoteSource.dto.POSProfileDto
import com.erpnext.pos.remoteSource.mapper.toEntity
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
        RepoTrace.breadcrumb(
            "PosProfilePaymentMethodSyncRepository",
            "syncProfiles",
            "assignedTo=$assignedTo"
        )
        val now = Clock.System.now().toEpochMilliseconds()
        val snapshot = apiService.getBootstrapPosSyncSnapshot()
        val profiles = snapshot.posProfiles
            ?: throw IllegalStateException("sync.bootstrap no retorno pos_profiles")
        return persistProfilesWithPaymentsSnapshot(profiles, now)
    }

    suspend fun syncProfilesWithPayments(assignedTo: String?): List<PosProfileLocalEntity> {
        RepoTrace.breadcrumb("PosProfilePaymentMethodSyncRepository", "syncProfilesWithPayments")
        val now = Clock.System.now().toEpochMilliseconds()
        val snapshot = apiService.getBootstrapPosSyncSnapshot()
        val bootstrapProfiles = snapshot.posProfiles
            ?: throw IllegalStateException("sync.bootstrap no retorno pos_profiles")
        val local = persistProfilesWithPaymentsSnapshot(bootstrapProfiles, now)
        syncModeOfPaymentDetails(
            profiles = bootstrapProfiles,
            paymentMethods = snapshot.resolvedPaymentMethods,
            now = now,
            pruneCompanies = true
        )
        return local
    }

    suspend fun syncProfilePayments(profileId: String): POSProfileDto {
        RepoTrace.breadcrumb("PosProfilePaymentMethodSyncRepository", "syncProfilePayments", profileId)
        val now = Clock.System.now().toEpochMilliseconds()
        val bootstrapSnapshot = apiService.getBootstrapPosSyncSnapshot(profileName = profileId)
        val bootstrapProfile = bootstrapSnapshot.posProfiles
            ?.firstOrNull { it.profileName == profileId }
            ?: throw IllegalStateException("sync.bootstrap no retorno pos_profile=$profileId")
        persistSingleProfileWithPayments(bootstrapProfile, now)
        syncModeOfPaymentDetails(
            profiles = listOf(bootstrapProfile),
            paymentMethods = bootstrapSnapshot.resolvedPaymentMethods,
            now = now,
            pruneCompanies = true
        )
        return bootstrapProfile
    }

    private suspend fun persistProfilesWithPaymentsSnapshot(
        profiles: List<POSProfileDto>,
        now: Long
    ): List<PosProfileLocalEntity> {
        val localProfiles = profiles.map { profile ->
            PosProfileLocalEntity(
                profileName = profile.profileName,
                company = profile.company,
                currency = profile.currency,
                lastSyncedAt = now
            )
        }
        posProfileLocalDao.upsertAll(localProfiles)
        posProfileDao.insertAll(profiles.toEntity())

        val profileNames = localProfiles.map { it.profileName }
        if (profileNames.isEmpty()) {
            posProfileLocalDao.hardDeleteAllDeleted()
            posProfileLocalDao.softDeleteAll()
            posProfileDao.hardDeleteAllDeleted()
            posProfileDao.softDeleteAll()
            posProfilePaymentMethodDao.hardDeleteAllDeletedRelations()
            posProfilePaymentMethodDao.softDeleteAllRelations()
            return localProfiles
        }

        posProfileLocalDao.hardDeleteDeletedNotIn(profileNames)
        posProfileLocalDao.softDeleteNotIn(profileNames)
        posProfileDao.hardDeleteDeletedNotIn(profileNames)
        posProfileDao.softDeleteNotIn(profileNames)
        posProfilePaymentMethodDao.hardDeleteDeletedForProfilesNotIn(profileNames)
        posProfilePaymentMethodDao.softDeleteForProfilesNotIn(profileNames)

        val paymentEntities = profiles.flatMap { profile ->
            profile.payments.mapIndexedNotNull { index, payment ->
                val modeName = payment.modeOfPayment.trim()
                if (modeName.isBlank()) return@mapIndexedNotNull null
                PosProfilePaymentMethodEntity(
                    profileId = profile.profileName,
                    mopName = modeName,
                    idx = index,
                    isDefault = payment.default,
                    allowInReturns = payment.allowInReturns,
                    enabledInProfile = payment.enabled,
                    lastSyncedAt = now
                )
            }
        }.distinctBy { it.profileId to it.mopName }

        if (paymentEntities.isNotEmpty()) {
            posProfilePaymentMethodDao.upsertAll(paymentEntities)
        }

        profiles.forEach { profile ->
            val activeMops = profile.payments
                .map { it.modeOfPayment.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            if (activeMops.isEmpty()) {
                posProfilePaymentMethodDao.hardDeleteAllDeletedForProfile(profile.profileName)
                posProfilePaymentMethodDao.softDeleteAllForProfile(profile.profileName)
            } else {
                posProfilePaymentMethodDao.hardDeleteDeletedStaleForProfile(profile.profileName, activeMops)
                posProfilePaymentMethodDao.softDeleteStaleForProfile(profile.profileName, activeMops)
            }
        }
        return localProfiles
    }

    private suspend fun persistSingleProfileWithPayments(profile: POSProfileDto, now: Long) {
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
        val paymentEntities = profile.payments.mapIndexedNotNull { index, payment ->
            val modeName = payment.modeOfPayment.trim()
            if (modeName.isBlank()) return@mapIndexedNotNull null
            PosProfilePaymentMethodEntity(
                profileId = profile.profileName,
                mopName = modeName,
                idx = index,
                isDefault = payment.default,
                allowInReturns = payment.allowInReturns,
                enabledInProfile = payment.enabled,
                lastSyncedAt = now
            )
        }.distinctBy { it.profileId to it.mopName }
        if (paymentEntities.isNotEmpty()) {
            posProfilePaymentMethodDao.upsertAll(paymentEntities)
            val mopNames = paymentEntities.map { it.mopName }
            posProfilePaymentMethodDao.hardDeleteDeletedStaleForProfile(profile.profileName, mopNames)
            posProfilePaymentMethodDao.softDeleteStaleForProfile(profile.profileName, mopNames)
        } else {
            posProfilePaymentMethodDao.hardDeleteAllDeletedForProfile(profile.profileName)
            posProfilePaymentMethodDao.softDeleteAllForProfile(profile.profileName)
        }
    }

    private suspend fun syncModeOfPaymentDetails(
        profiles: List<POSProfileDto>,
        paymentMethods: List<PaymentModesDto>?,
        now: Long,
        pruneCompanies: Boolean
    ) {
        val source = when {
            !paymentMethods.isNullOrEmpty() -> paymentMethods
            else -> profiles.flatMap { profile ->
                profile.payments.map { payment ->
                    if (payment.company.isNullOrBlank()) {
                        payment.copy(company = profile.company)
                    } else {
                        payment
                    }
                }
            }
        }
        val resolved = source.mapNotNull { payment ->
            val modeName = payment.modeOfPayment.trim()
            if (modeName.isBlank()) return@mapNotNull null
            val company = payment.company?.trim().orEmpty()
            if (company.isBlank()) return@mapNotNull null
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
                name = modeName,
                modeOfPayment = modeName,
                company = company,
                type = type,
                enabled = payment.enabled,
                currency = currency,
                account = account,
                lastSyncedAt = now
            )
        }.distinctBy { it.company to it.name }
        if (resolved.isNotEmpty()) {
            modeOfPaymentDao.insertAllModes(resolved)
        }
        if (!pruneCompanies) return

        val companiesToPrune = (profiles.map { it.company } + resolved.map { it.company })
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        companiesToPrune.forEach { company ->
            val names = resolved.asSequence()
                .filter { it.company == company }
                .map { it.name }
                .distinct()
                .toList()
            if (names.isEmpty()) {
                modeOfPaymentDao.hardDeleteAllDeletedForCompany(company)
                modeOfPaymentDao.softDeleteAllForCompany(company)
            } else {
                modeOfPaymentDao.hardDeleteDeletedNotIn(company, names)
                modeOfPaymentDao.softDeleteNotIn(company, names)
            }
        }
    }
}

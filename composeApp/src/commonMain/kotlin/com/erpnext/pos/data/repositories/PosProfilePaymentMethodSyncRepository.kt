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
import com.erpnext.pos.remoteSource.datasources.ModeOfPaymentRemoteSource
import com.erpnext.pos.utils.RepoTrace
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class PosProfilePaymentMethodSyncRepository(
    private val apiService: APIService,
    private val modeOfPaymentRemoteSource: ModeOfPaymentRemoteSource,
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
        return local
    }

    suspend fun syncProfilesWithPayments(assignedTo: String?): List<PosProfileLocalEntity> {
        RepoTrace.breadcrumb("PosProfilePaymentMethodSyncRepository", "syncProfilesWithPayments")
        val profiles = syncProfiles(assignedTo)
        profiles.forEach { profile ->
            syncProfilePayments(profile.profileName)
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
                allowInReturns = false,
                enabledInProfile = true,
                lastSyncedAt = now
            )
        }
        posProfilePaymentMethodDao.upsertAll(paymentEntities)
        if (paymentEntities.isEmpty()) {
            posProfilePaymentMethodDao.deleteAllForProfile(profile.profileName)
        } else {
            posProfilePaymentMethodDao.deleteStaleForProfile(
                profile.profileName,
                paymentEntities.map { it.mopName }
            )
        }

        syncModeOfPaymentDetails(profile, paymentEntities.map { it.mopName }, now)
        return profile
    }

    private suspend fun syncModeOfPaymentDetails(
        profile: POSProfileDto,
        mopNames: List<String>,
        now: Long
    ) {
        val company = profile.company
        val uniqueMops = mopNames.distinct()
        val existing = modeOfPaymentDao.getByNames(uniqueMops).associateBy { it.modeOfPayment }
        val missing = uniqueMops.filter { mopName ->
            val stored = existing[mopName]
            stored == null || stored.currency.isNullOrBlank() || stored.account.isNullOrBlank()
        }
        if (missing.isEmpty()) return

        val resolved = missing.mapNotNull { mopName ->
            val detail = modeOfPaymentRemoteSource.getModeDetail(mopName) ?: return@mapNotNull null
            val account = detail.accounts.firstOrNull { it.company == company }?.defaultAccount
                ?: detail.accounts.firstOrNull()?.defaultAccount
            val accountDetail = account?.let { modeOfPaymentRemoteSource.getAccountDetail(it) }
            val currency = accountDetail?.accountCurrency?.takeIf { it.isNotBlank() }
            ModeOfPaymentEntity(
                name = detail.name,
                modeOfPayment = detail.modeOfPayment,
                company = company,
                type = accountDetail?.accountType ?: detail.type ?: "Cash",
                enabled = detail.enabled,
                currency = currency,
                account = account,
                lastSyncedAt = now
            )
        }
        if (resolved.isNotEmpty()) {
            modeOfPaymentDao.insertAllModes(resolved)
        }
    }
}

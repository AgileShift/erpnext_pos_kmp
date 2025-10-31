package com.erpnext.pos.views

import com.erpnext.pos.localSource.dao.CashboxDao
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.UserDao
import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.CashboxEntity
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.TokenStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

sealed interface CashBoxState {
    object Closed : CashBoxState
    data class Opened(
        val posProfileName: String,
        val warehouse: String? = null,
        val territory: String? = null
    ) : CashBoxState
}

class CashBoxManager(
    private val profileDao: POSProfileDao,
    private val cashboxDao: CashboxDao,
    private val userDao: UserDao
) {
    private val _cashboxState = MutableStateFlow<CashBoxState>(CashBoxState.Closed)
    val cashboxState = _cashboxState.asStateFlow()

    suspend fun openCashBox(entry: POSOpeningEntryDto) {
        val user = userDao.getUserInfo()
        val cashbox = CashboxEntity(
            0,
            entry.posProfile,
            entry.company,
            entry.periodStartDate,
            user.username ?: "",
            status = true,
            pendingSync = true
        )
        val details = entry.balanceDetails.map {
            BalanceDetailsEntity(
                0,
                0,
                it.modeOfPayment,
                it.openingAmount
            )
        }
        cashboxDao.insert(cashbox, details)
        profileDao.updateProfileState(user.username ?: "", entry.posProfile, true)
        val currentProfile = profileDao.getActiveProfile()
        _cashboxState.update {
            CashBoxState.Opened(
                entry.posProfile,
                currentProfile?.warehouse,
                currentProfile?.route
            )
        }

        // Intentar Sync (como comentaste)
        /*try {
            val dto = entry.toDto(details)
            val erpName = repo.createPOSOpeningEntry(dto)
            dao.markAsSynced(entry.localId, erpName)
        } catch (e: Exception) {
            // Offline: Enqueue WorkManager
        }*/
    }

    suspend fun closeCashBox(userId: String) {
        val pos = profileDao.getActiveProfile()
        withContext(Dispatchers.IO) {  // Off main thread para Room query
            cashboxDao.getActiveEntry(userId, pos?.profileName ?: "")
                .first()  // Obtiene el primer (único) valor del Flow sync (ya que LIMIT 1)
                ?.let { entry ->
                    cashboxDao.updateStatus(
                        entry.cashbox.localId,
                        status = false,
                        pendingSync = true
                    )
                    _cashboxState.update { CashBoxState.Closed }
                }
        }
    }

    suspend fun isCashBoxOpen(): Boolean {
        val profile = profileDao.getActiveProfile()
        if (profile != null) {
            withContext(Dispatchers.IO) {
                cashboxDao.getActiveEntry(
                    profile.user,
                    profile.profileName
                )  // Asume user de profile
                    .first()
                    ?.let { entry ->
                        _cashboxState.update {
                            CashBoxState.Opened(
                                profile.profileName,
                                profile.warehouse,
                                profile.route
                            )
                        }
                        true
                    }
            }
        } else {
            _cashboxState.update { CashBoxState.Closed }
            false
        }
        return _cashboxState.value is CashBoxState.Opened
    }

    // Helper para subscribers (ViewModels coleccionan este Flow)
    fun observeCashBoxState(): Flow<CashBoxState> = cashboxState
}
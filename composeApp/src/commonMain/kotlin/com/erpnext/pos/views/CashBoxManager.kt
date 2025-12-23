package com.erpnext.pos.views

import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.PaymentModesBO
import com.erpnext.pos.localSource.dao.CashboxDao
import com.erpnext.pos.localSource.dao.POSClosingEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.UserDao
import com.erpnext.pos.localSource.entities.CashboxEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.BalanceDetailsDto
import com.erpnext.pos.remoteSource.dto.POSClosingEntryDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto
import com.erpnext.pos.remoteSource.mapper.toDto
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.toErpDateTime
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

data class PaymentModeWithAmount(
    val mode: PaymentModesBO, val amount: Double
)

data class POSContext(
    val username: String,
    val profileName: String,
    val company: String,
    val warehouse: String?,
    val route: String?,
    val territory: String?,
    val priceList: String?,
    val isCashBoxOpen: Boolean,
    val cashboxId: Long?,
    val incomeAccount: String,
    val expenseAccount: String,
    val branch: String,
    val currency: String,
)

class CashBoxManager(
    private val api: APIService,
    private val profileDao: POSProfileDao,
    private val openingDao: POSOpeningEntryDao,
    private val closingDao: POSClosingEntryDao,
    private val cashboxDao: CashboxDao,
    private val userDao: UserDao
) {

    //Contexto actual del POS cargado en memoria
    private var currentContext: POSContext? = null

    private val _cashboxState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val cashboxState = _cashboxState.asStateFlow()

    suspend fun initializeContext(): POSContext? = withContext(Dispatchers.IO) {
        val user = userDao.getUserInfo() ?: return@withContext null
        val profile = profileDao.getActiveProfile() ?: return@withContext null
        val activeCashbox = cashboxDao.getActiveEntry(user.email, profile.profileName)
            .firstOrNull()

        _cashboxState.update { activeCashbox != null }

        currentContext = POSContext(
            username = user.username ?: "",
            profileName = profile.profileName,
            company = profile.company,
            warehouse = profile.warehouse,
            route = profile.route,
            territory = profile.route,
            priceList = profile.sellingPriceList,
            isCashBoxOpen = activeCashbox != null,
            cashboxId = activeCashbox?.cashbox?.localId,
            incomeAccount = profile.incomeAccount,
            expenseAccount = profile.expenseAccount,
            branch = profile.branch,
            currency = profile.currency
        )
        currentContext
    }

    //TODO: Armar y enviar el POSOpeningEntry
    suspend fun openCashBox(
        entry: POSProfileSimpleBO,
        amounts: List<PaymentModeWithAmount>
    ): POSContext? = withContext(Dispatchers.IO) {
        val user = userDao.getUserInfo() ?: return@withContext null
        val newEntry = POSOpeningEntryDto(
            posProfile = entry.name,
            company = entry.company,
            user = user.email,
            periodStartDate = getTimeMillis().toErpDateTime(),
            postingDate = getTimeMillis().toErpDateTime(),

            balanceDetails = amounts.map {
                BalanceDetailsDto(
                    it.mode.modeOfPayment,
                    it.amount,
                    closingAmount = 0.0
                )
            },
            taxes = null
        )
        val poeId = api.openCashbox(newEntry)
        val cashbox = CashboxEntity(
            localId = 0,
            posProfile = newEntry.posProfile,
            company = entry.company,
            periodStartDate = newEntry.periodStartDate,
            user = user.email,
            status = true,
            pendingSync = true,
            openingEntryId = poeId.name,
        )

        cashboxDao.insert(cashbox, newEntry.balanceDetails.map {
            it.toEntity(cashboxId = cashbox.localId)
        })

        profileDao.updateProfileState(user.username, newEntry.posProfile, true)

        val profile = profileDao.getActiveProfile() ?: error("Profile not found")

        openingDao.insert(newEntry.toEntity(poeId.name))

        _cashboxState.update { true }

        currentContext = POSContext(
            username = user.username ?: user.name,
            profileName = newEntry.posProfile,
            company = entry.company,
            warehouse = profile.warehouse,
            route = profile.route,
            territory = profile.route,
            priceList = profile.sellingPriceList,
            isCashBoxOpen = true,
            cashboxId = cashbox.localId,
            incomeAccount = profile.incomeAccount,
            expenseAccount = profile.expenseAccount,
            branch = profile.branch,
            currency = profile.currency
        )
        currentContext!!
    }

    //TODO Armar y enviar el POSClosingEntry
    suspend fun closeCashBox(): POSContext? = withContext(Dispatchers.IO) {
        val ctx = currentContext ?: initializeContext()
        if (ctx == null) return@withContext null
        val user = userDao.getUserInfo() ?: return@withContext null

        val entry = cashboxDao.getActiveEntry(user.email, ctx.profileName).firstOrNull()
        val dto = POSClosingEntryDto(
            posProfile = ctx.profileName,
            posOpeningEntry = entry?.cashbox?.openingEntryId!!,
            user = user.email,
            company = ctx.company,
            postingDate = getTimeMillis().toErpDateTime(),
            periodStartDate = entry?.cashbox?.periodStartDate ?: "",
            periodEndDate = getTimeMillis().toErpDateTime(),
            balanceDetails = entry?.details?.toDto()!!
        )
        val pce = api.closeCashbox(dto)

        cashboxDao.updateStatus(
            entry.cashbox.localId,
            status = false,
            pce.name,
            dto.periodEndDate,
            pendingSync = true
        )
        profileDao.updateProfileState(ctx.username, ctx.profileName, false)
        closingDao.insert(dto.toEntity(pce.name))

        _cashboxState.update { false }
        currentContext = ctx.copy(isCashBoxOpen = false)
        currentContext
    }

    fun isCashboxOpen(): Boolean {
        return getContext()?.isCashBoxOpen ?: false
    }

    fun getContext(): POSContext? = currentContext

    fun requireContext(): POSContext =
        currentContext ?: error("POS context not initialized. Call initializeContext() first.")
}
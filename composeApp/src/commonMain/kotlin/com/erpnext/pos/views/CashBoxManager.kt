package com.erpnext.pos.views

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.POSCurrencyOption
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.PaymentModesBO
import com.erpnext.pos.localSource.dao.CashboxDao
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.dao.POSClosingEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryDao
import com.erpnext.pos.localSource.dao.POSOpeningEntryLinkDao
import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.dao.UserDao
import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.CashboxEntity
import com.erpnext.pos.localSource.entities.CashboxWithDetails
import com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
import com.erpnext.pos.localSource.entities.POSOpeningEntryLinkEntity
import com.erpnext.pos.localSource.preferences.ExchangeRatePreferences
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.POSClosingEntryDto
import com.erpnext.pos.remoteSource.dto.POSClosingInvoiceDto
import com.erpnext.pos.remoteSource.mapper.toDto
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.data.repositories.ExchangeRateRepository
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.localSource.dao.CompanyDao
import com.erpnext.pos.utils.parseErpDateTimeToEpochMillis
import com.erpnext.pos.utils.toErpDateTime
import io.ktor.util.date.getTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

data class PaymentModeWithAmount(
    val mode: PaymentModesBO, val amount: Double
)

data class POSContext(
    val cashier: UserBO,
    val username: String,
    val profileName: String,
    val company: String,
    val warehouse: String?,
    val route: String?,
    val territory: String?,
    val priceList: String?,
    val isCashBoxOpen: Boolean,
    val cashboxId: Long?,
    val incomeAccount: String?,
    val expenseAccount: String?,
    val branch: String?,
    val currency: String,
    val partyAccountCurrency: String,
    val exchangeRate: Double,
    val allowedCurrencies: List<POSCurrencyOption>,
    val paymentModes: List<POSPaymentModeOption>
)

class CashBoxManager(
    private val api: APIService,
    private val profileDao: POSProfileDao,
    private val openingDao: POSOpeningEntryDao,
    private val openingEntryLinkDao: POSOpeningEntryLinkDao,
    private val closingDao: POSClosingEntryDao,
    private val companyDao: CompanyDao,
    private val cashboxDao: CashboxDao,
    private val userDao: UserDao,
    private val exchangeRatePreferences: ExchangeRatePreferences,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val modeOfPaymentDao: ModeOfPaymentDao,
    private val salesInvoiceDao: SalesInvoiceDao
) {

    //Contexto actual del POS cargado en memoria
    private var currentContext: POSContext? = null
    private val _contextFlow: MutableStateFlow<POSContext?> = MutableStateFlow(null)
    val contextFlow = _contextFlow.asStateFlow()

    private val _cashboxState: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val cashboxState = _cashboxState.asStateFlow()

    suspend fun initializeContext(): POSContext? = withContext(Dispatchers.IO) {
        val user = userDao.getUserInfo() ?: return@withContext null
        val profile = profileDao.getActiveProfile() ?: return@withContext null
        val company = companyDao.getCompanyInfo()
        val activeCashbox = cashboxDao.getActiveEntry(user.email, profile.profileName)
            .firstOrNull()
        val exchangeRate = resolveExchangeRate(profile.currency)
        val allowedCurrencies = resolveSupportedCurrencies(profile.currency, profile.company)
        val paymentModes = resolvePaymentModes(profile.profileName, profile.company)

        _cashboxState.update { activeCashbox != null }

        currentContext = POSContext(
            cashier = user.toBO(),
            username = user.username ?: "",
            profileName = profile.profileName,
            company = company?.companyName ?: profile.company,
            partyAccountCurrency = company?.defaultCurrency ?: profile.currency,
            warehouse = profile.warehouse,
            route = profile.route,
            territory = profile.route,
            priceList = profile.sellingPriceList,
            isCashBoxOpen = activeCashbox != null,
            cashboxId = activeCashbox?.cashbox?.localId,
            incomeAccount = profile.incomeAccount,
            expenseAccount = profile.expenseAccount,
            branch = profile.branch,
            currency = profile.currency,
            exchangeRate = exchangeRate,
            allowedCurrencies = allowedCurrencies,
            paymentModes = paymentModes
        )
        _contextFlow.value = currentContext
        currentContext
    }

    suspend fun openCashBox(
        entry: POSProfileSimpleBO,
        amounts: List<PaymentModeWithAmount>
    ): POSContext? = withContext(Dispatchers.IO) {
        val user = userDao.getUserInfo() ?: return@withContext null
        val existing = cashboxDao.getActiveEntry(user.email, entry.name).firstOrNull()
        if (existing != null) {
            error("Ya existe una apertura abierta para este perfil.")
        }
        val now = getTimeMillis()
        val periodStart = now.toErpDateTime()
        val openingEntryName = buildLocalOpeningEntryId(entry.name, user.email, now)
        val openingEntry = POSOpeningEntryEntity(
            name = openingEntryName,
            posProfile = entry.name,
            company = entry.company,
            periodStartDate = periodStart,
            postingDate = periodStart,
            user = user.email,
            pendingSync = true
        )
        openingDao.insert(openingEntry)

        val cashbox = CashboxEntity(
            localId = 0,
            posProfile = entry.name,
            company = entry.company,
            periodStartDate = periodStart,
            user = user.email,
            status = true,
            pendingSync = true,
            openingEntryId = openingEntryName,
        )
        val balanceDetails = amounts.map {
            BalanceDetailsEntity(
                cashboxId = 0,
                posOpeningEntry = openingEntryName,
                modeOfPayment = it.mode.modeOfPayment,
                openingAmount = it.amount,
                closingAmount = null
            )
        }
        val cashboxId = cashboxDao.insert(cashbox, balanceDetails)
        openingEntryLinkDao.insert(
            POSOpeningEntryLinkEntity(
                cashboxId = cashboxId,
                localOpeningEntryName = openingEntryName,
                pendingSync = true
            )
        )

        profileDao.updateProfileState(user.username, entry.name, true)
        val company = companyDao.getCompanyInfo()
        val profile = profileDao.getActiveProfile() ?: error("Profile not found")

        _cashboxState.update { true }

        val exchangeRate = resolveExchangeRate(profile.currency)
        val allowedCurrencies = resolveSupportedCurrencies(profile.currency, profile.company)
        val paymentModes = resolvePaymentModes(profile.profileName, profile.company)
        currentContext = POSContext(
            username = user.username ?: user.name,
            profileName = entry.name,
            company = company?.companyName ?: profile.company,
            warehouse = profile.warehouse,
            route = profile.route,
            territory = profile.route,
            priceList = profile.sellingPriceList,
            isCashBoxOpen = true,
            cashboxId = cashboxId,
            incomeAccount = profile.incomeAccount,
            expenseAccount = profile.expenseAccount,
            branch = profile.branch,
            currency = profile.currency,
            exchangeRate = exchangeRate,
            allowedCurrencies = allowedCurrencies,
            paymentModes = paymentModes,
            cashier = user.toBO(),
            partyAccountCurrency = company?.defaultCurrency ?: profile.currency,
        )
        _contextFlow.value = currentContext
        currentContext!!
    }

    //TODO Armar y enviar el POSClosingEntry
    suspend fun closeCashBox(): POSContext? = withContext(Dispatchers.IO) {
        val ctx = currentContext ?: initializeContext()
        if (ctx == null) return@withContext null
        val user = userDao.getUserInfo() ?: return@withContext null

        val entry = cashboxDao.getActiveEntry(user.email, ctx.profileName).firstOrNull()
        if (entry == null) return@withContext null
        val endMillis = getTimeMillis()
        val startMillis = parseErpDateTimeToEpochMillis(entry.cashbox.periodStartDate)
            ?: endMillis
        val openingEntryId = openingEntryLinkDao.getRemoteOpeningEntryName(entry.cashbox.localId)
            ?: entry.cashbox.openingEntryId?.takeIf { it.isNotBlank() }
            ?: return@withContext null
        val shiftInvoices = salesInvoiceDao.getInvoicesForShift(
            profileId = ctx.profileName,
            startMillis = startMillis,
            endMillis = endMillis
        )
        val invoiceDetails = shiftInvoices.mapNotNull { invoice ->
            val name = invoice.invoiceName?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            POSClosingInvoiceDto(
                salesInvoice = name,
                postingDate = invoice.postingDate,
                customer = invoice.customer,
                grandTotal = invoice.grandTotal,
                paidAmount = invoice.paidAmount,
                outstandingAmount = invoice.outstandingAmount,
                isReturn = invoice.isReturn
            )
        }
        val dto = POSClosingEntryDto(
            posProfile = ctx.profileName,
            posOpeningEntry = openingEntryId,
            user = user.email,
            company = ctx.company,
            postingDate = getTimeMillis().toErpDateTime(),
            periodStartDate = entry.cashbox.periodStartDate,
            periodEndDate = getTimeMillis().toErpDateTime(),
            balanceDetails = entry.details.toDto(),
            posTransactions = invoiceDetails
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
        currentContext = null
        _contextFlow.value = null
        currentContext = null
        currentContext
    }

    fun getContext(): POSContext? = currentContext

    fun requireContext(): POSContext =
        currentContext ?: error("POS context not initialized. Call initializeContext() first.")

    fun activeCashboxStart(): Flow<String?> = flow {
        val ctx = currentContext ?: initializeContext()
        if (ctx == null) {
            emit(null)
            return@flow
        }
        val user = userDao.getUserInfo()
        if (user == null) {
            emit(null)
            return@flow
        }
        emitAll(
            cashboxDao.getActiveEntry(user.email, ctx.profileName)
                .map { it?.cashbox?.periodStartDate }
        )
    }

    suspend fun getActiveCashboxWithDetails(): CashboxWithDetails? = withContext(Dispatchers.IO) {
        val ctx = currentContext ?: initializeContext() ?: return@withContext null
        val user = userDao.getUserInfo() ?: return@withContext null
        cashboxDao.getActiveEntry(user.email, ctx.profileName).firstOrNull()
    }

    //TODO: Sincronizar las monedas existentes y activas en el ERP
    private suspend fun resolveSupportedCurrencies(
        baseCurrency: String,
        company: String
    ): List<POSCurrencyOption> {
        val modes = runCatching { modeOfPaymentDao.getAll(company) }.getOrElse { emptyList() }
        val mapped = modes.mapNotNull { mode ->
            mode.currency?.takeIf { it.isNotBlank() }?.let { currency ->
                POSCurrencyOption(
                    code = currency,
                    name = currency
                )
            }
        }
        val normalizedBase = baseCurrency.trim()
        val withBase = if (mapped.any { it.code.equals(normalizedBase, ignoreCase = true) }) {
            mapped
        } else {
            mapped + POSCurrencyOption(
                code = normalizedBase,
                name = normalizedBase
            )
        }
        val distinct = withBase.distinctBy { it.code.uppercase() }
        return distinct.ifEmpty {
            listOf(
                POSCurrencyOption(
                    code = normalizedBase,
                    name = normalizedBase
                )
            )
        }
    }

    private fun buildLocalOpeningEntryId(profileName: String, userEmail: String, now: Long): String {
        val normalizedProfile = profileName.trim().uppercase().take(12)
        val normalizedUser = userEmail.substringBefore('@').uppercase().take(8)
        return "LOCAL-OPEN-$normalizedProfile-$normalizedUser-$now"
    }

    private suspend fun resolvePaymentModes(
        profileName: String,
        company: String
    ): List<POSPaymentModeOption> {
        val storedModes = runCatching { modeOfPaymentDao.getAllModes(company) }
            .getOrElse { emptyList() }
        val modeTypes = storedModes.associateBy { it.modeOfPayment }
        return runCatching { modeOfPaymentDao.getAll(company) }
            .getOrElse { emptyList() }
            .map { mode ->
                POSPaymentModeOption(
                    name = mode.name,
                    modeOfPayment = mode.modeOfPayment,
                    type = modeTypes[mode.modeOfPayment]?.type,
                )
            }
    }

    suspend fun updateManualExchangeRate(rate: Double) {
        exchangeRatePreferences.saveManualRate(rate)
        currentContext = currentContext?.copy(exchangeRate = rate)
    }

    suspend fun updateClosingAmounts(
        cashboxId: Long,
        closingByMode: Map<String, Double>
    ) = withContext(Dispatchers.IO) {
        closingByMode.forEach { (mode, amount) ->
            cashboxDao.updateClosingAmount(cashboxId, mode, amount)
        }
    }

    suspend fun resolveExchangeRateBetween(
        fromCurrency: String,
        toCurrency: String
    ): Double? {
        val from = fromCurrency.trim().uppercase()
        val to = toCurrency.trim().uppercase()
        if (from.isBlank() || to.isBlank()) return null
        if (from == to) return 1.0

        return exchangeRateRepository.getRate(from, to)
    }

    private suspend fun resolveExchangeRate(baseCurrency: String): Double {
        val normalized = baseCurrency.trim().uppercase()
        if (normalized == "USD") return 1.0

        val cached = exchangeRateRepository.getRate("USD", normalized)
        if (cached != null && cached > 0.0) {
            return cached
        }

        return exchangeRatePreferences.loadManualRate()
            ?: ExchangeRatePreferences.DEFAULT_MANUAL_RATE
    }
}

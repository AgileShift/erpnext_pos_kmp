package com.erpnext.pos.remoteSource.api

import com.erpnext.pos.BuildKonfig
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.remoteSource.dto.AccountDetailDto
import com.erpnext.pos.remoteSource.dto.AddressListDto
import com.erpnext.pos.remoteSource.dto.BalanceDetailsDto
import com.erpnext.pos.remoteSource.dto.BinDto
import com.erpnext.pos.remoteSource.dto.BootstrapDataDto
import com.erpnext.pos.remoteSource.dto.BootstrapPosSyncDto
import com.erpnext.pos.remoteSource.dto.BootstrapRequestDto
import com.erpnext.pos.remoteSource.dto.CategoryDto
import com.erpnext.pos.remoteSource.dto.CompanyDto
import com.erpnext.pos.remoteSource.dto.ContactListDto
import com.erpnext.pos.remoteSource.dto.CurrencyDto
import com.erpnext.pos.remoteSource.dto.CustomerCreateDto
import com.erpnext.pos.remoteSource.dto.CustomerDto
import com.erpnext.pos.remoteSource.dto.CustomerGroupDto
import com.erpnext.pos.remoteSource.dto.DeliveryChargeDto
import com.erpnext.pos.remoteSource.dto.DocNameResponseDto
import com.erpnext.pos.remoteSource.dto.ExchangeRateResponse
import com.erpnext.pos.remoteSource.dto.ItemDto
import com.erpnext.pos.remoteSource.dto.ItemReorderDto
import com.erpnext.pos.remoteSource.dto.LinkRefDto
import com.erpnext.pos.remoteSource.dto.LoginInfo
import com.erpnext.pos.remoteSource.dto.ModeOfPaymentDetailDto
import com.erpnext.pos.remoteSource.dto.ModeOfPaymentDto
import com.erpnext.pos.remoteSource.dto.OutstandingInfo
import com.erpnext.pos.remoteSource.dto.POSClosingEntryDto
import com.erpnext.pos.remoteSource.dto.POSClosingEntryResponse
import com.erpnext.pos.remoteSource.dto.POSClosingEntrySummaryDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDetailDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryResponseDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntrySummaryDto
import com.erpnext.pos.remoteSource.dto.POSProfileDto
import com.erpnext.pos.remoteSource.dto.POSProfileSimpleDto
import com.erpnext.pos.remoteSource.dto.PaymentEntryCreateDto
import com.erpnext.pos.remoteSource.dto.PaymentEntryDto
import com.erpnext.pos.remoteSource.dto.PaymentTermDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.StockSettingsDto
import com.erpnext.pos.remoteSource.dto.SubmitResponseDto
import com.erpnext.pos.remoteSource.dto.TerritoryDto
import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.dto.UserDto
import com.erpnext.pos.remoteSource.dto.WarehouseItemDto
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.OAuthConfig
import com.erpnext.pos.remoteSource.oauth.Pkce
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.refreshAuthToken
import com.erpnext.pos.remoteSource.oauth.toOAuthConfig
import com.erpnext.pos.remoteSource.sdk.FrappeErrorResponse
import com.erpnext.pos.remoteSource.sdk.FrappeException
import com.erpnext.pos.remoteSource.sdk.withRetries
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.utils.TokenUtils
import com.erpnext.pos.utils.normalizeUrl
import com.erpnext.pos.utils.view.DateTimeProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.formUrlEncode
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class APIService(
    private val client: HttpClient,
    private val store: TokenStore,
    private val authStore: AuthInfoStore,
    private val tokenClient: HttpClient
) {
    private data class BootstrapCacheEntry(
        val key: String,
        val data: BootstrapDataDto,
        val createdAtMillis: Long
    )

    private companion object {
        const val DEFAULT_INVOICE_SYNC_DAYS = 90
        const val RECENT_PAID_INVOICE_DAYS = 7
        const val BOOTSTRAP_CACHE_WINDOW_MS = 15_000L
    }

    private var bootstrapCache: BootstrapCacheEntry? = null

    private suspend fun fetchInvoicesByStatus(
        posProfile: String,
        startDate: String,
        statuses: List<String>
    ): List<SalesInvoiceDto> {
        return fetchAllInvoicesCombined(posProfile, recentPaidOnly = false)
            .asSequence()
            .filter { it.status in statuses }
            .filter { it.postingDate.take(10) >= startDate }
            .toList()
    }

    suspend fun getCompanyInfo(): List<CompanyDto> {
        val bootstrapCompany = runCatching {
            val payload = BootstrapRequestDto(
                includeInventory = false,
                includeCustomers = false,
                includeInvoices = false,
                includeAlerts = false,
                includeActivity = false,
                recentPaidOnly = true
            )
            fetchBootstrapRaw(payload)["company"]?.jsonObject
        }.getOrNull()

        val fromBootstrap = bootstrapCompany?.let { company ->
            val name = company.stringOrNull("company")
            val defaultCurrency = company.stringOrNull("default_currency")
            if (!name.isNullOrBlank() && !defaultCurrency.isNullOrBlank()) {
                CompanyDto(
                    company = name,
                    defaultCurrency = defaultCurrency,
                    taxId = company.stringOrNull("tax_id"),
                    country = company.stringOrNull("country")
                )
            } else {
                null
            }
        }
        if (fromBootstrap != null) return listOf(fromBootstrap)

        val profiles = getPOSProfiles()
        val first = profiles.firstOrNull()
            ?: throw IllegalStateException("Company info not available")
        return listOf(
            CompanyDto(
                company = first.company,
                defaultCurrency = first.currency.ifBlank { "USD" },
                taxId = null,
                country = null
            )
        )
    }

    suspend fun getCompanyMonthlySalesTarget(companyId: String): Double? {
        companyId
        return null
    }

    suspend fun getStockSettings(): List<StockSettingsDto> {
        return runCatching {
            val payload = BootstrapRequestDto(
                includeInventory = false,
                includeCustomers = false,
                includeInvoices = false,
                includeAlerts = false,
                includeActivity = false,
                recentPaidOnly = true
            )
            val stock = fetchBootstrapRaw(payload)["stock_settings"] ?: JsonObject(emptyMap())
            listOf(json.decodeFromJsonElement<StockSettingsDto>(stock))
        }.getOrDefault(listOf(StockSettingsDto()))
    }

    suspend fun createPaymentEntry(entry: PaymentEntryCreateDto): SubmitResponseDto {
        return postMethodWithPayload(
            methodPath = "erpnext_pos.api.v1.payment_entry.create_submit",
            payload = entry
        )
    }

    suspend fun fetchInvoicesForTerritoryFromDate(
        territory: String, fromDate: String
    ): List<SalesInvoiceDto> {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = false,
            includeInvoices = true,
            includeAlerts = false,
            includeActivity = false,
            territory = territory,
            route = territory,
            recentPaidOnly = false
        )
        return fetchBootstrap(payload).invoices
            .asSequence()
            .filter { it.postingDate.take(10) >= fromDate }
            .sortedByDescending { it.postingDate }
            .toList()
    }

    suspend fun fetchPaymentTerms(): List<PaymentTermDto> {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = false,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = true
        )
        val raw = fetchBootstrapRaw(payload)
        return raw["payment_terms"]?.let { json.decodeFromJsonElement<List<PaymentTermDto>>(it) }
            .orEmpty()
    }

    suspend fun fetchDeliveryCharges(): List<DeliveryChargeDto> {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = false,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = true
        )
        val raw = fetchBootstrapRaw(payload)
        return raw["delivery_charges"]?.let { json.decodeFromJsonElement<List<DeliveryChargeDto>>(it) }
            .orEmpty()
    }

    suspend fun fetchCustomerGroups(): List<CustomerGroupDto> {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = false,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = true
        )
        val raw = fetchBootstrapRaw(payload)
        return raw["customer_groups"]?.let { json.decodeFromJsonElement<List<CustomerGroupDto>>(it) }
            .orEmpty()
    }

    suspend fun fetchTerritories(): List<TerritoryDto> {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = false,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = true
        )
        val raw = fetchBootstrapRaw(payload)
        return raw["territories"]?.let { json.decodeFromJsonElement<List<TerritoryDto>>(it) }
            .orEmpty()
    }

    suspend fun fetchCustomerContacts(): List<ContactListDto> {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = true,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = true
        )
        return fetchBootstrap(payload).customers.map { customer ->
            ContactListDto(
                name = "CONTACT-${customer.name}",
                emailId = customer.email,
                mobileNo = customer.mobileNo,
                phone = customer.mobileNo,
                links = listOf(LinkRefDto(linkDoctype = "Customer", linkName = customer.name))
            )
        }
    }

    suspend fun fetchCustomerAddresses(): List<AddressListDto> {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = true,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = true
        )
        return fetchBootstrap(payload).customers
            .filter { !it.address.isNullOrBlank() }
            .map { customer ->
                AddressListDto(
                    name = customer.address!!.trim(),
                    addressTitle = customer.customerName,
                    addressType = "Billing",
                    country = null,
                    emailId = customer.email,
                    phone = customer.mobileNo,
                    links = listOf(LinkRefDto(linkDoctype = "Customer", linkName = customer.name))
                )
            }
    }

    suspend fun createCustomer(payload: CustomerCreateDto): DocNameResponseDto {
        val body: JsonObject = postMethodWithPayload(
            methodPath = "erpnext_pos.api.v1.customer.upsert_atomic",
            payload = payload
        )
        val name = body.stringOrNull("customer")
            ?: body.stringOrNull("name")
            ?: body.stringOrNull("customer_name")
            ?: throw IllegalStateException("customer.upsert_atomic no retornó identificador")
        return DocNameResponseDto(name)
    }

    suspend fun exchangeCode(
        oauthConfig: OAuthConfig,
        code: String,
        pkce: Pkce,
        expectedState: String,
        returnedState: String
    ): TokenResponse? {
        try {
            require(expectedState == returnedState) { "CSRF state mismatch" }
            val res = tokenClient.post(oauthConfig.tokenUrl) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(Parameters.build {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", oauthConfig.redirectUrl)
                    append("client_id", oauthConfig.clientId)
                    append("code_verifier", pkce.verifier)
                    oauthConfig.clientSecret?.let { append("client_secret", it) }
                }.formUrlEncode())
            }.body<TokenResponse>()

            store.save(res)
            return res
        } catch (e: Throwable) {
            e.printStackTrace()
            AppSentry.capture(e, "exchangeCode failed")
            AppLogger.warn("exchangeCode failed", e)
            return null
        }
    }

    suspend fun refreshToken(refresh: String): TokenResponse {
        require(refresh.isNotBlank()) { "Missing refresh token" }
        return refreshAuthToken(tokenClient, authStore, refresh)
    }

    suspend fun getUserInfo(): UserDto {
        val remoteUser = fetchAuthenticatedUser()
        val currentSite = authStore.getCurrentSite()?.trim()?.trimEnd('/')
        val fallbackEmail = store.loadUser()?.trim()?.takeIf { it.contains("@") }
        val normalizedName = remoteUser.name.trim()
            .ifBlank { remoteUser.username?.trim().orEmpty() }
        val normalizedUsername = remoteUser.username?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: normalizedName
        val normalizedFirstName = remoteUser.firstName?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: remoteUser.fullName?.trim()?.substringBefore(" ")?.takeIf { it.isNotBlank() }
            ?: normalizedUsername
        val normalizedEmail = remoteUser.email?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: fallbackEmail
            ?: normalizedUsername
        val normalizedFullName = remoteUser.fullName?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: listOfNotNull(normalizedFirstName, remoteUser.lastName)
                .joinToString(" ")
                .trim()
        val normalizedImage = remoteUser.image
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { raw ->
                when {
                    raw.startsWith("http://", ignoreCase = true) ||
                            raw.startsWith("https://", ignoreCase = true) -> raw

                    raw.startsWith("/") && !currentSite.isNullOrBlank() -> "$currentSite$raw"
                    !currentSite.isNullOrBlank() -> "$currentSite/$raw"
                    else -> raw
                }
            }
        return remoteUser.copy(
            name = normalizedName,
            username = normalizedUsername,
            firstName = normalizedFirstName,
            email = normalizedEmail,
            fullName = normalizedFullName,
            image = normalizedImage
        )
    }

    suspend fun getIdTokenIssuerAndCurrentSite(idToken: String?): Pair<String?, String?> {
        val issuer = TokenUtils.decodePayload(idToken ?: "")
            ?.get("iss")
            ?.toString()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        val site = authStore.getCurrentSite()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        return issuer to site
    }

    suspend fun isIdTokenIssuerBoundToCurrentSite(idToken: String?): Boolean {
        if (idToken.isNullOrBlank()) return false
        val (issuer, site) = getIdTokenIssuerAndCurrentSite(idToken)
        if (issuer.isNullOrBlank() || site.isNullOrBlank()) return false
        val normalizedIssuer = normalizeUrl(issuer).trimEnd('/').lowercase()
        val normalizedSite = normalizeUrl(site).trimEnd('/').lowercase()
        if (normalizedIssuer == normalizedSite) return true
        val issuerHostPort = instanceHostPortKey(normalizedIssuer)
        val siteHostPort = instanceHostPortKey(normalizedSite)
        return issuerHostPort.isNotBlank() &&
                siteHostPort.isNotBlank() &&
                issuerHostPort == siteHostPort
    }

    private fun instanceHostPortKey(url: String): String {
        val noScheme = url
            .removePrefix("https://")
            .removePrefix("http://")
        return noScheme.substringBefore('/').trim()
    }

    suspend fun getExchangeRate(
        fromCurrency: String, toCurrency: String, date: String? = null
    ): Double? {
        val url = authStore.getCurrentSite() ?: return null
        val endpoint = "$url/api/method/erpnext.setup.utils.get_exchange_rate"
        return runCatching {
            val response = client.get(endpoint) {
                parameter("from_currency", fromCurrency)
                parameter("to_currency", toCurrency)
                date?.let { parameter("date", it) }
            }
            response.body<ExchangeRateResponse>().message
        }.onFailure { e ->
            AppSentry.capture(e, "getExchangeRate failed")
            AppLogger.warn("getExchangeRate failed", e)
        }.getOrNull()
    }

    suspend fun revoke() {
        val currentToken = store.load() ?: return
        val oAuthConfig = authStore.loadAuthInfoByUrl().toOAuthConfig()

        client.post(oAuthConfig.revokeUrl) {
            //header(HttpHeaders.Authorization, bearer)
            contentType(ContentType.Application.FormUrlEncoded)
            setBody(FormDataContent(Parameters.build {
                append("token", currentToken.access_token)
                append("token_type_hint", "access_token")
            }))
        }
        store.clear()
    }

    suspend fun getEnabledCurrencies(): List<CurrencyDto> {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = false,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = true
        )
        val raw = fetchBootstrapRaw(payload)
        return raw["currencies"]?.let { json.decodeFromJsonElement<List<CurrencyDto>>(it) }
            .orEmpty()
    }

    suspend fun getSystemSettingsRaw(): JsonObject? {
        return null
    }

    suspend fun getCurrencyDetail(code: String): JsonObject? {
        val normalized = code.trim().uppercase()
        if (normalized.isBlank()) return null
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = false,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = true
        )
        val currencies = fetchBootstrapRaw(payload)["currencies"]?.jsonArray ?: return null
        return currencies
            .asSequence()
            .mapNotNull { it as? JsonObject }
            .firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull == normalized }
    }

    suspend fun getActiveModeOfPayment(): List<ModeOfPaymentDto> {
        val snapshot = getBootstrapPosSyncSnapshot()
        return snapshot.resolvedPaymentMethods
            .orEmpty()
            .asSequence()
            .filter { it.enabled }
            .map { payment ->
                ModeOfPaymentDto(
                    name = payment.modeOfPayment,
                    modeOfPayment = payment.modeOfPayment,
                    currency = payment.accountCurrency ?: payment.currency,
                    enabled = payment.enabled
                )
            }
            .distinctBy { it.modeOfPayment }
            .toList()
    }

    suspend fun getModeOfPaymentDetail(name: String): ModeOfPaymentDetailDto? {
        val normalized = name.trim()
        if (normalized.isBlank()) return null
        val payment = getBootstrapPosSyncSnapshot().resolvedPaymentMethods
            .orEmpty()
            .firstOrNull { it.modeOfPayment == normalized || it.name == normalized }
            ?: return null
        return ModeOfPaymentDetailDto(
            name = payment.modeOfPayment,
            modeOfPayment = payment.modeOfPayment,
            enabled = payment.enabled,
            type = payment.accountType ?: payment.modeOfPaymentType,
            accounts = payment.accounts
        )
    }

    suspend fun getAccountDetail(name: String): AccountDetailDto? {
        val normalized = name.trim()
        if (normalized.isBlank()) return null
        val payment = getBootstrapPosSyncSnapshot().resolvedPaymentMethods
            .orEmpty()
            .firstOrNull { method ->
                method.account == normalized || method.defaultAccount == normalized ||
                        method.accounts.any { it.defaultAccount == normalized }
            } ?: return null
        return AccountDetailDto(
            name = normalized,
            accountCurrency = payment.accountCurrency ?: payment.currency,
            accountType = payment.accountType ?: payment.modeOfPaymentType,
            company = payment.company
        )
    }

    suspend fun getCategories(): List<CategoryDto> {
        val payload = BootstrapRequestDto(
            includeInventory = true,
            includeCustomers = false,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = true
        )
        val raw = fetchBootstrapRaw(payload)
        return raw["categories"]?.let { json.decodeFromJsonElement<List<CategoryDto>>(it) }
            .orEmpty()
    }

    suspend fun getItemDetail(itemId: String): ItemDto {
        val payload = BootstrapRequestDto(
            includeInventory = true,
            includeCustomers = false,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = true
        )
        val item = fetchBootstrap(payload).inventoryItems.firstOrNull { it.itemCode == itemId }
            ?: throw IllegalStateException("Item $itemId no encontrado en sync.bootstrap")
        return ItemDto(
            itemCode = item.itemCode,
            itemName = item.name,
            itemGroup = item.itemGroup,
            description = item.description,
            brand = item.brand,
            image = item.image,
            disabled = false,
            stockUom = item.stockUom,
            standardRate = item.price,
            isStockItem = item.isStocked,
            isSalesItem = true
        )
    }

    suspend fun openCashbox(pos: POSOpeningEntryDto): POSOpeningEntryResponseDto {
        val tokenUser = store.loadUser()?.trim().orEmpty()
        if (tokenUser.isBlank()) {
            store.clear()
            throw IllegalStateException(
                "Sesion invalida: no hay usuario autenticado para abrir caja. Vuelve a iniciar sesion."
            )
        }
        val authenticated = runCatching { fetchAuthenticatedUser() }
            .onFailure {
                AppLogger.warn("openCashbox: get_authenticated_user failed", it)
            }
            .getOrNull()
            ?: throw IllegalStateException(
                "No se pudo resolver el usuario autenticado del servidor. Vuelve a iniciar sesion."
            )
        val authenticatedUser =
            authenticated.name.trim().ifBlank { authenticated.username?.trim().orEmpty() }
        if (authenticatedUser.isBlank()) {
            throw IllegalStateException(
                "El endpoint get_authenticated_user no devolvio un identificador de usuario valido."
            )
        }
        val requestedUser = pos.user?.trim()
        if (!requestedUser.isNullOrBlank() && !requestedUser.equals(
                authenticatedUser,
                ignoreCase = true
            )
        ) {
            throw IllegalArgumentException(
                "payload.user ($requestedUser) no coincide con el usuario autenticado ($authenticatedUser)."
            )
        }
        if (!tokenUser.equals(authenticatedUser, ignoreCase = true)) {
            AppLogger.warn(
                "openCashbox: token user ($tokenUser) differs from authenticated server user ($authenticatedUser)."
            )
        }
        val payload = buildJsonObject {
            put("pos_profile", JsonPrimitive(pos.posProfile))
            put("company", JsonPrimitive(pos.company))
            put("user", JsonPrimitive(authenticatedUser))
            put("period_start_date", JsonPrimitive(pos.periodStartDate))
            put("period_end_date", JsonPrimitive(pos.postingDate))
            put(
                "balance_details",
                json.parseToJsonElement(json.encodeToString(pos.balanceDetails))
            )
            pos.taxes?.let { taxes ->
                put("taxes", json.parseToJsonElement(json.encodeToString(taxes)))
            }
            pos.docStatus?.let { docStatus ->
                put("docstatus", JsonPrimitive(docStatus))
            }
        }
        return runCatching {
            val response: POSOpeningEntryResponseDto = postMethodWithPayload(
                methodPath = "erpnext_pos.api.v1.pos_session.opening_create_submit",
                payload = payload
            )
            response
        }.getOrElse { error ->
            val mismatch = error.message
                ?.contains("payload.user must match authenticated user", ignoreCase = true) == true
            if (mismatch) {
                store.clear()
                throw IllegalStateException(
                    "Sesion invalida: el usuario autenticado no coincide con payload.user. Vuelve a iniciar sesion."
                )
            }
            throw error
        }
    }

    suspend fun getOpenPOSOpeningEntries(
        user: String,
        posProfile: String
    ): List<POSOpeningEntrySummaryDto> {
        return runCatching {
            val payload = BootstrapRequestDto(
                includeInventory = false,
                includeCustomers = false,
                includeInvoices = false,
                includeAlerts = false,
                includeActivity = false,
                profileName = posProfile,
                recentPaidOnly = true
            )
            val openShift = fetchBootstrapRaw(payload)["open_shift"]?.jsonObject
                ?: return@runCatching emptyList()
            val shiftUser = openShift.stringOrNull("user")
            val shiftProfile = openShift.stringOrNull("pos_profile")
            if (!shiftProfile.equals(posProfile, ignoreCase = true)) return@runCatching emptyList()
            val requestedUser = user.trim()
            if (requestedUser.isNotBlank() &&
                !shiftUser.isNullOrBlank() &&
                !isSameUserIdentifier(shiftUser, requestedUser)
            ) {
                return@runCatching emptyList()
            }
            listOf(
                POSOpeningEntrySummaryDto(
                    name = openShift.stringOrNull("name").orEmpty(),
                    posProfile = shiftProfile.orEmpty(),
                    user = shiftUser,
                    status = openShift.stringOrNull("status"),
                    docStatus = openShift.intOrNull("docstatus"),
                    periodStartDate = openShift.stringOrNull("period_start_date")
                )
            ).filter { it.name.isNotBlank() && it.posProfile.isNotBlank() }
        }.getOrElse {
            AppLogger.warn("getOpenPOSOpeningEntries failed", it)
            emptyList()
        }
    }

    suspend fun getPOSOpeningEntry(name: String): POSOpeningEntryDetailDto {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = false,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = true
        )
        val openShift = fetchBootstrapRaw(payload)["open_shift"]?.jsonObject
            ?: throw IllegalStateException("Open shift not available for POS Opening Entry lookup")
        val openName = openShift.stringOrNull("name")
        if (!openName.equals(name, ignoreCase = true)) {
            throw IllegalStateException("POS Opening Entry $name is not active in current shift")
        }
        val balances = openShift["balance_details"]?.jsonArray
            ?.mapNotNull { element ->
                val row = element as? JsonObject ?: return@mapNotNull null
                val mode = row.stringOrNull("mode_of_payment") ?: return@mapNotNull null
                val opening = row["opening_amount"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                BalanceDetailsDto(
                    modeOfPayment = mode,
                    openingAmount = opening,
                    closingAmount = null
                )
            }
            .orEmpty()
        return POSOpeningEntryDetailDto(
            name = openName.orEmpty(),
            posProfile = openShift.stringOrNull("pos_profile").orEmpty(),
            company = openShift.stringOrNull("company").orEmpty(),
            periodStartDate = openShift.stringOrNull("period_start_date")
                ?: openShift.stringOrNull("posting_date")
                ?: DateTimeProvider.todayDate(),
            postingDate = openShift.stringOrNull("posting_date")
                ?: openShift.stringOrNull("period_start_date")
                ?: DateTimeProvider.todayDate(),
            user = openShift.stringOrNull("user"),
            balanceDetails = balances
        )
    }

    suspend fun getBootstrapOpenShift(): POSOpeningEntryDetailDto? {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = false,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = true
        )
        val openShift = fetchBootstrapRaw(payload)["open_shift"]?.jsonObject ?: return null
        val name = openShift.stringOrNull("name") ?: return null
        return runCatching { getPOSOpeningEntry(name) }.getOrNull()
    }

    suspend fun cancelSalesInvoice(name: String): SubmitResponseDto {
        val url = authStore.getCurrentSite()
        if (url.isNullOrBlank()) throw Exception("URL Invalida")
        val endpoint = url.trimEnd('/') + "/api/method/erpnext_pos.api.v1.sales_invoice.cancel"
        return try {
            val response = withRetries {
                client.post {
                    url { takeFrom(endpoint) }
                    contentType(ContentType.Application.Json)
                    setBody(
                        buildJsonObject {
                            put("payload", buildJsonObject { put("name", JsonPrimitive(name)) })
                        }
                    )
                }
            }
            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                try {
                    val err = json.decodeFromString<FrappeErrorResponse>(bodyText)
                    throw FrappeException(err.exception ?: "Error: ${response.status.value}", err)
                } catch (e: Exception) {
                    throw Exception(
                        "Error en cancelSalesInvoice: ${response.status} - $bodyText",
                        e
                    )
                }
            }
            decodeMethodData(bodyText)
        } catch (e: Exception) {
            AppSentry.capture(e, "cancelSalesInvoice failed")
            AppLogger.warn("cancelSalesInvoice failed", e)
            throw e
        }
    }

    private inline fun <reified T> decodeMethodMessage(bodyText: String): T {
        val parsed = json.parseToJsonElement(bodyText).jsonObject
        val messageElement = parsed["message"]
            ?: throw FrappeException(
                "La respuesta no contiene 'message'. Respuesta: $bodyText"
            )
        return json.decodeFromJsonElement(messageElement)
    }

    private fun decodeMethodMessageAsObject(bodyText: String): JsonObject {
        return decodeMethodMessage(bodyText)
    }

    private fun extractMethodDataOrThrow(message: JsonObject): JsonObject {
        val success = message["success"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
            ?: true
        if (!success) {
            val errorMessage = message["error"]
                ?.jsonObject
                ?.get("message")
                ?.jsonPrimitive
                ?.contentOrNull
                ?: "Error en API v1"
            throw IllegalStateException(errorMessage)
        }
        return message["data"]?.jsonObject
            ?: throw FrappeException("La respuesta no contiene 'message.data'.")
    }

    private inline fun <reified T> decodeMethodData(bodyText: String): T {
        val messageObj = decodeMethodMessage<JsonObject>(bodyText)
        val success = messageObj["success"]?.jsonPrimitive?.contentOrNull?.toBooleanStrictOrNull()
            ?: true
        if (!success) {
            val errorMessage = messageObj["error"]
                ?.jsonObject
                ?.get("message")
                ?.jsonPrimitive
                ?.contentOrNull
                ?: "Error en API v1"
            throw IllegalStateException(errorMessage)
        }
        val dataElement = messageObj["data"]
            ?: throw FrappeException("La respuesta no contiene 'message.data'. Respuesta: $bodyText")
        return json.decodeFromJsonElement(dataElement)
    }

    private suspend inline fun <reified T, reified R> postMethodWithPayload(
        methodPath: String,
        payload: T
    ): R {
        val url = authStore.getCurrentSite()
        if (url.isNullOrBlank()) throw Exception("URL Invalida")
        val endpoint = url.trimEnd('/') + "/api/method/$methodPath"
        val response = withRetries {
            client.post {
                url { takeFrom(endpoint) }
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("payload", json.parseToJsonElement(json.encodeToString(payload)))
                    }
                )
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            try {
                val err = json.decodeFromString<FrappeErrorResponse>(bodyText)
                throw FrappeException(err.exception ?: "Error: ${response.status.value}", err)
            } catch (e: Exception) {
                throw Exception("Error en $methodPath: ${response.status} - $bodyText", e)
            }
        }
        return decodeMethodData(bodyText)
    }

    private suspend fun fetchBootstrap(payload: BootstrapRequestDto): BootstrapDataDto {
        requireAuthenticatedSession("sync.bootstrap")
        val site = authStore.getCurrentSite()?.trim()?.trimEnd('/').orEmpty()
        val cacheKey = "$site::${json.encodeToString(payload)}"
        val now = Clock.System.now().toEpochMilliseconds()
        bootstrapCache?.let { cached ->
            if (cached.key == cacheKey && now - cached.createdAtMillis <= BOOTSTRAP_CACHE_WINDOW_MS) {
                return cached.data
            }
        }
        val fresh: BootstrapDataDto = postMethodWithPayload(
            methodPath = "erpnext_pos.api.v1.sync.bootstrap",
            payload = payload
        )
        bootstrapCache = BootstrapCacheEntry(
            key = cacheKey,
            data = fresh,
            createdAtMillis = now
        )
        return fresh
    }

    private suspend fun fetchBootstrapRaw(payload: BootstrapRequestDto): JsonObject {
        requireAuthenticatedSession("sync.bootstrap")
        return postMethodWithPayload(
            methodPath = "erpnext_pos.api.v1.sync.bootstrap",
            payload = payload
        )
    }

    private suspend fun requireAuthenticatedSession(operation: String) {
        val tokens = store.load()
        if (tokens?.access_token.isNullOrBlank()) {
            throw IllegalStateException("Sesion no autenticada: $operation requiere login")
        }
    }

    private suspend fun fetchAuthenticatedUser(): UserDto {
        requireAuthenticatedSession("user.get_authenticated_user")
        val data: JsonObject = postMethodWithPayload(
            methodPath = "erpnext_pos.api.v1.user.get_authenticated_user",
            payload = buildJsonObject {}
        )
        val payload = when {
            data["data"] is JsonObject -> data["data"]!!.jsonObject
            data["user"] is JsonObject -> data["user"]!!.jsonObject
            else -> data
        }
        val userId = payload.stringOrNull("user")
            ?: payload.stringOrNull("name")
            ?: throw IllegalStateException(
                "get_authenticated_user no devolvio campo user/name."
            )
        val username = payload.stringOrNull("username") ?: userId
        val fullName = payload.stringOrNull("full_name")
        val firstName = payload.stringOrNull("first_name")
            ?: fullName?.substringBefore(" ")?.takeIf { it.isNotBlank() }
            ?: username
        val email = payload.stringOrNull("email")
            ?: store.loadUser()?.trim()?.takeIf { it.contains("@") }
            ?: username
        val enabled = when (payload["enabled"]?.jsonPrimitive?.contentOrNull?.lowercase()) {
            "0", "false", "no" -> false
            "1", "true", "yes" -> true
            else -> true
        }
        return UserDto(
            name = userId,
            username = username,
            firstName = firstName,
            lastName = payload.stringOrNull("last_name"),
            email = email,
            image = payload.stringOrNull("image"),
            language = payload.stringOrNull("language") ?: "es",
            timeZone = payload.stringOrNull("time_zone"),
            fullName = fullName,
            enabled = enabled
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? {
        return this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.intOrNull(key: String): Int? {
        val raw = this[key]?.jsonPrimitive?.contentOrNull ?: return null
        return raw.toIntOrNull()
    }

    private fun isSameUserIdentifier(left: String?, right: String?): Boolean {
        val normalizedLeft = left?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return false
        val normalizedRight = right?.trim()?.lowercase()?.takeIf { it.isNotBlank() } ?: return false
        return normalizedLeft == normalizedRight
    }

    suspend fun closeCashbox(entry: POSClosingEntryDto): POSClosingEntryResponse {
        return postMethodWithPayload(
            methodPath = "erpnext_pos.api.v1.pos_session.closing_create_submit",
            payload = entry
        )
    }

    suspend fun getPOSClosingEntriesForOpening(
        openingEntryName: String
    ): List<POSClosingEntrySummaryDto> {
        val opening = openingEntryName.trim()
        if (opening.isBlank()) return emptyList()

        // Primary API v1 lookup for a closing entry bound to a specific opening entry.
        val primary = runCatching {
            postMethodWithPayload<JsonObject, List<POSClosingEntrySummaryDto>>(
                methodPath = "erpnext_pos.api.v1.pos_session.closing_for_opening",
                payload = buildJsonObject {
                    put("pos_opening_entry", JsonPrimitive(opening))
                }
            )
        }.getOrNull()
        if (!primary.isNullOrEmpty()) return primary

        // Bootstrap fallback: if backend exposes the linked closing id in open_shift payload.
        val bootstrapShift = runCatching {
            fetchBootstrapRaw(
                BootstrapRequestDto(
                    includeInventory = false,
                    includeCustomers = false,
                    includeInvoices = false,
                    includeAlerts = false,
                    includeActivity = false,
                    recentPaidOnly = true
                )
            )["open_shift"]?.jsonObject
        }.getOrNull() ?: return emptyList()
        val openingInShift = bootstrapShift.stringOrNull("name")
        if (!openingInShift.equals(opening, ignoreCase = true)) return emptyList()
        val closingName = bootstrapShift.stringOrNull("pos_closing_entry") ?: return emptyList()
        return listOf(
            POSClosingEntrySummaryDto(
                name = closingName,
                posOpeningEntry = openingInShift,
                periodEndDate = bootstrapShift.stringOrNull("period_end_date"),
                postingDate = bootstrapShift.stringOrNull("posting_date"),
                docstatus = 1
            )
        )
    }

    suspend fun getPOSProfileDetails(profileId: String): POSProfileDto {
        return postMethodWithPayload(
            methodPath = "erpnext_pos.api.v1.pos_profile.detail",
            payload = buildJsonObject {
                put("profile_name", JsonPrimitive(profileId))
            }
        )
    }

    suspend fun getPOSProfiles(): List<POSProfileSimpleDto> {
        val data: JsonObject = postMethodWithPayload(
            methodPath = "erpnext_pos.api.v1.sync.my_pos_profiles",
            payload = buildJsonObject {}
        )
        val profilesElement = data["profiles"] ?: return emptyList()
        return json.decodeFromJsonElement(profilesElement)
    }

    suspend fun getBootstrapPosSyncSnapshot(profileName: String? = null): BootstrapPosSyncDto {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = false,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = true,
            profileName = profileName
        )
        val data = fetchBootstrapRaw(payload)
        return json.decodeFromJsonElement(data)
    }

    suspend fun getLoginWithSite(site: String): LoginInfo {
        val normalizedSite = normalizeUrl(site)
        val platform = if (getPlatformName() == "Desktop") "desktop" else "mobile"
        val endpoint =
            normalizedSite.trimEnd('/') + "/api/method/erpnext_pos.api.v1.discovery.resolve_site"
        val response = withRetries {
            client.post {
                url { takeFrom(endpoint) }
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("site_url", JsonPrimitive(normalizedSite))
                        put("platform", JsonPrimitive(platform))
                    }
                )
            }
        }
        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            try {
                val err = json.decodeFromString<FrappeErrorResponse>(bodyText)
                throw FrappeException(err.exception ?: "Error: ${response.status.value}", err)
            } catch (e: Exception) {
                throw Exception(
                    "Error en discovery.resolve_site: ${response.status} - $bodyText",
                    e
                )
            }
        }
        val message = decodeMethodMessageAsObject(bodyText)
        val data = extractMethodDataOrThrow(message)
        val clientId = data.stringOrNull("clientId") ?: data.stringOrNull("client_id")
        if (clientId.isNullOrBlank()) {
            throw IllegalStateException("Discovery no retornó clientId/client_id")
        }
        val redirectUri = data.stringOrNull("redirect_uri")
            ?: if (platform == "desktop") BuildKonfig.DESKTOP_REDIRECT_URI else BuildKonfig.REDIRECT_URI
        val scopes = data["scopes"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
            ?.filter { it.isNotBlank() }
            .orEmpty()
            .ifEmpty { listOf("all", "openid") }
        val companyNode = data["company"] as? JsonObject
        val discoveryCompanyName = listOfNotNull(
            data.stringOrNull("company_name"),
            data.stringOrNull("companyName"),
            data.stringOrNull("company"),
            data.stringOrNull("organization_name"),
            data.stringOrNull("organizationName"),
            companyNode?.stringOrNull("company"),
            companyNode?.stringOrNull("name"),
            companyNode?.stringOrNull("title"),
            companyNode?.stringOrNull("company_name")
        ).firstOrNull { it.isNotBlank() }
        val name = discoveryCompanyName
            ?: data.stringOrNull("name")
            ?: if (platform == "desktop") "ERPNext POS Desktop" else "ERPNext POS Mobile"
        val clientSecret = data.stringOrNull("clientSecret")
            ?: data.stringOrNull("client_secret")
            ?: ""
        return LoginInfo(
            url = normalizedSite,
            redirectUrl = redirectUri,
            clientId = clientId,
            clientSecret = clientSecret,
            scopes = scopes,
            name = name
        )
    }

    // Para Inventario Total: Fetch batch con extras
    suspend fun getInventoryForWarehouse(
        warehouse: String?,
        priceList: String?,
        offset: Int? = null,
        limit: Int? = null,
    ): List<WarehouseItemDto> {
        val warehouseId = warehouse?.trim().orEmpty()
        require(warehouseId.isNotEmpty()) { "Bodega es requerida para la carga de productos" }
        val payload = BootstrapRequestDto(
            includeInventory = true,
            includeCustomers = false,
            includeInvoices = false,
            includeAlerts = true,
            includeActivity = false,
            recentPaidOnly = true,
            warehouse = warehouseId,
            priceList = priceList?.trim(),
            offset = offset,
            limit = limit
        )
        return fetchBootstrap(payload).inventoryItems
    }

    suspend fun fetchStockForItems(
        warehouse: String,
        itemCodes: List<String>
    ): Map<String, Double> {
        if (itemCodes.isEmpty()) return emptyMap()
        val lookup = itemCodes.toSet()
        return getInventoryForWarehouse(warehouse = warehouse, priceList = null)
            .asSequence()
            .filter { it.itemCode in lookup }
            .associate { it.itemCode to it.actualQty.coerceAtLeast(0.0) }
    }

    suspend fun fetchBinsForItems(
        warehouse: String,
        itemCodes: List<String>
    ): List<BinDto> {
        if (itemCodes.isEmpty()) return emptyList()
        val lookup = itemCodes.toSet()
        return getInventoryForWarehouse(warehouse = warehouse, priceList = null)
            .asSequence()
            .filter { it.itemCode in lookup }
            .map { item ->
                BinDto(
                    itemCode = item.itemCode,
                    warehouse = warehouse,
                    actualQty = item.actualQty,
                    reservedQty = 0.0,
                    projectedQty = item.projectedQty ?: item.actualQty,
                    stockUom = item.stockUom,
                    valuationRate = item.valuationRate
                )
            }
            .toList()
    }

    suspend fun fetchItemReordersForItems(
        warehouse: String,
        itemCodes: List<String>
    ): List<ItemReorderDto> {
        if (itemCodes.isEmpty()) return emptyList()
        val lookup = itemCodes.toSet()
        return getInventoryForWarehouse(warehouse = warehouse, priceList = null)
            .asSequence()
            .filter { it.itemCode in lookup }
            .map {
                ItemReorderDto(
                    itemCode = it.itemCode,
                    warehouse = warehouse,
                    reorderLevel = it.stockAlertReorderLevel,
                    reorderQty = it.stockAlertReorderQty
                )
            }
            .toList()
    }

    suspend fun findInvoiceBySignature(
        posOpeningEntry: String?,
        postingDate: String?,
        customer: String?,
        grandTotal: Double?
    ): String? {
        if (posOpeningEntry.isNullOrBlank() ||
            postingDate.isNullOrBlank() ||
            customer.isNullOrBlank() ||
            grandTotal == null
        ) return null
        val profiles = getPOSProfiles().map { it.profileName }.ifEmpty { listOf("") }
        profiles.forEach { profile ->
            val invoices = fetchAllInvoicesCombined(profile, recentPaidOnly = false)
            val found = invoices.firstOrNull { invoice ->
                invoice.posOpeningEntry == posOpeningEntry &&
                        invoice.postingDate.take(10) == postingDate &&
                        invoice.customer == customer &&
                        invoice.grandTotal == grandTotal
            }
            if (found != null) return found.name
        }
        return null
    }

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun getCustomers(territory: String?): List<CustomerDto> {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = true,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            route = territory,
            territory = territory,
            recentPaidOnly = true
        )
        return fetchBootstrap(payload).customers
    }

    suspend fun fetchCustomersBootstrapSnapshot(
        profileName: String?,
        territory: String?,
        recentPaidOnly: Boolean
    ): BootstrapDataDto {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = true,
            includeInvoices = true,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = recentPaidOnly,
            profileName = profileName,
            route = territory,
            territory = territory
        )
        return fetchBootstrap(payload)
    }

    suspend fun fetchActivityBootstrapSnapshot(
        profileName: String?,
        territory: String?
    ): BootstrapDataDto {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = false,
            includeInvoices = false,
            includeAlerts = true,
            includeActivity = true,
            recentPaidOnly = true,
            profileName = profileName,
            route = territory,
            territory = territory
        )
        return fetchBootstrap(payload)
    }

    //Para monto total pendientes y List (method whitelisted)
    suspend fun getCustomerOutstanding(customer: String, posProfile: String): OutstandingInfo {
        val data: JsonObject = postMethodWithPayload(
            methodPath = "erpnext_pos.api.v1.customer.outstanding",
            payload = buildJsonObject {
                put("customer", JsonPrimitive(customer))
                put("pos_profile", JsonPrimitive(posProfile))
            }
        )
        val invoices = runCatching {
            json.decodeFromJsonElement<List<SalesInvoiceDto>>(
                data["pendingInvoices"] ?: JsonArray(
                    emptyList()
                )
            )
        }.getOrElse { emptyList() }
        val totalOutstanding = data["totalOutstanding"]?.jsonPrimitive?.doubleOrNull
            ?: data["outstanding"]?.jsonPrimitive?.doubleOrNull
            ?: invoices.sumOf { invoice ->
                invoice.outstandingAmount ?: (invoice.grandTotal - (invoice.paidAmount ?: 0.0))
            }
        return OutstandingInfo(totalOutstanding, invoices)
    }

    suspend fun fetchCustomerInvoicesForPeriod(
        customer: String,
        startDate: String,
        endDate: String,
        posProfile: String
    ): List<SalesInvoiceDto> {
        return fetchAllInvoicesCombined(posProfile, recentPaidOnly = false)
            .asSequence()
            .filter { it.customer == customer }
            .filter { invoice ->
                val posting = invoice.postingDate.take(10)
                posting >= startDate && posting <= endDate
            }
            .sortedByDescending { it.postingDate }
            .toList()
    }

    // Batch method for all outstanding invoices
    suspend fun getAllOutstandingInvoices(posProfile: String): List<SalesInvoiceDto> {
        return fetchAllInvoicesCombined(posProfile, recentPaidOnly = true).filter { invoice ->
            val outstanding =
                invoice.outstandingAmount ?: (invoice.grandTotal - (invoice.paidAmount ?: 0.0))
            outstanding > 0.0
        }
    }

    //Para facturas pendientes (lista simple de overdue)
    suspend fun fetchAllInvoices(
        posProfile: String, offset: Int = 0, limit: Int = Int.MAX_VALUE
    ): List<SalesInvoiceDto> {
        return try {
            val today = DateTimeProvider.todayDate()
            val startDate = DateTimeProvider.addDays(today, -DEFAULT_INVOICE_SYNC_DAYS)
            fetchAllInvoicesCombined(posProfile, recentPaidOnly = false)
                .asSequence()
                .filter { it.postingDate.take(10) >= startDate }
                .filter {
                    it.status in listOf(
                        "Draft",
                        "Unpaid",
                        "Overdue",
                        "Paid",
                        "Partly Paid",
                        "Overdue and Discounted",
                        "Unpaid and Discounted",
                        "Partly Paid and Discounted",
                        "Cancelled",
                        "Credit Note Issued",
                        "Return"
                    )
                }
                .drop(offset.coerceAtLeast(0))
                .let { seq ->
                    if (limit == Int.MAX_VALUE) seq else seq.take(limit.coerceAtLeast(0))
                }
                .toList()
        } catch (e: Exception) {
            e.printStackTrace()
            AppSentry.capture(e, "fetchAllInvoices failed")
            AppLogger.warn("fetchAllInvoices failed", e)
            emptyList()
        }
    }

    suspend fun fetchAllInvoicesCombined(
        posProfile: String,
        recentPaidOnly: Boolean = false,
    ): List<SalesInvoiceDto> {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = false,
            includeInvoices = true,
            includeAlerts = false,
            includeActivity = false,
            profileName = posProfile,
            recentPaidOnly = recentPaidOnly
        )
        return fetchBootstrap(payload).invoices
    }

    suspend fun fetchPaymentEntries(fromDate: String): List<PaymentEntryDto> {
        val payload = BootstrapRequestDto(
            includeInventory = false,
            includeCustomers = false,
            includeInvoices = false,
            includeAlerts = false,
            includeActivity = false,
            recentPaidOnly = true
        )
        return fetchBootstrap(payload).paymentEntries
            .asSequence()
            .filter { (it.postingDate?.take(10) ?: "") >= fromDate }
            .sortedByDescending { it.postingDate }
            .toList()
    }

    suspend fun getPaymentEntryByName(name: String): PaymentEntryDto {
        val fromDate = DateTimeProvider.addDays(DateTimeProvider.todayDate(), -90)
        return fetchPaymentEntries(fromDate).firstOrNull { it.name == name }
            ?: throw IllegalStateException("Payment Entry $name no encontrado en bootstrap")
    }

    suspend fun fetchReturnInvoiceNames(
        returnAgainst: String,
        posProfile: String
    ): List<String> {
        return fetchAllInvoicesCombined(posProfile, recentPaidOnly = false)
            .asSequence()
            .filter { it.returnAgainst == returnAgainst && it.isReturn == 1 }
            .mapNotNull { it.name }
            .toList()
    }

    //region Invoice - Checkout
    suspend fun createSalesInvoice(data: SalesInvoiceDto): SalesInvoiceDto {
        val created: JsonObject = postMethodWithPayload(
            methodPath = "erpnext_pos.api.v1.sales_invoice.create_submit",
            payload = data
        )
        return data.copy(
            name = created.stringOrNull("name") ?: data.name,
            docStatus = created.intOrNull("docstatus") ?: 1,
            status = created.stringOrNull("status") ?: data.status,
            postingDate = created.stringOrNull("posting_date") ?: data.postingDate,
            outstandingAmount = created["outstanding_amount"]?.jsonPrimitive?.doubleOrNull
                ?: data.outstandingAmount
        )
    }

    suspend fun getSalesInvoiceByName(name: String): SalesInvoiceDto {
        val profiles = getPOSProfiles().map { it.profileName }.ifEmpty { listOf("") }
        profiles.forEach { profile ->
            val invoices = runCatching {
                fetchAllInvoicesCombined(profile, recentPaidOnly = false)
            }.getOrElse { emptyList() }
            val found = invoices.firstOrNull { it.name == name }
            if (found != null) return found
        }
        throw IllegalStateException("Sales Invoice $name no encontrada en bootstrap")
    }

    suspend fun updateSalesInvoice(name: String, data: SalesInvoiceDto): SalesInvoiceDto {
        throw UnsupportedOperationException(
            "updateSalesInvoice no soportado en API v1. Usa sales_invoice.create_submit con idempotencia."
        )
    }
    //endregion
}

expect fun defaultEngine(): HttpClientEngine

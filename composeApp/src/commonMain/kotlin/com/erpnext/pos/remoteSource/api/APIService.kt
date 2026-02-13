package com.erpnext.pos.remoteSource.api

import com.erpnext.pos.BuildKonfig
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.remoteSource.dto.BinDto
import com.erpnext.pos.remoteSource.dto.BootstrapDataDto
import com.erpnext.pos.remoteSource.dto.BootstrapRequestDto
import com.erpnext.pos.remoteSource.dto.CategoryDto
import com.erpnext.pos.remoteSource.dto.CurrencyDto
import com.erpnext.pos.remoteSource.dto.ContactChildDto
import com.erpnext.pos.remoteSource.dto.ContactListDto
import com.erpnext.pos.remoteSource.dto.ContactUpdateDto
import com.erpnext.pos.remoteSource.dto.CustomerDto
import com.erpnext.pos.remoteSource.dto.CustomerCreateDto
import com.erpnext.pos.remoteSource.dto.AddressCreateDto
import com.erpnext.pos.remoteSource.dto.AddressListDto
import com.erpnext.pos.remoteSource.dto.AddressUpdateDto
import com.erpnext.pos.remoteSource.dto.CompanySalesTargetDto
import com.erpnext.pos.remoteSource.dto.ContactCreateDto
import com.erpnext.pos.remoteSource.dto.DocNameResponseDto
import com.erpnext.pos.remoteSource.dto.DeliveryChargeDto
import com.erpnext.pos.remoteSource.dto.ExchangeRateResponse
import com.erpnext.pos.remoteSource.dto.ItemDto
import com.erpnext.pos.remoteSource.dto.ItemPriceDto
import com.erpnext.pos.remoteSource.dto.ItemReorderDto
import com.erpnext.pos.remoteSource.dto.LoginInfo
import com.erpnext.pos.remoteSource.dto.AccountDetailDto
import com.erpnext.pos.remoteSource.dto.ModeOfPaymentDetailDto
import com.erpnext.pos.remoteSource.dto.ModeOfPaymentDto
import com.erpnext.pos.remoteSource.dto.OutstandingInfo
import com.erpnext.pos.remoteSource.dto.POSClosingEntryDto
import com.erpnext.pos.remoteSource.dto.POSClosingEntryResponse
import com.erpnext.pos.remoteSource.dto.POSClosingEntrySummaryDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDetailDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryResponseDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntrySummaryDto
import com.erpnext.pos.remoteSource.dto.POSProfileDto
import com.erpnext.pos.remoteSource.dto.POSProfileSimpleDto
import com.erpnext.pos.remoteSource.dto.PaymentTermDto
import com.erpnext.pos.remoteSource.dto.PaymentEntryDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.StockSettingsDto
import com.erpnext.pos.remoteSource.dto.SubmitResponseDto
import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.dto.UserDto
import com.erpnext.pos.remoteSource.dto.WarehouseItemDto
import com.erpnext.pos.remoteSource.dto.CustomerGroupDto
import com.erpnext.pos.remoteSource.dto.TerritoryDto
import com.erpnext.pos.remoteSource.dto.CompanyDto
import com.erpnext.pos.remoteSource.dto.CustomerAddressDto
import com.erpnext.pos.remoteSource.dto.PaymentEntryCreateDto
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.OAuthConfig
import com.erpnext.pos.remoteSource.oauth.Pkce
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.remoteSource.oauth.refreshAuthToken
import com.erpnext.pos.remoteSource.oauth.toOAuthConfig
import com.erpnext.pos.remoteSource.sdk.ERPDocType
import com.erpnext.pos.remoteSource.sdk.FrappeErrorResponse
import com.erpnext.pos.remoteSource.sdk.FrappeException
import com.erpnext.pos.remoteSource.sdk.filters
import com.erpnext.pos.remoteSource.sdk.getERPList
import com.erpnext.pos.remoteSource.sdk.getERPSingle
import com.erpnext.pos.remoteSource.sdk.getFields
import com.erpnext.pos.remoteSource.sdk.postERP
import com.erpnext.pos.remoteSource.sdk.putERP
import com.erpnext.pos.remoteSource.sdk.withRetries
import com.erpnext.pos.utils.view.DateTimeProvider
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.get
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.parameter
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Parameters
import io.ktor.http.contentType
import io.ktor.http.encodeURLParameter
import io.ktor.http.formUrlEncode
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import com.erpnext.pos.utils.normalizeUrl
import io.ktor.client.statement.HttpResponse
import io.ktor.http.takeFrom
import kotlinx.serialization.json.put
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
        doctype: String,
        posProfile: String,
        startDate: String,
        statuses: List<String>
    ): List<SalesInvoiceDto> {
        val url = authStore.getCurrentSite()
        return client.getERPList(
            doctype = doctype,
            fields = ERPDocType.SalesInvoice.getFields(),
            baseUrl = url,
            filters = filters {
                "pos_profile" eq posProfile
                "posting_date" gte startDate
                "status" `in` statuses
            }
        )
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

        val profiles = getPOSProfiles(assignedTo = null)
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
        val url = authStore.getCurrentSite()
        val rows = client.getERPList<CompanySalesTargetDto>(
            doctype = ERPDocType.Company.path,
            fields = listOf("name", "monthly_sales_target"),
            limit = 1,
            baseUrl = url
        ) {
            "name" eq companyId
        }
        return rows.firstOrNull()?.monthlySalesTarget
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
        val url = authStore.getCurrentSite()
        return client.getERPList(
            ERPDocType.SalesInvoice.path,
            ERPDocType.SalesInvoice.getFields(),
            baseUrl = url,
            orderBy = "posting_date desc",
            filters = filters {
                "territory" eq territory
                "posting_date" gte fromDate
            })
    }

    suspend fun fetchPaymentTerms(): List<PaymentTermDto> {
        val url = authStore.getCurrentSite()
        return client.getERPList(
            ERPDocType.PaymentTerm.path, listOf(
                "payment_term_name",
                "invoice_portion",
                "mode_of_payment",
                "due_date_based_on",
                "credit_days",
                "credit_months",
                "discount_type",
                "discount",
                "description",
                "discount_validity",
                "discount_validity_based_on"
            ), baseUrl = url
        )
    }

    suspend fun fetchDeliveryCharges(): List<DeliveryChargeDto> {
        val url = authStore.getCurrentSite()
        return client.getERPList(
            ERPDocType.DeliveryCharges.path, ERPDocType.DeliveryCharges.getFields(), baseUrl = url
        )
    }

    suspend fun fetchCustomerGroups(): List<CustomerGroupDto> {
        val url = authStore.getCurrentSite()
        return client.getERPList(
            ERPDocType.CustomerGroup.path,
            ERPDocType.CustomerGroup.getFields(),
            baseUrl = url
        )
    }

    suspend fun fetchTerritories(): List<TerritoryDto> {
        val url = authStore.getCurrentSite()
        return client.getERPList(
            ERPDocType.Territory.path,
            ERPDocType.Territory.getFields(),
            baseUrl = url
        )
    }

    suspend fun fetchCustomerContacts(): List<ContactListDto> {
        val url = authStore.getCurrentSite()
        return client.getERPList(
            ERPDocType.CustomerContact.path,
            listOf("name", "email_id", "mobile_no", "phone", "links"),
            baseUrl = url
        )
    }

    suspend fun fetchCustomerAddresses(): List<AddressListDto> {
        val url = authStore.getCurrentSite()
        return client.getERPList(
            ERPDocType.Address.path,
            listOf(
                "name",
                "address_title",
                "address_type",
                "address_line1",
                "address_line2",
                "city",
                "state",
                "country",
                "email_id",
                "phone",
                "links"
            ),
            baseUrl = url
        )
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

    suspend fun createAddress(payload: AddressCreateDto): DocNameResponseDto {
        val url = authStore.getCurrentSite()
        return client.postERP(ERPDocType.Address.path, payload, baseUrl = url)
    }

    suspend fun createContact(payload: ContactCreateDto): DocNameResponseDto {
        val url = authStore.getCurrentSite()
        return client.postERP(ERPDocType.CustomerContact.path, payload, baseUrl = url)
    }

    suspend fun findCustomerContacts(customerId: String): List<ContactListDto> {
        val url = authStore.getCurrentSite()
        val list: List<ContactListDto> = client.getERPList(
            ERPDocType.CustomerContact.path,
            listOf("name", "email_id", "mobile_no", "phone"),
            baseUrl = url
        )
        return list.filter { dto ->
            dto.links.any { it.linkDoctype == "Customer" && it.linkName == customerId }
        }
    }

    suspend fun findCustomerAddresses(customerId: String): List<AddressListDto> {
        val url = authStore.getCurrentSite()
        val list: List<AddressListDto> = client.getERPList(
            ERPDocType.Address.path,
            listOf(
                "name",
                "address_line1",
                "address_line2",
                "city",
                "state",
                "country",
                "email_id",
                "phone"
            ),
            baseUrl = url
        )
        return list.filter { dto ->
            dto.links.any { it.linkDoctype == "Customer" && it.linkName == customerId }
        }
    }

    suspend fun updateContact(contactId: String, payload: ContactUpdateDto): ContactListDto {
        val url = authStore.getCurrentSite()
        return client.putERP(
            doctype = ERPDocType.CustomerContact.path,
            name = contactId,
            payload = payload,
            baseUrl = url
        )
    }

    suspend fun updateAddress(addressId: String, payload: AddressUpdateDto): AddressListDto {
        val url = authStore.getCurrentSite()
        return client.putERP(
            doctype = ERPDocType.Address.path,
            name = addressId,
            payload = payload,
            baseUrl = url
        )
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

    suspend fun refreshToken(refresh: String): TokenResponse =
        refreshAuthToken(tokenClient, authStore, refresh)

    suspend fun getUserInfo(): UserDto {
        val userId = store.loadUser()
        if (userId.isNullOrBlank()) throw Exception("Usuario Invalido")
        val username = userId.substringBefore("@").ifBlank { userId }
        return UserDto(
            name = userId,
            username = username,
            firstName = username,
            lastName = null,
            email = userId,
            language = "es",
            enabled = true
        )
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
        val url = authStore.getCurrentSite() ?: return emptyList()
        return client.getERPList(
            doctype = "Currency",
            fields = listOf("name", "currency_name", "symbol", "number_format"),
            baseUrl = url,
            filters = filters {
                "enabled" eq 1
            })
    }

    suspend fun getSystemSettingsRaw(): JsonObject? {
        val url = authStore.getCurrentSite() ?: return null
        return runCatching {
            client.getERPSingle<JsonObject>(
                doctype = "System Settings",
                name = "System Settings",
                baseUrl = url
            )
        }.getOrNull()
    }

    suspend fun getCurrencyDetail(code: String): JsonObject? {
        val url = authStore.getCurrentSite() ?: return null
        val name = code.trim().uppercase().encodeURLParameter()
        return runCatching {
            client.getERPSingle<JsonObject>(
                doctype = "Currency",
                name = name,
                baseUrl = url
            )
        }.getOrNull()
    }

    suspend fun getActiveModeOfPayment(): List<ModeOfPaymentDto> {
        val url = authStore.getCurrentSite() ?: return emptyList()
        return runCatching {
            client.getERPList<ModeOfPaymentDto>(
                doctype = ERPDocType.ModeOfPayment.path,
                fields = ERPDocType.ModeOfPayment.getFields(),
                baseUrl = url,
                filters = filters {
                    "enabled" eq 1
                })
        }.getOrElse {
            AppLogger.warn("getActiveModeOfPayment failed", it)
            emptyList()
        }
    }

    suspend fun getModeOfPaymentDetail(name: String): ModeOfPaymentDetailDto? {
        val url = authStore.getCurrentSite() ?: return null
        return runCatching {
            client.getERPSingle<ModeOfPaymentDetailDto>(
                doctype = ERPDocType.ModeOfPayment.path,
                name = name.encodeURLParameter(),
                baseUrl = url
            )
        }.getOrNull()
    }

    suspend fun getAccountDetail(name: String): AccountDetailDto? {
        val url = authStore.getCurrentSite() ?: return null
        return runCatching {
            client.getERPSingle<AccountDetailDto>(
                doctype = ERPDocType.Account.path, name = name.encodeURLParameter(), baseUrl = url
            )
        }.getOrNull()
    }

    suspend fun getCategories(): List<CategoryDto> {
        val url = authStore.getCurrentSite() ?: ""
        return client.getERPList<CategoryDto>(
            ERPDocType.Category.path,
            ERPDocType.Category.getFields(),
            orderBy = "name asc",
            baseUrl = url
        )
    }

    suspend fun getItemDetail(itemId: String): ItemDto {
        val url = authStore.getCurrentSite()
        if (url.isNullOrEmpty()) throw Exception("URL Invalida")

        return client.getERPSingle(
            doctype = ERPDocType.Item.path, name = itemId, baseUrl = url
        )
    }

    suspend fun openCashbox(pos: POSOpeningEntryDto): POSOpeningEntryResponseDto {
        return postMethodWithPayload(
            methodPath = "erpnext_pos.api.v1.pos_session.opening_create_submit",
            payload = pos
        )
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
            val openShift = fetchBootstrapRaw(payload)["open_shift"]?.jsonObject ?: return@runCatching emptyList()
            val shiftUser = openShift.stringOrNull("user")
            val shiftProfile = openShift.stringOrNull("pos_profile")
            if (!shiftProfile.equals(posProfile, ignoreCase = true)) return@runCatching emptyList()
            if (!shiftUser.isNullOrBlank() && !shiftUser.equals(user, ignoreCase = true)) return@runCatching emptyList()
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

    suspend fun getOpenPOSOpeningEntriesForProfile(
        posProfile: String
    ): List<POSOpeningEntrySummaryDto> {
        return getOpenPOSOpeningEntries(user = "", posProfile = posProfile)
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
            balanceDetails = emptyList()
        )
    }

    suspend fun submitPOSOpeningEntry(name: String): SubmitResponseDto {
        throw UnsupportedOperationException(
            "submitPOSOpeningEntry legado removido. Usa erpnext_pos.api.v1.pos_session.opening_create_submit."
        )
    }

    suspend fun submitPOSClosingEntry(name: String): SubmitResponseDto {
        throw UnsupportedOperationException(
            "submitPOSClosingEntry legado removido. Usa erpnext_pos.api.v1.pos_session.closing_create_submit."
        )
    }

    suspend fun submitSalesInvoice(name: String): SubmitResponseDto {
        throw UnsupportedOperationException(
            "submitSalesInvoice legado removido. Usa erpnext_pos.api.v1.sales_invoice.create_submit."
        )
    }

    suspend fun submitPaymentEntry(name: String): SubmitResponseDto {
        throw UnsupportedOperationException(
            "submitPaymentEntry legado removido. Usa erpnext_pos.api.v1.payment_entry.create_submit."
        )
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
                    throw Exception("Error en cancelSalesInvoice: ${response.status} - $bodyText", e)
                }
            }
            decodeMethodData(bodyText)
        } catch (e: Exception) {
            AppSentry.capture(e, "cancelSalesInvoice failed")
            AppLogger.warn("cancelSalesInvoice failed", e)
            throw e
        }
    }

    suspend fun setValue(
        doctype: String,
        name: String,
        fieldname: String,
        value: String
    ) {
        throw UnsupportedOperationException(
            "setValue legado removido. Usa erpnext_pos.api.v1.settings.mobile_update."
        )
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
        val cacheKey = json.encodeToString(payload)
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
        return postMethodWithPayload(
            methodPath = "erpnext_pos.api.v1.sync.bootstrap",
            payload = payload
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? {
        return this[key]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
    }

    private fun JsonObject.intOrNull(key: String): Int? {
        val raw = this[key]?.jsonPrimitive?.contentOrNull ?: return null
        return raw.toIntOrNull()
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
        AppLogger.warn("getPOSClosingEntriesForOpening: no dedicated whitelist endpoint available yet")
        return emptyList()
    }

    suspend fun getPOSProfileDetails(profileId: String): POSProfileDto {
        return postMethodWithPayload(
            methodPath = "erpnext_pos.api.v1.pos_profile.detail",
            payload = buildJsonObject {
                put("profile_name", JsonPrimitive(profileId))
            }
        )
    }

    suspend fun getPOSProfiles(assignedTo: String?): List<POSProfileSimpleDto> {
        val data: JsonObject = postMethodWithPayload(
            methodPath = "erpnext_pos.api.v1.sync.my_pos_profiles",
            payload = buildJsonObject {}
        )
        val profilesElement = data["profiles"] ?: return emptyList()
        return json.decodeFromJsonElement(profilesElement)
    }

    suspend fun getLoginWithSite(site: String): LoginInfo {
        val normalizedSite = normalizeUrl(site)
        val platform = if (getPlatformName() == "Desktop") "desktop" else "mobile"
        val endpoint = normalizedSite.trimEnd('/') + "/api/method/erpnext_pos.api.v1.discovery.resolve_site"
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
                throw Exception("Error en discovery.resolve_site: ${response.status} - $bodyText", e)
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
        val name = data.stringOrNull("name")
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
        val url = authStore.getCurrentSite() ?: throw Exception("URL Invalida")
        val bins = client.getERPList<BinDto>(
            doctype = ERPDocType.Bin.path,
            fields = listOf("item_code", "warehouse", "stock_uom", "actual_qty", "reserved_qty"),
            limit = itemCodes.size,
            baseUrl = url
        ) {
            "warehouse" eq warehouse
            "item_code" `in` itemCodes
        }
        return bins.associate { bin ->
            val sellableQty = (bin.actualQty - (bin.reservedQty ?: 0.0)).coerceAtLeast(0.0)
            bin.itemCode to sellableQty
        }
    }

    suspend fun fetchBinsForItems(
        warehouse: String,
        itemCodes: List<String>
    ): List<BinDto> {
        if (itemCodes.isEmpty()) return emptyList()
        val url = authStore.getCurrentSite() ?: throw Exception("URL Invalida")
        val chunkSize = 50
        return itemCodes.chunked(chunkSize).flatMap { codes ->
            client.getERPList<BinDto>(
                doctype = ERPDocType.Bin.path,
                fields = listOf(
                    "item_code",
                    "warehouse",
                    "stock_uom",
                    "actual_qty",
                    "projected_qty",
                    "reserved_qty"
                ),
                limit = codes.size,
                baseUrl = url
            ) {
                "warehouse" eq warehouse
                "item_code" `in` codes
            }
        }
    }

    suspend fun fetchItemReordersForItems(
        warehouse: String,
        itemCodes: List<String>
    ): List<ItemReorderDto> {
        if (itemCodes.isEmpty()) return emptyList()
        val url = authStore.getCurrentSite() ?: throw Exception("URL Invalida")
        val chunkSize = 50
        return itemCodes.chunked(chunkSize).flatMap { codes ->
            val rows = client.getERPList<JsonObject>(
                doctype = ERPDocType.Item.path,
                fields = listOf("name", "reorder_levels"),
                limit = codes.size,
                baseUrl = url
            ) {
                "name" `in` codes
            }

            rows.mapNotNull { row ->
                val itemCode = row["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val reorderLevels = decodeReorderLevels(row["reorder_levels"])
                val warehouseMatch = reorderLevels.firstOrNull {
                    val wh = it["warehouse"]?.jsonPrimitive?.contentOrNull
                    wh.equals(warehouse, ignoreCase = true)
                } ?: return@mapNotNull null

                ItemReorderDto(
                    itemCode = itemCode,
                    warehouse = warehouse,
                    reorderLevel = warehouseMatch["warehouse_reorder_level"]?.asDoubleOrNull(),
                    reorderQty = warehouseMatch["warehouse_reorder_qty"]?.asDoubleOrNull()
                )
            }
        }
    }

    private fun decodeReorderLevels(raw: JsonElement?): List<JsonObject> {
        if (raw == null) return emptyList()
        val parsed: JsonElement = when (raw) {
            is JsonArray -> raw
            is JsonPrimitive -> {
                val content = raw.contentOrNull ?: return emptyList()
                runCatching { json.parseToJsonElement(content) }.getOrNull() ?: return emptyList()
            }

            else -> return emptyList()
        }
        return (parsed as? JsonArray)
            ?.mapNotNull { it as? JsonObject }
            .orEmpty()
    }

    private fun JsonElement.asDoubleOrNull(): Double? {
        val primitive = this as? JsonPrimitive ?: return null
        return primitive.doubleOrNull ?: primitive.contentOrNull?.toDoubleOrNull()
    }

    suspend fun findInvoiceBySignature(
        doctype: String,
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
        val url = authStore.getCurrentSite()
        return client.getERPList<SalesInvoiceDto>(
            doctype = doctype,
            fields = listOf("name", "docstatus"),
            baseUrl = url,
            limit = 1,
            orderBy = "modified desc",
            filters = filters {
                "pos_opening_entry" eq posOpeningEntry
                "posting_date" eq postingDate
                "customer" eq customer
                "grand_total" eq grandTotal
            }
        ).firstOrNull()?.name
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
            json.decodeFromJsonElement<List<SalesInvoiceDto>>(data["pendingInvoices"] ?: JsonArray(emptyList()))
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


    suspend fun getCustomerContact(customerId: String): ContactChildDto? {
        val url = authStore.getCurrentSite()
        return client.getERPList<ContactChildDto>(
            doctype = ERPDocType.CustomerContact.path,
            fields = listOf("name", "mobile_no", "email_id", "phone"),
            baseUrl = url,
            filters = filters {
                "link_doctype" eq "Customer"
                "link_name" eq customerId
            }
        ).firstOrNull()
    }

    suspend fun getCustomerAddress(customerId: String): CustomerAddressDto? {
        val url = authStore.getCurrentSite()
        return client.getERPList<CustomerAddressDto>(
            doctype = "Address",
            fields = listOf(
                "name",
                "address_title",
                "address_type",
                "address_line1",
                "address_line2",
                "city",
                "country"
            ),
            baseUrl = url,
            filters = filters {
                "link_doctype" eq "Customer"
                "link_name" eq customerId
            }
        ).firstOrNull()
    }

    // Batch method for all outstanding invoices
    suspend fun getAllOutstandingInvoices(posProfile: String): List<SalesInvoiceDto> {
        return fetchAllInvoicesCombined(posProfile, recentPaidOnly = true).filter { invoice ->
            val outstanding = invoice.outstandingAmount ?: (invoice.grandTotal - (invoice.paidAmount ?: 0.0))
            outstanding > 0.0
        }
    }

    //Para facturas pendientes (lista simple de overdue)
    suspend fun fetchAllInvoices(
        posProfile: String, offset: Int = 0, limit: Int = Int.MAX_VALUE
    ): List<SalesInvoiceDto> {
        return try {
            val url = authStore.getCurrentSite()
            val today = DateTimeProvider.todayDate()
            val startDate = DateTimeProvider.addDays(today, -DEFAULT_INVOICE_SYNC_DAYS)
            client.getERPList(
                doctype = ERPDocType.SalesInvoice.path,
                fields = ERPDocType.SalesInvoice.getFields(),
                offset = offset,
                limit = limit,
                baseUrl = url,
                filters = filters {
                    "pos_profile" eq posProfile
                    "posting_date" gte startDate
                    "status" `in` listOf(
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
                })
        } catch (e: Exception) {
            e.printStackTrace()
            AppSentry.capture(e, "fetchAllInvoices failed")
            AppLogger.warn("fetchAllInvoices failed", e)
            emptyList()
        }
    }

    suspend fun fetchAllInvoicesSmart(
        posProfile: String,
        paidDays: Int = RECENT_PAID_INVOICE_DAYS
    ): List<SalesInvoiceDto> {
        return try {
            val today = DateTimeProvider.todayDate()
            val startDate = DateTimeProvider.addDays(today, -DEFAULT_INVOICE_SYNC_DAYS)
            val paidStartDate = DateTimeProvider.addDays(today, -paidDays)
            val openStatuses = listOf(
                "Draft",
                "Unpaid",
                "Overdue",
                "Partly Paid",
                "Overdue and Discounted",
                "Unpaid and Discounted",
                "Partly Paid and Discounted",
                "Cancelled",
                "Credit Note Issued",
                "Return"
            )
            val openInvoices = fetchInvoicesByStatus(
                doctype = ERPDocType.SalesInvoice.path,
                posProfile = posProfile,
                startDate = startDate,
                statuses = openStatuses
            )
            val paidInvoices = fetchInvoicesByStatus(
                doctype = ERPDocType.SalesInvoice.path,
                posProfile = posProfile,
                startDate = paidStartDate,
                statuses = listOf("Paid")
            )
            (openInvoices + paidInvoices).distinctBy { it.name }
        } catch (e: Exception) {
            e.printStackTrace()
            AppSentry.capture(e, "fetchAllInvoicesSmart failed")
            AppLogger.warn("fetchAllInvoicesSmart failed", e)
            emptyList()
        }
    }

    suspend fun fetchAllInvoicesCombined(
        posProfile: String,
        recentPaidOnly: Boolean = false,
        paidDays: Int = RECENT_PAID_INVOICE_DAYS
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
        val profiles = getPOSProfiles(assignedTo = null).map { it.profileName }.ifEmpty { listOf("") }
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

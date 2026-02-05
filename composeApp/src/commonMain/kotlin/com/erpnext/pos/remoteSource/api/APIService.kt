package com.erpnext.pos.remoteSource.api

import com.erpnext.pos.BuildKonfig
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.remoteSource.dto.BinDto
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
import com.erpnext.pos.remoteSource.dto.v2.CompanyDto
import com.erpnext.pos.remoteSource.dto.v2.CustomerAddressDto
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryCreateDto
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
import io.ktor.client.statement.HttpResponse
import io.ktor.http.takeFrom
import kotlinx.serialization.json.put

class APIService(
    private val client: HttpClient,
    private val store: TokenStore,
    private val authStore: AuthInfoStore,
    private val tokenClient: HttpClient
) {
    private companion object {
        const val DEFAULT_INVOICE_SYNC_DAYS = 90
        const val RECENT_PAID_INVOICE_DAYS = 7
    }

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
        val url = authStore.getCurrentSite()
        return client.getERPSingle(
            doctype = ERPDocType.Company.path,
            fields = ERPDocType.Company.getFields(),
            name = "",
            baseUrl = url,
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
        val url = authStore.getCurrentSite()
        return client.getERPSingle(
            doctype = ERPDocType.StockSettings.path,
            fields = ERPDocType.StockSettings.getFields(),
            name = "",
            baseUrl = url,
        )
    }

    suspend fun createPaymentEntry(entry: PaymentEntryCreateDto): SubmitResponseDto {
        val url = authStore.getCurrentSite()
        return client.postERP(
            ERPDocType.PaymentEntry.path, payload = entry, baseUrl = url
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
        val url = authStore.getCurrentSite()
        return client.postERP(ERPDocType.Customer.path, payload, baseUrl = url)
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
        val url = authStore.getCurrentSite()
        if (url.isNullOrEmpty()) throw Exception("URL Invalida")

        val userId = store.loadUser()

        if (userId.isNullOrEmpty()) throw Exception("Usuario Invalido")

        return client.getERPSingle(
            ERPDocType.User.path, userId, url
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
        return client.getERPList(
            doctype = ERPDocType.ModeOfPayment.path,
            fields = ERPDocType.ModeOfPayment.getFields(),
            baseUrl = url,
            filters = filters {
                "enabled" eq 1
            })
    }

    suspend fun getModeOfPaymentDetail(name: String): ModeOfPaymentDetailDto? {
        val url = authStore.getCurrentSite() ?: return null
        return client.getERPSingle(
            doctype = ERPDocType.ModeOfPayment.path,
            name = name.encodeURLParameter(),
            baseUrl = url
        )
    }

    suspend fun getAccountDetail(name: String): AccountDetailDto? {
        val url = authStore.getCurrentSite() ?: return null
        return client.getERPSingle(
            doctype = ERPDocType.Account.path, name = name.encodeURLParameter(), baseUrl = url
        )
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
        val url = authStore.getCurrentSite()
        return client.postERP(
            ERPDocType.POSOpeningEntry.path, pos, url
        )
    }

    suspend fun getOpenPOSOpeningEntries(
        user: String,
        posProfile: String
    ): List<POSOpeningEntrySummaryDto> {
        val url = authStore.getCurrentSite()
        return client.getERPList(
            ERPDocType.POSOpeningEntry.path,
            fields = listOf(
                "name",
                "pos_profile",
                "user",
                "status",
                "docstatus",
                "period_start_date"
            ),
            baseUrl = url,
            filters = filters {
                // ASSUMPTION: POS Opening Entry uses status = "Open" for active sessions.
                "pos_profile" eq posProfile
                "user" eq user
                "docstatus" eq 1
                "status" eq "Open"
            }
        )
    }

    suspend fun submitPOSOpeningEntry(name: String): SubmitResponseDto {
        return submitDoc(ERPDocType.POSOpeningEntry.path, name, "submitPOSOpeningEntry")
    }

    suspend fun submitPOSClosingEntry(name: String): SubmitResponseDto {
        return submitDoc(ERPDocType.POSClosingEntry.path, name, "submitPOSClosingEntry")
    }

    suspend fun submitSalesInvoice(name: String): SubmitResponseDto {
        return submitDoc(ERPDocType.SalesInvoice.path, name, "submitSalesInvoice")
    }

    suspend fun submitPOSInvoice(name: String): SubmitResponseDto {
        return submitDoc("POS Invoice", name, "submitPOSInvoice")
    }

    suspend fun submitPaymentEntry(name: String): SubmitResponseDto {
        return submitDoc(ERPDocType.PaymentEntry.path, name, "submitPaymentEntry")
    }

    suspend fun cancelSalesInvoice(name: String): SubmitResponseDto {
        return cancelDoc(ERPDocType.SalesInvoice.path, name, "cancelSalesInvoice")
    }

    suspend fun cancelPOSInvoice(name: String): SubmitResponseDto {
        return cancelDoc("POS Invoice", name, "cancelPOSInvoice")
    }

    suspend fun setValue(
        doctype: String,
        name: String,
        fieldname: String,
        value: String
    ) {
        val url = authStore.getCurrentSite()
        if (url.isNullOrBlank()) throw Exception("URL Invalida")
        val endpoint = url.trimEnd('/') + "/api/method/frappe.client.set_value"
        val response = withRetries {
            client.post {
                url { takeFrom(endpoint) }
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("doctype", doctype)
                            append("name", name)
                            append("fieldname", fieldname)
                            append("value", value)
                        }
                    )
                )
            }
        }
        if (!response.status.isSuccess()) {
            val bodyText = response.bodyAsText()
            throw Exception("Error en setValue: ${response.status} - $bodyText")
        }
    }

    private suspend fun submitDoc(
        doctype: String,
        name: String,
        logContext: String
    ): SubmitResponseDto {
        val url = authStore.getCurrentSite()
        if (url.isNullOrBlank()) throw Exception("URL Invalida")
        val endpoint = url.trimEnd('/') + "/api/method/frappe.client.submit"
        suspend fun fetchSubmitDoc(): JsonObject {
            val doc = client.getERPSingle<JsonObject>(
                doctype = doctype,
                name = name.encodeURLParameter(),
                baseUrl = url,
                fields = emptyList()
            )
            return buildJsonObject {
                put("doctype", doctype)
                doc.forEach { (key, value) -> put(key, value) }
            }
        }
        return try {
            suspend fun submitWithDoc(doc: JsonObject): HttpResponse {
                return withRetries {
                    client.post {
                        url { takeFrom(endpoint) }
                        contentType(ContentType.Application.FormUrlEncoded)
                        setBody(
                            FormDataContent(
                                Parameters.build {
                                    val docJson = json.encodeToString(doc)
                                    append("doc", docJson)
                                }
                            )
                        )
                    }
                }
            }

            var doc = fetchSubmitDoc()
            var response = submitWithDoc(doc)
            var bodyText = response.bodyAsText()
            if (!response.status.isSuccess() && bodyText.contains("TimestampMismatchError")) {
                doc = fetchSubmitDoc()
                response = submitWithDoc(doc)
                bodyText = response.bodyAsText()
            }
            if (!response.status.isSuccess()) {
                try {
                    val err = json.decodeFromString<FrappeErrorResponse>(bodyText)
                    throw FrappeException(
                        err.exception ?: "Error: ${response.status.value}",
                        err
                    )
                } catch (e: Exception) {
                    throw Exception(
                        "Error en $logContext: ${response.status} - $bodyText",
                        e
                    )
                }
            }

            val parsed = json.parseToJsonElement(bodyText).jsonObject
            val messageElement = parsed["message"] ?: parsed["data"]
            if (messageElement == null) {
                throw FrappeException(
                    "La respuesta no contiene 'message'. Respuesta: $bodyText"
                )
            }
            json.decodeFromJsonElement(messageElement)
        } catch (e: Exception) {
            AppSentry.capture(e, "$logContext failed")
            AppLogger.warn("$logContext failed", e)
            throw e
        }
    }

    private suspend fun cancelDoc(
        doctype: String,
        name: String,
        logContext: String
    ): SubmitResponseDto {
        val url = authStore.getCurrentSite()
        if (url.isNullOrBlank()) throw Exception("URL Invalida")
        val endpoint = url.trimEnd('/') + "/api/method/frappe.client.cancel"

        return try {
            val response = withRetries {
                client.post {
                    url { takeFrom(endpoint) }
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(
                        FormDataContent(
                            Parameters.build {
                                append("doctype", doctype)
                                append("name", name)
                            }
                        )
                    )
                }
            }

            val bodyText = response.bodyAsText()
            if (!response.status.isSuccess()) {
                try {
                    val err = json.decodeFromString<FrappeErrorResponse>(bodyText)
                    throw FrappeException(
                        err.exception ?: "Error: ${response.status.value}",
                        err
                    )
                } catch (e: Exception) {
                    throw Exception("Error en $logContext: ${response.status} - $bodyText", e)
                }
            }

            val parsed = json.parseToJsonElement(bodyText).jsonObject
            val messageElement = parsed["message"] ?: parsed["data"]
            if (messageElement == null) {
                throw FrappeException(
                    "La respuesta no contiene 'message'. Respuesta: $bodyText"
                )
            }
            json.decodeFromJsonElement(messageElement)
        } catch (e: Exception) {
            AppSentry.capture(e, "$logContext failed")
            AppLogger.warn("$logContext failed", e)
            throw e
        }
    }

    suspend fun closeCashbox(entry: POSClosingEntryDto): POSClosingEntryResponse {
        val url = authStore.getCurrentSite()
        return client.postERP(
            ERPDocType.POSClosingEntry.path, entry, url
        )
    }

    suspend fun getPOSClosingEntriesForOpening(
        openingEntryName: String
    ): List<POSClosingEntrySummaryDto> {
        val url = authStore.getCurrentSite()
        return client.getERPList(
            ERPDocType.POSClosingEntry.path,
            fields = listOf(
                "name",
                "pos_opening_entry",
                "period_end_date",
                "posting_date",
                "docstatus"
            ),
            baseUrl = url,
            limit = 1,
            orderBy = "period_end_date",
            orderType = "desc",
            filters = filters {
                "pos_opening_entry" eq openingEntryName
            }
        )
    }

    suspend fun getPOSProfileDetails(profileId: String): POSProfileDto {
        val url = authStore.getCurrentSite()
        val fields = ERPDocType.POSProfileDetails.getFields()
        return client.getERPSingle(
            doctype = ERPDocType.POSProfile.path,
            fields = fields,
            name = profileId.encodeURLParameter(),
            baseUrl = url,
        )
    }

    suspend fun getPOSProfiles(assignedTo: String?): List<POSProfileSimpleDto> {
        val url = authStore.getCurrentSite()
        return try {
            client.getERPList(
                doctype = ERPDocType.POSProfile.path,
                fields = ERPDocType.POSProfile.getFields(),
                baseUrl = url,
                filters = filters {
                    "disabled" eq false
                })
        } catch (e: Exception) {
            e.printStackTrace()
            AppSentry.capture(e, "getPOSProfiles failed")
            AppLogger.warn("getPOSProfiles failed", e)
            emptyList()
        }
    }

    //TODO: Cuando tenga el API lo cambiamos
    //TODO: Tenemos que discriminar desde el API la plataforma
    fun getLoginWithSite(site: String): LoginInfo {
        return if (getPlatformName() == "Desktop") {
            LoginInfo(
                BuildKonfig.BASE_URL,
                BuildKonfig.DESKTOP_REDIRECT_URI,
                BuildKonfig.DESKTOP_CLIENT_ID,
                BuildKonfig.DESKTOP_CLIENT_SECRET,
                listOf("all", "openid"),
                "ERP-POS Clothing Center - Desktop"
            )
        } else {
            LoginInfo(
                BuildKonfig.BASE_URL,
                BuildKonfig.REDIRECT_URI,
                BuildKonfig.CLIENT_ID,
                BuildKonfig.CLIENT_SECRET,
                listOf("all", "openid"),
                "ERP-POS Clothing Center"
            )
        }/*return  client.get("") {
             contentType(ContentType.Application.Json)
             setBody(site)
         }.body()*/
    }

    // Para Inventario Total: Fetch batch con extras
    suspend fun getInventoryForWarehouse(
        warehouse: String?,
        priceList: String?,
        offset: Int? = null,
        limit: Int? = null,
    ): List<WarehouseItemDto> {
        val url = authStore.getCurrentSite() ?: throw Exception("URL Invalida")
        val warehouseId = warehouse?.trim().orEmpty()
        require(warehouseId.isNotEmpty()) { "Bodega es requerida para la carga de productos" }

        val chunkSize = 50
        val pageSize = limit ?: 50
        val startOffset = offset ?: 0

        suspend fun fetchBins(batchOffset: Int, batchLimit: Int): List<BinDto> {
            return client.getERPList<BinDto>(
                doctype = ERPDocType.Bin.path,
                fields = listOf(
                    "item_code",
                    "warehouse",
                    "stock_uom",
                    "actual_qty",
                    "reserved_qty",
                    "valuation_rate"
                ),
                limit = batchLimit,
                offset = batchOffset,
                orderBy = "item_code",
                baseUrl = url
            ) {
                "warehouse" eq warehouseId
                "actual_qty" gt 0.0
            }
        }

        suspend fun mapBinsToInventory(bins: List<BinDto>): List<WarehouseItemDto> {
            val itemCodes = bins.map { it.itemCode }.distinct()
            if (itemCodes.isEmpty()) return emptyList()

            // Fetch Items batch con fields extras (chunked to avoid URL length limits)
            val itemFields = listOf(
                "item_code",
                "item_name",
                "item_group",
                "description",
                "brand",
                "image",
                "stock_uom",
                "standard_rate",
                "is_stock_item",
                "is_sales_item",
                "disabled"
            )
            val items = itemCodes
                .chunked(chunkSize)
                .flatMap { codes ->
                    client.getERPList<ItemDto>(
                        doctype = ERPDocType.Item.path,
                        fields = itemFields,
                        limit = codes.size,
                        baseUrl = url
                    ) {
                        "name" `in` codes
                    }
                }

            val itemMap = items.associateBy { it.itemCode }

            // Fetch precios batch (chunked to avoid URL length limits)
            val prices = itemCodes
                .chunked(chunkSize)
                .flatMap { codes ->
                    client.getERPList<ItemPriceDto>(
                        doctype = ERPDocType.ItemPrice.path,
                        fields = ERPDocType.ItemPrice.getFields(),
                        limit = codes.size,
                        baseUrl = url
                    ) {
                        "item_code" `in` codes
                        if (!priceList.isNullOrEmpty())
                            "price_list" eq priceList
                    }
                }

            val priceMap = prices.associate { it.itemCode to it.priceListRate }
            val priceCurrency = prices.associate { it.itemCode to it.currency }

            // Combina todo en WarehouseItemDto
            return bins.mapNotNull { bin ->
                val item = itemMap[bin.itemCode]
                if (item == null) {
                    AppLogger.warn("Inventario: item no encontrado ${bin.itemCode}")
                    return@mapNotNull null
                }
                val price = priceMap[bin.itemCode] ?: item.standardRate
                val currency = priceCurrency[bin.itemCode] ?: ""
                val barcode = ""  // No en JSON; "" default
                val isStocked = item.isStockItem
                val isService =
                    !isStocked || (item.itemGroup == "COMPLEMENTARIOS")  // Infer de group en JSON
                val sellableQty = (bin.actualQty - (bin.reservedQty ?: 0.0)).coerceAtLeast(0.0)

                WarehouseItemDto(
                    itemCode = bin.itemCode,
                    actualQty = sellableQty,
                    price = price,
                    valuationRate = bin.valuationRate,
                    name = item.itemName,
                    itemGroup = item.itemGroup,
                    description = item.description,
                    barcode = barcode,
                    image = item.image ?: "",
                    discount = 0.0,  // No field; default 0
                    isService = isService,
                    isStocked = isStocked,
                    stockUom = item.stockUom,
                    brand = item.brand ?: "",
                    currency = currency
                )
            }
        }

        // Paged mode
        if (limit != null) {
            val bins = fetchBins(startOffset, pageSize)
            return mapBinsToInventory(bins)
        }

        // Full sync mode: page through bins
        val results = mutableListOf<WarehouseItemDto>()
        var currentOffset = startOffset
        while (true) {
            val bins = fetchBins(currentOffset, pageSize)
            if (bins.isEmpty()) break
            results += mapBinsToInventory(bins)
            currentOffset += pageSize
        }
        return results
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
        val url = authStore.getCurrentSite()
        return client.getERPList(
            ERPDocType.Customer.path,
            ERPDocType.Customer.getFields(),
            baseUrl = url,
            offset = 0,
            limit = Int.MAX_VALUE,
            orderBy = "customer_name asc",
            filters = filters {
                "disabled" eq false
                if (!territory.isNullOrEmpty()) "territory" eq territory
            })

        /*val detailFields = listOf("name", "credit_limit", "bypass_credit_limit_check", "company")
        val detailMap = customers.associateWith { customer ->
            runCatching {
                client.getERPSingle<CustomerDto>(
                    doctype = ERPDocType.Customer.path,
                    name = customer.name,
                    baseUrl = url,
                    fields = detailFields
                )
            }.getOrNull()
        }

        return customers.map { customer ->
            detailMap[customer]?.let { detail ->
                customer.copy(
                    creditLimits = detail.creditLimits,
                )
            } ?: customer
        }*/
    }

    //Para monto total pendientes y List (method whitelisted)
    suspend fun getCustomerOutstanding(customer: String, posProfile: String): OutstandingInfo {
        val url = authStore.getCurrentSite()
        val invoices = client.getERPList<SalesInvoiceDto>(
            doctype = ERPDocType.SalesInvoice.path,
            fields = ERPDocType.SalesInvoice.getFields(),
            baseUrl = url,
            filters = filters {
                "customer" eq customer
                "pos_profile" eq posProfile
                "status" `in` listOf(
                    "Unpaid",
                    "Overdue",
                    "Partly Paid",
                    "Overdue and Discounted",
                    "Unpaid and Discounted",
                    "Partly Paid and Discounted"
                )
            })
        val totalOutstanding = invoices.sumOf { invoice ->
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
        val url = authStore.getCurrentSite()
        return client.getERPList(
            ERPDocType.SalesInvoice.path,
            ERPDocType.SalesInvoice.getFields(),
            baseUrl = url,
            orderBy = "posting_date desc",
            filters = filters {
                "customer" eq customer
                "pos_profile" eq posProfile
                "posting_date" gte startDate
                "posting_date" lte endDate
            })
    }

    suspend fun fetchCustomerPosInvoicesForPeriod(
        customer: String,
        startDate: String,
        endDate: String,
        posProfile: String
    ): List<SalesInvoiceDto> {
        val url = authStore.getCurrentSite()
        return client.getERPList(
            doctype = "POS Invoice",
            fields = ERPDocType.SalesInvoice.getFields(),
            baseUrl = url,
            orderBy = "posting_date desc",
            filters = filters {
                "customer" eq customer
                "pos_profile" eq posProfile
                "posting_date" gte startDate
                "posting_date" lte endDate
            })
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
        val url = authStore.getCurrentSite()
        val today = DateTimeProvider.todayDate()
        val startDate = DateTimeProvider.addDays(today, -DEFAULT_INVOICE_SYNC_DAYS)
        return client.getERPList<SalesInvoiceDto>(
            doctype = ERPDocType.SalesInvoice.path,
            fields = ERPDocType.SalesInvoice.getFields(),
            baseUrl = url,
            filters = filters {
                "pos_profile" eq posProfile
                "posting_date" gte startDate
                "status" `in` listOf(
                    "Unpaid",
                    "Overdue",
                    "Partly Paid",
                    "Overdue and Discounted",
                    "Unpaid and Discounted",
                    "Partly Paid and Discounted"
                )
            })
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

    suspend fun fetchAllPosInvoices(
        posProfile: String, offset: Int = 0, limit: Int = Int.MAX_VALUE
    ): List<SalesInvoiceDto> {
        return try {
            val url = authStore.getCurrentSite()
            val today = DateTimeProvider.todayDate()
            val startDate = DateTimeProvider.addDays(today, -DEFAULT_INVOICE_SYNC_DAYS)
            client.getERPList(
                doctype = "POS Invoice",
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
            AppSentry.capture(e, "fetchAllPosInvoices failed")
            AppLogger.warn("fetchAllPosInvoices failed", e)
            emptyList()
        }
    }

    suspend fun fetchAllPosInvoicesSmart(
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
                doctype = "POS Invoice",
                posProfile = posProfile,
                startDate = startDate,
                statuses = openStatuses
            )
            val paidInvoices = fetchInvoicesByStatus(
                doctype = "POS Invoice",
                posProfile = posProfile,
                startDate = paidStartDate,
                statuses = listOf("Paid")
            )
            (openInvoices + paidInvoices).distinctBy { it.name }
        } catch (e: Exception) {
            e.printStackTrace()
            AppSentry.capture(e, "fetchAllPosInvoicesSmart failed")
            AppLogger.warn("fetchAllPosInvoicesSmart failed", e)
            emptyList()
        }
    }

    suspend fun fetchAllInvoicesCombined(
        posProfile: String,
        recentPaidOnly: Boolean = false,
        paidDays: Int = RECENT_PAID_INVOICE_DAYS
    ): List<SalesInvoiceDto> {
        val sales = if (recentPaidOnly) fetchAllInvoicesSmart(posProfile, paidDays)
        else fetchAllInvoices(posProfile)
        val pos = if (recentPaidOnly) fetchAllPosInvoicesSmart(posProfile, paidDays)
        else fetchAllPosInvoices(posProfile)
        return (sales + pos).distinctBy { it.name }
    }

    suspend fun fetchPaymentEntries(fromDate: String): List<PaymentEntryDto> {
        val url = authStore.getCurrentSite()
        return client.getERPList(
            doctype = ERPDocType.PaymentEntry.path,
            fields = listOf(
                "name",
                "posting_date",
                "party",
                "party_type",
                "payment_type",
                "mode_of_payment",
                "paid_amount",
                "received_amount",
                "paid_from_account_currency",
                "paid_to_account_currency"
            ),
            orderBy = "posting_date desc",
            baseUrl = url,
            filters = filters {
                "posting_date" gte fromDate
                "docstatus" eq 1
                "party_type" eq "Customer"
                "payment_type" eq "Receive"
            }
        )
    }

    suspend fun getPaymentEntryByName(name: String): PaymentEntryDto {
        val url = authStore.getCurrentSite()
        return client.getERPSingle(
            doctype = ERPDocType.PaymentEntry.path,
            name = name,
            baseUrl = url
        )
    }

    suspend fun fetchReturnInvoiceNames(
        returnAgainst: String,
        posProfile: String,
        isPos: Boolean
    ): List<String> {
        return try {
            val url = authStore.getCurrentSite()
            val doctype = if (isPos) "POS Invoice" else ERPDocType.SalesInvoice.path
            client.getERPList<SalesInvoiceDto>(
                doctype = doctype,
                fields = listOf("name"),
                baseUrl = url,
                filters = filters {
                    "return_against" eq returnAgainst
                    "is_return" eq 1
                    "pos_profile" eq posProfile
                }
            ).mapNotNull { it.name }
        } catch (e: Exception) {
            e.printStackTrace()
            AppSentry.capture(e, "fetchReturnInvoiceNames failed")
            AppLogger.warn("fetchReturnInvoiceNames failed", e)
            emptyList()
        }
    }

    //region Invoice - Checkout
    suspend fun createSalesInvoice(data: SalesInvoiceDto): SalesInvoiceDto {
        val url = authStore.getCurrentSite()
        val result: SalesInvoiceDto = client.postERP(
            doctype = ERPDocType.SalesInvoice.path,
            payload = data,
            baseUrl = url,
        )

        return result
    }

    suspend fun createPOSInvoice(data: SalesInvoiceDto): SalesInvoiceDto {
        val url = authStore.getCurrentSite()
        val result: SalesInvoiceDto = client.postERP(
            doctype = "POS Invoice",
            payload = data,
            baseUrl = url,
        )
        return result
    }

    suspend fun getSalesInvoiceByName(name: String): SalesInvoiceDto {
        val url = authStore.getCurrentSite()
        return client.getERPSingle(
            doctype = ERPDocType.SalesInvoice.path,
            name = name,
            baseUrl = url,
        )
    }

    suspend fun getPOSInvoiceByName(name: String): SalesInvoiceDto {
        val url = authStore.getCurrentSite()
        return client.getERPSingle(
            doctype = "POS Invoice",
            name = name,
            baseUrl = url,
        )
    }

    suspend fun updateSalesInvoice(name: String, data: SalesInvoiceDto): SalesInvoiceDto {
        val url = authStore.getCurrentSite()
        return client.putERP(
            doctype = ERPDocType.SalesInvoice.path, name = name, payload = data, baseUrl = url
        )
    }

    suspend fun updatePOSInvoice(name: String, data: SalesInvoiceDto): SalesInvoiceDto {
        val url = authStore.getCurrentSite()
        return client.putERP(
            doctype = "POS Invoice", name = name, payload = data, baseUrl = url
        )
    }
    //endregion
}

expect fun defaultEngine(): HttpClientEngine

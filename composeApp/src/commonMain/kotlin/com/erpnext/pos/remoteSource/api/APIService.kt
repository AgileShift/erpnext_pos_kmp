package com.erpnext.pos.remoteSource.api

import com.erpnext.pos.BuildKonfig
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.remoteSource.dto.BinDto
import com.erpnext.pos.remoteSource.dto.CategoryDto
import com.erpnext.pos.remoteSource.dto.CurrencyDto
import com.erpnext.pos.remoteSource.dto.ContactChildDto
import com.erpnext.pos.remoteSource.dto.CustomerDto
import com.erpnext.pos.remoteSource.dto.DeliveryChargeDto
import com.erpnext.pos.remoteSource.dto.ExchangeRateResponse
import com.erpnext.pos.remoteSource.dto.ItemDetailDto
import com.erpnext.pos.remoteSource.dto.ItemDto
import com.erpnext.pos.remoteSource.dto.ItemPriceDto
import com.erpnext.pos.remoteSource.dto.LoginInfo
import com.erpnext.pos.remoteSource.dto.AccountDetailDto
import com.erpnext.pos.remoteSource.dto.ModeOfPaymentDetailDto
import com.erpnext.pos.remoteSource.dto.ModeOfPaymentDto
import com.erpnext.pos.remoteSource.dto.OutstandingInfo
import com.erpnext.pos.remoteSource.dto.POSClosingEntryDto
import com.erpnext.pos.remoteSource.dto.POSClosingEntryResponse
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryResponseDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntrySummaryDto
import com.erpnext.pos.remoteSource.dto.POSProfileDto
import com.erpnext.pos.remoteSource.dto.POSProfileSimpleDto
import com.erpnext.pos.remoteSource.dto.PaymentTermDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.SubmitResponseDto
import com.erpnext.pos.remoteSource.dto.TokenResponse
import com.erpnext.pos.remoteSource.dto.UserDto
import com.erpnext.pos.remoteSource.dto.WarehouseItemDto
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
import com.erpnext.pos.remoteSource.sdk.json
import com.erpnext.pos.remoteSource.sdk.postERP
import com.erpnext.pos.remoteSource.sdk.putERP
import com.erpnext.pos.remoteSource.sdk.withRetries
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.AppSentry
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.invoke
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.takeFrom
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.put

class APIService(
    private val client: HttpClient,
    private val store: TokenStore,
    private val authStore: AuthInfoStore,
    private val tokenClient: HttpClient
) {
    suspend fun getCompanyInfo(): List<CompanyDto> {
        val url = authStore.getCurrentSite()
        return client.getERPSingle(
            doctype = ERPDocType.Company.path,
            fields = ERPDocType.Company.getFields(),
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

    suspend fun exchangeCode(
        oauthConfig: OAuthConfig,
        code: String,
        pkce: Pkce,
        expectedState: String,
        returnedState: String
    ): TokenResponse? {
        try {
            require(expectedState == returnedState) { "CSRF state mismatch" }
            val res = client.post(oauthConfig.tokenUrl) {
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

    suspend fun submitPaymentEntry(name: String): SubmitResponseDto {
        return submitDoc(ERPDocType.PaymentEntry.path, name, "submitPaymentEntry")
    }

    suspend fun cancelSalesInvoice(name: String): SubmitResponseDto {
        return cancelDoc(ERPDocType.SalesInvoice.path, name, "cancelSalesInvoice")
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
    suspend fun getLoginWithSite(site: String): LoginInfo {
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
        require(!warehouse.isNullOrEmpty()) { "Bodega es requerida para la carga de productos" }

        val chunkSize = 50
        val pageSize = limit ?: 50
        val startOffset = offset ?: 0

        suspend fun fetchBins(batchOffset: Int, batchLimit: Int): List<BinDto> {
            return client.getERPList<BinDto>(
                doctype = ERPDocType.Bin.path,
                fields = ERPDocType.Bin.getFields(),
                limit = batchLimit,
                offset = batchOffset,
                orderBy = "item_code",
                baseUrl = url
            ) {
                if (warehouse.isNotEmpty())
                    "warehouse" eq warehouse
                "actual_qty" gt 0.0
                "valuation_rate" gt 0.0
            }
        }

        suspend fun mapBinsToInventory(bins: List<BinDto>): List<WarehouseItemDto> {
            val itemCodes = bins.map { it.itemCode }.distinct()
            if (itemCodes.isEmpty()) return emptyList()

            // Fetch Items batch con fields extras (chunked to avoid URL length limits)
            val items = itemCodes
                .chunked(chunkSize)
                .flatMap { codes ->
                    client.getERPList<ItemDto>(
                        doctype = ERPDocType.Item.path,
                        fields = ERPDocType.Item.getFields(),
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
            return bins.map { bin ->
                val item = itemMap[bin.itemCode]
                    ?: throw FrappeException("Item no encontrado: ${bin.itemCode}")
                val price = priceMap[bin.itemCode] ?: item.standardRate
                val currency = priceCurrency[bin.itemCode] ?: ""
                val barcode = ""  // No en JSON; "" default
                val isStocked = item.isStockItem
                val isService =
                    !isStocked || (item.itemGroup == "COMPLEMENTARIOS")  // Infer de group en JSON

                WarehouseItemDto(
                    itemCode = bin.itemCode,
                    actualQty = bin.actualQty,
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

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun getItemStockAndPrice(
        itemCode: String, warehouse: String, priceList: String = "Standard Selling"
    ): ItemDetailDto {
        val url = authStore.getCurrentSite() ?: throw Exception("URL Invalida")
        val endpoint = "$url/api/method/erpnext.stock.get_item_details"

        val response = client.post(endpoint) {
            contentType(ContentType.Application.Json)
            setBody(
                mapOf(
                    "args" to mapOf(
                        "item_code" to itemCode,
                        "warehouse" to warehouse,
                        "price_list" to priceList,
                        "qty" to 1,
                        "transaction_type" to "selling",
                        "doctype" to "POS Invoice",
                        "set_basic_rate" to 1,
                        "ignore_pricing_rule" to 0
                    )
                )
            )
        }

        val bodyText = response.bodyAsText()
        if (!response.status.isSuccess()) {
            try {
                val err = json.decodeFromString<FrappeErrorResponse>(bodyText)
                throw FrappeException(err.exception ?: "Error: ${response.status}", err)
            } catch (e: Exception) {
                throw Exception("Error en get_item_details: ${response.status} - $bodyText", e)
            }
        }

        val parsed = json.parseToJsonElement(bodyText).jsonObject
        val messageElement =
            parsed["message"] ?: throw FrappeException("No 'message' en respuesta: $bodyText")

        val details = json.decodeFromJsonElement<ItemDetailDto>(messageElement)

        // Procesamiento post-fetch: Ajusta fields según reglas de negocios
        val processedBarcode = ""  // No en response; default ""
        val processedIsStocked = details.isStocked  // De is_stock_item si en response
        val processedIsService = !processedIsStocked || (details.itemGroup == "COMPLEMENTARIOS")

        return details.copy(
            itemCode = details.itemCode ?: itemCode,
            price = details.price,
            name = details.name,
            barcode = processedBarcode,
            discount = 0.0,
            isStocked = processedIsStocked,
            isService = processedIsService
        )
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
    suspend fun getCustomerOutstanding(customer: String): OutstandingInfo {
        val url = authStore.getCurrentSite()
        val invoices = client.getERPList<SalesInvoiceDto>(
            doctype = ERPDocType.SalesInvoice.path,
            fields = ERPDocType.SalesInvoice.getFields(),
            baseUrl = url,
            filters = filters {
                "customer" eq customer
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
            invoice.outstandingAmount ?: (invoice.grandTotal - invoice.paidAmount)
        }
        return OutstandingInfo(totalOutstanding, invoices)
    }

    suspend fun fetchCustomerInvoicesForPeriod(
        customer: String,
        startDate: String,
        endDate: String
    ): List<SalesInvoiceDto> {
        val url = authStore.getCurrentSite()
        return client.getERPList(
            ERPDocType.SalesInvoice.path,
            ERPDocType.SalesInvoice.getFields(),
            baseUrl = url,
            orderBy = "posting_date desc",
            filters = filters {
                "customer" eq customer
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
    suspend fun getAllOutstandingInvoices(): List<SalesInvoiceDto> {
        val url = authStore.getCurrentSite()
        return client.getERPList<SalesInvoiceDto>(
            doctype = ERPDocType.SalesInvoice.path,
            fields = ERPDocType.SalesInvoice.getFields(),
            baseUrl = url,
            filters = filters {
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
            client.getERPList(
                doctype = ERPDocType.SalesInvoice.path,
                fields = ERPDocType.SalesInvoice.getFields(),
                offset = offset,
                limit = limit,
                baseUrl = url,
                filters = filters {
                    "pos_profile" eq posProfile
                    "status" `in` listOf(
                        "Unpaid",
                        "Overdue",
                        "Partly Paid",
                        "Overdue and Discounted",
                        "Unpaid and Discounted",
                        "Partly Paid and Discounted"
                    )
                    "outstanding_amount" gt 0.0
                })
        } catch (e: Exception) {
            e.printStackTrace()
            AppSentry.capture(e, "fetchAllInvoices failed")
            AppLogger.warn("fetchAllInvoices failed", e)
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

    suspend fun getSalesInvoiceByName(name: String): SalesInvoiceDto {
        val url = authStore.getCurrentSite()
        return client.getERPSingle(
            doctype = ERPDocType.SalesInvoice.path,
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
    //endregion
}

expect fun defaultEngine(): HttpClientEngine

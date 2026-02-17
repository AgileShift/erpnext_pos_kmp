package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.configuration.ConfigurationStore
import com.erpnext.pos.localSource.dao.CompanyDao
import com.erpnext.pos.localSource.datasources.CustomerGroupLocalSource
import com.erpnext.pos.localSource.datasources.CustomerLocalSource
import com.erpnext.pos.localSource.datasources.CustomerOutboxLocalSource
import com.erpnext.pos.localSource.datasources.DeliveryChargeLocalSource
import com.erpnext.pos.localSource.datasources.ExchangeRateLocalSource
import com.erpnext.pos.localSource.datasources.InventoryLocalSource
import com.erpnext.pos.localSource.datasources.InvoiceLocalSource
import com.erpnext.pos.localSource.datasources.PaymentTermLocalSource
import com.erpnext.pos.localSource.datasources.TerritoryLocalSource
import com.erpnext.pos.localSource.entities.CompanyEntity
import com.erpnext.pos.localSource.entities.ExchangeRateEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.BootstrapFullSnapshotDto
import com.erpnext.pos.remoteSource.dto.BootstrapPosSyncDto
import com.erpnext.pos.remoteSource.dto.BootstrapRequestDto
import com.erpnext.pos.remoteSource.mapper.resolveReceivableAccount
import com.erpnext.pos.remoteSource.mapper.toEntities
import com.erpnext.pos.remoteSource.mapper.toEntity
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class BootstrapSyncRepository(
    private val api: APIService,
    private val configurationStore: ConfigurationStore,
    private val posProfilePaymentMethodSyncRepository: PosProfilePaymentMethodSyncRepository,
    private val companyDao: CompanyDao,
    private val stockSettingsRepository: StockSettingsRepository,
    private val exchangeRateLocalSource: ExchangeRateLocalSource,
    private val paymentTermLocalSource: PaymentTermLocalSource,
    private val deliveryChargeLocalSource: DeliveryChargeLocalSource,
    private val customerGroupLocalSource: CustomerGroupLocalSource,
    private val territoryLocalSource: TerritoryLocalSource,
    private val inventoryLocalSource: InventoryLocalSource,
    private val customerLocalSource: CustomerLocalSource,
    private val customerOutboxLocalSource: CustomerOutboxLocalSource,
    private val invoiceLocalSource: InvoiceLocalSource
) {
    companion object {
        private const val KEY_BOOTSTRAP_RAW = "bootstrap.raw.payload"
        private const val KEY_BOOTSTRAP_PAYMENT_ENTRIES = "bootstrap.raw.payment_entries"
        private const val KEY_BOOTSTRAP_INVENTORY_ALERTS = "bootstrap.raw.inventory_alerts"
        private const val KEY_BOOTSTRAP_ACTIVITY_EVENTS = "bootstrap.raw.activity_events"
        private const val KEY_BOOTSTRAP_META = "bootstrap.debug.meta"
        private const val KEY_BOOTSTRAP_COUNTS = "bootstrap.debug.counts"
        private const val DEFAULT_PAGE_LIMIT = 5_000
        private const val MAX_PAGE_FETCH = 200
    }

    private data class PaginationMeta(
        val offset: Int,
        val limit: Int,
        val total: Int,
        val hasMore: Boolean
    )

    private data class PagedFetchResult<T>(
        val items: List<T>,
        val debug: JsonObject
    )

    enum class Section(
        val label: String,
        val message: String
    ) {
        CACHE_RAW(
            label = "Informacion total",
            message = "Guardando snapshot completo..."
        ),
        POS_PROFILES(
            label = "POS Profiles",
            message = "Guardando perfiles POS y metodos de pago..."
        ),
        COMPANY(
            label = "Compania",
            message = "Guardando compania..."
        ),
        STOCK_SETTINGS(
            label = "Configuracion de Stock",
            message = "Guardando configuracion de inventario..."
        ),
        EXCHANGE_RATES(
            label = "Tasas de cambio",
            message = "Guardando tasas de cambio..."
        ),
        PAYMENT_TERMS(
            label = "Terminos de pago",
            message = "Guardando terminos de pago..."
        ),
        DELIVERY_CHARGES(
            label = "Cargo por envio",
            message = "Guardando cargos de entrega..."
        ),
        CUSTOMER_GROUPS(
            label = "Grupos de clientes",
            message = "Guardando grupos de clientes..."
        ),
        TERRITORIES(
            label = "Territorios",
            message = "Guardando territorios..."
        ),
        CATEGORIES(
            label = "Categorias",
            message = "Guardando categorias..."
        ),
        INVENTORY_ITEMS(
            label = "Inventario",
            message = "Guardando inventario..."
        ),
        CUSTOMERS(
            label = "Clientes",
            message = "Guardando clientes..."
        ),
        INVOICES(
            label = "Facturas",
            message = "Guardando facturas..."
        ),
        PAYMENT_ENTRIES(
            label = "Entradas de pago",
            message = "Guardando snapshot de entradas de pago..."
        ),
        INVENTORY_ALERTS(
            label = "Alertas de Inventario",
            message = "Guardando snapshot de alertas..."
        ),
        ACTIVITY_EVENTS(
            label = "Eventos",
            message = "Guardando snapshot de eventos..."
        )
    }

    data class Snapshot(
        val raw: JsonObject,
        val data: BootstrapFullSnapshotDto
    )

    fun orderedSections(): List<Section> = Section.entries

    suspend fun fetchSnapshot(profileName: String? = null): Snapshot {
        val baseRaw = api.getBootstrapRawSnapshot(
            BootstrapRequestDto(
                includeInventory = false,
                includeCustomers = false,
                includeInvoices = false,
                includeAlerts = false,
                includeActivity = false,
                recentPaidOnly = true,
                profileName = profileName,
                offset = 0,
                limit = DEFAULT_PAGE_LIMIT
            )
        )
        val baseData = api.decodeBootstrapFullSnapshot(baseRaw)

        val selectedProfile = profileName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { requested ->
                baseData.posProfiles.firstOrNull {
                    it.profileName.equals(
                        requested,
                        ignoreCase = true
                    )
                }
            }
            ?: baseData.posProfiles.firstOrNull()

        val warehouse = baseData.context?.warehouse
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: selectedProfile?.warehouse?.trim()?.takeIf { it.isNotBlank() }
        val priceList = baseData.context?.priceListCamel
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: baseData.context?.priceListSnake?.trim()?.takeIf { it.isNotBlank() }
            ?: selectedProfile?.sellingPriceList?.trim()?.takeIf { it.isNotBlank() }

        val inventoryRaw = if (warehouse.isNullOrBlank()) {
            JsonObject(emptyMap())
        } else {
            api.getBootstrapRawSnapshot(
                BootstrapRequestDto(
                    includeInventory = true,
                    includeCustomers = false,
                    includeInvoices = false,
                    includeAlerts = false,
                    includeActivity = false,
                    recentPaidOnly = true,
                    profileName = profileName,
                    warehouse = warehouse,
                    priceList = priceList,
                    offset = 0,
                    limit = DEFAULT_PAGE_LIMIT
                )
            )
        }
        val inventoryFirst = if (warehouse.isNullOrBlank()) {
            emptyList()
        } else {
            api.decodeBootstrapFullSnapshot(inventoryRaw).inventoryItems
        }
        val inventoryFetch = if (warehouse.isNullOrBlank()) {
            PagedFetchResult(
                items = emptyList(),
                debug = buildJsonObject {
                    put("enabled", JsonPrimitive(false))
                    put("reason", JsonPrimitive("missing_warehouse"))
                }
            )
        } else {
            fetchPaged(
                firstRaw = inventoryRaw,
                firstItems = inventoryFirst,
                sectionKey = "inventory",
                dedupeKey = { it.itemCode }
            ) { nextOffset, pageLimit ->
                val pageRaw = api.getBootstrapRawSnapshot(
                    BootstrapRequestDto(
                        includeInventory = true,
                        includeCustomers = false,
                        includeInvoices = false,
                        includeAlerts = false,
                        includeActivity = false,
                        recentPaidOnly = true,
                        profileName = profileName,
                        warehouse = warehouse,
                        priceList = priceList,
                        offset = nextOffset,
                        limit = pageLimit
                    )
                )
                api.decodeBootstrapFullSnapshot(pageRaw).inventoryItems
            }
        }

        val customersRaw = api.getBootstrapRawSnapshot(
            BootstrapRequestDto(
                includeInventory = false,
                includeCustomers = true,
                includeInvoices = false,
                includeAlerts = false,
                includeActivity = false,
                recentPaidOnly = true,
                profileName = profileName,
                offset = 0,
                limit = DEFAULT_PAGE_LIMIT
            )
        )
        val customersFirst = api.decodeBootstrapFullSnapshot(customersRaw).customers
        val customersFetch = fetchPaged(
            firstRaw = customersRaw,
            firstItems = customersFirst,
            sectionKey = "customers",
            dedupeKey = { it.name }
        ) { nextOffset, pageLimit ->
            val pageRaw = api.getBootstrapRawSnapshot(
                BootstrapRequestDto(
                    includeInventory = false,
                    includeCustomers = true,
                    includeInvoices = false,
                    includeAlerts = false,
                    includeActivity = false,
                    recentPaidOnly = true,
                    profileName = profileName,
                    offset = nextOffset,
                    limit = pageLimit
                )
            )
            api.decodeBootstrapFullSnapshot(pageRaw).customers
        }

        val invoicesRaw = api.getBootstrapRawSnapshot(
            BootstrapRequestDto(
                includeInventory = false,
                includeCustomers = false,
                includeInvoices = true,
                includeAlerts = false,
                includeActivity = false,
                recentPaidOnly = true,
                profileName = profileName,
                offset = 0,
                limit = DEFAULT_PAGE_LIMIT
            )
        )
        val invoicesFirstData = api.decodeBootstrapFullSnapshot(invoicesRaw)
        val invoicesFetch = fetchPaged(
            firstRaw = invoicesRaw,
            firstItems = invoicesFirstData.invoices,
            sectionKey = "invoices",
            dedupeKey = { it.name ?: "${it.customer}-${it.postingDate}-${it.grandTotal}" }
        ) { nextOffset, pageLimit ->
            val pageRaw = api.getBootstrapRawSnapshot(
                BootstrapRequestDto(
                    includeInventory = false,
                    includeCustomers = false,
                    includeInvoices = true,
                    includeAlerts = false,
                    includeActivity = false,
                    recentPaidOnly = true,
                    profileName = profileName,
                    offset = nextOffset,
                    limit = pageLimit
                )
            )
            api.decodeBootstrapFullSnapshot(pageRaw).invoices
        }
        val paymentEntriesFetch = fetchPaged(
            firstRaw = invoicesRaw,
            firstItems = invoicesFirstData.paymentEntries,
            sectionKey = "payment_entries",
            dedupeKey = { it.name ?: "${it.party}-${it.postingDate}-${it.paidAmount}" }
        ) { nextOffset, pageLimit ->
            val pageRaw = api.getBootstrapRawSnapshot(
                BootstrapRequestDto(
                    includeInventory = false,
                    includeCustomers = false,
                    includeInvoices = true,
                    includeAlerts = false,
                    includeActivity = false,
                    recentPaidOnly = true,
                    profileName = profileName,
                    offset = nextOffset,
                    limit = pageLimit
                )
            )
            api.decodeBootstrapFullSnapshot(pageRaw).paymentEntries
        }

        val alertsRaw = api.getBootstrapRawSnapshot(
            BootstrapRequestDto(
                includeInventory = false,
                includeCustomers = false,
                includeInvoices = false,
                includeAlerts = true,
                includeActivity = false,
                recentPaidOnly = true,
                profileName = profileName,
                offset = 0,
                limit = DEFAULT_PAGE_LIMIT
            )
        )
        val allAlerts = api.decodeBootstrapFullSnapshot(alertsRaw).inventoryAlerts

        val activityRaw = api.getBootstrapRawSnapshot(
            BootstrapRequestDto(
                includeInventory = false,
                includeCustomers = false,
                includeInvoices = false,
                includeAlerts = false,
                includeActivity = true,
                recentPaidOnly = true,
                profileName = profileName,
                offset = 0,
                limit = DEFAULT_PAGE_LIMIT
            )
        )
        val activityFirst = api.decodeBootstrapFullSnapshot(activityRaw).activityEvents
        val activityFetch = fetchPaged(
            firstRaw = activityRaw,
            firstItems = activityFirst,
            sectionKey = "activity",
            dedupeKey = { it.toString() }
        ) { nextOffset, pageLimit ->
            val pageRaw = api.getBootstrapRawSnapshot(
                BootstrapRequestDto(
                    includeInventory = false,
                    includeCustomers = false,
                    includeInvoices = false,
                    includeAlerts = false,
                    includeActivity = true,
                    recentPaidOnly = true,
                    profileName = profileName,
                    offset = nextOffset,
                    limit = pageLimit
                )
            )
            api.decodeBootstrapFullSnapshot(pageRaw).activityEvents
        }

        val allInventory = inventoryFetch.items
        val allCustomers = customersFetch.items
        val allInvoices = invoicesFetch.items
        val allPaymentEntries = paymentEntriesFetch.items
        val allActivity = activityFetch.items

        val mergedData = baseData.copy(
            inventoryItems = allInventory,
            customers = allCustomers,
            invoices = allInvoices,
            paymentEntries = allPaymentEntries,
            inventoryAlerts = allAlerts,
            activityEvents = allActivity
        )
        val normalizedRaw = baseRaw.toMutableMap().apply {
            put(
                "inventory",
                buildSectionObject(
                    firstRaw = inventoryRaw,
                    sectionKey = "inventory",
                    items = api.json.encodeToJsonElement(mergedData.inventoryItems)
                )
            )
            put(
                "customers",
                buildSectionObject(
                    firstRaw = customersRaw,
                    sectionKey = "customers",
                    items = api.json.encodeToJsonElement(mergedData.customers)
                )
            )
            put(
                "invoices",
                buildSectionObject(
                    firstRaw = invoicesRaw,
                    sectionKey = "invoices",
                    items = api.json.encodeToJsonElement(mergedData.invoices)
                )
            )
            put(
                "payment_entries",
                buildSectionObject(
                    firstRaw = invoicesRaw,
                    sectionKey = "payment_entries",
                    items = api.json.encodeToJsonElement(mergedData.paymentEntries)
                )
            )
            put("inventory_alerts", api.json.encodeToJsonElement(mergedData.inventoryAlerts))
            put(
                "activity",
                buildSectionObject(
                    firstRaw = activityRaw,
                    sectionKey = "activity",
                    items = api.json.encodeToJsonElement(mergedData.activityEvents)
                )
            )
        }

        persistBootstrapMeta(
            metas = mapOf(
                "inventory" to parsePaginationMeta(inventoryRaw, "inventory"),
                "customers" to parsePaginationMeta(customersRaw, "customers"),
                "invoices" to parsePaginationMeta(invoicesRaw, "invoices"),
                "payment_entries" to parsePaginationMeta(invoicesRaw, "payment_entries"),
                "activity" to parsePaginationMeta(activityRaw, "activity")
            ),
            fetchedCounts = mapOf(
                "inventory" to mergedData.inventoryItems.size,
                "customers" to mergedData.customers.size,
                "invoices" to mergedData.invoices.size,
                "payment_entries" to mergedData.paymentEntries.size,
                "inventory_alerts" to mergedData.inventoryAlerts.size,
                "activity" to mergedData.activityEvents.size
            ),
            pagingDebug = mapOf(
                "inventory" to inventoryFetch.debug,
                "customers" to customersFetch.debug,
                "invoices" to invoicesFetch.debug,
                "payment_entries" to paymentEntriesFetch.debug,
                "activity" to activityFetch.debug
            )
        )

        return Snapshot(raw = JsonObject(normalizedRaw), data = mergedData)
    }

    private fun buildSectionObject(
        firstRaw: JsonObject,
        sectionKey: String,
        items: kotlinx.serialization.json.JsonElement
    ): JsonObject {
        val baseLimit = parsePaginationMeta(
            raw = firstRaw,
            sectionKey = sectionKey
        )?.limit ?: DEFAULT_PAGE_LIMIT
        val total = (items as? JsonArray)?.size ?: 0
        return buildJsonObject {
            put("items", items)
            put(
                "pagination",
                buildJsonObject {
                    put("offset", JsonPrimitive(0))
                    put("limit", JsonPrimitive(baseLimit))
                    put("total", JsonPrimitive(total))
                    put("has_more", JsonPrimitive(0))
                }
            )
        }
    }

    private fun parsePaginationMeta(raw: JsonObject, sectionKey: String): PaginationMeta? {
        val section = raw[sectionKey] as? JsonObject ?: return null
        val pagination = section["pagination"]?.jsonObject ?: return null
        val offset = pagination["offset"]?.jsonPrimitive?.intOrNull ?: 0
        val limit = pagination["limit"]?.jsonPrimitive?.intOrNull ?: DEFAULT_PAGE_LIMIT
        val total = pagination["total"]?.jsonPrimitive?.intOrNull ?: 0
        val hasMoreRaw = pagination["has_more"]?.jsonPrimitive
        val hasMore = hasMoreRaw?.booleanOrNull
            ?: (hasMoreRaw?.intOrNull?.let { it != 0 })
            ?: (hasMoreRaw?.contentOrNull?.lowercase() in setOf("1", "true", "yes"))
        return PaginationMeta(
            offset = offset.coerceAtLeast(0),
            limit = limit.coerceAtLeast(1),
            total = total.coerceAtLeast(0),
            hasMore = hasMore
        )
    }

    private suspend fun persistBootstrapMeta(
        metas: Map<String, PaginationMeta?>,
        fetchedCounts: Map<String, Int>,
        pagingDebug: Map<String, JsonObject> = emptyMap()
    ) {
        val metaPayload = buildJsonObject {
            metas.forEach { (section, meta) ->
                val fetched = fetchedCounts[section] ?: 0
                val sectionDebug = pagingDebug[section]
                put(
                    section,
                    buildJsonObject {
                        put("pagination", paginationToJson(meta))
                        put("fetched", JsonPrimitive(fetched))
                        if (sectionDebug != null) {
                            put("paging", sectionDebug)
                        }
                    }
                )
            }
        }
        configurationStore.saveRaw(KEY_BOOTSTRAP_META, metaPayload.toString())
    }

    private fun paginationToJson(meta: PaginationMeta?) = if (meta == null) {
        JsonNull
    } else {
        buildJsonObject {
            put("offset", JsonPrimitive(meta.offset))
            put("limit", JsonPrimitive(meta.limit))
            put("total", JsonPrimitive(meta.total))
            put("has_more", JsonPrimitive(meta.hasMore))
        }
    }

    private suspend fun <T> fetchPaged(
        firstRaw: JsonObject,
        firstItems: List<T>,
        sectionKey: String,
        dedupeKey: (T) -> String,
        fetchPage: suspend (offset: Int, limit: Int) -> List<T>
    ): PagedFetchResult<T> {
        val baseSeen = LinkedHashSet<String>()
        val baseItems = mutableListOf<T>()
        firstItems.forEach { item ->
            if (baseSeen.add(dedupeKey(item))) {
                baseItems.add(item)
            }
        }

        val meta = parsePaginationMeta(firstRaw, sectionKey)
        if (meta == null) {
            return PagedFetchResult(
                items = baseItems,
                debug = buildJsonObject {
                    put("enabled", JsonPrimitive(false))
                    put("reason", JsonPrimitive("missing_pagination"))
                    put("first_count", JsonPrimitive(firstItems.size))
                    put("first_unique", JsonPrimitive(baseItems.size))
                }
            )
        }
        if (!meta.hasMore || baseItems.size >= meta.total) {
            return PagedFetchResult(
                items = baseItems,
                debug = buildJsonObject {
                    put("enabled", JsonPrimitive(true))
                    put("reason", JsonPrimitive("single_page"))
                    put("pagination", paginationToJson(meta))
                    put("first_count", JsonPrimitive(firstItems.size))
                    put("first_unique", JsonPrimitive(baseItems.size))
                    put("fetched_unique", JsonPrimitive(baseItems.size))
                }
            )
        }

        data class StrategyOutcome(
            val name: String,
            val items: List<T>,
            val pagesFetched: Int,
            val duplicatePages: Int,
            val offsets: List<Int>,
            val terminatedBy: String
        )

        suspend fun runStrategy(
            name: String,
            offsetForPage: (pageIndex: Int) -> Int
        ): StrategyOutcome {
            val seen = LinkedHashSet(baseSeen)
            val merged = baseItems.toMutableList()
            val offsets = mutableListOf<Int>()
            var pageIndex = 1
            var pagesFetched = 0
            var duplicatePages = 0
            var terminatedBy = "max_page_fetch"

            while (pagesFetched < MAX_PAGE_FETCH && merged.size < meta.total) {
                val offset = offsetForPage(pageIndex).coerceAtLeast(0)
                if (offsets.contains(offset)) {
                    terminatedBy = "repeated_offset"
                    break
                }
                offsets += offset

                val page = fetchPage(offset, meta.limit)
                pagesFetched += 1
                if (page.isEmpty()) {
                    terminatedBy = "empty_page"
                    break
                }

                var added = 0
                page.forEach { item ->
                    if (seen.add(dedupeKey(item))) {
                        merged.add(item)
                        added += 1
                    }
                }

                if (added == 0) {
                    duplicatePages += 1
                } else {
                    duplicatePages = 0
                }

                if (merged.size >= meta.total) {
                    terminatedBy = "reached_total"
                    break
                }
                if (page.size < meta.limit) {
                    terminatedBy = "short_page"
                    break
                }
                if (duplicatePages >= 2) {
                    terminatedBy = "stalled_duplicates"
                    break
                }
                pageIndex += 1
            }

            return StrategyOutcome(
                name = name,
                items = merged,
                pagesFetched = pagesFetched,
                duplicatePages = duplicatePages,
                offsets = offsets,
                terminatedBy = terminatedBy
            )
        }

        fun outcomeToJson(outcome: StrategyOutcome): JsonObject {
            val sampledOffsets = outcome.offsets.take(12).map { JsonPrimitive(it) }
            return buildJsonObject {
                put("name", JsonPrimitive(outcome.name))
                put("fetched_unique", JsonPrimitive(outcome.items.size))
                put("pages_fetched", JsonPrimitive(outcome.pagesFetched))
                put("duplicate_pages", JsonPrimitive(outcome.duplicatePages))
                put("terminated_by", JsonPrimitive(outcome.terminatedBy))
                put("offsets_sample", JsonArray(sampledOffsets))
            }
        }

        val absolute = runStrategy(name = "absolute_offset") { pageIndex ->
            meta.offset + (meta.limit * pageIndex)
        }

        val shouldTryPageIndex =
            absolute.items.size <= baseItems.size && meta.total > baseItems.size
        val pageIndex = if (shouldTryPageIndex) {
            runStrategy(name = "page_index_offset") { pageIndexNumber ->
                meta.offset + pageIndexNumber
            }
        } else {
            null
        }

        val selected = when {
            pageIndex != null && pageIndex.items.size > absolute.items.size -> pageIndex
            else -> absolute
        }

        val debug = buildJsonObject {
            put("enabled", JsonPrimitive(true))
            put("pagination", paginationToJson(meta))
            put("first_count", JsonPrimitive(firstItems.size))
            put("first_unique", JsonPrimitive(baseItems.size))
            put("selected_strategy", JsonPrimitive(selected.name))
            put("fetched_unique", JsonPrimitive(selected.items.size))
            put("absolute_strategy", outcomeToJson(absolute))
            if (pageIndex != null) {
                put("page_index_strategy", outcomeToJson(pageIndex))
            }
        }

        return PagedFetchResult(
            items = selected.items,
            debug = debug
        )
    }

    suspend fun persistAll(snapshot: Snapshot) {
        orderedSections().forEach { section ->
            persistSection(snapshot, section)
        }
    }

    suspend fun persistSection(snapshot: Snapshot, section: Section) {
        when (section) {
            Section.CACHE_RAW -> persistRawSnapshot(snapshot)
            Section.POS_PROFILES -> persistPosProfiles(snapshot)
            Section.COMPANY -> persistCompany(snapshot)
            Section.STOCK_SETTINGS -> persistStockSettings(snapshot)
            Section.EXCHANGE_RATES -> persistExchangeRates(snapshot)
            Section.PAYMENT_TERMS -> persistPaymentTerms(snapshot)
            Section.DELIVERY_CHARGES -> persistDeliveryCharges(snapshot)
            Section.CUSTOMER_GROUPS -> persistCustomerGroups(snapshot)
            Section.TERRITORIES -> persistTerritories(snapshot)
            Section.CATEGORIES -> persistCategories(snapshot)
            Section.INVENTORY_ITEMS -> persistInventoryItems(snapshot)
            Section.CUSTOMERS -> persistCustomers(snapshot)
            Section.INVOICES -> persistInvoices(snapshot)
            Section.PAYMENT_ENTRIES -> persistPaymentEntriesSnapshot(snapshot)
            Section.INVENTORY_ALERTS -> persistInventoryAlertsSnapshot(snapshot)
            Section.ACTIVITY_EVENTS -> persistActivityEventsSnapshot(snapshot)
        }
    }

    private suspend fun persistRawSnapshot(snapshot: Snapshot) {
        configurationStore.saveRaw(KEY_BOOTSTRAP_RAW, snapshot.raw.toString())
    }

    private suspend fun persistPosProfiles(snapshot: Snapshot) {
        val posSyncSnapshot = BootstrapPosSyncDto(
            posProfiles = snapshot.data.posProfiles,
            paymentMethods = snapshot.data.paymentMethods,
            paymentModes = snapshot.data.paymentModes
        )
        posProfilePaymentMethodSyncRepository.syncProfilesWithPaymentsSnapshot(posSyncSnapshot)
    }

    private suspend fun persistCompany(snapshot: Snapshot) {
        val companies = snapshot.data.resolvedCompanies
        if (companies.isEmpty()) return
        companies.forEach { company ->
            val companyName = company.resolvedCompanyName?.trim()?.takeIf { it.isNotBlank() }
                ?: return@forEach
            val entity = CompanyEntity(
                companyName = companyName,
                defaultCurrency = company.defaultCurrency ?: "NIO",
                taxId = company.taxId,
                country = company.country,
                defaultReceivableAccount = company.defaultReceivableAccount,
                defaultReceivableAccountCurrency = company.defaultReceivableAccountCurrency,
                isDeleted = false
            )
            companyDao.insert(entity)
        }
        val names = companies.mapNotNull {
            it.resolvedCompanyName?.trim()?.takeIf { name -> name.isNotBlank() }
        }
            .distinct()
        companyDao.hardDeleteDeletedNotIn(names)
        companyDao.softDeleteNotIn(names)
    }

    private suspend fun persistStockSettings(snapshot: Snapshot) {
        stockSettingsRepository.applyBootstrapStockSettings(snapshot.data.stockSettings)
    }

    private suspend fun persistExchangeRates(snapshot: Snapshot) {
        val normalizedRates = mutableMapOf<String, Double>()
        snapshot.data.exchangeRates?.rates?.forEach { (code, rate) ->
            val normalizedCode = code.trim().uppercase()
            if (normalizedCode.isNotBlank() && rate > 0.0) {
                normalizedRates[normalizedCode] = rate
            }
        }

        val baseCurrency = snapshot.data.exchangeRates?.baseCurrency
            ?.trim()
            ?.uppercase()
            ?.takeIf { it.isNotBlank() }

        if (!baseCurrency.isNullOrBlank() && normalizedRates[baseCurrency] == null) {
            normalizedRates[baseCurrency] = 1.0
        }
        if (normalizedRates.isEmpty()) return
        exchangeRateLocalSource.clear()

        val currencies = normalizedRates.keys.toList()
        currencies.forEach { from ->
            val fromToBase = normalizedRates[from] ?: return@forEach
            currencies.forEach inner@{ to ->
                if (from == to) return@inner
                val toToBase = normalizedRates[to] ?: return@inner
                if (fromToBase <= 0.0 || toToBase <= 0.0) return@inner
                val crossRate = fromToBase / toToBase
                if (crossRate <= 0.0) return@inner
                exchangeRateLocalSource.save(ExchangeRateEntity.fromPair(from, to, crossRate))
            }
        }
    }

    private suspend fun persistPaymentTerms(snapshot: Snapshot) {
        val entities = snapshot.data.paymentTerms.map { it.toEntity() }
        if (entities.isNotEmpty()) {
            paymentTermLocalSource.insertAll(entities)
        }
        val names = entities.map { it.name }.ifEmpty { listOf("__empty__") }
        paymentTermLocalSource.hardDeleteDeletedMissing(names)
        paymentTermLocalSource.softDeleteMissing(names)
    }

    private suspend fun persistDeliveryCharges(snapshot: Snapshot) {
        val entities = snapshot.data.deliveryCharges.map { it.toEntity() }
        if (entities.isNotEmpty()) {
            deliveryChargeLocalSource.insertAll(entities)
        }
        val labels = entities.map { it.label }.ifEmpty { listOf("__empty__") }
        deliveryChargeLocalSource.hardDeleteDeletedMissing(labels)
        deliveryChargeLocalSource.softDeleteMissing(labels)
    }

    private suspend fun persistCustomerGroups(snapshot: Snapshot) {
        val entities = snapshot.data.customerGroups.map { it.toEntity() }
        if (entities.isNotEmpty()) {
            customerGroupLocalSource.insertAll(entities)
        }
        val names = entities.map { it.name }.ifEmpty { listOf("__empty__") }
        customerGroupLocalSource.hardDeleteDeletedMissing(names)
        customerGroupLocalSource.softDeleteMissing(names)
    }

    private suspend fun persistTerritories(snapshot: Snapshot) {
        val entities = snapshot.data.territories.map { it.toEntity() }
        if (entities.isNotEmpty()) {
            territoryLocalSource.insertAll(entities)
        }
        val names = entities.map { it.name }.ifEmpty { listOf("__empty__") }
        territoryLocalSource.hardDeleteDeletedMissing(names)
        territoryLocalSource.softDeleteMissing(names)
    }

    private suspend fun persistCategories(snapshot: Snapshot) {
        val entities = snapshot.data.categories.map { it.toEntity() }
        if (entities.isNotEmpty()) {
            inventoryLocalSource.insertCategories(entities)
        }
        val names = entities.map { it.name }.ifEmpty { listOf("__empty__") }
        inventoryLocalSource.deleteMissingCategories(names)
    }

    private suspend fun persistInventoryItems(snapshot: Snapshot) {
        val entities = snapshot.data.inventoryItems.toEntity()
        if (entities.isNotEmpty()) {
            inventoryLocalSource.insertAll(entities)
        }
        val codes = entities.map { it.itemCode }.ifEmpty { listOf("__empty__") }
        inventoryLocalSource.deleteMissing(codes)
        savePersistedCount(
            section = "inventory",
            fetched = snapshot.data.inventoryItems.size,
            persisted = inventoryLocalSource.count()
        )
    }

    private suspend fun persistCustomers(snapshot: Snapshot) {
        val contextCompany = snapshot.data.context?.company?.trim()?.takeIf { it.isNotBlank() }
        val companyFallbackByName = snapshot.data.resolvedCompanies
            .mapNotNull { company ->
                val name = company.resolvedCompanyName?.trim()?.takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                name.lowercase() to company
            }
            .toMap()
        val contextCompanyFallback = contextCompany?.lowercase()?.let { companyFallbackByName[it] }
        val entities = snapshot.data.customers.map { dto ->
            val companyCreditLimit = dto.creditLimits.firstOrNull()?.creditLimit
            val receivable = dto.resolveReceivableAccount(contextCompany)
            val fallbackReceivable = contextCompanyFallback?.defaultReceivableAccount
            val fallbackReceivableCurrency =
                contextCompanyFallback?.defaultReceivableAccountCurrency
            dto.toEntity(
                creditLimit = companyCreditLimit,
                availableCredit = companyCreditLimit,
                pendingInvoicesCount = 0,
                totalPendingAmount = 0.0,
                state = "Sin Pendientes",
                receivableAccount = receivable?.account ?: fallbackReceivable,
                receivableAccountCurrency = receivable?.accountCurrency
                    ?: fallbackReceivableCurrency
                    ?: dto.partyAccountCurrency
            )
        }
        if (entities.isNotEmpty()) {
            customerLocalSource.insertAll(entities)
        }
        val ids = entities.map { it.name }
        customerLocalSource.deleteMissing(ids)
        customerOutboxLocalSource.deleteMissingCustomerIds(ids)
        savePersistedCount(
            section = "customers",
            fetched = snapshot.data.customers.size,
            persisted = customerLocalSource.count()
        )
    }

    private suspend fun persistInvoices(snapshot: Snapshot) {
        val invoices = snapshot.data.invoices.toEntities()
        if (invoices.isNotEmpty()) {
            customerLocalSource.saveInvoices(invoices)
        }
        val invoiceNames = invoices.mapNotNull { payload ->
            payload.invoice.invoiceName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }.distinct()
        invoiceLocalSource.softDeleteMissingRemoteInvoices(invoiceNames)
        val customerIds = snapshot.data.customers
            .map { it.name }
            .filter { it.isNotBlank() }
            .ifEmpty {
                invoices.map { it.invoice.customer }.distinct()
            }
        customerIds.forEach { customerId ->
            customerLocalSource.refreshCustomerSummary(customerId)
        }
        savePersistedCount(
            section = "invoices",
            fetched = snapshot.data.invoices.size,
            persisted = invoiceLocalSource.countAllInvoices()
        )
    }

    private suspend fun persistPaymentEntriesSnapshot(snapshot: Snapshot) {
        val entries = api.json.encodeToString(snapshot.data.paymentEntries)
        configurationStore.saveRaw(KEY_BOOTSTRAP_PAYMENT_ENTRIES, entries)
        savePersistedCount(
            section = "payment_entries",
            fetched = snapshot.data.paymentEntries.size,
            persisted = snapshot.data.paymentEntries.size
        )
    }

    private suspend fun persistInventoryAlertsSnapshot(snapshot: Snapshot) {
        val alerts = api.json.encodeToString(snapshot.data.inventoryAlerts)
        configurationStore.saveRaw(KEY_BOOTSTRAP_INVENTORY_ALERTS, alerts)
        savePersistedCount(
            section = "inventory_alerts",
            fetched = snapshot.data.inventoryAlerts.size,
            persisted = snapshot.data.inventoryAlerts.size
        )
    }

    private suspend fun persistActivityEventsSnapshot(snapshot: Snapshot) {
        val events = api.json.encodeToString(snapshot.data.activityEvents)
        configurationStore.saveRaw(KEY_BOOTSTRAP_ACTIVITY_EVENTS, events)
        savePersistedCount(
            section = "activity",
            fetched = snapshot.data.activityEvents.size,
            persisted = snapshot.data.activityEvents.size
        )
    }

    private suspend fun savePersistedCount(
        section: String,
        fetched: Int,
        persisted: Int
    ) {
        val existingRaw = configurationStore.loadRaw(KEY_BOOTSTRAP_COUNTS)
        val existingObject = runCatching {
            if (existingRaw.isNullOrBlank()) JsonObject(emptyMap())
            else api.json.decodeFromString<JsonObject>(existingRaw)
        }.getOrDefault(JsonObject(emptyMap()))
        val updated = existingObject.toMutableMap().apply {
            put(
                section,
                buildJsonObject {
                    put("fetched", JsonPrimitive(fetched))
                    put("persisted", JsonPrimitive(persisted))
                    put("synced_at", JsonPrimitive(Clock.System.now().toEpochMilliseconds()))
                }
            )
        }
        configurationStore.saveRaw(KEY_BOOTSTRAP_COUNTS, JsonObject(updated).toString())
    }
}

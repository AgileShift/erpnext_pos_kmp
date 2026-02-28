package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.configuration.ConfigurationStore
import com.erpnext.pos.localSource.dao.CompanyDao
import com.erpnext.pos.localSource.datasources.CompanyAccountLocalSource
import com.erpnext.pos.localSource.datasources.CustomerGroupLocalSource
import com.erpnext.pos.localSource.datasources.CustomerLocalSource
import com.erpnext.pos.localSource.datasources.CustomerOutboxLocalSource
import com.erpnext.pos.localSource.datasources.DeliveryChargeLocalSource
import com.erpnext.pos.localSource.datasources.ExchangeRateLocalSource
import com.erpnext.pos.localSource.datasources.InventoryLocalSource
import com.erpnext.pos.localSource.datasources.InvoiceLocalSource
import com.erpnext.pos.localSource.datasources.PaymentTermLocalSource
import com.erpnext.pos.localSource.datasources.SupplierLocalSource
import com.erpnext.pos.localSource.datasources.TerritoryLocalSource
import com.erpnext.pos.localSource.entities.CompanyEntity
import com.erpnext.pos.localSource.entities.ExchangeRateEntity
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.BootstrapFullSnapshotDto
import com.erpnext.pos.remoteSource.dto.BootstrapPosSyncDto
import com.erpnext.pos.remoteSource.dto.BootstrapRequestDto
import com.erpnext.pos.remoteSource.dto.PaymentEntryDto
import com.erpnext.pos.remoteSource.mapper.resolveReceivableAccount
import com.erpnext.pos.remoteSource.mapper.toEntities
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.AppLogger
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
    private val supplierLocalSource: SupplierLocalSource,
    private val companyAccountLocalSource: CompanyAccountLocalSource,
    private val customerGroupLocalSource: CustomerGroupLocalSource,
    private val territoryLocalSource: TerritoryLocalSource,
    private val inventoryLocalSource: InventoryLocalSource,
    private val customerLocalSource: CustomerLocalSource,
    private val customerOutboxLocalSource: CustomerOutboxLocalSource,
    private val invoiceLocalSource: InvoiceLocalSource
) {
    companion object {
        private const val KEY_BOOTSTRAP_INVENTORY_ALERTS = "bootstrap.raw.inventory_alerts"
        private const val KEY_BOOTSTRAP_META = "bootstrap.debug.meta"
        private const val KEY_BOOTSTRAP_COUNTS = "bootstrap.debug.counts"
        private const val DEFAULT_PAGE_LIMIT = 50
        private const val MAX_PAGE_FETCH = 200
    }

    private data class PaginationMeta(
        val offset: Int,
        val limit: Int,
        val total: Int,
        val hasMore: Boolean?
    )

    private data class PagedFetchResult<T>(
        val items: List<T>,
        val debug: JsonObject
    )

    enum class Section(
        val label: String,
        val message: String
    ) {
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
        SUPPLIERS(
            label = "Proveedores",
            message = "Guardando proveedores..."
        ),
        COMPANY_ACCOUNTS(
            label = "Cuentas contables",
            message = "Guardando cuentas contables..."
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
            message = "Guardando entradas de pago en facturas..."
        ),
        INVENTORY_ALERTS(
            label = "Alertas de Inventario",
            message = "Guardando snapshot de alertas..."
        ),
        ACTIVITY_EVENTS(
            label = "Eventos",
            message = "Eventos via WS (omitido en bootstrap)."
        )
    }

    data class Snapshot(
        val raw: JsonObject,
        val data: BootstrapFullSnapshotDto
    )

    fun orderedSections(): List<Section> = Section.entries.filterNot { it == Section.ACTIVITY_EVENTS }

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
                // Invoice snapshot must be complete to preserve customer pending balances.
                recentPaidOnly = false,
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
                    recentPaidOnly = false,
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
                    recentPaidOnly = false,
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

        val allInventory = inventoryFetch.items
        val allCustomers = customersFetch.items
        val allInvoices = invoicesFetch.items
        val allPaymentEntries = paymentEntriesFetch.items

        val mergedData = baseData.copy(
            inventoryItems = allInventory,
            customers = allCustomers,
            invoices = allInvoices,
            paymentEntries = allPaymentEntries,
            inventoryAlerts = allAlerts,
            activityEvents = emptyList()
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
        }

        persistBootstrapMeta(
            metas = mapOf(
                "inventory" to parsePaginationMeta(inventoryRaw, "inventory"),
                "customers" to parsePaginationMeta(customersRaw, "customers"),
                "invoices" to parsePaginationMeta(invoicesRaw, "invoices"),
                "payment_entries" to parsePaginationMeta(invoicesRaw, "payment_entries")
            ),
            fetchedCounts = mapOf(
                "inventory" to mergedData.inventoryItems.size,
                "customers" to mergedData.customers.size,
                "invoices" to mergedData.invoices.size,
                "payment_entries" to mergedData.paymentEntries.size,
                "inventory_alerts" to mergedData.inventoryAlerts.size
            ),
            pagingDebug = mapOf(
                "inventory" to inventoryFetch.debug,
                "customers" to customersFetch.debug,
                "invoices" to invoicesFetch.debug,
                "payment_entries" to paymentEntriesFetch.debug
            )
        )

        val snapshot = Snapshot(raw = JsonObject(normalizedRaw), data = mergedData)
        logSnapshotOverview(snapshot)
        return snapshot
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
        val hasMore = when {
            hasMoreRaw == null -> null
            hasMoreRaw.booleanOrNull != null -> hasMoreRaw.booleanOrNull
            hasMoreRaw.intOrNull != null -> hasMoreRaw.intOrNull != 0
            hasMoreRaw.contentOrNull != null ->
                hasMoreRaw.contentOrNull?.lowercase() in setOf("1", "true", "yes")
            else -> null
        }
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

        val meta = parsePaginationMeta(firstRaw, sectionKey) ?: return PagedFetchResult(
            items = baseItems,
            debug = buildJsonObject {
                put("enabled", JsonPrimitive(false))
                put("reason", JsonPrimitive("missing_pagination"))
                put("first_count", JsonPrimitive(firstItems.size))
                put("first_unique", JsonPrimitive(baseItems.size))
            }
        )
        val targetTotal = meta.total.takeIf { it > 0 }
        val shouldFetchMore =
            (targetTotal != null && baseItems.size < targetTotal) || meta.hasMore == true
        if (!shouldFetchMore) {
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

            while (pagesFetched < MAX_PAGE_FETCH &&
                (targetTotal == null || merged.size < targetTotal)
            ) {
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

                if (targetTotal != null && merged.size >= targetTotal) {
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
            absolute.items.size <= baseItems.size &&
                ((targetTotal != null && targetTotal > baseItems.size) || meta.hasMore != false)
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
        val payloadCount = sectionPayloadCount(snapshot, section)
        AppLogger.info(
            "BootstrapSyncRepository.persistSection start ${section.name} " +
                "(items=$payloadCount)"
        )
        when (section) {
            Section.POS_PROFILES -> persistPosProfiles(snapshot)
            Section.COMPANY -> persistCompany(snapshot)
            Section.STOCK_SETTINGS -> persistStockSettings(snapshot)
            Section.EXCHANGE_RATES -> persistExchangeRates(snapshot)
            Section.PAYMENT_TERMS -> persistPaymentTerms(snapshot)
            Section.DELIVERY_CHARGES -> persistDeliveryCharges(snapshot)
            Section.SUPPLIERS -> persistSuppliers(snapshot)
            Section.COMPANY_ACCOUNTS -> persistCompanyAccounts(snapshot)
            Section.CUSTOMER_GROUPS -> persistCustomerGroups(snapshot)
            Section.TERRITORIES -> persistTerritories(snapshot)
            Section.CATEGORIES -> persistCategories(snapshot)
            Section.INVENTORY_ITEMS -> persistInventoryItems(snapshot)
            Section.CUSTOMERS -> persistCustomers(snapshot)
            Section.INVOICES -> persistInvoices(snapshot)
            Section.PAYMENT_ENTRIES -> persistPaymentEntriesSnapshot(snapshot)
            Section.INVENTORY_ALERTS -> persistInventoryAlertsSnapshot(snapshot)
            Section.ACTIVITY_EVENTS -> Unit
        }
        AppLogger.info(
            "BootstrapSyncRepository.persistSection done ${section.name} " +
                "(items=$payloadCount)"
        )
    }

    private fun logSnapshotOverview(snapshot: Snapshot) {
        val data = snapshot.data
        AppLogger.info(
            "BootstrapSyncRepository.fetchSnapshot payload counts: " +
                "context=${if (data.context != null) 1 else 0}, " +
                "pos_profiles=${data.posProfiles.size}, " +
                "payment_methods=${data.paymentMethods?.size ?: 0}, " +
                "payment_modes=${data.paymentModes?.size ?: 0}, " +
                "companies=${data.resolvedCompanies.size}, " +
                "exchange_rates=${if (data.exchangeRates != null) 1 else 0}, " +
                "payment_terms=${data.paymentTerms.size}, " +
                "delivery_charges=${data.shippingRules.size}, " +
                "suppliers=${data.suppliers.size}, " +
                "company_accounts=${data.companyAccounts.size}, " +
                "customer_groups=${data.customerGroups.size}, " +
                "territories=${data.territories.size}, " +
                "categories=${data.categories.size}, " +
                "inventory_items=${data.inventoryItems.size}, " +
                "customers=${data.customers.size}, " +
                "invoices=${data.invoices.size}, " +
                "payment_entries=${data.paymentEntries.size}, " +
                "inventory_alerts=${data.inventoryAlerts.size}, " +
                "activity_events=${data.activityEvents.size}"
        )
    }

    private fun sectionPayloadCount(snapshot: Snapshot, section: Section): Int {
        val data = snapshot.data
        return when (section) {
            Section.POS_PROFILES -> data.posProfiles.size
            Section.COMPANY -> data.resolvedCompanies.size
            Section.STOCK_SETTINGS -> if (data.stockSettings != null) 1 else 0
            Section.EXCHANGE_RATES -> if (data.exchangeRates != null) 1 else 0
            Section.PAYMENT_TERMS -> data.paymentTerms.size
            Section.DELIVERY_CHARGES -> data.shippingRules.size
            Section.SUPPLIERS -> data.suppliers.size
            Section.COMPANY_ACCOUNTS -> data.companyAccounts.size
            Section.CUSTOMER_GROUPS -> data.customerGroups.size
            Section.TERRITORIES -> data.territories.size
            Section.CATEGORIES -> data.categories.size
            Section.INVENTORY_ITEMS -> data.inventoryItems.size
            Section.CUSTOMERS -> data.customers.size
            Section.INVOICES -> data.invoices.size
            Section.PAYMENT_ENTRIES -> data.paymentEntries.size
            Section.INVENTORY_ALERTS -> data.inventoryAlerts.size
            Section.ACTIVITY_EVENTS -> data.activityEvents.size
        }
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
        if (companies.isEmpty()) {
            companyDao.softDeleteAll()
            companyDao.hardDeleteAllDeleted()
            return
        }
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
            val normalizedRate = rate ?: return@forEach
            if (normalizedCode.isNotBlank() && normalizedRate > 0.0) {
                normalizedRates[normalizedCode] = normalizedRate
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
        val entities = snapshot.data.shippingRules.map { it.toEntity() }
        if (entities.isNotEmpty()) {
            deliveryChargeLocalSource.insertAll(entities)
        }
        val labels = entities.map { it.label }.ifEmpty { listOf("__empty__") }
        deliveryChargeLocalSource.hardDeleteDeletedMissing(labels)
        deliveryChargeLocalSource.softDeleteMissing(labels)
    }

    private suspend fun persistSuppliers(snapshot: Snapshot) {
        val entities = snapshot.data.suppliers.map { it.toEntity() }
        if (entities.isNotEmpty()) {
            supplierLocalSource.insertAll(entities)
        }
        val names = entities.map { it.name }.ifEmpty { listOf("__empty__") }
        supplierLocalSource.hardDeleteDeletedMissing(names)
        supplierLocalSource.softDeleteMissing(names)
    }

    private suspend fun persistCompanyAccounts(snapshot: Snapshot) {
        val entities = snapshot.data.companyAccounts.map { it.toEntity() }
        if (entities.isNotEmpty()) {
            companyAccountLocalSource.insertAll(entities)
        }
        val names = entities.map { it.name }.ifEmpty { listOf("__empty__") }
        companyAccountLocalSource.hardDeleteDeletedMissing(names)
        companyAccountLocalSource.softDeleteMissing(names)
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
        val fallbackProfile = resolveBootstrapContextProfile(snapshot)
        val fallbackOpening = resolveBootstrapContextOpening(snapshot)
        val invoices = snapshot.data.invoices
            .toEntities()
            .map { payload ->
                normalizeInvoicePayload(
                    payload = payload,
                    fallbackProfile = fallbackProfile,
                    fallbackOpening = fallbackOpening
                )
            }
        if (invoices.isNotEmpty()) {
            customerLocalSource.saveInvoices(invoices)
        }
        val invoiceNames = invoices.mapNotNull { payload ->
            payload.invoice.invoiceName
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }.distinct()
        if (shouldSoftDeleteMissingInvoices(invoiceNames.size)) {
            invoiceLocalSource.softDeleteMissingRemoteInvoices(invoiceNames)
        } else {
            AppLogger.warn(
                "BootstrapSyncRepository.persistInvoices: snapshot incompleto, " +
                    "se omite soft-delete de facturas (fetched=${invoiceNames.size})"
            )
        }
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
        val payments = buildInvoicePaymentRowsFromPaymentEntries(
            entries = snapshot.data.paymentEntries,
            fallbackOpening = resolveBootstrapContextOpening(snapshot)
        )
        val remoteEntryNames = payments
            .mapNotNull { it.remotePaymentEntry?.trim()?.takeIf { value -> value.isNotBlank() } }
            .distinct()
        if (remoteEntryNames.isNotEmpty()) {
            invoiceLocalSource.deleteRemotePaymentsByRemoteEntries(remoteEntryNames)
        }
        if (payments.isNotEmpty()) {
            invoiceLocalSource.upsertPayments(payments)
        }
        savePersistedCount(
            section = "payment_entries",
            fetched = snapshot.data.paymentEntries.size,
            persisted = payments.size
        )
    }

    private suspend fun buildInvoicePaymentRowsFromPaymentEntries(
        entries: List<PaymentEntryDto>,
        fallbackOpening: String?
    ): List<POSInvoicePaymentEntity> {
        if (entries.isEmpty()) return emptyList()

        val existingInvoiceCache = mutableMapOf<String, com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments?>()

        suspend fun localInvoice(invoiceName: String): com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments? {
            existingInvoiceCache[invoiceName]?.let { return it }
            val wrapper = invoiceLocalSource.getInvoiceByNameAny(invoiceName)
            existingInvoiceCache[invoiceName] = wrapper
            return wrapper
        }

        val rows = mutableListOf<POSInvoicePaymentEntity>()
        for (entry in entries) {
            val remotePaymentEntry = entry.name?.trim()?.takeIf { it.isNotBlank() } ?: continue
            val modeOfPayment = entry.modeOfPayment?.trim()?.takeIf { it.isNotBlank() } ?: continue
            val postingDate = entry.postingDate?.trim()?.takeIf { it.isNotBlank() }
            val openingFromEntry = entry.posOpeningEntry?.trim()?.takeIf { it.isNotBlank() }
            val paymentCurrency = entry.paidToAccountCurrency
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: entry.paidFromAccountCurrency?.trim()?.takeIf { it.isNotBlank() }

            entry.references.forEach { reference ->
                if (!reference.referenceDoctype.equals("Sales Invoice", ignoreCase = true)) return@forEach
                val invoiceName = reference.referenceName?.trim()?.takeIf { it.isNotBlank() } ?: return@forEach
                val wrapper = localInvoice(invoiceName) ?: return@forEach
                val invoice = wrapper.invoice

                val allocatedAmount = reference.allocatedAmount
                if (allocatedAmount == 0.0) return@forEach
                val paidAmount = entry.paidAmount
                val receivedAmount = entry.receivedAmount
                val allocatedRatio = if (paidAmount > 0.0) {
                    (allocatedAmount / paidAmount).coerceIn(0.0, 1.0)
                } else {
                    1.0
                }
                val enteredAmount = if (receivedAmount > 0.0 && paidAmount > 0.0) {
                    receivedAmount * allocatedRatio
                } else {
                    allocatedAmount
                }
                val exchangeRate = when {
                    enteredAmount > 0.0 -> allocatedAmount / enteredAmount
                    else -> 1.0
                }
                val openingFromReference = reference.posOpeningEntry?.trim()?.takeIf { it.isNotBlank() }
                val openingFromExistingPaymentMatch = wrapper.payments.firstOrNull { payment ->
                    payment.remotePaymentEntry?.equals(remotePaymentEntry, ignoreCase = true) == true &&
                        !payment.posOpeningEntry.isNullOrBlank()
                }?.posOpeningEntry
                val openingFromAnyExistingPayment = wrapper.payments
                    .firstOrNull { !it.posOpeningEntry.isNullOrBlank() }
                    ?.posOpeningEntry

                rows += POSInvoicePaymentEntity(
                    parentInvoice = invoiceName,
                    modeOfPayment = modeOfPayment,
                    amount = allocatedAmount,
                    enteredAmount = enteredAmount,
                    paymentCurrency = paymentCurrency,
                    exchangeRate = exchangeRate,
                    paymentReference = remotePaymentEntry,
                    remotePaymentEntry = remotePaymentEntry,
                    paymentDate = postingDate,
                    posOpeningEntry = openingFromReference
                        ?: openingFromEntry
                        ?: openingFromExistingPaymentMatch
                        ?: openingFromAnyExistingPayment
                        ?: invoice.posOpeningEntry?.takeIf { it.isNotBlank() }
                        ?: fallbackOpening,
                    syncStatus = "Synced"
                )
            }
        }
        return rows
    }

    private suspend fun normalizeInvoicePayload(
        payload: com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments,
        fallbackProfile: String?,
        fallbackOpening: String?
    ): com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments {
        val invoiceName = payload.invoice.invoiceName?.trim().orEmpty()
        val local = if (invoiceName.isBlank()) null else invoiceLocalSource.getInvoiceByNameAny(invoiceName)
        val localInvoice = local?.invoice

        val resolvedProfile = payload.invoice.profileId
            ?.takeIf { it.isNotBlank() }
            ?: localInvoice?.profileId?.takeIf { it.isNotBlank() }
            ?: fallbackProfile
        val openingFromAnyLocalPayment = local?.payments
            ?.firstOrNull { !it.posOpeningEntry.isNullOrBlank() }
            ?.posOpeningEntry
            ?.takeIf { it.isNotBlank() }
        val hasPaymentSignal = payload.invoice.paidAmount > 0.0 || payload.payments.any { payment ->
            payment.amount > 0.0 || payment.enteredAmount > 0.0 || !payment.remotePaymentEntry.isNullOrBlank()
        }
        val resolvedOpening = payload.invoice.posOpeningEntry
            ?.takeIf { it.isNotBlank() }
            ?: localInvoice?.posOpeningEntry?.takeIf { it.isNotBlank() }
            ?: openingFromAnyLocalPayment
            ?: fallbackOpening?.takeIf { hasPaymentSignal && it.isNotBlank() }

        val normalizedInvoice = payload.invoice.copy(
            profileId = resolvedProfile,
            posOpeningEntry = resolvedOpening
        )
        val normalizedPayments = payload.payments
            .filter { payment ->
                payment.amount > 0.0 || payment.enteredAmount > 0.0 ||
                    !payment.remotePaymentEntry.isNullOrBlank()
            }
            .map { payment ->
                val openingFromLocalPaymentMatch = local?.payments
                    ?.firstOrNull { existing ->
                        existing.remotePaymentEntry?.isNotBlank() == true &&
                            existing.remotePaymentEntry.equals(
                                payment.remotePaymentEntry,
                                ignoreCase = true
                            ) &&
                            !existing.posOpeningEntry.isNullOrBlank()
                    }
                    ?.posOpeningEntry
                val openingFromLocalPaymentAny = local?.payments
                    ?.firstOrNull { !it.posOpeningEntry.isNullOrBlank() }
                    ?.posOpeningEntry
                payment.copy(
                    parentInvoice = normalizedInvoice.invoiceName.orEmpty(),
                    posOpeningEntry = payment.posOpeningEntry
                        ?.takeIf { it.isNotBlank() }
                        ?: openingFromLocalPaymentMatch
                        ?: openingFromLocalPaymentAny
                        ?: resolvedOpening
                )
            }
        return payload.copy(
            invoice = normalizedInvoice,
            payments = normalizedPayments
        )
    }

    private suspend fun resolveBootstrapContextProfile(snapshot: Snapshot): String? {
        val fromSnapshot = snapshot.data.context?.profileName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        if (!fromSnapshot.isNullOrBlank()) return fromSnapshot

        val raw = configurationStore.loadRaw("bootstrap.context") ?: return null
        val root = runCatching {
            api.json.parseToJsonElement(raw).jsonObject
        }.getOrNull() ?: return null
        return root["profileName"]?.jsonPrimitive?.contentOrNull
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private suspend fun resolveBootstrapContextOpening(snapshot: Snapshot): String? {
        val fromSnapshotRaw = (snapshot.raw["context"] as? JsonObject)?.let { context ->
            context["pos_opening_entry"]?.jsonPrimitive?.contentOrNull
                ?: context["posOpeningEntry"]?.jsonPrimitive?.contentOrNull
        }?.trim()?.takeIf { it.isNotBlank() }
        if (!fromSnapshotRaw.isNullOrBlank()) return fromSnapshotRaw

        val raw = configurationStore.loadRaw("bootstrap.context") ?: return null
        val root = runCatching {
            api.json.parseToJsonElement(raw).jsonObject
        }.getOrNull() ?: return null
        return root["posOpeningEntry"]?.jsonPrimitive?.contentOrNull
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private suspend fun shouldSoftDeleteMissingInvoices(fetchedCount: Int): Boolean {
        val raw = configurationStore.loadRaw(KEY_BOOTSTRAP_META) ?: return false
        val root = runCatching {
            api.json.parseToJsonElement(raw).jsonObject
        }.getOrNull() ?: return false
        val invoices = root["invoices"]?.jsonObject ?: return false
        val fetched = invoices["fetched"]?.jsonPrimitive?.intOrNull ?: fetchedCount
        val pagination = invoices["pagination"]?.jsonObject
        val total = pagination?.get("total")?.jsonPrimitive?.intOrNull
        val hasMore = pagination?.get("has_more")?.jsonPrimitive?.let(::parseJsonBoolean)

        if (fetched <= 0) return false
        if (total != null && fetched < total) return false
        if (total == null && hasMore == true) return false
        return true
    }

    private fun parseJsonBoolean(value: JsonPrimitive): Boolean? {
        return value.booleanOrNull
            ?: value.intOrNull?.let { it != 0 }
            ?: value.contentOrNull?.lowercase()?.let { token ->
                token in setOf("1", "true", "yes")
            }
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

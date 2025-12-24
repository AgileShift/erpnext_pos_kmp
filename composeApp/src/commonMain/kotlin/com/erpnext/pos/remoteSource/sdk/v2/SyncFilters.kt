package com.erpnext.pos.remoteSource.sdk.v2

import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.remoteSource.sdk.Filter
import com.erpnext.pos.remoteSource.sdk.filters

/**
 * Helper functions to build incremental sync filters for ERPNext list queries.
 *
 * Each function returns a flat list of filters that can be passed to the list endpoint.
 */
object IncrementalSyncFilters {
    fun company(modifiedSince: String? = null): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
        }

    fun employee(modifiedSince: String? = null): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
        }

    fun salesPerson(modifiedSince: String? = null): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
        }

    fun item(modifiedSince: String? = null): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
        }

    fun category(modifiedSince: String? = null): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
        }

    fun itemPrice(
        ctx: SyncContext,
        modifiedSince: String? = null,
        priceList: String? = ctx.priceList
    ): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
            priceList?.let { "price_list" eq it }
        }

    fun user(modifiedSince: String? = null): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
        }

    fun address(modifiedSince: String? = null): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
        }

    fun contacts(modifiedSince: String? = null): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
        }

    fun customer(ctx: SyncContext, modifiedSince: String? = null): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
            "territory" eq ctx.territoryId
        }

    fun customerContact(modifiedSince: String? = null): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
        }

    fun salesInvoice(
        ctx: SyncContext,
        modifiedSince: String? = null,
        routeId: String? = null
    ): List<Filter> =
        filters {
            "posting_date" gte ctx.fromDate
            modifiedSince?.let { "modified" gte it }
            routeId?.let { "route" eq it }
            "territory" eq ctx.territoryId
        }

    fun quotation(ctx: SyncContext, modifiedSince: String? = null, routeId: String? = null): List<Filter> =
        filters {
            "transaction_date" gte ctx.fromDate
            modifiedSince?.let { "modified" gte it }
            routeId?.let { "route" eq it }
            "territory" eq ctx.territoryId
        }

    fun salesOrder(
        ctx: SyncContext,
        modifiedSince: String? = null,
        routeId: String? = null
    ): List<Filter> =
        filters {
            "transaction_date" gte ctx.fromDate
            modifiedSince?.let { "modified" gte it }
            routeId?.let { "route" eq it }
            "territory" eq ctx.territoryId
        }

    fun paymentEntry(ctx: SyncContext, modifiedSince: String? = null): List<Filter> =
        filters {
            "posting_date" gte ctx.fromDate
            modifiedSince?.let { "modified" gte it }
            "territory" eq ctx.territoryId
        }

    fun deliveryNote(
        ctx: SyncContext,
        modifiedSince: String? = null,
        warehouseId: String? = ctx.warehouseId
    ): List<Filter> =
        filters {
            "posting_date" gte ctx.fromDate
            modifiedSince?.let { "modified" gte it }
            warehouseId?.let { "set_warehouse" eq it }
            "territory" eq ctx.territoryId
        }

    fun pricingRule(
        ctx: SyncContext,
        modifiedSince: String? = null,
        priceList: String? = ctx.priceList
    ): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
            priceList?.let { "for_price_list" eq it }
            "valid_upto" gte ctx.fromDate
            "territory" eq ctx.territoryId
        }

    fun bin(
        ctx: SyncContext,
        modifiedSince: String? = null,
        warehouseId: String? = ctx.warehouseId
    ): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
            warehouseId?.let { "warehouse" eq it }
        }

    fun purchaseInvoice(ctx: SyncContext, modifiedSince: String? = null): List<Filter> =
        filters {
            "posting_date" gte ctx.fromDate
            modifiedSince?.let { "modified" gte it }
        }

    fun stockEntry(ctx: SyncContext, modifiedSince: String? = null): List<Filter> =
        filters {
            "posting_date" gte ctx.fromDate
            modifiedSince?.let { "modified" gte it }
        }

    fun posProfile(ctx: SyncContext, modifiedSince: String? = null): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
            "company" eq ctx.companyId
        }

    fun posProfileDetails(ctx: SyncContext, modifiedSince: String? = null): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
            "company" eq ctx.companyId
        }

    fun posOpeningEntry(ctx: SyncContext, modifiedSince: String? = null): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
            "company" eq ctx.companyId
        }

    fun posClosingEntry(ctx: SyncContext, modifiedSince: String? = null): List<Filter> =
        filters {
            modifiedSince?.let { "modified" gte it }
            "company" eq ctx.companyId
        }
}

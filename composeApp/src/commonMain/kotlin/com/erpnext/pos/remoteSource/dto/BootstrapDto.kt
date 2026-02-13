package com.erpnext.pos.remoteSource.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class BootstrapRequestDto(
    @SerialName("include_inventory")
    val includeInventory: Boolean = true,
    @SerialName("include_customers")
    val includeCustomers: Boolean = true,
    @SerialName("include_invoices")
    val includeInvoices: Boolean = true,
    @SerialName("include_alerts")
    val includeAlerts: Boolean = true,
    @SerialName("include_activity")
    val includeActivity: Boolean = true,
    @SerialName("recent_paid_only")
    val recentPaidOnly: Boolean = true,
    @SerialName("profile_name")
    val profileName: String? = null,
    @SerialName("warehouse")
    val warehouse: String? = null,
    @SerialName("price_list")
    val priceList: String? = null,
    @SerialName("route")
    val route: String? = null,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("offset")
    val offset: Int? = null,
    @SerialName("limit")
    val limit: Int? = null
)

@Serializable
data class BootstrapDataDto(
    @SerialName("inventory_items")
    val inventoryItems: List<WarehouseItemDto> = emptyList(),
    @SerialName("customers")
    val customers: List<CustomerDto> = emptyList(),
    @SerialName("invoices")
    val invoices: List<SalesInvoiceDto> = emptyList(),
    @SerialName("payment_entries")
    val paymentEntries: List<PaymentEntryDto> = emptyList(),
    @SerialName("inventory_alerts")
    val inventoryAlerts: List<JsonObject> = emptyList(),
    @SerialName("activity_events")
    val activityEvents: List<JsonObject> = emptyList()
)

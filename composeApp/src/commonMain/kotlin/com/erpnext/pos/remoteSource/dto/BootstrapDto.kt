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
    @SerialName("pos_opening_entry")
    val posOpeningEntry: String? = null,
    @SerialName("warehouse")
    val warehouse: String? = null,
    @SerialName("price_list")
    val priceList: String? = null,
    @SerialName("route")
    val route: String? = null,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("from_date")
    val fromDate: String? = null,
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

@Serializable
data class BootstrapPosSyncDto(
    @SerialName("pos_profiles")
    val posProfiles: List<POSProfileDto>? = null,
    @SerialName("payment_methods")
    val paymentMethods: List<PaymentModesDto>? = null,
    @SerialName("payment_modes")
    val paymentModes: List<PaymentModesDto>? = null
) {
    val resolvedPaymentMethods: List<PaymentModesDto>?
        get() = paymentMethods ?: paymentModes
}

data class BootstrapClosingEntryDto(
    val name: String,
    val status: String? = null,
    val posProfile: String? = null,
    val company: String? = null,
    val user: String? = null,
    val postingDate: String? = null,
    val periodStartDate: String? = null,
    val periodEndDate: String? = null,
    val posOpeningEntry: String? = null,
    val docStatus: Int? = null,
    val paymentReconciliation: List<PaymentReconciliationDto> = emptyList()
)

data class BootstrapShiftSnapshotDto(
    val openShift: POSOpeningEntryDetailDto? = null,
    val posClosingEntry: BootstrapClosingEntryDto? = null
)

@Serializable
data class BootstrapContextDto(
    @SerialName("profileName")
    val profileNameCamel: String? = null,
    @SerialName("profile_name")
    val profileNameSnake: String? = null,
    @SerialName("company")
    val company: String? = null,
    @SerialName("companyCurrency")
    val companyCurrencyCamel: String? = null,
    @SerialName("company_currency")
    val companyCurrencySnake: String? = null,
    @SerialName("warehouse")
    val warehouse: String? = null,
    @SerialName("route")
    val route: String? = null,
    @SerialName("territory")
    val territory: String? = null,
    @SerialName("priceList")
    val priceListCamel: String? = null,
    @SerialName("price_list")
    val priceListSnake: String? = null,
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("partyAccountCurrency")
    val partyAccountCurrencyCamel: String? = null,
    @SerialName("party_account_currency")
    val partyAccountCurrencySnake: String? = null,
    @SerialName("monthly_sales_target")
    val monthlySalesTarget: Double? = null
) {
    val profileName: String?
        get() = profileNameCamel ?: profileNameSnake
}

@Serializable
data class BootstrapCompanyDto(
    @SerialName("company")
    val companyName: String? = null,
    @SerialName("name")
    val name: String? = null,
    @SerialName("default_currency")
    val defaultCurrency: String? = null,
    @SerialName("country")
    val country: String? = null,
    @SerialName("tax_id")
    val taxId: String? = null,
    @SerialName("default_receivable_account")
    val defaultReceivableAccount: String? = null,
    @SerialName("default_receivable_account_currency")
    val defaultReceivableAccountCurrency: String? = null
) {
    val resolvedCompanyName: String?
        get() = companyName ?: name
}

@Serializable
data class BootstrapExchangeRatesDto(
    @SerialName("base_currency")
    val baseCurrency: String? = null,
    @SerialName("date")
    val date: String? = null,
    @SerialName("rates")
    val rates: Map<String, Double> = emptyMap()
)

@Serializable
data class BootstrapFullSnapshotDto(
    @SerialName("context")
    val context: BootstrapContextDto? = null,
    @SerialName("pos_profiles")
    val posProfiles: List<POSProfileDto> = emptyList(),
    @SerialName("payment_methods")
    val paymentMethods: List<PaymentModesDto>? = null,
    @SerialName("payment_modes")
    val paymentModes: List<PaymentModesDto>? = null,
    @SerialName("company")
    val company: BootstrapCompanyDto? = null,
    @SerialName("companies")
    val companies: List<BootstrapCompanyDto> = emptyList(),
    @SerialName("stock_settings")
    val stockSettings: StockSettingsDto? = null,
    @SerialName("currencies")
    val currencies: List<CurrencyDto> = emptyList(),
    @SerialName("exchange_rates")
    val exchangeRates: BootstrapExchangeRatesDto? = null,
    @SerialName("payment_terms")
    val paymentTerms: List<PaymentTermDto> = emptyList(),
    @SerialName("delivery_charges")
    val deliveryCharges: List<DeliveryChargeDto> = emptyList(),
    @SerialName("customer_groups")
    val customerGroups: List<CustomerGroupDto> = emptyList(),
    @SerialName("territories")
    val territories: List<TerritoryDto> = emptyList(),
    @SerialName("categories")
    val categories: List<CategoryDto> = emptyList(),
    @SerialName("inventory_items")
    val inventoryItems: List<WarehouseItemDto> = emptyList(),
    @SerialName("inventory_alerts")
    val inventoryAlerts: List<JsonObject> = emptyList(),
    @SerialName("customers")
    val customers: List<CustomerDto> = emptyList(),
    @SerialName("invoices")
    val invoices: List<SalesInvoiceDto> = emptyList(),
    @SerialName("payment_entries")
    val paymentEntries: List<PaymentEntryDto> = emptyList(),
    @SerialName("activity_events")
    val activityEvents: List<JsonObject> = emptyList(),
    @SerialName("suppliers")
    val suppliers: List<SupplierDto> = emptyList(),
    @SerialName("company_accounts")
    val companyAccounts: List<CompanyAccountDto> = emptyList()
) {
    val resolvedCompanies: List<BootstrapCompanyDto>
        get() = when {
            companies.isNotEmpty() -> companies
            company != null -> listOf(company)
            else -> emptyList()
        }
}

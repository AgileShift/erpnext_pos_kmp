package com.erpnext.pos.remoteSource.dto.v2

import com.erpnext.pos.remoteSource.dto.IntAsBooleanSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CompanyDto(
    @SerialName("name")
    val companyId: String,
    @SerialName("company_name")
    val companyName: String,
    @SerialName("abbr")
    val abbr: String,
    @SerialName("default_currency")
    val defaultCurrency: String,
    @SerialName("country")
    val country: String,
    @SerialName("domain")
    val domain: String? = null,
    @SerialName("tax_id")
    val taxId: String? = null,
    @SerialName("is_group")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isGroup: Boolean = false,
    @SerialName("parent_company")
    val parentCompanyId: String? = null,
    @SerialName("company_logo")
    val companyLogo: String? = null,
    @SerialName("default_letter_head")
    val letterHead: String? = null
)

@Serializable
data class UserDto(
    @SerialName("name")
    val userId: String,
    @SerialName("email")
    val email: String? = null,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("enabled")
    @Serializable(with = IntAsBooleanSerializer::class)
    val enabled: Boolean = true,
    @SerialName("user_type")
    val userType: String? = null
)

@Serializable
data class EmployeeDto(
    @SerialName("name")
    val employeeId: String,
    @SerialName("employee_name")
    val employeeName: String,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("status")
    val status: String? = null,
    @SerialName("company")
    val company: String
)

@Serializable
data class SalesPersonDto(
    @SerialName("name")
    val salesPersonId: String,
    @SerialName("sales_person_name")
    val salesPersonName: String,
    @SerialName("employee")
    val employeeId: String? = null,
    @SerialName("is_group")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isGroup: Boolean = false,
    @SerialName("parent_sales_person")
    val parentSalesPersonId: String? = null
)

@Serializable
data class TerritoryDto(
    @SerialName("name")
    val territoryId: String,
    @SerialName("territory_name")
    val territoryName: String? = null,
    @SerialName("is_group")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isGroup: Boolean = false,
    @SerialName("parent_territory")
    val parentTerritoryId: String? = null,
    @SerialName("territory_manager")
    val territoryManagerSalesPersonId: String? = null
)

@Serializable
data class POSPaymentMethodDto(
    @SerialName("mode_of_payment")
    val modeOfPayment: String,
    @SerialName("default")
    @Serializable(with = IntAsBooleanSerializer::class)
    val isDefault: Boolean = false
)

@Serializable
data class POSProfileDto(
    @SerialName("name")
    val posProfileId: String,
    @SerialName("warehouse")
    val warehouseId: String,
    @SerialName("route")
    val routeId: String? = null,
    @SerialName("cost_center")
    val costCenterId: String,
    @SerialName("currency")
    val currency: String,
    @SerialName("selling_price_list")
    val priceList: String,
    @SerialName("allow_negative_stock")
    @Serializable(with = IntAsBooleanSerializer::class)
    val allowNegativeStock: Boolean = false,
    @SerialName("update_stock")
    @Serializable(with = IntAsBooleanSerializer::class)
    val updateStock: Boolean = true,
    @SerialName("allow_credit_sales")
    @Serializable(with = IntAsBooleanSerializer::class)
    val allowCreditSales: Boolean = false,
    @SerialName("customer")
    val customerId: String? = null,
    @SerialName("naming_series")
    val namingSeries: String? = null,
    @SerialName("taxes_and_charges")
    val taxTemplateId: String? = null,
    @SerialName("write_off_account")
    val writeOffAccount: String? = null,
    @SerialName("write_off_cost_center")
    val writeOffCostCenter: String? = null,
    @SerialName("disabled")
    @Serializable(with = IntAsBooleanSerializer::class)
    val disabled: Boolean = false,
    @SerialName("payments")
    val payments: List<POSPaymentMethodDto> = emptyList()
)

package com.erpnext.pos.remoteSource.sdk.v2

data class DocTypeFields(val doctype: ERPDocType, val fields: List<String>)

enum class ERPDocType(val path: String) {
    Company("Company"),
    Employee("Employee"),
    SalesPerson("Sales Person"),
    Territory("Territory"),
    Item("Item"),
    Category("Item Group"),
    ItemPrice("Item Price"),
    User("User"),
    Address("Address"),
    Contacts("Contact"),
    Bin("Bin"),
    Customer("Customer"),
    CustomerContact("Contact"),
    Quotation("Quotation"),
    SalesOrder("Sales Order"),
    SalesInvoice("Sales Invoice"),
    PaymentEntry("Payment Entry"),
    DeliveryNote("Delivery Note"),
    PricingRule("Pricing Rule"),
    PurchaseInvoice("Purchase Invoice"),
    StockEntry("Stock Entry"),
    POSProfile("POS Profile"),
    POSProfileDetails("POS Profile"),
    POSOpeningEntry("POS Opening Entry"),
    POSClosingEntry("POS Closing Entry"),
    PaymentTerm("Payment Term")
}

val fields: List<DocTypeFields> = listOf(
    DocTypeFields(
        ERPDocType.Company,
        listOf(
            "name",
            "company_name",
            "abbr",
            "default_currency",
            "country",
            "domain",
            "tax_id",
            "is_group",
            "parent_company",
            "default_letter_head",
            "company_logo",
            ""
        )
    ),
    DocTypeFields(
        ERPDocType.Employee,
        listOf("name", "employee_name", "user_id", "status", "company")
    ),
    DocTypeFields(
        ERPDocType.SalesPerson,
        listOf("name", "sales_person_name", "employee", "is_group", "parent_sales_person")
    ),
    DocTypeFields(
        ERPDocType.Territory,
        listOf(
            "name",
            "territory_name",
            "is_group",
            "parent_territory",
            "territory_manager"
        )
    ),
    DocTypeFields(
        ERPDocType.Customer,
        listOf("name", "customer_name", "customer_type", "customer_group", "territory", "credit_limits", "payment_terms", "default_price_list", "default_currency", "primary_address", "mobile_no", "disabled")
    ),
    DocTypeFields(
        ERPDocType.SalesInvoice,
        listOf("name", "posting_date", "due_date", "grand_total", "outstanding_amount", "status", )
    ),
    DocTypeFields(
        ERPDocType.Address,
        listOf("name", "address_title", "address_line1", "address_line2", "city", "county", "state", "country", "pincode", "is_primary_address", "is_shipping_address", "disabled", "links")
    ),


    DocTypeFields(
        ERPDocType.Item,
        listOf(
            "item_code",
            "item_name",
            "item_group",
            "description",
            "brand",
            "image",
            "disabled",
            "barcodes",
            "stock_uom",
            "sales_uom",
            "standard_rate",
            "is_stock_item",
            "is_sales_item",
            "allow_negative_stock"
        )
    ),
    DocTypeFields(
        ERPDocType.Category,
        listOf("name", "item_group_name", "parent_item_group", "is_group")
    ),
    DocTypeFields(
        ERPDocType.POSProfile, listOf("name", "company", "currency")
    ),
    DocTypeFields(
        doctype = ERPDocType.POSProfileDetails,
        fields = listOf(
            "name",
            "warehouse",
            "route",
            "currency",
            "payments",
            "selling_price_list",
            "cost_center",
            "allow_negative_stock",
            "update_stock",
            "allow_credit_sales",
            "customer",
            "naming_series",
            "taxes_and_charges",
            "write_off_account",
            "write_off_cost_center",
            "disabled"
        )
    ),
    DocTypeFields(
        ERPDocType.User,
        listOf(
            "name",
            "email",
            "full_name",
            "enabled",
            "user_type"
        )
    ),
    DocTypeFields(
        ERPDocType.Bin,
        listOf(
            "item_code",
            "warehouse",
            "actual_qty",
            "projected_qty",
            "reserved_qty",
            "stock_uom",
            "valuation_rate"
        )
    ),
    DocTypeFields(
        ERPDocType.ItemPrice,
        listOf("name", "item_code", "uom", "price_list", "price_list_rate", "selling", "currency")
    ),
    DocTypeFields(
        ERPDocType.Customer,
        listOf(
            "name",
            "customer_name",
            "territory",
            "mobile_no",
            "customer_type",
            "disabled",
            "credit_limits.credit_limit",
            "credit_limits.company"
        )
    ),
    DocTypeFields(
        ERPDocType.SalesInvoice,
        listOf(
            "name",
            "customer",
            "company",
            "customer_name",
            "posting_date",
            "due_date",
            "status",
            "outstanding_amount",
            "grand_total",
            "paid_amount",
            "net_total",
            "is_pos",
            "pos_profile",
            "docstatus",
            "route",
            "territory",
            "contact_display",
            "contact_mobile",
            "party_account_currency",
        )
    ),
    DocTypeFields(
        ERPDocType.Quotation,
        listOf(
            "name",
            "transaction_date",
            "valid_till",
            "company",
            "party_name",
            "customer_name",
            "territory",
            "status",
            "price_list_currency",
            "selling_price_list",
            "net_total",
            "grand_total"
        )
    ),
    DocTypeFields(
        ERPDocType.SalesOrder,
        listOf(
            "name",
            "transaction_date",
            "delivery_date",
            "company",
            "customer",
            "customer_name",
            "territory",
            "status",
            "delivery_status",
            "billing_status",
            "price_list_currency",
            "selling_price_list",
            "net_total",
            "grand_total"
        )
    ),
    DocTypeFields(
        ERPDocType.PaymentEntry,
        listOf(
            "name",
            "posting_date",
            "company",
            "territory",
            "payment_type",
            "mode_of_payment",
            "party_type",
            "party",
            "paid_amount",
            "received_amount",
            "unallocated_amount"
        )
    ),
    DocTypeFields(
        ERPDocType.DeliveryNote,
        listOf(
            "name",
            "posting_date",
            "company",
            "customer",
            "customer_name",
            "territory",
            "status",
            "set_warehouse"
        )
    ),
    DocTypeFields(
        ERPDocType.PricingRule,
        listOf(
            "name",
            "priority",
            "condition",
            "territory",
            "for_price_list",
            "other_item_code",
            "other_item_group",
            "valid_from",
            "valid_upto"
        )
    ),
    DocTypeFields(
        ERPDocType.CustomerContact,
        listOf("phone", "mobile_no", "email_id")
    ),
    DocTypeFields(
        ERPDocType.PaymentTerm,
        listOf(
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
        )
    )
)

fun ERPDocType.getFields(): List<String> {
    val f = fields.first { it.doctype.path == this.path }
    return f.fields
}

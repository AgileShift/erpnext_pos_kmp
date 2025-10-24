package com.erpnext.pos.remoteSource.sdk

data class DocTypeFields(val doctype: ERPDocType, val fields: List<String>)

enum class ERPDocType(val path: String) {
    Item("Item"),
    Category("Item Group"),
    ItemPrice("Item Price"),
    User("User"),
    Bin("Bin"),
    Customer("Customer"),
    CustomerContact("Contact"),
    SalesInvoice("Sales Invoice"),
    PurchaseInvoice("Purchase Invoice"),
    StockEntry("Stock Entry"),
    POSProfile("POS Profile"),
    POSOpeningEntry("POS Profile Entry")
}

val fields: List<DocTypeFields> = listOf(
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
            "standard_rate",
            "is_stock_item",
            "is_sales_item"  // Correcciones
        )
    ),
    DocTypeFields(
        ERPDocType.Category, listOf("name")
    ),
    DocTypeFields(
        ERPDocType.POSProfile, listOf(
            "name", "company", "route",
            "customer", "disabled", "currency", "warehouse", "country"
        )
    ),
    DocTypeFields(
        ERPDocType.User,
        listOf(
            "name",
            "first_name",
            "last_name",
            "username",
            "language",
            "full_name"
        )
    ),
    DocTypeFields(
        ERPDocType.Bin,
        listOf(
            "item_code",
            "warehouse",
            "actual_qty",
            "projected_qty",
            "stock_uom",
            "valuation_rate"
        )
    ),
    DocTypeFields(
        ERPDocType.ItemPrice,
        listOf("item_code", "uom", "price_list", "price_list_rate", "selling", "currency")
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
            "contact_display",
            "contact_mobile",
            "party_account_currency",
        )
    ),
    DocTypeFields(
        ERPDocType.CustomerContact,
        listOf("phone", "mobile_no", "email_id")
    )
)

fun ERPDocType.getFields(): List<String> {
    val f = fields.first { it.doctype.path == this.path }
    return f.fields
}
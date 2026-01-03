package com.erpnext.pos.remoteSource.sdk

data class DocTypeFields(val doctype: ERPDocType, val fields: List<String>)

enum class ERPDocType(val path: String) {
    Item("Item"),
    PaymentEntry("Payment Entry"),
    ModeOfPayment("Mode of Payment"),
    Account("Account"),
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
    POSProfileDetails("POS Profile Detail"),
    POSOpeningEntry("POS Opening Entry"),
    POSClosingEntry("POS Closing Entry"),
    PaymentTerm("Payment Term"),
    DeliveryCharges("Delivery Charges")
}

val fields: List<DocTypeFields> = listOf(
    DocTypeFields(
        ERPDocType.ModeOfPayment,
        listOf("name", "mode_of_payment", "enabled")
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
            "standard_rate",
            "is_stock_item",
            "is_sales_item"  // Correcciones
        )
    ),
    DocTypeFields(
        ERPDocType.Category, listOf("name")
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
            "country",
            "company",
            "currency",
            "income_account",
            "expense_account",
            "payments",
            "branch",
            "applyDiscountOn",
            "cost_center",
            "selling_price_list"
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
            "credit_limits.company",
            "credit_limits.bypass_credit_limit_check"
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
            "contact_display",
            "contact_mobile",
            "party_account_currency",
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
    ),
    DocTypeFields(
        ERPDocType.DeliveryCharges,
        listOf(
            "label",
            "default_rate"
        )
    )
)

fun ERPDocType.getFields(): List<String> {
    val f = fields.first { it.doctype.path == this.path }
    return f.fields
}

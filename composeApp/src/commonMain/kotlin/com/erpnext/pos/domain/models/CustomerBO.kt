package com.erpnext.pos.domain.models

data class CustomerBO(
    val name: String,
    val customerName: String,
    val territory: String? = null,
    val mobileNo: String? = null,
    val email: String? = null,
    val customerType: String,
    val image: String? = null,
    val state: String? = null,
    val creditLimit: Double? = 0.0,  // BO version
    val totalPendingAmount: Double? = 0.0,
    val address: String? = "",  // Combined from child, e.g., addressLine1 + city
    val currentBalance: Double? = 0.0,  // total_outstanding
    val pendingInvoices: Int? = 0,  // total monto pendiente
    val availableCredit: Double? = 0.0, // credit_limit - currentBalance
    val currency: String = "NIO",
    val partyAccountCurrency: String? = null,
    val receivableAccount: String? = null,
    val receivableAccountCurrency: String? = null,
    val lastSyncedAt: Long? = null,
)

package com.erpnext.pos.domain.models

data class CustomerBO(
    val name: String,
    val customerName: String,
    val territory: String? = null,
    val mobileNo: String?,
    val customerType: String,
    val creditLimit: List<CreditLimit> = emptyList(),  // BO version
    val totalPendingAmount: Double = 0.0,
    val address: String? = "",  // Combined from child, e.g., addressLine1 + city
    val currentBalance: Double = 0.0,  // total_outstanding
    val pendingInvoices: Int = 0,  // total monto pendiente
    val availableCredit: Double = 0.0  // credit_limit - currentBalance
)

data class CreditLimit(
    val creditLimit: Double,
    val company: String? = null
)
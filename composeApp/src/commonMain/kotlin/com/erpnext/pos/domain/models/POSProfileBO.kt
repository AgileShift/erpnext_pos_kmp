package com.erpnext.pos.domain.models

data class POSProfileBO(
    val name: String = "",
    val warehouse: String = "",
    val country: String = "",
    val disabled: Boolean = false,
    val company: String = "",
    val route: String? = "",
    val incomeAccount: String = "",
    val expenseAccount: String = "",
    val costCenter: String = "",
    val sellingPriceList: String = "",
    val applyDiscountOn: String = "",
    val branch: String = "",
    val currency: String = "",
    val paymentModes: List<PaymentModesBO>
)

data class POSProfileSimpleBO(
    val name: String = "",
    val company: String = "",
    val currency: String = "",
    val paymentModes: List<PaymentModesBO>
)

data class PaymentModesBO(
    val name: String,
    val modeOfPayment: String
)
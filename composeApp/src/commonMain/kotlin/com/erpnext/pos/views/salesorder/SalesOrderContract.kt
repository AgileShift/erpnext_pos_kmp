package com.erpnext.pos.views.salesorder

sealed class SalesOrderState {
    data object Loading : SalesOrderState()
    data object Ready : SalesOrderState()
    data class Error(val message: String) : SalesOrderState()
}

data class SalesOrderAction(
    val onBack: () -> Unit = {},
    val onRefresh: () -> Unit = {}
)

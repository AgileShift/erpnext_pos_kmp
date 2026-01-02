package com.erpnext.pos.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Description
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavRoute(
    val path: String,
    val title: String,
    val icon: ImageVector
) {
    object Splash : NavRoute("splash", "Splash", Icons.Filled.Home)
    object Login : NavRoute("login", "Inicio de sesion", Icons.Filled.Home)
    object Home : NavRoute("home", "Inicio", Icons.Filled.Home)
    object Inventory : NavRoute("inventory", "Inventario", Icons.Filled.Inventory2)
    object Billing : NavRoute("sale", "Ventas", Icons.Filled.PointOfSale)
    object Customer : NavRoute("customer", "Clientes", Icons.Filled.People)
    object Credits : NavRoute("credits", "Créditos", Icons.Filled.Receipt)
    object Quotation : NavRoute("quotation", "Cotizaciones", Icons.Filled.Description)
    object SalesOrder : NavRoute("sales-order", "Orden de venta", Icons.Filled.ShoppingCart)
    object DeliveryNote : NavRoute("delivery-note", "Nota de entrega", Icons.Filled.LocalShipping)
    object Settings : NavRoute("settings", "", Icons.Filled.Settings)
    data class PaymentEntry(val invoiceId: String? = null) : NavRoute(
        path = if (invoiceId.isNullOrBlank()) {
            "payment-entry"
        } else {
            "payment-entry?invoiceId=$invoiceId"
        },
        title = "Payment Entry",
        icon = Icons.Filled.Payments
    )

    object NavigateUp : NavRoute("navigate-up", "Navigate Up", Icons.Filled.Home)
}

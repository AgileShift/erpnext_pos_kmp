package com.erpnext.pos.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Description
import androidx.compose.ui.graphics.vector.ImageVector
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.localization.AppStringsFactory
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.views.reconciliation.ReconciliationMode

val strings = AppStringsFactory.forLanguage(AppLanguage.Spanish)
val menuStrings = strings.navigation
sealed class NavRoute(
    val path: String,
    val title: String,
    val icon: ImageVector
) {

    object Splash : NavRoute("splash", "Splash", Icons.Filled.Home)
    object Login : NavRoute("login", "Inicio de sesion", Icons.Filled.Home)

    object Home : NavRoute("home", menuStrings.home, Icons.Filled.Home)
    object Inventory : NavRoute("inventory", menuStrings.inventory, Icons.Filled.Inventory2)
    object Billing : NavRoute("sale-lab", menuStrings.billing, Icons.Filled.PointOfSale)
    object Customer : NavRoute("customer", menuStrings.customer, Icons.Filled.People)
    object Credits : NavRoute("credits", menuStrings.credits, Icons.Filled.Receipt)
    object Quotation : NavRoute("quotation", menuStrings.quotations, Icons.Filled.Description)
    object SalesOrder : NavRoute("sales-order", "Orden de venta", Icons.Filled.ShoppingCart)
    object DeliveryNote : NavRoute("delivery-note", "Nota de entrega", Icons.Filled.LocalShipping)
    data class Reconciliation(
        val mode: ReconciliationMode = ReconciliationMode.Close
    ) : NavRoute(
        path = "reconciliation?mode=${mode.value}",
        title = "Reconciliation",
        icon = Icons.Filled.AccountBalance
    ) {
        companion object {
            const val ROUTE = "reconciliation?mode={mode}"
        }
    }

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

package com.erpnext.pos.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.runtime.Composable
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

    object Splash : NavRoute("splash", "", Icons.Filled.Home)
    object Login : NavRoute("login", "", Icons.Filled.Home)

    object Home : NavRoute("home", menuStrings.home, Icons.Filled.Home)
    object Inventory : NavRoute("inventory", menuStrings.inventory, Icons.Filled.Inventory2)
    object Billing : NavRoute("sale-lab", menuStrings.billing, Icons.Filled.PointOfSale)
    object Customer : NavRoute("customer", menuStrings.customer, Icons.Filled.People)
    object Credits : NavRoute("credits", menuStrings.credits, Icons.Filled.Receipt)
    object Quotation : NavRoute("quotation", menuStrings.quotations, Icons.Filled.Description)
    object SalesOrder : NavRoute("sales-order", menuStrings.salesOrder, Icons.Filled.ShoppingCart)
    object DeliveryNote :
        NavRoute("delivery-note", menuStrings.deliveryNote, Icons.Filled.LocalShipping)

    object Activity : NavRoute("activity", "", Icons.Filled.Notifications)
    data class Reconciliation(
        val mode: ReconciliationMode = ReconciliationMode.Close
    ) : NavRoute(
        path = "reconciliation?mode=${mode.value}",
        title = strings.navigation.reconciliation,
        icon = Icons.Filled.AccountBalance
    ) {
        companion object {
            const val ROUTE = "reconciliation?mode={mode}"
        }
    }

    object Settings : NavRoute("settings", "", Icons.Filled.Settings)
    object Expenses : NavRoute(
        path = "expenses",
        title = strings.navigation.expenses,
        icon = Icons.Filled.Payments
    )

    data class PaymentEntry(val invoiceId: String? = null) : NavRoute(
        path = if (invoiceId.isNullOrBlank()) {
            "payment-entry?invoiceId=&entryType=pay"
        } else {
            "payment-entry?invoiceId=$invoiceId&entryType=receive"
        },
        title = strings.navigation.paymentEntry,
        icon = Icons.Filled.Payments
    )

    object InternalTransfer : NavRoute(
        path = "payment-entry?invoiceId=&entryType=internal-transfer",
        title = "Transferencia interna",
        icon = Icons.Filled.SwapHoriz
    )

    object NavigateUp : NavRoute("navigate-up", "", Icons.Filled.Home)
}

@Composable
fun NavRoute.localizedTitle(): String {
    val strings = LocalAppStrings.current.navigation
    return when (this) {
        NavRoute.Home -> strings.home
        NavRoute.Inventory -> strings.inventory
        NavRoute.Billing -> strings.billing
        NavRoute.Customer -> strings.customer
        NavRoute.Credits -> strings.credits
        NavRoute.Quotation -> strings.quotations
        NavRoute.SalesOrder -> strings.salesOrder
        NavRoute.DeliveryNote -> strings.deliveryNote
        NavRoute.Activity -> "" // strings.activity
        NavRoute.Settings -> "" //strings.settings
        NavRoute.Expenses -> strings.expenses
        is NavRoute.Reconciliation -> strings.reconciliation
        is NavRoute.PaymentEntry -> strings.expenses
        NavRoute.InternalTransfer -> "Transferencia interna"
        NavRoute.Splash -> ""
        NavRoute.Login -> ""
        NavRoute.NavigateUp -> ""
    }
}

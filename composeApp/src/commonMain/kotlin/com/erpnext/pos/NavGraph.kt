package com.erpnext.pos

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.NavType
import androidx.savedstate.read
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.views.billing.BillingRoute
import com.erpnext.pos.views.billing.BillingLabRoute
import com.erpnext.pos.views.customer.CustomerRoute
import com.erpnext.pos.views.deliverynote.DeliveryNoteRoute
import com.erpnext.pos.views.home.HomeRoute
import com.erpnext.pos.views.inventory.InventoryRoute
import com.erpnext.pos.views.invoice.InvoiceRoute
import com.erpnext.pos.views.login.LoginRoute
import com.erpnext.pos.views.quotation.QuotationRoute
import com.erpnext.pos.views.salesorder.SalesOrderRoute
import com.erpnext.pos.views.settings.SettingsRoute
import com.erpnext.pos.views.splash.SplashRoute
import com.erpnext.pos.views.paymententry.PaymentEntryRoute
import com.erpnext.pos.views.reconciliation.ReconciliationRoute
import com.erpnext.pos.views.reconciliation.ReconciliationMode

@ExperimentalMaterial3Api
object NavGraph {

    @Composable
    fun Setup(
        navController: NavHostController,
        isExpandedScreen: Boolean
    ) {
        NavHost(navController, startDestination = NavRoute.Splash.path) {
            composable(NavRoute.Splash.path) {
                SplashRoute()
            }
            composable(NavRoute.Login.path) {
                LoginRoute()
            }
            composable(NavRoute.Home.path) {
                HomeRoute()
            }
            composable(NavRoute.Inventory.path) {
                InventoryRoute()
            }
            composable(NavRoute.Billing.path) {
                BillingRoute()
            }
            composable(NavRoute.BillingLab.path) {
                BillingLabRoute()
            }
            composable(NavRoute.Customer.path) {
                CustomerRoute()
            }
            composable(NavRoute.Credits.path) {
                InvoiceRoute()
            }
            composable(NavRoute.Quotation.path) {
                QuotationRoute()
            }
            composable(NavRoute.SalesOrder.path) {
                SalesOrderRoute()
            }
            composable(NavRoute.DeliveryNote.path) {
                DeliveryNoteRoute()
            }
            composable(
                route = NavRoute.Reconciliation.route,
                arguments = listOf(
                    navArgument("mode") {
                        type = NavType.StringType
                        defaultValue = ReconciliationMode.Review.value
                    }
                )
            ) { entry ->
                val mode = ReconciliationMode.from(
                    entry.arguments?.getString("mode")
                )
                ReconciliationRoute(mode)
            }
            composable(NavRoute.Settings.path) {
                SettingsRoute()
            }
            composable(
                route = "payment-entry?invoiceId={invoiceId}",
                arguments = listOf(
                    navArgument("invoiceId") {
                        defaultValue = ""
                        nullable = true
                    }
                )
            ) { entry ->
                PaymentEntryRoute(entry.arguments?.read { getString("invoiceId") })
            }
        }
    }
}

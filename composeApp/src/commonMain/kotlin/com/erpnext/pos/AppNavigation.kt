package com.erpnext.pos

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.getValue
import com.erpnext.pos.NavGraph.Setup
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.navigation.DesktopNavigationRail
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.navigation.v2.BottomBarWithCenterFab
import com.erpnext.pos.utils.view.SnackbarHost
import com.erpnext.pos.utils.view.rememberSnackbarController
import com.erpnext.pos.views.CashBoxManager
import org.koin.compose.koinInject

fun shouldShowBottomBar(currentRoute: String): Boolean {
    return currentRoute !in listOf(NavRoute.Login.path, NavRoute.Splash.path)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val snackbarController = rememberSnackbarController()

    val snackbar by snackbarController.snackbar.collectAsState()
    val cashBoxManager = koinInject<CashBoxManager>()

    LaunchedEffect(cashBoxManager) {
        cashBoxManager.initializeContext()
    }

    val visibleEntries by navController.visibleEntries.collectAsState()
    val currentRoute = visibleEntries.lastOrNull()?.destination?.route ?: ""
    val isDesktop = getPlatformName() == "Desktop"

    Scaffold(
        bottomBar = {
            if (!isDesktop && shouldShowBottomBar(currentRoute)) {
                BottomBarWithCenterFab(
                    snackbarController = snackbarController,
                    navController = navController,
                    contextProvider = cashBoxManager,
                    leftItems = listOf(NavRoute.Home, NavRoute.Inventory),
                    rightItems = listOf(NavRoute.Customer, NavRoute.Settings),
                    fabItem = NavRoute.Billing
                )
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (isDesktop && shouldShowBottomBar(currentRoute)) {
                DesktopNavigationRail(
                    navController = navController,
                    contextProvider = cashBoxManager
                )
            }
            Box(
                modifier = Modifier.weight(1f)
            ) {
                Setup(navController, false)

                SnackbarHost(
                    snackbar = snackbar,
                    onDismiss = snackbarController::dismiss
                )

                val navManager: NavigationManager = koinInject()
                LaunchedEffect(Unit) {
                    navManager.navigationEvents.collect { event ->
                        when (event) {
                            is NavRoute.Login -> navController.navigate(NavRoute.Login.path)
                            is NavRoute.Home -> navController.navigate(NavRoute.Home.path)
                            is NavRoute.Billing -> navController.navigate(NavRoute.Billing.path)
                            is NavRoute.Credits -> navController.navigate(NavRoute.Credits.path)
                            is NavRoute.Quotation -> navController.navigate(NavRoute.Quotation.path)
                            is NavRoute.SalesOrder -> navController.navigate(NavRoute.SalesOrder.path)
                            is NavRoute.DeliveryNote -> navController.navigate(NavRoute.DeliveryNote.path)
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

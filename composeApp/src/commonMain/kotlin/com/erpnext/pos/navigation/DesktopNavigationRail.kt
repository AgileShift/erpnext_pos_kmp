package com.erpnext.pos.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.erpnext.pos.views.CashBoxManager

@Composable
fun DesktopNavigationRail(
    navController: NavController,
    contextProvider: CashBoxManager
) {
    val isCashBoxOpen by contextProvider.cashboxState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoutePath = navBackStackEntry?.destination?.route

    val items = listOf(
        NavRoute.Home,
        NavRoute.Inventory,
        NavRoute.Billing,
        NavRoute.Customer,
        NavRoute.Credits
    )

    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 12.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { navRoute ->
                val isEnabled = when (navRoute) {
                    NavRoute.Home -> true
                    NavRoute.Inventory,
                    NavRoute.Billing,
                    NavRoute.Customer,
                    NavRoute.Credits -> isCashBoxOpen
                    else -> true
                }
                val isSelected = currentRoutePath == navRoute.path

                NavigationRailItem(
                    selected = isSelected,
                    onClick = {
                        if (isEnabled && currentRoutePath != navRoute.path) {
                            navController.navigate(navRoute.path) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    icon = {
                        androidx.compose.material3.Icon(
                            imageVector = navRoute.icon,
                            contentDescription = navRoute.title,
                            tint = if (isSelected && isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                                    .copy(alpha = if (isEnabled) 1f else 0.4f)
                            }
                        )
                    },
                    label = {
                        Text(
                            text = navRoute.title,
                            color = if (isSelected && isEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                                    .copy(alpha = if (isEnabled) 1f else 0.4f)
                            }
                        )
                    },
                    modifier = Modifier.alpha(if (isEnabled) 1f else 0.4f)
                )
            }
        }
    }
}

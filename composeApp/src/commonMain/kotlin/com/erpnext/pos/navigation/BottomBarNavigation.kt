package com.erpnext.pos.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.erpnext.pos.views.CashBoxManager

@Composable
fun BottomBarNavigation(
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
        NavRoute.Customer
    )

    BottomAppBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = contentColorFor(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { navRoute ->
                val isEnabled = when (navRoute) {
                    NavRoute.Home -> true
                    NavRoute.Inventory,
                    NavRoute.Billing,
                    NavRoute.Customer -> isCashBoxOpen
                    else -> true
                }
                val isSelected = currentRoutePath == navRoute.path

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .alpha(if (isEnabled) 1f else 0.4f) // Opacidad reducida para deshabilitados
                        .clickable(enabled = isEnabled) {
                            if (currentRoutePath != navRoute.path) {
                                navController.navigate(navRoute.path) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    Icon(
                        imageVector = navRoute.icon,
                        contentDescription = navRoute.title,
                        tint = if (isSelected && isEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isEnabled) 1f else 0.4f)
                    )
                    Text(
                        text = navRoute.title,
                        fontSize = 12.sp,
                        color = if (isSelected && isEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isEnabled) 1f else 0.4f)
                    )
                }
            }
        }
    }
}

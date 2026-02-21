package com.erpnext.pos.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.DpOffset
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.erpnext.pos.views.CashBoxManager

@Composable
fun DesktopNavigationRail(
    navController: NavController,
    contextProvider: CashBoxManager,
    activityBadgeCount: Int = 0
) {
    val isCashBoxOpen by contextProvider.cashboxState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoutePath = navBackStackEntry?.destination?.route

    val primaryItems = listOf(
        NavRoute.Home,
        NavRoute.Inventory,
        NavRoute.Billing,
        NavRoute.Customer,
        NavRoute.Expenses
    )
    val secondaryItems = listOf(NavRoute.Settings)

    NavigationRail(
        modifier = Modifier
            .fillMaxHeight()
            .padding(vertical = 12.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                primaryItems.forEach { navRoute ->
                    val isEnabled = when (navRoute) {
                        NavRoute.Home,
                        NavRoute.Settings -> true

                        NavRoute.Inventory,
                        NavRoute.Billing,
                        NavRoute.Customer,
                        NavRoute.Expenses -> isCashBoxOpen

                        else -> true
                    }
                    NavigationRailEntry(
                        navController = navController,
                        navRoute = navRoute,
                        isEnabled = isEnabled,
                        isSelected = if (navRoute == NavRoute.Expenses) {
                            currentRoutePath?.startsWith("payment-entry") == true
                        } else {
                            currentRoutePath == navRoute.path
                        }
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityNavigationRailEntry(
                    navController = navController,
                    isSelected = currentRoutePath == NavRoute.Activity.path,
                    activityBadgeCount = activityBadgeCount
                )
                secondaryItems.forEach { navRoute ->
                    NavigationRailEntry(
                        navController = navController,
                        navRoute = navRoute,
                        isEnabled = true,
                        isSelected = currentRoutePath == navRoute.path
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityNavigationRailEntry(
    navController: NavController,
    isSelected: Boolean,
    activityBadgeCount: Int
) {
    val hasBadge = activityBadgeCount > 0
    val badgeText = if (activityBadgeCount > 99) "99+" else activityBadgeCount.toString()
    val title = NavRoute.Activity.localizedTitle()
    NavigationRailItem(
        selected = isSelected,
        onClick = {
            if (navController.currentDestination?.route != NavRoute.Activity.path) {
                navController.navigate(NavRoute.Activity.path) {
                    launchSingleTop = true
                }
            }
        },
        icon = {
            BadgedBox(
                badge = {
                    if (hasBadge) {
                        Badge {
                            Text(text = badgeText)
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = title,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        /*label = {
            Text(
                text = NavRoute.Activity.title,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }*/
    )
}

@Composable
private fun NavigationRailEntry(
    navController: NavController,
    navRoute: NavRoute,
    isEnabled: Boolean,
    isSelected: Boolean
) {
    val title = navRoute.localizedTitle()
    var expensesMenuExpanded by remember { mutableStateOf(false) }
    if (navRoute == NavRoute.Expenses) {
        val menuBump by animateFloatAsState(
            targetValue = if (expensesMenuExpanded) 1.08f else if (isSelected) 1.06f else 1f,
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioNoBouncy
            ),
            label = "railExpensesBump"
        )
        Box {
            NavigationRailItem(
                selected = isSelected,
                onClick = {
                    if (isEnabled) expensesMenuExpanded = true
                },
                icon = {
                    Icon(
                        modifier = Modifier.graphicsLayer {
                            scaleX = menuBump
                            scaleY = menuBump
                        },
                        imageVector = navRoute.icon,
                        contentDescription = title,
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
                        text = title,
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
            DropdownMenu(
                expanded = expensesMenuExpanded,
                onDismissRequest = { expensesMenuExpanded = false },
                offset = DpOffset(x = 74.dp, y = (-6).dp),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                tonalElevation = 8.dp,
                shadowElevation = 18.dp,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                DropdownMenuItem(
                    text = { Text("Gastos") },
                    leadingIcon = {
                        Icon(
                            imageVector = NavRoute.Expenses.icon,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        expensesMenuExpanded = false
                        if (navController.currentDestination?.route != NavRoute.PaymentEntry().path) {
                            navController.navigate(NavRoute.PaymentEntry().path) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
                DropdownMenuItem(
                    text = { Text("Transferencia Interna") },
                    leadingIcon = {
                        Icon(
                            imageVector = NavRoute.InternalTransfer.icon,
                            contentDescription = null
                        )
                    },
                    onClick = {
                        expensesMenuExpanded = false
                        if (navController.currentDestination?.route != NavRoute.InternalTransfer.path) {
                            navController.navigate(NavRoute.InternalTransfer.path) {
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        }
        return
    }
    NavigationRailItem(
        selected = isSelected,
        onClick = {
            if (isEnabled && navController.currentDestination?.route != navRoute.path) {
                navController.navigate(navRoute.path) {
                    launchSingleTop = true
                }
            }
        },
        icon = {
            Icon(
                imageVector = navRoute.icon,
                contentDescription = title,
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
                text = title,
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

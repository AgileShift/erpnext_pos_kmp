package com.erpnext.pos.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.erpnext.pos.views.CashBoxManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.utils.view.rememberSnackbarController

// RULES
private fun NavRoute.isEnabled(isCashBoxOpen: Boolean): Boolean = when (this) {
    NavRoute.Home -> true
    NavRoute.Inventory, NavRoute.Billing, NavRoute.Customer, NavRoute.Credits -> isCashBoxOpen

    else -> true
}

// NAV UTILITY
fun safeNavigate(navController: NavController, route: String) {
    if (navController.currentDestination?.route == route) return
    navController.navigate(route) {
        launchSingleTop = true
    }
}

@Composable
fun BottomBarWithCenterFab(
    snackbarController: SnackbarController,
    navController: NavController,
    contextProvider: CashBoxManager,
    leftItems: List<NavRoute>,
    rightItems: List<NavRoute>,
    fabItem: NavRoute,
    modifier: Modifier = Modifier
) {
    val isCashBoxOpen by contextProvider.cashboxState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isDektop = getPlatformName() == "Desktop"
    val navBarSize = 76.dp
    val fabSize = 64.dp
    val barMaxWidth = if (isDektop) 720.dp else Dp.Unspecified
    val barHorizontalPadding = if (isDektop) 24.dp else 14.dp
    val barVerticalPadding = if (isDektop) 12.dp else 8.dp

    Box(
        modifier = modifier
            //.shadow(elevation = 4.dp, RoundedCornerShape(24.dp))
            .fillMaxWidth()
            .padding(vertical = barVerticalPadding, horizontal = barHorizontalPadding)
            .widthIn(max = barMaxWidth)
            .height(navBarSize)
    ) {
        // Barra
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(navBarSize)
                .padding(vertical = 8.dp, horizontal = 14.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Row(
                modifier = Modifier
                    .height(64.dp)
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    leftItems.forEach {
                        AnimatedBottomNavItem(
                            it,
                            navController,
                            currentRoute,
                            it.isEnabled(isCashBoxOpen)
                        )
                    }
                }

                Spacer(modifier = Modifier.width((fabSize - 14.dp)))

                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    rightItems.forEach {
                        AnimatedBottomNavItem(
                            it,
                            navController,
                            current = currentRoute,
                            it.isEnabled(isCashBoxOpen)
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                if (isCashBoxOpen) safeNavigate(
                    navController,
                    fabItem.path
                ) else snackbarController.show(
                    "Necesitas abrir caja antes de operar",
                    SnackbarType.Error,
                    position = SnackbarPosition.Top
                )
            },
            modifier = Modifier
                .size(fabSize)
                .align(Alignment.TopCenter)
                .offset(y = (-16).dp),
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(12.dp),
            containerColor = if (isCashBoxOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.4f
            ),
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(fabItem.icon, fabItem.title)
        }
    }
}

@Composable
fun NavItem(
    navController: NavController,
    item: NavRoute,
    currentPath: String?,
    enabled: Boolean = true
) {
    val selected = currentPath == item.path
    val colors = MaterialTheme.colorScheme

    val iconTint = when {
        !enabled -> colors.onSurfaceVariant.copy(alpha = 0.4f)
        selected -> colors.primary
        else -> colors.onSurfaceVariant
    }

    Column(
        modifier = Modifier
            .height(64.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                safeNavigate(navController, item.path)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .then(
                    if (selected) Modifier
                        .background(
                            colors.primary.copy(alpha = 0.12f),
                            CircleShape
                        )
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = iconTint
            )
        }

        Text(
            text = item.title,
            color = iconTint,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(6.dp))
    }
}

@Composable
fun AnimatedBottomNavItem(
    item: NavRoute,
    navController: NavController,
    current: String?,
    enabled: Boolean = true
) {
    val colors = MaterialTheme.colorScheme

    val interactionSource = remember { MutableInteractionSource() }
    val selected = current == item.path

    // 🎨 Animaciones
    val iconColor by animateColorAsState(
        targetValue = when {
            !enabled -> colors.onSurfaceVariant.copy(alpha = 0.4f)
            selected -> colors.primary
            else -> colors.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = 120),
        label = "iconColor"
    )

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.15f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "iconScale"
    )

    val textColor by animateColorAsState(
        targetValue = iconColor,
        label = "textColor"
    )

    Column(
        modifier = Modifier
            .height(64.dp)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null
            ) {
                safeNavigate(navController, item.path)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 🔥 Icon container con glow simulado
        Box(
            modifier = Modifier
                .graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                }
                .size(34.dp)
                .background(
                    if (selected)
                        colors.primary.copy(alpha = 0.12f)
                    else
                        Color.Transparent,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                tint = iconColor
            )
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1
        )

        Spacer(modifier = Modifier.height(7.dp))
    }
}

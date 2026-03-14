package com.erpnext.pos.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.views.CashBoxManager

// RULES
private fun NavRoute.isEnabled(isCashBoxOpen: Boolean): Boolean =
    when (this) {
      NavRoute.Home -> true
      NavRoute.Inventory,
      NavRoute.Billing,
      NavRoute.Customer,
      NavRoute.Credits,
      NavRoute.Expenses,
      is NavRoute.PaymentEntry,
      NavRoute.InternalTransfer -> isCashBoxOpen

      else -> true
    }

// NAV UTILITY
fun safeNavigate(navController: NavController, route: String) {
  if (navController.currentDestination?.route == route) return
  navController.navigate(route) { launchSingleTop = true }
}

@Composable
fun BottomBarWithCenterFab(
    snackbarController: SnackbarController,
    navController: NavController,
    contextProvider: CashBoxManager,
    leftItems: List<NavRoute>,
    rightItems: List<NavRoute>,
    fabItem: NavRoute,
    modifier: Modifier = Modifier,
) {
  val isCashBoxOpen by contextProvider.cashboxState.collectAsState()
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val currentRoute = navBackStackEntry?.destination?.route
  var expensesMenuExpanded by remember { mutableStateOf(false) }
  var moreMenuExpanded by remember { mutableStateOf(false) }

  val isDektop = getPlatformName() == "Desktop"
  BoxWithConstraints(
      modifier =
          modifier
              .fillMaxWidth()
  ) {
    val compactBar = !isDektop && (maxWidth < 430.dp || maxHeight < 500.dp)
    val navBarSize = if (compactBar) 66.dp else 76.dp
    val fabSize = if (compactBar) 54.dp else 64.dp
    val barMaxWidth = if (isDektop) 720.dp else Dp.Unspecified
    val barHorizontalPadding = if (isDektop) 24.dp else if (compactBar) 6.dp else 14.dp
    val barVerticalPadding = if (isDektop) 12.dp else 8.dp
    val compactOverflowItems = rightItems.filter { it == NavRoute.Activity || it == NavRoute.Settings }

    if (compactBar) {
      val compactLeftItems = listOf(NavRoute.Home, NavRoute.Inventory, NavRoute.Customer, NavRoute.Expenses)
      Surface(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(vertical = barVerticalPadding, horizontal = barHorizontalPadding)
                  .widthIn(max = barMaxWidth)
                  .height(navBarSize)
                  .align(Alignment.BottomCenter),
          shape = RoundedCornerShape(22.dp),
          tonalElevation = 8.dp,
          shadowElevation = 12.dp,
          color = MaterialTheme.colorScheme.surface,
      ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            compactLeftItems.forEach { item ->
              if (item == NavRoute.Expenses) {
                ExpensesBottomNavItem(
                    current = currentRoute,
                    enabled = item.isEnabled(isCashBoxOpen),
                    showLabel = false,
                    expanded = expensesMenuExpanded,
                    onExpandedChange = { expensesMenuExpanded = it },
                    onGoExpenses = {
                      expensesMenuExpanded = false
                      safeNavigate(navController, NavRoute.PaymentEntry().path)
                    },
                    onGoInternalTransfer = {
                      expensesMenuExpanded = false
                      safeNavigate(navController, NavRoute.InternalTransfer.path)
                    },
                )
              } else {
                AnimatedBottomNavItem(
                    item = item,
                    navController = navController,
                    current = currentRoute,
                    enabled = item.isEnabled(isCashBoxOpen),
                    showLabel = false,
                )
              }
            }
          }
          Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BillingBottomNavItem(
                current = currentRoute,
                enabled = fabItem.isEnabled(isCashBoxOpen),
                onClick = {
                  if (isCashBoxOpen) safeNavigate(navController, fabItem.path)
                  else
                      snackbarController.show(
                          "Necesitas abrir caja antes de operar",
                          SnackbarType.Error,
                          position = SnackbarPosition.Top,
                      )
                },
            )
            if (compactOverflowItems.isNotEmpty()) {
              MoreBottomNavItem(
                  expanded = moreMenuExpanded,
                  onExpandedChange = { moreMenuExpanded = it },
                  onGoActivity = {
                    moreMenuExpanded = false
                    safeNavigate(navController, NavRoute.Activity.path)
                  },
                  onGoSettings = {
                    moreMenuExpanded = false
                    safeNavigate(navController, NavRoute.Settings.path)
                  },
                  selected =
                      currentRoute == NavRoute.Activity.path ||
                          currentRoute == NavRoute.Settings.path,
              )
            }
          }
        }
      }
    } else {
      Surface(
          modifier =
              Modifier.fillMaxWidth()
                  .padding(vertical = barVerticalPadding, horizontal = barHorizontalPadding)
                  .widthIn(max = barMaxWidth)
                  .height(navBarSize)
                  .align(Alignment.BottomCenter),
          shape = RoundedCornerShape(24.dp),
          tonalElevation = 8.dp,
          shadowElevation = 12.dp,
          color = MaterialTheme.colorScheme.surface,
      ) {
        Row(
            modifier = Modifier.height(64.dp).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            leftItems.forEach {
              AnimatedBottomNavItem(
                  it,
                  navController,
                  currentRoute,
                  it.isEnabled(isCashBoxOpen),
                  showLabel = currentRoute == it.path,
              )
            }
          }

          Spacer(modifier = Modifier.width(fabSize - 14.dp))

          Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            rightItems.forEach {
              if (it == NavRoute.Expenses) {
                ExpensesBottomNavItem(
                    current = currentRoute,
                    enabled = it.isEnabled(isCashBoxOpen),
                    showLabel = currentRoute?.startsWith("payment-entry") == true,
                    expanded = expensesMenuExpanded,
                    onExpandedChange = { expensesMenuExpanded = it },
                    onGoExpenses = {
                      expensesMenuExpanded = false
                      safeNavigate(navController, NavRoute.PaymentEntry().path)
                    },
                    onGoInternalTransfer = {
                      expensesMenuExpanded = false
                      safeNavigate(navController, NavRoute.InternalTransfer.path)
                    },
                )
              } else {
                AnimatedBottomNavItem(
                    it,
                    navController,
                    current = currentRoute,
                    it.isEnabled(isCashBoxOpen),
                    showLabel = currentRoute == it.path,
                )
              }
            }
          }
        }
      }

      FloatingActionButton(
          onClick = {
            if (isCashBoxOpen) safeNavigate(navController, fabItem.path)
            else
                snackbarController.show(
                    "Necesitas abrir caja antes de operar",
                    SnackbarType.Error,
                    position = SnackbarPosition.Top,
                )
          },
          modifier = Modifier.size(fabSize).align(Alignment.TopCenter).offset(y = (-16).dp),
          shape = CircleShape,
          elevation = FloatingActionButtonDefaults.elevation(12.dp),
          containerColor =
              if (isCashBoxOpen) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
          contentColor = MaterialTheme.colorScheme.onPrimary,
      ) {
        Icon(fabItem.icon, fabItem.localizedTitle())
      }
    }
  }
}

@Composable
private fun ExpensesBottomNavItem(
    current: String?,
    enabled: Boolean,
    showLabel: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onGoExpenses: () -> Unit,
    onGoInternalTransfer: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val selected = current?.startsWith("payment-entry") == true
  val title = NavRoute.Expenses.localizedTitle()
  val interactionSource = remember { MutableInteractionSource() }
  val menuBump by
      animateFloatAsState(
          targetValue = if (expanded) 1.08f else if (selected) 1.15f else 1f,
          animationSpec =
              spring(
                  stiffness = Spring.StiffnessMediumLow,
                  dampingRatio = Spring.DampingRatioNoBouncy,
              ),
          label = "expensesMenuBump",
      )

  val iconColor by
      animateColorAsState(
          targetValue =
              when {
                !enabled -> colors.onSurfaceVariant.copy(alpha = 0.4f)
                selected -> colors.primary
                else -> colors.onSurfaceVariant
              },
          animationSpec = tween(durationMillis = 120),
          label = "expensesIconColor",
      )
  val iconScale by
      animateFloatAsState(
          targetValue = menuBump,
          animationSpec =
              spring(
                  stiffness = Spring.StiffnessMediumLow,
                  dampingRatio = Spring.DampingRatioNoBouncy,
              ),
          label = "expensesIconScale",
      )
  val textColor by animateColorAsState(targetValue = iconColor, label = "expensesTextColor")

  Box {
    Column(
        modifier =
            Modifier.height(64.dp).clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
            ) {
              onExpandedChange(!expanded)
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
      Box(
          modifier =
              Modifier.graphicsLayer {
                    scaleX = iconScale
                    scaleY = iconScale
                  }
                  .size(34.dp)
                  .background(
                      if (selected) colors.primary.copy(alpha = 0.12f) else Color.Transparent,
                      CircleShape,
                  ),
          contentAlignment = Alignment.Center,
      ) {
        Icon(imageVector = NavRoute.Expenses.icon, contentDescription = title, tint = iconColor)
      }

      if (showLabel) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(7.dp))
      }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onExpandedChange(false) },
        offset = DpOffset(x = 82.dp, y = (-6).dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        shadowElevation = 18.dp,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
      DropdownMenuItem(
          text = { Text("Gastos") },
          leadingIcon = { Icon(imageVector = NavRoute.Expenses.icon, contentDescription = null) },
          onClick = onGoExpenses,
      )
      DropdownMenuItem(
          text = { Text("Transferencia Interna") },
          leadingIcon = {
            Icon(imageVector = NavRoute.InternalTransfer.icon, contentDescription = null)
          },
          onClick = onGoInternalTransfer,
      )
    }
  }
}

@Composable
private fun MoreBottomNavItem(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onGoActivity: () -> Unit,
    onGoSettings: () -> Unit,
    selected: Boolean,
) {
  val colors = MaterialTheme.colorScheme
  val interactionSource = remember { MutableInteractionSource() }
  val tint by
      animateColorAsState(
          targetValue =
              if (selected) colors.primary else colors.onSurfaceVariant,
          animationSpec = tween(durationMillis = 120),
          label = "moreNavTint",
      )

  Box {
    Column(
        modifier =
            Modifier.height(64.dp).clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onExpandedChange(!expanded) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
      Box(
          modifier =
              Modifier.size(34.dp)
                  .background(
                      if (selected) colors.primary.copy(alpha = 0.12f) else Color.Transparent,
                      CircleShape,
                  ),
          contentAlignment = Alignment.Center,
      ) {
        Icon(
            imageVector = Icons.Outlined.MoreVert,
            contentDescription = "Más",
            tint = tint,
        )
      }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { onExpandedChange(false) },
        offset = DpOffset(x = 12.dp, y = (-8).dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 8.dp,
        shadowElevation = 18.dp,
        containerColor = colors.surface,
    ) {
      DropdownMenuItem(
          text = { Text(NavRoute.Activity.localizedTitle()) },
          leadingIcon = { Icon(imageVector = NavRoute.Activity.icon, contentDescription = null) },
          onClick = onGoActivity,
      )
      DropdownMenuItem(
          text = { Text(NavRoute.Settings.localizedTitle()) },
          leadingIcon = { Icon(imageVector = NavRoute.Settings.icon, contentDescription = null) },
          onClick = onGoSettings,
      )
    }
  }
}

@Composable
private fun BillingBottomNavItem(
    current: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val selected = current == NavRoute.Billing.path
  val interactionSource = remember { MutableInteractionSource() }
  val iconTint by
      animateColorAsState(
          targetValue =
              when {
                !enabled -> colors.onSurfaceVariant.copy(alpha = 0.4f)
                selected -> colors.primary
                else -> colors.onSurfaceVariant
              },
          animationSpec = tween(durationMillis = 120),
          label = "billingBottomNavTint",
      )
  Column(
      modifier =
          Modifier.height(64.dp).clickable(
              enabled = enabled,
              interactionSource = interactionSource,
              indication = null,
          ) { onClick() },
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Box(
        modifier =
            Modifier.size(34.dp)
                .background(
                    if (selected) colors.primary.copy(alpha = 0.12f) else Color.Transparent,
                    CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
      Icon(
          imageVector = NavRoute.Billing.icon,
          contentDescription = NavRoute.Billing.localizedTitle(),
          tint = iconTint,
      )
    }
  }
}

@Composable
fun NavItem(
    navController: NavController,
    item: NavRoute,
    currentPath: String?,
    enabled: Boolean = true,
) {
  val selected = currentPath == item.path
  val title = item.localizedTitle()
  val colors = MaterialTheme.colorScheme

  val iconTint =
      when {
        !enabled -> colors.onSurfaceVariant.copy(alpha = 0.4f)
        selected -> colors.primary
        else -> colors.onSurfaceVariant
      }

  Column(
      modifier =
          Modifier.height(64.dp).alpha(if (enabled) 1f else 0.5f).clickable(
              enabled = enabled,
              indication = null,
              interactionSource = remember { MutableInteractionSource() },
          ) {
            safeNavigate(navController, item.path)
          },
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    Box(
        modifier =
            Modifier.size(32.dp)
                .then(
                    if (selected)
                        Modifier.background(colors.primary.copy(alpha = 0.12f), CircleShape)
                    else Modifier
                ),
        contentAlignment = Alignment.Center,
    ) {
      Icon(imageVector = item.icon, contentDescription = title, tint = iconTint)
    }

    Text(text = title, color = iconTint, style = MaterialTheme.typography.labelSmall, maxLines = 1)

    Spacer(modifier = Modifier.height(6.dp))
  }
}

@Composable
fun AnimatedBottomNavItem(
    item: NavRoute,
    navController: NavController,
    current: String?,
    enabled: Boolean = true,
    showLabel: Boolean = true,
) {
  val colors = MaterialTheme.colorScheme

  val interactionSource = remember { MutableInteractionSource() }
  val selected = current == item.path
  val title = item.localizedTitle()

  // 🎨 Animaciones
  val iconColor by
      animateColorAsState(
          targetValue =
              when {
                !enabled -> colors.onSurfaceVariant.copy(alpha = 0.4f)
                selected -> colors.primary
                else -> colors.onSurfaceVariant
              },
          animationSpec = tween(durationMillis = 120),
          label = "iconColor",
      )

  val iconScale by
      animateFloatAsState(
          targetValue = if (selected) 1.15f else 1f,
          animationSpec =
              spring(
                  stiffness = Spring.StiffnessMediumLow,
                  dampingRatio = Spring.DampingRatioNoBouncy,
              ),
          label = "iconScale",
      )

  val textColor by animateColorAsState(targetValue = iconColor, label = "textColor")

  Column(
      modifier =
          Modifier.height(64.dp).clickable(
              enabled = enabled,
              interactionSource = interactionSource,
              indication = null,
          ) {
            safeNavigate(navController, item.path)
          },
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
  ) {
    // 🔥 Icon container con glow simulado
    Box(
        modifier =
            Modifier.graphicsLayer {
                  scaleX = iconScale
                  scaleY = iconScale
                }
                .size(34.dp)
                .background(
                    if (selected) colors.primary.copy(alpha = 0.12f) else Color.Transparent,
                    CircleShape,
                ),
        contentAlignment = Alignment.Center,
    ) {
      Icon(imageVector = item.icon, contentDescription = title, tint = iconColor)
    }

    if (showLabel) {
      Text(
          text = title,
          style = MaterialTheme.typography.labelSmall,
          color = textColor,
          maxLines = 1,
      )
      Spacer(modifier = Modifier.height(7.dp))
    }
  }
}

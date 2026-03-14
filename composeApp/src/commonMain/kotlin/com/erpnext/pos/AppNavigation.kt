@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos

import AppColorTheme
import AppTheme
import AppThemeMode
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.erpnext.pos.NavGraph.Setup
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.domain.usecases.LogoutUseCase
import com.erpnext.pos.domain.printing.model.PrintJobStatus
import com.erpnext.pos.domain.printing.model.TransportType
import com.erpnext.pos.domain.repositories.printing.IPrintJobRepository
import com.erpnext.pos.domain.repositories.printing.IPrinterProfileRepository
import com.erpnext.pos.domain.printing.ports.PrinterDiscoveryService
import com.erpnext.pos.localSource.dao.CompanyDao
import com.erpnext.pos.localSource.dao.UserDao
import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.localSource.preferences.ThemePreferences
import com.erpnext.pos.localization.AppStrings
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.localization.ProvideAppStrings
import com.erpnext.pos.navigation.BottomBarWithCenterFab
import com.erpnext.pos.navigation.DesktopNavigationRail
import com.erpnext.pos.navigation.GlobalTopBar
import com.erpnext.pos.navigation.LocalTopBarController
import com.erpnext.pos.navigation.NavRoute
import com.erpnext.pos.navigation.NavigationManager
import com.erpnext.pos.navigation.ShiftOpenChip
import com.erpnext.pos.navigation.StatusIconButton
import com.erpnext.pos.navigation.TopBarController
import com.erpnext.pos.navigation.formatShiftDuration
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.WindowHeightSizeClass
import com.erpnext.pos.utils.WindowWidthSizeClass
import com.erpnext.pos.utils.loading.LoadingIndicator
import com.erpnext.pos.utils.loading.LoadingUiState
import com.erpnext.pos.utils.rememberWindowSizeClass
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarHost
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.activity.ActivityCenter
import com.erpnext.pos.views.billing.BillingResetController
import com.erpnext.pos.views.home.HomeRefreshController
import com.erpnext.pos.views.inventory.InventoryRefreshController
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

fun shouldShowBottomBar(currentRoute: String): Boolean {
  return currentRoute !in listOf(NavRoute.Login.path, NavRoute.Splash.path)
}

fun shouldShowTopBar(currentRoute: String): Boolean {
  return shouldShowBottomBar(currentRoute)
}

private fun defaultTitleForRoute(route: String, strings: AppStrings): String {
  return when {
    route == NavRoute.Home.path -> strings.navigation.home
    route == NavRoute.Inventory.path -> strings.navigation.inventory
    route == NavRoute.Billing.path -> strings.navigation.billing
    route == NavRoute.Customer.path -> strings.navigation.customer
    route == NavRoute.Credits.path -> strings.navigation.credits
    route == NavRoute.Quotation.path -> strings.navigation.quotations
    route == NavRoute.SalesOrder.path -> strings.navigation.salesOrder
    route == NavRoute.DeliveryNote.path -> strings.navigation.deliveryNote
    route.startsWith("reconciliation") -> strings.navigation.reconciliation
    route == NavRoute.InternalTransfer.path -> "Transferencia interna"
    route.startsWith("payment-entry") -> strings.navigation.expenses
    route == NavRoute.Activity.path -> strings.navigation.activity
    route == NavRoute.Settings.path -> strings.navigation.settings
    route == NavRoute.Printers.path -> strings.navigation.printers
    else -> ""
  }
}

private enum class TopBarPrinterState {
  CONNECTED,
  DISCONNECTED,
  OFF,
  PRINTING,
  PROBLEM,
}

private fun NavHostController.navigateTopLevel(route: String) {
  navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
  }
}

private fun NavHostController.navigateSingle(route: String) {
  navigate(route) { launchSingleTop = true }
}

private fun NavHostController.navigateToLoginRoot() {
  navigate(NavRoute.Login.path) {
    popUpTo(graph.findStartDestination().id) { saveState = false }
    launchSingleTop = true
    restoreState = false
  }
}

private fun NavHostController.navigateFromAuthToHome() {
  navigate(NavRoute.Home.path) {
    popUpTo(graph.findStartDestination().id) {
      inclusive = true
      saveState = false
    }
    launchSingleTop = true
    restoreState = false
  }
}

@Composable
fun ImageFromUrl(
    url: String,
    modifier: Modifier = Modifier,
) {
  val strings = LocalAppStrings.current
  SubcomposeAsyncImage(
      model = ImageRequest.Builder(LocalPlatformContext.current).data(url).crossfade(true).build(),
      contentDescription = null,
      modifier = modifier,
      contentScale = ContentScale.Crop,
      loading = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      },
      error = {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Text(strings.common.errorLabel)
        }
      },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
  val navController = rememberNavController()
  val snackbarController = koinInject<SnackbarController>()
  val themePreferences = koinInject<ThemePreferences>()
  val generalPreferences = koinInject<GeneralPreferences>()
  val activityCenter = koinInject<ActivityCenter>()
  val syncManager = koinInject<SyncManager>()
  val syncPreferences = koinInject<SyncPreferences>()
  val networkMonitor = koinInject<NetworkMonitor>()
  val printerProfileRepository = koinInject<IPrinterProfileRepository>()
  val printJobRepository = koinInject<IPrintJobRepository>()
  val printerDiscoveryService = koinInject<PrinterDiscoveryService>()
  val appTheme by themePreferences.theme.collectAsState(initial = AppColorTheme.Noir)
  val appThemeMode by themePreferences.themeMode.collectAsState(initial = AppThemeMode.System)

  val snackbar by snackbarController.snackbar.collectAsState()
  val loadingState by LoadingIndicator.state.collectAsState(initial = LoadingUiState())
  val cashBoxManager = koinInject<CashBoxManager>()
  val homeRefreshController = koinInject<HomeRefreshController>()
  val inventoryRefreshController = koinInject<InventoryRefreshController>()
  val billingResetController = koinInject<BillingResetController>()
  val logoutUseCase = koinInject<LogoutUseCase>()
  val tokenStore = koinInject<TokenStore>()
  val authInfoStore = koinInject<AuthInfoStore>()
  val companyDao = koinInject<CompanyDao>()
  val userDao = koinInject<UserDao>()
  val topBarController = remember { TopBarController() }
  val scope = rememberCoroutineScope()
  val syncState by syncManager.state.collectAsState(initial = SyncState.IDLE)
  val syncSettings by
      syncPreferences.settings.collectAsState(
          initial =
              SyncSettings(
                  autoSync = true,
                  syncOnStartup = true,
                  wifiOnly = false,
                  lastSyncAt = null,
                  useTtl = false,
                  ttlHours = 6,
              )
      )
  val isOnline by networkMonitor.isConnected.collectAsState(false)
  val activityBadgeCount by activityCenter.unreadCount.collectAsState(0)
  val printerEnabled by generalPreferences.printerEnabled.collectAsState(true)
  val defaultPrinter by printerProfileRepository.observeDefaultProfile().collectAsState(initial = null)
  val printJobs by printJobRepository.observeJobs().collectAsState(initial = emptyList())
  val posContext by cashBoxManager.contextFlow.collectAsState(null)
  val tokenSnapshot by tokenStore.tokensFlow().collectAsState(initial = null)
  val isCashboxOpen by cashBoxManager.cashboxState.collectAsState()
  var profileMenuExpanded by remember { mutableStateOf(false) }
  var tick by remember { mutableStateOf(0L) }
  var settingsFromMenu by remember { mutableStateOf(false) }
  var isBluetoothOff by remember { mutableStateOf(false) }
  var showBluetoothOffDialog by remember { mutableStateOf(false) }
  var bluetoothOffDialogAcknowledged by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    while (true) {
      tick = Clock.System.now().toEpochMilliseconds()
      delay(1000)
    }
  }
  val visibleEntries by navController.visibleEntries.collectAsState()
  val currentRoute = visibleEntries.lastOrNull()?.destination?.route ?: ""
  val shiftStart by
      produceState<String?>(initialValue = null, posContext, currentRoute) {
        if (posContext == null || !shouldShowTopBar(currentRoute)) {
          value = null
          return@produceState
        }
        cashBoxManager.activeCashboxStart().collect { value = it }
      }
  LaunchedEffect(currentRoute) { AppLogger.info("AppNavigation currentRoute -> $currentRoute") }

  LaunchedEffect(printerEnabled, defaultPrinter?.id) {
    while (true) {
      val profile = defaultPrinter
      if (
          !printerEnabled ||
              profile == null ||
              profile.preferredTransport !in setOf(TransportType.BT_SPP, TransportType.BT_DESKTOP)
      ) {
        isBluetoothOff = false
        showBluetoothOffDialog = false
        bluetoothOffDialogAcknowledged = false
      } else {
        val bluetoothState =
            runCatching { printerDiscoveryService.bondedDevices(); false }
                .getOrElse { error ->
                  val message = error.message.orEmpty().lowercase()
                  message.contains("bluetooth is turned off") || message.contains("bluetooth apagado")
                }
        if (!bluetoothState) {
          bluetoothOffDialogAcknowledged = false
        }
        isBluetoothOff = bluetoothState
        if (bluetoothState && !bluetoothOffDialogAcknowledged) {
          showBluetoothOffDialog = true
          bluetoothOffDialogAcknowledged = true
        }
      }
      delay(5_000)
    }
  }

  val isDesktop = getPlatformName() == "Desktop"
  val windowSizeClass = rememberWindowSizeClass()
  val isPhoneCompact =
      !isDesktop &&
          (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact ||
              (windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium &&
                  windowSizeClass.heightSizeClass == WindowHeightSizeClass.Compact))
  val previousRoute = navController.previousBackStackEntry?.destination?.route
  val noBackRoutes =
      setOf(
          NavRoute.Home.path,
          NavRoute.Splash.path,
          NavRoute.Login.path,
          NavRoute.Inventory.path,
          NavRoute.Billing.path,
          NavRoute.Customer.path,
      )
  val showBackDefault =
      when (currentRoute) {
        in noBackRoutes -> false
        NavRoute.Settings.path ->
            settingsFromMenu &&
                previousRoute != null &&
                previousRoute !in
                    listOf(NavRoute.Settings.path, NavRoute.Splash.path, NavRoute.Login.path)

        else ->
            previousRoute != null &&
                currentRoute !in listOf(NavRoute.Splash.path, NavRoute.Login.path)
      }
  val topBarState = topBarController.state
  val resolvedShowBack = topBarState.showBack ?: showBackDefault
  val resolvedOnBack: () -> Unit = topBarState.onBack ?: { navController.popBackStack() }
  val resolvedTopBarVisible = topBarState.isVisible ?: true
  val subtitle = topBarState.subtitle
  val cashier = posContext?.cashier
  val cashierDisplayName =
      listOfNotNull(
              cashier?.firstName?.takeIf { it.isNotBlank() },
              cashier?.lastName?.takeIf { it.isNotBlank() },
          )
          .joinToString(" ")
          .ifBlank {
            cashier?.name?.takeIf { it.isNotBlank() }
                ?: cashier?.username?.takeIf { it.isNotBlank() }
                ?: cashier?.email?.takeIf { it.isNotBlank() }
                ?: "Cajero"
          }
  val cashierInitials =
      cashierDisplayName
          .split(" ")
          .filter { it.isNotBlank() }
          .take(2)
          .map { it.first().uppercaseChar() }
          .joinToString("")
          .ifBlank { "C" }
  val companyName = posContext?.company?.takeIf { it.isNotBlank() }
  val currentSiteUrl by
      produceState<String?>(initialValue = null, tokenSnapshot) {
        value =
            runCatching { authInfoStore.getCurrentSite()?.trim()?.takeIf { it.isNotBlank() } }
                .getOrNull()
      }
  val currentSiteCompany by
      produceState<String?>(initialValue = null, currentSiteUrl) {
        val selectedSite = currentSiteUrl?.trim()?.takeIf { it.isNotBlank() }
        if (selectedSite == null) {
          value = null
          return@produceState
        }
        value =
            runCatching {
                  authInfoStore.loadAuthInfoByUrl(selectedSite).company.trim().takeIf {
                    it.isNotBlank()
                  }
                }
                .getOrNull()
      }
  val dbCompanyName by
      produceState<String?>(initialValue = null, currentSiteUrl, tokenSnapshot) {
        value =
            runCatching {
                  companyDao.getCompanyInfo()?.companyName?.trim()?.takeIf { it.isNotBlank() }
                }
                .getOrNull()
      }
  val instanceDisplayName = currentSiteCompany ?: dbCompanyName ?: companyName ?: currentSiteUrl
  LaunchedEffect(currentSiteUrl, dbCompanyName, companyName, instanceDisplayName) {
    AppLogger.info(
        "AppNavigation.instanceLabel site=${currentSiteUrl ?: "none"} " +
            "dbCompany=${dbCompanyName ?: "none"} " +
            "contextCompany=${companyName ?: "none"} " +
            "display=${instanceDisplayName ?: "none"}"
    )
  }
  val localUserImage by
      produceState<String?>(initialValue = null) {
        value =
            runCatching { userDao.getUserInfo()?.image?.trim()?.takeIf { it.isNotBlank() } }
                .getOrNull()
      }
  val cashierImageUrl = cashier?.image?.trim()?.takeIf { it.isNotBlank() } ?: localUserImage

  AppTheme(theme = appTheme, themeMode = appThemeMode) {
    ProvideAppStrings {
      val strings = LocalAppStrings.current
      val titleText =
          if (currentRoute == NavRoute.Home.path) {
            "ERPNext POS"
          } else {
            defaultTitleForRoute(currentRoute, strings).ifBlank { "ERPNext POS" }
          }
      CompositionLocalProvider(LocalTopBarController provides topBarController) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
              if (!isDesktop && shouldShowBottomBar(currentRoute)) {
                BottomBarWithCenterFab(
                    snackbarController = snackbarController,
                    navController = navController,
                    contextProvider = cashBoxManager,
                    leftItems = listOf(NavRoute.Home, NavRoute.Inventory),
                    rightItems =
                        listOf(
                            NavRoute.Customer,
                            NavRoute.Expenses,
                            NavRoute.Activity,
                            NavRoute.Settings,
                        ),
                    fabItem = NavRoute.Billing,
                )
              }
            },
        ) { padding ->
          Row(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (isDesktop && shouldShowBottomBar(currentRoute)) {
              DesktopNavigationRail(
                  navController = navController,
                  contextProvider = cashBoxManager,
                  activityBadgeCount = activityBadgeCount,
              )
            }
            Column(modifier = Modifier.weight(1f)) {
              if (shouldShowTopBar(currentRoute) && (!isPhoneCompact || resolvedTopBarVisible)) {
                GlobalTopBar(
                    title = {
                      Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(12.dp),
                      ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                          Text(
                              text = titleText,
                              color = MaterialTheme.colorScheme.onSurface,
                              style =
                                  if (isPhoneCompact) {
                                    MaterialTheme.typography.titleMedium
                                  } else {
                                    MaterialTheme.typography.titleLarge
                                  },
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                          )
                          AnimatedVisibility(
                              visible = !subtitle.isNullOrBlank(),
                              enter =
                                  fadeIn(tween(200)) +
                                      slideInVertically(
                                          animationSpec = tween(180, easing = FastOutSlowInEasing),
                                          initialOffsetY = { it / 3 },
                                      ),
                              exit =
                                  fadeOut(tween(120)) +
                                      slideOutVertically(
                                          animationSpec = tween(120, easing = FastOutSlowInEasing),
                                          targetOffsetY = { it / 4 },
                                      ),
                          ) {
                            AnimatedContent(
                                targetState = subtitle.orEmpty(),
                                transitionSpec = {
                                  (fadeIn(tween(160)) + slideInVertically { it / 6 }).togetherWith(
                                      fadeOut(tween(120)) + slideOutVertically { -it / 6 }
                                  )
                                },
                                label = "topbarSubtitleAnim",
                            ) { value ->
                              Text(
                                  text = value,
                                  style = MaterialTheme.typography.bodySmall,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                                  maxLines = 1,
                                  overflow = TextOverflow.Ellipsis,
                              )
                            }
                          }
                        }
                        if (!isPhoneCompact) {
                          ShiftOpenChip(
                              isOpen = isCashboxOpen,
                              duration = formatShiftDuration(shiftStart, tick),
                          )
                        }
                      }
                    },
                    actions = {
                      val isRecentlySynced =
                          syncSettings.lastSyncAt?.let { tick - it < 10 * 60 * 1000 } == true
                      val dbHealthy = isOnline && isRecentlySynced && syncState !is SyncState.ERROR
                      val dbTint =
                          when {
                            isCashboxOpen -> MaterialTheme.colorScheme.onSurfaceVariant
                            syncState is SyncState.SYNCING -> Color(0xFFF59E0B)
                            syncState is SyncState.ERROR -> MaterialTheme.colorScheme.error
                            dbHealthy -> Color(0xFF2E7D32)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                          }
                      val dbLabel =
                          when (syncState) {
                            is SyncState.SYNCING ->
                                "${strings.common.databaseSyncing}: ${(syncState as SyncState.SYNCING).message}"
                            is SyncState.ERROR ->
                                "${strings.common.databaseError}: ${(syncState as SyncState.ERROR).message}"
                            is SyncState.SUCCESS -> strings.common.databaseSynced
                            else ->
                                if (dbHealthy) {
                                  strings.common.databaseHealthy
                                } else {
                                  strings.common.databasePending
                                }
                          }
                      val showNewSale =
                          (currentRoute == NavRoute.Billing.path ||
                              currentRoute == NavRoute.Billing.path) && !subtitle.isNullOrBlank()
                      val latestJob = printJobs.maxByOrNull { it.createdAtEpochMs }
                      val hasFailedJob = latestJob?.status == PrintJobStatus.FAILED
                      val isPrintingJob =
                          latestJob?.status in
                              setOf(
                                  PrintJobStatus.CONNECTING,
                                  PrintJobStatus.RENDERING,
                                  PrintJobStatus.PRINTING,
                                  PrintJobStatus.RETRYING,
                              )
                      val hasDefaultPrinter = defaultPrinter?.isEnabled == true
                      val isConnected =
                          hasDefaultPrinter && !isBluetoothOff && !hasFailedJob && !isPrintingJob
                      val printerState =
                          when {
                            isPrintingJob -> TopBarPrinterState.PRINTING
                            hasFailedJob -> TopBarPrinterState.PROBLEM
                            isBluetoothOff -> TopBarPrinterState.OFF
                            isConnected -> TopBarPrinterState.CONNECTED
                            else -> TopBarPrinterState.DISCONNECTED
                          }
                      val printerLabel =
                          when (printerState) {
                            TopBarPrinterState.CONNECTED -> strings.common.printerConnected
                            TopBarPrinterState.DISCONNECTED -> strings.common.printerDisconnected
                            TopBarPrinterState.OFF -> strings.common.printerOff
                            TopBarPrinterState.PRINTING -> strings.common.printerPrinting
                            TopBarPrinterState.PROBLEM -> strings.common.printerProblem
                          }
                      val printerTint =
                          when (printerState) {
                            TopBarPrinterState.CONNECTED -> Color(0xFF2E7D32)
                            TopBarPrinterState.DISCONNECTED ->
                                MaterialTheme.colorScheme.onSurfaceVariant
                            TopBarPrinterState.OFF -> Color(0xFFF59E0B)
                            TopBarPrinterState.PRINTING -> MaterialTheme.colorScheme.primary
                            TopBarPrinterState.PROBLEM -> MaterialTheme.colorScheme.error
                          }
                      val printerIcon =
                          when (printerState) {
                            TopBarPrinterState.PROBLEM,
                            TopBarPrinterState.OFF -> Icons.Outlined.Info
                            else -> Icons.Outlined.Print
                          }
                      Row(
                          horizontalArrangement =
                              Arrangement.spacedBy(if (isPhoneCompact) 8.dp else 12.dp),
                          verticalAlignment = Alignment.CenterVertically,
                      ) {
                        if (!isPhoneCompact) {
                          AnimatedVisibility(
                              visible = showNewSale,
                              enter = fadeIn(tween(180)),
                              exit = fadeOut(tween(160)),
                          ) {
                            StatusIconButton(
                                label = strings.common.newSale,
                                onClick = { billingResetController.reset() },
                                tint = MaterialTheme.colorScheme.primary,
                            ) {
                              Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
                            }
                          }
                        }
                        StatusIconButton(
                            label =
                                if (isOnline) strings.common.internetConnected
                                else strings.common.internetDisconnected,
                            onClick = {},
                            enabled = false,
                            tint =
                                if (isOnline) Color(0xFF2E7D32)
                                else MaterialTheme.colorScheme.error,
                        ) {
                          Icon(
                              if (isOnline) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
                              contentDescription = null,
                          )
                        }
                        StatusIconButton(
                            label = dbLabel,
                            onClick = {
                              scope.launch { syncManager.fullSync(force = true) }
                              if (!isCashboxOpen) {
                                snackbarController.show(
                                    strings.common.baseCompanySyncOnlyNotice,
                                    SnackbarType.Info,
                                    SnackbarPosition.Bottom,
                                )
                              }
                            },
                            tint = dbTint,
                        ) {
                          if (syncState is SyncState.SYNCING) {
                            CircularProgressIndicator(Modifier.size(18.dp))
                          } else {
                            Icon(Icons.Outlined.Storage, contentDescription = null)
                          }
                        }
                        /* StatusIconButton(
                            label = if (activityBadgeCount > 0) {
                                "${strings.navigation.activity}: $activityBadgeCount"
                            } else {
                                strings.navigation.activity
                            },
                            onClick = {
                                navController.navigateSingle(NavRoute.Activity.path)
                            },
                            tint = if (activityBadgeCount > 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = null
                            )
                        }*/
                        if (!isPhoneCompact) {
                          StatusIconButton(
                              label = strings.common.retry,
                              onClick = {
                                when (currentRoute) {
                                  NavRoute.Inventory.path -> inventoryRefreshController.refresh()
                                  NavRoute.Home.path -> homeRefreshController.refresh()
                                  else -> homeRefreshController.refresh()
                                }
                              },
                          ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                          }
                        }
                        if (printerEnabled) {
                          StatusIconButton(
                              label = printerLabel,
                              onClick = { navController.navigateSingle(NavRoute.Printers.path) },
                              tint = printerTint,
                          ) {
                            Icon(printerIcon, contentDescription = null)
                          }
                        }
                      }
                      // Spacer(modifier = Modifier.width(8.dp))
                      Row(
                          modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(10.dp),
                      ) {
                        if (instanceDisplayName != null && !isPhoneCompact) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium,
                                modifier =
                                    Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                      cashBoxManager.clearContext()
                                      navController.navigateToLoginRoot()
                                    },
                            ) {
                              Row(
                                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                  verticalAlignment = Alignment.CenterVertically,
                                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                              ) {
                                Icon(
                                    imageVector = Icons.Outlined.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                  Text(
                                      text = instanceDisplayName,
                                      style = MaterialTheme.typography.labelLarge,
                                      fontWeight = FontWeight.SemiBold,
                                      color = MaterialTheme.colorScheme.onSurface,
                                      maxLines = 1,
                                      overflow = TextOverflow.Ellipsis,
                                  )
                                  Text(
                                      text = strings.common.switchInstance,
                                      style = MaterialTheme.typography.labelSmall,
                                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                                  )
                                }
                              }
                            }
                        }
                        if (cashier != null && !isPhoneCompact) {
                          Column(horizontalAlignment = Alignment.End) {
                            Text(
                                cashierDisplayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                              Surface(
                                  color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                  shape = MaterialTheme.shapes.small,
                              ) {
                                Text(
                                    if (isOnline) strings.common.online else strings.common.offline,
                                    modifier =
                                        Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color =
                                        if (isOnline) {
                                          MaterialTheme.colorScheme.primary
                                        } else {
                                          MaterialTheme.colorScheme.error
                                        },
                                )
                              }
                            }
                          }
                        }
                        Box {
                          Surface(
                              color = MaterialTheme.colorScheme.surfaceVariant,
                              shape = CircleShape,
                          ) {
                            Box(
                                modifier =
                                    Modifier.size(36.dp).clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                    ) {
                                      profileMenuExpanded = true
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                              if (!cashierImageUrl.isNullOrBlank()) {
                                ImageFromUrl(
                                    url = cashierImageUrl,
                                    modifier = Modifier.fillMaxSize(),
                                )
                              } else {
                                Text(
                                    text = cashierInitials,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                              }
                            }
                          }
                          DropdownMenu(
                              expanded = profileMenuExpanded,
                              onDismissRequest = { profileMenuExpanded = false },
                              offset = DpOffset(x = 0.dp, y = 8.dp),
                          ) {
                            /*Column(
                                modifier = Modifier.padding(
                                    horizontal = 14.dp,
                                    vertical = 8.dp
                                )
                            ) {
                                Text(
                                    cashierDisplayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isOnline) "Online" else "Offline",
                                    text = if (isOnline) strings.common.online else strings.common.offline,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOnline) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    }
                                )
                            }*/
                            // HorizontalDivider()
                            /*DropdownMenuItem(
                                text = { Text("Pago de factura") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = NavRoute.PaymentEntry().icon,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    profileMenuExpanded = false
                                    navController.navigateSingle(NavRoute.PaymentEntry().path)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Transferencia interna") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = NavRoute.InternalTransfer.icon,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    profileMenuExpanded = false
                                    navController.navigateSingle(NavRoute.InternalTransfer.path)
                                }
                            )*/
                            // HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(strings.navigation.settings) },
                                leadingIcon = {
                                  Icon(
                                      imageVector = Icons.Outlined.Settings,
                                      contentDescription = null,
                                  )
                                },
                                onClick = {
                                  profileMenuExpanded = false
                                  navController.navigateSingle(NavRoute.Settings.path)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(strings.navigation.reconciliation) },
                                leadingIcon = {
                                  Icon(imageVector = Icons.Outlined.Tune, contentDescription = null)
                                },
                                onClick = {
                                  profileMenuExpanded = false
                                  navController.navigateSingle(NavRoute.Reconciliation().path)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(strings.common.retry) },
                                leadingIcon = {
                                  Icon(imageVector = Icons.Outlined.Refresh, contentDescription = null)
                                },
                                onClick = {
                                  profileMenuExpanded = false
                                  when (currentRoute) {
                                    NavRoute.Inventory.path -> inventoryRefreshController.refresh()
                                    NavRoute.Home.path -> homeRefreshController.refresh()
                                    else -> homeRefreshController.refresh()
                                  }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text(strings.common.switchInstance) },
                                leadingIcon = {
                                  Icon(
                                      imageVector = Icons.Outlined.SwapHoriz,
                                      contentDescription = null,
                                  )
                                },
                                onClick = {
                                  profileMenuExpanded = false
                                  scope.launch {
                                    runCatching { tokenStore.clear() }
                                    cashBoxManager.clearContext()
                                  }
                                  navController.navigateToLoginRoot()
                                },
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text(strings.common.logout) },
                                leadingIcon = {
                                  Icon(
                                      imageVector = Icons.AutoMirrored.Outlined.Logout,
                                      contentDescription = null,
                                  )
                                },
                                onClick = {
                                  profileMenuExpanded = false
                                  scope.launch { runCatching { logoutUseCase.invoke(null) } }
                                  navController.navigateToLoginRoot()
                                },
                            )
                          }
                        }
                      }
                    },
                    showBack = resolvedShowBack,
                    onBack = resolvedOnBack,
                    bottomContent = {},
                )
              }
              Box(modifier = Modifier.weight(1f)) {
                Setup(navController, false)
                if (showBluetoothOffDialog) {
                  AlertDialog(
                      onDismissRequest = { showBluetoothOffDialog = false },
                      icon = {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                      },
                      title = { Text(strings.common.printerOff) },
                      text = {
                        Text(strings.common.printerBluetoothOffDialogBody)
                      },
                      confirmButton = {
                        TextButton(
                            onClick = {
                              showBluetoothOffDialog = false
                              navController.navigateSingle(NavRoute.Printers.path)
                            }
                        ) {
                          Text(strings.navigation.printers)
                        }
                      },
                      dismissButton = {
                        TextButton(onClick = { showBluetoothOffDialog = false }) {
                          Text(strings.common.understood)
                        }
                      },
                  )
                }

                SnackbarHost(snackbar = snackbar, onDismiss = snackbarController::dismiss)

                androidx.compose.animation.AnimatedVisibility(
                    visible = loadingState.isLoading,
                    modifier = Modifier.align(Alignment.TopCenter),
                    enter =
                        fadeIn(tween(160)) +
                            slideInVertically(
                                animationSpec = tween(180, easing = FastOutSlowInEasing),
                                initialOffsetY = { -it / 2 },
                            ),
                    exit =
                        fadeOut(tween(120)) +
                            slideOutVertically(
                                animationSpec = tween(140, easing = FastOutSlowInEasing),
                                targetOffsetY = { -it / 2 },
                            ),
                ) {
                  Surface(
                      color = MaterialTheme.colorScheme.surfaceContainerHigh,
                      shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                      tonalElevation = 6.dp,
                      shadowElevation = 10.dp,
                  ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                      val strings = LocalAppStrings.current
                      val message =
                          loadingState.message.ifBlank {
                            (syncState as? SyncState.SYNCING)?.message ?: "Procesando..."
                          }
                      Text(
                          text = message,
                          modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                          style = MaterialTheme.typography.labelMedium,
                          color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      val step = loadingState.currentStep
                      val total = loadingState.totalSteps
                      if (step != null && total != null && total > 0) {
                        Text(
                            text =
                                "${strings.settings.syncStepLabel} $step ${strings.settings.syncStepOfLabel} $total",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                      }
                      if (loadingState.progress != null) {
                        LinearProgressIndicator(
                            progress = { loadingState.progress ?: 0f },
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(6.dp)
                                    .clip(
                                        RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                                    ),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        )
                      } else {
                        LinearProgressIndicator(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .height(6.dp)
                                    .clip(
                                        RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                                    ),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        )
                      }
                    }
                  }
                }

                val navManager: NavigationManager = koinInject()
                LaunchedEffect(Unit) {
                  navManager.navigationEvents.collect { event ->
                    AppLogger.info(
                        "AppNavigation nav event -> ${event::class.simpleName} " +
                            "currentRoute=${navController.currentBackStackEntry?.destination?.route}"
                    )
                    when (event) {
                      is NavRoute.Login -> {
                        val route = navController.currentBackStackEntry?.destination?.route
                        if (route != NavRoute.Login.path) {
                          navController.navigateToLoginRoot()
                        }
                      }

                      is NavRoute.Home -> {
                        val route = navController.currentBackStackEntry?.destination?.route
                        runCatching {
                              if (route == NavRoute.Login.path || route == NavRoute.Splash.path) {
                                AppLogger.info("AppNavigation: Home navigation via auth reset")
                                navController.navigateFromAuthToHome()
                              } else {
                                AppLogger.info("AppNavigation: Home navigation via top level")
                                navController.navigateTopLevel(NavRoute.Home.path)
                              }
                            }
                            .onFailure { error ->
                              AppLogger.warn(
                                  "AppNavigation: Home navigation failed, fallback to single",
                                  error,
                              )
                              navController.navigateSingle(NavRoute.Home.path)
                            }
                      }
                      // is NavRoute.Billing ->
                      // navController.navigateTopLevel(NavRoute.Billing.path)
                      is NavRoute.Billing -> navController.navigateSingle(NavRoute.Billing.path)

                      is NavRoute.Credits -> navController.navigateSingle(NavRoute.Credits.path)

                      is NavRoute.Quotation -> navController.navigateSingle(NavRoute.Quotation.path)

                      is NavRoute.SalesOrder ->
                          navController.navigateSingle(NavRoute.SalesOrder.path)

                      is NavRoute.DeliveryNote ->
                          navController.navigateSingle(NavRoute.DeliveryNote.path)

                      is NavRoute.Reconciliation -> navController.navigateSingle(event.path)

                      is NavRoute.Settings -> navController.navigateTopLevel(NavRoute.Settings.path)
                      is NavRoute.Printers -> navController.navigateSingle(NavRoute.Printers.path)

                      is NavRoute.Activity -> navController.navigateSingle(NavRoute.Activity.path)

                      is NavRoute.PaymentEntry -> navController.navigateSingle(event.path)
                      NavRoute.Expenses -> {}
                      is NavRoute.InternalTransfer -> navController.navigateSingle(event.path)

                      is NavRoute.NavigateUp -> navController.popBackStack()
                      else -> {}
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

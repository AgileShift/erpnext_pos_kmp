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
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.erpnext.pos.localSource.dao.UserDao
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.localSource.preferences.ThemePreferences
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
import com.erpnext.pos.remoteSource.oauth.TokenStore
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.loading.LoadingIndicator
import com.erpnext.pos.utils.loading.LoadingUiState
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarHost
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.billing.BillingResetController
import com.erpnext.pos.views.home.HomeRefreshController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

fun shouldShowBottomBar(currentRoute: String): Boolean {
    return currentRoute !in listOf(NavRoute.Login.path, NavRoute.Splash.path)
}

fun shouldShowTopBar(currentRoute: String): Boolean {
    return shouldShowBottomBar(currentRoute)
}

//TOOD: Localizar
private fun defaultTitleForRoute(route: String): String {
    return when {
        route == NavRoute.Home.path -> "Inicio"
        route == NavRoute.Inventory.path -> "Inventario"
        route == NavRoute.Billing.path -> "Ventas"
        route == NavRoute.Billing.path -> "POS Lab"
        route == NavRoute.Customer.path -> "Clientes"
        route == NavRoute.Credits.path -> "Créditos"
        route == NavRoute.Quotation.path -> "Cotizaciones"
        route == NavRoute.SalesOrder.path -> "Orden de venta"
        route == NavRoute.DeliveryNote.path -> "Nota de entrega"
        route.startsWith("reconciliation") -> "Reconciliación"
        route.startsWith("payment-entry") -> "Entrada de pago"
        route == NavRoute.Settings.path -> "Configuración"
        else -> ""
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateSingle(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

@Composable
fun ImageFromUrl(
    url: String,
    modifier: Modifier = Modifier,
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalPlatformContext.current)
            .data(url)
            .crossfade(true)
            .build(),
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
                Text("No se pudo cargar la imagen")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val snackbarController = koinInject<SnackbarController>()
    val themePreferences = koinInject<ThemePreferences>()
    val syncManager = koinInject<SyncManager>()
    val syncPreferences = koinInject<SyncPreferences>()
    val networkMonitor = koinInject<NetworkMonitor>()
    val appTheme by themePreferences.theme.collectAsState(initial = AppColorTheme.Noir)
    val appThemeMode by themePreferences.themeMode.collectAsState(initial = AppThemeMode.System)

    val snackbar by snackbarController.snackbar.collectAsState()
    val loadingState by LoadingIndicator.state.collectAsState(initial = LoadingUiState())
    val cashBoxManager = koinInject<CashBoxManager>()
    val homeRefreshController = koinInject<HomeRefreshController>()
    val billingResetController = koinInject<BillingResetController>()
    val logoutUseCase = koinInject<LogoutUseCase>()
    val tokenStore = koinInject<TokenStore>()
    val userDao = koinInject<UserDao>()
    val topBarController = remember { TopBarController() }
    val scope = rememberCoroutineScope()
    val syncState by syncManager.state.collectAsState(initial = SyncState.IDLE)
    val syncSettings by syncPreferences.settings.collectAsState(
        initial = SyncSettings(
            autoSync = true,
            syncOnStartup = true,
            wifiOnly = false,
            lastSyncAt = null,
            useTtl = false
        )
    )
    val isOnline by networkMonitor.isConnected.collectAsState(false)
    val posContext by cashBoxManager.contextFlow.collectAsState(null)
    val isCashboxOpen by cashBoxManager.cashboxState.collectAsState()
    val shiftStart by cashBoxManager.activeCashboxStart().collectAsState(null)
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0L) }
    var settingsFromMenu by remember { mutableStateOf(false) }
    var activityBadgeCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            tick = Clock.System.now().toEpochMilliseconds()
            delay(1000)
        }
    }
    val visibleEntries by navController.visibleEntries.collectAsState()
    val currentRoute = visibleEntries.lastOrNull()?.destination?.route ?: ""


    LaunchedEffect(isOnline, posContext?.profileName, posContext?.territory, posContext?.route) {
        activityBadgeCount = 0
    }

    val isDesktop = getPlatformName() == "Desktop"
    val titleFallback = defaultTitleForRoute(currentRoute)
    val previousRoute = navController.previousBackStackEntry?.destination?.route
    val noBackRoutes = setOf(
        NavRoute.Home.path,
        NavRoute.Splash.path,
        NavRoute.Login.path,
        NavRoute.Inventory.path,
        NavRoute.Billing.path,
        NavRoute.Customer.path
    )
    val showBackDefault = when (currentRoute) {
        in noBackRoutes -> false
        NavRoute.Settings.path ->
            settingsFromMenu && previousRoute != null &&
                    previousRoute !in listOf(
                NavRoute.Settings.path,
                NavRoute.Splash.path,
                NavRoute.Login.path
            )

        else -> previousRoute != null && currentRoute !in listOf(
            NavRoute.Splash.path,
            NavRoute.Login.path
        )
    }
    val topBarState = topBarController.state
    val resolvedShowBack = topBarState.showBack ?: showBackDefault
    val resolvedOnBack: () -> Unit = topBarState.onBack ?: {
        navController.popBackStack()
    }
    val subtitle = topBarState.subtitle
    val titleText = if (currentRoute == NavRoute.Home.path) {
        "ERPNext POS"
    } else {
        titleFallback.ifBlank { "ERPNext POS" }
    }
    val cashier = posContext?.cashier
    val cashierDisplayName = listOfNotNull(
        cashier?.firstName?.takeIf { it.isNotBlank() },
        cashier?.lastName?.takeIf { it.isNotBlank() }
    ).joinToString(" ").ifBlank {
        cashier?.name?.takeIf { it.isNotBlank() }
            ?: cashier?.username?.takeIf { it.isNotBlank() }
            ?: cashier?.email?.takeIf { it.isNotBlank() }
            ?: "Cajero"
    }
    val cashierInitials = cashierDisplayName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifBlank { "C" }
    val companyName = posContext?.company?.takeIf { it.isNotBlank() }
    val localUserImage by produceState<String?>(initialValue = null) {
        value = runCatching { userDao.getUserInfo()?.image?.trim()?.takeIf { it.isNotBlank() } }
            .getOrNull()
    }
    val cashierImageUrl = cashier?.image?.trim()?.takeIf { it.isNotBlank() } ?: localUserImage

    AppTheme(theme = appTheme, themeMode = appThemeMode) {
        ProvideAppStrings {
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
                                contextProvider = cashBoxManager,
                                activityBadgeCount = activityBadgeCount
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            if (shouldShowTopBar(currentRoute)) {
                                GlobalTopBar(
                                    title = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = titleText,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    style = MaterialTheme.typography.titleLarge
                                                )
                                                AnimatedVisibility(
                                                    visible = !subtitle.isNullOrBlank(),
                                                    enter = fadeIn(tween(200)) + slideInVertically(
                                                        animationSpec = tween(
                                                            180,
                                                            easing = FastOutSlowInEasing
                                                        ),
                                                        initialOffsetY = { it / 3 }
                                                    ),
                                                    exit = fadeOut(tween(120)) + slideOutVertically(
                                                        animationSpec = tween(
                                                            120,
                                                            easing = FastOutSlowInEasing
                                                        ),
                                                        targetOffsetY = { it / 4 }
                                                    )
                                                ) {
                                                    AnimatedContent(
                                                        targetState = subtitle.orEmpty(),
                                                        transitionSpec = {
                                                            (fadeIn(tween(160)) + slideInVertically { it / 6 })
                                                                .togetherWith(
                                                                    fadeOut(tween(120)) + slideOutVertically { -it / 6 }
                                                                )
                                                        },
                                                        label = "topbarSubtitleAnim"
                                                    ) { value ->
                                                        Text(
                                                            text = value,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                            ShiftOpenChip(
                                                isOpen = isCashboxOpen,
                                                duration = formatShiftDuration(shiftStart, tick),
                                                closeAction = { }
                                            )
                                        }
                                    },
                                    actions = {
                                        val isRecentlySynced =
                                            syncSettings.lastSyncAt?.let { tick - it < 10 * 60 * 1000 } == true
                                        val dbHealthy =
                                            isOnline && isRecentlySynced && syncState !is SyncState.ERROR
                                        val dbTint = when {
                                            isCashboxOpen -> MaterialTheme.colorScheme.onSurfaceVariant
                                            syncState is SyncState.SYNCING -> Color(0xFFF59E0B)
                                            syncState is SyncState.ERROR -> MaterialTheme.colorScheme.error
                                            dbHealthy -> Color(0xFF2E7D32)
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                        val dbLabel = when (syncState) {
                                            is SyncState.SYNCING -> "Base de datos: ${(syncState as SyncState.SYNCING).message}"
                                            is SyncState.ERROR -> "Base de datos: ${(syncState as SyncState.ERROR).message}"
                                            is SyncState.SUCCESS -> "Base de datos: Sincronizada"
                                            else -> if (dbHealthy) {
                                                "Base de datos: Saludable"
                                            } else {
                                                "Base de datos: Pendiente"
                                            }
                                        }
                                        val showNewSale = (currentRoute == NavRoute.Billing.path ||
                                                currentRoute == NavRoute.Billing.path) &&
                                                !subtitle.isNullOrBlank()
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            AnimatedVisibility(
                                                visible = showNewSale,
                                                enter = fadeIn(tween(180)),
                                                exit = fadeOut(tween(160))
                                            ) {
                                                StatusIconButton(
                                                    label = "Nueva venta",
                                                    onClick = { billingResetController.reset() },
                                                    tint = MaterialTheme.colorScheme.primary
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Add,
                                                        contentDescription = null
                                                    )
                                                }
                                            }
                                            StatusIconButton(
                                                label = if (isOnline) "Internet: Conectado" else "Internet: Sin conexión",
                                                onClick = {},
                                                enabled = false,
                                                tint = if (isOnline) Color(0xFF2E7D32)
                                                else MaterialTheme.colorScheme.error,
                                            ) {
                                                Icon(
                                                    if (isOnline) Icons.Outlined.Wifi else Icons.Outlined.WifiOff,
                                                    contentDescription = null
                                                )
                                            }
                                            StatusIconButton(
                                                label = dbLabel,
                                                onClick = {
                                                    if (!isCashboxOpen) {
                                                        snackbarController.show(
                                                            "No podemos sincronizar sin anter aperturar caja",
                                                            SnackbarType.Error,
                                                            SnackbarPosition.Bottom
                                                        )
                                                    } else {
                                                        scope.launch { syncManager.fullSync(force = true) }
                                                    }
                                                },
                                                tint = dbTint,
                                            ) {
                                                if (syncState is SyncState.SYNCING) {
                                                    CircularProgressIndicator(Modifier.size(18.dp))
                                                } else {
                                                    Icon(
                                                        Icons.Outlined.Storage,
                                                        contentDescription = null
                                                    )
                                                }
                                            }
                                            StatusIconButton(
                                                label = "Refrescar",
                                                onClick = { homeRefreshController.refresh() },
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Refresh,
                                                    contentDescription = null
                                                )
                                            }
                                            val printerConnected = false
                                            StatusIconButton(
                                                label = if (printerConnected) "Impresora: Conectada"
                                                else "Impresora: Sin conexión",
                                                onClick = {},
                                                enabled = false,
                                                tint = if (printerConnected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            ) {
                                                Icon(
                                                    Icons.Outlined.Print,
                                                    contentDescription = null
                                                )
                                            }
                                        }
                                        //Spacer(modifier = Modifier.width(8.dp))
                                        Row(
                                            modifier = Modifier.padding(start = 8.dp, end = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            if (companyName != null) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = MaterialTheme.shapes.medium,
                                                    modifier = Modifier.clickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        indication = null
                                                    ) {
                                                        navController.navigateSingle(NavRoute.Login.path)
                                                    }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(
                                                            horizontal = 12.dp,
                                                            vertical = 8.dp
                                                        ),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            8.dp
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Business,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                        Column(horizontalAlignment = Alignment.End) {
                                                            Text(
                                                                text = companyName,
                                                                style = MaterialTheme.typography.labelLarge,
                                                                fontWeight = FontWeight.SemiBold,
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "Cambiar instancia",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            if (cashier != null) {
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        cashierDisplayName,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(
                                                            6.dp
                                                        )
                                                    ) {
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.primary.copy(
                                                                alpha = 0.12f
                                                            ),
                                                            shape = MaterialTheme.shapes.small
                                                        ) {
                                                            Text(
                                                                if (isOnline) "Online" else "Offline",
                                                                modifier = Modifier.padding(
                                                                    horizontal = 10.dp,
                                                                    vertical = 4.dp
                                                                ),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = if (isOnline) {
                                                                    MaterialTheme.colorScheme.primary
                                                                } else {
                                                                    MaterialTheme.colorScheme.error
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            Box {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = CircleShape
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(36.dp)
                                                            .clickable(
                                                                interactionSource = remember { MutableInteractionSource() },
                                                                indication = null
                                                            ) { profileMenuExpanded = true },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (!cashierImageUrl.isNullOrBlank()) {
                                                            ImageFromUrl(
                                                                url = cashierImageUrl,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        } else {
                                                            Text(
                                                                text = cashierInitials,
                                                                style = MaterialTheme.typography.labelLarge,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                    }
                                                }
                                                DropdownMenu(
                                                    expanded = profileMenuExpanded,
                                                    onDismissRequest = {
                                                        profileMenuExpanded = false
                                                    },
                                                    offset = DpOffset(x = 0.dp, y = 8.dp)
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
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isOnline) {
                                                                MaterialTheme.colorScheme.primary
                                                            } else {
                                                                MaterialTheme.colorScheme.error
                                                            }
                                                        )
                                                    }*/
                                                    //HorizontalDivider()
                                                    DropdownMenuItem(
                                                        text = { Text("Configuración") },
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Settings,
                                                                contentDescription = null
                                                            )
                                                        },
                                                        onClick = {
                                                            profileMenuExpanded = false
                                                            navController.navigateSingle(NavRoute.Settings.path)
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Reconciliación") },
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Tune,
                                                                contentDescription = null
                                                            )
                                                        },
                                                        onClick = {
                                                            profileMenuExpanded = false
                                                            navController.navigateSingle(NavRoute.Reconciliation().path)
                                                        }
                                                    )
                                                    DropdownMenuItem(
                                                        text = { Text("Cambiar instancia") },
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.Outlined.SwapHoriz,
                                                                contentDescription = null
                                                            )
                                                        },
                                                        onClick = {
                                                            profileMenuExpanded = false
                                                            scope.launch {
                                                                runCatching { tokenStore.clear() }
                                                                cashBoxManager.clearContext()
                                                            }
                                                            navController.navigateSingle(NavRoute.Login.path)
                                                        }
                                                    )
                                                    HorizontalDivider()
                                                    DropdownMenuItem(
                                                        text = { Text("Cerrar sesión") },
                                                        leadingIcon = {
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                                                contentDescription = null
                                                            )
                                                        },
                                                        onClick = {
                                                            profileMenuExpanded = false
                                                            scope.launch {
                                                                runCatching {
                                                                    logoutUseCase.invoke(
                                                                        null
                                                                    )
                                                                }
                                                            }
                                                            navController.navigateSingle(NavRoute.Login.path)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    showBack = resolvedShowBack,
                                    onBack = resolvedOnBack,
                                    bottomContent = {}
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

                                androidx.compose.animation.AnimatedVisibility(
                                    visible = loadingState.isLoading,
                                    modifier = Modifier.align(Alignment.TopCenter),
                                    enter = fadeIn(tween(160)) + slideInVertically(
                                        animationSpec = tween(180, easing = FastOutSlowInEasing),
                                        initialOffsetY = { -it / 2 }
                                    ),
                                    exit = fadeOut(tween(120)) + slideOutVertically(
                                        animationSpec = tween(140, easing = FastOutSlowInEasing),
                                        targetOffsetY = { -it / 2 }
                                    )
                                ) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        shape = RoundedCornerShape(
                                            bottomStart = 12.dp,
                                            bottomEnd = 12.dp
                                        ),
                                        tonalElevation = 6.dp,
                                        shadowElevation = 10.dp
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            val strings = LocalAppStrings.current
                                            val message = loadingState.message.ifBlank {
                                                (syncState as? SyncState.SYNCING)?.message
                                                    ?: "Procesando..."
                                            }
                                            Text(
                                                text = message,
                                                modifier = Modifier.padding(
                                                    horizontal = 12.dp,
                                                    vertical = 6.dp
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            val step = loadingState.currentStep
                                            val total = loadingState.totalSteps
                                            if (step != null && total != null && total > 0) {
                                                Text(
                                                    text = "${strings.settings.syncStepLabel} $step ${strings.settings.syncStepOfLabel} $total",
                                                    modifier = Modifier.padding(
                                                        horizontal = 12.dp,
                                                        vertical = 2.dp
                                                    ),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (loadingState.progress != null) {
                                                LinearProgressIndicator(
                                                    progress = { loadingState.progress ?: 0f },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(
                                                            RoundedCornerShape(
                                                                bottomStart = 12.dp,
                                                                bottomEnd = 12.dp
                                                            )
                                                        ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.15f
                                                    )
                                                )
                                            } else {
                                                LinearProgressIndicator(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(6.dp)
                                                        .clip(
                                                            RoundedCornerShape(
                                                                bottomStart = 12.dp,
                                                                bottomEnd = 12.dp
                                                            )
                                                        ),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    trackColor = MaterialTheme.colorScheme.primary.copy(
                                                        alpha = 0.15f
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }

                                val navManager: NavigationManager = koinInject()
                                LaunchedEffect(Unit) {
                                    navManager.navigationEvents.collect { event ->
                                        when (event) {
                                            is NavRoute.Login -> {
                                                val route =
                                                    navController.currentBackStackEntry?.destination?.route
                                                if (route != NavRoute.Login.path) {
                                                    navController.navigateSingle(NavRoute.Login.path)
                                                }
                                            }

                                            is NavRoute.Home -> navController.navigateTopLevel(
                                                NavRoute.Home.path
                                            )
                                            //is NavRoute.Billing -> navController.navigateTopLevel(NavRoute.Billing.path)
                                            is NavRoute.Billing -> navController.navigateSingle(
                                                NavRoute.Billing.path
                                            )

                                            is NavRoute.Credits -> navController.navigateSingle(
                                                NavRoute.Credits.path
                                            )

                                            is NavRoute.Quotation -> navController.navigateSingle(
                                                NavRoute.Quotation.path
                                            )

                                            is NavRoute.SalesOrder -> navController.navigateSingle(
                                                NavRoute.SalesOrder.path
                                            )

                                            is NavRoute.DeliveryNote -> navController.navigateSingle(
                                                NavRoute.DeliveryNote.path
                                            )

                                            is NavRoute.Reconciliation -> navController.navigateSingle(
                                                event.path
                                            )

                                            is NavRoute.Settings -> navController.navigateTopLevel(
                                                NavRoute.Settings.path
                                            )

                                            is NavRoute.PaymentEntry -> navController.navigateSingle(
                                                event.path
                                            )

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

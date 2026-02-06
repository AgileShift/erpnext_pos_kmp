@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.views.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.datetimeNow
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.utils.formatCurrency
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import kotlinx.coroutines.flow.MutableStateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import kotlin.math.ceil
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeState,
    actions: HomeAction,
) {
    var showOpeningView by remember { mutableStateOf(false) }
    var currentProfiles by remember { mutableStateOf(emptyList<POSProfileSimpleBO>()) }
    var currentUser by remember { mutableStateOf<UserBO?>(null) }
    val snackbar: SnackbarController = koinInject()
    val syncState by actions.syncState.collectAsState()
    val strings = LocalAppStrings.current
    val homeMetrics by actions.homeMetrics.collectAsState()
    val openingState by actions.openingState.collectAsState()
    val openingEntryId by actions.openingEntryId.collectAsState()
    val isCashboxOpen by actions.isCashboxOpen().collectAsState()
    val inventoryAlertMessage by actions.inventoryAlertMessage.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is HomeState.POSProfiles) {
            currentProfiles = uiState.posProfiles
            currentUser = uiState.user
        }
    }

    LaunchedEffect(isCashboxOpen) {
        if (isCashboxOpen && showOpeningView) {
            showOpeningView = false
        }
    }

    if (showOpeningView) {
        CashboxOpeningScreen(
            uiState = uiState,
            profiles = currentProfiles,
            user = currentUser,
            openingState = openingState,
            onLoadOpeningProfile = actions.onLoadOpeningProfile,
            onOpenCashbox = actions.onOpenCashbox,
            onSelectProfile = { actions.onPosSelected(it) },
            onDismiss = {
                showOpeningView = false
                actions.initialState()
            },
            snackbar
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        ) { paddingValues ->
            LaunchedEffect(inventoryAlertMessage) {
                val message = inventoryAlertMessage ?: return@LaunchedEffect
                snackbar.show(message, SnackbarType.Info, SnackbarPosition.Top)
                actions.onInventoryAlertConsumed()
            }
            Column(
                modifier = Modifier.padding(paddingValues).fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 12.dp, start = 12.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (uiState) {
                    is HomeState.Loading -> FullScreenLoadingIndicator()
                    is HomeState.Error -> FullScreenErrorMessage(
                        uiState.message, { actions.loadInitialData() })

                    is HomeState.POSProfiles -> {
                        // Saludo y banners
                        if (isCashboxOpen) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Text(
                                    "Bienvenido ${uiState.user.firstName}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = datetimeNow(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(24.dp))

                            BISection(
                                metrics = homeMetrics,
                                actions = actions,
                                modifier = Modifier.weight(1f).fillMaxWidth()
                            )

                            Spacer(Modifier.height(24.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                when (syncState) {
                                    is SyncState.SYNCING -> {
                                        // --- INICIO DE LA SECCIÓN MEJORADA ---

                                        // Creamos una transición infinita para la animación del icono.
                                        val infiniteTransition =
                                            rememberInfiniteTransition(label = "sync_icon_transition")
                                        val angle by infiniteTransition.animateFloat(
                                            initialValue = 0f,
                                            targetValue = 360f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(
                                                    durationMillis = 2000, easing = LinearEasing
                                                ), repeatMode = RepeatMode.Restart
                                            ),
                                            label = "sync_icon_rotation"
                                        )

                                        Card(
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            shape = MaterialTheme.shapes.large,
                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Icono animado
                                                Icon(
                                                    imageVector = Icons.Filled.Sync,
                                                    contentDescription = "Sincronizando",
                                                    modifier = Modifier.size(40.dp)
                                                        .rotate(angle), // Aplicamos la rotación
                                                    tint = MaterialTheme.colorScheme.primary
                                                )

                                                Spacer(Modifier.width(16.dp))

                                                // Columna para los textos
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "Sincronizando datos...",
                                                        style = MaterialTheme.typography.titleMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(Modifier.height(4.dp))
                                                    Text(
                                                        // Mostramos el mensaje específico de la sincronización
                                                        text = (syncState as SyncState.SYNCING).message,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                            alpha = 0.8f
                                                        )
                                                    )
                                                    val step =
                                                        (syncState as SyncState.SYNCING).currentStep
                                                    val total =
                                                        (syncState as SyncState.SYNCING).totalSteps
                                                    if (step != null && total != null && total > 0) {
                                                        Spacer(Modifier.height(6.dp))
                                                        Text(
                                                            text = "${strings.settings.syncStepLabel} $step ${strings.settings.syncStepOfLabel} $total",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Button(
                                                    onClick = { actions.cancelSync() },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = MaterialTheme.colorScheme.error,
                                                        contentColor = MaterialTheme.colorScheme.onError
                                                    )
                                                ) {
                                                    Text(strings.settings.syncCancelButton)
                                                }
                                            }
                                        }
                                        // --- FIN DE LA SECCIÓN MEJORADA ---
                                    }

                                    else -> {
                                        // No se muestra nada si no se está sincronizando
                                    }
                                }

                            }
                        } else {
                            Column(
                                modifier = Modifier.weight(6f).fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "¡Es hora de empezar a vender!",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.headlineMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                Spacer(Modifier.height(24.dp))
                                Icon(
                                    imageVector = Icons.Filled.ArrowDownward,
                                    contentDescription = "Abrir caja",
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (isCashboxOpen && !openingEntryId.isNullOrBlank()) {
                            Text(
                                text = "POE actual: $openingEntryId",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        // Botón abrir caja
                        if (!isCashboxOpen) {
                            Button(
                                onClick = {
                                    if (isCashboxOpen) {
                                        actions.onCloseCashbox()
                                    } else {
                                        if (currentProfiles.isNotEmpty()) {
                                            showOpeningView = true
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                enabled = syncState !is SyncState.SYNCING,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isCashboxOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                )
                            ) {
                                Text(
                                    text = if (isCashboxOpen) "Cerrar Caja" else "Abrir Caja",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                    )
                                )
                            }
                        }
                    }

                    else -> {}
                }

            }
        }
    }
}

@Composable
private fun BISection(
    metrics: HomeMetrics,
    actions: HomeAction,
    modifier: Modifier = Modifier
) {
    val currencyMetrics = metrics.currencyMetrics
    val strings = LocalAppStrings.current
    if (currencyMetrics.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Aún no hay métricas disponibles.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val preferredCurrency = metrics.salesTarget?.secondaryCurrency
        ?.takeIf { preferred -> currencyMetrics.any { it.currency.equals(preferred, true) } }
        ?: currencyMetrics.first().currency
    var selectedCurrency by remember(currencyMetrics, preferredCurrency) {
        mutableStateOf(preferredCurrency)
    }
    val selectedMetric = currencyMetrics.firstOrNull { it.currency == selectedCurrency }
        ?: currencyMetrics.first()
    val symbol = selectedMetric.currency.toCurrencySymbol().ifBlank { selectedMetric.currency }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currencyMetrics.forEach { metric ->
                    FilterChip(
                        selected = metric.currency == selectedCurrency,
                        onClick = { selectedCurrency = metric.currency },
                        label = { Text(metric.currency) })
                }
            }
        }

        item {
            HeroAndActionsRow(metric = selectedMetric, symbol = symbol, actions)
        }

        metrics.salesTarget?.let { target ->
            item {
                SalesTargetCard(
                    target = target,
                    metric = selectedMetric,
                    title = strings.settings.salesTargetSuggestedLabel,
                    monthlyLabel = strings.settings.salesTargetMonthlyLabel,
                    weeklyLabel = strings.settings.salesTargetWeeklyLabel,
                    dailyLabel = strings.settings.salesTargetDailyLabel,
                    staleHint = strings.settings.salesTargetConversionStaleHint,
                    missingHint = strings.settings.salesTargetConversionMissingHint
                )
            }
        }

        item {
            KpiRow(metric = selectedMetric, symbol = symbol)
        }

        item {
            InventoryAlertsCard(
                items = metrics.inventoryAlerts,
                onViewInventory = actions.onOpenSettings
            )
        }
    }
}

@Composable
private fun HeroAndActionsRow(metric: CurrencyHomeMetric, symbol: String, actions: HomeAction) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 840.dp
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiveSalesCard(
                    metric = metric, symbol = symbol, modifier = Modifier.weight(1f)
                )
                QuickActionsGrid(modifier = Modifier.weight(1f), actions)
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiveSalesCard(metric = metric, symbol = symbol, modifier = Modifier.fillMaxWidth())
                QuickActionsGrid(modifier = Modifier.fillMaxWidth(), actions)
            }
        }
    }
}

@Composable
private fun LiveSalesCard(
    metric: CurrencyHomeMetric, symbol: String, modifier: Modifier = Modifier
) {
    val target = if (metric.salesLast7 > 0.0) metric.salesLast7 / 7.0 else metric.totalSalesToday
    val progress = if (target > 0.0) (metric.totalSalesToday / target) else 0.0
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ventas en turno",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LivePill()
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "$symbol ${formatAmount(metric.totalSalesToday)}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Vs ayer: ${formatPercent(metric.compareVsYesterday)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Vs semana: ${formatPercent(metric.compareVsLastWeek)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.toFloat().coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Meta sugerida: $symbol ${formatAmount(target)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LivePill() {
    Row(
        modifier = Modifier.background(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(999.dp)
        ).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(6.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(50))
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "LIVE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun QuickActionsGrid(modifier: Modifier = Modifier, actions: HomeAction) {
    val actions = listOf(
        ActionItem(
            "Sincronizar ahora",
            Icons.Filled.Sync,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
            action = { actions.sync() }
        ), ActionItem(
            "Reconciliación",
            Icons.Filled.Shield,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary,
            action = { actions.onOpenReconciliation() }
        ), ActionItem(
            "Ajustes POS",
            Icons.Filled.Settings,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary,
            action = { actions.onOpenSettings() }
        ), ActionItem(
            "Cerrar caja",
            Icons.AutoMirrored.Filled.Logout,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
            action = { actions.onCloseCashbox() }
        ), ActionItem(
            "Cerrar sesión",
            Icons.Filled.Close,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            action = { actions.onLogout() }
        ), ActionItem(
            "Resumen diario",
            Icons.Filled.LocalOffer,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.onSurface,
            action = { actions.onOpenReconciliation() }
        )
    )
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Acciones rápidas",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            actions.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { action ->
                        QuickActionButton(action, Modifier.weight(1f))
                    }
                    if (row.size < 3) Spacer(Modifier.weight((3 - row.size).toFloat()))
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

private data class ActionItem(
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val contentColor: Color,
    val action: () -> Unit
)

@Composable
private fun QuickActionButton(action: ActionItem, modifier: Modifier = Modifier) {
    Button(
        onClick = action.action,
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = action.color, contentColor = action.contentColor
        )
    ) {
        Icon(
            imageVector = action.icon,
            contentDescription = null,
            tint = action.contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = action.label, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun KpiRow(metric: CurrencyHomeMetric, symbol: String) {
    val cards = listOf(
        KpiCell("Tickets", metric.invoicesToday.toString()),
        KpiCell("Ticket prom.", "$symbol ${formatAmount(metric.avgTicket)}"),
        KpiCell("Clientes", metric.customersToday.toString()),
        KpiCell("Pendiente", "$symbol ${formatAmount(metric.outstandingTotal)}")
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 840.dp
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cards.forEach { cell ->
                    KpiTile(
                        title = cell.title, value = cell.value, modifier = Modifier.weight(1f)
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cards.chunked(2).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { cell ->
                            KpiTile(
                                title = cell.title,
                                value = cell.value,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesTargetCard(
    target: SalesTargetMetric,
    metric: CurrencyHomeMetric,
    title: String,
    monthlyLabel: String,
    weeklyLabel: String,
    dailyLabel: String,
    staleHint: String,
    missingHint: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val targetInMetricCurrency = resolveTargetForCurrency(target, metric.currency)
            val targetCurrency = targetInMetricCurrency?.currency ?: target.baseCurrency
            val dailyGoal = roundUpTarget(targetInMetricCurrency?.daily ?: target.dailyBase)
            val weeklyGoal = roundUpTarget(targetInMetricCurrency?.weekly ?: target.weeklyBase)
            val monthlyGoal = roundUpTarget(targetInMetricCurrency?.monthly ?: target.monthlyBase)

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            val monthlyProjection = metric.salesLast7 * (30.0 / 7.0)
            val rows = listOf(
                TargetProgressRow(
                    label = dailyLabel,
                    actual = metric.totalSalesToday,
                    goal = dailyGoal,
                    currency = targetCurrency,
                    context = "Objetivo del día"
                ),
                TargetProgressRow(
                    label = weeklyLabel,
                    actual = metric.salesLast7,
                    goal = weeklyGoal,
                    currency = targetCurrency,
                    context = "Últimos 7 días"
                ),
                TargetProgressRow(
                    label = monthlyLabel,
                    actual = monthlyProjection,
                    goal = monthlyGoal,
                    currency = targetCurrency,
                    context = "Proyección mensual por ritmo semanal"
                )
            )

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 900.dp
                if (isWide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { row ->
                            TargetProgressTile(row = row, modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { row ->
                            TargetProgressTile(row = row, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            if (targetInMetricCurrency == null && !metric.currency.equals(
                    target.baseCurrency,
                    true
                )
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No hay conversión local ${target.baseCurrency} -> ${metric.currency}. Se muestran metas base.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (target.secondaryCurrency != null && target.monthlySecondary == null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = missingHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (target.conversionStale) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = staleHint,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

private data class TargetProgressRow(
    val label: String,
    val actual: Double,
    val goal: Double,
    val currency: String,
    val context: String
)

private data class TargetCurrencyValues(
    val currency: String,
    val monthly: Double,
    val weekly: Double,
    val daily: Double
)

private fun roundUpTarget(value: Double): Double = ceil(value)

private fun resolveTargetForCurrency(
    target: SalesTargetMetric,
    currency: String
): TargetCurrencyValues? {
    if (currency.equals(target.baseCurrency, ignoreCase = true)) {
        return TargetCurrencyValues(
            currency = target.baseCurrency,
            monthly = target.monthlyBase,
            weekly = target.weeklyBase,
            daily = target.dailyBase
        )
    }
    if (currency.equals(target.secondaryCurrency, ignoreCase = true)) {
        val monthly = target.monthlySecondary ?: return null
        val weekly = target.weeklySecondary ?: return null
        val daily = target.dailySecondary ?: return null
        return TargetCurrencyValues(
            currency = currency,
            monthly = monthly,
            weekly = weekly,
            daily = daily
        )
    }
    return null
}

@Composable
private fun TargetProgressTile(row: TargetProgressRow, modifier: Modifier = Modifier) {
    val roundedGoal = roundUpTarget(row.goal)
    val progressRaw =
        if (roundedGoal <= 0.0) 0f else (row.actual / roundedGoal).toFloat().coerceIn(0f, 1f)
    val progress by animateFloatAsState(
        targetValue = progressRaw,
        animationSpec = tween(durationMillis = 700),
        label = "target_progress"
    )
    val reached = row.actual >= roundedGoal
    val pending = (roundedGoal - row.actual).coerceAtLeast(0.0)
    val progressColor = Color(0xFF2E7D32)
    val trackColor = Color(0xFFD32F2F).copy(alpha = 0.30f)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.35f
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = row.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${formatCurrency(row.currency, row.actual)} / ${
                    formatCurrency(
                        row.currency,
                        roundedGoal
                    )
                }",
                style = MaterialTheme.typography.bodyMedium
            )
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = progressColor,
                trackColor = trackColor
            )
            Text(
                text = if (reached) "Meta alcanzada" else "Faltan ${
                    formatCurrency(
                        row.currency,
                        pending
                    )
                }",
                style = MaterialTheme.typography.labelSmall,
                color = if (reached) progressColor else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = row.context,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class KpiCell(val title: String, val value: String)

@Composable
private fun KpiTile(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Preview
@Composable
fun InventoryAlertsCardPreview() {
    InventoryAlertsCard(
        listOf(
            InventoryAlert(
                "123123",
                "Test",
                10.0,
                InventoryAlertStatus.LOW,
                6.0,
                20.0
            ),
            InventoryAlert(
                "123123123",
                "Test",
                10.0,
                InventoryAlertStatus.LOW,
                6.0,
                20.0
            )
        ),
        {}
    )
}

@Composable
private fun InventoryAlertsCard(
    items: List<InventoryAlert>, onViewInventory: () -> Unit
) {
    val criticalCount = items.count { it.status == InventoryAlertStatus.CRITICAL }
    val lowCount = items.count { it.status == InventoryAlertStatus.LOW }
    val titleBrush = Brush.linearGradient(
        colors = listOf(
            MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
        )
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Inventario en riesgo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Monitoreo del almacén en tiempo real",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier.background(
                        brush = titleBrush, shape = RoundedCornerShape(999.dp)
                    ).border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(999.dp)
                    ).padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Activa",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SeverityBadge(
                    "Crítico",
                    criticalCount,
                    MaterialTheme.colorScheme.error,
                    Modifier.weight(1f)
                )
                SeverityBadge(
                    "Bajo",
                    lowCount,
                    MaterialTheme.colorScheme.tertiary,
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(10.dp)
                ).padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Tooltip: cobertura = stock disponible frente al nivel de reorden (100% o más está saludable).",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(14.dp)
                    ).padding(14.dp)
                ) {
                    Text(
                        text = "Sin alertas de inventario",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Mostrando ${items.size} productos con mayor riesgo de quiebre.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val isWide = maxWidth >= 900.dp
                    if (isWide) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    row.forEach { item ->
                                        InventoryAlertRow(item, Modifier.weight(1f))
                                    }
                                    if (row.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            items.forEach { item ->
                                InventoryAlertRow(item, Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onViewInventory,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Text("Configurar alertas")
            }
        }
    }
}

@Composable
private fun SeverityBadge(
    label: String,
    value: Int,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = tint)
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = tint,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview
@Composable
fun InventoryAlertRowPreview() {
    InventoryAlertRow(InventoryAlert("123123", "Test", 10.0, InventoryAlertStatus.LOW, 6.0, 20.0))
}

@Composable
private fun InventoryAlertRow(item: InventoryAlert, modifier: Modifier = Modifier) {
    val statusColor = when (item.status) {
        InventoryAlertStatus.CRITICAL -> MaterialTheme.colorScheme.error
        InventoryAlertStatus.LOW -> MaterialTheme.colorScheme.tertiary
    }
    val statusLabel = when (item.status) {
        InventoryAlertStatus.CRITICAL -> "Crítico"
        InventoryAlertStatus.LOW -> "Bajo"
    }
    val reorderLevel = item.reorderLevel
    val coverageRatio = reorderLevel?.takeIf { it > 0.0 }?.let { item.qty / it }
    val progressRaw = coverageRatio?.coerceIn(0.0, 1.0)?.toFloat()
    val progress by animateFloatAsState(
        targetValue = progressRaw ?: 0f,
        animationSpec = tween(durationMillis = 650),
        label = "inventory_coverage"
    )
    val progressColor = when {
        coverageRatio == null -> MaterialTheme.colorScheme.outline
        coverageRatio >= 1.0 -> Color(0xFF2E7D32)
        coverageRatio >= 0.6 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val missingToReorder = reorderLevel?.let { (it - item.qty).coerceAtLeast(0.0) }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.fillMaxWidth(0.74f)) {
                    Text(
                        item.itemName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        item.itemCode,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier.background(
                        color = statusColor.copy(alpha = 0.13f),
                        shape = RoundedCornerShape(999.dp)
                    ).border(
                        width = 1.dp,
                        color = statusColor.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(999.dp)
                    ).padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (progressRaw != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cobertura vs reorden",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${formatAmount((coverageRatio * 100.0).coerceAtLeast(0.0))}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = progressColor
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            } else {
                Text(
                    text = "Sin nivel de reorden configurado para este artículo.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Stock: ${formatAmount(item.qty)}", style = MaterialTheme.typography.bodySmall
                )
                Text("Reorden: ${item.reorderLevel?.let { formatAmount(it) } ?: "N/D"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            missingToReorder?.let {
                Text(
                    text = if (it <= 0.0) "Sobre nivel de reorden" else "Faltan ${formatAmount(it)} para llegar al nivel de reorden",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatAmount(value: Double): String = formatDoubleToString(value, 2)

private fun formatPercent(value: Double?): String {
    if (value == null) return "N/D"
    val sign = if (value >= 0) "+" else ""
    return "$sign${formatDoubleToString(value, 1)}%"
}

@Composable
private fun FullScreenErrorMessage(
    errorMessage: String, onRetry: () -> Unit, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Filled.CloudOff,
                "Error",
                Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Text(
                errorMessage,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text("Reintentar") }
        }
    }
}

@Composable
private fun FullScreenLoadingIndicator(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.Center),
            trackColor = Color.Blue,
            color = Color.Cyan,
            strokeWidth = 2.dp
        )
    }
}

@Composable
@Preview
fun HomePreview() {
    MaterialTheme {
        HomeScreen(

            HomeState.POSProfiles(
                listOf(), UserBO(firstName = "Ruta Ciudad Sandino")
            ), HomeAction(
                isCashboxOpen = { MutableStateFlow(true) },
                syncState = MutableStateFlow(SyncState.SYNCING("Categoria de productos")),
                syncSettings = MutableStateFlow(
                    SyncSettings(
                        autoSync = true,
                        syncOnStartup = true,
                        wifiOnly = false,
                        lastSyncAt = Clock.System.now().toEpochMilliseconds(),
                        useTtl = false
                    )
                ),
                homeMetrics = MutableStateFlow(HomeMetrics())
            )
        )
    }
}

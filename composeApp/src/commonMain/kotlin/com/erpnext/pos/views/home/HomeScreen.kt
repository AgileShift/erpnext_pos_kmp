@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.views.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Logout
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.datetimeNow
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
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
    val homeMetrics by actions.homeMetrics.collectAsState()
    val openingState by actions.openingState.collectAsState()
    val isCashboxOpen by actions.isCashboxOpen().collectAsState()
    val inventoryAlertMessage by actions.inventoryAlertMessage.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is HomeState.POSProfiles) {
            currentProfiles = uiState.posProfiles
            currentUser = uiState.user
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
                actions.initialState()
                showOpeningView = false
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
                        uiState.message,
                        { actions.loadInitialData() })

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

                            BISection(metrics = homeMetrics)

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
                                                    durationMillis = 2000,
                                                    easing = LinearEasing
                                                ),
                                                repeatMode = RepeatMode.Restart
                                            ),
                                            label = "sync_icon_rotation"
                                        )

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            shape = MaterialTheme.shapes.large,
                                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Icono animado
                                                Icon(
                                                    imageVector = Icons.Filled.Sync,
                                                    contentDescription = "Sincronizando",
                                                    modifier = Modifier
                                                        .size(40.dp)
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
                                modifier = Modifier
                                    .weight(6f)
                                    .fillMaxSize(),
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

                        Spacer(Modifier.weight(1f))

                        // Botón abrir caja
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

                    else -> {}
                }

            }
        }
    }
}

@Composable
private fun BISection(metrics: HomeMetrics) {
    val currencyMetrics = metrics.currencyMetrics
    Column(
        modifier = Modifier.fillMaxWidth()
            .verticalScroll(rememberScrollState(), true),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (currencyMetrics.isEmpty()) {
            Text(
                text = "Aún no hay métricas disponibles.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return@Column
        }

        var selectedCurrency by remember(currencyMetrics) {
            mutableStateOf(currencyMetrics.first().currency)
        }
        val selectedMetric = currencyMetrics.firstOrNull { it.currency == selectedCurrency }
            ?: currencyMetrics.first()
        val symbol = selectedMetric.currency.toCurrencySymbol()
            .ifBlank { selectedMetric.currency }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            currencyMetrics.forEach { metric ->
                FilterChip(
                    selected = metric.currency == selectedCurrency,
                    onClick = { selectedCurrency = metric.currency },
                    label = { Text(metric.currency) }
                )
            }
        }

        HeroAndActionsRow(metric = selectedMetric, symbol = symbol)

        KpiRow(metric = selectedMetric, symbol = symbol)

        InventoryAlertsCard(
            items = metrics.inventoryAlerts,
            onViewInventory = {}
        )
    }
}

@Composable
private fun HeroAndActionsRow(metric: CurrencyHomeMetric, symbol: String) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val isWide = maxWidth >= 840.dp
        if (isWide) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiveSalesCard(
                    metric = metric,
                    symbol = symbol,
                    modifier = Modifier.weight(1f)
                )
                QuickActionsGrid(modifier = Modifier.weight(1f))
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LiveSalesCard(metric = metric, symbol = symbol, modifier = Modifier.fillMaxWidth())
                QuickActionsGrid(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun LiveSalesCard(
    metric: CurrencyHomeMetric,
    symbol: String,
    modifier: Modifier = Modifier
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
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
                progress = progress.toFloat().coerceIn(0f, 1f),
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
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
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
private fun QuickActionsGrid(modifier: Modifier = Modifier) {
    val actions = listOf(
        ActionItem(
            "Nueva venta",
            Icons.Filled.Add,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary
        ),
        ActionItem(
            "Anular",
            Icons.Filled.Close,
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError
        ),
        ActionItem(
            "Override",
            Icons.Filled.Shield,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        ),
        ActionItem(
            "Precio",
            Icons.Filled.LocalOffer,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.onTertiary
        ),
        ActionItem(
            "Break",
            Icons.Filled.LocalCafe,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.onSecondary
        ),
        ActionItem(
            "Cerrar turno",
            Icons.Filled.Logout,
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.onSurface
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
    val contentColor: Color
)

@Composable
private fun QuickActionButton(action: ActionItem, modifier: Modifier = Modifier) {
    Button(
        onClick = {},
        modifier = modifier.height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = action.color,
            contentColor = action.contentColor
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
            text = action.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
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
                        title = cell.title,
                        value = cell.value,
                        modifier = Modifier.weight(1f)
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

@Composable
private fun InventoryAlertsCard(
    items: List<InventoryAlert>,
    onViewInventory: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                    text = "Inventory alerts",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Live",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            if (items.isEmpty()) {
                Text(
                    text = "Sin alertas de inventario",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                InventoryAlertsTable(items)
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onViewInventory,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Text("Ver inventario")
            }
        }
    }
}

@Composable
private fun InventoryAlertsTable(items: List<InventoryAlert>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Item", style = MaterialTheme.typography.labelSmall)
            Text("Stock", style = MaterialTheme.typography.labelSmall)
            Text("Estado", style = MaterialTheme.typography.labelSmall)
            Text("Reorden", style = MaterialTheme.typography.labelSmall)
        }
        items.forEach { item ->
            val statusColor = when (item.status) {
                InventoryAlertStatus.CRITICAL -> MaterialTheme.colorScheme.error
                InventoryAlertStatus.LOW -> MaterialTheme.colorScheme.tertiary
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.itemName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(item.itemCode, style = MaterialTheme.typography.labelSmall)
                }
                Text(formatAmount(item.qty))
                Text(item.status.name, color = statusColor)
                Text(item.reorderLevel?.let { formatAmount(it) } ?: "N/D")
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
        modifier = modifier.fillMaxSize().padding(16.dp),
        contentAlignment = Alignment.Center
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
        val _stateFlow: MutableStateFlow<SyncState> =
            MutableStateFlow(SyncState.SYNCING("Categoria de Productos"))
        val stateFlow = _stateFlow.asStateFlow()
        HomeScreen(

            HomeState.POSProfiles(
                listOf(),
                UserBO(firstName = "Ruta Ciudad Sandino")
            ),
            HomeAction(
                isCashboxOpen = { MutableStateFlow(true) },
                syncState = stateFlow,
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

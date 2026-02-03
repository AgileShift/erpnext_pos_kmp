@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.views.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import kotlin.math.max
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
    val cashboxManager: CashBoxManager = koinInject()

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
        /*Text(
            text = "Business Insights",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )*/
        if (currencyMetrics.isEmpty()) {
            Text(
                text = "No currency metrics available.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Sales today",
                    value = "$symbol ${formatAmount(selectedMetric.totalSalesToday)}",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Outstanding",
                    value = "$symbol ${formatAmount(selectedMetric.outstandingTotal)}",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Sales last 7 days",
                    value = "$symbol ${formatAmount(selectedMetric.salesLast7)}",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Average ticket",
                    value = "$symbol ${formatAmount(selectedMetric.avgTicket)}",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Margin today",
                    value = formatMargin(
                        selectedMetric.marginToday,
                        selectedMetric.marginTodayPercent,
                        symbol
                    ),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Margin 7 days",
                    value = formatMargin(
                        selectedMetric.marginLast7,
                        selectedMetric.marginLast7Percent,
                        symbol
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Invoices today",
                    value = selectedMetric.invoicesToday.toString(),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Customers today",
                    value = selectedMetric.customersToday.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val outstandingRatio = if (selectedMetric.salesLast7 > 0.0) {
                    (selectedMetric.outstandingTotal / selectedMetric.salesLast7) * 100.0
                } else {
                    null
                }
                MetricCard(
                    title = "Outstanding ratio (7d)",
                    value = formatPercent(outstandingRatio),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Cost coverage",
                    value = formatPercent(selectedMetric.costCoveragePercent),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "Vs yesterday",
                    value = formatPercent(selectedMetric.compareVsYesterday),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "Vs last week",
                    value = formatPercent(selectedMetric.compareVsLastWeek),
                    modifier = Modifier.weight(1f)
                )
            }
            SalesLineChart(selectedMetric.weekSeries)
        }
        TopProductsCard(metrics.topProducts, "C$")
        TopProductsByMarginCard(metrics.topProductsByMargin, "C$")
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SalesLineChart(series: List<DailyMetric>) {
    val totals = series.map { it.total }
    val maxValue = max(totals.maxOrNull() ?: 0.0, 1.0)
    val lineColor = MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Ventas últimos 7 días",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                if (series.isEmpty()) return@Canvas
                val stepX = size.width / (series.size - 1).coerceAtLeast(1)
                val path = Path()
                series.forEachIndexed { index, item ->
                    val ratio = (item.total / maxValue).toFloat()
                    val x = stepX * index
                    val y = size.height - (size.height * ratio)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 4f, cap = StrokeCap.Round)
                )
            }
            if (series.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(series.first().date, style = MaterialTheme.typography.labelSmall)
                    Text(series.last().date, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun TopProductsCard(products: List<TopProductMetric>, currencySymbol: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Top productos (7 días)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            if (products.isEmpty()) {
                Text(
                    text = "Sin datos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                products.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${item.itemName ?: item.itemCode}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Qty ${formatAmount(item.qty)} • ${item.itemCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$currencySymbol ${formatAmount(item.total)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (index != products.lastIndex) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TopProductsByMarginCard(
    products: List<TopProductMarginMetric>,
    currencySymbol: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Top productos por margen (7 días)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            if (products.isEmpty()) {
                Text(
                    text = "Sin datos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                products.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${index + 1}. ${item.itemName ?: item.itemCode}",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Qty ${formatAmount(item.qty)} • ${item.itemCode}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "$currencySymbol ${formatAmount(item.margin)} (${
                                formatPercent(
                                    item.marginPercent
                                )
                            })",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.End
                        )
                    }
                    if (index != products.lastIndex) {
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

private fun resolveHomeCurrencySymbol(
    cashboxManager: CashBoxManager,
    profiles: List<POSProfileSimpleBO>
): String {
    val ctx = cashboxManager.getContext()
    val currency = ctx?.currency ?: profiles.firstOrNull()?.currency ?: "USD"
    return currency.toCurrencySymbol()
}

private fun formatAmount(value: Double): String = formatDoubleToString(value, 2)

private fun formatPercent(value: Double?): String {
    if (value == null) return "N/D"
    val sign = if (value >= 0) "+" else ""
    return "$sign${formatDoubleToString(value, 1)}%"
}

private fun formatMargin(
    margin: Double?,
    percent: Double?,
    currencySymbol: String
): String {
    if (margin == null || percent == null) return "N/D"
    return "$currencySymbol ${formatAmount(margin)} (${formatPercent(percent)})"
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

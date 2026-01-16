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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.domain.models.POSProfileSimpleBO
import com.erpnext.pos.domain.models.PaymentModesBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.sync.SyncState
import kotlinx.coroutines.delay
import com.erpnext.pos.utils.datetimeNow
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.formatDoubleToString
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.PaymentModeWithAmount
import io.ktor.client.request.invoke
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
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
    var showDialog by remember { mutableStateOf(false) }
    var currentProfiles by remember { mutableStateOf(emptyList<POSProfileSimpleBO>()) }
    val syncState by actions.syncState.collectAsState()
    val syncSettings by actions.syncSettings.collectAsState()
    val homeMetrics by actions.homeMetrics.collectAsState()
    val isCashboxOpen by actions.isCashboxOpen().collectAsState()
    val appStrings = LocalAppStrings.current
    val networkMonitor: NetworkMonitor = koinInject()
    val cashboxManager: CashBoxManager = koinInject()
    val isOnline by networkMonitor.isConnected.collectAsState(false)
    val shiftStart by cashboxManager.activeCashboxStart().collectAsState(null)
    var profileMenuExpanded by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            tick = Clock.System.now().toEpochMilliseconds()
            delay(1000)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is HomeState.POSProfiles) {
            currentProfiles = uiState.posProfiles
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.fillMaxWidth(),
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ERPNext POS",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                            )
                        )
                        ShiftOpenChip(
                            isOpen = isCashboxOpen,
                            duration = formatShiftDuration(shiftStart, tick),
                            closeAction = actions.onCloseCashbox
                        )
                    }
                },
                actions = {
                    val isRecentlySynced =
                        syncSettings.lastSyncAt?.let { tick - it < 10 * 60 * 1000 } == true
                    val dbHealthy = isOnline && isRecentlySynced && syncState !is SyncState.ERROR
                    val dbTint = when {
                        syncState is SyncState.SYNCING -> Color(0xFFF59E0B)
                        syncState is SyncState.ERROR -> MaterialTheme.colorScheme.error
                        dbHealthy -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val dbLabel = when (syncState) {
                        is SyncState.SYNCING -> "Base de datos: Sincronizando"
                        is SyncState.ERROR -> "Base de datos: Error de sincronización"
                        is SyncState.SUCCESS -> "Base de datos: Sincronizada"
                        else -> if (dbHealthy) "Base de datos: Saludable" else "Base de datos: Pendiente"
                    }
                    StatusIconButton(
                        label = if (isOnline) "Internet: Conectado" else "Internet: Sin conexión",
                        onClick = {},
                        enabled = false,
                        tint = if (isOnline) Color(0xFF2E7D32)
                        else MaterialTheme.colorScheme.error,
                    ) {
                        Icon(
                            if (isOnline) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                            contentDescription = null
                        )
                    }
                    StatusIconButton(
                        label = dbLabel,
                        onClick = { actions.sync() },
                        tint = dbTint,
                    ) {
                        if (syncState is SyncState.SYNCING) {
                            CircularProgressIndicator(Modifier.size(18.dp))
                        } else {
                            Icon(Icons.Filled.Storage, contentDescription = null)
                        }
                    }
                    StatusIconButton(
                        label = "Refrescar",
                        onClick = { actions.loadInitialData() },
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                    }
                    val printerConnected = false
                    StatusIconButton(
                        label = if (printerConnected) "Impresora: Conectada" else "Impresora: Sin conexión",
                        onClick = {},
                        enabled = false,
                        tint = if (printerConnected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null)
                    }
                    StatusIconButton(
                        label = "Perfil",
                        onClick = { profileMenuExpanded = true },
                    ) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = profileMenuExpanded,
                        onDismissRequest = { profileMenuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Configuración") },
                            onClick = {
                                profileMenuExpanded = false
                                actions.onOpenSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(appStrings.navigation.reconciliation) },
                            onClick = {
                                profileMenuExpanded = false
                                actions.onOpenReconciliation()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Cerrar sesión") },
                            onClick = {
                                profileMenuExpanded = false
                                actions.onLogout()
                            }
                        )
                    }
                }
            )
            if (syncState is SyncState.SYNCING) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
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

                        // Tarjetas resumen
                        /*Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Stock pendiente por recibir",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("Tienes 3 productos en espera")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Filled.Warehouse, contentDescription = null)
                                }
                            }

                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            "Recarga pendiente por recibir",
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text("Tienes 2 recargas en espera")
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Filled.CreditCard, contentDescription = null)
                                }
                            }
                        }

                        Spacer(Modifier.height(36.dp)) */

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
                                    showDialog = true
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

            if (showDialog && currentProfiles.isNotEmpty()) {
                POSProfileDialog(
                    uiState = uiState,
                    profiles = currentProfiles,
                    onSelectProfile = { actions.onPosSelected(it) },
                    onOpenCashbox = { pos, amounts ->
                        actions.openCashbox(pos, amounts)
                    },
                    onDismiss = {
                        actions.initialState()
                        showDialog = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ShiftOpenChip(isOpen: Boolean, duration: String, closeAction: () -> Unit = {}) {
    val openBg = Color(0xFFE8F5E9)
    val openText = Color(0xFF2E7D32)
    val closedBg = Color(0xFFFFEBEE)
    val closedText = Color(0xFFC62828)
    Surface(
        color = if (isOpen) openBg else closedBg,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (isOpen) openText else closedText,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Shift Open: ${if (isOpen) duration else "--"}",
                Modifier.clickable(
                    enabled = isOpen,
                    onClickLabel = "Close Shift",
                    interactionSource = MutableInteractionSource()
                ) {
                    closeAction()
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (isOpen) openText else closedText
            )
        }
    }
}

private fun parseLooseLocalDateTime(input: String): LocalDateTime? {
    val s = input.trim().replace("T", " ")
    val parts = s.split(" ", limit = 2)

    val date = parts.getOrNull(0) ?: return null
    val time = parts.getOrNull(1) ?: "00:00:00"

    val d = date.split("-")
    if (d.size != 3) return null

    val t = time.split(":")
    val year = d[0].toIntOrNull() ?: return null
    val month = d[1].toIntOrNull() ?: return null
    val day = d[2].toIntOrNull() ?: return null

    val hour = t.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = t.getOrNull(1)?.toIntOrNull() ?: 0
    val second = t.getOrNull(2)?.toIntOrNull() ?: 0

    return LocalDateTime(
        year = year,
        month = Month(month).ordinal + 1,
        day = day,
        hour = hour,
        minute = minute,
        second = second,
        nanosecond = 0,
    )
}

private fun formatShiftDuration(start: String?, nowMillis: Long): String {
    return try {
        if (start.isNullOrBlank()) return "--"

        val ldt = parseLooseLocalDateTime(start) ?: return "--"
        val instant = ldt.toInstant(TimeZone.currentSystemDefault())

        val diffMillis = nowMillis - instant.toEpochMilliseconds()
        if (diffMillis < 0) return "--"

        val totalSeconds = diffMillis / 1_000
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60

        "${hours}h ${minutes}m ${seconds}s"
    } catch (e: Exception) {
        e.printStackTrace()
        "--"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatusIconButton(
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onClick, enabled = enabled) {
            CompositionLocalProvider(LocalContentColor provides tint) {
                content()
            }
        }
    }
}

@Composable
private fun BISection(metrics: HomeMetrics) {
    val currencyMetrics = metrics.currencyMetrics
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Business Insights",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
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

            Text(
                text = "Currency focus: ${selectedMetric.currency}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Metrics are shown per selected currency to avoid duplicated totals.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
        //TopProductsCard(metrics.topProducts, currencySymbol)
        //TopProductsByMarginCard(metrics.topProductsByMargin, currencySymbol)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun POSProfileDialog(
    uiState: HomeState,
    profiles: List<POSProfileSimpleBO>,
    onSelectProfile: (POSProfileSimpleBO) -> Unit,
    onOpenCashbox: (POSProfileSimpleBO, List<PaymentModeWithAmount>) -> Unit,
    onDismiss: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var selectedProfile by remember { mutableStateOf<POSProfileSimpleBO?>(null) }
    var paymentAmounts by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(profiles) {
        if (profiles.size == 1) {
            selectedProfile = profiles.first()
            onSelectProfile(profiles.first())
            expanded = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (uiState) {
                    is HomeState.POSInfoLoading -> "Cargando configuración..."
                    is HomeState.POSInfoLoaded -> "Balance de Apertura"
                    else -> "Seleccione un POS:"
                },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = selectedProfile?.name ?: "Seleccionar POS",
                        onValueChange = {},
                        label = { Text("Perfil POS") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.textFieldColors()
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier
                            .exposedDropdownSize()
                            .heightIn(max = 250.dp)
                    ) {
                        profiles.forEach { profile ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(profile.name, fontWeight = FontWeight.Bold)
                                        Text(
                                            profile.company,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                onClick = {
                                    selectedProfile = profile
                                    onSelectProfile(profile)
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
                when (uiState) {
                    is HomeState.POSInfoLoading -> {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                trackColor = Color.Blue,
                                color = Color.Cyan,
                                strokeWidth = 2.dp
                            )
                        }
                    }

                    is HomeState.POSInfoLoaded -> {
                        val modes = uiState.info.paymentModes
                        LazyColumn(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(modes) { mode ->
                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    elevation = CardDefaults.cardElevation(0.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            mode.modeOfPayment,
                                            modifier = Modifier.width(125.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            softWrap = true,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.Bold,
                                            )
                                        )
                                        NumericCurrencyTextField(
                                            value = paymentAmounts[mode.name] ?: "",
                                            onValueChange = {
                                                paymentAmounts =
                                                    paymentAmounts.toMutableMap().apply {
                                                        put(mode.name, it)
                                                    }
                                            },
                                            placeholder = "0.0",
                                            modifier = Modifier.width(100.dp),
                                            currencySymbol = uiState.currency.toCurrencySymbol()
                                        )
                                    }
                                }
                            }
                        }
                    }

                    else -> {}
                }
            }
        },
        confirmButton = {
            when (uiState) {
                is HomeState.POSProfiles -> {
                    TextButton(onClick = onDismiss) { Text("Cerrar") }
                }

                is HomeState.POSInfoLoaded -> {
                    Row {
                        TextButton(onClick = { onDismiss() }) { Text("Cancelar") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val amounts = uiState.info.paymentModes.map {
                                    PaymentModeWithAmount(
                                        mode = it,
                                        amount = paymentAmounts[it.name]?.toDoubleOrNull() ?: 0.0
                                    )
                                }
                                selectedProfile?.let { profile ->
                                    onOpenCashbox(profile, amounts)
                                    onDismiss()
                                }
                            }
                        ) {
                            Text("Abrir Caja")
                        }
                    }
                }

                else -> {}
            }
        }
    )
}

@Composable
fun NumericCurrencyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    currencySymbol: String = "C$",
    placeholder: String = "0.00"
) {
    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            val filtered = newValue.filter { it.isDigit() || it == '.' }
            val finalValue = if (filtered.count { it == '.' } > 1) {
                filtered.dropLast(1)
            } else filtered
            onValueChange(finalValue)
        },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Text(
                text = currencySymbol,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Black
                ),
                color = MaterialTheme.colorScheme.primary
            )
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        textStyle = MaterialTheme.typography.bodySmall.copy(
            textAlign = TextAlign.End,
            fontWeight = FontWeight.Black,
        )
    )
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
                        lastSyncAt = Clock.System.now().toEpochMilliseconds()
                    )
                ),
                homeMetrics = MutableStateFlow(HomeMetrics())
            )
        )
    }
}

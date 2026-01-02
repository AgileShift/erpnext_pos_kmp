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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OnlinePrediction
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warehouse
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
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
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.datetimeNow
import com.erpnext.pos.utils.toErpDateTime
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.views.PaymentModeWithAmount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.compose.ui.tooling.preview.Preview

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
    val isCashboxOpen by actions.isCashboxOpen().collectAsState()

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
                    Text(
                        text = "ERPNext POS",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp
                        )
                    )
                },
                actions = {
                    IconButton(onClick = { actions.onLogout() }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout")
                    }
                    IconButton(onClick = { actions.loadInitialData() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = { actions.sync() }) {
                        Icon(
                            Icons.Filled.OnlinePrediction, contentDescription = "Online Prediction"
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

                        SyncCenterCard(
                            syncState = syncState,
                            settings = syncSettings,
                            onSyncNow = actions.sync,
                            onAutoSyncChanged = actions.onAutoSyncChanged,
                            onSyncOnStartupChanged = actions.onSyncOnStartupChanged,
                            onWifiOnlyChanged = actions.onWifiOnlyChanged
                        )

                        Spacer(Modifier.height(24.dp))

                        // Tarjetas resumen
                        Column(
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
                                    Icon(Icons.Default.Warehouse, contentDescription = null)
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
                                    Icon(Icons.Default.CreditCard, contentDescription = null)
                                }
                            }
                        }

                        Spacer(Modifier.height(36.dp))

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
                                                imageVector = Icons.Default.Sync,
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
                                actions.closeCashbox()
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
private fun SyncCenterCard(
    syncState: SyncState,
    settings: SyncSettings,
    onSyncNow: () -> Unit,
    onAutoSyncChanged: (Boolean) -> Unit,
    onSyncOnStartupChanged: (Boolean) -> Unit,
    onWifiOnlyChanged: (Boolean) -> Unit
) {
    val lastSyncLabel = settings.lastSyncAt?.toErpDateTime() ?: "Never synced"
    val statusLabel = when (syncState) {
        SyncState.IDLE -> "Idle"
        SyncState.SUCCESS -> "Synced"
        is SyncState.ERROR -> "Error"
        is SyncState.SYNCING -> "Syncing"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Synchronization",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Status: $statusLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Last sync: $lastSyncLabel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(onClick = onSyncNow) {
                    Text("Sync now")
                }
            }

            HorizontalDivider()

            SyncToggleRow(
                label = "Auto-sync",
                checked = settings.autoSync,
                onCheckedChange = onAutoSyncChanged
            )
            SyncToggleRow(
                label = "Sync on startup",
                checked = settings.syncOnStartup,
                onCheckedChange = onSyncOnStartupChanged
            )
            SyncToggleRow(
                label = "Wi-Fi only",
                checked = settings.wifiOnly,
                onCheckedChange = onWifiOnlyChanged
            )
        }
    }
}

@Composable
private fun SyncToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
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
                syncState = stateFlow
            )
        )
    }
}

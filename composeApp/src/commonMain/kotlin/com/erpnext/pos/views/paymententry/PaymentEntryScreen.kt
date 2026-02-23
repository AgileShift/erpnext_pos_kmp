@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.views.paymententry

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.erpnext.pos.utils.loading.LoadingIndicator
import com.erpnext.pos.utils.loading.LoadingUiState
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.views.billing.MoneyTextField
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentEntryScreen(
    state: PaymentEntryState,
    action: PaymentEntryAction
) {
    val snackbar = koinInject<SnackbarController>()
    val loadingState by LoadingIndicator.state.collectAsState(initial = LoadingUiState())
    val globalBusy = loadingState.isLoading
    val colorScheme = MaterialTheme.colorScheme
    val fieldShape = RoundedCornerShape(12.dp)
    val fieldColors = paymentEntryFieldColors()

    state.errorMessage?.let {
        snackbar.show(it, SnackbarType.Error, SnackbarPosition.Top)
    }

    state.successMessage?.let {
        snackbar.show(it, SnackbarType.Success, SnackbarPosition.Top)
    }

    Scaffold(containerColor = colorScheme.surfaceVariant.copy(alpha = 0.20f)) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (state.offlineModeEnabled || !state.isOnline) {
                val offlineMessage = when {
                    state.offlineModeEnabled && !state.isOnline ->
                        "Modo offline activo y sin Internet. Este módulo solo funciona en línea."
                    state.offlineModeEnabled ->
                        "Modo offline activo. Desactívalo en Configuraciones para continuar."
                    else ->
                        "No hay conexión a Internet. Este módulo solo funciona en línea."
                }
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Text(
                        text = offlineMessage,
                        color = colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (state.entryType == PaymentEntryType.Receive) {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Documento", style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = state.invoiceId,
                            onValueChange = action.onInvoiceIdChanged,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Factura") },
                            shape = fieldShape,
                            colors = fieldColors,
                            readOnly = true,
                            singleLine = true
                        )
                    }
                }
            }

            when (state.entryType) {
                PaymentEntryType.InternalTransfer -> {
                    TransferFlowSection(
                        sourceValue = state.sourceAccount,
                        destinationValue = state.targetAccount,
                        options = state.accountOptions,
                        onSourceSelected = action.onSourceAccountChanged,
                        onDestinationSelected = action.onTargetAccountChanged,
                        fieldShape = fieldShape,
                        fieldColors = fieldColors
                    )
                }

                PaymentEntryType.Pay, PaymentEntryType.Receive -> {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val title = if (state.entryType == PaymentEntryType.Pay) {
                                "Información de la parte"
                            } else {
                                "Cuenta y cobro"
                            }
                            Text(
                                title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )

                            ModeSelectorField(
                                label = "Modo de pago",
                                value = state.modeOfPayment,
                                options = state.availableModes,
                                shape = fieldShape,
                                colors = fieldColors,
                                onSelected = action.onModeOfPaymentChanged
                            )

                            if (state.entryType == PaymentEntryType.Pay) {
                                if (state.partyOptions.isNotEmpty()) {
                                    ModeSelectorField(
                                        label = "Proveedor / Tercero",
                                        value = state.party,
                                        options = state.partyOptions,
                                        shape = fieldShape,
                                        colors = fieldColors,
                                        onSelected = action.onPartyChanged
                                    )
                                } else {
                                    OutlinedTextField(
                                        value = state.party,
                                        onValueChange = action.onPartyChanged,
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text("Proveedor / Tercero") },
                                        shape = fieldShape,
                                        colors = fieldColors,
                                        singleLine = true
                                    )
                                }

                                OutlinedTextField(
                                    value = state.concept,
                                    onValueChange = action.onConceptChanged,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Concepto del gasto") },
                                    placeholder = { Text("Describe motivo y naturaleza del gasto") },
                                    shape = fieldShape,
                                    colors = fieldColors,
                                    minLines = 3,
                                    maxLines = 5
                                )
                            }
                        }
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    val wide = maxWidth > 720.dp
                    val medium = maxWidth > 460.dp

                    val detailTitle = when (state.entryType) {
                        PaymentEntryType.Pay -> "Detalle del gasto"
                        PaymentEntryType.InternalTransfer -> "Detalle de transferencia"
                        PaymentEntryType.Receive -> "Detalle del cobro"
                    }
                    val referenceNoLabelCompact = when (state.entryType) {
                        PaymentEntryType.InternalTransfer -> "Ref. transferencia"
                        PaymentEntryType.Pay -> "Ref. comprobante"
                        PaymentEntryType.Receive -> "Ref. cobro"
                    }
                    val referenceNoLabelFull = when (state.entryType) {
                        PaymentEntryType.InternalTransfer -> "Referencia de transferencia"
                        PaymentEntryType.Pay -> "Referencia del comprobante"
                        PaymentEntryType.Receive -> "Número de referencia"
                    }
                    val referenceNoPlaceholder = when (state.entryType) {
                        PaymentEntryType.InternalTransfer -> "TRX-001"
                        PaymentEntryType.Pay -> "FACT/CHK-001"
                        PaymentEntryType.Receive -> "REF-001"
                    }
                    val amountLabel = when (state.entryType) {
                        PaymentEntryType.Pay -> "Monto del gasto"
                        PaymentEntryType.InternalTransfer -> "Monto a transferir"
                        PaymentEntryType.Receive -> "Monto a registrar"
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            detailTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = when (state.entryType) {
                                PaymentEntryType.Pay -> "Completa el monto y, si aplica, los datos del comprobante bancario."
                                PaymentEntryType.InternalTransfer -> "Usa referencia y fecha cuando la cuenta origen o destino sea bancaria."
                                PaymentEntryType.Receive -> "Registra el importe cobrado y la referencia si el medio de pago la requiere."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )

                        if (wide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                MoneyTextField(
                                    currencyCode = state.currencyCode,
                                    rawValue = state.amount,
                                    onRawValueChange = action.onAmountChanged,
                                    modifier = Modifier.weight(1.15f),
                                    label = amountLabel,
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Next
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = state.referenceNo,
                                        onValueChange = action.onReferenceNoChanged,
                                        modifier = Modifier.fillMaxWidth(),
                                        label = { Text(referenceNoLabelCompact) },
                                        placeholder = { Text(referenceNoPlaceholder) },
                                        isError = state.referenceNoError != null,
                                        supportingText = { state.referenceNoError?.let { Text(it) } },
                                        shape = fieldShape,
                                        colors = fieldColors,
                                        singleLine = true
                                    )
                                    ReferenceDatePickerField(
                                        value = state.referenceDate,
                                        onDateSelected = action.onReferenceDateChanged,
                                        modifier = Modifier.fillMaxWidth(),
                                        label = "Fecha referencia",
                                        isError = state.referenceDateError != null,
                                        errorText = state.referenceDateError,
                                        shape = fieldShape,
                                        colors = fieldColors
                                    )
                                }
                            }
                        } else {
                            MoneyTextField(
                                currencyCode = state.currencyCode,
                                rawValue = state.amount,
                                onRawValueChange = action.onAmountChanged,
                                label = amountLabel,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Next
                            )

                            if (medium) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    OutlinedTextField(
                                        value = state.referenceNo,
                                        onValueChange = action.onReferenceNoChanged,
                                        modifier = Modifier.weight(1f),
                                        label = { Text(referenceNoLabelCompact) },
                                        placeholder = { Text(referenceNoPlaceholder) },
                                        isError = state.referenceNoError != null,
                                        supportingText = { state.referenceNoError?.let { Text(it) } },
                                        shape = fieldShape,
                                        colors = fieldColors,
                                        singleLine = true
                                    )
                                    ReferenceDatePickerField(
                                        value = state.referenceDate,
                                        onDateSelected = action.onReferenceDateChanged,
                                        modifier = Modifier.weight(1f),
                                        label = "Fecha referencia",
                                        isError = state.referenceDateError != null,
                                        errorText = state.referenceDateError,
                                        shape = fieldShape,
                                        colors = fieldColors
                                    )
                                }
                            } else {
                                OutlinedTextField(
                                    value = state.referenceNo,
                                    onValueChange = action.onReferenceNoChanged,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(referenceNoLabelFull) },
                                    placeholder = { Text(referenceNoPlaceholder) },
                                    isError = state.referenceNoError != null,
                                    supportingText = { state.referenceNoError?.let { Text(it) } },
                                    shape = fieldShape,
                                    colors = fieldColors,
                                    singleLine = true
                                )
                                ReferenceDatePickerField(
                                    value = state.referenceDate,
                                    onDateSelected = action.onReferenceDateChanged,
                                    modifier = Modifier.fillMaxWidth(),
                                    label = "Fecha de referencia",
                                    isError = state.referenceDateError != null,
                                    errorText = state.referenceDateError,
                                    shape = fieldShape,
                                    colors = fieldColors
                                )
                            }
                        }

                        if (state.entryType != PaymentEntryType.Pay) {
                            OutlinedTextField(
                                value = state.notes,
                                onValueChange = action.onNotesChanged,
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Notas / observaciones") },
                                shape = fieldShape,
                                colors = fieldColors,
                                minLines = 2,
                                maxLines = 3
                            )
                        }
                    }
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val compactAction = maxWidth < 520.dp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (compactAction) Arrangement.Center else Arrangement.End
                ) {
                    Button(
                        onClick = action.onSubmit,
                        modifier = if (compactAction) Modifier.fillMaxWidth() else Modifier,
                        enabled = !state.isSubmitting && !globalBusy && state.isOnline && !state.offlineModeEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        )
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(end = 8.dp).size(16.dp),
                                strokeWidth = 2.dp
                            )
                        }
                        Text(
                            when (state.entryType) {
                                PaymentEntryType.Pay -> "Registrar gasto"
                                PaymentEntryType.InternalTransfer -> "Transferir"
                                PaymentEntryType.Receive -> "Registrar cobro"
                            }
                        )
                    }
                }
            }

            Text(
                text = if (state.entryType == PaymentEntryType.InternalTransfer) {
                    "No altera el total del turno; reclasifica saldo entre cuentas contables."
                } else if (state.entryType == PaymentEntryType.Receive) {
                    "Entrada permitida únicamente para cobro de factura del cliente."
                } else {
                    "El gasto disminuye caja/banco y se registra contra la cuenta de gasto."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            )
        }
    }
}

@Composable
private fun HeaderCard(entryType: PaymentEntryType) {
    val scheme = MaterialTheme.colorScheme
    val style = when (entryType) {
        PaymentEntryType.Pay -> {
            HeaderStyle(
                title = "Gastos",
                subtitle = "Gasto",
                icon = Icons.Filled.Payments,
                gradient = listOf(scheme.primary, scheme.tertiary)
            )
        }

        PaymentEntryType.InternalTransfer -> {
            HeaderStyle(
                title = "Gastos",
                subtitle = "Transferencia Interna",
                icon = Icons.AutoMirrored.Filled.CompareArrows,
                gradient = listOf(scheme.primary, scheme.secondary)
            )
        }

        PaymentEntryType.Receive -> {
            HeaderStyle(
                title = "Gastos",
                subtitle = "Cobro de Factura",
                icon = Icons.Filled.AccountBalanceWallet,
                gradient = listOf(scheme.secondary, scheme.primary)
            )
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(style.gradient))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = style.title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text(
                        text = style.subtitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.92f),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Registro contable operativo del turno POS",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(style.icon, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun TransferFlowSection(
    sourceValue: String,
    destinationValue: String,
    options: List<String>,
    onSourceSelected: (String) -> Unit,
    onDestinationSelected: (String) -> Unit,
    fieldShape: RoundedCornerShape,
    fieldColors: androidx.compose.material3.TextFieldColors
) {
    val scheme = MaterialTheme.colorScheme
    val pulse = rememberInfiniteTransition(label = "transferPulse").animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    ).value

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            val horizontal = maxWidth > 700.dp
            if (horizontal) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TransferModeCard(
                        modifier = Modifier.weight(1f),
                        title = "Cuenta origen",
                        label = "Desde (cuenta)",
                        value = sourceValue,
                        options = options,
                        accent = scheme.primary,
                        shape = fieldShape,
                        colors = fieldColors,
                        onSelected = onSourceSelected
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(scheme.primary, scheme.secondary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.scale(pulse)
                        )
                    }

                    TransferModeCard(
                        modifier = Modifier.weight(1f),
                        title = "Cuenta destino",
                        label = "Hacia (cuenta)",
                        value = destinationValue,
                        options = options,
                        accent = scheme.secondary,
                        shape = fieldShape,
                        colors = fieldColors,
                        onSelected = onDestinationSelected
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TransferModeCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Cuenta origen",
                        label = "Desde (cuenta)",
                        value = sourceValue,
                        options = options,
                        accent = scheme.primary,
                        shape = fieldShape,
                        colors = fieldColors,
                        onSelected = onSourceSelected
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Brush.horizontalGradient(listOf(scheme.primary, scheme.secondary))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.scale(pulse)
                            )
                        }
                    }
                    TransferModeCard(
                        modifier = Modifier.fillMaxWidth(),
                        title = "Cuenta destino",
                        label = "Hacia (cuenta)",
                        value = destinationValue,
                        options = options,
                        accent = scheme.secondary,
                        shape = fieldShape,
                        colors = fieldColors,
                        onSelected = onDestinationSelected
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransferModeCard(
    modifier: Modifier,
    title: String,
    label: String,
    value: String,
    options: List<String>,
    accent: Color,
    shape: RoundedCornerShape,
    colors: androidx.compose.material3.TextFieldColors,
    onSelected: (String) -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.08f))
            .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(title, style = MaterialTheme.typography.labelLarge, color = accent)
        }

        ModeSelectorField(
            label = label,
            value = value,
            options = options,
            shape = shape,
            colors = colors,
            onSelected = onSelected
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeSelectorField(
    label: String,
    value: String,
    options: List<String>,
    shape: RoundedCornerShape,
    colors: androidx.compose.material3.TextFieldColors,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            label = { Text(label) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            shape = shape,
            colors = colors,
            placeholder = { Text("Seleccionar...") },
            readOnly = true,
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Sin opciones disponibles") },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun paymentEntryFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceDatePickerField(
    value: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String,
    isError: Boolean,
    errorText: String?,
    shape: RoundedCornerShape,
    colors: androidx.compose.material3.TextFieldColors
) {
    var openPicker by remember { mutableStateOf(false) }
    val dateFieldInteraction = remember { MutableInteractionSource() }
    val initialSelectedDateMillis = remember(value) {
        runCatching {
            val date = LocalDate.parse(value)
            val millis = date
                .atStartOfDayIn(TimeZone.currentSystemDefault())
                .toEpochMilliseconds()
            millis
        }.getOrNull()
    }
    val pickerState = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initialSelectedDateMillis
    )

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            placeholder = { Text("YYYY-MM-DD") },
            isError = isError,
            supportingText = { errorText?.let { Text(it) } },
            shape = shape,
            colors = colors,
            singleLine = true,
            readOnly = true,
            trailingIcon = {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = "Seleccionar fecha"
                )
            }
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = dateFieldInteraction,
                    indication = null
                ) { openPicker = true }
        )
    }

    if (openPicker) {
        DatePickerDialog(
            onDismissRequest = { openPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val isoDate = Instant
                                .fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
                                .toString()
                            onDateSelected(isoDate)
                        }
                        openPicker = false
                    }
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { openPicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private data class HeaderStyle(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val gradient: List<Color>
)

@Preview(showBackground = true)
@Composable
private fun PaymentEntryScreenPreview() {
    PaymentEntryScreen(
        state = PaymentEntryState(
            entryType = PaymentEntryType.Pay,
            modeOfPayment = "Efectivo",
            amount = "1250.00"
        ),
        action = PaymentEntryAction()
    )
}

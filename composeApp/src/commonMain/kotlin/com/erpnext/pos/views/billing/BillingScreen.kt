@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)

package com.erpnext.pos.views.billing

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.ItemBO
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.pointer.pointerInput
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.utils.formatAmount
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.resolveRateBetweenFromBaseRates
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.models.PaymentTermBO
import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.moneyScale
import com.erpnext.pos.utils.oauth.toDouble
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarHost
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.navigation.GlobalTopBarState
import com.erpnext.pos.navigation.LocalTopBarController
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class CartItem(
    val itemCode: String,
    val name: String,
    val currency: String?,
    val quantity: Double,
    val price: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    state: BillingState,
    action: BillingAction,
    snackbar: SnackbarController
) {
    val uiSnackbar = snackbar.snackbar.collectAsState().value
    val colors = MaterialTheme.colorScheme
    var step by rememberSaveable { mutableStateOf(LabCheckoutStep.Cart) }
    val successState = when (state) {
        is BillingState.Success -> state
        is BillingState.Error -> state.previous
        else -> null
    }
    val successMessage = successState?.successMessage
    val successDialogMessage = successState?.successDialogMessage
    val successDialogInvoice = successState?.successDialogInvoice
    val successDialogId = successState?.successDialogId ?: 0L
    var popupMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var popupInvoice by rememberSaveable { mutableStateOf<String?>(null) }
    val topBarController = LocalTopBarController.current
    val inactivityTimeoutMs = 5 * 60 * 1000L
    var lastInteraction by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds()) }
    val hasActiveSale = state is BillingState.Success &&
            (state.selectedCustomer != null ||
                    state.cartItems.isNotEmpty() ||
                    state.paymentLines.isNotEmpty())

    // Si salimos de Success, regresamos al primer paso.
    LaunchedEffect(state, successState) {
        if (successState == null) {
            popupMessage = null
            popupInvoice = null
            action.onClearSuccessMessage()
            step = LabCheckoutStep.Cart
        } else if (successState.selectedCustomer == null && successState.cartItems.isEmpty() && step != LabCheckoutStep.Cart) {
            step = LabCheckoutStep.Cart
        }
    }

    LaunchedEffect(successDialogId, successDialogMessage, successMessage) {
        if (successDialogId == 0L) return@LaunchedEffect
        val message = (successDialogMessage ?: successMessage)
            ?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        popupMessage = message
        popupInvoice = successDialogInvoice
        delay(3000)
        popupMessage = null
        popupInvoice = null
        action.onClearSuccessMessage()
        if (step == LabCheckoutStep.Checkout) {
            step = LabCheckoutStep.Cart
        }
    }

    LaunchedEffect(lastInteraction, hasActiveSale) {
        if (!hasActiveSale) return@LaunchedEffect
        val now = Clock.System.now().toEpochMilliseconds()
        val remaining = inactivityTimeoutMs - (now - lastInteraction)
        if (remaining <= 0) {
            step = LabCheckoutStep.Cart
            action.onResetSale()
            snackbar.show(
                "Venta reiniciada por inactividad",
                SnackbarType.Info,
                SnackbarPosition.Top
            )
            return@LaunchedEffect
        }
        delay(remaining)
        val elapsed = Clock.System.now().toEpochMilliseconds() - lastInteraction
        if (elapsed >= inactivityTimeoutMs && hasActiveSale) {
            step = LabCheckoutStep.Cart
            action.onResetSale()
            snackbar.show(
                "Venta reiniciada por inactividad",
                SnackbarType.Info,
                SnackbarPosition.Top
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { topBarController.reset() }
    }

    LaunchedEffect(step, state) {
        topBarController.set(
            GlobalTopBarState(
                subtitle = if (state is BillingState.Success && state.selectedCustomer != null) {
                    state.selectedCustomer.customerName
                } else {
                    null
                },
                showBack = step != LabCheckoutStep.Cart,
                onBack = {
                    if (step == LabCheckoutStep.Checkout) {
                        step = LabCheckoutStep.Cart
                    } else if (step == LabCheckoutStep.Cart) {
                        action.onBack()
                    }
                }
            )
        )
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                        lastInteraction = Clock.System.now().toEpochMilliseconds()
                    }
                }
            }
    ) {
        Scaffold(
            containerColor = colors.background,
            bottomBar = {
                if (state is BillingState.Success && step == LabCheckoutStep.Checkout) {
                    // Botón fijo para pagar siempre visible.
                    Box(
                        contentAlignment = Alignment.CenterEnd,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        colors.background.copy(alpha = 0.0f),
                                        colors.background
                                    )
                                )
                            )
                    ) {
                        Button(
                            onClick = action.onFinalizeSale,
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            enabled = state.selectedCustomer != null &&
                                    state.cartItems.isNotEmpty() &&
                                    (state.isCreditSale || state.paidAmountBase + 0.01 >= state.total) &&
                                    (!state.isCreditSale || state.selectedPaymentTerm != null),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            )
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pagar", fontWeight = FontWeight.Bold)
                                Icon(
                                    modifier = Modifier.size(14.dp),
                                    imageVector = Icons.Default.ShoppingCartCheckout,
                                    contentDescription = "Pagar",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        ) { paddingValues ->
            when (state) {
                BillingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            trackColor = colors.onSecondary, //Color.Blue,
                            color = colors.onPrimary, //Color.Cyan,
                            strokeWidth = 2.dp
                        )
                    }
                }

                is BillingState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurface
                                )
                                if (state.showSyncRates) {
                                    Button(onClick = action.onSyncExchangeRates) {
                                        Text("Sincronizar tasas de cambio")
                                    }
                                }
                            }
                        }
                    }
                }

                BillingState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        snackbar.show(
                            "Sin datos disponibles.",
                            SnackbarType.Info,
                            SnackbarPosition.Top
                        )
                    }
                }

                is BillingState.Success -> {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            fadeIn(tween(180)) + slideInVertically(
                                animationSpec = tween(180),
                                initialOffsetY = { it / 6 }
                            ) togetherWith fadeOut(tween(160)) + slideOutVertically(
                                animationSpec = tween(160),
                                targetOffsetY = { -it / 8 }
                            )
                        },
                        label = "billing_step_transition"
                    ) { targetStep ->
                        when (targetStep) {
                            LabCheckoutStep.Cart -> BillingLabContent(
                                state = state,
                                action = action,
                                onCheckout = {
                                    if (state.selectedCustomer == null)
                                        snackbar.show(
                                            "Seleccione primero al cliente.",
                                            SnackbarType.Error,
                                            SnackbarPosition.Top
                                        )
                                    else if (state.cartItems.isEmpty()) {
                                        snackbar.show(
                                            "Seleccione el(los) productos del cliente.",
                                            SnackbarType.Error,
                                            SnackbarPosition.Top
                                        )
                                    } else
                                        step = LabCheckoutStep.Checkout
                                },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = paddingValues.calculateTopPadding())
                            )

                            LabCheckoutStep.Checkout -> BillingLabCheckoutStep(
                                state = state,
                                action = action,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = paddingValues.calculateTopPadding())
                            )
                        }
                    }
                }
            }
        }

        if (!popupMessage.isNullOrBlank()) {
            Dialog(
                onDismissRequest = {
                    popupMessage = null
                    popupInvoice = null
                    action.onClearSuccessMessage()
                },
                properties = DialogProperties(dismissOnClickOutside = false)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier.widthIn(min = 420.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 36.dp, vertical = 30.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = popupMessage ?: "Exito",
                            color = colors.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        )
                        popupInvoice?.let { invoice ->
                            Text(
                                text = "Referencia: $invoice",
                                color = colors.onSurface.copy(alpha = 0.75f),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }

    SnackbarHost(
        snackbar = uiSnackbar, onDismiss = snackbar::dismiss, modifier = Modifier.fillMaxSize()
    )
}

private enum class LabCheckoutStep {
    Cart,
    Checkout
}

@Composable
private fun BillingLabContent(
    state: BillingState.Success,
    action: BillingAction,
    onCheckout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary
    val background = colors.background
    val leftPanelBg = colors.surfaceVariant
    val invoiceCurrency = state.currency?.trim()?.uppercase().orEmpty().ifBlank { "USD" }
    val baseCurrency = state.baseCurrency?.trim()?.uppercase().orEmpty().ifBlank { invoiceCurrency }
    val secondaryCurrency = resolveSecondaryCurrency(
        invoiceCurrency = invoiceCurrency,
        baseCurrency = baseCurrency,
        exchangeRateByCurrency = state.exchangeRateByCurrency
    )
    fun toSecondary(amount: Double): Double? {
        return convertToSecondary(
            amount = amount,
            secondaryCurrency = secondaryCurrency,
            exchangeRateByCurrency = state.exchangeRateByCurrency
        )
    }

    val categories =
        state.productSearchResults.mapNotNull { it.itemGroup.takeIf { g -> g.isNotBlank() } }
            .distinct()
            .sorted()
    var selectedCategory by rememberSaveable { mutableStateOf("Todos") }
    val filteredProducts =
        if (selectedCategory == "Todos") state.productSearchResults
        else state.productSearchResults.filter { it.itemGroup == selectedCategory }

    Column(
        modifier = modifier
            .background(background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(leftPanelBg, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                LabSearchBar(
                    value = state.productSearchQuery,
                    onChange = action.onProductSearchQueryChange,
                    onClear = { action.onProductSearchQueryChange("") }
                )

                Spacer(Modifier.height(12.dp))

                LabCategoryTabs(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onSelect = { selectedCategory = it }
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCategory == "Todos") "Todos los productos" else selectedCategory,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = "(${filteredProducts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                Spacer(Modifier.height(12.dp))

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 220.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredProducts, key = { it.itemCode }) { item ->
                        LabProductCard(
                            item = item,
                            baseCurrency = invoiceCurrency,
                            exchangeRateByCurrency = state.exchangeRateByCurrency,
                            accent = accent,
                            onClick = { action.onProductAdded(item) }
                        )
                    }
                }
            }

            Surface(
                color = colors.surface,
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 2.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .widthIn(min = 320.dp, max = 420.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier.fillMaxHeight().padding(16.dp)
                ) {
                    LabCartHeader(
                        itemCount = state.cartItems.sumOf { it.quantity }.toInt(),
                        accent = accent
                    )

                    Spacer(Modifier.height(12.dp))

                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        CustomerSelector(
                            customers = state.customers,
                            query = state.customerSearchQuery,
                            onQueryChange = action.onCustomerSearchQueryChange,
                            onCustomerSelected = action.onCustomerSelected
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Carrito",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface
                    )

                    Spacer(Modifier.height(8.dp))

                    if (state.cartItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Carrito vacío",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.cartItems, key = { it.itemCode }) { item ->
                                LabCartItem(
                                    item = item,
                                    baseCurrency = invoiceCurrency,
                                    exchangeRateByCurrency = state.exchangeRateByCurrency,
                                    onUpdateQuantity = { qty ->
                                        action.onQuantityChanged(item.itemCode, qty)
                                    },
                                    onRemove = { action.onRemoveItem(item.itemCode) }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Resumen de carrito para contexto rápido.
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Resumen",
                                style = MaterialTheme.typography.labelLarge,
                                color = colors.onSurface
                            )
                            Text(
                                text = "Cliente: ${state.selectedCustomer?.customerName ?: "--"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                            Text(
                                text = "Artículos: ${state.cartItems.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                            HorizontalDivider(color = colors.outlineVariant, thickness = 1.dp)
                            PaymentTotalsRow(
                                "Subtotal",
                                invoiceCurrency,
                                state.subtotal,
                                secondaryCurrencyCode = secondaryCurrency,
                                secondaryAmount = toSecondary(state.subtotal)
                            )
                            if (state.taxes > 0.0) {
                                PaymentTotalsRow(
                                    "Impuestos",
                                    invoiceCurrency,
                                    state.taxes,
                                    secondaryCurrencyCode = secondaryCurrency,
                                    secondaryAmount = toSecondary(state.taxes)
                                )
                            }
                            if (state.discount > 0.0) {
                                PaymentTotalsRow(
                                    "Descuento",
                                    invoiceCurrency,
                                    -state.discount,
                                    secondaryCurrencyCode = secondaryCurrency,
                                    secondaryAmount = toSecondary(-state.discount)
                                )
                            }
                            if (state.shippingAmount > 0.0) {
                                PaymentTotalsRow(
                                    "Envío",
                                    invoiceCurrency,
                                    state.shippingAmount,
                                    secondaryCurrencyCode = secondaryCurrency,
                                    secondaryAmount = toSecondary(state.shippingAmount)
                                )
                            }
                            PaymentTotalsRow(
                                "Total",
                                invoiceCurrency,
                                state.total,
                                secondaryCurrencyCode = secondaryCurrency,
                                secondaryAmount = toSecondary(state.total)
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Botón de checkout: pasamos al siguiente paso sin validar pagos.
                    Button(
                        onClick = onCheckout,
                        enabled = state.selectedCustomer != null && state.cartItems.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        )
                    ) {
                        Text("Checkout")
                    }

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Continua al paso de pagos.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun BillingLabCheckoutStep(
    state: BillingState.Success,
    action: BillingAction,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val invoiceCurrency = state.currency?.trim()?.uppercase().orEmpty().ifBlank { "USD" }
    val baseCurrency = state.baseCurrency?.trim()?.uppercase().orEmpty().ifBlank { invoiceCurrency }
    val secondaryCurrency = resolveSecondaryCurrency(
        invoiceCurrency = invoiceCurrency,
        baseCurrency = baseCurrency,
        exchangeRateByCurrency = state.exchangeRateByCurrency
    )
    fun toSecondary(amount: Double): Double? {
        return convertToSecondary(
            amount = amount,
            secondaryCurrency = secondaryCurrency,
            exchangeRateByCurrency = state.exchangeRateByCurrency
        )
    }
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        // Encabezado principal del checkout.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Datos de pago",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface
            )
            Text(
                text = "Revisa y confirma la venta",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(12.dp))
        // Tarjetas de total y crédito alineadas.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = formatAmount(invoiceCurrency.toCurrencySymbol(), state.total),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = colors.onSurface
                    )
                    HorizontalDivider(color = colors.outlineVariant, thickness = (1.2).dp)
                    PaymentTotalsRow(
                        "Pagado",
                        invoiceCurrency,
                        state.paidAmountBase,
                        secondaryCurrencyCode = secondaryCurrency,
                        secondaryAmount = toSecondary(state.paidAmountBase)
                    )
                    PaymentTotalsRow(
                        "Pendiente",
                        invoiceCurrency,
                        state.balanceDueBase,
                        secondaryCurrencyCode = secondaryCurrency,
                        secondaryAmount = toSecondary(state.balanceDueBase)
                    )
                    PaymentTotalsRow(
                        "Cambio",
                        invoiceCurrency,
                        state.changeDueBase,
                        secondaryCurrencyCode = secondaryCurrency,
                        secondaryAmount = toSecondary(state.changeDueBase)
                    )
                    HorizontalDivider(color = colors.outlineVariant, thickness = (1.2).dp)
                    PaymentTotalsRow(
                        "Subtotal",
                        invoiceCurrency,
                        state.subtotal,
                        secondaryCurrencyCode = secondaryCurrency,
                        secondaryAmount = toSecondary(state.subtotal)
                    )
                    if (state.taxes > 0.0) {
                        PaymentTotalsRow(
                            "Impuestos",
                            invoiceCurrency,
                            state.taxes,
                            secondaryCurrencyCode = secondaryCurrency,
                            secondaryAmount = toSecondary(state.taxes)
                        )
                    }
                    if (state.discount > 0.0) {
                        PaymentTotalsRow(
                            "Descuento",
                            invoiceCurrency,
                            -state.discount,
                            secondaryCurrencyCode = secondaryCurrency,
                            secondaryAmount = toSecondary(-state.discount)
                        )
                    }
                    if (state.shippingAmount > 0.0) {
                        PaymentTotalsRow(
                            "Envío",
                            invoiceCurrency,
                            state.shippingAmount,
                            secondaryCurrencyCode = secondaryCurrency,
                            secondaryAmount = toSecondary(state.shippingAmount)
                        )
                    }
                }
            }
            if (state.paymentTerms.isNotEmpty()) {
                ElevatedCard(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Crédito",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface
                        )

                        CreditTermsSection(
                            isCreditSale = state.isCreditSale,
                            paymentTerms = state.paymentTerms,
                            selectedPaymentTerm = state.selectedPaymentTerm,
                            onCreditSaleChanged = action.onCreditSaleChanged,
                            onPaymentTermSelected = action.onPaymentTermSelected
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        // Contenido scrolleable.
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(bottom = 72.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Descuento y envío",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface
                    )
                    DiscountShippingInputs(state, action)
                }
            }
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Pagos",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface
                    )
                    PaymentSection(
                        state = state,
                        baseCurrency = invoiceCurrency,
                        exchangeRateByCurrency = state.exchangeRateByCurrency,
                        paymentLines = state.paymentLines,
                        paymentModes = state.paymentModes,
                        paidAmountBase = state.paidAmountBase,
                        totalAmount = state.total,
                        paymentErrorMessage = state.paymentErrorMessage,
                        isCreditSale = state.isCreditSale,
                        onAddPaymentLine = action.onAddPaymentLine,
                        onRemovePaymentLine = action.onRemovePaymentLine,
                        onPaymentCurrencySelected = action.onPaymentCurrencySelected
                    )
                }
            }
        }
        /*Button(
            onClick = action.onFinalizeSale,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.selectedCustomer != null &&
                    state.cartItems.isNotEmpty() &&
                    (state.isCreditSale || state.paidAmountBase + 0.01 >= state.total) &&
                    (!state.isCreditSale || state.selectedPaymentTerm != null)
        ) {
            Text("Pagar")
        }*/
    }
}

@Composable
private fun LabSearchBar(
    value: String,
    onChange: (String) -> Unit,
    onClear: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                }
            }
        },
        placeholder = { Text("Buscar productos o escanear...") },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        shape = RoundedCornerShape(18.dp)
    )
}

@Composable
private fun LabCategoryTabs(
    categories: List<String>,
    selectedCategory: String,
    onSelect: (String) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            LabCategoryChip(
                label = "Todos",
                selected = selectedCategory == "Todos",
                onClick = { onSelect("Todos") }
            )
        }
        items(categories, key = { it }) { category ->
            LabCategoryChip(
                label = category,
                selected = selectedCategory == category,
                onClick = { onSelect(category) }
            )
        }
    }
}

@Composable
private fun LabCategoryChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val background = if (selected) colors.primary else colors.surfaceVariant
    val textColor = if (selected) colors.onPrimary else colors.onSurfaceVariant
    Surface(
        color = background,
        shape = RoundedCornerShape(999.dp),
        shadowElevation = if (selected) 3.dp else 0.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun LabProductCard(
    item: ItemBO,
    baseCurrency: String,
    exchangeRateByCurrency: Map<String, Double>,
    accent: Color,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val isDesktop = remember { getPlatformName() == "Desktop" }
    val imageUrl = item.image?.trim().orEmpty()

    val itemCurrency = item.currency?.trim()?.uppercase().orEmpty()
    val base = baseCurrency.trim().uppercase()
    val baseRates = exchangeRateByCurrency.toMutableMap().apply { this[base] = 1.0 }
    val rate = if (itemCurrency.isBlank() || itemCurrency == base) 1.0
    else resolveRateBetweenFromBaseRates(
        fromCurrency = itemCurrency,
        toCurrency = base,
        baseCurrency = base,
        baseRates = baseRates
    ) ?: 1.0
    val displayPrice = bd(item.price * rate).moneyScale(0).toDouble(0)

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val showAction = !isDesktop || hovered

    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier
            .hoverable(interactionSource)
            .clickable(onClick = onClick)
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(106.dp)
                        .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                ) {
                    if (imageUrl.isNotBlank()) {
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colors.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.name.take(2).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = colors.primary,
                        shape = RoundedCornerShape(999.dp),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "Disp. ${item.actualQty.formatQty()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${baseCurrency.toCurrencySymbol()} $displayPrice",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (showAction) {
                Surface(
                    color = accent,
                    shape = RoundedCornerShape(10.dp),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clickable(onClick = onClick)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Agregar",
                        tint = colors.onPrimary,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LabCartHeader(
    itemCount: Int,
    accent: Color
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = accent,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Money,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = "Orden actual",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface
                )
                Text(
                    text = "$itemCount items",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LabCartItem(
    item: CartItem,
    baseCurrency: String,
    exchangeRateByCurrency: Map<String, Double>,
    onUpdateQuantity: (Double) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val isDesktop = remember { getPlatformName() == "Desktop" }

    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()

    val showActions = !isDesktop || hovered
    val actionAlpha by animateFloatAsState(
        targetValue = if (showActions) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "cartActionsAlpha"
    )
    val itemCurrency = item.currency?.trim()?.uppercase().orEmpty()
    val base = baseCurrency.trim().uppercase()
    val baseRates = exchangeRateByCurrency.toMutableMap().apply { this[base] = 1.0 }
    val rate = if (itemCurrency.isBlank() || itemCurrency == base) 1.0
    else resolveRateBetweenFromBaseRates(
        fromCurrency = itemCurrency,
        toCurrency = base,
        baseCurrency = base,
        baseRates = baseRates
    ) ?: 1.0
    val unitDisplay = bd(item.price * rate).toDouble(0)
    val lineDisplay = unitDisplay * item.quantity
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(item.itemCode) { appeared = true }
    var isRemoving by remember { mutableStateOf(false) }
    LaunchedEffect(isRemoving) {
        if (isRemoving) {
            delay(180)
            onRemove()
        }
    }

    val appearAlpha by animateFloatAsState(
        targetValue = if (appeared && !isRemoving) 1f else 0f,
        animationSpec = tween(durationMillis = 170),
        label = "cartAppearAlpha"
    )
    val appearScale by animateFloatAsState(
        targetValue = if (appeared && !isRemoving) 1f else 0.92f,
        animationSpec = tween(durationMillis = 170),
        label = "cartAppearScale"
    )

    AnimatedVisibility(
        visible = !isRemoving,
        exit = fadeOut(tween(160)) + shrinkVertically(tween(160))
    ) {
        Surface(
            color = colors.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, colors.outlineVariant),
            modifier = modifier
                .hoverable(interactionSource)
                .graphicsLayer(
                    alpha = appearAlpha,
                    scaleX = appearScale,
                    scaleY = appearScale
                )
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatAmount(baseCurrency.toCurrencySymbol(), unitDisplay),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = { onUpdateQuantity((item.quantity - 1).coerceAtLeast(1.0)) },
                        modifier = Modifier.graphicsLayer(alpha = actionAlpha),
                        enabled = showActions
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Menos")
                    }
                    Text(
                        text = item.quantity.formatQty(),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp),
                        color = colors.onSurface
                    )
                    IconButton(
                        onClick = { onUpdateQuantity(item.quantity + 1) },
                        modifier = Modifier.graphicsLayer(alpha = actionAlpha),
                        enabled = showActions
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Más")
                    }
                    Text(
                        text = formatAmount(baseCurrency.toCurrencySymbol(), lineDisplay),
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.onSurface
                    )
                    IconButton(
                        onClick = { isRemoving = true },
                        modifier = Modifier.graphicsLayer(alpha = actionAlpha),
                        enabled = showActions
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomerSelector(
    customers: List<CustomerBO>,
    query: String,
    onQueryChange: (String) -> Unit,
    onCustomerSelected: (CustomerBO) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var displayLimit by remember { mutableStateOf(50) }
    var anchorWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val hasCustomers = customers.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded && hasCustomers, onExpandedChange = { }) {
        AppTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                expanded = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .onGloballyPositioned { anchorWidthPx = it.size.width }
                .onFocusChanged { focusState ->
                    expanded = focusState.isFocused
                },
            label = "Buscar o Seleccionar",
            placeholder = "Nombre, codigo, telefono...",
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(
            expanded = expanded && hasCustomers,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(
                focusable = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                clippingEnabled = true
            )
        ) {
            val menuWidth = remember(anchorWidthPx) {
                if (anchorWidthPx > 0) with(density) { anchorWidthPx.toDp() } else 360.dp
            }
            Column(
                modifier = Modifier
                    .width(menuWidth.coerceIn(280.dp, 520.dp))
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                customers.take(displayLimit).forEach { customer ->
                    DropdownMenuItem(
                        text = { Text("${customer.name} - ${customer.customerName}") },
                        onClick = {
                            onCustomerSelected(customer)
                            expanded = false
                        }
                    )
                }
                if (customers.size > displayLimit) {
                    DropdownMenuItem(
                        text = { Text("Mostrar más...") },
                        onClick = { displayLimit += 50 }
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentTotalsRow(
    label: String,
    currencyCode: String,
    amount: Double,
    secondaryCurrencyCode: String? = null,
    secondaryAmount: Double? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatAmount(currencyCode.toCurrencySymbol(), amount),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (secondaryCurrencyCode != null && secondaryAmount != null) {
                Text(
                    text = formatAmount(secondaryCurrencyCode.toCurrencySymbol(), secondaryAmount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun resolveSecondaryCurrency(
    invoiceCurrency: String,
    baseCurrency: String?,
    exchangeRateByCurrency: Map<String, Double>
): String? {
    val invoice = invoiceCurrency.trim().uppercase()
    val preferred = when (invoice) {
        "USD" -> "NIO"
        "NIO" -> "USD"
        else -> baseCurrency?.trim()?.uppercase()
    }
    if (preferred.isNullOrBlank() || preferred == invoice) return null
    return preferred.takeIf { exchangeRateByCurrency[it]?.let { rate -> rate > 0.0 } == true }
}

private fun convertToSecondary(
    amount: Double,
    secondaryCurrency: String?,
    exchangeRateByCurrency: Map<String, Double>
): Double? {
    if (secondaryCurrency.isNullOrBlank()) return null
    val rate = exchangeRateByCurrency[secondaryCurrency]?.takeIf { it > 0.0 } ?: return null
    return amount / rate
}

private fun inferCurrencyFromModeName(
    modeOfPayment: String,
    fallback: String
): String {
    val upper = modeOfPayment.trim().uppercase()
    return when {
        upper.contains("USD") || upper.contains("DOLAR") -> "USD"
        upper.contains("NIO") || upper.contains("CORDO") -> "NIO"
        else -> fallback
    }
}

@Composable
private fun PaymentSection(
    state: BillingState,
    baseCurrency: String,
    exchangeRateByCurrency: Map<String, Double>,
    paymentLines: List<PaymentLine>,
    paymentModes: List<POSPaymentModeOption>,
    paidAmountBase: Double,
    totalAmount: Double,
    paymentErrorMessage: String?,
    isCreditSale: Boolean,
    onAddPaymentLine: (PaymentLine) -> Unit,
    onRemovePaymentLine: (Int) -> Unit,
    onPaymentCurrencySelected: (String) -> Unit
) {
    val modeOptions = remember(paymentModes) { paymentModes.map { it.modeOfPayment }.distinct() }
    var selectedMode by remember(modeOptions) { mutableStateOf("") }
    val selectedModeOption = paymentModes.firstOrNull { it.modeOfPayment == selectedMode }
    val requiresReference = remember(selectedModeOption) {
        requiresReference(selectedModeOption)
    }
    val inferredCurrency = remember(selectedMode, baseCurrency) {
        inferCurrencyFromModeName(selectedMode, baseCurrency)
    }
    val modeCurrency = remember(state, selectedMode) {
        (state as? BillingState.Success)?.paymentModeCurrencyByMode?.get(selectedMode)
    }
    var selectedCurrency by remember(selectedMode, baseCurrency) {
        mutableStateOf(modeCurrency ?: inferredCurrency)
    }
    var amountInput by remember { mutableStateOf("") }
    var amountValue by remember { mutableStateOf(0.0) }
    var rateInput by remember { mutableStateOf("1.0") }
    var referenceInput by remember { mutableStateOf("") }

    LaunchedEffect(selectedMode, modeCurrency, inferredCurrency) {
        selectedCurrency = modeCurrency?.trim()?.uppercase() ?: inferredCurrency
    }

    LaunchedEffect(selectedCurrency, exchangeRateByCurrency) {
        onPaymentCurrencySelected(selectedCurrency)
        if (selectedCurrency.equals(baseCurrency, ignoreCase = true)) {
            rateInput = "1.0"
        } else {
            exchangeRateByCurrency[selectedCurrency.uppercase()]?.let { rate ->
                rateInput = rate.toString()
            }
        }
    }

    LaunchedEffect(selectedMode) {
        referenceInput = ""
    }
    Column(modifier = Modifier.padding(end = 12.dp, start = 12.dp, bottom = 8.dp)) {
        if (paymentLines.isEmpty()) {
            // Estado vacío con tarjeta para mayor claridad visual.
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Sin pagos registrados",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paymentLines.forEachIndexed { index, line ->
                    // Animamos la aparición/desaparición de cada pago.
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(360)) + expandVertically(),
                        exit = fadeOut(animationSpec = tween(320)) + shrinkVertically()
                    ) {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = line.modeOfPayment,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = formatAmount(
                                            line.currency.toCurrencySymbol(), line.enteredAmount
                                        ),
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Base: ${
                                            formatAmount(
                                                baseCurrency.toCurrencySymbol(), line.baseAmount
                                            )
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    /*Text(
                                        text = "Tasa: ${line.exchangeRate}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )*/
                                    /*if (!line.referenceNumber.isNullOrBlank()) {
                                        Text(
                                            text = "Referencia: ${line.referenceNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }*/
                                }
                                IconButton(onClick = { onRemovePaymentLine(index) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Eliminar línea de pago",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (isCreditSale) {
            Text(
                "Pago parcial (opcional). El restante quedará como saldo a crédito.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
        }

        Text("Método de pago", style = MaterialTheme.typography.bodyMedium)
        var modeExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = modeExpanded, onExpandedChange = { modeExpanded = it }) {
            AppTextField(
                value = selectedMode,
                onValueChange = {},
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                label = "Método de pago",
                placeholder = "Selecciona el metodo de pago del cliente",
                leadingIcon = { Icon(Icons.Default.Money, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) })
            ExposedDropdownMenu(
                expanded = modeExpanded, onDismissRequest = { modeExpanded = false }) {
                modeOptions.forEach { mode ->
                    DropdownMenuItem(text = { Text(mode) }, onClick = {
                        selectedMode = mode
                        modeExpanded = false
                    })
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        val rateValue = rateInput.toDoubleOrNull() ?: 0.0
        val canAdd =
            amountValue > 0.0 && rateValue > 0.0 && selectedMode.isNotBlank() &&
                    (!requiresReference || referenceInput.isNotBlank()) &&
                    (paidAmountBase < totalAmount)

        MoneyTextField(
            currencyCode = selectedCurrency,
            rawValue = amountInput,
            onRawValueChange = { amountInput = it },
            label = "Monto",
            enabled = true, //!isCreditSale,
            onAmountChanged = { amountValue = it },
            supportingText = {
                if (!selectedCurrency.equals(baseCurrency, ignoreCase = true)) {
                    val rate = rateInput.toDoubleOrNull() ?: 0.0
                    val base = amountValue * rate
                    Text("Base: ${formatAmount(baseCurrency.toCurrencySymbol(), base)}")
                }
            },
            trailingIcon = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            amountInput = ""
                            amountValue = 0.0
                        },
                        enabled = amountInput.isNotBlank()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Limpiar")
                    }
                    IconButton(
                        onClick = {
                            val rate = if (selectedCurrency == baseCurrency) 1.0 else rateValue
                            onAddPaymentLine(
                                PaymentLine(
                                    modeOfPayment = selectedMode,
                                    enteredAmount = amountValue,
                                    currency = selectedCurrency,
                                    exchangeRate = rate,
                                    baseAmount = amountValue * rate,
                                    referenceNumber = referenceInput.takeIf { it.isNotBlank() })
                            )
                            amountInput = ""
                            amountValue = 0.0
                            referenceInput = ""
                            if (selectedCurrency == baseCurrency) {
                                rateInput = "1.0"
                            }
                        },
                        enabled = canAdd
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar pago")
                    }
                }
            }
        )

        if (requiresReference) {
            AppTextField(
                value = referenceInput,
                onValueChange = { referenceInput = it },
                label = "Número de referencia",
                placeholder = "#11231",
                leadingIcon = { Icon(Icons.Default.ConfirmationNumber, contentDescription = null) },
                supportingText = {
                    if (referenceInput.isBlank()) {
                        Text("Requerido para pagos con ${selectedMode}.")
                    }
                },
                isError = referenceInput.isBlank(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
        }

        if (!paymentErrorMessage.isNullOrBlank()) {
            Text(
                text = paymentErrorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

enum class DiscountInputType { Code, Percent, Amount }

@Composable
private fun DiscountShippingInputs(
    state: BillingState.Success, action: BillingAction
) {
    val baseCurrency = state.currency ?: "USD"
    val initialType = remember(
        state.manualDiscountAmount,
        state.discountCode,
        state.manualDiscountPercent,
    ) {
        when {
            state.manualDiscountAmount > 0.0 -> DiscountInputType.Amount
            state.discountCode.isNotBlank() -> DiscountInputType.Code
            state.manualDiscountPercent > 0.0 -> DiscountInputType.Percent
            else -> DiscountInputType.Percent
        }
    }

    var discountType by rememberSaveable(initialType) { mutableStateOf(initialType) }

    fun selectDiscountType(type: DiscountInputType) {
        discountType = type
        when (type) {
            DiscountInputType.Amount -> {
                action.onDiscountCodeChanged("")
                action.onManualDiscountPercentChanged("")

            }

            DiscountInputType.Code -> {
                action.onManualDiscountAmountChanged("")
                action.onManualDiscountPercentChanged("")
            }

            DiscountInputType.Percent -> {
                action.onDiscountCodeChanged("")
                action.onManualDiscountAmountChanged("")
            }
        }
    }

    Column(
        modifier = Modifier.padding(end = 12.dp, start = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Descuento", style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = discountType == DiscountInputType.Amount,
                onClick = { selectDiscountType(DiscountInputType.Amount) },
                label = { Text("Monto") }
            )
            FilterChip(
                selected = discountType == DiscountInputType.Code,
                onClick = { selectDiscountType(DiscountInputType.Code) },
                label = { Text("Codigo") }
            )
            FilterChip(
                selected = discountType == DiscountInputType.Percent,
                onClick = { selectDiscountType(DiscountInputType.Percent) },
                label = { Text("Porcentaje") }
            )
        }
        val paymentModes = state.paymentModes
        val modeOptions =
            remember(paymentModes) { paymentModes.map { it.modeOfPayment }.distinct() }
        var selectedMode by remember(modeOptions) { mutableStateOf("") }
        val selectedCurrency = remember(state, selectedMode) {
            state.paymentModeCurrencyByMode[selectedMode] ?: baseCurrency
        }
        var amountInput by remember { mutableStateOf(state.manualDiscountAmount.toString()) }
        var amountValue by remember { mutableStateOf(state.manualDiscountAmount) }
        var rateInput by remember { mutableStateOf("1.0") }
        when (discountType) {
            DiscountInputType.Amount -> {
                MoneyTextField(
                    currencyCode = selectedCurrency,
                    rawValue = if (state.manualDiscountAmount > 0.0) amountInput else "",
                    onRawValueChange = {
                        amountInput = it
                        action.onManualDiscountAmountChanged(amountInput)
                    },
                    label = "Monto (${baseCurrency.toCurrencySymbol()})",
                    enabled = true,
                    onAmountChanged = {
                        amountValue = it
                    },
                    supportingText = {
                        if (!selectedCurrency.equals(baseCurrency, ignoreCase = true)) {
                            val rate = rateInput.toDoubleOrNull() ?: 0.0
                            val base = amountValue * rate
                            Text("Base: ${formatAmount(baseCurrency.toCurrencySymbol(), base)}")
                        }
                    },
                    trailingIcon = { Icon(Icons.Default.Money, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            DiscountInputType.Code -> {
                AppTextField(
                    value = state.discountCode,
                    onValueChange = action.onDiscountCodeChanged,
                    label = "Codigo de descuento",
                    placeholder = "Ingresa el codigo de descuento",
                    trailingIcon = { Icon(Icons.Default.Money, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            DiscountInputType.Percent -> {
                AppTextField(
                    value = if (state.manualDiscountPercent > 0.0) state.manualDiscountPercent.toString() else "",
                    onValueChange = action.onManualDiscountPercentChanged,
                    label = "Porcentaje (%)",
                    trailingIcon = { Icon(Icons.Default.Percent, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        if (state.deliveryCharges.isNotEmpty()) {
            Text("Envío", style = MaterialTheme.typography.bodyMedium)
            val deliveryChargeLabel =
                state.selectedDeliveryCharge?.label ?: "Selecciona cargo de envío"
            var deliveryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = deliveryExpanded, onExpandedChange = { deliveryExpanded = it }) {
                AppTextField(
                    value = deliveryChargeLabel,
                    onValueChange = {},
                    label = "Cargo de envío",
                    modifier = Modifier.fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = deliveryExpanded)
                    })
                ExposedDropdownMenu(
                    expanded = deliveryExpanded, onDismissRequest = { deliveryExpanded = false }) {
                    DropdownMenuItem(text = { Text("Sin envío") }, onClick = {
                        action.onDeliveryChargeSelected(null)
                        deliveryExpanded = false
                    })
                    state.deliveryCharges.forEach { charge ->
                        val chargeLabel = "${
                            charge.label
                        } (${formatAmount(baseCurrency.toCurrencySymbol(), charge.defaultRate)})"
                        DropdownMenuItem(text = { Text(chargeLabel) }, onClick = {
                            action.onDeliveryChargeSelected(charge)
                            deliveryExpanded = false
                        })
                    }
                }
            }
        }
        Text(
            "Se aplicará el descuento manual o código según corresponda.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start
        )
    }
}

@Composable
private fun CreditTermsSection(
    isCreditSale: Boolean,
    paymentTerms: List<PaymentTermBO>,
    selectedPaymentTerm: PaymentTermBO?,
    onCreditSaleChanged: (Boolean) -> Unit,
    onPaymentTermSelected: (PaymentTermBO?) -> Unit
) {
    Column(
        modifier = Modifier.padding(end = 12.dp, start = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val canEnableCredit = paymentTerms.isNotEmpty()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Venta de credito", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = isCreditSale,
                onCheckedChange = onCreditSaleChanged,
                enabled = canEnableCredit
            )
        }

        if (isCreditSale) {
            Text("Condiciones de pago", style = MaterialTheme.typography.bodyMedium)
            var templateExpanded by remember { mutableStateOf(false) }
            val templateLabel = selectedPaymentTerm?.name ?: "Selecciona la condicion de pago"
            ExposedDropdownMenuBox(
                expanded = templateExpanded, onExpandedChange = { templateExpanded = it }) {
                AppTextField(
                    value = templateLabel,
                    onValueChange = {},
                    label = "Selecciona la condicion de pago",
                    placeholder = "Condicion de pago",
                    modifier = Modifier//.fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateExpanded) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) })
                ExposedDropdownMenu(
                    expanded = templateExpanded, onDismissRequest = { templateExpanded = false }) {
                    paymentTerms.forEach { term ->
                        DropdownMenuItem(text = { Text(term.name) }, onClick = {
                            onPaymentTermSelected(term)
                            templateExpanded = false
                        })
                    }
                }
            }
            selectedPaymentTerm?.let { term ->
                val creditDays = term.creditDays ?: 0
                val creditMonths = term.creditMonths ?: 0
                val termsLabel = buildString {
                    if (creditMonths > 0) {
                        append("$creditMonths mes(es)")
                    }
                    if (creditDays > 0) {
                        if (isNotEmpty()) append(" + ")
                        append("$creditDays dia(s)")
                    }
                }.ifBlank { "Mismo dia" }
                Text(
                    text = "Terminos: $termsLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                term.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else if (!canEnableCredit) {
            Text(
                text = "No hay terminos de pago disponibles, Ventas de credito deshabilitadas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Double.formatQty(): String {
    return if (this % 1.0 == 0.0) {
        this.toInt().toString()
    } else {
        this.toString()
    }
}

private fun requiresReference(option: POSPaymentModeOption?): Boolean {
    val type = option?.type?.trim().orEmpty()
    return type.equals("Bank", ignoreCase = true) || type.equals(
        "Card", ignoreCase = true
    ) || option?.modeOfPayment?.contains(
        "bank", ignoreCase = true
    ) == true || option?.modeOfPayment?.contains("card", ignoreCase = true) == true
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    supportingText: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        singleLine = singleLine,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

data class MoneyUiSpec(
    val code: String,
    val decimals: Int = 2,
    val groupSep: Char = ',',
    val decimalSep: Char = '.',
)

private fun moneyUiSpec(currencyCode: String, fallbackDecimals: Int = 2): MoneyUiSpec {
    return when (val code = currencyCode.trim().uppercase()) {
        "NIO", "USD" -> MoneyUiSpec(code = code, decimals = 2, groupSep = ',', decimalSep = '.')
        "EUR" -> MoneyUiSpec(code = code, decimals = 2, groupSep = '.', decimalSep = ',')
        else -> MoneyUiSpec(
            code = code, decimals = fallbackDecimals, groupSep = ',', decimalSep = '.'
        )
    }
}

private fun sanitizeMoneyInput(input: String, maxDecimals: Int): String {
    val s = input.trim().replace(" ", "")
    val filtered = s.filter { it.isDigit() || it == '.' || it == ',' }
    if (filtered.isBlank()) return ""

    val lastDot = filtered.lastIndexOf('.')
    val lastComma = filtered.lastIndexOf(',')
    val decIdx = maxOf(lastDot, lastComma)

    fun normalizeInt(digits: String): String = digits.trimStart('0').ifBlank { "0" }

    return if (decIdx >= 0) {
        val intDigits = filtered.take(decIdx).filter { it.isDigit() }
        val decDigits = filtered.substring(decIdx + 1).filter { it.isDigit() }.take(maxDecimals)
        normalizeInt(intDigits) + "." + decDigits
    } else {
        val intDigits = filtered.filter { it.isDigit() }
        normalizeInt(intDigits)
    }
}

private fun parseMoneyToDouble(raw: String): Double =
    raw.trim().let { if (it.endsWith(".")) it.dropLast(1) else it }.toDoubleOrNull() ?: 0.0

private fun normalizeRawMoneyInput(input: String, maxDecimals: Int): String {
    val s = input.trim().replace(" ", "")
    if (s.isBlank()) return ""

    // Permitimos dígitos y separadores . ,
    val filtered = s.filter { it.isDigit() || it == '.' || it == ',' }
    if (filtered.isBlank()) return ""

    // Usamos el ÚLTIMO separador como decimal, el resto se considera miles
    val lastDot = filtered.lastIndexOf('.')
    val lastComma = filtered.lastIndexOf(',')
    val decIdx = maxOf(lastDot, lastComma)

    fun cleanIntDigits(d: String): String {
        val digits = d.filter { it.isDigit() }
        // Evita "00012" -> "12"
        val trimmed = digits.trimStart('0')
        return trimmed.ifBlank { "0" }
    }

    return if (decIdx >= 0) {
        val intDigits = cleanIntDigits(filtered.take(decIdx))
        val decDigits = filtered.substring(decIdx + 1).filter { it.isDigit() }.take(maxDecimals)
        "$intDigits.$decDigits" // raw siempre con '.'
    } else {
        cleanIntDigits(filtered)
    }
}

private class MoneyVisualTransformation(
    private val spec: MoneyUiSpec,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val normalized = normalizeRawMoneyInput(raw, spec.decimals)

        // Mantén el raw original (lo que el usuario edita) tal cual; el display se basa en normalized.
        // Para el mapping, usamos el mismo "raw" (porque offsets se miden sobre text.text).
        // OJO: lo ideal es que tu TextField.value SIEMPRE sea normalized.
        // (abajo te lo dejo así, para que el mapping sea exacto)
        val value = normalized

        val dotIndex = value.indexOf('.')
        val hasDot = dotIndex >= 0
        val intPart = if (hasDot) value.take(dotIndex) else value
        val decRaw = if (hasDot) value.substring(dotIndex + 1) else ""

        // --- build grouped int + mapping int offsets ---
        val n = intPart.length
        val mapIntOffsets = IntArray(n + 1)
        val groupedInt = StringBuilder()

        var rawIntOffset = 0
        for (ch in intPart) {
            mapIntOffsets[rawIntOffset] = groupedInt.length
            groupedInt.append(ch)
            rawIntOffset++

            val remaining = n - rawIntOffset
            if (remaining > 0 && remaining % 3 == 0) {
                groupedInt.append(spec.groupSep)
            }
        }
        mapIntOffsets[n] = groupedInt.length

        val decShown = decRaw.take(spec.decimals).padEnd(spec.decimals, '0')
        val transformed = buildString {
            append(groupedInt)
            append(spec.decimalSep)
            append(decShown)
        }

        val intTransLen = groupedInt.length
        val decStart = intTransLen + 1

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val o = offset.coerceIn(0, value.length)

                // offsets dentro del entero
                val intLen = intPart.length
                if (!hasDot) {
                    return if (o <= intLen) mapIntOffsets[o] else intTransLen
                }

                // has dot
                return when {
                    o <= intLen -> mapIntOffsets[o]
                    o == intLen + 1 -> decStart // cursor “después del punto”
                    else -> {
                        val decOffset = (o - (intLen + 1)).coerceAtMost(spec.decimals)
                        decStart + decOffset
                    }
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                val t = offset.coerceIn(0, transformed.length)

                // dentro del entero (incluye comas)
                if (t <= intTransLen) {
                    // busca el mayor rawIntOffset cuyo mapped <= t
                    var i = 0
                    while (i < mapIntOffsets.size && mapIntOffsets[i] <= t) i++
                    return (i - 1).coerceAtLeast(0)
                }

                // en la parte decimal (siempre existe visualmente)
                if (!hasDot) {
                    // si no hay punto en raw, no permitimos que el cursor se meta en los decimales “falsos”
                    return intPart.length
                }

                // hay punto en raw: permitimos editar decimales
                if (t == intTransLen) return intPart.length
                if (t == decStart) return intPart.length + 1

                val decOffset = (t - decStart).coerceIn(0, spec.decimals)
                val rawDecLen = decRaw.length.coerceAtMost(spec.decimals)
                val clamped = decOffset.coerceAtMost(rawDecLen)
                return (intPart.length + 1 + clamped)
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyTextField(
    currencyCode: String,
    rawValue: String,
    onRawValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Monto",
    enabled: Boolean = true,
    isError: Boolean = false,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    imeAction: ImeAction = ImeAction.Next,
    onAmountChanged: (Double) -> Unit = {}
) {
    val spec = remember(currencyCode) { moneyUiSpec(currencyCode) }
    val symbol = remember(currencyCode) {
        val s = currencyCode.toCurrencySymbol()
        s.ifBlank { currencyCode }
    }
    val transformation = remember(spec) { MoneyVisualTransformation(spec) }

    var tfv by remember(rawValue, spec.decimals) {
        val sanitized = sanitizeMoneyInput(rawValue, spec.decimals)
        mutableStateOf(TextFieldValue(sanitized, selection = TextRange(sanitized.length)))
    }

    LaunchedEffect(tfv.text) {
        onAmountChanged(parseMoneyToDouble(tfv.text))
    }

    TextField(
        value = tfv,
        onValueChange = { typed ->
            val sanitized = sanitizeMoneyInput(typed.text, spec.decimals)
            tfv = TextFieldValue(
                text = sanitized,
                selection = TextRange(sanitized.length)
            )
            onRawValueChange(sanitized)
        },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        label = { Text(label) },
        prefix = {
            Text(
                text = symbol.ifBlank { symbol },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
        },
        trailingIcon = trailingIcon,
        singleLine = true,
        enabled = enabled,
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = imeAction
        ),
        visualTransformation = transformation,
        shape = RoundedCornerShape(14.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.22f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}

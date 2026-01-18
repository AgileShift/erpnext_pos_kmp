@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.domain.models.DeliveryChargeBO
import com.erpnext.pos.utils.formatAmount
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.utils.resolveRateBetweenFromBaseRates
import com.erpnext.pos.views.billing.BillingAction
import com.erpnext.pos.views.billing.BillingState
import com.erpnext.pos.views.billing.PaymentLine
import com.erpnext.pos.domain.models.POSCurrencyOption
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.domain.models.PaymentTermBO
import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.moneyScale
import com.erpnext.pos.utils.oauth.toDouble
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarHost
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import com.erpnext.pos.views.salesflow.SalesFlowContextSummary
import com.erpnext.pos.views.salesflow.SalesFlowSource
import org.jetbrains.compose.ui.tooling.preview.Preview


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
    state: BillingState, action: BillingAction,
    snackbar: SnackbarController
) {
    val uiSnackbar = snackbar.snackbar.collectAsState().value

    Box(Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            sheetShadowElevation = 12.dp,
            containerColor = MaterialTheme.colorScheme.background,
            sheetContainerColor = MaterialTheme.colorScheme.surface,
            sheetContentColor = MaterialTheme.colorScheme.onSurface,
            topBar = {
                TopAppBar(
                    title = {
                        Text("Nueva Factura")
                    }, navigationIcon = {
                        IconButton(onClick = action.onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás"
                            )
                        }
                    }, actions = {
                        TextButton(onClick = action.onOpenLab) {
                            Text("Modo prueba")
                        }
                    }, colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }, sheetPeekHeight = 120.dp, sheetDragHandle = {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Box(
                            modifier = Modifier.size(width = 48.dp, height = 6.dp)
                                .padding(horizontal = 12.dp)
                        )
                    }
                }
            }, sheetContent = {
                val sheetState = when (state) {
                    is BillingState.Success -> state
                    is BillingState.Error -> state.previous
                    else -> null
                }
                if (sheetState != null) {
                    TotalsPaymentsSheet(
                        state = sheetState, action = action
                    )
                } else {
                    Spacer(Modifier.height(1.dp))
                }
            }) { padding ->

            when (state) {
                is BillingState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is BillingState.Success -> {
                    BillingContent(
                        state = state, action = action, padding = padding, snackbar = snackbar
                    )
                }

                is BillingState.Empty -> {
                    LaunchedEffect(Unit) {
                        snackbar.show(
                            "No hay productos en el carrito",
                            SnackbarType.Info,
                            SnackbarPosition.Top
                        )
                    }
                }

                is BillingState.Error -> {
                    LaunchedEffect(state.message) {
                        snackbar.dismiss()
                        snackbar.show(state.message, SnackbarType.Error, SnackbarPosition.Top)
                    }
                    val previous = state.previous
                    if (previous != null) {
                        BillingContent(
                            state = previous,
                            action = action,
                            padding = padding,
                            snackbar = snackbar
                        )
                    } else {
                        Box(
                            modifier = Modifier.padding(padding).fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = action.onBack) {
                                    Text("Volver")
                                }
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingLabScreen(
    state: BillingState,
    action: BillingAction,
    snackbar: SnackbarController
) {
    val uiSnackbar = snackbar.snackbar.collectAsState().value
    val colors = MaterialTheme.colorScheme
    var step by rememberSaveable { mutableStateOf(LabCheckoutStep.Cart) }

    // Si salimos de Success, regresamos al primer paso.
    LaunchedEffect(state) {
        if (state !is BillingState.Success) {
            step = LabCheckoutStep.Cart
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = colors.background,
            topBar = {
                TopAppBar(
                    title = { Text("POS Lab") },
                    navigationIcon = {
                        IconButton(onClick = action.onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás"
                            )
                        }
                    },
                    actions = {
                        TextButton(onClick = action.onBack) {
                            Text("Volver a clásico")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            },
        ) { paddingValues ->
            when (state) {
                BillingState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is BillingState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        snackbar.dismiss()
                        snackbar.show(state.message, SnackbarType.Error, SnackbarPosition.Top)
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
                    when (step) {
                        LabCheckoutStep.Cart -> BillingLabContent(
                            state = state,
                            action = action,
                            onCheckout = { step = LabCheckoutStep.Checkout },
                            onStepSelected = { step = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = paddingValues.calculateTopPadding())
                        )
                        LabCheckoutStep.Checkout -> BillingLabCheckoutStep(
                            state = state,
                            action = action,
                            onStepSelected = { step = it },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = paddingValues.calculateTopPadding())
                        )
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
    onStepSelected: (LabCheckoutStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val accent = colors.primary
    val background = colors.background
    val leftPanelBg = colors.surfaceVariant
    val baseCurrency = state.currency ?: "USD"

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
        LabStepper(current = LabCheckoutStep.Cart, onStepSelected = onStepSelected)
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
                        baseCurrency = baseCurrency,
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

                CollapsibleSection(title = "Cliente", defaultExpanded = true) {
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
                                baseCurrency = baseCurrency,
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
                        Divider(color = colors.outlineVariant, thickness = 1.dp)
                        PaymentTotalsRow("Subtotal", baseCurrency, state.subtotal)
                        if (state.taxes > 0.0) {
                            PaymentTotalsRow("Impuestos", baseCurrency, state.taxes)
                        }
                        if (state.discount > 0.0) {
                            PaymentTotalsRow("Descuento", baseCurrency, -state.discount)
                        }
                        if (state.shippingAmount > 0.0) {
                            PaymentTotalsRow("Envío", baseCurrency, state.shippingAmount)
                        }
                        PaymentTotalsRow("Total", baseCurrency, state.total)
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

@Composable
private fun BillingLabCheckoutStep(
    state: BillingState.Success,
    action: BillingAction,
    onStepSelected: (LabCheckoutStep) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val baseCurrency = state.currency ?: "USD"
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LabStepper(current = LabCheckoutStep.Checkout, onStepSelected = onStepSelected)
        // Encabezado principal del checkout.
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Checkout",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onSurface
            )
            Text(
                text = "Revisa y confirma la venta",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
        // Tarjeta hero con total, cliente y saldo.
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surface)
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
                    text = formatAmount(baseCurrency.toCurrencySymbol(), state.total),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Cliente",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = state.selectedCustomer?.customerName ?: "--",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Artículos",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = state.cartItems.size.toString(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurface
                        )
                    }
                }
                Divider(color = colors.outlineVariant, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pagado",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = formatAmount(baseCurrency.toCurrencySymbol(), state.paidAmountBase),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Pendiente",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = formatAmount(baseCurrency.toCurrencySymbol(), state.balanceDueBase),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        if (state.paymentTerms.isNotEmpty()) {
            // Sección de crédito con estilo de tarjeta.
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.elevatedCardColors(containerColor = colors.surface)
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
        // Sección de descuento y envío con estilo compacto.
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surface)
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
        // Sección de pagos completa con multimoneda y métodos de pago.
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(containerColor = colors.surface)
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
                Text(
                    text = "Métodos disponibles: ${state.paymentModes.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant
                )
                PaymentSection(
                    state = state,
                    baseCurrency = baseCurrency,
                    exchangeRateByCurrency = state.exchangeRateByCurrency,
                    paymentLines = state.paymentLines,
                    paymentModes = state.paymentModes,
                    allowedCurrencies = state.allowedCurrencies,
                    paidAmountBase = state.paidAmountBase,
                    totalAmount = state.total,
                    balanceDueBase = state.balanceDueBase,
                    changeDueBase = state.changeDueBase,
                    paymentErrorMessage = state.paymentErrorMessage,
                    isCreditSale = state.isCreditSale,
                    onAddPaymentLine = action.onAddPaymentLine,
                    onRemovePaymentLine = action.onRemovePaymentLine,
                    onPaymentCurrencySelected = action.onPaymentCurrencySelected
                )
            }
        }
        Button(
            onClick = action.onFinalizeSale,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.selectedCustomer != null &&
                    state.cartItems.isNotEmpty() &&
                    (state.isCreditSale || state.paidAmountBase + 0.01 >= state.total) &&
                    (!state.isCreditSale || state.selectedPaymentTerm != null)
        ) {
            Text("Pagar")
        }
    }
}

@Composable
private fun LabStepper(
    current: LabCheckoutStep,
    onStepSelected: (LabCheckoutStep) -> Unit
) {
    // Stepper simple para visualizar el flujo.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LabStepTile(
            title = "Carrito",
            isActive = current == LabCheckoutStep.Cart,
            onClick = { onStepSelected(LabCheckoutStep.Cart) }
        )
        LabStepTile(
            title = "Checkout",
            isActive = current == LabCheckoutStep.Checkout,
            onClick = { onStepSelected(LabCheckoutStep.Checkout) }
        )
    }
}

@Composable
private fun LabStepTile(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val container = if (isActive) colors.primaryContainer else colors.surfaceVariant
    val content = if (isActive) colors.onPrimaryContainer else colors.onSurfaceVariant
    Card(
        modifier = Modifier.weight(1f).clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = container)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = title, style = MaterialTheme.typography.labelMedium, color = content)
        }
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
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            }
        },
        placeholder = { Text("Buscar productos o escanear...") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp)
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
            kotlinx.coroutines.delay(180)
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

@Composable
private fun LabTotalsCard(
    baseCurrency: String,
    subtotal: Double,
    taxes: Double
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        color = colors.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SummaryRow("Subtotal", baseCurrency, subtotal)
            if (taxes > 0.0) {
                SummaryRow("Impuestos", baseCurrency, taxes)
            }
        }
    }
}

@Composable
private fun BillingContent(
    state: BillingState.Success,
    action: BillingAction,
    padding: PaddingValues,
    snackbar: SnackbarController
) {
    var showSourceSheet by remember { mutableStateOf(false) }

    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbar.show(it, SnackbarType.Success, SnackbarPosition.Top)
            action.onClearSuccessMessage()
        }
    }
    LaunchedEffect(state.isFinalizingSale) {
        if (state.isFinalizingSale) {
            snackbar.show(
                "Guardando la factura localmente. Se sincronizará automáticamente cuando haya conexión.",
                SnackbarType.Loading,
                SnackbarPosition.Top
            )
        } else if (state.successMessage == null) {
            snackbar.dismiss()
        }
    }
    Column(
        modifier = Modifier.padding(padding).fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        CollapsibleSection(
            title = "Cliente", defaultExpanded = true
        ) {
            Column(
                modifier = Modifier.padding(end = 12.dp, start = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CustomerSelector(
                    customers = state.customers,
                    query = state.customerSearchQuery,
                    onQueryChange = action.onCustomerSearchQueryChange,
                    onCustomerSelected = action.onCustomerSelected
                )
                SourceDocumentRow(
                    hasSource = state.salesFlowContext?.sourceType != null,
                    onLink = { showSourceSheet = true },
                    onClear = action.onClearSource
                )
                state.salesFlowContext?.let { context ->
                    SalesFlowContextSummary(context)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        CollapsibleSection(
            title = "Carrito", defaultExpanded = false
        ) {
            ProductSelector(
                query = state.productSearchQuery,
                onQueryChange = action.onProductSearchQueryChange,
                results = state.productSearchResults,
                onProductAdded = action.onProductAdded
            )

            CartList(
                cartItems = state.cartItems,
                currency = state.currency ?: "USD",
                onQuantityChanged = { itemCode, newQuantity ->
                    action.onQuantityChanged(itemCode, newQuantity)
                },
                onRemoveItem = action.onRemoveItem
            )
            if (!state.cartErrorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = state.cartErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Spacer(Modifier.height(4.dp))
    }

    if (showSourceSheet) {
        SourceDocumentSheet(
            state = state,
            onDismiss = { showSourceSheet = false },
            onLoad = action.onLoadSourceDocuments,
            onApply = { source, reference ->
                action.onLinkSource(source, reference)
                showSourceSheet = false
            }
        )
    }
}

@Composable
private fun SourceDocumentRow(
    hasSource: Boolean,
    onLink: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (hasSource) "Source document linked" else "Link a source document",
            style = MaterialTheme.typography.bodySmall
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hasSource) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
            TextButton(onClick = onLink) { Text("Link") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceDocumentSheet(
    state: BillingState.Success,
    onDismiss: () -> Unit,
    onLoad: (SalesFlowSource) -> Unit,
    onApply: (SalesFlowSource, String) -> Unit
) {
    var sourceType by remember { mutableStateOf(SalesFlowSource.SalesOrder) }
    var reference by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(sourceType) {
        reference = ""
    }

    LaunchedEffect(sourceType, state.selectedCustomer?.name) {
        if (state.selectedCustomer != null) {
            onLoad(sourceType)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Link source document",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = when (sourceType) {
                        SalesFlowSource.Quotation -> "Quotation"
                        SalesFlowSource.SalesOrder -> "Sales Order"
                        SalesFlowSource.DeliveryNote -> "Delivery Note"
                        SalesFlowSource.Customer -> "Customer"
                    },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Source type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryEditable).fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    listOf(
                        SalesFlowSource.Quotation,
                        SalesFlowSource.SalesOrder,
                        SalesFlowSource.DeliveryNote
                    ).forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    when (option) {
                                        SalesFlowSource.Quotation -> "Quotation"
                                        SalesFlowSource.SalesOrder -> "Sales Order"
                                        SalesFlowSource.DeliveryNote -> "Delivery Note"
                                        SalesFlowSource.Customer -> "Customer"
                                    }
                                )
                            },
                            onClick = {
                                sourceType = option
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (state.selectedCustomer == null) {
                Text(
                    text = "Select a customer to load documents.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Filter by ID or status") },
                    modifier = Modifier.fillMaxWidth()
                )

                when {
                    state.isLoadingSourceDocuments -> {
                        CircularProgressIndicator()
                    }

                    !state.sourceDocumentsError.isNullOrBlank() -> {
                        Text(
                            text = state.sourceDocumentsError.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    state.sourceDocuments.isEmpty() -> {
                        Text(
                            text = "No documents found.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    else -> {
                        val filtered = state.sourceDocuments.filter { doc ->
                            val query = searchQuery.trim()
                            if (query.isBlank()) true else {
                                doc.id.contains(query, ignoreCase = true) ||
                                        (doc.status?.contains(query, ignoreCase = true) == true)
                            }
                        }
                        filtered.forEach { doc ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (doc.id == reference) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { reference = doc.id }
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = doc.id,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Date: ${doc.date ?: "N/A"}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        text = "Status: ${doc.status ?: "Unknown"}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = { onApply(sourceType, reference) },
                modifier = Modifier.fillMaxWidth(),
                enabled = reference.isNotBlank()
            ) {
                Text("Apply")
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

    //Text("Cliente", style = MaterialTheme.typography.titleMedium)
    ExposedDropdownMenuBox(
        expanded = expanded && hasCustomers, onExpandedChange = { expanded = it }) {
        AppTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                expanded = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .onGloballyPositioned { anchorWidthPx = it.size.width },
            /*.onFocusChanged { focusState ->
                expanded = focusState.isFocused
            }*/
            label = "Buscar o Seleccionar",
            placeholder = "Nombre, codigo, telefono...",
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )
        DropdownMenu(
            expanded = expanded && hasCustomers,
            onDismissRequest = { expanded = false },
            properties = PopupProperties(focusable = false)
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
private fun ProductSelector(
    query: String,
    onQueryChange: (String) -> Unit,
    results: List<ItemBO>,
    onProductAdded: (ItemBO) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var displayLimit by remember { mutableStateOf(50) }
    var anchorWidthPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    val hasResults = results.isNotEmpty()
    val context = LocalPlatformContext.current

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Text("Producto", style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(
            expanded = expanded && hasResults, onExpandedChange = { expanded = it }) {
            AppTextField(
                value = query,
                onValueChange = {
                    onQueryChange(it)
                    expanded = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                    .onGloballyPositioned { anchorWidthPx = it.size.width }/*.onFocusChanged { focusState ->
                    expanded = focusState.isFocused
                }*/,
                label = "Buscar por nombre o código",
                placeholder = "Nombre, codigo...",
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) })

            DropdownMenu(
                expanded = expanded && hasResults,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = false)
            ) {
                val menuWidth = remember(anchorWidthPx) {
                    if (anchorWidthPx > 0) with(density) { anchorWidthPx.toDp() } else 420.dp
                }
                Column(
                    modifier = Modifier
                        .width(menuWidth.coerceIn(320.dp, 640.dp))
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    results.take(displayLimit).forEach { item ->
                        DropdownMenuItem(text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = remember(item.image) {
                                        ImageRequest.Builder(context)
                                            .data(item.image?.ifBlank { "https://placehold.co/64x64" })
                                            .crossfade(true).build()
                                    },
                                    contentDescription = item.name,
                                    modifier = Modifier.size(40.dp)
                                        .clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Crop
                                )
                                Column {
                                    Text(item.name)
                                    Text(
                                        text = "Código: ${item.itemCode}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Text(
                                        "Precio: ${
                                            formatAmount(
                                                item.currency?.toCurrencySymbol() ?: "", item.price
                                            )
                                        }",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "Disponible: ${item.actualQty.formatQty()}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }, onClick = {
                            onProductAdded(item)
                            onQueryChange("")
                            expanded = false
                        })
                    }
                    if (results.size > displayLimit) {
                        DropdownMenuItem(
                            text = { Text("Mostrar más...") },
                            onClick = { displayLimit += 50 }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSection(
    title: String, defaultExpanded: Boolean, content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(defaultExpanded) }
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable(
                    interactionSource = interactionSource, indication = ripple(bounded = true)
                ) { expanded = !expanded }.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                IconButton(
                    onClick = { expanded = !expanded }, modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Colapsar sección" else "Expandir sección"
                    )
                }
            }
            if (expanded) {
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun CartList(
    cartItems: List<CartItem>,
    currency: String,
    onQuantityChanged: (String, Double) -> Unit,
    onRemoveItem: (String) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
        if (cartItems.isEmpty()) {
            Text(
                "Carrito vacío",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                "Artículos (${cartItems.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp, max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(cartItems, key = { it.itemCode }) { item ->
                    CartItemRow(
                        item = item,
                        onQuantityChanged = { newQuantity ->
                            onQuantityChanged(item.itemCode, newQuantity)
                        },
                        onRemoveItem = { onRemoveItem(item.itemCode) },
                        currency = currency
                    )
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem, onQuantityChanged: (Double) -> Unit, onRemoveItem: () -> Unit, currency: String
) {
    val subtotal = item.price * item.quantity
    val displayQty = item.quantity.formatQty()

    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Qty $displayQty • ${
                        formatAmount(
                            currency.toCurrencySymbol(), item.price
                        )
                    }",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatAmount(currency.toCurrencySymbol(), subtotal),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onQuantityChanged(item.quantity - 1.0) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = "Decrease quantity"
                    )
                }
                IconButton(
                    onClick = { onQuantityChanged(item.quantity + 1.0) },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.Add, contentDescription = "Increase quantity"
                    )
                }
                IconButton(
                    onClick = onRemoveItem, modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, symbol: String, amount: Double, bold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            formatAmount(symbol.toCurrencySymbol(), amount),
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun PaymentTotalsRow(label: String, symbol: String, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = formatAmount(symbol.toCurrencySymbol(), amount),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CompactTotalsCard(
    baseCurrency: String,
    subtotal: Double,
    taxes: Double,
    discount: Double,
    shipping: Double,
    total: Double,
    balance: Double,
    change: Double
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 0.dp,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "Resumen",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurface
            )
            PaymentTotalsRow("Subtotal", baseCurrency, subtotal)
            if (taxes > 0.0) PaymentTotalsRow("Impuestos", baseCurrency, taxes)
            if (discount > 0.0) PaymentTotalsRow("Descuento", baseCurrency, -discount)
            if (shipping > 0.0) PaymentTotalsRow("Envío", baseCurrency, shipping)
            Divider(color = colors.outlineVariant, thickness = 1.dp)
            PaymentTotalsRow("Total", baseCurrency, total)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Pendiente",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    formatAmount(baseCurrency.toCurrencySymbol(), balance),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Cambio",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant
                )
                Text(
                    formatAmount(baseCurrency.toCurrencySymbol(), change),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TotalsPaymentsSheet(
    state: BillingState.Success, action: BillingAction
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Totales + Pagos", style = MaterialTheme.typography.titleMedium
            )
        }
        item {
            val currency = state.currency ?: "USD"
            CollapsibleSection(title = "Totales + Descuentos + Envío", defaultExpanded = true) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SummaryRow("Subtotal", currency, state.subtotal)
                    SummaryRow("Impuestos", currency, state.taxes)
                    SummaryRow("Descuento", currency, state.discount)
                    SummaryRow("Envío", currency, state.shippingAmount)
                    HorizontalDivider()
                    SummaryRow("Total", currency, state.total, bold = true)
                }
                Spacer(Modifier.height(12.dp))
                DiscountShippingInputs(
                    state = state, action = action
                )
            }
        }
        item {
            CollapsibleSection(title = "Pagos", defaultExpanded = true) {
                PaymentSection(
                    state = state,
                    baseCurrency = state.currency ?: "USD",
                    exchangeRateByCurrency = state.exchangeRateByCurrency,
                    paymentLines = state.paymentLines,
                    paymentModes = state.paymentModes,
                    allowedCurrencies = state.allowedCurrencies,
                    paidAmountBase = state.paidAmountBase,
                    totalAmount = state.total,
                    balanceDueBase = state.balanceDueBase,
                    changeDueBase = state.changeDueBase,
                    paymentErrorMessage = state.paymentErrorMessage,
                    isCreditSale = state.isCreditSale,
                    onAddPaymentLine = action.onAddPaymentLine,
                    onRemovePaymentLine = action.onRemovePaymentLine,
                    onPaymentCurrencySelected = action.onPaymentCurrencySelected
                )
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PaymentSection(
    state: BillingState,
    baseCurrency: String,
    exchangeRateByCurrency: Map<String, Double>,
    paymentLines: List<PaymentLine>,
    paymentModes: List<POSPaymentModeOption>,
    allowedCurrencies: List<POSCurrencyOption>,
    paidAmountBase: Double,
    totalAmount: Double,
    balanceDueBase: Double,
    changeDueBase: Double,
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
    val modeCurrency = remember(state, selectedMode) {
        (state as? BillingState.Success)?.paymentModeCurrencyByMode?.get(selectedMode)
    }
    var selectedCurrency by remember(selectedMode, baseCurrency) {
        mutableStateOf(modeCurrency ?: baseCurrency)
    }
    var amountInput by remember { mutableStateOf("") }
    var amountValue by remember { mutableStateOf(0.0) }
    var rateInput by remember { mutableStateOf("1.0") }
    var referenceInput by remember { mutableStateOf("") }
    val successState = state as? BillingState.Success

    LaunchedEffect(selectedMode, modeCurrency, baseCurrency) {
        selectedCurrency = modeCurrency?.trim()?.uppercase() ?: baseCurrency
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

        /*if (isCreditSale) {
            Text(
                "Pagos deshabilitados para ventas de crédito.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }*/

        if (paymentLines.isEmpty()) {
            // Estado vacío con tarjeta para mayor claridad visual.
            Card(
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
                        enter = fadeIn(animationSpec = tween(180)),
                        exit = fadeOut(animationSpec = tween(160))
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(line.modeOfPayment, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "Monto: ${
                                            formatAmount(
                                                line.currency.toCurrencySymbol(), line.enteredAmount
                                            )
                                        }"
                                    )
                                    Text(
                                        "Base: ${
                                            formatAmount(
                                                baseCurrency.toCurrencySymbol(), line.baseAmount
                                            )
                                        }"
                                    )
                                    Text("Tasa: ${line.exchangeRate}")
                                    if (!line.referenceNumber.isNullOrBlank()) {
                                        Text("Referencia: ${line.referenceNumber}")
                                    }
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
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
        Text(
            text = "Moneda principal: $baseCurrency",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        Text("Moneda de pago: $selectedCurrency", style = MaterialTheme.typography.bodyMedium)
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
    var usePercent by rememberSaveable(state.manualDiscountPercent, state.manualDiscountAmount) {
        mutableStateOf(state.manualDiscountPercent > 0.0 || state.manualDiscountAmount == 0.0)
    }

    val initialType = remember(
        state.discountCode,
        state.manualDiscountPercent,
        state.manualDiscountAmount
    ) {
        when {
            state.discountCode.isNotBlank() -> DiscountInputType.Code
            state.manualDiscountPercent > 0.0 -> DiscountInputType.Percent
            state.manualDiscountAmount > 0.0 -> DiscountInputType.Amount
            else -> DiscountInputType.Percent
        }
    }

    var discountType by rememberSaveable(initialType) { mutableStateOf(initialType) }

    fun selectDiscountType(type: DiscountInputType) {
        discountType = type
        when (type) {
            DiscountInputType.Code -> {
                action.onManualDiscountAmountChanged("")
                action.onManualDiscountPercentChanged("")
            }

            DiscountInputType.Percent -> {
                action.onDiscountCodeChanged("")
                action.onManualDiscountAmountChanged("")
            }

            DiscountInputType.Amount -> {
                action.onDiscountCodeChanged("")
                action.onManualDiscountPercentChanged("")
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
                selected = discountType == DiscountInputType.Code,
                onClick = { selectDiscountType(DiscountInputType.Code) },
                label = { Text("Codigo") }
            )
            FilterChip(
                selected = discountType == DiscountInputType.Percent,
                onClick = { selectDiscountType(DiscountInputType.Percent) },
                label = { Text("Porcentaje") }
            )
            FilterChip(
                selected = discountType == DiscountInputType.Amount,
                onClick = { selectDiscountType(DiscountInputType.Amount) },
                label = { Text("Monto") }
            )
        }
        when (discountType) {
            DiscountInputType.Code -> {
                AppTextField(
                    value = state.discountCode,
                    onValueChange = action.onDiscountCodeChanged,
                    label = "Discount code",
                    placeholder = "Enter discount code",
                    trailingIcon = { Icon(Icons.Default.Money, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            DiscountInputType.Percent -> {
                AppTextField(
                    value = if (state.manualDiscountPercent > 0.0) state.manualDiscountPercent.toString() else "",
                    onValueChange = action.onManualDiscountPercentChanged,
                    label = "Percent (%)",
                    trailingIcon = { Icon(Icons.Default.Percent, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            DiscountInputType.Amount -> {
                AppTextField(
                    value = if (state.manualDiscountAmount > 0.0) state.manualDiscountAmount.toString() else "",
                    onValueChange = action.onManualDiscountAmountChanged,
                    label = "Amount (${baseCurrency.toCurrencySymbol()})",
                    trailingIcon = { Icon(Icons.Default.Money, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number, imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Text("Envío", style = MaterialTheme.typography.bodyMedium)
        val deliveryChargeLabel = state.selectedDeliveryCharge?.label ?: "Selecciona cargo de envío"
        var deliveryExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = deliveryExpanded, onExpandedChange = { deliveryExpanded = it }) {
            AppTextField(
                value = deliveryChargeLabel,
                onValueChange = {},
                label = "Cargo de envío",
                modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
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
    val code = currencyCode.trim().uppercase()
    return when (code) {
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
        val intDigits = filtered.substring(0, decIdx).filter { it.isDigit() }
        val decDigits = filtered.substring(decIdx + 1).filter { it.isDigit() }.take(maxDecimals)
        normalizeInt(intDigits) + "." + decDigits
    } else {
        val intDigits = filtered.filter { it.isDigit() }
        normalizeInt(intDigits)
    }
}

private fun formatWithGrouping(intPart: String, groupSep: Char): String =
    intPart.ifBlank { "0" }.reversed().chunked(3).joinToString(groupSep.toString()).reversed()

private fun formatMoneyDisplay(raw: String, spec: MoneyUiSpec, focused: Boolean): String {
    if (raw.isBlank()) return ""
    val parts = raw.split('.', limit = 2)
    val intPart = parts.getOrNull(0).orEmpty()
    val decPart = parts.getOrNull(1).orEmpty()

    return if (focused) {
        // En edición: NO estorbar, NO agrupar, NO forzar .00
        if (raw.contains('.')) intPart + spec.decimalSep + decPart else intPart
    } else {
        val grouped = formatWithGrouping(intPart, spec.groupSep)
        val padded = decPart.padEnd(spec.decimals, '0').take(spec.decimals)
        grouped + spec.decimalSep + padded
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
        return if (trimmed.isBlank()) "0" else trimmed
    }

    return if (decIdx >= 0) {
        val intDigits = cleanIntDigits(filtered.substring(0, decIdx))
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
        val intPart = if (hasDot) value.substring(0, dotIndex) else value
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
        val decSepPos = intTransLen
        val decStart = decSepPos + 1

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
                if (t == decSepPos) return intPart.length
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

    val display = remember(tfv.text, spec) {
        formatMoneyDisplay(tfv.text, spec, true)
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

@Preview
@Composable
private fun BillingScreenPreview() {
    val sampleCustomers = listOf(
        CustomerBO(
            name = "Cliente Ejemplo 1",
            customerName = "Cliente Ejemplo 1",
            customerType = "Individual"
        ), CustomerBO(
            name = "Cliente Frecuente 2",
            customerName = "Cliente Ejemplo 2",
            customerType = "Company"
        )
    )
    val sampleProducts = listOf(
        ItemBO(
            itemCode = "P1", name = "Producto A", price = 150.0, description = "", uom = "Unidad"
        ), ItemBO(
            itemCode = "P2", name = "Producto B", price = 2500.0, description = "", uom = "Unidad"
        )
    )
    val sampleCart = listOf(
        CartItem("P1", "Producto A", "C$", 2.0, 150.0)
    )
    val deliveryCharges = listOf(
        DeliveryChargeBO(
            label = "Zona urbana", defaultRate = 10.0
        )
    )

    MaterialTheme {
        BillingScreen(
            state = BillingState.Success(
                customers = sampleCustomers,
                selectedCustomer = sampleCustomers.first(),
                productSearchResults = sampleProducts,
                cartItems = sampleCart,
                subtotal = 300.0,
                taxes = 45.0,
                currency = "USD",
                discount = 0.0,
                shippingAmount = 10.0,
                deliveryCharges = deliveryCharges,
                selectedDeliveryCharge = deliveryCharges.first(),
                total = 355.0,
                paymentLines = listOf(
                    PaymentLine(
                        modeOfPayment = "Cash",
                        enteredAmount = 100.0,
                        currency = "USD",
                        exchangeRate = 1.0,
                        baseAmount = 100.0
                    )
                ),
                paymentModes = listOf(
                    POSPaymentModeOption(
                        name = "Cash",
                        modeOfPayment = "Cash",
                    )
                ),
                paymentTerms = listOf(
                    PaymentTermBO(
                        name = "Layaway 30 days", creditDays = 30
                    )
                ),
                selectedPaymentTerm = null,
                allowedCurrencies = listOf(
                    POSCurrencyOption(
                        code = "USD", name = "US Dollar", symbol = "$"
                    )
                ),
                paidAmountBase = 100.0,
                balanceDueBase = 255.0,
                changeDueBase = 0.0,
                exchangeRate = 1.0
            ), action = BillingAction(),
            snackbar = SnackbarController()
        )
    }
}

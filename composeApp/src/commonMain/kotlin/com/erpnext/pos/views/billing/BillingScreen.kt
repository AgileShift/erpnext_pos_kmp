@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.foundation.shape.RoundedCornerShape
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.domain.models.ItemBO
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.erpnext.pos.utils.formatAmount
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.views.billing.BillingAction
import com.erpnext.pos.views.billing.BillingState
import com.erpnext.pos.views.billing.PaymentLine
import com.erpnext.pos.domain.models.POSCurrencyOption
import com.erpnext.pos.domain.models.POSPaymentModeOption
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject


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
    action: BillingAction
) {
    val snackbar = koinInject<SnackbarController>()

    BottomSheetScaffold(
        sheetShadowElevation = 12.dp,
        topBar = {
            TopAppBar(
                title = {
                    Text("Nueva Factura")
                },
                navigationIcon = {
                    IconButton(onClick = action.onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás"
                        )
                    }
                })
        },
        sheetPeekHeight = 140.dp,
        sheetDragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 6.dp)
                            .padding(horizontal = 12.dp)
                    )
                }
            }
        },
        sheetContent = {
            val sheetState = when (state) {
                is BillingState.Success -> state
                is BillingState.Error -> state.previous
                else -> null
            }
            if (sheetState != null) {
                TotalsPaymentsSheet(
                    state = sheetState,
                    action = action
                )
            } else {
                Spacer(Modifier.height(1.dp))
            }
        }
    ) { padding ->

        when (state) {
            is BillingState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is BillingState.Success -> {
                BillingContent(
                    state = state,
                    action = action,
                    padding = padding,
                    snackbar = snackbar
                )
            }

            is BillingState.Empty -> {
                snackbar.show(
                    "No hay productos en el carrito",
                    SnackbarType.Info,
                    SnackbarPosition.Top
                )
            }

            is BillingState.Error -> {
                LaunchedEffect(state.message) {
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
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
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
}

@Composable
private fun BillingContent(
    state: BillingState.Success,
    action: BillingAction,
    padding: PaddingValues,
    snackbar: SnackbarController
) {
    LaunchedEffect(state.successMessage) {
        state.successMessage?.let {
            snackbar.show(it, SnackbarType.Success, SnackbarPosition.Top)
            action.onClearSuccessMessage()
        }
    }
    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        CollapsibleSection(
            title = "Cliente",
            defaultExpanded = true
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
            }
        }

        Spacer(Modifier.height(8.dp))

        CollapsibleSection(
            title = "Carrito",
            defaultExpanded = false
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

        if (state.paymentTerms.isNotEmpty()) {
            CollapsibleSection(
                title = "Terminos de credito",
                defaultExpanded = false
            ) {
                CreditTermsSection(
                    isCreditSale = state.isCreditSale,
                    paymentTerms = state.paymentTerms,
                    selectedPaymentTerm = state.selectedPaymentTerm,
                    onCreditSaleChanged = action.onCreditSaleChanged,
                    onPaymentTermSelected = action.onPaymentTermSelected
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = action.onFinalizeSale,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.selectedCustomer != null &&
                    state.cartItems.isNotEmpty() &&
                    (state.isCreditSale || state.paidAmountBase >= state.total) &&
                    (!state.isCreditSale || state.selectedPaymentTerm != null)
        ) {
            Text("Finalizar venta")
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
    val hasCustomers = customers.isNotEmpty()

    //Text("Cliente", style = MaterialTheme.typography.titleMedium)
    ExposedDropdownMenuBox(
        expanded = expanded && hasCustomers,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onQueryChange(it)
                expanded = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable)
                .onFocusChanged { focusState ->
                    expanded = focusState.isFocused
                },
            label = { Text("Buscar o Seleccionar Cliente") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded && hasCustomers,
            onDismissRequest = { expanded = false }
        ) {
            customers.forEach { customer ->
                DropdownMenuItem(
                    text = { Text("${customer.name} - ${customer.customerName}") },
                    onClick = {
                        onCustomerSelected(customer)
                        expanded = false
                    })
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
    val hasResults = results.isNotEmpty()
    val context = LocalPlatformContext.current

    Column(modifier = Modifier.padding(horizontal = 12.dp)) {
        Text("Producto", style = MaterialTheme.typography.titleMedium)
        ExposedDropdownMenuBox(
            expanded = expanded && hasResults,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    onQueryChange(it)
                    expanded = true
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                    .onFocusChanged { focusState ->
                        expanded = focusState.isFocused
                    },
                label = { Text("Buscar por nombre o código") },
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
            )

            ExposedDropdownMenu(
                expanded = expanded && hasResults,
                onDismissRequest = { expanded = false }
            ) {
                results.forEach { item ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SubcomposeAsyncImage(
                                    model = remember(item.image) {
                                        ImageRequest.Builder(context)
                                            .data(item.image?.ifBlank { "https://placehold.co/64x64" })
                                            .crossfade(true)
                                            .build()
                                    },
                                    contentDescription = item.name,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Crop,
                                    loading = {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(
                                                16.dp
                                            )
                                        )
                                    },
                                    error = {
                                        AsyncImage(
                                            model = "https://placehold.co/64x64",
                                            contentDescription = "placeholder",
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
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
                                                item.currency ?: "USD",
                                                item.price
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
                        },
                        onClick = {
                            onProductAdded(item)
                            onQueryChange("")
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSection(
    title: String,
    defaultExpanded: Boolean,
    content: @Composable () -> Unit
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
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = interactionSource,
                        indication = ripple(bounded = true)
                    ) { expanded = !expanded }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(28.dp)
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
                        currency = item.currency ?: currency
                    )
                }
            }
        }
    }
}

@Composable
private fun CartItemRow(
    item: CartItem,
    onQuantityChanged: (Double) -> Unit,
    onRemoveItem: () -> Unit,
    currency: String
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
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
                            currency.toCurrencySymbol(),
                            item.price
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
                        Icons.Default.Add,
                        contentDescription = "Increase quantity"
                    )
                }
                IconButton(
                    onClick = onRemoveItem,
                    modifier = Modifier.size(26.dp)
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
private fun TotalsPaymentsSheet(
    state: BillingState.Success,
    action: BillingAction
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Totales + Pagos",
                style = MaterialTheme.typography.titleMedium
            )
        }
        item {
            val currency = state.currency ?: "USD"
            CollapsibleSection(title = "Totales + Descuentos + Envío", defaultExpanded = true) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 12.dp),
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
                    state = state,
                    action = action
                )
            }
        }
        item {
            CollapsibleSection(title = "Pagos", defaultExpanded = true) {
                PaymentSection(
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
    val defaultMode = paymentModes.firstOrNull()?.modeOfPayment.orEmpty()
    var selectedMode by remember(modeOptions, defaultMode) { mutableStateOf(defaultMode) }
    val selectedModeOption = paymentModes.firstOrNull { it.modeOfPayment == selectedMode }
    val requiresReference = remember(selectedModeOption) {
        requiresReference(selectedModeOption)
    }
    val allowedCodes = remember(allowedCurrencies, baseCurrency) {
        val codes = allowedCurrencies.map { it.code }.filter { it.isNotBlank() }
        codes.ifEmpty { listOf(baseCurrency) }
    }
    var selectedCurrency by remember(allowedCodes, baseCurrency) {
        mutableStateOf(allowedCodes.firstOrNull() ?: baseCurrency)
    }
    var amountInput by remember { mutableStateOf("") }
    var rateInput by remember { mutableStateOf("1.0") }
    var referenceInput by remember { mutableStateOf("") }
    val currencyOptions = remember(allowedCodes, baseCurrency) {
        val baseOptions = allowedCodes.ifEmpty { listOf(baseCurrency) }
        val ensuredBase = if (baseOptions.any { it.equals(baseCurrency, ignoreCase = true) }) {
            baseOptions
        } else {
            baseOptions + baseCurrency
        }
        ensuredBase.distinct()
    }

    LaunchedEffect(currencyOptions, baseCurrency) {
        val resolved = currencyOptions.firstOrNull {
            it.equals(baseCurrency, ignoreCase = true)
        } ?: currencyOptions.firstOrNull() ?: baseCurrency
        selectedCurrency = resolved
        if (resolved.equals(baseCurrency, ignoreCase = true)) {
            rateInput = "1.0"
        }
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

        if (isCreditSale) {
            Text(
                "Pagos deshabilitados para ventas de crédito.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        if (paymentLines.isEmpty()) {
            Text(
                "Sin pagos registrados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                paymentLines.forEachIndexed { index, line ->
                    Surface(
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        shape = MaterialTheme.shapes.medium,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                                            line.currency.toCurrencySymbol(),
                                            line.enteredAmount
                                        )
                                    }"
                                )
                                Text(
                                    "Base: ${
                                        formatAmount(
                                            baseCurrency.toCurrencySymbol(),
                                            line.baseAmount
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

        Spacer(Modifier.height(12.dp))

        if (!isCreditSale) {
            Text("Modo de pago", style = MaterialTheme.typography.bodyMedium)
            var modeExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = modeExpanded,
                onExpandedChange = { modeExpanded = it }
            ) {
                OutlinedTextField(
                    value = selectedMode,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = modeExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = modeExpanded,
                    onDismissRequest = { modeExpanded = false }
                ) {
                    modeOptions.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode) },
                            onClick = {
                                selectedMode = mode
                                modeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Moneda base de factura: $baseCurrency",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it },
                label = { Text("Monto") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            if (requiresReference) {
                OutlinedTextField(
                    value = referenceInput,
                    onValueChange = { referenceInput = it },
                    label = { Text("Número de referencia") },
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val amountValue = amountInput.toDoubleOrNull()
                val rateValue = rateInput.toDoubleOrNull()
                val canAdd = amountValue != null &&
                        amountValue > 0.0 &&
                        (rateValue != null && rateValue > 0.0) &&
                        selectedMode.isNotBlank() &&
                        (!requiresReference || referenceInput.isNotBlank()) &&
                        !isCreditSale

                Button(
                    onClick = {
                        val amount = amountValue ?: return@Button
                        val rate = if (selectedCurrency == baseCurrency) 1.0 else rateValue ?: 1.0
                        onAddPaymentLine(
                            PaymentLine(
                                modeOfPayment = selectedMode,
                                enteredAmount = amount,
                                currency = selectedCurrency,
                                exchangeRate = rate,
                                baseAmount = amount * rate,
                                referenceNumber = referenceInput.takeIf { it.isNotBlank() }
                            )
                        )
                        amountInput = ""
                        referenceInput = ""
                        if (selectedCurrency == baseCurrency) {
                            rateInput = "1.0"
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = canAdd
                ) {
                    Text("Agregar pago")
                }

                OutlinedButton(
                    onClick = {
                        if (paymentLines.isNotEmpty()) {
                            onRemovePaymentLine(paymentLines.lastIndex)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = paymentLines.isNotEmpty()
                ) {
                    Text("Eliminar")
                }
            }

            if (!paymentErrorMessage.isNullOrBlank()) {
                Text(
                    text = paymentErrorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(Modifier.height(12.dp))

            SummaryRow("Pagado (base)", baseCurrency, paidAmountBase, bold = true)
            SummaryRow("Balance pendiente", baseCurrency, balanceDueBase, bold = true)
            SummaryRow("Cambio", baseCurrency, changeDueBase, bold = true)
        }
    }
}

@Composable
private fun DiscountShippingInputs(
    state: BillingState.Success,
    action: BillingAction
) {
    val baseCurrency = state.currency ?: "USD"
    Column(
        modifier = Modifier.padding(end = 12.dp, start = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Descuento manual", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = if (state.manualDiscountPercent > 0.0) state.manualDiscountPercent.toString() else "",
            onValueChange = action.onManualDiscountPercentChanged,
            label = { Text("Porcentaje (%)") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = if (state.manualDiscountAmount > 0.0) state.manualDiscountAmount.toString() else "",
            onValueChange = action.onManualDiscountAmountChanged,
            label = { Text("Monto (${baseCurrency.toCurrencySymbol()})") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text("Código de descuento", style = MaterialTheme.typography.bodyMedium)
        OutlinedTextField(
            value = state.discountCode,
            onValueChange = action.onDiscountCodeChanged,
            label = { Text("Código") },
            modifier = Modifier.fillMaxWidth()
        )
        Text("Envío", style = MaterialTheme.typography.bodyMedium)
        val deliveryChargeLabel = state.selectedDeliveryCharge?.label ?: "Selecciona cargo de envío"
        var deliveryExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = deliveryExpanded,
            onExpandedChange = { deliveryExpanded = it }
        ) {
            OutlinedTextField(
                value = deliveryChargeLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Cargo de envío") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = deliveryExpanded)
                }
            )
            ExposedDropdownMenu(
                expanded = deliveryExpanded,
                onDismissRequest = { deliveryExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Sin envío") },
                    onClick = {
                        action.onDeliveryChargeSelected(null)
                        deliveryExpanded = false
                    }
                )
                state.deliveryCharges.forEach { charge ->
                    val chargeLabel = "${
                        charge.label
                    } (${formatAmount(baseCurrency.toCurrencySymbol(), charge.defaultRate)})"
                    DropdownMenuItem(
                        text = { Text(chargeLabel) },
                        onClick = {
                            action.onDeliveryChargeSelected(charge)
                            deliveryExpanded = false
                        }
                    )
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
    paymentTerms: List<com.erpnext.pos.domain.models.PaymentTermBO>,
    selectedPaymentTerm: com.erpnext.pos.domain.models.PaymentTermBO?,
    onCreditSaleChanged: (Boolean) -> Unit,
    onPaymentTermSelected: (com.erpnext.pos.domain.models.PaymentTermBO?) -> Unit
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
                expanded = templateExpanded,
                onExpandedChange = { templateExpanded = it }
            ) {
                OutlinedTextField(
                    value = templateLabel,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Selecciona la condicion de pago") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = templateExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = templateExpanded,
                    onDismissRequest = { templateExpanded = false }
                ) {
                    paymentTerms.forEach { term ->
                        DropdownMenuItem(
                            text = { Text(term.name) },
                            onClick = {
                                onPaymentTermSelected(term)
                                templateExpanded = false
                            }
                        )
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
    return type.equals("Bank", ignoreCase = true) ||
            type.equals("Card", ignoreCase = true) ||
            option?.modeOfPayment?.contains("bank", ignoreCase = true) == true ||
            option?.modeOfPayment?.contains("card", ignoreCase = true) == true
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
        com.erpnext.pos.domain.models.DeliveryChargeBO(
            label = "Zona urbana",
            defaultRate = 10.0
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
                    com.erpnext.pos.domain.models.PaymentTermBO(
                        name = "Layaway 30 days",
                        creditDays = 30
                    )
                ),
                selectedPaymentTerm = null,
                allowedCurrencies = listOf(
                    POSCurrencyOption(
                        code = "USD",
                        name = "US Dollar",
                        symbol = "$"
                    )
                ),
                paidAmountBase = 100.0,
                balanceDueBase = 255.0,
                changeDueBase = 0.0,
                exchangeRate = 1.0
            ), action = BillingAction()
        )
    }
}

@file:OptIn(ExperimentalMaterial3Api::class)

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
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
    state: BillingState,
    action: BillingAction
) {
    //val snackbar = koinInject<SnackbarController>()

    BottomSheetScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Nueva Factura")
                },
                navigationIcon = {
                    IconButton(onClick = action.onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                })
        },
        sheetPeekHeight = 140.dp,
        sheetDragHandle = { BottomSheetDefaults.DragHandle() },
        sheetContent = {
            if (state is BillingState.Success) {
                TotalsPaymentsSheet(
                    state = state,
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
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    CollapsibleSection(
                        title = "Cliente + Productos",
                        defaultExpanded = true
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            CustomerSelector(
                                customers = state.customers,
                                query = state.customerSearchQuery,
                                onQueryChange = action.onCustomerSearchQueryChange,
                                onCustomerSelected = action.onCustomerSelected
                            )

                            ProductSelector(
                                query = state.productSearchQuery,
                                onQueryChange = action.onProductSearchQueryChange,
                                results = state.productSearchResults,
                                onProductAdded = action.onProductAdded
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    CollapsibleSection(
                        title = "Carrito",
                        defaultExpanded = true
                    ) {
                        CartList(
                            cartItems = state.cartItems,
                            currency = state.currency ?: "USD",
                            onQuantityChanged = { itemCode, newQuantity ->
                                action.onQuantityChanged(itemCode, newQuantity)
                            },
                            onRemoveItem = action.onRemoveItem
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = action.onFinalizeSale,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.selectedCustomer != null &&
                            state.cartItems.isNotEmpty() &&
                            state.paidAmountBase >= state.total
                    ) {
                        Text("Finalizar venta")
                    }
                }
            }

            is BillingState.Empty -> {
                /*snackbar.show(
                    "No hay productos en el carrito",
                    SnackbarType.Info,
                    SnackbarPosition.Top
                )*/
            }

            is BillingState.Error -> {
                //snackbar.show(state.message, SnackbarType.Error, SnackbarPosition.Top)
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
    val hasCustomers = customers.isNotEmpty()

    Text("Cliente", style = MaterialTheme.typography.titleMedium)
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
                                loading = { CircularProgressIndicator(modifier = Modifier.size(16.dp)) },
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
                                    text = "Codigo: ${item.itemCode}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Precio: ${formatAmount(item.currency ?: "USD", item.price)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
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

@Composable
private fun CollapsibleSection(
    title: String,
    defaultExpanded: Boolean,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(defaultExpanded) }
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
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
                        contentDescription = if (expanded) "Collapse section" else "Expand section"
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
            "Items (${cartItems.size})",
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
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.small
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
                    text = "Qty $displayQty • ${formatAmount(currency.toCurrencySymbol(), item.price)}",
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SummaryRow("Subtotal", currency, state.subtotal)
                    SummaryRow("Impuestos", currency, state.taxes)
                    SummaryRow("Descuento", currency, state.discount)
                    SummaryRow("Envío", currency, state.shippingAmount)
                    HorizontalDivider()
                    SummaryRow("Total", currency, state.total, bold = true)
                }
            }
        }
        item {
            CollapsibleSection(title = "Pagos", defaultExpanded = true) {
                PaymentSection(
                    baseCurrency = state.currency ?: "USD",
                    paymentLines = state.paymentLines,
                    paymentModes = state.paymentModes,
                    allowedCurrencies = state.allowedCurrencies,
                    paidAmountBase = state.paidAmountBase,
                    totalAmount = state.total,
                    balanceDueBase = state.balanceDueBase,
                    changeDueBase = state.changeDueBase,
                    paymentErrorMessage = state.paymentErrorMessage,
                    onAddPaymentLine = action.onAddPaymentLine,
                    onRemovePaymentLine = action.onRemovePaymentLine
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
    paymentLines: List<PaymentLine>,
    paymentModes: List<POSPaymentModeOption>,
    allowedCurrencies: List<POSCurrencyOption>,
    paidAmountBase: Double,
    totalAmount: Double,
    balanceDueBase: Double,
    changeDueBase: Double,
    paymentErrorMessage: String?,
    onAddPaymentLine: (PaymentLine) -> Unit,
    onRemovePaymentLine: (Int) -> Unit
) {
    val modeOptions = remember(paymentModes) { paymentModes.map { it.modeOfPayment }.distinct() }
    val defaultMode = paymentModes.firstOrNull { it.isDefault }?.modeOfPayment
        ?: modeOptions.firstOrNull().orEmpty()
    var selectedMode by remember(modeOptions, defaultMode) { mutableStateOf(defaultMode) }
    val selectedModeOption = paymentModes.firstOrNull { it.modeOfPayment == selectedMode }
    val requiresReference = remember(selectedModeOption) {
        requiresReference(selectedModeOption)
    }
    val allowedCodes = remember(allowedCurrencies, baseCurrency) {
        val codes = allowedCurrencies.map { it.code }.filter { it.isNotBlank() }
        if (codes.isEmpty()) listOf(baseCurrency) else codes
    }
    var selectedCurrency by remember(allowedCodes, baseCurrency) {
        mutableStateOf(allowedCodes.firstOrNull() ?: baseCurrency)
    }
    var amountInput by remember { mutableStateOf("") }
    var rateInput by remember { mutableStateOf("1.0") }
    var referenceInput by remember { mutableStateOf("") }
    val modeCurrency = paymentModes.firstOrNull { it.modeOfPayment == selectedMode }?.currency
    val currencyOptions = remember(allowedCodes, baseCurrency, modeCurrency) {
        val baseOptions = allowedCodes.ifEmpty { listOf(baseCurrency) }
        val filtered = if (!modeCurrency.isNullOrBlank()) {
            baseOptions.filter { it.equals(modeCurrency, ignoreCase = true) }
        } else {
            baseOptions
        }
        val ensuredBase = if (filtered.any { it.equals(baseCurrency, ignoreCase = true) }) {
            filtered
        } else {
            filtered + baseCurrency
        }
        ensuredBase.distinct()
    }

    LaunchedEffect(modeCurrency, currencyOptions, baseCurrency) {
        val preferredCurrency = modeCurrency ?: baseCurrency
        val resolved = currencyOptions.firstOrNull {
            it.equals(preferredCurrency, ignoreCase = true)
        } ?: currencyOptions.firstOrNull() ?: baseCurrency
        selectedCurrency = resolved
        if (resolved.equals(baseCurrency, ignoreCase = true)) {
            rateInput = "1.0"
        }
    }

    LaunchedEffect(selectedMode) {
        referenceInput = ""
    }

    if (paymentLines.isEmpty()) {
        Text(
            "Sin pagos registrados",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            paymentLines.forEachIndexed { index, line ->
                Surface(
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(line.modeOfPayment, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Amount: ${
                                    formatAmount(
                                        line.currency.toCurrencySymbol(),
                                        line.enteredAmount
                                    )
                                }"
                            )
                            Text(
                                "Base: ${formatAmount(baseCurrency.toCurrencySymbol(), line.baseAmount)}"
                            )
                            Text("Rate: ${line.exchangeRate}")
                            if (!line.referenceNumber.isNullOrBlank()) {
                                Text("Reference: ${line.referenceNumber}")
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

    Text("Moneda", style = MaterialTheme.typography.bodyMedium)
    var currencyExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = currencyExpanded,
        onExpandedChange = { currencyExpanded = it }
    ) {
        OutlinedTextField(
            value = selectedCurrency,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = currencyExpanded) }
        )
        ExposedDropdownMenu(
            expanded = currencyExpanded,
            onDismissRequest = { currencyExpanded = false }
        ) {
            currencyOptions.forEach { currency ->
                DropdownMenuItem(
                    text = { Text(currency) },
                    onClick = {
                        selectedCurrency = currency
                        if (currency == baseCurrency) {
                            rateInput = "1.0"
                        }
                        currencyExpanded = false
                    }
                )
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = amountInput,
        onValueChange = { amountInput = it },
        label = { Text("Monto") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(12.dp))

    OutlinedTextField(
        value = rateInput,
        onValueChange = { rateInput = it },
        label = { Text("Tasa de cambio") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        enabled = selectedCurrency != baseCurrency,
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(Modifier.height(12.dp))

    if (requiresReference) {
        OutlinedTextField(
            value = referenceInput,
            onValueChange = { referenceInput = it },
            label = { Text("Reference number") },
            supportingText = {
                if (referenceInput.isBlank()) {
                    Text("Required for ${selectedMode} payments.")
                }
            },
            isError = referenceInput.isBlank(),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val amountValue = amountInput.toDoubleOrNull()
        val rateValue = rateInput.toDoubleOrNull()
        val currencyAllowed = modeCurrency == null ||
            modeCurrency.equals(selectedCurrency, ignoreCase = true)
        val canAdd = amountValue != null &&
            amountValue > 0.0 &&
            (rateValue != null && rateValue > 0.0) &&
            currencyAllowed &&
            selectedMode.isNotBlank() &&
            (!requiresReference || referenceInput.isNotBlank())

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

    if (!modeCurrency.isNullOrBlank() &&
        !modeCurrency.equals(selectedCurrency, ignoreCase = true)
    ) {
        Text(
            text = "Currency must be $modeCurrency for $selectedMode.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
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
                        currency = "USD",
                        isDefault = true
                    )
                ),
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

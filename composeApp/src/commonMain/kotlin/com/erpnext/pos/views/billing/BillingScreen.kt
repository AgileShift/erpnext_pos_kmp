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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.erpnext.pos.utils.formatAmount
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.views.billing.BillingAction
import com.erpnext.pos.views.billing.BillingState
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

    Scaffold(
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
        }) { padding ->

        when (state) {
            is BillingState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is BillingState.Success -> {
                Column(
                    modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize()
                ) {
                    // Customer Selection Dropdown
                    CustomerSelector(
                        customers = state.customers,
                        selectedCustomer = state.selectedCustomer,
                        query = state.customerSearchQuery,
                        onQueryChange = action.onCustomerSearchQueryChange,
                        onCustomerSelected = action.onCustomerSelected
                    )

                    Spacer(Modifier.height(16.dp))

                    // Product Search
                    ProductSelector(
                        query = state.productSearchQuery,
                        onQueryChange = action.onProductSearchQueryChange,
                        results = state.productSearchResults,
                        onProductAdded = action.onProductAdded
                    )

                    Spacer(Modifier.height(16.dp))

                    // Cart Items Header
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("Artículo", Modifier.weight(2f))
                        Text("Cant.", Modifier.weight(1.2f), textAlign = TextAlign.Center)
                        Text("Tarifa", Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("SubTotal", Modifier.weight(1f), textAlign = TextAlign.End)
                        Spacer(Modifier.width(40.dp)) // For delete icon
                    }
                    HorizontalDivider()

                    // Cart Items List
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.cartItems, key = { it.itemCode }) { item ->
                            CartItemRow(
                                item = item, onQuantityChanged = { newQuantity ->
                                    action.onQuantityChanged(item.itemCode, newQuantity)
                                }, onRemoveItem = { action.onRemoveItem(item.itemCode) },
                                currency = state.currency ?: item.currency ?: "C$"
                            )
                            HorizontalDivider()
                        }
                    }

                    // Sale Summary
                    Spacer(Modifier.height(16.dp))
                    SummaryRow("Subtotal", state.currency.toString(), state.subtotal)
                    SummaryRow("Impuestos", state.currency.toString(), state.taxes)
                    SummaryRow("Descuento", state.currency.toString(), state.discount)
                    HorizontalDivider()
                    SummaryRow("Total", state.currency.toString(), state.total, bold = true)

                    Spacer(Modifier.height(16.dp))

                    // Finalize Button
                    Button(
                        onClick = action.onFinalizeSale,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.selectedCustomer != null && state.cartItems.isNotEmpty()
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
    selectedCustomer: CustomerBO?,
    query: String,
    onQueryChange: (String) -> Unit,
    onCustomerSelected: (CustomerBO) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Text("Cliente", style = MaterialTheme.typography.titleMedium)
    ExposedDropdownMenuBox(
        expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = selectedCustomer?.customerName ?: query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth().menuAnchor(),
            label = { Text("Buscar o Seleccionar Cliente") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            singleLine = true
        )
        ExposedDropdownMenu(
            expanded = expanded, onDismissRequest = { expanded = false }) {
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
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        expanded = false
                    }
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
                        //onQueryChange("")
                        expanded = false
                    }
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

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(item.name, Modifier.weight(2f), style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = item.quantity.toString(),
            onValueChange = { qty ->
                onQuantityChanged(qty.toDoubleOrNull() ?: 0.0)
            },
            modifier = Modifier.weight(1.2f).padding(horizontal = 4.dp),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Text(
            text = formatAmount(currency.toCurrencySymbol(), item.price),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        Text(
            text = formatAmount(currency.toCurrencySymbol(), subtotal),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
        IconButton(onClick = onRemoveItem, modifier = Modifier.width(40.dp)) {
            Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
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
                total = 345.0,
                exchangeRate = 36.6243
            ), action = BillingAction()
        )
    }
}
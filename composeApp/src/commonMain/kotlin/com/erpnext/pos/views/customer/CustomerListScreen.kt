package com.erpnext.pos.views.customer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.domain.models.CustomerBO
import com.erpnext.pos.utils.toCurrencySymbol
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerListScreen(
    state: CustomerState,
    actions: CustomerAction
) {
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState()
    )

    var searchQuery by remember { mutableStateOf("") }
    var selectedState by remember { mutableStateOf("Todos") }

    val customers = remember(state) {
        if (state is CustomerState.Success) state.customers else emptyList()
    }

    val filterElevation by animateDpAsState(
        targetValue = if (customers.isNotEmpty()) 4.dp else 0.dp,
        label = "filterElevation"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("Clientes", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    IconButton(onClick = actions.fetchAll) {
                        Icon(Icons.Filled.Refresh, "Actualizar Clientes")
                    }
                },
                scrollBehavior = topAppBarScrollBehavior
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Filtros y búsqueda
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = filterElevation,
                shadowElevation = filterElevation
            ) {
                CustomerFilters(
                    searchQuery = searchQuery,
                    selectedState = selectedState,
                    onQueryChange = {
                        searchQuery = it
                        actions.onSearchQueryChanged(it)
                    },
                    onStateChange = {
                        selectedState = it ?: "Todos"
                        actions.onStateSelected(it)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contenido principal según estado
            // Slot-based rendering: estructura fija con visibilidad dinámica
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                // SLOT: LISTA DE CLIENTES
                androidx.compose.animation.AnimatedVisibility(
                    visible = state is CustomerState.Success &&
                            (state.customers.isNotEmpty()),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val filtered = customers.filter { customer ->
                        customer.customerName.contains(searchQuery, ignoreCase = true) ||
                                (customer.mobileNo ?: "").contains(searchQuery)
                    }

                    if (filtered.isEmpty()) {
                        EmptyStateMessage(
                            message = if (searchQuery.isEmpty())
                                "No hay clientes disponibles."
                            else
                                "No se encontraron clientes que coincidan con tu búsqueda.",
                            icon = Icons.Filled.People
                        )
                    } else {
                        CustomerListContent(filtered, actions)
                    }
                }

                // SLOT: LOADING
                androidx.compose.animation.AnimatedVisibility(
                    visible = state is CustomerState.Loading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    CustomerShimmerList()
                }

                // SLOT: EMPTY
                androidx.compose.animation.AnimatedVisibility(
                    visible = state is CustomerState.Empty,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    EmptyStateMessage(
                        message = "No hay clientes disponibles.",
                        icon = Icons.Filled.People
                    )
                }

                // SLOT: ERROR
                androidx.compose.animation.AnimatedVisibility(
                    visible = state is CustomerState.Error,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    FullScreenErrorMessage(
                        errorMessage = (state as CustomerState.Error).message,
                        onRetry = actions.fetchAll
                    )
                }
            }

        }
    }
}

@Composable
private fun CustomerFilters(
    searchQuery: String,
    selectedState: String,
    states: List<String> = listOf("Pendientes", "Sin Pendientes"),
    onQueryChange: (String) -> Unit,
    onStateChange: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                val isSelected = selectedState == "Todos"
                FilterChipItem("Todos", isSelected) { onStateChange("Todos") }
            }
            items(states) { state ->
                val isSelected = selectedState == state
                FilterChipItem(state, isSelected) { onStateChange(state) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        SearchTextField(
            searchQuery = searchQuery,
            onSearchQueryChange = onQueryChange,
            placeholderText = "Buscar cliente por nombre o teléfono..."
        )
    }
}

@Composable
fun SearchTextField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholderText: String = "Buscar...",
    onSearchAction: (() -> Unit)? = null
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value =
            searchQuery,
        onValueChange = { query -> onSearchQueryChange(query) },
        modifier = modifier.fillMaxWidth().padding(vertical = 8.dp),
        placeholder = {
            Text(
                placeholderText,
                style = MaterialTheme.typography.bodyLarge,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                maxLines = 1
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Icono de búsqueda",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onSearchQueryChange("")
                        keyboardController?.show()
                    }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = "Borrar búsqueda",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(imeAction = if (onSearchAction != null) ImeAction.Search else ImeAction.Done),
        keyboardActions = KeyboardActions(onSearch = {
            if (onSearchAction != null) {
                onSearchAction()
                keyboardController?.hide()
            } else {
                keyboardController?.hide()
            }
        }, onDone = { keyboardController?.hide() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        ),
        shape = MaterialTheme.shapes.medium
    )
}

@Composable
private fun FilterChipItem(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            if (selected) Icon(
                Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun CustomerListContent(
    customers: List<CustomerBO>,
    actions: CustomerAction
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(customers, key = { it.name }) { customer ->
            CustomerItem(customer) { actions.toDetails(customer.name) }
        }
    }
}

@Composable
fun CustomerItem(customer: CustomerBO, onClick: () -> Unit) {
    val isOverLimit = (customer.availableCredit ?: 0.0) < 0 || (customer.currentBalance ?: 0.0) > 0

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOverLimit) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar circular
            Card(
                modifier = Modifier.size(48.dp).clip(CircleShape),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOverLimit) MaterialTheme.colorScheme.errorContainer.copy(
                        alpha = 0.2f
                    ) else MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = customer.customerName,
                    modifier = Modifier.size(48.dp).padding(12.dp),
                    tint = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Contenido expandible
            Column(
                modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    customer.customerName,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isOverLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    customer.mobileNo ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOverLimit) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    customer.territory ?: "N/A",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Columna derecha para balance/pendientes
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${customer.currency.toCurrencySymbol()}${customer.currentBalance}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isOverLimit) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
                Text(
                    "${customer.pendingInvoices} pendientes",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isOverLimit) MaterialTheme.colorScheme.error.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CustomerShimmerList() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(6) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .shimmerBackground(RoundedCornerShape(16.dp))
            )
        }
    }
}

@Composable
private fun FullScreenErrorMessage(
    errorMessage: String, onRetry: () -> Unit, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Filled.Error,
                "Error",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                errorMessage,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
            Button(
                onClick = onRetry, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(
    message: String, icon: ImageVector, modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                message,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

fun Modifier.shimmerBackground(
    shape: RoundedCornerShape = RoundedCornerShape(4.dp),
    baseAlpha: Float = 0.6f,
    highlightAlpha: Float = 0.2f,
    gradientWidth: Float = 400f,
    durationMillis: Int = 1200
): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = gradientWidth,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            RepeatMode.Restart
        ),
        label = "shimmerTranslateAnim"
    )

    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = baseAlpha),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = highlightAlpha),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = baseAlpha)
    )

    this.background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - gradientWidth, translateAnim - gradientWidth),
            end = Offset(translateAnim, translateAnim)
        ),
        shape = shape
    )
}

@Preview
@Composable
fun CustomerListScreenPreview() {
    MaterialTheme {
        CustomerListScreen(
            state = CustomerState.Success(
                customers = listOf(
                    CustomerBO(
                        name = "1",
                        customerName = "Ricardo García",
                        territory = "Managua",
                        mobileNo = "+505 8888 0505",
                        customerType = "Individual",
                        currentBalance = 13450.0,
                        pendingInvoices = 2,
                        availableCredit = 0.0
                    )
                )
            ), actions = CustomerAction()
        )
    }
}

@Preview
@Composable
fun CustomerListScreenLoadingPreview() {
    MaterialTheme {
        CustomerListScreen(
            state = CustomerState.Loading, actions = CustomerAction()
        )
    }
}

@Preview
@Composable
fun CustomerListScreenErrorPreview() {
    MaterialTheme {
        CustomerListScreen(
            state = CustomerState.Error("Error al cargar clientes"), actions = CustomerAction()
        )
    }
}

@Preview
@Composable
fun CustomerListScreenEmptyPreview() {
    MaterialTheme {
        CustomerListScreen(
            state = CustomerState.Empty, actions = CustomerAction()
        )
    }
}

@Preview
@Composable
fun CustomerItemPreview() {
    MaterialTheme {
        CustomerItem(
            customer = CustomerBO(
                name = "1",
                customerName = "Ricardo García",
                territory = "Managua",
                mobileNo = "+505 8888 0505",
                customerType = "Individual",
                currentBalance = 13450.0,
                pendingInvoices = 2,
                availableCredit = 0.0
            ), onClick = {})
    }
}

@Preview
@Composable
fun CustomerItemOverLimitPreview() {
    MaterialTheme {
        CustomerItem(
            customer = CustomerBO(
                name = "2",
                customerName = "Sofía Ramírez",
                territory = "León",
                mobileNo = "+505 7777 0404",
                customerType = "Company",
                currentBalance = 0.0,
                pendingInvoices = 0,
                availableCredit = -500.0  // Sobre límite para rojo
            ), onClick = {})
    }
}
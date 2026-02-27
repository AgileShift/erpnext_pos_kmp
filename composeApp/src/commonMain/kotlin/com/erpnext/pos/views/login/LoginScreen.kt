@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.views.login

import AppTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.base.getPlatformName
import com.erpnext.pos.utils.WindowHeightSizeClass
import com.erpnext.pos.utils.WindowWidthSizeClass
import com.erpnext.pos.utils.isValidUrlInput
import com.erpnext.pos.utils.rememberWindowSizeClass
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LoginScreen(
    state: LoginState, actions: LoginAction
) {
    var siteUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        actions.existingSites()
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val isDesktopPlatform = getPlatformName() == "Desktop"
        val sizeClass = rememberWindowSizeClass()
        val isCompact = sizeClass.widthSizeClass == WindowWidthSizeClass.Compact ||
            sizeClass.widthSizeClass == WindowWidthSizeClass.Medium
        val isExpandedWidth = sizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
        val isExpandedHeight = sizeClass.heightSizeClass == WindowHeightSizeClass.Expanded
        val useSideLayout = !isCompact
        val useGridForSites = isExpandedWidth && isExpandedHeight
        val compactSites = !useGridForSites
        val horizontalPadding = if (useSideLayout) 56.dp else 24.dp
        val verticalPadding = if (useSideLayout) 48.dp else 24.dp

        val background = Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                MaterialTheme.colorScheme.background
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(background)
        ) {
            val scrollState = rememberScrollState()
            if (useSideLayout) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                        .verticalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    BrandPanel(
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = 16.dp)
                    )
                    LoginCard(
                        state = state,
                        siteUrl = siteUrl,
                        onSiteUrlChanged = { siteUrl = it },
                        actions = actions,
                        compact = compactSites,
                        useGridForSites = useGridForSites,
                        isDesktop = isDesktopPlatform,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = horizontalPadding, vertical = verticalPadding)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BrandPanel(
                        compact = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LoginCard(
                        state = state,
                        siteUrl = siteUrl,
                        onSiteUrlChanged = { siteUrl = it },
                        actions = actions,
                        compact = compactSites,
                        useGridForSites = useGridForSites,
                        isDesktop = isDesktopPlatform,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                //}
            }
        }
    }
}

@Composable
private fun BrandPanel(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "ERPNext POS",
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.4).sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Conecta tu instancia y continúa con tu caja sin fricción.",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        //if (!compact) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureRow(icon = Icons.Default.Security, text = "OAuth seguro con ERPNext")
                FeatureRow(icon = Icons.Default.Speed, text = "Sincronización rápida y confiable")
                FeatureRow(
                    icon = Icons.Default.Language,
                    text = "Soporte multi‑instancia y multi‑sucursal"
                )
            }
        //}
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LoginCard(
    state: LoginState,
    siteUrl: String,
    onSiteUrlChanged: (String) -> Unit,
    actions: LoginAction,
    compact: Boolean,
    useGridForSites: Boolean,
    isDesktop: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compact) 20.dp else 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Inicio de sesión",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Selecciona una instancia o agrega una nueva URL.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = { actions.clear() },
                enabled = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "Limpiar instancias")
            }

            when (state) {
                is LoginState.Loading -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }

                is LoginState.Success -> {
                    val sites = state.sites
                    AnimatedVisibility(visible = !sites.isNullOrEmpty()) {
                        SitePicker(
                            sites = sites.orEmpty(),
                            onSelect = { actions.onSiteSelected(it) },
                            onToggleFavorite = { actions.onToggleFavorite(it) },
                            onDelete = { actions.onDeleteSite(it) },
                            compact = compact,
                            useGrid = useGridForSites,
                            enableSwipeDelete = !isDesktop,
                            showDesktopDelete = isDesktop
                        )
                    }

                    UrlInputField(
                        url = siteUrl,
                        onUrlChanged = onSiteUrlChanged,
                    )

                    Button(
                        onClick = { if (siteUrl.isNotBlank()) actions.onAddSite(siteUrl) },
                        enabled = siteUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(text = "Conectar instancia")
                    }
                }

                is LoginState.Authenticated -> {
//                    LaunchedEffect(state.tokens) {
//                        actions.isAuthenticated(state.tokens)
//                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 3.dp
                        )
                    }
                }

                is LoginState.Error -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { actions.onReset() }) {
                            Text(text = "Reintentar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SitePicker(
    sites: List<Site>,
    onSelect: (Site) -> Unit,
    onToggleFavorite: (Site) -> Unit,
    onDelete: (Site) -> Unit,
    compact: Boolean,
    useGrid: Boolean,
    enableSwipeDelete: Boolean,
    showDesktopDelete: Boolean
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(sites, query) {
        if (query.isBlank()) sites
        else sites.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.url.contains(query, ignoreCase = true)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Instancias guardadas (${sites.size})",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Search is intentionally hidden for now (kept for future toggle).
        if (!useGrid) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            text = "No hay coincidencias",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                } else {
                    itemsIndexed(filtered) { index, site ->
                        StaggeredIn(index = index) {
                            if (enableSwipeDelete) {
                                SwipeToDeleteSiteRow(
                                    site = site,
                                    onClick = { onSelect(site) },
                                    onToggleFavorite = { onToggleFavorite(site) },
                                    onDelete = { onDelete(site) }
                                )
                            } else {
                                SiteRow(
                                    site = site,
                                    onClick = { onSelect(site) },
                                    onToggleFavorite = { onToggleFavorite(site) },
                                    onDelete = if (showDesktopDelete) ({ onDelete(site) }) else null,
                                    showDeleteAction = showDesktopDelete
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                columns = GridCells.Fixed(2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            text = "No hay coincidencias",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                } else {
                    itemsIndexed(filtered) { index, site ->
                        StaggeredIn(index = index) {
                            SiteRow(
                                site = site,
                                onClick = { onSelect(site) },
                                onToggleFavorite = { onToggleFavorite(site) },
                                onDelete = if (showDesktopDelete) ({ onDelete(site) }) else null,
                                showDeleteAction = showDesktopDelete
                            )
                        }
                    }
                }
            }
        }
        if (sites.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = "o",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SwipeToDeleteSiteRow(
    site: Site,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { distance -> distance * 0.35f }
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.errorContainer
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Eliminar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onError
                    )
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError
                    )
                }
            }
        }
    ) {
        SiteRow(
            site = site,
            onClick = onClick,
            onToggleFavorite = onToggleFavorite
        )
    }
}

@Composable
fun SiteRow(
    site: Site,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: (() -> Unit)? = null,
    showDeleteAction: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val scale = if (isPressed) 0.98f else if (isHovered) 1.01f else 1f
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isHovered) 8.dp else 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = site.name.take(2).uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            site.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            site.lastUsedAt?.let {
                                InfoBadge(text = formatLastSession(it))
                            }
                            if (site.isFavorite) {
                                IconBadge(onClick = onToggleFavorite)
                            }
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (showDeleteAction && onDelete != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.clickable { onDelete() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Eliminar",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                    if (!site.isFavorite) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier
                                .clickable { onToggleFavorite() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.StarBorder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Fav",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StaggeredIn(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index * 50L).coerceAtMost(300L))
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 6 },
        exit = fadeOut(tween(120))
    ) {
        content()
    }
}

private fun formatLastSession(epochMillis: Long): String {
    val ldt = Instant.fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val day = ldt.date.day.toString().padStart(2, '0')
    val month = ldt.date.month.number.toString().padStart(2, '0')
    val hour = ldt.hour.toString().padStart(2, '0')
    val minute = ldt.minute.toString().padStart(2, '0')
    return "Última sesión: $day/$month ${hour}:$minute"
}

@Composable
private fun InfoBadge(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun IconBadge(onClick: () -> Unit) {
    Surface(
        color = Color(0xFFFFF1C2),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clickable { onClick() }
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Color(0xFFF4C430),
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = "Favorito",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8A6D00)
            )
        }
    }
}

@Composable
fun UrlInputField(
    url: String,
    onUrlChanged: (String) -> Unit,
) {
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    OutlinedTextField(
        value = url,
        onValueChange = { input ->
            onUrlChanged(input.trim())
            val isValid = isValidUrlInput(input)
            isError = !isValid
            errorMessage = if (isError) "URL inválida, usa http://ejemplo.com o https://ejemplo.com" else ""
        },
        label = { Text("URL del Sitio") },
        placeholder = { Text("https://erp.frappe.cloud") },
        isError = isError,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.3.sp
        ),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        supportingText = {
            Text(
                text = if (isError) errorMessage else "Ingresa la URL completa de tu instancia",
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    )
}

@Composable
@Preview
fun LoginPreview() {
    AppTheme {
        LoginScreen(
            state = LoginState.Success(listOf(Site("http://localhost:8000", "La Casita del Queso", null, true), Site("httsp://staging.clothingcenterni.com", "Clothing Center", null, false), Site("httsp://staging.gamezonenic.com", "Game Zone", null, false))),
            actions = LoginAction()
        )
    }
}

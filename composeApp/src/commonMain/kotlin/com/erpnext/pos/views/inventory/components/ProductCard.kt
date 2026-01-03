package com.erpnext.pos.views.inventory.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.views.inventory.InventoryAction
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ProductCard(
    actions: InventoryAction,
    product: ItemBO,
    isDesktop: Boolean = false
) {
    val formattedPrice =
        remember(product.price) { com.erpnext.pos.utils.formatDoubleToString(product.price, 2) }
    val formattedQty = remember(product.actualQty) {
        com.erpnext.pos.utils.formatDoubleToString(product.actualQty, 0)
    }
    val context = LocalPlatformContext.current
    val strings = LocalAppStrings.current
    val imageWidth = if (isDesktop) 200.dp else 150.dp
    val cardHeight = if (isDesktop) 190.dp else 170.dp
    val statusLabel =
        if (product.actualQty <= 0) strings.customer.outOfStock else strings.customer.inStock
    val hasStock = product.actualQty > 0
    val priceLabel =
        "${product.currency?.toCurrencySymbol().orEmpty()} $formattedPrice"
    val colorLabel = product.brand?.takeIf { it.isNotBlank() } ?: "--"
    val sizeLabel = "--"

    val cardShape = RoundedCornerShape(16.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(cardHeight)
            .clip(cardShape),
        shape = cardShape,
        onClick = { actions.onItemClick(product) } // asegúrate de pasar product
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (isDesktop) 18.dp else 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                tonalElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .width(imageWidth)
                        .fillMaxHeight()
                        .padding(6.dp)
                ) {
                    SubcomposeAsyncImage(
                        model = remember(product.image) {
                            ImageRequest.Builder(context)
                                .data(product.image?.ifBlank { "https://placehold.co/600x400" })
                                .crossfade(true)
                                .build()
                        },
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        },
                        error = {
                            AsyncImage(
                                model = "https://placehold.co/600x400",
                                contentScale = ContentScale.Crop,
                                contentDescription = "placeholder",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        tonalElevation = 0.dp
                    ) {
                        Text(
                            text = priceLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoBadge(
                        label = "${strings.inventory.productAvailableLabel} $formattedQty",
                        isMuted = false
                    )
                    InfoBadge(
                        label = statusLabel,
                        isMuted = hasStock
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoBadge(label = "${strings.inventory.productUomLabel} ${product.uom}", isMuted = true)
                    InfoBadge(label = "${strings.inventory.productCodeLabel} ${product.itemCode}", isMuted = true)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    InfoBadge(label = "Color $colorLabel", isMuted = true)
                    InfoBadge(label = "Talla $sizeLabel", isMuted = true)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProductMetadataRow(
    category: String,
    uom: String,
    itemCode: String
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MetadataPill(
            label = "Category: ${
                category.lowercase().replaceFirstChar { it.titlecase() }
            }"
        )
        MetadataPill(label = "UOM: ${uom.lowercase().replaceFirstChar { it.titlecase() }}")
        MetadataPill(label = "Code: $itemCode")
    }
}

@Composable
fun MetadataPill(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun InfoBadge(label: String, isMuted: Boolean) {
    val containerColor = if (isMuted) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = if (isMuted) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        tonalElevation = 0.dp
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
@Preview
fun ProductCardPreview() {
    ProductCard(
        actions = InventoryAction(),
        product = ItemBO(
            name = "Producto de prueba",
            price = 10.0,
            actualQty = 5.0,
            itemGroup = "Grupo de prueba",
            uom = "UOM de prueba",
            itemCode = "123456",
            image = "https://placehold.co/600x400",
            description = "Descripción del producto de prueba"
        ),
        isDesktop = false
    )
}

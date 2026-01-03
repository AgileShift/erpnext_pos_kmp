package com.erpnext.pos.views.inventory.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.utils.toCurrencySymbol
import com.erpnext.pos.views.inventory.InventoryAction
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ProductCard(
    actions: InventoryAction, product: ItemBO, isDesktop: Boolean = false
) {
    val formattedPrice =
        remember(product.price) { com.erpnext.pos.utils.formatDoubleToString(product.price, 2) }
    val formattedQty = remember(product.actualQty) {
        com.erpnext.pos.utils.formatDoubleToString(product.actualQty, 0)
    }
    val context = LocalPlatformContext.current
    val imageSize = if (isDesktop) 96.dp else 72.dp
    val statusLabel = if (product.actualQty <= 0) "Out of stock" else "In stock"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = { actions.onItemClick(product) } // asegúrate de pasar product
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(if (isDesktop) 16.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Imagen con fallback
                SubcomposeAsyncImage(
                    model = remember(product.image) {
                        ImageRequest.Builder(context)
                            .data(product.image?.ifBlank { "https://placehold.co/600x400" }) // fallback
                            .scale(Scale.FIT).crossfade(true).build()
                    },
                    contentDescription = product.name,
                    modifier = Modifier.size(imageSize),
                    loading = {
                        // mientras carga
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    },
                    error = {
                        // si falla
                        AsyncImage(
                            model = "https://placehold.co/600x400",
                            contentDescription = "placeholder",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.size(imageSize)
                        )
                    })

                Column(
                    modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${product.currency?.toCurrencySymbol()} $formattedPrice",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoBadge(
                            label = "Stock $formattedQty", isMuted = false
                        )
                        InfoBadge(
                            label = statusLabel, isMuted = product.actualQty > 0
                        )
                    }
                }
            }

            ProductMetadataRow(
                category = product.itemGroup, uom = product.uom, itemCode = product.itemCode
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductMetadataRow(
    category: String, uom: String, itemCode: String
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MetadataPill(
            label = "Category: ${
                category.lowercase().replaceFirstChar { it.titlecase() }
            }")
        MetadataPill(label = "UOM: ${uom.lowercase().replaceFirstChar { it.titlecase() }}")
        MetadataPill(label = "Code: $itemCode")
    }
}

@Composable
private fun MetadataPill(label: String) {
    Surface(
        shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant
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
        shape = RoundedCornerShape(10.dp), color = containerColor
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
        actions = InventoryAction(), product = ItemBO(
            name = "Producto de prueba",
            price = 10.0,
            actualQty = 5.0,
            itemGroup = "Grupo de prueba",
            uom = "UOM de prueba",
            itemCode = "123456",
            image = "https://placehold.co/600x400",
            description = "Descripción del producto de prueba"
        ), isDesktop = false
    )
}

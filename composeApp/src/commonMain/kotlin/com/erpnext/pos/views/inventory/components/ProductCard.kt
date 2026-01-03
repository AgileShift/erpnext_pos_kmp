package com.erpnext.pos.views.inventory.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun ProductCard(actions: InventoryAction, product: ItemBO) {
    val formattedPrice =
        remember(product.price) { com.erpnext.pos.utils.formatDoubleToString(product.price, 2) }
    val formattedQty = remember(product.actualQty) {
        com.erpnext.pos.utils.formatDoubleToString(product.actualQty, 0)
    }
    val context = LocalPlatformContext.current
    val strings = LocalAppStrings.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        onClick = { actions.onItemClick(product) } // asegúrate de pasar product
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Imagen con fallback
            SubcomposeAsyncImage(
                model = remember(product.image) {
                    ImageRequest.Builder(context)
                        .data(product.image?.ifBlank { "https://placehold.co/600x400" }) // fallback
                        .crossfade(true)
                        .build()
                },
                contentDescription = product.name,
                modifier = Modifier.size(90.dp),
                loading = {
                    // mientras carga
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                },
                error = {
                    // si falla
                    AsyncImage(
                        model = "https://placehold.co/600x400",
                        contentDescription = strings.common.imagePlaceholderDescription,
                        modifier = Modifier.size(72.dp)
                    )
                }
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Text(
                    "${strings.inventory.productAvailableLabel}: $formattedQty",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "${strings.inventory.productCategoryLabel}: ${
                        product.itemGroup.lowercase().replaceFirstChar { it.titlecase() }
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "${strings.inventory.productUomLabel}: ${
                        product.uom.lowercase().replaceFirstChar { it.titlecase() }
                    }",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "${strings.inventory.productCodeLabel}: ${product.itemCode}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = 90.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${product.currency?.toCurrencySymbol()} $formattedPrice",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                if (product.actualQty <= 0) {
                    Text(strings.inventory.productOutOfStock, color = MaterialTheme.colorScheme.error)
                }
            }
        }
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
        )
    )
}

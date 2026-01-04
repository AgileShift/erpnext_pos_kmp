package com.erpnext.pos.views.inventory.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.erpnext.pos.domain.models.ItemBO
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.moneyScale
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
        remember(product.price) { bd(product.price).moneyScale(2) } //com.erpnext.pos.utils.formatDoubleToString(product.price, 2) }
    val formattedQty = remember(product.actualQty) {
        com.erpnext.pos.utils.formatDoubleToString(product.actualQty, 0)
    }
    val context = LocalPlatformContext.current
    val strings = LocalAppStrings.current
    val imageWidth = if (isDesktop) 170.dp else 120.dp
    val cardHeight = if (isDesktop) 200.dp else 180.dp
    val statusLabel =
        if (product.actualQty <= 0) strings.customer.outOfStock else strings.customer.inStock
    val hasStock = product.actualQty > 0
    val priceLabel =
        "${product.currency?.toCurrencySymbol().orEmpty()} $formattedPrice"
    val colorLabel = product.brand?.takeIf { it.isNotBlank() } ?: "--"
    val sizeLabel = "--"

    val cardShape = RoundedCornerShape(18.dp)
    Card(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxWidth()
            .height(cardHeight)
            .clip(cardShape),
        shape = cardShape,
        onClick = { actions.onItemClick(product) } // asegúrate de pasar product
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(imageWidth)
                    .fillMaxHeight()
                    .clip(cardShape)
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
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = if (isDesktop) 16.dp else 12.dp, top = 14.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    product.name,
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = priceLabel,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        InfoBadge(
                            label = "${strings.inventory.productAvailableLabel}: $formattedQty ${product.uom}",
                            isMuted = false,
                            bgColor = if (product.actualQty > 0) Color.Green.copy(alpha = .15f) else Color.Red.copy(
                                alpha = .15f
                            )
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        /*InfoBadge(
                            label = "${strings.inventory.productCodeLabel}: ${product.itemCode}",
                            isMuted = false
                        )*/
                        InfoBadge(label = "Color: $colorLabel", isMuted = false)
                        InfoBadge(label = "Talla: $sizeLabel", isMuted = false)
                    }
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
private fun InfoBadge(
    label: String,
    isMuted: Boolean,
    bgColor: Color? = null,
    textColor: Color? = null
) {
    val containerColor = if (isMuted) {
        bgColor ?: MaterialTheme.colorScheme.surfaceVariant
    } else {
        bgColor ?: MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = if (isMuted) {
        textColor ?: MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        textColor ?: MaterialTheme.colorScheme.onSecondaryContainer
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
            name = "Set de Fajas 272 - NEGRO y CAMEL",
            price = 217.0,
            actualQty = 0.0,
            currency = "NIO",
            itemGroup = "Grupo de prueba",
            uom = "Unidad (es)",
            itemCode = "123456",
            image = "https://placehold.co/600x400",
            description = "Descripción del producto de prueba"
        ),
        isDesktop = false
    )
}

package com.erpnext.pos.views.customer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import erpnextpos.composeapp.generated.resources.Res
import erpnextpos.composeapp.generated.resources.compose_multiplatform

data class Invoice(
    val number: String,
    val date: String,
    val total: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerDetailScreen(
    customerId: String,
    onBackClick: () -> Unit
) {
    val invoices = listOf(
        Invoice("FACT #2024-001", "May 12, 2024", "C\$11,500"),
        Invoice("FACT #2024-002", "April 20, 2024", "C\$22,540")
    )
    Scaffold(
        bottomBar = {
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("Nueva Factura", fontSize = 16.sp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Foto y datos del cliente
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(Res.drawable.compose_multiplatform), // Imagen de perfil
                    contentDescription = "Foto de cliente",
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Ricardo García", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("ricardo.garcia@email.com", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Información de contacto
            SectionTitle("Información de Contacto")
            InfoRow(icon = Icons.Default.Phone, text = "+505 8888 0505")
            InfoRow(icon = Icons.Default.LocationOn, text = "Google Maps | WAZE")

            // Forma de pago
            SectionTitle("Forma de Pago")
            InfoRow(icon = Icons.Default.CreditCard, text = "Crédito")

            // Balance adeudado
            SectionTitle("Balance Adeudado")
            InfoRow(icon = Icons.Default.AttachMoney, text = "C\$13,450")

            // Facturas
            SectionTitle("Facturas")
            invoices.forEach { invoice ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(invoice.number, fontWeight = FontWeight.Medium)
                        Text(invoice.date, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(invoice.total, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
}

@Composable
fun InfoRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}

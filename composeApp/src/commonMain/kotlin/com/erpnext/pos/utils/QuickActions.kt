package com.erpnext.pos.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import com.erpnext.pos.domain.models.CustomerQuickAction
import com.erpnext.pos.domain.models.CustomerQuickActionType

object QuickActions {
    fun customerQuickActions(): List<CustomerQuickAction> = listOf(
        CustomerQuickAction(
            type = CustomerQuickActionType.PendingInvoices,
            label = "Ver facturas pendientes",
            icon = Icons.Filled.ReceiptLong
        ),
        CustomerQuickAction(
            type = CustomerQuickActionType.CreateQuotation,
            label = "Crear cotización",
            icon = Icons.Filled.Description
        ),
        CustomerQuickAction(
            type = CustomerQuickActionType.CreateSalesOrder,
            label = "Crear orden de venta",
            icon = Icons.Filled.PointOfSale
        ),
        CustomerQuickAction(
            type = CustomerQuickActionType.CreateDeliveryNote,
            label = "Crear nota de entrega",
            icon = Icons.Filled.LocalShipping
        ),
        CustomerQuickAction(
            type = CustomerQuickActionType.CreateInvoice,
            label = "Crear factura",
            icon = Icons.Filled.Receipt
        ),
        CustomerQuickAction(
            type = CustomerQuickActionType.RegisterPayment,
            label = "Registrar pago",
            icon = Icons.Filled.Payments
        )
    )
}
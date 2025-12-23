package com.erpnext.pos.remoteSource.dto.v2

import com.erpnext.pos.localSource.entities.v2.POSPaymentMethodEntity

data class POSConfigurationSnapshot(
    val currency: String,
    val priceList: String,
    val warehouseId: String,
    val paymentMethods: List<POSPaymentMethodEntity>,
    val taxTemplateId: String?
)
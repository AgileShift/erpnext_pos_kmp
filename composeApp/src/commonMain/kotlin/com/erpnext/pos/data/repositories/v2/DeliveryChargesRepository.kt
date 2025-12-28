package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.remoteSource.dto.v2.DeliveryChargeDto
import com.erpnext.pos.remoteSource.sdk.v2.ERPDocType
import com.erpnext.pos.remoteSource.sdk.v2.getFields

class DeliveryChargesRepository(
    private val api: APIServiceV2
) {
    suspend fun fetchDeliveryCharges(): List<DeliveryChargeDto> {
        return api.list(
            doctype = ERPDocType.DeliveryCharges,
            fields = ERPDocType.DeliveryCharges.getFields()
        )
    }
}

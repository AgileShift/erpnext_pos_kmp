package com.erpnext.pos.remoteSource.mapper

import com.erpnext.pos.localSource.entities.DeliveryChargeEntity
import com.erpnext.pos.localSource.entities.PaymentTermEntity
import com.erpnext.pos.remoteSource.dto.ShippingRuleDto
import com.erpnext.pos.remoteSource.dto.PaymentTermDto
import kotlin.time.Clock

fun PaymentTermDto.toEntity(): PaymentTermEntity {
    return PaymentTermEntity(
        name = this.paymentTermName,
        invoicePortion = this.invoicePortion,
        modeOfPayment = this.modeOfPayment,
        dueDateBasedOn = this.dueDateBasedOn,
        creditDays = this.creditDays,
        creditMonths = this.creditMonths,
        discountType = this.discountType,
        discount = this.discount,
        description = this.description,
        discountValidity = this.discountValidity,
        discountValidityBasedOn = this.discountValidityBasedOn,
        lastSyncedAt = Clock.System.now().toEpochMilliseconds()
    )
}

fun ShippingRuleDto.toEntity(): DeliveryChargeEntity {
    return DeliveryChargeEntity(
        label = this.label,
        defaultRate = this.defaultRate,
        lastSyncedAt = Clock.System.now().toEpochMilliseconds()
    )
}

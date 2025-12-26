package com.erpnext.pos.remoteSource.mapper.v2

import com.erpnext.pos.localSource.entities.v2.PaymentScheduleEntity
import com.erpnext.pos.remoteSource.dto.v2.PaymentScheduleRowDto

fun PaymentScheduleRowDto.toEntity(
    referenceType: String,
    referenceId: String,
    instanceId: String,
    companyId: String
) = PaymentScheduleEntity(
    referenceType = referenceType,
    referenceId = referenceId,
    dueDate = dueDate,
    paymentAmount = paymentAmount,
    outstandingAmount = outstandingAmount
).apply {
    this.instanceId = instanceId
    this.companyId = companyId
}

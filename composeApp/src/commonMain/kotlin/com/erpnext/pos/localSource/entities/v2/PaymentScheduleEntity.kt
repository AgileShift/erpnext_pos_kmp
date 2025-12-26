package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "payment_schedules",
    indices = [
        Index(value = ["instanceId", "companyId", "referenceType", "referenceId"])
    ]
)
data class PaymentScheduleEntity(
    var referenceType: String,
    var referenceId: String,
    var dueDate: String?,
    var paymentAmount: Double?,
    var outstandingAmount: Double?
) : BaseEntity()

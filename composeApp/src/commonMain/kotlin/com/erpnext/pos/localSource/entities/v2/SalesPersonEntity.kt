package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(
    tableName = "sales_people",
    primaryKeys = ["instanceId", "companyId", "salesPersonId"]
)
data class SalesPersonEntity(
    var salesPersonId: String,
    var salesPersonName: String,
    var employeeId: String,
    var isGroup: Boolean = false,
    var parentSalesPersonId: String? = null
) : BaseEntity()
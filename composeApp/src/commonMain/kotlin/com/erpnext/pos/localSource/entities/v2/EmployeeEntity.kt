package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(
    tableName = "employees",
    primaryKeys = ["instanceId", "companyId", "employeeId"]
)
data class EmployeeEntity(
    var employeeId: String,
    var employeeName: String,
    var userId: String,
    var status: Boolean = true,
    var company: String
) : BaseEntity()
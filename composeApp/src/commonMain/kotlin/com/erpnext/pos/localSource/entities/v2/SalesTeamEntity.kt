package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(tableName = "sales_team")
data class SalesTeamEntity(
    var salesPerson: String,
    var allocatedPercentage: Float
) : BaseEntity()

package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(
    tableName = "companies",
    primaryKeys = ["instanceId", "companyId"]
)
data class CompanyEntity(
    val companyName: String,
    var abbr: String,
    var defaultCurrency: String,
    var country: String,
    var domain: String? = null,
    var taxId: String? = null,
    var isGroup: Boolean = false,
    var parentCompanyId: String? = null,
    var companyLogo: String? = null,
    var letterHead: String?,
) : BaseEntity()
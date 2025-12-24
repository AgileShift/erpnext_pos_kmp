package com.erpnext.pos.localSource.entities.v2

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import com.erpnext.pos.localSource.dao.SyncStatus

@Entity(
    tableName = "pricing_rules",
    indices = [
        Index(value = ["instanceId", "companyId", "pricingRuleId"])
    ]
)
data class PricingRuleEntity(
    var pricingRuleId: String,
    var priority: Int,
    var condition: String?,
    var territory: String?,
    var forPriceList: String,
    var otherItemCode: String?,
    var otherItemGroup: String?,
    var validFrom: String?,
    var validUntil: String?,
    var syncStatus: SyncStatus? = null,
    @ColumnInfo(name = "remote_modified")
    var remoteModified: String? = null,
    @ColumnInfo("remote_name")
    var remoteName: String? = null
) : BaseEntity()

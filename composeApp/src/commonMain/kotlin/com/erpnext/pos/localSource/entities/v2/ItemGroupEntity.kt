package com.erpnext.pos.localSource.entities.v2

import androidx.room.Entity

@Entity(tableName = "item_groups",
    primaryKeys = ["instanceId", "companyId", "itemGroupId"])
data class ItemGroupEntity(
    var itemGroupId: String,
    var itemGroupName: String,
    var parentItemGroupId: String? = null,
    var isGroup: Boolean = false
) : BaseEntity()
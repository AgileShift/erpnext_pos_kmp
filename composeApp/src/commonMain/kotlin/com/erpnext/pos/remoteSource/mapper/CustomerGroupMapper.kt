package com.erpnext.pos.remoteSource.mapper

import com.erpnext.pos.localSource.entities.CustomerGroupEntity
import com.erpnext.pos.localSource.entities.TerritoryEntity
import com.erpnext.pos.remoteSource.dto.CustomerGroupDto
import com.erpnext.pos.remoteSource.dto.TerritoryDto
import kotlin.time.Clock

fun CustomerGroupDto.toEntity(): CustomerGroupEntity {
    return CustomerGroupEntity(
        name = this.name,
        customerGroupName = this.customerGroupName,
        isGroup = this.isGroup,
        parentCustomerGroup = this.parentCustomerGroup,
        lastSyncedAt = Clock.System.now().toEpochMilliseconds()
    )
}

fun TerritoryDto.toEntity(): TerritoryEntity {
    return TerritoryEntity(
        name = this.name,
        territoryName = this.territoryName,
        isGroup = this.isGroup,
        parentTerritory = this.parentTerritory,
        lastSyncedAt = Clock.System.now().toEpochMilliseconds()
    )
}

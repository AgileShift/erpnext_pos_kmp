package com.erpnext.pos.remoteSource.mapper

import com.erpnext.pos.localSource.entities.CompanyAccountEntity
import com.erpnext.pos.localSource.entities.SupplierEntity
import com.erpnext.pos.remoteSource.dto.CompanyAccountDto
import com.erpnext.pos.remoteSource.dto.SupplierDto
import kotlin.time.Clock

fun SupplierDto.toEntity(): SupplierEntity {
    return SupplierEntity(
        name = name,
        supplierName = supplierName,
        supplierGroup = supplierGroup,
        supplierType = supplierType,
        defaultCurrency = defaultCurrency,
        disabled = disabled,
        lastSyncedAt = Clock.System.now().toEpochMilliseconds()
    )
}

fun CompanyAccountDto.toEntity(): CompanyAccountEntity {
    return CompanyAccountEntity(
        name = name,
        accountName = accountName,
        accountType = accountType,
        accountCurrency = accountCurrency,
        company = company,
        isGroup = isGroup,
        disabled = disabled,
        lastSyncedAt = Clock.System.now().toEpochMilliseconds()
    )
}

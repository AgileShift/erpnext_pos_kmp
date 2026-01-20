package com.erpnext.pos.data.mappers

import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
import com.erpnext.pos.remoteSource.dto.BalanceDetailsDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto

fun buildOpeningEntryDto(
    openingEntry: POSOpeningEntryEntity,
    balanceDetails: List<BalanceDetailsEntity>
): POSOpeningEntryDto {
    return POSOpeningEntryDto(
        posProfile = openingEntry.posProfile,
        company = openingEntry.company,
        user = openingEntry.user,
        periodStartDate = openingEntry.periodStartDate,
        postingDate = openingEntry.postingDate,
        balanceDetails = balanceDetails.map { detail ->
            BalanceDetailsDto(
                modeOfPayment = detail.modeOfPayment,
                openingAmount = detail.openingAmount,
                closingAmount = detail.closingAmount
            )
        },
        taxes = null,
        docStatus = 0
    )
}

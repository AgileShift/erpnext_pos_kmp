package com.erpnext.pos.data.mappers

import com.erpnext.pos.localSource.entities.BalanceDetailsEntity
import com.erpnext.pos.localSource.entities.POSOpeningEntryEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OpeningEntryMapperTest {

    @Test
    fun buildOpeningEntryDtoMapsFields() {
        val openingEntry = POSOpeningEntryEntity(
            name = "LOCAL-OPEN-TEST",
            posProfile = "POS-01",
            company = "ERPNext",
            periodStartDate = "2024-01-01 10:00:00",
            postingDate = "2024-01-01 10:00:00",
            user = "cashier@example.com",
            pendingSync = true
        )
        val balanceDetails = listOf(
            BalanceDetailsEntity(
                cashboxId = 1,
                posOpeningEntry = openingEntry.name,
                modeOfPayment = "Cash",
                openingAmount = 120.0,
                closingAmount = null
            )
        )

        val dto = buildOpeningEntryDto(openingEntry, balanceDetails)

        assertEquals(openingEntry.posProfile, dto.posProfile)
        assertEquals(openingEntry.company, dto.company)
        assertEquals(openingEntry.user, dto.user)
        assertEquals(openingEntry.periodStartDate, dto.periodStartDate)
        assertEquals(openingEntry.postingDate, dto.postingDate)
        assertEquals(1, dto.balanceDetails.size)
        assertEquals("Cash", dto.balanceDetails.first().modeOfPayment)
        assertEquals(120.0, dto.balanceDetails.first().openingAmount)
        assertNull(dto.balanceDetails.first().closingAmount)
    }
}

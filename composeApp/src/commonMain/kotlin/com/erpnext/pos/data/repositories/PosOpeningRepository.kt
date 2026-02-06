package com.erpnext.pos.data.repositories

import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntryResponseDto
import com.erpnext.pos.remoteSource.dto.POSOpeningEntrySummaryDto
import com.erpnext.pos.remoteSource.dto.SubmitResponseDto
import com.erpnext.pos.remoteSource.sdk.FrappeException
import com.erpnext.pos.utils.AppLogger

class PosOpeningRepository(
    private val apiService: APIService
) {
    suspend fun getOpenSession(user: String, posProfile: String): POSOpeningEntrySummaryDto? {
        return try {
            apiService.getOpenPOSOpeningEntries(user, posProfile).firstOrNull()
        } catch (e: FrappeException) {
            AppLogger.warn("getOpenSession failed", e)
            throw e
        } catch (e: Exception) {
            AppLogger.warn("getOpenSession failed", e)
            throw e
        }
    }

    suspend fun getOpenSessionsForProfile(posProfile: String): List<POSOpeningEntrySummaryDto> {
        return try {
            apiService.getOpenPOSOpeningEntriesForProfile(posProfile)
        } catch (e: FrappeException) {
            AppLogger.warn("getOpenSessionsForProfile failed", e)
            throw e
        } catch (e: Exception) {
            AppLogger.warn("getOpenSessionsForProfile failed", e)
            throw e
        }
    }

    suspend fun createOpeningEntry(payload: POSOpeningEntryDto): POSOpeningEntryResponseDto {
        return apiService.openCashbox(payload)
    }

    suspend fun submitOpeningEntry(name: String): SubmitResponseDto {
        return apiService.submitPOSOpeningEntry(name)
    }
}

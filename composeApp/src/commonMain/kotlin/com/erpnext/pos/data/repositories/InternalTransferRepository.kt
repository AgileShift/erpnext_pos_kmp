package com.erpnext.pos.data.repositories

import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.InternalTransferCreateDto
import com.erpnext.pos.remoteSource.dto.InternalTransferSubmitDto
import com.erpnext.pos.utils.RepoTrace

class InternalTransferRepository(
    private val api: APIService
) {
    suspend fun createInternalTransfer(
        clientRequestId: String,
        payload: InternalTransferCreateDto
    ): InternalTransferSubmitDto {
        RepoTrace.breadcrumb("InternalTransferRepository", "createInternalTransfer")
        return runCatching {
            api.createInternalTransfer(clientRequestId, payload)
        }.getOrElse {
            RepoTrace.capture("InternalTransferRepository", "createInternalTransfer", it)
            throw it
        }
    }
}

package com.erpnext.pos.remoteSource.datasources

import com.erpnext.pos.localSource.dao.POSProfileDao
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.POSProfileDto
import com.erpnext.pos.remoteSource.mapper.toEntity

class POSProfileRemoteSource(
    private val api: APIService,
    private val posProfileDao: POSProfileDao
) {
}

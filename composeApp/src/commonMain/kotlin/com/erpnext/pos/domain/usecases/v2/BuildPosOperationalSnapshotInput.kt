package com.erpnext.pos.domain.usecases.v2

data class BuildPOSOperationalSnapshotInput(
    val instanceId: String,
    val companyId: String,
    val userId: String,
    val posProfileId: String,
    val fromDate: String
)

package com.erpnext.pos.domain.repositories.v2

data class ContextPullInput(
    val instanceId: String,
    val companyId: String,
    val userId: String,
    val posProfileId: String
)

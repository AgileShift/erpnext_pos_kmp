package com.erpnext.pos.domain.models

data class CompanyBO(
    val company: String,
    val defaultCurrency: String,
    val country: String?,
    val ruc: String?,
)
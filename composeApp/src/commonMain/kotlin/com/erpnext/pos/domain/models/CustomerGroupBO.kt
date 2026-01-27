package com.erpnext.pos.domain.models

data class CustomerGroupBO(
    val name: String,
    val displayName: String? = null,
    val isGroup: Boolean = false,
    val parent: String? = null
)

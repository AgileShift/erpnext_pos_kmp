package com.erpnext.pos.domain.models

data class UserBO(
    val name: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String? = null,
    val email: String = "",
    val language: String = "",
    val enabled: Boolean = false,
)
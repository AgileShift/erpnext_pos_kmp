package com.erpnext.pos.domain.models

data class UserBO(
    val name: String = "",
    val username: String = "",
    val firstName: String = "",
    val lastName: String? = null,
    val email: String = "",
    val image: String? = null,
    val language: String = "",
    val timeZone: String = "",
    val fullName: String = "",
    val enabled: Boolean = false,
)

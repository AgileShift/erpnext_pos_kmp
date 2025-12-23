package com.erpnext.pos.domain.repositories

import com.erpnext.pos.domain.models.UserBO

interface IUserRepository {
    suspend fun getUserInfo(): UserBO
}
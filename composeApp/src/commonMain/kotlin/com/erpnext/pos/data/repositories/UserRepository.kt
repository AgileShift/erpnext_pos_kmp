package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.domain.repositories.IUserRepository
import com.erpnext.pos.remoteSource.datasources.UserRemoteSource

class UserRepository(
    private val remoteSource: UserRemoteSource
) : IUserRepository {
    override suspend fun getUserInfo(): UserBO {
        return remoteSource.getUserInfo().toBO()
    }
}
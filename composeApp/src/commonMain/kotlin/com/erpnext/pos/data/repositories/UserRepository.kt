package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.domain.repositories.IUserRepository
import com.erpnext.pos.remoteSource.datasources.UserRemoteSource
import com.erpnext.pos.utils.RepoTrace

class UserRepository(
    private val remoteSource: UserRemoteSource
) : IUserRepository {
    override suspend fun getUserInfo(): UserBO {
        RepoTrace.breadcrumb("UserRepository", "getUserInfo")
        return runCatching { remoteSource.getUserInfo().toBO() }
            .getOrElse {
                RepoTrace.capture("UserRepository", "getUserInfo", it)
                throw it
            }
    }
}

package com.erpnext.pos.remoteSource.datasources

import com.erpnext.pos.localSource.dao.UserDao
import com.erpnext.pos.remoteSource.api.APIService
import com.erpnext.pos.remoteSource.dto.UserDto
import com.erpnext.pos.remoteSource.mapper.toEntity

class UserRemoteSource(
    private val api: APIService,
    private val userDao: UserDao
) {
    suspend fun getUserInfo(): UserDto {
        val user = api.getUserInfo()
        userDao.addUser(user.toEntity())

        return user
    }
}
package com.erpnext.pos.data.repositories

import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.domain.repositories.IUserRepository
import com.erpnext.pos.localSource.dao.UserDao
import com.erpnext.pos.utils.RepoTrace

class UserRepository(
    private val userDao: UserDao
) : IUserRepository {
    override suspend fun getUserInfo(): UserBO {
        RepoTrace.breadcrumb("UserRepository", "getUserInfo")
        val local = userDao.getUserInfo()
        if (local != null) return local.toBO()
        throw IllegalStateException("No hay usuario local cacheado. Ejecuta sincronización para cargar datos.")
    }
}

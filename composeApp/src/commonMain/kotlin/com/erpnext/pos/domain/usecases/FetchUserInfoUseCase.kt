package com.erpnext.pos.domain.usecases

import com.erpnext.pos.domain.models.UserBO
import com.erpnext.pos.domain.repositories.IUserRepository

class FetchUserInfoUseCase(
    private val repo: IUserRepository
) : UseCase<Unit?, UserBO>() {
    override suspend fun useCaseFunction(input: Unit?): UserBO {
        return repo.getUserInfo()
    }
}

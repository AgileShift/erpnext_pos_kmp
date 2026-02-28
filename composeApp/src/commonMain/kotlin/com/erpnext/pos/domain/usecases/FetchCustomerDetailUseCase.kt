package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.CustomerRepository
import com.erpnext.pos.domain.models.CustomerBO

class FetchCustomerDetailUseCase(private val repo: CustomerRepository) :
    UseCase<String, CustomerBO?>() {
  override suspend fun useCaseFunction(input: String): CustomerBO? {
    return repo.getCustomerByName(input)
  }
}

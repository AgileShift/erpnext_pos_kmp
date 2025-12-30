package com.erpnext.pos.data.repositories

interface IModeOfPaymentRepository {

}

class ModeOfPaymentRepository(
    private val remoteSource: Any,
    private val localSource: Any
): IModeOfPaymentRepository {
}
package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity

class ModeOfPaymentLocalSource(
    private val dao: ModeOfPaymentDao
) {
    suspend fun getAllModes(company: String): List<ModeOfPaymentEntity> {
        return dao.getAllModes(company)
    }

    suspend fun insertAllModes(items: List<ModeOfPaymentEntity>) {
        dao.insertAllModes(items)
    }

    suspend fun deleteMissing(company: String, names: List<String>) {
        val ids = names.ifEmpty { listOf("__empty__") }
        dao.hardDeleteDeletedNotIn(company, ids)
        dao.softDeleteNotIn(company, ids)
    }

    suspend fun getLastSyncedAt(company: String): Long? {
        return dao.getLastSyncedAt(company)
    }
}

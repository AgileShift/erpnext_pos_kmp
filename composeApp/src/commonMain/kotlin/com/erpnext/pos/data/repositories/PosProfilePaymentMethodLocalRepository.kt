package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.dao.PosProfilePaymentMethodDao
import com.erpnext.pos.localSource.dao.ResolvedPaymentMethod

class PosProfilePaymentMethodLocalRepository(
    private val dao: PosProfilePaymentMethodDao
) {
    suspend fun getMethodsForProfile(profileId: String): List<ResolvedPaymentMethod> {
        return dao.getResolvedMethodsForProfile(profileId)
    }

    suspend fun getCashMethodsGroupedByCurrency(
        profileId: String
    ): Map<String, List<ResolvedPaymentMethod>> {
        val resolved = dao.getResolvedMethodsForProfile(profileId)
        return resolved
            .filter { method ->
                method.enabled && method.enabledInProfile &&
                    method.type?.equals("Cash", ignoreCase = true) == true
            }
            .filter { method -> !method.currency.isNullOrBlank() }
            .groupBy { method ->
                method.currency ?: ""
            }
    }

    suspend fun hasResolvedMethods(profileId: String): Boolean {
        return dao.countResolvedForProfile(profileId) > 0
    }
}

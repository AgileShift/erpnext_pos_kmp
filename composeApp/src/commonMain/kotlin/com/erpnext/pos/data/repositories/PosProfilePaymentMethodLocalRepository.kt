package com.erpnext.pos.data.repositories

import com.erpnext.pos.localSource.dao.PosProfilePaymentMethodDao
import com.erpnext.pos.localSource.dao.ResolvedPaymentMethod
import com.erpnext.pos.utils.normalizeCurrency

class PosProfilePaymentMethodLocalRepository(
    private val dao: PosProfilePaymentMethodDao
) {
    suspend fun getMethodsForProfile(profileId: String): List<ResolvedPaymentMethod> {
        return dao.getResolvedMethodsForProfile(profileId)
    }

    suspend fun getCashMethodsGroupedByCurrency(
        profileId: String,
        baseCurrency: String? = null
    ): Map<String, List<ResolvedPaymentMethod>> {
        val resolved = dao.getResolvedMethodsForProfile(profileId)
        return resolved
            .asSequence()
            .filter { method ->
                method.enabled && method.enabledInProfile &&
                    method.type?.equals("Cash", ignoreCase = true) == true
            }
            .mapNotNull { method ->
                val currency = method.currency?.takeIf { it.isNotBlank() }
                    ?.let { normalizeCurrency(it) }
                currency?.let { it to method }
            }
            .groupBy({ it.first }, { it.second })
    }

    suspend fun hasResolvedMethods(profileId: String): Boolean {
        return dao.countResolvedForProfile(profileId) > 0
    }
}

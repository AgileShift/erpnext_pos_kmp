package com.erpnext.pos.views.home

import com.erpnext.pos.localSource.dao.ResolvedPaymentMethod

data class CashboxOpeningProfileState(
    val profileId: String? = null,
    val company: String = "",
    val baseCurrency: String = "USD",
    val methods: List<ResolvedPaymentMethod> = emptyList(),
    val cashMethodsByCurrency: Map<String, List<ResolvedPaymentMethod>> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

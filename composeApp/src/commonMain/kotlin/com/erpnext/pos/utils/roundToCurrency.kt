package com.erpnext.pos.utils

import com.erpnext.pos.utils.oauth.bd
import com.erpnext.pos.utils.oauth.moneyScale
import com.erpnext.pos.utils.oauth.toDouble

fun roundToCurrency(value: Double, scale: Int = 2): Double {
    if (!value.isFinite()) return value
    return bd(value).moneyScale(scale).toDouble(scale)
}
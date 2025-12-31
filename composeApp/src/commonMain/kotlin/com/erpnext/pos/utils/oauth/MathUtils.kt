package com.erpnext.pos.utils.oauth

//TODO: Mover esta cagada, no se en que momento se metio en oauth
expect class Decimal
expect fun bd(value: String): Decimal
expect fun bd(value: Double): Decimal
expect fun db(value: Float): Decimal
expect fun Decimal.coerceAtLeastZero(): Decimal
expect fun Decimal.isZero(): Boolean
expect fun Decimal.moneyScale(scale: Int = 2): Decimal
expect fun Decimal.safeDiv(divisor: Decimal, scale: Int = 8): Decimal
expect fun Decimal.safeMul(other: Decimal): Decimal
expect fun min(a: Decimal, b: Decimal): Decimal
expect fun minOfBd(a: Decimal, b: Decimal): Decimal
expect fun Decimal.toDouble(scale: Int): Double
expect operator fun Decimal.compareTo(other: Decimal): Int
expect fun roundCashIfNeeded(amount: Decimal, spec: CurrencySpec): Decimal
data class CurrencySpec(
    val code: String,
    val minorUnits: Int, // 0, 2, 3
    val cashScale: Int
)
package com.erpnext.pos.utils.oauth

import java.math.BigDecimal
import java.math.RoundingMode

actual typealias Decimal = BigDecimal

actual fun bd(value: String): Decimal = BigDecimal(value)
actual fun bd(value: Double): Decimal = BigDecimal(value)
actual fun db(value: Float): Decimal = BigDecimal(value.toDouble())

actual fun Decimal.coerceAtLeastZero(): Decimal =
    if (this < BigDecimal.ZERO) BigDecimal.ZERO else this

actual fun Decimal.moneyScale(scale: Int): Decimal =
    this.setScale(scale, RoundingMode.HALF_UP)

actual fun Decimal.safeDiv(divisor: Decimal, scale: Int): Decimal =
    if (divisor.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
    else this.divide(divisor, scale, RoundingMode.HALF_UP)

actual fun Decimal.safeMul(other: Decimal): Decimal = this.multiply(other)

actual fun min(a: Decimal, b: Decimal): Decimal = if (a <= b) a else b

actual fun Decimal.isZero(): Boolean =
    this.compareTo(Decimal.ZERO) == 0

actual fun minOfBd(a: BigDecimal, b: BigDecimal): BigDecimal =
    if (a <= b) a else b

actual fun Decimal.toDouble(scale: Int): Double =
    this.setScale(scale, RoundingMode.HALF_UP).toDouble()

actual operator fun Decimal.compareTo(other: Decimal): Int = this.compareTo(other)

actual fun roundCashIfNeeded(
    amount: Decimal,
    spec: CurrencySpec
): Decimal {
    // Si no tiene decimales no hacemos nada
    if (spec.minorUnits == 0) return amount.setScale(0, RoundingMode.UNNECESSARY)

    if (spec.cashScale >= spec.minorUnits)
        return amount.setScale(spec.minorUnits, RoundingMode.HALF_UP)

    return amount.setScale(spec.cashScale, RoundingMode.HALF_UP)
}
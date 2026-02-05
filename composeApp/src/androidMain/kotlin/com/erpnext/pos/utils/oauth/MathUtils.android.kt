package com.erpnext.pos.utils.oauth

import java.math.BigDecimal
import java.math.RoundingMode

actual typealias Decimal = BigDecimal

actual fun bd(value: String): Decimal = value.toBigDecimal()
actual fun bd(value: Double): Decimal = value.toBigDecimal()
actual fun db(value: Float): Decimal = value.toBigDecimal()

actual fun Decimal.moneyScale(scale: Int): Decimal =
    this.setScale(scale, RoundingMode.HALF_UP)

actual fun Decimal.moneyScaleBankers(scale: Int): Decimal =
    this.setScale(scale, RoundingMode.HALF_EVEN)

actual fun Decimal.moneyScaleDown(scale: Int): Decimal =
    this.setScale(scale, RoundingMode.DOWN)

actual fun Decimal.coerceAtLeastZero(): Decimal =
    if (this < BigDecimal.ZERO) BigDecimal.ZERO else this

actual fun Decimal.safeDiv(divisor: Decimal, scale: Int): Decimal =
    if (divisor.compareTo(BigDecimal.ZERO) == 0) BigDecimal.ZERO
    else this.divide(divisor, scale, RoundingMode.HALF_UP)

actual fun Decimal.safeMul(other: Decimal): Decimal = this.multiply(other)

actual fun min(a: Decimal, b: Decimal): Decimal = if (a <= b) a else b

actual fun minOfBd(a: BigDecimal, b: BigDecimal): BigDecimal =
    if (a <= b) a else b

actual operator fun Decimal.compareTo(other: Decimal): Int = this.compareTo(other)

actual fun Decimal.toDouble(scale: Int): Double =
    this.setScale(scale, RoundingMode.HALF_UP).toDouble()

actual fun BigDecimal.isZero(): Boolean =
    this.compareTo(BigDecimal.ZERO) == 0

actual fun roundCashIfNeeded(
    amount: Decimal,
    spec: CurrencySpec
): Decimal {
    if (spec.minorUnits == 0) return amount.setScale(0, RoundingMode.UNNECESSARY)

    if (spec.cashScale >= spec.minorUnits)
        return amount.setScale(spec.minorUnits, RoundingMode.HALF_UP)

    return amount.setScale(spec.cashScale, RoundingMode.HALF_UP)
}

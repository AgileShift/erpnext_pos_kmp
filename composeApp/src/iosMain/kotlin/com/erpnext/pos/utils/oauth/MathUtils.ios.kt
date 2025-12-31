package com.erpnext.pos.utils.oauth

import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSDecimalNumberHandler
import platform.Foundation.NSNumber
import platform.Foundation.NSRoundingMode.NSRoundPlain

actual typealias Decimal = NSDecimalNumber

actual fun bd(value: String): Decimal =
    NSDecimalNumber.decimalNumberWithString(value)

actual fun db(value: Float): Decimal = NSDecimalNumber.decimalNumberWithString(value.toString())
actual fun bd(value: Double): Decimal = NSDecimalNumber.decimalNumberWithString(value.toString())

private fun handler(scale: Int): NSDecimalNumberHandler =
    NSDecimalNumberHandler(
        roundingMode = NSRoundPlain,          // equivalente práctico a HALF_UP en la mayoría de casos
        scale = scale.toShort(),
        raiseOnExactness = false,
        raiseOnOverflow = false,
        raiseOnUnderflow = false,
        raiseOnDivideByZero = false
    )

private val ZERO: Decimal = NSDecimalNumber.zero()

actual fun Decimal.moneyScale(scale: Int): Decimal =
    this.decimalNumberByRoundingAccordingToBehavior(handler(scale))

actual fun Decimal.safeDiv(divisor: Decimal, scale: Int): Decimal {
    // divisor == 0 ?
    if (divisor.compare(ZERO as NSNumber) == 0L) return ZERO

    // Divide con comportamiento (escala + rounding + sin crash)
    return this.decimalNumberByDividingBy(divisor, handler(scale))
}

actual fun Decimal.safeMul(other: Decimal): Decimal =
    this.decimalNumberByMultiplyingBy(other)

actual fun min(a: Decimal, b: Decimal): Decimal =
    if (a <= b) a else b

actual operator fun Decimal.compareTo(other: Decimal): Int {
    val r = this.compare(other as NSNumber) // -1,0,1 como Long
    return when (r) {
        -1L -> -1
        0L -> 0
        else -> 1
    }
}

actual fun Decimal.coerceAtLeastZero(): Decimal =
    if (this < ZERO) ZERO else this

actual fun NSDecimalNumber.isZero(): Boolean =
    this.compareTo(ZERO) == 0

actual fun Decimal.toDouble(scale: Int): Double =
    this.decimalNumberByRoundingAccordingToBehavior(handler(scale)).doubleValue

actual fun minOfBd(a: NSDecimalNumber, b: NSDecimalNumber): NSDecimalNumber =
    if (a <= b) a else b

actual fun roundCashIfNeeded(
    amount: Decimal,
    spec: CurrencySpec
): Decimal {
    return amount.decimalNumberByRoundingAccordingToBehavior(handler(spec.cashScale))
}
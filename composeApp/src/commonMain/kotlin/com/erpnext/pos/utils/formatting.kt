package com.erpnext.pos.utils

expect class DecimalFormatter() {
    fun format(value: Double, decimals: Int, includeSeparator: Boolean = false): String
}

// Función de utilidad más simple si no necesitas tanta configuración
expect fun formatDoubleToString(value: Double, decimals: Int): String
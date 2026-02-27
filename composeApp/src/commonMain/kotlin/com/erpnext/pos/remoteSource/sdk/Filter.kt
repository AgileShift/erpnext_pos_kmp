package com.erpnext.pos.remoteSource.sdk

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray

/**
 * Filter tipado: evita Any en la API pública.
 * value puede ser String/Number/Boolean o List<String|Number|Boolean>.
 */
data class Filter(
    val field: String,
    val operator: Operator,
    val value: Any
)

/** Convierte un valor permitido a JsonElement; lanza si tipo no soportado. */
private fun toJsonElement(value: Any): JsonElement = when (value) {
    is String -> JsonPrimitive(value)
    is Number -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    is List<*> -> buildJsonArray {
        value.forEach { v ->
            when (v) {
                is String -> add(JsonPrimitive(v))
                is Number -> add(JsonPrimitive(v))
                is Boolean -> add(JsonPrimitive(v))
                null -> add(JsonNull)
                else -> throw IllegalArgumentException("Tipo no soportado en lista de filtro: ${v::class.simpleName}")
            }
        }
    }

    else -> throw IllegalArgumentException("Tipo de filtro no soportado: ${value::class.simpleName}")
}

/** Construye el JSON que ERPNext espera para `filters`. */
fun buildFiltersJson(filters: List<Filter>): String {
    val arr = buildJsonArray {
        filters.forEach { f ->
            add(buildJsonArray {
                add(JsonPrimitive(f.field))
                add(JsonPrimitive(f.operator.symbol))
                add(toJsonElement(f.value))
            })
        }
    }
    return arr.toString()
}

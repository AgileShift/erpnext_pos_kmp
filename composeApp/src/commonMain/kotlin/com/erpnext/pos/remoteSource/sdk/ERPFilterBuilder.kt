package com.erpnext.pos.remoteSource.sdk

// -----------------------------
// Filtros tipados + DSL
// -----------------------------
enum class Operator(val symbol: String) {
    EQ("="),
    NE("!="),
    IN("in"),
    LIKE("like"),
    GT(">"),
    LT("<")
}

/** DSL builder para filtros legibles */
class FiltersBuilder {
    private val list = mutableListOf<Filter>()

    infix fun String.eq(value: Any) {
        list += Filter(this, Operator.EQ, value)
    }

    infix fun String.ne(value: Any) {
        list += Filter(this, Operator.NE, value)
    }

    infix fun String.`in`(values: List<Any>) {
        list += Filter(this, Operator.IN, values)
    }

    infix fun String.like(value: Any) {
        list += Filter(this, Operator.LIKE, value)
    }

    // Add others if needed...
    fun build(): List<Filter> = list.toList()
}

/** Helper DSL de conveniencia */
fun filters(block: FiltersBuilder.() -> Unit): List<Filter> {
    return FiltersBuilder().apply(block).build()
}
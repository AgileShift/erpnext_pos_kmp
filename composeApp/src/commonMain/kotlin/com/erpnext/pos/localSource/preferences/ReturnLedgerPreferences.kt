package com.erpnext.pos.localSource.preferences

import com.erpnext.pos.localSource.configuration.ConfigurationStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
private data class ReturnLedgerSnapshot(
    val byInvoice: Map<String, Map<String, Double>> = emptyMap()
)

class ReturnLedgerPreferences(
    private val store: ConfigurationStore
) {
    private val key = "return_ledger.v1"
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun returnedByItem(invoiceName: String): Map<String, Double> {
        val invoiceKey = invoiceName.trim()
        if (invoiceKey.isBlank()) return emptyMap()
        val snapshot = load()
        return snapshot.byInvoice[invoiceKey]
            ?.mapValues { (_, qty) -> qty.coerceAtLeast(0.0) }
            ?: emptyMap()
    }

    suspend fun returnedQty(invoiceName: String, itemCode: String): Double {
        val invoiceKey = invoiceName.trim()
        val itemKey = itemCode.trim()
        if (invoiceKey.isBlank() || itemKey.isBlank()) return 0.0
        return returnedByItem(invoiceKey)[itemKey]?.coerceAtLeast(0.0) ?: 0.0
    }

    suspend fun recordReturn(invoiceName: String, qtyByItemCode: Map<String, Double>) {
        val invoiceKey = invoiceName.trim()
        if (invoiceKey.isBlank() || qtyByItemCode.isEmpty()) return

        val current = load()
        val currentByInvoice = current.byInvoice.toMutableMap()
        val currentByItem = currentByInvoice[invoiceKey].orEmpty().toMutableMap()

        qtyByItemCode.forEach { (itemCodeRaw, qtyRaw) ->
            val itemCode = itemCodeRaw.trim()
            val qty = qtyRaw.coerceAtLeast(0.0)
            if (itemCode.isBlank() || qty <= 0.0) return@forEach
            currentByItem[itemCode] = (currentByItem[itemCode] ?: 0.0) + qty
        }

        currentByInvoice[invoiceKey] = currentByItem
        save(current.copy(byInvoice = currentByInvoice))
    }

    private suspend fun load(): ReturnLedgerSnapshot {
        val raw = store.loadRaw(key) ?: return ReturnLedgerSnapshot()
        return runCatching { json.decodeFromString<ReturnLedgerSnapshot>(raw) }
            .getOrElse { ReturnLedgerSnapshot() }
    }

    private suspend fun save(snapshot: ReturnLedgerSnapshot) {
        store.saveRaw(key, json.encodeToString(ReturnLedgerSnapshot.serializer(), snapshot))
    }
}

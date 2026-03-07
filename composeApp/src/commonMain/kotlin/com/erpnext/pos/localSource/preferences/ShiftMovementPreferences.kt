package com.erpnext.pos.localSource.preferences

import com.erpnext.pos.localSource.configuration.ConfigurationStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class ShiftMovementType {
  COLLECTION,
  REFUND,
  EXPENSE,
  INTERNAL_TRANSFER_IN,
  INTERNAL_TRANSFER_OUT,
  CASH_IN,
  CASH_OUT,
}

@Serializable
data class ShiftMovementRecord(
    val id: String,
    val posOpeningEntry: String,
    val profileName: String,
    val movementType: ShiftMovementType,
    val modeOfPayment: String,
    val amount: Double,
    val currency: String,
    val createdAt: Long,
    val note: String? = null,
)

@Serializable
private data class ShiftMovementSnapshot(
    val byOpeningEntry: Map<String, List<ShiftMovementRecord>> = emptyMap()
)

class ShiftMovementPreferences(private val store: ConfigurationStore) {
  private val key = "shift_movement_ledger.v1"
  private val json = Json { ignoreUnknownKeys = true }

  suspend fun append(record: ShiftMovementRecord) {
    val opening = record.posOpeningEntry.trim()
    if (opening.isBlank()) return
    val current = load()
    val byOpening = current.byOpeningEntry.toMutableMap()
    val existing = byOpening[opening].orEmpty().toMutableList()
    if (existing.none { it.id == record.id }) {
      existing.add(record)
      byOpening[opening] = existing
      save(current.copy(byOpeningEntry = byOpening))
    }
  }

  suspend fun listForOpeningEntry(posOpeningEntry: String): List<ShiftMovementRecord> {
    val opening = posOpeningEntry.trim()
    if (opening.isBlank()) return emptyList()
    return load().byOpeningEntry[opening].orEmpty().sortedBy { it.createdAt }
  }

  suspend fun clearOpeningEntry(posOpeningEntry: String) {
    val opening = posOpeningEntry.trim()
    if (opening.isBlank()) return
    val current = load()
    if (!current.byOpeningEntry.containsKey(opening)) return
    val byOpening = current.byOpeningEntry.toMutableMap()
    byOpening.remove(opening)
    save(current.copy(byOpeningEntry = byOpening))
  }

  private suspend fun load(): ShiftMovementSnapshot {
    val raw = store.loadRaw(key) ?: return ShiftMovementSnapshot()
    return runCatching { json.decodeFromString<ShiftMovementSnapshot>(raw) }
        .getOrElse { ShiftMovementSnapshot() }
  }

  private suspend fun save(snapshot: ShiftMovementSnapshot) {
    store.saveRaw(key, json.encodeToString(ShiftMovementSnapshot.serializer(), snapshot))
  }
}

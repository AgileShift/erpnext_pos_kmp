package com.erpnext.pos.localSource.entities

import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
interface SyncableEntity {
    var lastSyncedAt: Long?
}

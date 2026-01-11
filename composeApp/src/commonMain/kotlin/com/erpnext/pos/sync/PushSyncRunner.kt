package com.erpnext.pos.sync

import com.erpnext.pos.domain.sync.SyncContext

interface PushSyncRunner {
    suspend fun runPushQueue(ctx: SyncContext, onDocType: (String) -> Unit): Boolean
}

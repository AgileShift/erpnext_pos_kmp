package com.erpnext.pos.views.activity

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.sync.SyncState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ActivityFilter {
    Unread,
    All,
    HighPriority
}

data class ActivityUiState(
    val entries: List<ActivityEntry> = emptyList(),
    val unreadCount: Int = 0,
    val filter: ActivityFilter = ActivityFilter.Unread,
    val syncState: SyncState = SyncState.IDLE
)

class ActivityViewModel(
    private val activityCenter: ActivityCenter,
    private val syncManager: SyncManager
) : BaseViewModel() {

    private val _filter = MutableStateFlow(ActivityFilter.Unread)
    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                activityCenter.entries,
                _filter,
                syncManager.state
            ) { entries, filter, syncState ->
                val filtered = when (filter) {
                    ActivityFilter.Unread -> entries.filter { !it.isRead }
                    ActivityFilter.All -> entries
                    ActivityFilter.HighPriority -> entries.filter { it.priority == ActivityPriority.HIGH }
                }
                ActivityUiState(
                    entries = filtered,
                    unreadCount = entries.count { !it.isRead },
                    filter = filter,
                    syncState = syncState
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun setFilter(filter: ActivityFilter) {
        _filter.value = filter
    }

    fun markRead(id: String) {
        viewModelScope.launch {
            activityCenter.markRead(id)
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            activityCenter.markAllRead()
        }
    }

    fun syncNow() {
        syncManager.fullSync(force = true)
    }

    fun markViewed() {
        if (_uiState.value.unreadCount <= 0) return
        viewModelScope.launch {
            activityCenter.markAllRead()
            _uiState.update { it.copy(filter = ActivityFilter.All) }
        }
    }
}


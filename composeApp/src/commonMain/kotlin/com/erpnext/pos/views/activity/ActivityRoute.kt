package com.erpnext.pos.views.activity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.koin.compose.koinInject

@Composable
fun ActivityRoute(
    viewModel: ActivityViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.markViewed()
    }
    ActivityScreen(
        state = state,
        onFilterChange = viewModel::setFilter,
        onMarkRead = viewModel::markRead,
        onMarkAllRead = viewModel::markAllRead,
    )
}


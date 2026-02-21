package com.erpnext.pos.views.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.toErpDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun ActivityScreenPreview() {
    ActivityScreen(
        ActivityUiState(listOf(), 100, ActivityFilter.Unread, SyncState.IDLE),
        onFilterChange = {}, onMarkRead = {}, onMarkAllRead = {}, onSyncNow = {})
}

@Composable
fun ActivityScreen(
    state: ActivityUiState,
    onFilterChange: (ActivityFilter) -> Unit,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit,
    onSyncNow: () -> Unit
) {
    val strings = LocalAppStrings.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ActivityHero(
            unreadCount = state.unreadCount,
            syncState = state.syncState,
            strings = strings,
            onMarkAllRead = onMarkAllRead,
            onSyncNow = onSyncNow
        )
        Spacer(modifier = Modifier.height(12.dp))
        ActivityFilters(
            current = state.filter,
            strings = strings,
            onFilterChange = onFilterChange
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (state.entries.isEmpty()) {
            EmptyActivityState(strings)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.entries, key = { it.id }) { item ->
                    ActivityItemCard(
                        item = item,
                        onClick = { onMarkRead(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityHero(
    unreadCount: Int,
    syncState: SyncState,
    strings: com.erpnext.pos.localization.AppStrings,
    onMarkAllRead: () -> Unit,
    onSyncNow: () -> Unit
) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f),
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f)
        )
    )
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.activity.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${strings.activity.pendingLabel}: $unreadCount",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = when (syncState) {
                            is SyncState.SYNCING -> strings.activity.statusSyncing
                            is SyncState.ERROR -> strings.activity.statusError
                            SyncState.SUCCESS -> strings.activity.statusSynced
                            SyncState.IDLE -> strings.activity.statusIdle
                        },
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSyncNow,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.activity.syncButton)
                }
                Button(
                    onClick = onMarkAllRead,
                    enabled = unreadCount > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.activity.markReadButton)
                }
            }
        }
    }
}

@Composable
private fun ActivityFilters(
    current: ActivityFilter,
    strings: com.erpnext.pos.localization.AppStrings,
    onFilterChange: (ActivityFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = current == ActivityFilter.Unread,
            onClick = { onFilterChange(ActivityFilter.Unread) },
            label = { Text(strings.activity.unreadFilter) }
        )
        FilterChip(
            selected = current == ActivityFilter.All,
            onClick = { onFilterChange(ActivityFilter.All) },
            label = { Text(strings.activity.allFilter) }
        )
        FilterChip(
            selected = current == ActivityFilter.HighPriority,
            onClick = { onFilterChange(ActivityFilter.HighPriority) },
            label = { Text(strings.activity.highPriorityFilter) }
        )
    }
}

@Composable
private fun ActivityItemCard(
    item: ActivityEntry,
    onClick: () -> Unit
) {
    val strings = LocalAppStrings.current
    val indicatorColor = when (item.priority) {
        ActivityPriority.HIGH -> MaterialTheme.colorScheme.error
        ActivityPriority.MEDIUM -> MaterialTheme.colorScheme.tertiary
        ActivityPriority.LOW -> MaterialTheme.colorScheme.primary
    }
    val priorityText = when (item.priority) {
        ActivityPriority.HIGH -> strings.activity.priorityHigh
        ActivityPriority.MEDIUM -> strings.activity.priorityMedium
        ActivityPriority.LOW -> strings.activity.priorityLow
    }
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = if (item.isRead) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            }
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when (item.priority) {
                            ActivityPriority.HIGH -> Icons.Outlined.ErrorOutline
                            ActivityPriority.MEDIUM -> Icons.Outlined.Notifications
                            ActivityPriority.LOW -> Icons.Outlined.CheckCircle
                        },
                        contentDescription = null,
                        tint = indicatorColor
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (!item.isRead) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = indicatorColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${strings.activity.priorityPrefix} $priorityText",
                        style = MaterialTheme.typography.labelSmall,
                        color = indicatorColor
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = item.createdAt.toErpDateTime(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EmptyActivityState(strings: com.erpnext.pos.localization.AppStrings) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null,
                tint = Color(0xFF6B7280),
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = strings.activity.emptyTitle,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = strings.activity.emptyMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

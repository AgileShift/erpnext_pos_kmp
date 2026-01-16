package com.erpnext.pos.views.reconciliation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.erpnext.pos.localization.LocalAppStrings
import com.erpnext.pos.utils.formatDoubleToString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReconciliationScreen(
    state: ReconciliationState,
    actions: ReconciliationAction
) {
    val appStrings = LocalAppStrings.current
    val strings = appStrings.reconciliation

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.title) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = appStrings.common.back
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(strings.subtitle, style = MaterialTheme.typography.bodyMedium)
            when (state) {
                ReconciliationState.Loading -> {
                    Text(strings.loadingLabel)
                }
                ReconciliationState.Empty -> {
                    EmptyReconciliationState(strings)
                }
                is ReconciliationState.Error -> {
                    Text(state.message)
                }
                is ReconciliationState.Success -> {
                    ReconciliationSummary(state.sessions, strings)
                    ReconciliationChecklist(strings)
                    ReconciliationList(state.sessions, strings)
                }
            }
        }
    }
}

@Composable
private fun ReconciliationSummary(
    sessions: List<ReconciliationSessionUi>,
    strings: com.erpnext.pos.localization.ReconciliationStrings
) {
    val pendingCount = sessions.count { it.pendingSync }
    val latest = sessions.firstOrNull()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.summaryTitle, style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(strings.totalSessionsLabel, style = MaterialTheme.typography.bodySmall)
                    Text(
                        sessions.size.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(strings.pendingSyncLabel, style = MaterialTheme.typography.bodySmall)
                    Text(
                        pendingCount.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            latest?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${strings.latestCloseLabel}: ${it.periodEnd}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun ReconciliationChecklist(
    strings: com.erpnext.pos.localization.ReconciliationStrings
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.checklistTitle, style = MaterialTheme.typography.titleMedium)
            Text("1. ${strings.checklistStepOne}", style = MaterialTheme.typography.bodySmall)
            Text("2. ${strings.checklistStepTwo}", style = MaterialTheme.typography.bodySmall)
            Text("3. ${strings.checklistStepThree}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ReconciliationList(
    sessions: List<ReconciliationSessionUi>,
    strings: com.erpnext.pos.localization.ReconciliationStrings
) {
    Text(strings.sessionsTitle, style = MaterialTheme.typography.titleMedium)
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(sessions, key = { it.id }) { session ->
            ReconciliationSessionCard(session, strings)
        }
    }
}

@Composable
private fun ReconciliationSessionCard(
    session: ReconciliationSessionUi,
    strings: com.erpnext.pos.localization.ReconciliationStrings
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        session.posProfile,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${strings.openingLabel}: ${session.openingEntry}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                ReconciliationStatusChip(isPending = session.pendingSync, strings = strings)
            }
            Text(
                "${strings.periodLabel}: ${session.periodStart} → ${session.periodEnd}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "${strings.closingAmountLabel}: ${formatDoubleToString(session.closingAmount, 2)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${strings.entryIdLabel}: ${session.id}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ReconciliationStatusChip(
    isPending: Boolean,
    strings: com.erpnext.pos.localization.ReconciliationStrings
) {
    val label = if (isPending) strings.statusPending else strings.statusSynced
    val icon = if (isPending) Icons.Filled.Sync else Icons.Filled.CheckCircle
    val color = if (isPending) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun EmptyReconciliationState(
    strings: com.erpnext.pos.localization.ReconciliationStrings
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(strings.emptyTitle, style = MaterialTheme.typography.titleMedium)
            Text(strings.emptyMessage, style = MaterialTheme.typography.bodySmall)
        }
    }
}

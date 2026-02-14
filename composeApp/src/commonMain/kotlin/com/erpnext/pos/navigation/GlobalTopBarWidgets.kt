@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults.rememberTooltipPositionProvider
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

@Composable
fun ShiftOpenChip(isOpen: Boolean, duration: String, closeAction: () -> Unit = {}) {
    val openBg = Color(0xFFE8F5E9)
    val openText = Color(0xFF2E7D32)
    val closedBg = Color(0xFFFFEBEE)
    val closedText = Color(0xFFC62828)
    Surface(
        color = if (isOpen) openBg else closedBg,
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = if (isOpen) openText else closedText,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "Shift Open: ${if (isOpen) duration else "--"}",
                modifier = Modifier.clickable(
                    enabled = isOpen,
                    onClickLabel = "Close Shift",
                    interactionSource = MutableInteractionSource()
                ) {
                    closeAction()
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (isOpen) openText else closedText
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusIconButton(
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    TooltipBox(
        modifier = Modifier.size(24.dp),
        positionProvider = rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        val container = if (enabled) {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        }
        Surface(
            color = container,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 0.dp
        ) {
            IconButton(
                onClick = onClick,
                enabled = enabled,
                modifier = Modifier.size(36.dp)
            ) {
                CompositionLocalProvider(LocalContentColor provides tint) {
                    content()
                }
            }
        }
    }
}

fun formatShiftDuration(start: String?, nowMillis: Long): String {
    return try {
        if (start.isNullOrBlank()) return "--"

        val ldt = parseLooseLocalDateTime(start) ?: return "--"
        val instant = ldt.toInstant(TimeZone.currentSystemDefault())

        val diffMillis = nowMillis - instant.toEpochMilliseconds()
        if (diffMillis < 0) return "--"

        val totalSeconds = diffMillis / 1_000
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60

        "${hours}h ${minutes}m ${seconds}s"
    } catch (e: Exception) {
        e.printStackTrace()
        "--"
    }
}

private fun parseLooseLocalDateTime(input: String): LocalDateTime? {
    val s = input.trim().replace("T", " ")
    val parts = s.split(" ", limit = 2)

    val date = parts.getOrNull(0) ?: return null
    val time = parts.getOrNull(1) ?: "00:00:00"

    val d = date.split("-")
    if (d.size != 3) return null

    val t = time.split(":")
    val year = d[0].toIntOrNull() ?: return null
    val month = d[1].toIntOrNull() ?: return null
    val day = d[2].toIntOrNull() ?: return null

    val hour = t.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = t.getOrNull(1)?.toIntOrNull() ?: 0
    val second = t.getOrNull(2)?.toIntOrNull() ?: 0

    return LocalDateTime(
        year = year,
        month = Month(month).ordinal + 1,
        day = day,
        hour = hour,
        minute = minute,
        second = second,
        nanosecond = 0,
    )
}

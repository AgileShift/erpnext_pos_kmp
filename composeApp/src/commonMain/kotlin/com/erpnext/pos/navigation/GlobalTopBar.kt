package com.erpnext.pos.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp

data class GlobalTopBarState(
    val subtitle: String? = null,
    val showBack: Boolean? = null,
    val onBack: (() -> Unit)? = null
)

class TopBarController(
    initialState: GlobalTopBarState = GlobalTopBarState()
) {
    var state: GlobalTopBarState by mutableStateOf(initialState)
        private set

    fun set(newState: GlobalTopBarState) {
        state = newState
    }

    fun update(
        subtitle: String? = state.subtitle,
        showBack: Boolean? = state.showBack,
        onBack: (() -> Unit)? = state.onBack
    ) {
        state = state.copy(
            subtitle = subtitle,
            showBack = showBack,
            onBack = onBack
        )
    }

    fun reset() {
        state = GlobalTopBarState()
    }
}

val LocalTopBarController = staticCompositionLocalOf { TopBarController() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalTopBar(
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    showBack: Boolean,
    onBack: () -> Unit,
    bottomContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val backProgress by animateFloatAsState(
        targetValue = if (showBack) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "globalTopBarBackProgress"
    )
    val backWidth = lerp(0.dp, 48.dp, backProgress)
    Surface(
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
    ) {
        Column {
            TopAppBar(
                title = { title() },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .width(backWidth)
                            .alpha(backProgress)
                            .graphicsLayer {
                                scaleX = lerp(0.9f, 1f, backProgress)
                                scaleY = lerp(0.9f, 1f, backProgress)
                                clip = true
                            }
                    ) {
                        IconButton(
                            onClick = onBack,
                            enabled = showBack
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Atrás"
                            )
                        }
                    }
                },
                actions = {
                    actions()
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
            bottomContent()
        }
    }
}

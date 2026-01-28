package com.erpnext.pos.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo

enum class WindowWidthSizeClass { Compact, Medium, Expanded }
enum class WindowHeightSizeClass { Compact, Medium, Expanded }

data class WindowSizeClass(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass
)

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val widthDp = with(density) { windowInfo.containerSize.width.toDp().value }
    val heightDp = with(density) { windowInfo.containerSize.height.toDp().value }
    return WindowSizeClass(
        widthSizeClass = widthSizeClassFor(widthDp),
        heightSizeClass = heightSizeClassFor(heightDp)
    )
}

private fun widthSizeClassFor(widthDp: Float): WindowWidthSizeClass =
    when {
        widthDp < 600f -> WindowWidthSizeClass.Compact
        widthDp < 840f -> WindowWidthSizeClass.Medium
        else -> WindowWidthSizeClass.Expanded
    }

private fun heightSizeClassFor(heightDp: Float): WindowHeightSizeClass =
    when {
        heightDp < 480f -> WindowHeightSizeClass.Compact
        heightDp < 900f -> WindowHeightSizeClass.Medium
        else -> WindowHeightSizeClass.Expanded
    }

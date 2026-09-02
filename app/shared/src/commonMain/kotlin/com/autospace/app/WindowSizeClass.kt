package com.autospace.app

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSizeClass {
    Compact,   // телефон, < 600dp
    Medium,    // планшет, 600-840dp
    Expanded   // десктоп/широкое окно, > 840dp
}

fun windowSizeClassFor(widthDp: Dp): WindowSizeClass {
    return when {
        widthDp < 600.dp -> WindowSizeClass.Compact
        widthDp < 840.dp -> WindowSizeClass.Medium
        else -> WindowSizeClass.Expanded
    }
}

val LocalWindowSizeClass = compositionLocalOf { WindowSizeClass.Compact }
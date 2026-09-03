package com.autospace.app

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class AppTheme { Light, Dark }

val LocalAppTheme = compositionLocalOf { AppTheme.Dark }

data class AppColors(
    val bg0: Color,
    val bg1: Color,
    val card: Color,
    val border: Color,
    val borderStrong: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val success: Color,
    val error: Color,
    val examBlue: Color
)

fun darkAppColors() = AppColors(
    bg0 = Color(0xFF0A0917),
    bg1 = Color(0xFF100E24),
    card = Color(0x09FFFFFF),
    border = Color(0x17FFFFFF),
    borderStrong = Color(0x29FFFFFF),
    accent = Color(0xFF8B5CF6),
    textPrimary = Color(0xFFF5F3FB),
    textSecondary = Color(0xFF9E98B8),
    success = Color(0xFF4CAF6D),
    error = Color(0xFFE24B4B),
    examBlue = Color(0xFF3E7CE0)
)

fun lightAppColors() = AppColors(
    bg0 = Color(0xFFF5F4FA),
    bg1 = Color(0xFFF5F4FA),
    card = Color(0xFFFFFFFF),
    border = Color(0x17171317),
    borderStrong = Color(0x29171317),
    accent = Color(0xFF7C3AED),
    textPrimary = Color(0xFF16132B),
    textSecondary = Color(0xFF625C78),
    success = Color(0xFF3D9159),
    error = Color(0xFFC93E3E),
    examBlue = Color(0xFF2E63C2)
)

fun appColorsFor(theme: AppTheme) = if (theme == AppTheme.Dark) darkAppColors() else lightAppColors()

val LocalAppColors = compositionLocalOf { darkAppColors() }

fun autoSpaceColorScheme(theme: AppTheme): ColorScheme {
    val c = appColorsFor(theme)
    return if (theme == AppTheme.Dark) {
        darkColorScheme(
            primary = c.accent,
            onPrimary = Color.White,
            background = c.bg0,
            onBackground = c.textPrimary,
            surface = c.bg1,
            onSurface = c.textPrimary,
            surfaceVariant = c.card,
            onSurfaceVariant = c.textSecondary,
            error = c.error,
            onError = Color.White,
            outline = c.border
        )
    } else {
        lightColorScheme(
            primary = c.accent,
            onPrimary = Color.White,
            background = c.bg0,
            onBackground = c.textPrimary,
            surface = c.card,
            onSurface = c.textPrimary,
            surfaceVariant = Color(0xFFEFEDF7),
            onSurfaceVariant = c.textSecondary,
            error = c.error,
            onError = Color.White,
            outline = c.border
        )
    }
}

data class SemanticColors(
    val correct: Color,
    val incorrect: Color,
    val examSelected: Color,
    val examCompleted: Color
)

val LocalSemanticColors = compositionLocalOf { semanticColorsFor(AppTheme.Dark) }

fun semanticColorsFor(theme: AppTheme): SemanticColors {
    val c = appColorsFor(theme)
    return SemanticColors(
        correct = c.success,
        incorrect = c.error,
        examSelected = c.accent,
        examCompleted = c.examBlue
    )
}
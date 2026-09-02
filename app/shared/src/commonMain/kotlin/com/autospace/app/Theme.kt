package com.autospace.app

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// Асфальт — фон в тёмной теме, цвет текста в светлой. Один и тот же смысл, разные роли.
val AsphaltDark = Color(0xFF24262A)
val AsphaltDarkSurface = Color(0xFF2F3237)

val ConcreteLight = Color(0xFFF2F3F2)
val ConcreteLightSurface = Color(0xFFFFFFFF)

// Дорожная разметка — фирменный акцент, чуть темнее в светлой теме ради контраста.
val RoadMarkingYellowDark = Color(0xFFF2B705)
val RoadMarkingYellowLight = Color(0xFFE0A400)

// Семантика дорожных знаков — общая для обеих тем, чуть темнее в светлой для контраста на белом.
val SignGreenDark = Color(0xFF4CAF6D)
val SignGreenLight = Color(0xFF3D9159)

val SignRedDark = Color(0xFFE24B4B)
val SignRedLight = Color(0xFFC93E3E)

val SignBlueDark = Color(0xFF3E7CE0)
val SignBlueLight = Color(0xFF2E63C2)

val TextOnDark = Color(0xFFECE9E1)
val TextOnLight = AsphaltDark

enum class AppTheme {
    Light,
    Dark
}

val LocalAppTheme = compositionLocalOf { AppTheme.Dark }

fun autoSpaceColorScheme(theme: AppTheme): ColorScheme {
    return if (theme == AppTheme.Dark) {
        darkColorScheme(
            primary = RoadMarkingYellowDark,
            onPrimary = AsphaltDark,
            background = AsphaltDark,
            onBackground = TextOnDark,
            surface = AsphaltDarkSurface,
            onSurface = TextOnDark,
            surfaceVariant = AsphaltDarkSurface,
            onSurfaceVariant = TextOnDark,
            error = SignRedDark,
            onError = TextOnDark,
            outline = Color(0xFF57595E)
        )
    } else {
        lightColorScheme(
            primary = RoadMarkingYellowLight,
            onPrimary = TextOnLight,
            background = ConcreteLight,
            onBackground = TextOnLight,
            surface = ConcreteLightSurface,
            onSurface = TextOnLight,
            surfaceVariant = Color(0xFFE6E7E6),
            onSurfaceVariant = TextOnLight,
            error = SignRedLight,
            onError = ConcreteLightSurface,
            outline = Color(0xFFB8BAB9)
        )
    }
}

// Цвета для семантики "правильно/неправильно/экзамен", которые сейчас разбросаны
// по коду как Color(0xFF...) в TestScreen/MainMenuScreen/StatsScreen — собираем их сюда,
// чтобы дальше менять в одном месте, а не по всем экранам разом.
data class SemanticColors(
    val correct: Color,
    val incorrect: Color,
    val examSelected: Color,
    val examCompleted: Color
)

val LocalSemanticColors = compositionLocalOf {
    SemanticColors(
        correct = SignGreenDark,
        incorrect = SignRedDark,
        examSelected = Color(0xFF7E57C2),
        examCompleted = SignBlueDark
    )
}

fun semanticColorsFor(theme: AppTheme): SemanticColors {
    return if (theme == AppTheme.Dark) {
        SemanticColors(
            correct = SignGreenDark,
            incorrect = SignRedDark,
            examSelected = Color(0xFF9575CD),
            examCompleted = SignBlueDark
        )
    } else {
        SemanticColors(
            correct = SignGreenLight,
            incorrect = SignRedLight,
            examSelected = Color(0xFF7E57C2),
            examCompleted = SignBlueLight
        )
    }
}
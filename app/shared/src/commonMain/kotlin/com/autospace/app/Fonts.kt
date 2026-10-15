package com.autospace.app

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import autospace.app.shared.generated.resources.Res
import autospace.app.shared.generated.resources.*
import org.jetbrains.compose.resources.Font

@Composable
fun interFontFamily(): FontFamily = FontFamily(
    Font(Res.font.inter_regular, weight = FontWeight.Normal),
    Font(Res.font.inter_medium, weight = FontWeight.Medium),
    Font(Res.font.inter_semibold, weight = FontWeight.SemiBold)
)

@Composable
fun spaceGroteskFontFamily(): FontFamily = FontFamily(
    Font(Res.font.space_grotesk_regular, weight = FontWeight.Normal),
    Font(Res.font.space_grotesk_medium, weight = FontWeight.Medium),
    Font(Res.font.space_grotesk_bold, weight = FontWeight.Bold)
)

/**
 * Типографика Material на шрифте Inter. В браузере нет системных шрифтов, и стандартный шрифт
 * Material может не содержать кириллицу, поэтому для веба задаём Inter явно.
 */
@Composable
fun autoSpaceTypography(): Typography {
    val inter = interFontFamily()
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = inter),
        displayMedium = base.displayMedium.copy(fontFamily = inter),
        displaySmall = base.displaySmall.copy(fontFamily = inter),
        headlineLarge = base.headlineLarge.copy(fontFamily = inter),
        headlineMedium = base.headlineMedium.copy(fontFamily = inter),
        headlineSmall = base.headlineSmall.copy(fontFamily = inter),
        titleLarge = base.titleLarge.copy(fontFamily = inter),
        titleMedium = base.titleMedium.copy(fontFamily = inter),
        titleSmall = base.titleSmall.copy(fontFamily = inter),
        bodyLarge = base.bodyLarge.copy(fontFamily = inter),
        bodyMedium = base.bodyMedium.copy(fontFamily = inter),
        bodySmall = base.bodySmall.copy(fontFamily = inter),
        labelLarge = base.labelLarge.copy(fontFamily = inter),
        labelMedium = base.labelMedium.copy(fontFamily = inter),
        labelSmall = base.labelSmall.copy(fontFamily = inter)
    )
}

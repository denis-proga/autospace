package com.autospace.app

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
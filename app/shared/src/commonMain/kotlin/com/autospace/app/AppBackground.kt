package com.autospace.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = LocalAppColors.current
    val theme = LocalAppTheme.current

    Box(modifier = modifier.fillMaxSize().background(colors.bg0)) {
        if (theme == AppTheme.Dark) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-140).dp, y = (-140).dp)
                    .size(420.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(colors.accent.copy(alpha = 0.25f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 140.dp, y = (-120).dp)
                    .size(380.dp)
                    .background(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFF6D5BD0).copy(alpha = 0.22f), Color.Transparent)
                        ),
                        shape = CircleShape
                    )
            )
        }
        content()
    }
}
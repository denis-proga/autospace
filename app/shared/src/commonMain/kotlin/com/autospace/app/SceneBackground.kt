package com.autospace.app

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import autospace.app.shared.generated.resources.Res
import autospace.app.shared.generated.resources.scene_landscape
import autospace.app.shared.generated.resources.scene_portrait
import org.jetbrains.compose.resources.painterResource

/**
 * Фирменный фон: вечерняя дорога. Для широких окон берётся горизонтальная картинка,
 * для высоких (телефон) вертикальная. Камера очень медленно «приближается» и отдаляется.
 *
 * dim: 0..1, дополнительное затемнение для экранов с большим количеством текста.
 */
@Composable
fun SceneBackground(modifier: Modifier = Modifier, dim: Float = 0f) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            // пока картинка загружается (в вебе это доли секунды), виден градиент неба
            .background(
                Brush.verticalGradient(
                    listOf(Brand.Indigo, Color(0xFF2B2380), Color(0xFF5B3FA8), Color(0xFFF2A65A))
                )
            )
    ) {
        val landscape = maxWidth >= maxHeight

        val transition = rememberInfiniteTransition(label = "sceneZoom")
        val zoom by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.07f,
            animationSpec = infiniteRepeatable(
                animation = tween(24_000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "zoom"
        )

        Image(
            painter = painterResource(if (landscape) Res.drawable.scene_landscape else Res.drawable.scene_portrait),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomCenter,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
        )

        // лёгкое затемнение сверху и снизу, чтобы текст читался на любой части картинки
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0x66050520), Color.Transparent, Color(0x55050520))
                    )
                )
        )

        if (dim > 0f) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF070720).copy(alpha = dim)))
        }
    }
}

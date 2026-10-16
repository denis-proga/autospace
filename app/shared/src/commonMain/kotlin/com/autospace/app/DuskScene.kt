package com.autospace.app

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

/** Фирменные цвета AutoSpace. */
object Brand {
    val Yellow = Color(0xFFFFC83D)
    val Amber = Color(0xFFF2A65A)
    val Indigo = Color(0xFF12154E)
    val Violet = Color(0xFF5B3FA8)
    val Success = Color(0xFF3DDC97)
    val Error = Color(0xFFFF6B6B)
}

/**
 * Вечерняя дорога: небо с закатом, горы, ёлки и дорога с бегущей разметкой.
 * Нарисована кодом, поэтому почти ничего не весит и одинаково выглядит на всех платформах.
 * animated = false останавливает разметку (например, для экранов, где она отвлекает).
 */
@Composable
fun DuskScene(modifier: Modifier = Modifier, animated: Boolean = true) {
    val transition = rememberInfiniteTransition(label = "duskRoad")
    val roadPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "roadPhase"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        drawDusk(if (animated) roadPhase else 0f)
    }
}

private fun DrawScope.drawDusk(phase: Float) {
    val w = size.width
    val h = size.height
    val horizon = h * 0.70f
    val vpX = w * 0.60f

    // небо
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.00f to Color(0xFF12154E),
                0.32f to Color(0xFF2B2380),
                0.55f to Color(0xFF5B3FA8),
                0.66f to Color(0xFFB4548F),
                1.00f to Color(0xFFF2A65A)
            ),
            startY = 0f,
            endY = horizon
        ),
        size = Size(w, horizon)
    )

    // звёзды
    for (i in 0 until 46) {
        val fx = frac(sin(i * 12.9898f) * 43758.547f)
        val fy = frac(sin(i * 78.233f) * 12345.679f)
        val radius = 0.9f + frac(sin(i * 3.17f) * 999.1f) * 1.2f
        drawCircle(
            color = Color.White.copy(alpha = 0.25f + 0.45f * frac(sin(i * 5.3f) * 77.7f)),
            radius = radius,
            center = Offset(fx * w, fy * horizon * 0.55f)
        )
    }

    // свечение солнца у горизонта
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFD27A).copy(alpha = 0.95f),
                Color(0xFFFFA24A).copy(alpha = 0.45f),
                Color.Transparent
            ),
            center = Offset(vpX, horizon),
            radius = w * 0.34f
        ),
        radius = w * 0.34f,
        center = Offset(vpX, horizon)
    )

    // горы
    drawPath(
        path = mountains(baseY = horizon, amp = h * 0.11f, seed = 1.3f, freq = 2.1f),
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF5A3C9E), Color(0xFF2E2275)),
            startY = horizon - h * 0.12f,
            endY = horizon + h * 0.02f
        )
    )
    drawPath(
        path = mountains(baseY = horizon + h * 0.012f, amp = h * 0.075f, seed = 4.1f, freq = 3.0f),
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF2E2275), Color(0xFF1A1556)),
            startY = horizon - h * 0.07f,
            endY = horizon + h * 0.03f
        )
    )

    // земля
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF1B1650), Color(0xFF0B0A2B)),
            startY = horizon,
            endY = h
        ),
        topLeft = Offset(0f, horizon),
        size = Size(w, h - horizon)
    )

    // дорога
    val halfTop = w * 0.004f
    val halfBottom = w * 0.46f
    val road = Path().apply {
        moveTo(vpX - halfTop, horizon)
        lineTo(vpX + halfTop, horizon)
        lineTo(vpX + halfBottom, h)
        lineTo(vpX - halfBottom, h)
        close()
    }
    drawPath(
        path = road,
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF7A4A9A), Color(0xFF241D5C), Color(0xFF0E0C32)),
            startY = horizon,
            endY = h
        )
    )

    // отблеск заката на асфальте
    clipPath(road) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFB45A).copy(alpha = 0.40f), Color.Transparent),
                center = Offset(vpX, horizon + h * 0.02f),
                radius = w * 0.26f
            ),
            radius = w * 0.26f,
            center = Offset(vpX, horizon + h * 0.02f)
        )
    }

    // кромки дороги
    drawLine(
        color = Color.White.copy(alpha = 0.35f),
        start = Offset(vpX - halfTop, horizon),
        end = Offset(vpX - halfBottom, h),
        strokeWidth = 3f
    )
    drawLine(
        color = Color.White.copy(alpha = 0.35f),
        start = Offset(vpX + halfTop, horizon),
        end = Offset(vpX + halfBottom, h),
        strokeWidth = 3f
    )

    // бегущая жёлтая разметка: полосы движутся к зрителю
    val dashCount = 9
    for (i in 0 until dashCount) {
        val t0 = (i + phase) / dashCount
        val t1 = t0 + 0.42f / dashCount
        val y0 = horizon + (h - horizon) * t0 * t0
        val y1 = horizon + (h - horizon) * t1 * t1
        val f0 = (y0 - horizon) / (h - horizon)
        val f1 = (y1 - horizon) / (h - horizon)
        val half0 = w * 0.0015f + w * 0.011f * f0
        val half1 = w * 0.0015f + w * 0.011f * f1
        val dash = Path().apply {
            moveTo(vpX - half0, y0)
            lineTo(vpX + half0, y0)
            lineTo(vpX + half1, y1)
            lineTo(vpX - half1, y1)
            close()
        }
        drawPath(dash, color = Brand.Yellow.copy(alpha = (0.25f + 0.75f * f1).coerceIn(0f, 1f)))
    }

    // ёлки по краям: дальние светлее, ближние темнее
    val leftTrees = listOf(
        Triple(0.34f, 0.075f, 0.735f), Triple(0.29f, 0.12f, 0.765f), Triple(0.22f, 0.20f, 0.82f),
        Triple(0.13f, 0.32f, 0.90f), Triple(0.03f, 0.46f, 1.00f)
    )
    val rightTrees = listOf(
        Triple(0.76f, 0.08f, 0.738f), Triple(0.81f, 0.13f, 0.77f), Triple(0.87f, 0.21f, 0.83f),
        Triple(0.94f, 0.34f, 0.91f), Triple(1.00f, 0.48f, 1.02f)
    )
    for (tree in leftTrees + rightTrees) {
        val depth = ((tree.third - 0.735f) / 0.285f).coerceIn(0f, 1f)
        val color = lerpColor(Color(0xFF241D63), Color(0xFF080826), depth)
        drawFir(cx = tree.first * w, baseY = tree.third * h, treeH = tree.second * h, color = color)
    }

    // виньетка: затемняет края, чтобы текст читался
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0xAA050520)),
            center = Offset(w / 2f, h * 0.45f),
            radius = max(w, h) * 0.78f
        )
    )
}

private fun frac(x: Float): Float = x - floor(x)

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red = a.red + (b.red - a.red) * t,
    green = a.green + (b.green - a.green) * t,
    blue = a.blue + (b.blue - a.blue) * t,
    alpha = 1f
)

private fun DrawScope.mountains(baseY: Float, amp: Float, seed: Float, freq: Float): Path {
    val w = size.width
    val path = Path()
    path.moveTo(0f, size.height)
    path.lineTo(0f, baseY)
    val step = w / 90f
    var x = 0f
    val twoPi = 2f * PI.toFloat()
    while (x <= w + step) {
        val u = x / w
        val noise = 0.55f * sin(u * freq * twoPi + seed) +
                0.30f * sin(u * freq * 2.7f * twoPi + seed * 1.7f) +
                0.15f * sin(u * freq * 6.1f * twoPi + seed * 2.9f)
        path.lineTo(x, baseY - amp * (0.5f + 0.5f * noise))
        x += step
    }
    path.lineTo(w, size.height)
    path.close()
    return path
}

private fun DrawScope.drawFir(cx: Float, baseY: Float, treeH: Float, color: Color) {
    for (i in 0 until 4) {
        val apexY = baseY - treeH + i * treeH * 0.19f
        val layerBaseY = apexY + treeH * 0.40f
        val half = treeH * (0.14f + 0.055f * i)
        val layer = Path().apply {
            moveTo(cx, apexY)
            lineTo(cx - half, layerBaseY)
            lineTo(cx + half, layerBaseY)
            close()
        }
        drawPath(layer, color = color)
    }
}

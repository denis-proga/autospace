package com.autospace.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Значки главной страницы, нарисованные кодом. Все масштабируются без потери чёткости.

private fun lineStroke(width: Float) = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

@Composable
fun HomeIcon(color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val path = Path().apply {
            moveTo(w * 0.5f, h * 0.12f)
            lineTo(w * 0.92f, h * 0.48f)
            lineTo(w * 0.80f, h * 0.48f)
            lineTo(w * 0.80f, h * 0.88f)
            lineTo(w * 0.60f, h * 0.88f)
            lineTo(w * 0.60f, h * 0.62f)
            lineTo(w * 0.40f, h * 0.62f)
            lineTo(w * 0.40f, h * 0.88f)
            lineTo(w * 0.20f, h * 0.88f)
            lineTo(w * 0.20f, h * 0.48f)
            lineTo(w * 0.08f, h * 0.48f)
            close()
        }
        drawPath(path, color = color)
    }
}

@Composable
fun DocIcon(color: Color, size: Dp = 24.dp, filled: Boolean = false) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.08f
        if (filled) {
            drawRoundRect(color = color, topLeft = Offset(w * 0.2f, h * 0.1f), size = Size(w * 0.6f, h * 0.8f), cornerRadius = CornerRadius(w * 0.1f))
            for (i in 0 until 3) {
                drawLine(Color.White.copy(alpha = 0.85f), Offset(w * 0.32f, h * (0.34f + i * 0.17f)), Offset(w * 0.68f, h * (0.34f + i * 0.17f)), sw, StrokeCap.Round)
            }
        } else {
            drawRoundRect(color = color, topLeft = Offset(w * 0.2f, h * 0.1f), size = Size(w * 0.6f, h * 0.8f), cornerRadius = CornerRadius(w * 0.1f), style = lineStroke(sw))
            for (i in 0 until 3) {
                drawLine(color, Offset(w * 0.34f, h * (0.34f + i * 0.17f)), Offset(w * 0.66f, h * (0.34f + i * 0.17f)), sw, StrokeCap.Round)
            }
        }
    }
}

@Composable
fun RefreshIcon(color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f
        drawArc(color = color, startAngle = -50f, sweepAngle = 280f, useCenter = false, topLeft = Offset(w * 0.15f, h * 0.15f), size = Size(w * 0.7f, h * 0.7f), style = lineStroke(sw))
        val head = Path().apply {
            moveTo(w * 0.62f, h * 0.10f)
            lineTo(w * 0.86f, h * 0.22f)
            lineTo(w * 0.60f, h * 0.36f)
            close()
        }
        drawPath(head, color = color)
    }
}

private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float): Path {
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) outer else inner
        val angle = -PI.toFloat() / 2f + i * PI.toFloat() / 5f
        val x = cx + r * cos(angle)
        val y = cy + r * sin(angle)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

@Composable
fun StarIcon(color: Color, size: Dp = 24.dp, filled: Boolean = false) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val path = starPath(w * 0.5f, w * 0.54f, w * 0.44f, w * 0.19f)
        if (filled) drawPath(path, color = color) else drawPath(path, color = color, style = lineStroke(w * 0.08f))
    }
}

@Composable
fun TrophyIcon(size: Dp = 28.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val gold = Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFA928)))
        val cup = Path().apply {
            moveTo(w * 0.26f, h * 0.10f)
            lineTo(w * 0.74f, h * 0.10f)
            lineTo(w * 0.70f, h * 0.46f)
            quadraticTo(w * 0.5f, h * 0.70f, w * 0.30f, h * 0.46f)
            close()
        }
        drawPath(cup, brush = gold)
        drawArc(Color(0xFFFFB92E), 90f, 180f, false, Offset(w * 0.04f, h * 0.14f), Size(w * 0.30f, h * 0.30f), style = lineStroke(w * 0.07f))
        drawArc(Color(0xFFFFB92E), -90f, 180f, false, Offset(w * 0.66f, h * 0.14f), Size(w * 0.30f, h * 0.30f), style = lineStroke(w * 0.07f))
        drawRect(Color(0xFFFFB92E), Offset(w * 0.45f, h * 0.62f), Size(w * 0.10f, h * 0.16f))
        drawRoundRect(brush = gold, topLeft = Offset(w * 0.28f, h * 0.78f), size = Size(w * 0.44f, h * 0.13f), cornerRadius = CornerRadius(w * 0.04f))
    }
}

@Composable
fun BulbIcon(size: Dp = 28.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val c = Color(0xFFFFD84D)
        val sw = w * 0.07f
        drawCircle(c, radius = w * 0.24f, center = Offset(w * 0.5f, h * 0.38f), style = lineStroke(sw))
        drawLine(c, Offset(w * 0.40f, h * 0.70f), Offset(w * 0.60f, h * 0.70f), sw, StrokeCap.Round)
        drawLine(c, Offset(w * 0.42f, h * 0.80f), Offset(w * 0.58f, h * 0.80f), sw, StrokeCap.Round)
        drawLine(c, Offset(w * 0.50f, h * 0.05f), Offset(w * 0.50f, h * 0.0f), sw, StrokeCap.Round)
        drawLine(c, Offset(w * 0.18f, h * 0.18f), Offset(w * 0.10f, h * 0.10f), sw, StrokeCap.Round)
        drawLine(c, Offset(w * 0.82f, h * 0.18f), Offset(w * 0.90f, h * 0.10f), sw, StrokeCap.Round)
    }
}

@Composable
fun TargetIcon(size: Dp = 28.dp, color: Color = Color(0xFFFF7A59)) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val sw = w * 0.08f
        val c = Offset(w * 0.46f, w * 0.54f)
        drawCircle(color, radius = w * 0.38f, center = c, style = lineStroke(sw))
        drawCircle(color, radius = w * 0.24f, center = c, style = lineStroke(sw))
        drawCircle(color, radius = w * 0.08f, center = c)
        drawLine(Color(0xFFFFD84D), c, Offset(w * 0.92f, w * 0.08f), sw, StrokeCap.Round)
    }
}

@Composable
fun CalendarIcon(color: Color, size: Dp = 24.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.08f
        drawRoundRect(color, Offset(w * 0.12f, h * 0.2f), Size(w * 0.76f, h * 0.68f), CornerRadius(w * 0.12f), style = lineStroke(sw))
        drawLine(color, Offset(w * 0.12f, h * 0.4f), Offset(w * 0.88f, h * 0.4f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.32f, h * 0.1f), Offset(w * 0.32f, h * 0.28f), sw, StrokeCap.Round)
        drawLine(color, Offset(w * 0.68f, h * 0.1f), Offset(w * 0.68f, h * 0.28f), sw, StrokeCap.Round)
        drawCircle(color, w * 0.05f, Offset(w * 0.34f, h * 0.58f))
        drawCircle(color, w * 0.05f, Offset(w * 0.5f, h * 0.58f))
        drawCircle(color, w * 0.05f, Offset(w * 0.66f, h * 0.58f))
        drawCircle(color, w * 0.05f, Offset(w * 0.34f, h * 0.74f))
    }
}

@Composable
fun FlameIcon(size: Dp = 36.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val outer = Path().apply {
            moveTo(w * 0.5f, h * 0.04f)
            cubicTo(w * 0.62f, h * 0.30f, w * 0.92f, h * 0.42f, w * 0.88f, h * 0.68f)
            cubicTo(w * 0.85f, h * 0.90f, w * 0.65f, h * 0.98f, w * 0.5f, h * 0.98f)
            cubicTo(w * 0.35f, h * 0.98f, w * 0.12f, h * 0.88f, w * 0.12f, h * 0.66f)
            cubicTo(w * 0.12f, h * 0.48f, w * 0.30f, h * 0.40f, w * 0.34f, h * 0.24f)
            cubicTo(w * 0.42f, h * 0.30f, w * 0.44f, h * 0.20f, w * 0.5f, h * 0.04f)
            close()
        }
        drawPath(outer, brush = Brush.verticalGradient(listOf(Color(0xFFFFB02E), Color(0xFFFF5A1F))))
        val inner = Path().apply {
            moveTo(w * 0.5f, h * 0.50f)
            cubicTo(w * 0.62f, h * 0.62f, w * 0.72f, h * 0.70f, w * 0.68f, h * 0.82f)
            cubicTo(w * 0.64f, h * 0.94f, w * 0.36f, h * 0.94f, w * 0.32f, h * 0.82f)
            cubicTo(w * 0.28f, h * 0.70f, w * 0.44f, h * 0.62f, w * 0.5f, h * 0.50f)
            close()
        }
        drawPath(inner, brush = Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFB02E))))
    }
}

/** Круглый значок статуса: заливка цветом и белая галочка или крестик. */
@Composable
fun StatusDot(color: Color, check: Boolean, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        drawCircle(color = color)
        val mark = Path().apply {
            if (check) {
                moveTo(w * 0.28f, w * 0.52f)
                lineTo(w * 0.44f, w * 0.67f)
                lineTo(w * 0.72f, w * 0.36f)
            } else {
                moveTo(w * 0.33f, w * 0.33f)
                lineTo(w * 0.67f, w * 0.67f)
                moveTo(w * 0.67f, w * 0.33f)
                lineTo(w * 0.33f, w * 0.67f)
            }
        }
        drawPath(mark, color = Color.White, style = lineStroke(w * 0.11f))
    }
}

/** Пустое кольцо (тест не начат) или кольцо с жёлтой дугой (тест в процессе). */
@Composable
fun RingIcon(color: Color, size: Dp = 22.dp, progress: Float = 0f) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val sw = w * 0.1f
        drawCircle(color = color, radius = w * 0.40f, center = Offset(w * 0.5f, w * 0.5f), style = lineStroke(sw))
        if (progress > 0f) {
            drawArc(Brand.Yellow, -90f, 360f * progress, false, Offset(w * 0.1f, w * 0.1f), Size(w * 0.8f, w * 0.8f), style = lineStroke(sw * 1.3f))
        }
    }
}

@Composable
fun ChevronDownIcon(color: Color, size: Dp = 16.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val p = Path().apply {
            moveTo(w * 0.2f, w * 0.36f)
            lineTo(w * 0.5f, w * 0.66f)
            lineTo(w * 0.8f, w * 0.36f)
        }
        drawPath(p, color = color, style = lineStroke(w * 0.12f))
    }
}

@Composable
fun LogoutIcon(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.085f
        val door = Path().apply {
            moveTo(w * 0.46f, h * 0.18f)
            lineTo(w * 0.18f, h * 0.18f)
            lineTo(w * 0.18f, h * 0.82f)
            lineTo(w * 0.46f, h * 0.82f)
        }
        drawPath(door, color = color, style = lineStroke(sw))
        drawLine(color, Offset(w * 0.40f, h * 0.5f), Offset(w * 0.86f, h * 0.5f), sw, StrokeCap.Round)
        val arrow = Path().apply {
            moveTo(w * 0.68f, h * 0.32f)
            lineTo(w * 0.86f, h * 0.5f)
            lineTo(w * 0.68f, h * 0.68f)
        }
        drawPath(arrow, color = color, style = lineStroke(sw))
    }
}

@Composable
fun BarsIcon(color: Color, size: Dp = 22.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawRoundRect(color, Offset(w * 0.14f, h * 0.52f), Size(w * 0.17f, h * 0.38f), CornerRadius(w * 0.05f))
        drawRoundRect(color, Offset(w * 0.415f, h * 0.30f), Size(w * 0.17f, h * 0.60f), CornerRadius(w * 0.05f))
        drawRoundRect(color, Offset(w * 0.69f, h * 0.10f), Size(w * 0.17f, h * 0.80f), CornerRadius(w * 0.05f))
    }
}

/** Значок звания: шестиугольник и академическая шапочка. */
@Composable
fun RankBadgeIcon(size: Dp = 52.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val hex = Path()
        for (i in 0 until 6) {
            val a = -PI.toFloat() / 2f + i * PI.toFloat() / 3f
            val x = w * 0.5f + w * 0.46f * cos(a)
            val y = h * 0.5f + h * 0.46f * sin(a)
            if (i == 0) hex.moveTo(x, y) else hex.lineTo(x, y)
        }
        hex.close()
        drawPath(hex, brush = Brush.verticalGradient(listOf(Color(0xFF2A3FC7), Color(0xFF14197A))))
        drawPath(hex, brush = Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFA928))), style = lineStroke(w * 0.055f))
        val cap = Path().apply {
            moveTo(w * 0.5f, h * 0.30f)
            lineTo(w * 0.80f, h * 0.45f)
            lineTo(w * 0.5f, h * 0.60f)
            lineTo(w * 0.20f, h * 0.45f)
            close()
        }
        drawPath(cap, brush = Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFA928))))
        val base = Path().apply {
            moveTo(w * 0.32f, h * 0.55f)
            lineTo(w * 0.32f, h * 0.66f)
            quadraticTo(w * 0.5f, h * 0.76f, w * 0.68f, h * 0.66f)
            lineTo(w * 0.68f, h * 0.55f)
            lineTo(w * 0.5f, h * 0.64f)
            close()
        }
        drawPath(base, color = Color(0xFFFFB92E))
    }
}

/** Значок достижения: круг со звездой и лентами. */
@Composable
fun MedalIcon(size: Dp = 28.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        drawLine(Color(0xFFFF7A59), Offset(w * 0.34f, h * 0.62f), Offset(w * 0.26f, h * 0.96f), w * 0.14f, StrokeCap.Round)
        drawLine(Color(0xFF3D8BFF), Offset(w * 0.66f, h * 0.62f), Offset(w * 0.74f, h * 0.96f), w * 0.14f, StrokeCap.Round)
        drawCircle(brush = Brush.verticalGradient(listOf(Color(0xFFFFE27A), Color(0xFFFFA928))), radius = w * 0.40f, center = Offset(w * 0.5f, h * 0.38f))
        drawPath(starPath(w * 0.5f, h * 0.39f, w * 0.22f, w * 0.095f), color = Color(0xFFFFF4C2))
    }
}

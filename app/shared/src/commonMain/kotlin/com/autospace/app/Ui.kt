package com.autospace.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

// ---------------------------------------------------------------------------------------------
// Общий набор деталей интерфейса в стиле «Вечерняя дорога». Экраны собираются из них.
// ---------------------------------------------------------------------------------------------

private val InkDark = Color(0xFF1B1540)

/** «Auto» белым и «Space» жёлтым. */
@Composable
fun Wordmark(fontSize: TextUnit = 34.sp, modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White)) { append("Auto") }
            withStyle(SpanStyle(color = Brand.Yellow)) { append("Space") }
        },
        fontFamily = interFontFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize = fontSize,
        modifier = modifier
    )
}

/** Стеклянная карточка: тёмное полупрозрачное стекло со светлой кромкой. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A1470).copy(alpha = 0.90f), Color(0xFF0E0B42).copy(alpha = 0.94f))
                )
            )
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0.08f))),
                shape
            )
            .padding(24.dp),
        content = content
    )
}

/** Поле ввода: значок слева, подсказка, жёлтая рамка в фокусе и коралловая при ошибке. */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leading: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val interFamily = interFontFamily()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val requester = remember { FocusRequester() }
    val shape = RoundedCornerShape(18.dp)

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> Brand.Error
            focused -> Brand.Yellow
            else -> Color.White.copy(alpha = 0.28f)
        },
        animationSpec = tween(160),
        label = "fieldBorder"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(Color(0xFF2A2590).copy(alpha = if (focused) 0.70f else 0.55f))
            .border(if (focused || isError) 1.5.dp else 1.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { requester.requestFocus() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading()
        Box(
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    fontFamily = interFamily,
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.68f)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontFamily = interFamily, fontSize = 16.sp),
                cursorBrush = SolidColor(Brand.Yellow),
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                interactionSource = interaction,
                modifier = Modifier.fillMaxWidth().focusRequester(requester)
            )
        }
        if (trailing != null) trailing()
    }
}

/** Главная кнопка: жёлтая, когда доступна; в недоступном состоянии фиолетовая, но текст остаётся читаемым. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val active = enabled || isLoading
    val scale by animateFloatAsState(
        targetValue = when {
            pressed && enabled -> 0.97f
            hovered && enabled -> 1.015f
            else -> 1f
        },
        animationSpec = tween(120),
        label = "primaryScale"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(50))
            .background(
                if (active) Brush.verticalGradient(listOf(Color(0xFFFFD84D), Color(0xFFFFB627)))
                else Brush.verticalGradient(listOf(Color(0xFF3B3597), Color(0xFF2B2680)))
            )
            .hoverable(interaction)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && !isLoading,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = InkDark, strokeWidth = 2.5.dp)
        } else {
            Text(
                text = text,
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
                color = if (active) InkDark else Color.White.copy(alpha = 0.72f)
            )
        }
    }
}

/** Вторая кнопка: контурная. withChevron: стрелка справа; leadingChevron: стрелка слева («назад»). */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    withChevron: Boolean = true,
    leadingChevron: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val hovered by interaction.collectIsHoveredAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(120),
        label = "secondaryScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (hovered && enabled) Brand.Yellow else Color.White.copy(alpha = 0.42f),
        animationSpec = tween(160),
        label = "secondaryBorder"
    )
    val shape = RoundedCornerShape(50)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.55f
            }
            .clip(shape)
            .background(Color(0xFF2A2590).copy(alpha = 0.35f))
            .border(1.dp, borderColor, shape)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (leadingChevron) ChevronIcon(color = Brand.Yellow, left = true, modifier = Modifier.size(20.dp))
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = Color.White,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )
        if (leadingChevron) Box(modifier = Modifier.width(20.dp))
        if (withChevron && !leadingChevron) ChevronIcon(color = Brand.Yellow, modifier = Modifier.size(20.dp))
    }
}

/** Ненавязчивое сообщение об ошибке пользователя: коралловая точка и спокойный текст. */
@Composable
fun GentleError(message: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(Brand.Error)
        )
        Text(
            text = message,
            fontFamily = interFontFamily(),
            fontSize = 14.sp,
            color = Color(0xFFFFB3B3),
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

// ----------------------------- значки, нарисованные кодом -----------------------------------

private fun strokeFor(width: Float) = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round)

@Composable
fun UserIcon(color: Color = Brand.Yellow, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = strokeFor(w * 0.075f)
        drawCircle(color = color, radius = w * 0.19f, center = Offset(w * 0.5f, h * 0.33f), style = stroke)
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.18f, h * 0.60f),
            size = Size(w * 0.64f, h * 0.66f),
            style = stroke
        )
    }
}

@Composable
fun LockIcon(color: Color = Brand.Yellow, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = strokeFor(w * 0.075f)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.2f, h * 0.46f),
            size = Size(w * 0.6f, h * 0.44f),
            cornerRadius = CornerRadius(w * 0.1f),
            style = stroke
        )
        drawArc(
            color = color,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.3f, h * 0.12f),
            size = Size(w * 0.4f, h * 0.5f),
            style = stroke
        )
        drawCircle(color = color, radius = w * 0.045f, center = Offset(w * 0.5f, h * 0.66f))
    }
}

@Composable
fun MailIcon(color: Color = Brand.Yellow, modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = strokeFor(w * 0.075f)
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.1f, h * 0.24f),
            size = Size(w * 0.8f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.09f),
            style = stroke
        )
        val flap = Path().apply {
            moveTo(w * 0.13f, h * 0.30f)
            lineTo(w * 0.5f, h * 0.56f)
            lineTo(w * 0.87f, h * 0.30f)
        }
        drawPath(flap, color = color, style = stroke)
    }
}

/** open = true: глаз открыт (пароль виден), false: перечёркнут. */
@Composable
fun EyeIcon(open: Boolean, color: Color = Color.White.copy(alpha = 0.75f), modifier: Modifier = Modifier.size(24.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = strokeFor(w * 0.075f)
        val eye = Path().apply {
            moveTo(w * 0.06f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 0.0f, w * 0.94f, h * 0.5f)
            quadraticTo(w * 0.5f, h * 1.0f, w * 0.06f, h * 0.5f)
            close()
        }
        drawPath(eye, color = color, style = stroke)
        drawCircle(color = color, radius = w * 0.13f, center = Offset(w * 0.5f, h * 0.5f))
        if (!open) {
            drawLine(color, Offset(w * 0.16f, h * 0.86f), Offset(w * 0.84f, h * 0.14f), w * 0.075f, StrokeCap.Round)
        }
    }
}

@Composable
fun ChevronIcon(color: Color = Brand.Yellow, modifier: Modifier = Modifier.size(20.dp), left: Boolean = false) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val x1 = if (left) 0.64f else 0.36f
        val x2 = if (left) 0.34f else 0.66f
        val path = Path().apply {
            moveTo(w * x1, h * 0.2f)
            lineTo(w * x2, h * 0.5f)
            lineTo(w * x1, h * 0.8f)
        }
        drawPath(path, color = color, style = strokeFor(w * 0.1f))
    }
}

// ------------------------------- новые детали для форм --------------------------------------

/**
 * Заголовок с жёлтым акцентом. Акцент можно задать разметкой "Введите [[код подтверждения]]";
 * если разметки нет, жёлтым выделяется последнее слово.
 */
@Composable
fun AccentTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    textAlign: TextAlign = TextAlign.Start
) {
    val open = text.indexOf("[[")
    val close = text.indexOf("]]")
    val before: String
    val accent: String
    val after: String
    if (open >= 0 && close > open) {
        before = text.substring(0, open)
        accent = text.substring(open + 2, close)
        after = text.substring(close + 2)
    } else {
        val trimmed = text.trim()
        val split = trimmed.lastIndexOf(' ')
        before = if (split < 0) "" else trimmed.substring(0, split + 1)
        accent = if (split < 0) trimmed else trimmed.substring(split + 1)
        after = ""
    }

    Text(
        text = buildAnnotatedString {
            append(before)
            withStyle(SpanStyle(color = Brand.Yellow)) { append(accent) }
            append(after)
        },
        fontFamily = interFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = fontSize,
        lineHeight = fontSize * 1.2f,
        color = Color.White,
        textAlign = textAlign,
        modifier = modifier
    )
}

/**
 * Основа экранов-форм: сцена на весь экран и содержимое по центру.
 * framed = true: содержимое обычно кладут на стеклянную карточку.
 * framed = false: элементы лежат прямо на странице. Верх экрана затемняется плотным слоем, чтобы
 * текст читался, а внизу остаётся видна сцена с дорогой и машиной.
 */
@Composable
fun FormScaffold(
    onBack: (() -> Unit)? = null,
    topEnd: (@Composable BoxScope.() -> Unit)? = null,
    framed: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        SceneBackground(dim = if (framed) 0.06f else 0f)

        if (!framed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color(0xF20D0B45),
                                0.55f to Color(0xE60D0B45),
                                0.80f to Color(0x800D0B45),
                                1.00f to Color(0x000D0B45)
                            )
                        )
                    )
            )
        }

        val columnModifier = if (framed) {
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 64.dp, bottom = 24.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .height(maxHeight * 0.84f)
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = 12.dp)
        }

        Column(
            modifier = columnModifier.graphicsLayer {
                alpha = appear.value
                translationY = (1f - appear.value) * 30f
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            content = content
        )

        if (onBack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF14104A).copy(alpha = 0.75f))
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onBack() },
                contentAlignment = Alignment.Center
            ) {
                ChevronIcon(color = Brand.Yellow, left = true, modifier = Modifier.size(22.dp))
            }
        }

        if (topEnd != null) {
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) { topEnd() }
        }
    }
}

/** Флажок согласия. */
@Composable
fun GlassCheckbox(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(8.dp)
    val fill by animateColorAsState(
        targetValue = if (checked) Brand.Yellow else Color(0xFF2A2590).copy(alpha = 0.55f),
        animationSpec = tween(140),
        label = "checkFill"
    )
    Box(
        modifier = modifier
            .size(26.dp)
            .clip(shape)
            .background(fill)
            .border(1.5.dp, if (checked) Brand.Yellow else Color.White.copy(alpha = 0.5f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                onCheckedChange(!checked)
            },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val w = size.width
                val h = size.height
                val tick = Path().apply {
                    moveTo(w * 0.14f, h * 0.52f)
                    lineTo(w * 0.40f, h * 0.76f)
                    lineTo(w * 0.86f, h * 0.24f)
                }
                drawPath(tick, color = InkDark, style = strokeFor(w * 0.16f))
            }
        }
    }
}

/** Выделение для ссылок внутри текста (условия, политика). */
fun linkStyle() = SpanStyle(color = Brand.Yellow, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline)

/**
 * Поле для кода из нескольких цифр: одна ячейка на цифру, активная подсвечена жёлтым.
 * Внутри обычное текстовое поле, поэтому работают вставка из буфера и клавиатура.
 */
@Composable
fun CodeBoxes(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = 6,
    isError: Boolean = false,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val interFamily = interFontFamily()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val requester = remember { FocusRequester() }
    val shape = RoundedCornerShape(14.dp)

    LaunchedEffect(Unit) {
        try {
            requester.requestFocus()
        } catch (_: Exception) {
        }
    }

    BasicTextField(
        value = value,
        onValueChange = { new -> onValueChange(new.filter { it.isDigit() }.take(length)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = keyboardActions,
        interactionSource = interaction,
        cursorBrush = SolidColor(Color.Transparent),
        textStyle = TextStyle(color = Color.Transparent),
        modifier = modifier.fillMaxWidth().focusRequester(requester),
        decorationBox = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(length) { index ->
                    val isActive = focused && index == value.length.coerceAtMost(length - 1)
                    val borderColor by animateColorAsState(
                        targetValue = when {
                            isError -> Brand.Error
                            isActive -> Brand.Yellow
                            else -> Color.White.copy(alpha = 0.28f)
                        },
                        animationSpec = tween(140),
                        label = "codeBorder"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp)
                            .clip(shape)
                            .background(Color(0xFF2A2590).copy(alpha = if (isActive) 0.70f else 0.55f))
                            .border(if (isActive || isError) 1.5.dp else 1.dp, borderColor, shape),
                        contentAlignment = Alignment.Center
                    ) {
                        val digit = value.getOrNull(index)
                        if (digit != null) {
                            Text(
                                text = digit.toString(),
                                fontFamily = interFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 24.sp,
                                color = Color.White
                            )
                        } else if (isActive) {
                            Box(modifier = Modifier.width(2.dp).height(26.dp).background(Brand.Yellow))
                        }
                    }
                }
            }
        }
    )
}

/** Маленькие часы для таймера кода. */
@Composable
fun ClockIcon(color: Color = Brand.Yellow, modifier: Modifier = Modifier.size(18.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = strokeFor(w * 0.09f)
        drawCircle(color = color, radius = w * 0.42f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
        drawLine(color, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.27f), w * 0.09f, StrokeCap.Round)
        drawLine(color, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.66f, h * 0.58f), w * 0.09f, StrokeCap.Round)
    }
}

private val RayColor = Color(0xFFFFE9A8)

/** Короткий лучик-искорка вокруг значка. */
private fun DrawScope.ray(cx: Float, cy: Float, dx: Float, dy: Float, length: Float, width: Float) {
    val norm = sqrt(dx * dx + dy * dy)
    val ux = dx / norm
    val uy = dy / norm
    drawLine(RayColor, Offset(cx, cy), Offset(cx + ux * length, cy + uy * length), width, StrokeCap.Round)
}

/** Большое письмо с фиолетовым значком «@» и искорками вокруг. */
@Composable
fun EmailHeroIcon(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width = 176.dp, height = 132.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rw = w * 0.022f

            ray(w * 0.11f, h * 0.30f, -1f, -0.45f, w * 0.07f, rw)
            ray(w * 0.09f, h * 0.46f, -1f, 0.55f, w * 0.07f, rw)
            ray(w * 0.93f, h * 0.15f, 1f, -0.35f, w * 0.06f, rw)
            ray(w * 0.95f, h * 0.31f, 1f, 0.35f, w * 0.06f, rw)
            ray(w * 0.88f, h * 0.54f, 0.8f, 0.9f, w * 0.06f, rw)

            val bx = w * 0.16f
            val by = h * 0.32f
            val bw = w * 0.60f
            val bh = h * 0.56f

            rotate(degrees = -9f, pivot = Offset(bx + bw / 2f, by + bh / 2f)) {
                // тёплое свечение под письмом
                drawRoundRect(
                    color = Color(0x55FFB020),
                    topLeft = Offset(bx - 6f, by + 10f),
                    size = Size(bw + 12f, bh),
                    cornerRadius = CornerRadius(bw * 0.1f)
                )
                // корпус
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFD84D), Color(0xFFFFA21F)),
                        startY = by,
                        endY = by + bh
                    ),
                    topLeft = Offset(bx, by),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(bw * 0.09f)
                )
                // нижние складки
                val folds = Path().apply {
                    moveTo(bx + bw * 0.03f, by + bh * 0.97f)
                    lineTo(bx + bw * 0.42f, by + bh * 0.52f)
                    moveTo(bx + bw * 0.97f, by + bh * 0.97f)
                    lineTo(bx + bw * 0.58f, by + bh * 0.52f)
                }
                drawPath(folds, color = Color(0x80E8891C), style = strokeFor(bw * 0.025f))
                // клапан
                val flap = Path().apply {
                    moveTo(bx, by + bh * 0.05f)
                    lineTo(bx + bw * 0.5f, by + bh * 0.62f)
                    lineTo(bx + bw, by + bh * 0.05f)
                    close()
                }
                drawPath(
                    flap,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFFEE9A), Color(0xFFFFC94A)),
                        startY = by,
                        endY = by + bh * 0.62f
                    )
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.45f),
                    topLeft = Offset(bx, by),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(bw * 0.09f),
                    style = Stroke(width = 2f)
                )
            }
        }
        Box(
            modifier = Modifier
                .offset(x = (176f * 0.74f - 26f).dp, y = (132f * 0.20f - 26f).dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF8C6CF2), Color(0xFF4B36C9))))
                .border(2.dp, Color.White.copy(alpha = 0.55f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("@", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = interFontFamily())
        }
    }
}

/** Синяя табличка с четырьмя точками и жёлтый щит с галочкой, вокруг искорки. */
@Composable
fun ShieldHeroIcon(modifier: Modifier = Modifier) {
    Box(modifier = modifier.size(width = 184.dp, height = 116.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val rw = w * 0.02f

            ray(w * 0.10f, h * 0.20f, -0.7f, -1f, h * 0.12f, rw)
            ray(w * 0.18f, h * 0.10f, -0.15f, -1f, h * 0.12f, rw)
            ray(w * 0.90f, h * 0.28f, 1f, -0.25f, w * 0.06f, rw)
            ray(w * 0.93f, h * 0.45f, 1f, 0.05f, w * 0.06f, rw)

            val bx = w * 0.14f
            val by = h * 0.28f
            val bw = w * 0.64f
            val bh = h * 0.46f

            rotate(degrees = -4f, pivot = Offset(bx + bw / 2f, by + bh / 2f)) {
                drawRoundRect(
                    color = Color(0x553A4BFF),
                    topLeft = Offset(bx - 6f, by + 10f),
                    size = Size(bw + 12f, bh),
                    cornerRadius = CornerRadius(bh * 0.3f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF5A6BFF), Color(0xFF2B2AA8)),
                        startY = by,
                        endY = by + bh
                    ),
                    topLeft = Offset(bx, by),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(bh * 0.3f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.4f),
                    topLeft = Offset(bx, by),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(bh * 0.3f),
                    style = Stroke(width = 2f)
                )
                for (i in 0 until 4) {
                    drawCircle(
                        color = Color.White,
                        radius = bh * 0.085f,
                        center = Offset(bx + bw * (0.22f + i * 0.19f), by + bh * 0.5f)
                    )
                }
            }

            // щит
            val sx = w * 0.64f
            val sy = h * 0.46f
            val sw = w * 0.20f
            val sh = h * 0.46f
            val shield = Path().apply {
                moveTo(sx + sw * 0.5f, sy)
                lineTo(sx + sw * 0.96f, sy + sh * 0.16f)
                lineTo(sx + sw * 0.96f, sy + sh * 0.52f)
                quadraticTo(sx + sw * 0.96f, sy + sh * 0.86f, sx + sw * 0.5f, sy + sh)
                quadraticTo(sx + sw * 0.04f, sy + sh * 0.86f, sx + sw * 0.04f, sy + sh * 0.52f)
                lineTo(sx + sw * 0.04f, sy + sh * 0.16f)
                close()
            }
            drawPath(shield, brush = Brush.verticalGradient(listOf(Color(0xFFFFD84D), Color(0xFFFF9F1C)), startY = sy, endY = sy + sh))
            drawPath(shield, color = Color(0xCCFFFFFF), style = Stroke(width = 2.5f, join = StrokeJoin.Round))
            val tick = Path().apply {
                moveTo(sx + sw * 0.27f, sy + sh * 0.50f)
                lineTo(sx + sw * 0.45f, sy + sh * 0.67f)
                lineTo(sx + sw * 0.75f, sy + sh * 0.34f)
            }
            drawPath(tick, color = Color.White, style = strokeFor(sw * 0.11f))
        }
    }
}

/** Маленькое жёлтое письмо для карточки с адресом. */
@Composable
fun MiniEnvelopeIcon(modifier: Modifier = Modifier.size(width = 44.dp, height = 34.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFFFD84D), Color(0xFFFFA21F))),
            cornerRadius = CornerRadius(w * 0.14f)
        )
        val flap = Path().apply {
            moveTo(w * 0.06f, h * 0.12f)
            lineTo(w * 0.5f, h * 0.58f)
            lineTo(w * 0.94f, h * 0.12f)
        }
        drawPath(flap, color = Color(0xFFD9811A), style = strokeFor(w * 0.06f))
    }
}

/**
 * Карточка «Проверьте почту»: адрес и строка «Не пришёл код? Отправить повторно».
 * waitText: если не null, вместо ссылки показывается текст ожидания (кулдаун повторной отправки).
 */
@Composable
fun EmailInfoCard(
    email: String,
    noCodeText: String,
    resendText: String,
    onResend: () -> Unit,
    resendEnabled: Boolean,
    waitText: String?,
    modifier: Modifier = Modifier
) {
    val interFamily = interFontFamily()
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF2A2590).copy(alpha = 0.55f))
            .border(1.dp, Color.White.copy(alpha = 0.30f), shape)
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniEnvelopeIcon()
            Text(
                text = email,
                fontFamily = interFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 14.dp)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (waitText != null) {
                Text(
                    text = waitText,
                    fontFamily = interFamily,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = noCodeText,
                    fontFamily = interFamily,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.78f)
                )
                LinkText(text = resendText, onClick = onResend, enabled = resendEnabled)
            }
        }
    }
}

/** Текстовая ссылка (жёлтая) для вторичных действий: «Отправить код снова» и т. п. */
@Composable
fun LinkText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = Brand.Yellow
) {
    Text(
        text = text,
        fontFamily = interFontFamily(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = if (enabled) color else color.copy(alpha = 0.5f),
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    )
}

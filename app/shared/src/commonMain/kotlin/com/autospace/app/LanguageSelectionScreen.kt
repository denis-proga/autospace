package com.autospace.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import autospace.app.shared.generated.resources.Res
import autospace.app.shared.generated.resources.autospace_logo
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource

/**
 * Первый экран: логотип, название и выбор языка.
 * Названия языков написаны на самих языках, поэтому отдельные заголовки не нужны.
 * Широкий экран: пять карточек в ряд. Узкий (телефон): список строк.
 */
@Composable
fun LanguageSelectionScreen(onLanguageSelected: (Language) -> Unit) {
    val interFamily = interFontFamily()

    var selected by remember { mutableStateOf<Language?>(null) }
    val locked = selected != null

    LaunchedEffect(selected) {
        val chosen = selected
        if (chosen != null) {
            delay(380)
            onLanguageSelected(chosen)
        }
    }

    val headerAppear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        headerAppear.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SceneBackground()

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRows = maxWidth < 720.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(if (useRows) 20.dp else 36.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.graphicsLayer {
                        alpha = headerAppear.value
                        translationY = (1f - headerAppear.value) * -24f
                    }
                ) {
                    Image(
                        painter = painterResource(Res.drawable.autospace_logo),
                        contentDescription = "AutoSpace",
                        modifier = Modifier.size(if (useRows) 128.dp else 112.dp)
                    )

                    if (!useRows) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(color = Color.White)) { append("Auto") }
                                withStyle(SpanStyle(color = Brand.Yellow)) { append("Space") }
                            },
                            fontFamily = interFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 36.sp,
                            modifier = Modifier.padding(top = 14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(if (useRows) 32.dp else 48.dp))

                if (useRows) {
                    Column(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Language.entries.forEachIndexed { index, language ->
                            LanguageCard(
                                language = language,
                                asRow = true,
                                index = index,
                                isSelected = selected == language,
                                dimmed = locked && selected != language,
                                enabled = !locked,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { selected = language }
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().widthIn(max = 940.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Language.entries.forEachIndexed { index, language ->
                            LanguageCard(
                                language = language,
                                asRow = false,
                                index = index,
                                isSelected = selected == language,
                                dimmed = locked && selected != language,
                                enabled = !locked,
                                modifier = Modifier.weight(1f),
                                onClick = { selected = language }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageCard(
    language: Language,
    asRow: Boolean,
    index: Int,
    isSelected: Boolean,
    dimmed: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interFamily = interFontFamily()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val active = hovered || pressed || isSelected

    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.97f
            hovered || isSelected -> 1.035f
            else -> 1f
        },
        animationSpec = tween(150),
        label = "cardScale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (active) Brand.Yellow else Color.White.copy(alpha = 0.18f),
        animationSpec = tween(180),
        label = "cardBorder"
    )
    val glow by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(220),
        label = "cardGlow"
    )
    val topColor by animateColorAsState(
        targetValue = if (active) Color(0xFF2A2478).copy(alpha = 0.78f) else Color(0xFF15114A).copy(alpha = 0.58f),
        animationSpec = tween(200),
        label = "cardTop"
    )
    val bottomColor by animateColorAsState(
        targetValue = if (active) Color(0xFF16124F).copy(alpha = 0.82f) else Color(0xFF0A0830).copy(alpha = 0.66f),
        animationSpec = tween(200),
        label = "cardBottom"
    )
    val dim by animateFloatAsState(
        targetValue = if (dimmed) 0.35f else 1f,
        animationSpec = tween(250),
        label = "cardDim"
    )

    // карточки появляются по очереди
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(160L + index * 90L)
        appear.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }

    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = appear.value * dim
                translationY = (1f - appear.value) * 40f
            }
            .drawBehind {
                // мягкое жёлтое свечение вокруг активной карточки
                for (k in 1..4) {
                    val grow = k * 5f
                    drawRoundRect(
                        color = Brand.Yellow.copy(alpha = 0.07f * glow),
                        topLeft = Offset(-grow, -grow),
                        size = Size(size.width + grow * 2f, size.height + grow * 2f),
                        cornerRadius = CornerRadius(22.dp.toPx() + grow)
                    )
                }
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
            .border(if (active) 1.5.dp else 1.dp, borderColor, shape)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled) { onClick() }
    ) {
        if (asRow) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlagBadge(language = language, diameter = 44.dp)
                Text(
                    text = language.displayName,
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.padding(start = 16.dp).weight(1f)
                )
                Text(
                    text = "›",
                    fontFamily = interFamily,
                    fontSize = 28.sp,
                    color = Brand.Yellow
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth().height(172.dp).padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                FlagBadge(language = language, diameter = 64.dp)
                Text(
                    text = language.displayName,
                    fontFamily = interFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

/** Круглый флаг, нарисованный кодом: не весит ничего и остаётся чётким в любом размере. */
@Composable
fun FlagBadge(language: Language, diameter: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .size(diameter)
            .clip(CircleShape)
            .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawFlag(language)
            // мягкий блик, чтобы флаг выглядел объёмным
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(size.width * 0.32f, size.height * 0.26f),
                    radius = size.width * 0.75f
                )
            )
        }
    }
}

private fun DrawScope.drawFlag(language: Language) {
    when (language) {
        Language.RUSSIAN -> stripes(
            listOf(Color(0xFFFFFFFF), Color(0xFF0039A6), Color(0xFFD52B1E)), listOf(1f, 1f, 1f)
        )
        Language.GERMAN -> stripes(
            listOf(Color(0xFF000000), Color(0xFFDD0000), Color(0xFFFFCE00)), listOf(1f, 1f, 1f)
        )
        Language.UKRAINIAN -> stripes(
            listOf(Color(0xFF0057B7), Color(0xFFFFD700)), listOf(1f, 1f)
        )
        Language.SPANISH -> stripes(
            listOf(Color(0xFFAA151B), Color(0xFFF1BF00), Color(0xFFAA151B)), listOf(1f, 2f, 1f)
        )
        Language.ENGLISH -> unionJack()
    }
}

private fun DrawScope.stripes(colors: List<Color>, weights: List<Float>) {
    val total = weights.sum()
    var y = 0f
    colors.forEachIndexed { i, color ->
        val stripeHeight = size.height * weights[i] / total
        // +1 пиксель, чтобы между полосами не было щелей
        drawRect(color = color, topLeft = Offset(0f, y), size = Size(size.width, stripeHeight + 1f))
        y += stripeHeight
    }
}

private fun DrawScope.unionJack() {
    val w = size.width
    val h = size.height
    val red = Color(0xFFC8102E)

    drawRect(color = Color(0xFF012169))

    // диагонали: белые широкие, поверх них красные узкие
    drawLine(Color.White, Offset(0f, 0f), Offset(w, h), w * 0.22f)
    drawLine(Color.White, Offset(w, 0f), Offset(0f, h), w * 0.22f)
    drawLine(red, Offset(0f, 0f), Offset(w, h), w * 0.07f)
    drawLine(red, Offset(w, 0f), Offset(0f, h), w * 0.07f)

    // крест: белая окантовка и красная середина
    drawRect(Color.White, topLeft = Offset(w * 0.35f, 0f), size = Size(w * 0.30f, h))
    drawRect(Color.White, topLeft = Offset(0f, h * 0.35f), size = Size(w, h * 0.30f))
    drawRect(red, topLeft = Offset(w * 0.41f, 0f), size = Size(w * 0.18f, h))
    drawRect(red, topLeft = Offset(0f, h * 0.41f), size = Size(w, h * 0.18f))
}

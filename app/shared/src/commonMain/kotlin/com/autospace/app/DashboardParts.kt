package com.autospace.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import autospace.app.shared.generated.resources.Res
import autospace.app.shared.generated.resources.scene_landscape
import org.jetbrains.compose.resources.painterResource

/**
 * Значения, которые пока нарисованы для примера: логики под ними ещё нет (уровни, серия дней, рейтинг,
 * вопрос дня, ошибки, достижения). Когда логика появится, блоки подключатся к реальным данным.
 * SHOW = false убирает все такие блоки с экрана, например для релиза без них.
 */
object PreviewData {
    const val SHOW = true
    const val RANK = "rank"
    const val LEVEL = 4
    const val XP = 360
    const val XP_NEXT = 500
    const val STREAK_DAYS = 5
    const val CONTINUE_TEST = 7
    const val CONTINUE_QUESTION = 12
    const val MISTAKES = 12
    const val MY_PLACE = 12
    val TOP = listOf(Triple("AlexDrive", 978, 1), Triple("FastRoad", 942, 2), Triple("KiraTest", 901, 3))
}

/** Цвета главной страницы: глубокий синий с яркими статусами. */
internal object Dash {
    val Bg0 = Color(0xFF070B33)
    val Bg1 = Color(0xFF0B1150)
    val PanelTop = Color(0xFF131E70)
    val PanelBottom = Color(0xFF0D1458)
    val PanelBorder = Color(0xFF4458E0).copy(alpha = 0.40f)
    val Text2 = Color(0xFFA9B6FF)
    val Green = Color(0xFF2BD18A)
    val Red = Color(0xFFFF4D5E)
    val Blue = Color(0xFF3D8BFF)
    val Ring = Color(0xFF7088FF)
}

internal enum class DashLayout { Wide, Large, Medium, Compact }

internal enum class TileState { NotStarted, Passed, Failed, ExamDone, InProgress, Locked }

/** Панель-карточка в стиле страницы. */
@Composable
internal fun DashPanel(
    modifier: Modifier = Modifier,
    padding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Dash.PanelTop.copy(alpha = 0.92f), Dash.PanelBottom.copy(alpha = 0.95f))))
            .border(1.dp, Dash.PanelBorder, shape)
            .padding(padding),
        content = content
    )
}

/** Фон главной страницы. */
@Composable
internal fun DashboardBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Dash.Bg1, Dash.Bg0)))
    )
}

@Composable
internal fun AvatarCircle(initial: String, size: Dp = 40.dp, ring: Color = Color.White.copy(alpha = 0.35f)) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF7A5CFF), Color(0xFF2B3FD0))))
            .border(1.5.dp, ring, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.take(1).uppercase(),
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.42f).sp,
            color = Color.White
        )
    }
}

/** Плитка теста: значок статуса, номер и подпись. */
@Composable
internal fun TestTile(
    number: Int,
    label: String,
    state: TileState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dense: Boolean = false
) {
    val interFamily = interFontFamily()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val corner = if (dense) 10.dp else 16.dp
    val iconSize = if (dense) 12.dp else 22.dp
    val shape = RoundedCornerShape(corner)

    val (top, bottom, edge) = when (state) {
        TileState.Passed -> Triple(Color(0xFF0F4D43), Color(0xFF0A3A36), Dash.Green)
        TileState.Failed -> Triple(Color(0xFF55152F), Color(0xFF3E1030), Dash.Red)
        TileState.ExamDone -> Triple(Color(0xFF16358A), Color(0xFF102668), Dash.Blue)
        TileState.Locked -> Triple(Color(0xFF111A58), Color(0xFF0B1244), Dash.Ring.copy(alpha = 0.25f))
        else -> Triple(Color(0xFF16226F), Color(0xFF0F1752), Dash.Ring.copy(alpha = 0.40f))
    }
    val borderColor by animateColorAsState(
        targetValue = if (hovered && state != TileState.Locked) Brand.Yellow else edge,
        animationSpec = tween(150),
        label = "tileBorder"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.96f
            hovered && state != TileState.Locked -> 1.04f
            else -> 1f
        },
        animationSpec = tween(130),
        label = "tileScale"
    )

    Box(
        modifier = modifier
            .height(if (dense) 48.dp else 84.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .then(
                if (state == TileState.InProgress) {
                    Modifier.drawBehind {
                        drawRoundRect(
                            color = Brand.Yellow,
                            cornerRadius = CornerRadius(corner.toPx()),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 9f)))
                        )
                    }
                } else {
                    Modifier.border(1.dp, borderColor, shape)
                }
            )
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            when (state) {
                TileState.Passed -> StatusDot(Dash.Green, check = true, size = iconSize)
                TileState.Failed -> StatusDot(Dash.Red, check = false, size = iconSize)
                TileState.ExamDone -> StatusDot(Dash.Blue, check = true, size = iconSize)
                TileState.InProgress -> RingIcon(Dash.Ring, size = iconSize, progress = 0.28f)
                TileState.Locked -> LockIcon(color = Dash.Text2, modifier = Modifier.size(iconSize))
                TileState.NotStarted -> RingIcon(Dash.Ring, size = iconSize)
            }
            Text(
                text = number.toString(),
                fontFamily = interFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (dense) 12.sp else 18.sp,
                lineHeight = if (dense) 14.sp else 22.sp,
                color = Color.White,
                modifier = Modifier.padding(top = if (dense) 1.dp else 2.dp)
            )
            Text(
                text = label,
                fontFamily = interFamily,
                fontSize = if (dense) 9.5.sp else 11.sp,
                lineHeight = if (dense) 11.sp else 14.sp,
                color = Dash.Text2,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

/**
 * Сетка плиток: число столбцов считается по ширине, последний ряд добивается пустыми местами.
 * visibleRows: сколько рядов видно сразу, остальные прокручиваются внутри контейнера
 * (следующий ряд выглядывает наполовину, чтобы было понятно, что ниже есть ещё).
 */
@Composable
internal fun <T> AdaptiveGrid(
    items: List<T>,
    minTile: Dp,
    maxColumns: Int,
    gap: Dp = 10.dp,
    visibleRows: Int = Int.MAX_VALUE,
    rowHeight: Dp = 84.dp,
    tile: @Composable (item: T, modifier: Modifier) -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = ((maxWidth + gap) / (minTile + gap)).toInt().coerceIn(2, maxColumns)
        val rows = items.chunked(columns)
        val scrolls = rows.size > visibleRows
        val containerHeight = if (scrolls) rowHeight * (visibleRows + 0.4f) + gap * visibleRows else Dp.Unspecified

        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = (if (scrolls) Modifier.height(containerHeight).verticalScroll(rememberScrollState()) else Modifier)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                rows.forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        rowItems.forEach { item -> tile(item, Modifier.weight(1f)) }
                        repeat(columns - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
            if (scrolls) {
                // мягкое затемнение внизу подсказывает, что список прокручивается
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Dash.PanelBottom.copy(alpha = 0.9f))))
                )
            }
        }
    }
}

/** Переключатель «Обучение | Экзамен»: выбранный пункт жёлтый. */
@Composable
internal fun <T> ModeToggle(
    options: List<T>,
    selected: T,
    labelFor: (T) -> String,
    onSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    dense: Boolean = false
) {
    val interFamily = interFontFamily()
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF0A1244))
            .border(1.dp, Dash.PanelBorder, shape)
            .padding(if (dense) 2.dp else 3.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val bg by animateColorAsState(
                targetValue = if (isSelected) Brand.Yellow else Color.Transparent,
                animationSpec = tween(200),
                label = "modeBg"
            )
            Box(
                modifier = Modifier
                    .clip(shape)
                    .background(bg)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelected(option) }
                    .padding(horizontal = if (dense) 13.dp else 22.dp, vertical = if (dense) 5.dp else 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelFor(option),
                    fontFamily = interFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (dense) 10.5.sp else 14.sp,
                    color = if (isSelected) Color(0xFF1B1540) else Color.White
                )
            }
        }
    }
}

/** Кнопка в панелях: синяя контурная, со стрелкой. */
@Composable
internal fun PanelButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, yellow: Boolean = false, dense: Boolean = false) {
    val interFamily = interFontFamily()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(if (dense) 26.dp else 44.dp)
            .clip(shape)
            .background(
                if (yellow) Brush.verticalGradient(listOf(Color(0xFFFFD84D), Color(0xFFFFB627)))
                else Brush.verticalGradient(listOf(Color(0xFF2A3FC7).copy(alpha = if (hovered) 0.9f else 0.7f), Color(0xFF1B2A9C).copy(alpha = if (hovered) 0.9f else 0.7f)))
            )
            .then(if (yellow) Modifier else Modifier.border(1.dp, Dash.Ring.copy(alpha = 0.6f), shape))
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = if (dense) 11.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            fontFamily = interFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (dense) 10.5.sp else 15.sp,
            color = if (yellow) Color(0xFF1B1540) else Color.White
        )
        ChevronIcon(color = if (yellow) Color(0xFF1B1540) else Color.White, modifier = Modifier.padding(start = if (dense) 3.dp else 6.dp).size(if (dense) 12.dp else 18.dp))
    }
}

/** Чип «Доступ активен»: цвет и текст зависят от лицензии. */
@Composable
internal fun AccessChip(license: LicenseInfoDto?, onClick: () -> Unit, modifier: Modifier = Modifier, dense: Boolean = false) {
    if (license == null) return
    val strings = LocalStrings.current
    val interFamily = interFontFamily()

    val title: String
    val subtitle: String
    val accent: Color
    when (license.status) {
        "ACTIVE" -> { title = strings.dashAccessActive; subtitle = strings.dashDaysLeft(license.daysLeft ?: 0); accent = Dash.Green }
        "FRIEND" -> { title = strings.dashAccessActive; subtitle = strings.dashNoLimit; accent = Dash.Green }
        "DEMO" -> {
            title = strings.dashAccessDemo
            subtitle = strings.mainMenuDemoLeft((license.demoLimit - license.demoTests.size).coerceAtLeast(0), license.demoLimit)
            accent = Brand.Yellow
        }
        "EXPIRED" -> { title = strings.mainMenuExpired; subtitle = strings.mainMenuTariffs; accent = Dash.Red }
        else -> return
    }

    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(accent.copy(alpha = 0.14f))
            .border(if (dense) 1.dp else 1.5.dp, accent.copy(alpha = 0.85f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = if (dense) 10.dp else 14.dp, vertical = if (dense) 5.dp else 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CalendarIcon(color = accent, size = if (dense) 18.dp else 26.dp)
        Column(modifier = Modifier.padding(start = if (dense) 7.dp else 10.dp, end = if (dense) 8.dp else 12.dp)) {
            Text(text = title, fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = if (dense) 11.5.sp else 15.sp, color = Color.White, maxLines = 1, softWrap = false)
            Text(text = subtitle, fontFamily = interFamily, fontSize = if (dense) 9.5.sp else 12.sp, color = Color.White.copy(alpha = 0.8f), maxLines = 1, softWrap = false)
        }
        ChevronIcon(color = Color.White, modifier = Modifier.size(if (dense) 12.dp else 18.dp))
    }
}

/** Картинка сцены внутри панели с затемнением слева, чтобы текст читался. */
@Composable
internal fun SceneStrip(modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(Res.drawable.scene_landscape),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.BottomEnd,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xF00D1458), Color(0xB00D1458), Color(0x200D1458))
                    )
                )
        )
    }
}

/**
 * Главная кнопка карточки «Продолжить» для широких экранов: по ширине содержимого, слева,
 * с мягким жёлтым свечением и стрелкой в тёмном кружке. При наведении приподнимается и светится сильнее.
 */
@Composable
internal fun ContinueButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.97f
            hovered -> 1.04f
            else -> 1f
        },
        animationSpec = tween(140),
        label = "continueScale"
    )
    val glow by animateFloatAsState(if (hovered) 0.42f else 0.22f, tween(200), label = "continueGlow")
    val shape = RoundedCornerShape(50)

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .drawBehind {
                // свечение под кнопкой
                drawRoundRect(
                    color = Brand.Yellow.copy(alpha = glow),
                    topLeft = androidx.compose.ui.geometry.Offset(4f, size.height * 0.28f),
                    size = androidx.compose.ui.geometry.Size(size.width - 8f, size.height),
                    cornerRadius = CornerRadius(size.height / 2f)
                )
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFDE5E), Color(0xFFFFB627))))
            .border(1.dp, Color.White.copy(alpha = 0.45f), shape)
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(start = 24.dp, end = 7.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF1B1540),
            maxLines = 1,
            softWrap = false
        )
        Box(
            modifier = Modifier
                .padding(start = 16.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B1540)),
            contentAlignment = Alignment.Center
        ) {
            ChevronIcon(color = Brand.Yellow, modifier = Modifier.size(18.dp))
        }
    }
}

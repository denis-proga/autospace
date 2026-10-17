package com.autospace.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.util.date.GMTDate

/** Результат считается сданным от 90% (27 из 30), как на плитках главной страницы. */
private const val STATS_PASS_PERCENT = 90

private fun statsPercent(r: TestResultItemDto): Int =
    if (r.totalQuestions <= 0) 0 else r.correctCount * 100 / r.totalQuestions

private fun statsIsPass(r: TestResultItemDto): Boolean = statsPercent(r) >= STATS_PASS_PERCENT

private class StatsSummary(
    val testsPassed: Int,
    val testsTotal: Int,
    val avgPercent: Int,
    val correct: Int,
    val answered: Int,
    val examsPassed: Int,
    val examAttempts: Int
)

private fun statsSummarize(results: List<TestResultItemDto>): StatsSummary {
    val exams = results.filter { it.mode == "EXAM" }
    return StatsSummary(
        testsPassed = results.filter { statsIsPass(it) }.map { it.testNumber }.distinct().size,
        testsTotal = generateTestList().size,
        avgPercent = if (results.isEmpty()) 0 else results.sumOf { statsPercent(it) } / results.size,
        correct = results.sumOf { it.correctCount },
        answered = results.sumOf { it.totalQuestions },
        examsPassed = exams.count { statsIsPass(it) },
        examAttempts = exams.size
    )
}

private fun statsGroupDigits(n: Int): String {
    val s = n.toString()
    val sb = StringBuilder()
    s.forEachIndexed { i, c ->
        if (i > 0 && (s.length - i) % 3 == 0) sb.append('\u00A0')
        sb.append(c)
    }
    return sb.toString()
}

private fun statsTwo(n: Int): String = n.toString().padStart(2, '0')

private fun statsDayLabel(epochMillis: Long): String {
    val d = GMTDate(epochMillis)
    return "${statsTwo(d.dayOfMonth)}.${statsTwo(d.month.ordinal + 1)}"
}

private fun statsFullDate(epochMillis: Long): String {
    val d = GMTDate(epochMillis)
    return "${statsTwo(d.dayOfMonth)}.${statsTwo(d.month.ordinal + 1)}.${d.year} ${statsTwo(d.hours)}:${statsTwo(d.minutes)}"
}

/** Страница статистики. results приходят от новых к старым. */
@Composable
fun StatsScreen(
    isLoading: Boolean,
    results: List<TestResultItemDto>,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val st = statsStrings(strings)

    ProvideTextStyle(
        LocalTextStyle.current.copy(lineHeight = TextUnit.Unspecified, letterSpacing = TextUnit.Unspecified)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wide = maxWidth >= 900.dp
            val pad = if (wide) 28.dp else 14.dp

            SceneBackground(dim = 0f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color(0x330D0B45),
                                0.30f to Color(0xB30D0B45),
                                0.55f to Color(0xE60D0B45),
                                1.00f to Color(0xF20D0B45)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = pad, vertical = if (wide) 20.dp else 12.dp)
            ) {
                StatsBackPill(text = strings.supportBack, onClick = onBack)
                Spacer(Modifier.height(if (wide) 22.dp else 14.dp))

                Text(
                    text = strings.statsTitle,
                    style = TextStyle(
                        brush = Brush.horizontalGradient(
                            0.0f to Color.White,
                            0.55f to Color.White,
                            0.75f to Brand.Yellow,
                            1.0f to Brand.Yellow
                        )
                    ),
                    fontFamily = interFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = if (wide) 46.sp else 30.sp
                )
                Text(
                    text = st.subtitle,
                    fontFamily = interFontFamily(),
                    fontSize = if (wide) 15.sp else 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 6.dp, bottom = if (wide) 22.dp else 14.dp)
                )

                when {
                    isLoading -> Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Brand.Yellow)
                    }

                    results.isEmpty() -> DashPanel {
                        Text(
                            text = strings.statsEmpty,
                            fontFamily = interFontFamily(),
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    else -> StatsContent(results = results, st = st, strings = strings, wide = wide)
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

private class StatCardData(
    val kind: Int,
    val color: Color,
    val label: String,
    val value: String,
    val sub: String?
)

@Composable
private fun StatsContent(results: List<TestResultItemDto>, st: StatsStrings, strings: AppStrings, wide: Boolean) {
    val summary = remember(results) { statsSummarize(results) }
    val cards = listOf(
        StatCardData(0, Brand.Yellow, st.cardTests, summary.testsPassed.toString(), st.ofTotal(summary.testsTotal.toString())),
        StatCardData(1, Dash.Green, st.cardAvg, "${summary.avgPercent}%", null),
        StatCardData(2, Brand.Yellow, st.cardCorrect, statsGroupDigits(summary.correct), st.ofTotal(statsGroupDigits(summary.answered))),
        StatCardData(
            3, Dash.Blue, st.cardExams, summary.examsPassed.toString(),
            if (summary.examAttempts > 0) st.attemptsCount(summary.examAttempts) else null
        )
    )

    if (wide) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            cards.forEach { StatCard(it, wide = true, modifier = Modifier.weight(1f).fillMaxHeight()) }
        }
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(modifier = Modifier.weight(2f)) { ResultsChart(results, st, wide = true) }
            Box(modifier = Modifier.weight(1f)) { AttemptsPanel(results, st, strings, wide = true) }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(cards[0], wide = false, modifier = Modifier.weight(1f).fillMaxHeight())
                StatCard(cards[1], wide = false, modifier = Modifier.weight(1f).fillMaxHeight())
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(cards[2], wide = false, modifier = Modifier.weight(1f).fillMaxHeight())
                StatCard(cards[3], wide = false, modifier = Modifier.weight(1f).fillMaxHeight())
            }
            ResultsChart(results, st, wide = false)
            AttemptsPanel(results, st, strings, wide = false)
        }
    }
}

@Composable
private fun StatCard(d: StatCardData, wide: Boolean, modifier: Modifier = Modifier) {
    DashPanel(modifier = modifier, padding = if (wide) 16.dp else 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(if (wide) 46.dp else 36.dp)
                    .clip(CircleShape)
                    .background(d.color.copy(alpha = 0.16f))
                    .border(1.dp, d.color.copy(alpha = 0.7f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                StatGlyph(kind = d.kind, color = d.color, glyphSize = if (wide) 22.dp else 18.dp)
            }
            Spacer(Modifier.width(if (wide) 14.dp else 10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = d.label,
                    fontFamily = interFontFamily(),
                    fontSize = if (wide) 13.sp else 11.sp,
                    color = Dash.Text2,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (wide) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = d.value,
                            fontFamily = interFontFamily(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = Color.White
                        )
                        if (d.sub != null) {
                            Text(
                                text = d.sub,
                                fontFamily = interFontFamily(),
                                fontSize = 12.sp,
                                color = Dash.Text2,
                                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                            )
                        }
                    }
                } else {
                    Text(
                        text = d.value,
                        fontFamily = interFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                    if (d.sub != null) {
                        Text(
                            text = d.sub,
                            fontFamily = interFontFamily(),
                            fontSize = 11.sp,
                            color = Dash.Text2
                        )
                    }
                }
            }
        }
    }
}

/** Значки карточек нарисованы вручную: 0 — документ, 1 — мишень, 2 — галочка в круге, 3 — кубок. */
@Composable
private fun StatGlyph(kind: Int, color: Color, glyphSize: Dp) {
    Canvas(modifier = Modifier.size(glyphSize)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            0 -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.18f, h * 0.08f),
                    size = Size(w * 0.64f, h * 0.84f),
                    cornerRadius = CornerRadius(w * 0.1f),
                    style = stroke
                )
                for (i in 0..2) {
                    val y = h * (0.34f + i * 0.17f)
                    drawLine(color = color, start = Offset(w * 0.32f, y), end = Offset(w * 0.68f, y), strokeWidth = sw, cap = StrokeCap.Round)
                }
            }

            1 -> {
                drawCircle(color = color, radius = w * 0.42f, style = stroke)
                drawCircle(color = color, radius = w * 0.24f, style = stroke)
                drawCircle(color = color, radius = w * 0.07f)
            }

            2 -> {
                drawCircle(color = color, radius = w * 0.42f, style = stroke)
                val tick = Path().apply {
                    moveTo(w * 0.30f, h * 0.52f)
                    lineTo(w * 0.44f, h * 0.66f)
                    lineTo(w * 0.70f, h * 0.36f)
                }
                drawPath(path = tick, color = color, style = stroke)
            }

            else -> {
                val cup = Path().apply {
                    moveTo(w * 0.26f, h * 0.12f)
                    lineTo(w * 0.74f, h * 0.12f)
                    lineTo(w * 0.68f, h * 0.45f)
                    quadraticTo(w * 0.5f, h * 0.64f, w * 0.32f, h * 0.45f)
                    close()
                }
                drawPath(path = cup, color = color, style = stroke)
                drawLine(color = color, start = Offset(w * 0.5f, h * 0.58f), end = Offset(w * 0.5f, h * 0.80f), strokeWidth = sw, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(w * 0.34f, h * 0.86f), end = Offset(w * 0.66f, h * 0.86f), strokeWidth = sw, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun ResultsChart(results: List<TestResultItemDto>, st: StatsStrings, wide: Boolean) {
    // последние 14 попыток, слева старые, справа новые
    val attempts = remember(results) { results.take(14).reversed() }

    DashPanel(padding = if (wide) 20.dp else 14.dp) {
        Text(
            text = st.chartTitle,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = if (wide) 20.sp else 17.sp,
            color = Color.White
        )
        Spacer(Modifier.height(14.dp))

        if (attempts.size < 2) {
            Text(
                text = st.chartEmpty,
                fontFamily = interFontFamily(),
                fontSize = 13.sp,
                color = Dash.Text2,
                modifier = Modifier.padding(vertical = 18.dp)
            )
        } else {
            ChartBody(attempts = attempts, st = st, wide = wide)
        }
    }
}

@Composable
private fun ChartBody(attempts: List<TestResultItemDto>, st: StatsStrings, wide: Boolean) {
    val n = attempts.size
    val chartHeight = if (wide) 230.dp else 150.dp
    val labelColumn = 42.dp
    val dotInner = Color(0xFF0D1458)

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val perCell = (maxWidth - labelColumn) / n
        val step = maxOf(1, kotlin.math.ceil(38f / perCell.value).toInt())

        Column {
            Row(modifier = Modifier.fillMaxWidth().height(chartHeight)) {
                Column(
                    modifier = Modifier.width(labelColumn - 8.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    listOf(100, 75, 50, 25, 0).forEach { v ->
                        Text(text = "$v%", fontFamily = interFontFamily(), fontSize = 10.sp, color = Dash.Text2)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val inset = 7.dp.toPx()
                        val innerH = this.size.height - 2 * inset
                        fun yOf(percent: Float): Float = inset + innerH * (1f - percent / 100f)
                        fun xOf(i: Int): Float = this.size.width * (i + 0.5f) / n

                        for (v in listOf(0, 25, 50, 75, 100)) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.10f),
                                start = Offset(0f, yOf(v.toFloat())),
                                end = Offset(this.size.width, yOf(v.toFloat())),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // порог сдачи
                        drawLine(
                            color = Dash.Green.copy(alpha = 0.9f),
                            start = Offset(0f, yOf(STATS_PASS_PERCENT.toFloat())),
                            end = Offset(this.size.width, yOf(STATS_PASS_PERCENT.toFloat())),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                        )

                        val points = attempts.map { statsPercent(it).toFloat() }
                        val line = Path()
                        val area = Path()
                        points.forEachIndexed { i, p ->
                            val x = xOf(i)
                            val y = yOf(p)
                            if (i == 0) {
                                line.moveTo(x, y)
                                area.moveTo(x, yOf(0f))
                                area.lineTo(x, y)
                            } else {
                                line.lineTo(x, y)
                                area.lineTo(x, y)
                            }
                        }
                        area.lineTo(xOf(n - 1), yOf(0f))
                        area.close()

                        drawPath(
                            path = area,
                            brush = Brush.verticalGradient(
                                colors = listOf(Brand.Yellow.copy(alpha = 0.32f), Brand.Yellow.copy(alpha = 0.02f)),
                                startY = yOf(100f),
                                endY = yOf(0f)
                            )
                        )
                        drawPath(
                            path = line,
                            color = Brand.Yellow,
                            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        points.forEachIndexed { i, p ->
                            val c = Offset(xOf(i), yOf(p))
                            drawCircle(color = Brand.Yellow, radius = 5.dp.toPx(), center = c)
                            drawCircle(color = dotInner, radius = 2.dp.toPx(), center = c)
                        }
                    }
                    Text(
                        text = st.threshold,
                        fontFamily = interFontFamily(),
                        fontSize = 10.sp,
                        color = Dash.Green,
                        modifier = Modifier.align(Alignment.TopEnd).padding(end = 4.dp, top = 1.dp)
                    )
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(start = labelColumn)) {
                attempts.forEachIndexed { i, r ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (i % step == 0) {
                            Text(
                                text = statsDayLabel(r.completedAt),
                                fontFamily = interFontFamily(),
                                fontSize = 10.sp,
                                color = Dash.Text2,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.wrapContentWidth(Alignment.CenterHorizontally, unbounded = true)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AttemptsPanel(results: List<TestResultItemDto>, st: StatsStrings, strings: AppStrings, wide: Boolean) {
    var showAll by remember { mutableStateOf(false) }
    val limit = if (wide) 5 else 3
    val shown = if (showAll) results.take(50) else results.take(limit)

    DashPanel(padding = if (wide) 20.dp else 14.dp) {
        Text(
            text = st.attemptsTitle,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = if (wide) 20.sp else 17.sp,
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            shown.forEach { StatsAttemptRow(it, strings) }
        }
        if (results.size > limit) {
            Text(
                text = if (showAll) st.collapse else st.showAll(minOf(results.size, 50)),
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = Brand.Yellow,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        showAll = !showAll
                    }
            )
        }
    }
}

@Composable
private fun StatsAttemptRow(r: TestResultItemDto, strings: AppStrings) {
    val pass = statsIsPass(r)
    val chipColor = if (pass) Dash.Green else Dash.Red
    val shape = RoundedCornerShape(14.dp)
    val modeLabel = if (r.mode == "EXAM") strings.commonExam else strings.commonLearning

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF0A0F45).copy(alpha = 0.65f))
            .border(1.dp, Dash.PanelBorder, shape)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${strings.commonTestWord} ${r.testNumber} · $modeLabel",
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statsFullDate(r.completedAt),
                fontFamily = interFontFamily(),
                fontSize = 11.sp,
                color = Dash.Text2
            )
        }
        Spacer(Modifier.width(8.dp))
        val chipShape = RoundedCornerShape(50)
        Box(
            modifier = Modifier
                .clip(chipShape)
                .background(chipColor.copy(alpha = 0.22f))
                .border(1.dp, chipColor.copy(alpha = 0.8f), chipShape)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${r.correctCount} / ${r.totalQuestions}",
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Color.White
            )
        }
    }
}

@Composable
private fun StatsBackPill(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(Color(0xFF14104A).copy(alpha = 0.78f))
            .border(1.dp, Dash.Ring.copy(alpha = 0.7f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(start = 10.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChevronIcon(color = Brand.Yellow, left = true, modifier = Modifier.size(18.dp))
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

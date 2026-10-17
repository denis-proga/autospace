package com.autospace.app

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun parseAnswers(raw: String): Map<Int, Int> {
    if (raw.isBlank()) return emptyMap()
    return raw.split(",").mapNotNull { pair ->
        val parts = pair.split(":")
        if (parts.size == 2) {
            val k = parts[0].toIntOrNull()
            val v = parts[1].toIntOrNull()
            if (k != null && v != null) k to v else null
        } else null
    }.toMap()
}

/** Статус вопроса на полоске прогресса: Answered — отвечен в режиме экзамена (верность не показываем). */
private enum class TestSegStatus { None, Correct, Wrong, Answered }

private enum class TestOptionState { Normal, Selected, Correct, Wrong }

@Composable
fun TestScreen(
    test: TestInfo,
    mode: TestMode,
    language: String,
    username: String,
    scope: CoroutineScope,
    onFinish: () -> Unit,
    onSaveResult: (correctCount: Int, total: Int, answers: String) -> Unit,
    onLicenseRequired: (code: String) -> Unit = {},
    onSessionExpired: () -> Unit = {}
) {
    val strings = LocalStrings.current
    val ui = testUiStrings(strings)
    val interFamily = interFontFamily()

    var questions by remember { mutableStateOf<List<Question>?>(null) }
    var loadErrorMessage by remember { mutableStateOf<String?>(null) }
    var showWakingHint by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(test.number, reloadTrigger) {
        questions = null
        loadErrorMessage = null
        try {
            val response = ApiClient.getQuestions(test.number, language)
            questions = response.questions.map { it.toQuestion() }
        } catch (e: Exception) {
            when (e) {
                is LicenseRequiredException -> onLicenseRequired(e.code)
                is SessionExpiredException -> onSessionExpired()
                else -> loadErrorMessage = friendlyServerErrorMessage(e, strings)
            }
        }
    }

    LaunchedEffect(questions, loadErrorMessage) {
        showWakingHint = false
        if (questions == null && loadErrorMessage == null) {
            delay(5000)
            showWakingHint = true
        }
    }

    val loadedQuestions = questions

    if (loadedQuestions == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            DashboardBackground()
            Column(
                modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val errorMessage = loadErrorMessage
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = Dash.Red,
                        textAlign = TextAlign.Center,
                        fontFamily = interFamily,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TestYellowButton(text = strings.testRetry, showFlag = false, onClick = { reloadTrigger++ })
                } else {
                    CircularProgressIndicator(color = Brand.Yellow)
                    if (showWakingHint) {
                        Text(
                            text = strings.commonWakingHint,
                            fontFamily = interFamily,
                            color = Dash.Text2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
                // верхней панели с кнопкой «Назад» на экране теста нет, поэтому выход нужен и здесь
                Spacer(Modifier.height(12.dp))
                TestOutlineButton(text = ui.goBack, modifier = Modifier.fillMaxWidth(), onClick = onFinish)
            }
        }
        return
    }

    var currentIndex by remember { mutableStateOf(0) }
    val answers = remember { mutableStateMapOf<Int, Int>() }
    var showExplanation by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }
    var showFinishConfirmation by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(30 * 60) }
    var progressChecked by remember { mutableStateOf(false) }

    LaunchedEffect(loadedQuestions) {
        if (!progressChecked) {
            try {
                val progress = ApiClient.getProgress(username, test.number, mode.name)
                if (progress.found) {
                    answers.putAll(parseAnswers(progress.answersData ?: ""))
                    currentIndex = (progress.currentIndex ?: 0).coerceIn(0, loadedQuestions.lastIndex)
                    if (mode == TestMode.EXAM && progress.secondsLeft != null) {
                        secondsLeft = progress.secondsLeft
                    }
                }
            } catch (e: Exception) {
            }
            progressChecked = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (!showResult) {
                val answersData = answers.entries.joinToString(",") { "${it.key}:${it.value}" }
                scope.launch {
                    try {
                        ApiClient.saveProgress(
                            SaveProgressRequestDto(
                                username = username,
                                testNumber = test.number,
                                mode = mode.name,
                                answersData = answersData,
                                currentIndex = currentIndex,
                                secondsLeft = if (mode == TestMode.EXAM) secondsLeft else null
                            )
                        )
                    } catch (e: Exception) {
                    }
                }
            }
        }
    }

    fun computeCorrectCount(): Int {
        return loadedQuestions.indices.count { idx -> answers[idx] == loadedQuestions[idx].correctOptionId }
    }

    // Один источник статусов для полоски прогресса и сетки в окне завершения.
    fun segStatus(idx: Int): TestSegStatus {
        val answeredOptionId = answers[idx] ?: return TestSegStatus.None
        return when {
            mode == TestMode.EXAM -> TestSegStatus.Answered
            answeredOptionId == loadedQuestions[idx].correctOptionId -> TestSegStatus.Correct
            else -> TestSegStatus.Wrong
        }
    }

    LaunchedEffect(mode) {
        if (mode == TestMode.EXAM) {
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
            }
            correctCount = computeCorrectCount()
            showResult = true
        }
    }

    if (showResult) {
        LaunchedEffect(Unit) {
            val answersPayload = loadedQuestions.indices.mapNotNull { idx ->
                answers[idx]?.let { optionId -> "${loadedQuestions[idx].id}:$optionId" }
            }.joinToString(",")
            onSaveResult(correctCount, loadedQuestions.size, answersPayload)
        }
        ResultScreen(correctCount = correctCount, total = loadedQuestions.size, onFinish = onFinish)
        return
    }

    val question = loadedQuestions[currentIndex]
    val selectedOptionId = answers[currentIndex]
    val hasAnswered = selectedOptionId != null
    val isAnswerLocked = mode == TestMode.LEARNING && hasAnswered
    val isExam = mode == TestMode.EXAM
    val modeLabel = if (isExam) strings.commonExam else strings.commonLearning
    val testTitle = "${strings.commonTestWord} ${test.number}"
    val statuses = loadedQuestions.indices.map { segStatus(it) }

    fun goTo(index: Int) {
        currentIndex = index
        showExplanation = false
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 900.dp
        val pad = if (wide) 28.dp else 14.dp

        DashboardBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = pad, vertical = if (wide) 18.dp else 12.dp)
        ) {
            TestHeader(
                wide = wide,
                testTitle = testTitle,
                modeLabel = modeLabel,
                isExam = isExam,
                secondsLeft = secondsLeft,
                questionWord = strings.commonQuestionWord,
                ofWord = ui.ofWord,
                number = currentIndex + 1,
                total = loadedQuestions.size,
                exitLabel = ui.exit,
                onExit = onFinish
            )
            Spacer(Modifier.height(if (wide) 14.dp else 10.dp))
            TestProgress(
                statuses = statuses,
                currentIndex = currentIndex,
                wide = wide,
                onClick = { goTo(it) }
            )
            Spacer(Modifier.height(if (wide) 16.dp else 12.dp))

            // содержимое панели с вопросом: текст, варианты и кнопка «Пояснение»
            val questionContent: @Composable ColumnScope.() -> Unit = {
                Text(
                    text = question.text,
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (wide) 19.sp else 16.sp,
                    color = Color.White
                )
                Spacer(Modifier.height(if (wide) 16.dp else 12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(if (wide) 10.dp else 8.dp)) {
                    question.options.forEach { option ->
                        val isThisSelected = option.id == selectedOptionId
                        val isThisCorrect = option.id == question.correctOptionId
                        val state = when {
                            !hasAnswered -> TestOptionState.Normal
                            isExam -> if (isThisSelected) TestOptionState.Selected else TestOptionState.Normal
                            isThisCorrect -> TestOptionState.Correct
                            isThisSelected -> TestOptionState.Wrong
                            else -> TestOptionState.Normal
                        }
                        TestOptionCard(
                            text = option.text,
                            state = state,
                            enabled = !isAnswerLocked,
                            wide = wide,
                            onClick = { answers[currentIndex] = option.id }
                        )
                    }
                }
                if (hasAnswered && !isExam) {
                    Spacer(Modifier.height(if (wide) 14.dp else 10.dp))
                    TestExplanationButton(label = ui.explanation, onClick = { showExplanation = true })
                }
            }

            if (wide) {
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    TestPanel(modifier = Modifier.weight(1f).fillMaxHeight(), padding = 14.dp) {
                        Box(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) {
                            NetworkImage(
                                url = question.imageUrl,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    TestPanel(modifier = Modifier.weight(1f).fillMaxHeight(), padding = 20.dp) {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            questionContent()
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TestNavButton(
                        text = ui.back,
                        left = true,
                        enabled = currentIndex > 0,
                        modifier = Modifier.width(190.dp),
                        onClick = { goTo(currentIndex - 1) }
                    )
                    TestNavButton(
                        text = ui.next,
                        left = false,
                        enabled = currentIndex < loadedQuestions.lastIndex,
                        modifier = Modifier.width(190.dp),
                        onClick = { goTo(currentIndex + 1) }
                    )
                }
                Spacer(Modifier.height(12.dp))
                TestYellowButton(text = strings.testFinish, showFlag = true, onClick = { showFinishConfirmation = true })
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    TestPanel(padding = 8.dp) {
                        NetworkImage(
                            url = question.imageUrl,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    TestPanel(padding = 14.dp) {
                        questionContent()
                        Spacer(Modifier.height(12.dp))
                        TestYellowButton(text = strings.testFinish, showFlag = true, onClick = { showFinishConfirmation = true })
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TestNavButton(
                        text = ui.back,
                        left = true,
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(1f),
                        onClick = { goTo(currentIndex - 1) }
                    )
                    TestNavButton(
                        text = ui.next,
                        left = false,
                        enabled = currentIndex < loadedQuestions.lastIndex,
                        modifier = Modifier.weight(1f),
                        onClick = { goTo(currentIndex + 1) }
                    )
                }
            }
        }
    }

    if (showExplanation) {
        TestDialogCard(onDismiss = { showExplanation = false }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BulbGlyph(color = Brand.Yellow, glyphSize = 30.dp)
                Text(
                    text = ui.explanation,
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.White,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            Text(
                text = question.explanation,
                fontFamily = interFamily,
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.92f),
                modifier = Modifier.padding(top = 14.dp, bottom = 18.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TestSmallYellowButton(text = strings.testClose, onClick = { showExplanation = false })
            }
        }
    }

    if (showFinishConfirmation) {
        val unansweredCount = loadedQuestions.size - answers.size
        TestDialogCard(onDismiss = { showFinishConfirmation = false }) {
            Text(
                text = strings.testFinishDialogTitle,
                fontFamily = interFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Color.White
            )
            Text(
                text = if (unansweredCount > 0)
                    strings.testFinishUnansweredText(unansweredCount, loadedQuestions.size)
                else
                    strings.testFinishAllAnsweredText,
                fontFamily = interFamily,
                fontSize = 14.sp,
                color = Dash.Text2,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(6),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.height(260.dp)
            ) {
                items(loadedQuestions.size) { idx ->
                    val st = statuses[idx]
                    val fill = when (st) {
                        TestSegStatus.Correct -> Dash.Green
                        TestSegStatus.Wrong -> Dash.Red
                        TestSegStatus.Answered -> Dash.Blue
                        TestSegStatus.None -> Color(0xFF16206F)
                    }
                    val squareShape = RoundedCornerShape(10.dp)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(squareShape)
                            .background(fill)
                            .border(1.dp, if (st == TestSegStatus.None) Dash.PanelBorder else fill, squareShape)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                goTo(idx)
                                showFinishConfirmation = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            color = Color.White,
                            fontFamily = interFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TestOutlineButton(
                    text = strings.testContinue,
                    modifier = Modifier.weight(1f),
                    onClick = { showFinishConfirmation = false }
                )
                TestSmallYellowButton(
                    text = strings.testFinishConfirm,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        showFinishConfirmation = false
                        correctCount = computeCorrectCount()
                        showResult = true
                    }
                )
            }
        }
    }
}

// ───────────────────────── шапка и полоска прогресса ─────────────────────────

@Composable
private fun TestHeader(
    wide: Boolean,
    testTitle: String,
    modeLabel: String,
    isExam: Boolean,
    secondsLeft: Int,
    questionWord: String,
    ofWord: String,
    number: Int,
    total: Int,
    exitLabel: String,
    onExit: () -> Unit
) {
    val modeColor = if (isExam) Color(0xFF6FA8FF) else Color(0xFF4DC8FF)

    if (wide) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TestPill(onClick = onExit) {
                ChevronIcon(color = Brand.Yellow, left = true, modifier = Modifier.size(18.dp))
                Text(
                    text = exitLabel,
                    fontFamily = interFontFamily(),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Text(
                text = testTitle,
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = Color.White,
                modifier = Modifier.padding(start = 20.dp)
            )
            Spacer(Modifier.width(14.dp))
            ModeChip(label = modeLabel, color = modeColor, isExam = isExam)
            Spacer(Modifier.weight(1f))
            if (isExam) {
                TimerChip(secondsLeft = secondsLeft)
                Spacer(Modifier.width(16.dp))
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = "$questionWord ", fontFamily = interFontFamily(), fontSize = 15.sp, color = Dash.Text2)
                Text(text = "$number", fontFamily = interFontFamily(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Brand.Yellow)
                Text(text = " $ofWord $total", fontFamily = interFontFamily(), fontSize = 15.sp, color = Dash.Text2)
            }
        }
    } else {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TestPill(onClick = onExit) {
                ChevronIcon(color = Brand.Yellow, left = true, modifier = Modifier.size(16.dp))
                Text(
                    text = testTitle,
                    fontFamily = interFontFamily(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 6.dp)
                )
                Text(text = " · ", fontFamily = interFontFamily(), fontSize = 13.sp, color = Dash.Text2)
                Text(
                    text = modeLabel,
                    fontFamily = interFontFamily(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = modeColor,
                    maxLines = 1
                )
            }
            Spacer(Modifier.weight(1f))
            if (isExam) {
                TimerChip(secondsLeft = secondsLeft)
                Spacer(Modifier.width(8.dp))
            }
            TestPill(onClick = null) {
                Text(text = "$number", fontFamily = interFontFamily(), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Brand.Yellow)
                Text(text = " / $total", fontFamily = interFontFamily(), fontSize = 13.sp, color = Dash.Text2)
            }
        }
    }
}

@Composable
private fun TestPill(onClick: (() -> Unit)?, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(50)
    val base = Modifier
        .clip(shape)
        .background(Color(0xFF14104A).copy(alpha = 0.78f))
        .border(1.dp, Dash.Ring.copy(alpha = 0.7f), shape)
    val interactive = if (onClick != null) {
        base.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    } else base
    Row(
        modifier = interactive.padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        content()
    }
}

@Composable
private fun ModeChip(label: String, color: Color, isExam: Boolean) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.16f))
            .border(1.dp, color.copy(alpha = 0.7f), shape)
            .padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isExam) ClockGlyph(color = color, glyphSize = 18.dp) else CapGlyph(color = color, glyphSize = 18.dp)
        Text(
            text = label,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = color,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun TimerChip(secondsLeft: Int) {
    val shape = RoundedCornerShape(50)
    val minutes = secondsLeft / 60
    val seconds = secondsLeft % 60
    val low = secondsLeft in 0..59
    val color = if (low) Dash.Red else Dash.Blue
    Row(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.20f))
            .border(1.dp, color.copy(alpha = 0.85f), shape)
            .padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClockGlyph(color = Color.White, glyphSize = 16.dp)
        Text(
            text = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}",
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.White,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun TestProgress(statuses: List<TestSegStatus>, currentIndex: Int, wide: Boolean, onClick: (Int) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val n = statuses.size
        val gap = if (wide) 6.dp else 3.dp
        val segmentWidth = (maxWidth - gap * (n - 1)) / n
        val segmentHeight = if (wide) 24.dp else 18.dp
        val showIcons = segmentWidth >= 15.dp
        val corner = if (segmentWidth >= 15.dp) 7.dp else 4.dp

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
            statuses.forEachIndexed { index, status ->
                TestSegment(
                    status = status,
                    isCurrent = index == currentIndex,
                    showIcon = showIcons,
                    corner = corner,
                    modifier = Modifier.weight(1f).height(segmentHeight),
                    onClick = { onClick(index) }
                )
            }
        }
    }
}

/**
 * Квадратик полоски прогресса. При наведении чуть приподнимается, текущий приподнят и обведён белым.
 * Цвет заливки всегда показывает статус вопроса: не отвечен, верно или ошибка.
 */
@Composable
private fun TestSegment(
    status: TestSegStatus,
    isCurrent: Boolean,
    showIcon: Boolean,
    corner: Dp,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val lift by animateDpAsState(
        targetValue = when {
            isCurrent -> (-3).dp
            hovered -> (-2).dp
            else -> 0.dp
        },
        animationSpec = tween(140)
    )
    val unanswered = Color(0xFF16206F)
    val fill = when (status) {
        TestSegStatus.Correct -> Dash.Green
        TestSegStatus.Wrong -> Dash.Red
        TestSegStatus.Answered -> Dash.Blue
        TestSegStatus.None -> unanswered
    }
    val borderColor = when {
        isCurrent -> Color.White
        hovered -> Color.White.copy(alpha = 0.6f)
        status == TestSegStatus.None -> Dash.PanelBorder
        else -> fill
    }
    val shape = RoundedCornerShape(corner)

    Box(
        modifier = modifier
            .offset(y = lift)
            .clip(shape)
            .background(fill)
            .border(if (isCurrent) 2.dp else 1.dp, borderColor, shape)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (showIcon) {
            SegmentMark(status = status)
        }
    }
}

@Composable
private fun SegmentMark(status: TestSegStatus) {
    Canvas(modifier = Modifier.size(12.dp)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.2f
        val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (status) {
            TestSegStatus.Correct -> {
                val tick = Path().apply {
                    moveTo(w * 0.15f, h * 0.55f)
                    lineTo(w * 0.42f, h * 0.80f)
                    lineTo(w * 0.85f, h * 0.22f)
                }
                drawPath(path = tick, color = Color.White, style = stroke)
            }

            TestSegStatus.Wrong -> {
                drawLine(color = Color.White, start = Offset(w * 0.2f, h * 0.2f), end = Offset(w * 0.8f, h * 0.8f), strokeWidth = sw, cap = StrokeCap.Round)
                drawLine(color = Color.White, start = Offset(w * 0.8f, h * 0.2f), end = Offset(w * 0.2f, h * 0.8f), strokeWidth = sw, cap = StrokeCap.Round)
            }

            else -> Unit
        }
    }
}

// ───────────────────────── панели, варианты, кнопки ─────────────────────────

@Composable
private fun TestPanel(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
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

@Composable
private fun TestOptionCard(text: String, state: TestOptionState, enabled: Boolean, wide: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    val tint: Color? = when (state) {
        TestOptionState.Correct -> Dash.Green
        TestOptionState.Wrong -> Dash.Red
        TestOptionState.Selected -> Dash.Blue
        TestOptionState.Normal -> null
    }
    val background = if (tint != null) {
        Brush.horizontalGradient(listOf(tint.copy(alpha = 0.30f), tint.copy(alpha = 0.12f)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF1B2790).copy(alpha = 0.70f), Color(0xFF131E70).copy(alpha = 0.70f)))
    }
    val borderColor = tint ?: Dash.PanelBorder

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(if (tint != null) 1.5.dp else 1.dp, borderColor, shape)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = if (wide) 13.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OptionIndicator(state = state)
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontSize = if (wide) 14.sp else 13.sp,
            color = Color.White,
            modifier = Modifier.weight(1f).padding(start = 14.dp)
        )
    }
}

@Composable
private fun OptionIndicator(state: TestOptionState) {
    val fill: Color? = when (state) {
        TestOptionState.Correct -> Dash.Green
        TestOptionState.Wrong -> Dash.Red
        TestOptionState.Selected -> Dash.Blue
        TestOptionState.Normal -> null
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .then(if (fill != null) Modifier.background(fill) else Modifier.border(2.dp, Dash.Ring.copy(alpha = 0.8f), CircleShape)),
        contentAlignment = Alignment.Center
    ) {
        if (fill != null) {
            Canvas(modifier = Modifier.size(14.dp)) {
                val w = this.size.width
                val h = this.size.height
                val sw = w * 0.17f
                when (state) {
                    TestOptionState.Wrong -> {
                        drawLine(color = Color.White, start = Offset(w * 0.15f, h * 0.15f), end = Offset(w * 0.85f, h * 0.85f), strokeWidth = sw, cap = StrokeCap.Round)
                        drawLine(color = Color.White, start = Offset(w * 0.85f, h * 0.15f), end = Offset(w * 0.15f, h * 0.85f), strokeWidth = sw, cap = StrokeCap.Round)
                    }

                    else -> {
                        val tick = Path().apply {
                            moveTo(w * 0.1f, h * 0.55f)
                            lineTo(w * 0.40f, h * 0.85f)
                            lineTo(w * 0.92f, h * 0.18f)
                        }
                        drawPath(path = tick, color = Color.White, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            }
        }
    }
}

@Composable
private fun TestExplanationButton(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF1B2790).copy(alpha = 0.75f), Color(0xFF131E70).copy(alpha = 0.75f))))
            .border(1.dp, Dash.Ring.copy(alpha = 0.7f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BulbGlyph(color = Brand.Yellow, glyphSize = 22.dp)
        Text(
            text = label,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
        ChevronIcon(color = Dash.Text2, left = false, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TestNavButton(text: String, left: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFF1B2790).copy(alpha = 0.85f), Color(0xFF131E70).copy(alpha = 0.85f))))
            .border(1.dp, Dash.Ring.copy(alpha = 0.7f), shape)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (left) {
            ChevronIcon(color = Brand.Yellow, left = true, modifier = Modifier.size(18.dp))
            Text(
                text = text,
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.White,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(18.dp))
        } else {
            Spacer(Modifier.width(18.dp))
            Text(
                text = text,
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.White,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            ChevronIcon(color = Brand.Yellow, left = false, modifier = Modifier.size(18.dp))
        }
    }
}

private val TestYellowGradient = Brush.horizontalGradient(listOf(Color(0xFFFFD23F), Color(0xFFFFB21E)))

@Composable
private fun TestYellowButton(text: String, showFlag: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(TestYellowGradient)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (showFlag) {
            FlagGlyph(color = Color(0xFF1A1440), glyphSize = 20.dp)
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color(0xFF1A1440)
        )
    }
}

@Composable
private fun TestSmallYellowButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(TestYellowGradient)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 26.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF1A1440)
        )
    }
}

@Composable
private fun TestOutlineButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, Dash.Ring.copy(alpha = 0.8f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color.White
        )
    }
}

/** Стеклянная карточка для модальных окон «Пояснение» и «Завершить тест?». */
@Composable
private fun TestDialogCard(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(24.dp)
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF1A2480), Color(0xFF0C1456))))
                .border(1.dp, Dash.PanelBorder, shape)
                .padding(22.dp),
            content = content
        )
    }
}

// ───────────────────────── значки, нарисованные вручную ─────────────────────────

@Composable
private fun BulbGlyph(color: Color, glyphSize: Dp) {
    Canvas(modifier = Modifier.size(glyphSize)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f
        drawCircle(color = color.copy(alpha = 0.25f), radius = w * 0.27f, center = Offset(w * 0.5f, h * 0.38f))
        drawCircle(color = color, radius = w * 0.27f, center = Offset(w * 0.5f, h * 0.38f), style = Stroke(width = sw))
        drawLine(color = color, start = Offset(w * 0.38f, h * 0.76f), end = Offset(w * 0.62f, h * 0.76f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.42f, h * 0.90f), end = Offset(w * 0.58f, h * 0.90f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
private fun FlagGlyph(color: Color, glyphSize: Dp) {
    Canvas(modifier = Modifier.size(glyphSize)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.1f
        drawLine(color = color, start = Offset(w * 0.24f, h * 0.08f), end = Offset(w * 0.24f, h * 0.94f), strokeWidth = sw, cap = StrokeCap.Round)
        val flag = Path().apply {
            moveTo(w * 0.24f, h * 0.12f)
            lineTo(w * 0.86f, h * 0.30f)
            lineTo(w * 0.24f, h * 0.52f)
        }
        drawPath(path = flag, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun ClockGlyph(color: Color, glyphSize: Dp) {
    Canvas(modifier = Modifier.size(glyphSize)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.1f
        drawCircle(color = color, radius = w * 0.42f, style = Stroke(width = sw))
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.5f), end = Offset(w * 0.5f, h * 0.26f), strokeWidth = sw, cap = StrokeCap.Round)
        drawLine(color = color, start = Offset(w * 0.5f, h * 0.5f), end = Offset(w * 0.68f, h * 0.6f), strokeWidth = sw, cap = StrokeCap.Round)
    }
}

@Composable
private fun CapGlyph(color: Color, glyphSize: Dp) {
    Canvas(modifier = Modifier.size(glyphSize)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.09f
        val top = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.96f, h * 0.38f)
            lineTo(w * 0.5f, h * 0.61f)
            lineTo(w * 0.04f, h * 0.38f)
            close()
        }
        drawPath(path = top, color = color)
        val base = Path().apply {
            moveTo(w * 0.24f, h * 0.55f)
            lineTo(w * 0.24f, h * 0.72f)
            quadraticTo(w * 0.5f, h * 0.92f, w * 0.76f, h * 0.72f)
            lineTo(w * 0.76f, h * 0.55f)
        }
        drawPath(path = base, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ───────────────────────── экран результата ─────────────────────────

/** Результат считается сданным от 90% (27 из 30), как на плитках главной страницы и в статистике. */
private const val RESULT_PASS_PERCENT = 90

@Composable
fun ResultScreen(correctCount: Int, total: Int, onFinish: () -> Unit) {
    val strings = LocalStrings.current
    val ui = testUiStrings(strings)
    val rs = resultUiStrings(strings)
    val interFamily = interFontFamily()

    val percent = if (total <= 0) 0 else correctCount * 100 / total
    val passed = percent >= RESULT_PASS_PERCENT
    val accent = if (passed) Dash.Green else Dash.Red
    val wrongCount = (total - correctCount).coerceAtLeast(0)
    val needed = (total * RESULT_PASS_PERCENT + 99) / 100
    val message = when {
        correctCount == total -> strings.resultPerfect
        correctCount in 27..29 -> strings.resultGood
        else -> strings.resultBad
    }

    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) percent / 100f else 0f,
        animationSpec = tween(900)
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val wide = maxWidth >= 600.dp
        val ringSize = if (wide) 230.dp else 190.dp

        DashboardBackground()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            DashPanel(modifier = Modifier.widthIn(max = 480.dp), padding = if (wide) 30.dp else 22.dp) {
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    // статус: сдан или не сдан
                    val chipShape = RoundedCornerShape(50)
                    Row(
                        modifier = Modifier
                            .clip(chipShape)
                            .background(accent.copy(alpha = 0.18f))
                            .border(1.dp, accent.copy(alpha = 0.85f), chipShape)
                            .padding(start = 10.dp, end = 16.dp, top = 7.dp, bottom = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusGlyph(passed = passed, color = accent, glyphSize = 18.dp)
                        Text(
                            text = if (passed) rs.statusPassed else rs.statusFailed,
                            fontFamily = interFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // кольцо результата
                    Box(modifier = Modifier.size(ringSize), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            val inset = strokeWidth / 2f
                            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
                            drawArc(
                                color = Color.White.copy(alpha = 0.10f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = strokeWidth)
                            )
                            if (progress > 0.001f) {
                                drawArc(
                                    color = accent,
                                    startAngle = -90f,
                                    sweepAngle = 360f * progress,
                                    useCenter = false,
                                    topLeft = Offset(inset, inset),
                                    size = arcSize,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$correctCount",
                                fontFamily = interFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = if (wide) 64.sp else 54.sp,
                                color = Color.White
                            )
                            Text(
                                text = "${ui.ofWord} $total",
                                fontFamily = interFamily,
                                fontSize = 16.sp,
                                color = Dash.Text2
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    Text(
                        text = message,
                        fontFamily = interFamily,
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center
                    )
                    if (!passed) {
                        Text(
                            text = rs.needToPass(needed, total),
                            fontFamily = interFamily,
                            fontSize = 13.sp,
                            color = Dash.Text2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ResultStat(value = "$correctCount", label = rs.correct, color = Dash.Green, modifier = Modifier.weight(1f))
                        ResultStat(value = "$wrongCount", label = rs.wrong, color = Dash.Red, modifier = Modifier.weight(1f))
                        ResultStat(value = "$percent%", label = rs.percent, color = Brand.Yellow, modifier = Modifier.weight(1f))
                    }

                    Spacer(Modifier.height(24.dp))

                    TestYellowButton(text = strings.resultBackToMenu, showFlag = false, onClick = onFinish)
                }
            }
        }
    }
}

@Composable
private fun ResultStat(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.55f), shape)
            .padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = color
        )
        Text(
            text = label,
            fontFamily = interFontFamily(),
            fontSize = 12.sp,
            color = Dash.Text2,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusGlyph(passed: Boolean, color: Color, glyphSize: Dp) {
    Canvas(modifier = Modifier.size(glyphSize)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f
        drawCircle(color = color, radius = w * 0.44f, style = Stroke(width = sw))
        if (passed) {
            val tick = Path().apply {
                moveTo(w * 0.28f, h * 0.52f)
                lineTo(w * 0.44f, h * 0.67f)
                lineTo(w * 0.73f, h * 0.34f)
            }
            drawPath(path = tick, color = color, style = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
        } else {
            drawLine(color = color, start = Offset(w * 0.34f, h * 0.34f), end = Offset(w * 0.66f, h * 0.66f), strokeWidth = sw, cap = StrokeCap.Round)
            drawLine(color = color, start = Offset(w * 0.66f, h * 0.34f), end = Offset(w * 0.34f, h * 0.66f), strokeWidth = sw, cap = StrokeCap.Round)
        }
    }
}

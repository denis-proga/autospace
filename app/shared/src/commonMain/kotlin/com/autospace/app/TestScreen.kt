package com.autospace.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun TestScreen(
    test: TestInfo,
    mode: TestMode,
    language: String,
    username: String,
    scope: CoroutineScope,
    onFinish: () -> Unit,
    onSaveResult: (correctCount: Int, total: Int) -> Unit
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    val interFamily = interFontFamily()

    val windowSizeClass = LocalWindowSizeClass.current
    val contentModifier = if (windowSizeClass == WindowSizeClass.Compact) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().widthIn(max = 600.dp)
    }

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
            loadErrorMessage = friendlyServerErrorMessage(e, strings)
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
            Column(
                modifier = contentModifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val errorMessage = loadErrorMessage
                if (errorMessage != null) {
                    Text(
                        text = errorMessage,
                        color = colors.error,
                        textAlign = TextAlign.Center,
                        fontFamily = interFamily,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(onClick = { reloadTrigger++ }) {
                        Text(strings.testRetry)
                    }
                } else {
                    CircularProgressIndicator()
                    if (showWakingHint) {
                        Text(
                            text = strings.commonWakingHint,
                            fontFamily = interFamily,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
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
            onSaveResult(correctCount, loadedQuestions.size)
        }
        ResultScreen(correctCount = correctCount, total = loadedQuestions.size, onFinish = onFinish)
        return
    }

    val question = loadedQuestions[currentIndex]
    val selectedOptionId = answers[currentIndex]
    val hasAnswered = selectedOptionId != null
    val isAnswerLocked = mode == TestMode.LEARNING && hasAnswered

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = contentModifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${strings.commonTestWord} ${test.number}",
                    fontFamily = interFamily,
                    color = colors.textSecondary
                )
                if (mode == TestMode.EXAM) {
                    val minutes = secondsLeft / 60
                    val seconds = secondsLeft % 60
                    Text(
                        text = "⏱ ${minutes}:${seconds.toString().padStart(2, '0')}",
                        fontFamily = interFamily,
                        color = colors.textPrimary
                    )
                }
            }

            Text(
                text = "${strings.commonQuestionWord} ${currentIndex + 1}/${loadedQuestions.size}",
                fontFamily = interFamily,
                color = colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
            )
            SegmentedProgressBar(
                total = loadedQuestions.size,
                currentIndex = currentIndex,
                modifier = Modifier.fillMaxWidth()
            )

            NetworkImage(
                url = question.imageUrl,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp)
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            )

            Text(
                text = question.text,
                fontFamily = interFamily,
                color = colors.textPrimary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                question.options.forEach { option ->
                    val isThisSelected = option.id == selectedOptionId
                    val isThisCorrect = option.id == question.correctOptionId

                    val backgroundColor: Color
                    val borderColor: Color
                    val textColor: Color
                    val rowAlpha: Float

                    when {
                        !hasAnswered -> {
                            backgroundColor = colors.card
                            borderColor = colors.border
                            textColor = colors.textPrimary
                            rowAlpha = 1f
                        }
                        mode == TestMode.EXAM -> {
                            if (isThisSelected) {
                                backgroundColor = colors.accent
                                borderColor = colors.accent
                                textColor = Color.White
                            } else {
                                backgroundColor = colors.card
                                borderColor = colors.border
                                textColor = colors.textPrimary
                            }
                            rowAlpha = 1f
                        }
                        isThisCorrect -> {
                            backgroundColor = colors.success
                            borderColor = colors.success
                            textColor = Color.White
                            rowAlpha = 1f
                        }
                        isThisSelected -> {
                            backgroundColor = colors.error
                            borderColor = colors.error
                            textColor = Color.White
                            rowAlpha = 1f
                        }
                        else -> {
                            backgroundColor = colors.card
                            borderColor = colors.border
                            textColor = colors.textPrimary
                            rowAlpha = 0.45f
                        }
                    }

                    val rowLocked = isAnswerLocked

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .alpha(rowAlpha)
                            .clip(RoundedCornerShape(14.dp))
                            .background(backgroundColor)
                            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                            .clickable(enabled = !rowLocked) {
                                answers[currentIndex] = option.id
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(text = option.text, fontFamily = interFamily, color = textColor)
                    }
                }
            }

            if (hasAnswered && mode == TestMode.LEARNING) {
                Text(
                    text = strings.testExplanation,
                    fontFamily = interFamily,
                    color = colors.accent,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { showExplanation = true }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = strings.testPrev,
                    fontFamily = interFamily,
                    color = if (currentIndex > 0) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.clickable(enabled = currentIndex > 0) {
                        currentIndex--
                        showExplanation = false
                    }
                )
                Text(
                    text = strings.testNext,
                    fontFamily = interFamily,
                    color = if (currentIndex < loadedQuestions.lastIndex) colors.textPrimary else colors.textSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.clickable(enabled = currentIndex < loadedQuestions.lastIndex) {
                        currentIndex++
                        showExplanation = false
                    }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accent)
                    .clickable { showFinishConfirmation = true }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(strings.testFinish, fontFamily = interFamily, color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }

    if (showExplanation) {
        Dialog(onDismissRequest = { showExplanation = false }) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(strings.testExplanation, fontFamily = interFamily, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                    Text(
                        text = question.explanation,
                        fontFamily = interFamily,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .align(Alignment.End)
                            .clickable { showExplanation = false }
                    ) {
                        Text(strings.testClose, fontFamily = interFamily, color = colors.accent)
                    }
                }
            }
        }
    }

    if (showFinishConfirmation) {
        val unansweredCount = loadedQuestions.size - answers.size
        Dialog(onDismissRequest = { showFinishConfirmation = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.widthIn(max = 420.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = strings.testFinishDialogTitle,
                        fontFamily = interFamily,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (unansweredCount > 0)
                            strings.testFinishUnansweredText(unansweredCount, loadedQuestions.size)
                        else
                            strings.testFinishAllAnsweredText,
                        fontFamily = interFamily,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(260.dp)
                    ) {
                        items(loadedQuestions.size) { idx ->
                            val answeredOptionId = answers[idx]
                            val squareColor = when {
                                answeredOptionId == null -> colors.card
                                mode == TestMode.EXAM -> colors.examBlue
                                answeredOptionId == loadedQuestions[idx].correctOptionId -> colors.success
                                else -> colors.error
                            }
                            val textColor = if (answeredOptionId == null) colors.textPrimary else Color.White

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(squareColor)
                                    .border(1.dp, colors.border, RoundedCornerShape(6.dp))
                                    .clickable {
                                        currentIndex = idx
                                        showExplanation = false
                                        showFinishConfirmation = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "${idx + 1}", color = textColor, fontFamily = interFamily, fontSize = 12.sp)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                                .clickable { showFinishConfirmation = false }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(strings.testContinue, fontFamily = interFamily, color = colors.textPrimary)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.accent)
                                .clickable {
                                    showFinishConfirmation = false
                                    correctCount = computeCorrectCount()
                                    showResult = true
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(strings.testFinishConfirm, fontFamily = interFamily, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultScreen(correctCount: Int, total: Int, onFinish: () -> Unit) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    val spaceGroteskFamily = spaceGroteskFontFamily()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val message = when {
            correctCount == total -> strings.resultPerfect
            correctCount in 27..29 -> strings.resultGood
            else -> strings.resultBad
        }

        Text(
            text = strings.resultOutOf(correctCount, total),
            fontFamily = spaceGroteskFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 32.sp,
            color = colors.textPrimary
        )
        Text(
            text = message,
            fontFamily = interFamily,
            color = colors.textSecondary,
            modifier = Modifier.padding(top = 16.dp)
        )
        Box(
            modifier = Modifier
                .padding(top = 32.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.accent)
                .clickable { onFinish() }
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Text(strings.resultBackToMenu, fontFamily = interFamily, color = Color.White)
        }
    }
}
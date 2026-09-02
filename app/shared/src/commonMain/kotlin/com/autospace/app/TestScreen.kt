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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    username: String,
    scope: CoroutineScope,
    onFinish: () -> Unit,
    onSaveResult: (correctCount: Int, total: Int) -> Unit
) {
    var questions by remember { mutableStateOf<List<Question>?>(null) }
    var loadErrorMessage by remember { mutableStateOf<String?>(null) }
    var showWakingHint by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableStateOf(0) }

    LaunchedEffect(test.number, reloadTrigger) {
        questions = null
        loadErrorMessage = null
        try {
            val response = ApiClient.getQuestions(test.number)
            questions = response.questions.map { it.toQuestion() }
        } catch (e: Exception) {
            loadErrorMessage = friendlyServerErrorMessage(e)
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
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val errorMessage = loadErrorMessage
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFF44336),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Button(onClick = { reloadTrigger++ }) {
                    Text("Повторить")
                }
            } else {
                CircularProgressIndicator()
                if (showWakingHint) {
                    Text(
                        text = "Подключение к серверу… Обычно это занимает до минуты",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 16.dp)
                    )
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
                // тихо игнорируем — тест просто начнётся заново
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
                        // тихо игнорируем — в следующий раз тест начнётся заново
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
    val isAnswerLocked = mode == TestMode.LEARNING && selectedOptionId != null

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("${test.title} — Вопрос ${currentIndex + 1}/${loadedQuestions.size}")
            if (mode == TestMode.EXAM) {
                val minutes = secondsLeft / 60
                val seconds = secondsLeft % 60
                Text("⏱ ${minutes}:${seconds.toString().padStart(2, '0')}")
            }
        }

        NetworkImage(
            url = question.imageUrl,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
        )

        Text(
            text = question.text,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            question.options.forEach { option ->
                val backgroundColor = when {
                    selectedOptionId == null -> MaterialTheme.colorScheme.surfaceVariant
                    mode == TestMode.EXAM -> {
                        if (option.id == selectedOptionId) Color(0xFF7E57C2)
                        else MaterialTheme.colorScheme.surfaceVariant
                    }
                    option.id == question.correctOptionId -> Color(0xFF4CAF50)
                    option.id == selectedOptionId -> Color(0xFFF44336)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }

                Button(
                    onClick = {
                        if (!isAnswerLocked) {
                            answers[currentIndex] = option.id
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = backgroundColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(option.text)
                }
            }
        }

        if (selectedOptionId != null && mode == TestMode.LEARNING) {
            OutlinedButton(
                onClick = { showExplanation = true },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            ) {
                Text("Объяснение")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(
                onClick = { currentIndex--; showExplanation = false },
                enabled = currentIndex > 0
            ) {
                Text("← Пред.")
            }

            OutlinedButton(
                onClick = { currentIndex++; showExplanation = false },
                enabled = currentIndex < loadedQuestions.lastIndex
            ) {
                Text("След. →")
            }
        }

        Button(
            onClick = { showFinishConfirmation = true },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text("Завершить тест")
        }
    }

    if (showExplanation) {
        AlertDialog(
            onDismissRequest = { showExplanation = false },
            confirmButton = {
                Button(onClick = { showExplanation = false }) {
                    Text("Закрыть")
                }
            },
            title = { Text("Объяснение") },
            text = { Text(question.explanation) }
        )
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
                        text = "Завершить тест?",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (unansweredCount > 0)
                            "Без ответа: $unansweredCount из ${loadedQuestions.size}. Нажмите на номер, чтобы перейти к вопросу."
                        else
                            "Все вопросы отвечены. Нажмите на номер, чтобы вернуться к вопросу.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(220.dp)
                    ) {
                        items(loadedQuestions.size) { idx ->
                            val answeredOptionId = answers[idx]
                            val squareColor = when {
                                answeredOptionId == null -> Color.White
                                mode == TestMode.EXAM -> Color(0xFF2196F3)
                                answeredOptionId == loadedQuestions[idx].correctOptionId -> Color(0xFF4CAF50)
                                else -> Color(0xFFF44336)
                            }
                            val textColor = if (answeredOptionId == null) Color.Black else Color.White

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(squareColor)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
                                    .clickable {
                                        currentIndex = idx
                                        showExplanation = false
                                        showFinishConfirmation = false
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    color = textColor,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showFinishConfirmation = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Продолжить")
                        }
                        Button(
                            onClick = {
                                showFinishConfirmation = false
                                correctCount = computeCorrectCount()
                                showResult = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Завершить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultScreen(correctCount: Int, total: Int, onFinish: () -> Unit) {
    val message = when {
        correctCount == total -> "Come on, go to pass the theory!"
        correctCount in 27..29 -> "Сомнительно, ну окей!"
        else -> "Ещё нужно подучить!"
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "$correctCount из $total",
            style = MaterialTheme.typography.displaySmall
        )
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp)
        )
        Button(
            onClick = onFinish,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text("В меню")
        }
    }
}
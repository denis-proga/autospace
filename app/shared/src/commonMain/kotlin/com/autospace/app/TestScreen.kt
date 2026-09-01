package com.autospace.app

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

@Composable
fun TestScreen(
    test: TestInfo,
    mode: TestMode,
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
            if (loadErrorMessage != null) {
                Text(
                    text = loadErrorMessage,
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
    var selectedOptionId by remember { mutableStateOf<Int?>(null) }
    var showExplanation by remember { mutableStateOf(false) }
    var correctCount by remember { mutableStateOf(0) }
    var showResult by remember { mutableStateOf(false) }

    var secondsLeft by remember { mutableStateOf(30 * 60) }

    LaunchedEffect(mode) {
        if (mode == TestMode.EXAM) {
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
            }
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

        AsyncImage(
            model = question.imageUrl,
            contentDescription = null,
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
                        if (selectedOptionId == null) {
                            selectedOptionId = option.id
                            if (option.id == question.correctOptionId) {
                                correctCount++
                            }
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

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedOptionId != null && mode == TestMode.LEARNING) {
                OutlinedButton(onClick = { showExplanation = true }) {
                    Text("Объяснение")
                }
            }

            if (selectedOptionId != null) {
                Button(onClick = {
                    if (currentIndex < loadedQuestions.lastIndex) {
                        currentIndex++
                        selectedOptionId = null
                        showExplanation = false
                    } else {
                        showResult = true
                    }
                }) {
                    Text(if (currentIndex < loadedQuestions.lastIndex) "Следующий >>" else "Завершить")
                }
            }
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
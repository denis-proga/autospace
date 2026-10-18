package com.autospace.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

/** Состояние вопроса дня: храним снаружи экрана, чтобы карточка на главной знала, отвечен ли он. */
internal object DailyQuestionState {
    var answeredToday by mutableStateOf(false)
    var correctToday by mutableStateOf(false)
}

/**
 * Окно «Вопрос дня». Вопрос один и тот же для всех, меняется раз в сутки,
 * отвечать можно один раз: после ответа видно верный вариант и пояснение.
 */
@Composable
internal fun DailyQuestionDialog(language: String, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val ui = testUiStrings(strings)
    val scope = rememberCoroutineScope()

    var data by remember { mutableStateOf<DailyQuestionDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var sending by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            val response = ApiClient.getDailyQuestion(language)
            data = response
            DailyQuestionState.answeredToday = response.answered
            DailyQuestionState.correctToday = response.correct
        } catch (e: Exception) {
            error = friendlyServerErrorMessage(e, strings)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(24.dp)
        // окно по центру: не шире 460 точек на большом экране и не шире самого экрана на телефоне
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .heightIn(max = 640.dp)
                .clip(shape)
                .background(Brush.verticalGradient(listOf(Color(0xFF1A2480), Color(0xFF0C1456))))
                .border(1.dp, Dash.PanelBorder, shape)
                .padding(20.dp)
        ) {
            Text(
                text = strings.dashQuestionOfDay,
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )
            Spacer(Modifier.height(14.dp))

            val current = data
            when {
                error != null -> Text(
                    text = error.orEmpty(),
                    fontFamily = interFontFamily(),
                    fontSize = 14.sp,
                    color = Dash.Red
                )

                current == null -> Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Brand.Yellow) }

                current.question == null -> Text(
                    text = strings.errorGeneric,
                    fontFamily = interFontFamily(),
                    fontSize = 14.sp,
                    color = Dash.Text2
                )

                else -> {
                    val question = current.question
                    Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                        NetworkImage(
                            url = question.imageUrl,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF0A0F45).copy(alpha = 0.5f))
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = question.questionText,
                            fontFamily = interFontFamily(),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Spacer(Modifier.height(12.dp))

                        val options = buildList {
                            add("A" to question.optionA)
                            add("B" to question.optionB)
                            question.optionC?.let { add("C" to it) }
                            question.optionD?.let { add("D" to it) }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            options.forEach { (letter, text) ->
                                val answered = current.answered
                                val isChosen = answered && letter == current.chosenOption
                                val isCorrect = answered && letter == question.correctOption
                                val tint = when {
                                    isCorrect -> Dash.Green
                                    isChosen -> Dash.Red
                                    else -> null
                                }
                                val optionShape = RoundedCornerShape(14.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(optionShape)
                                        .background(
                                            if (tint != null) tint.copy(alpha = 0.22f)
                                            else Color(0xFF1B2790).copy(alpha = 0.7f)
                                        )
                                        .border(1.dp, tint ?: Dash.PanelBorder, optionShape)
                                        .clickable(enabled = !answered && !sending, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                            sending = true
                                            scope.launch {
                                                try {
                                                    val response = ApiClient.answerDailyQuestion(letter, language)
                                                    data = response
                                                    DailyQuestionState.answeredToday = true
                                                    DailyQuestionState.correctToday = response.correct
                                                } catch (e: Exception) {
                                                    error = friendlyServerErrorMessage(e, strings)
                                                }
                                                sending = false
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = text,
                                        fontFamily = interFontFamily(),
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        if (current.answered && question.explanation.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            val hintShape = RoundedCornerShape(14.dp)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(hintShape)
                                    .background(Color(0xFF0A0F45).copy(alpha = 0.7f))
                                    .border(1.dp, Dash.PanelBorder, hintShape)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = ui.explanation,
                                    fontFamily = interFontFamily(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Brand.Yellow
                                )
                                Text(
                                    text = question.explanation,
                                    fontFamily = interFontFamily(),
                                    fontSize = 13.sp,
                                    color = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brand.Yellow)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                        .padding(horizontal = 24.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = strings.testClose,
                        fontFamily = interFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF1A1440)
                    )
                }
            }
        }
        }
    }
}

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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

/**
 * Работа над ошибками: вопросы, на которые человек в прошлый раз ответил неверно.
 * Верный ответ убирает вопрос из списка; результаты тестов и рейтинг это не меняет.
 */
@Composable
internal fun MistakesDialog(language: String, onDismiss: () -> Unit, onCountChanged: (Int) -> Unit) {
    val strings = LocalStrings.current
    val ui = testUiStrings(strings)
    val ms = mistakesStrings(strings)
    val scope = rememberCoroutineScope()

    var questions by remember { mutableStateOf<List<QuestionDto>?>(null) }
    var index by remember { mutableStateOf(0) }
    var answer by remember { mutableStateOf<MistakeAnswerDto?>(null) }
    var chosen by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = ApiClient.getMistakes(language, 10)
            questions = response.questions
            onCountChanged(response.count)
        } catch (e: Exception) {
            error = friendlyServerErrorMessage(e, strings)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(24.dp)
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
                val list = questions
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.dashMistakes,
                        fontFamily = interFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    if (list != null && list.isNotEmpty()) {
                        Text(
                            text = "${index + 1} ${ui.ofWord} ${list.size}",
                            fontFamily = interFontFamily(),
                            fontSize = 13.sp,
                            color = Dash.Text2
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                when {
                    error != null -> Text(error.orEmpty(), fontFamily = interFontFamily(), fontSize = 14.sp, color = Dash.Red)

                    list == null -> Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Brand.Yellow) }

                    list.isEmpty() -> Text(ms.empty, fontFamily = interFontFamily(), fontSize = 14.sp, color = Dash.Text2)

                    index >= list.size -> Text(ms.done, fontFamily = interFontFamily(), fontSize = 14.sp, color = Dash.Green)

                    else -> {
                        val question = list[index]
                        val result = answer
                        Column(modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                            NetworkImage(
                                url = question.imageUrl,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
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
                                    val answered = result != null
                                    val isCorrect = answered && letter == result?.correctOption
                                    val isChosen = answered && letter == chosen
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
                                            .clickable(enabled = !answered, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                                chosen = letter
                                                scope.launch {
                                                    try {
                                                        val response = ApiClient.answerMistake(question.id, letter, language)
                                                        answer = response
                                                        onCountChanged(response.left)
                                                    } catch (e: Exception) {
                                                        error = friendlyServerErrorMessage(e, strings)
                                                    }
                                                }
                                            }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = text, fontFamily = interFontFamily(), fontSize = 13.sp, color = Color.White)
                                    }
                                }
                            }

                            if (result != null && result.explanation.isNotBlank()) {
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
                                        text = result.explanation,
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
                    val canGoNext = answer != null && list != null && index < list.size
                    if (canGoNext) {
                        DialogYellowButton(text = ui.next) {
                            answer = null
                            chosen = ""
                            index++
                        }
                        Spacer(Modifier.height(0.dp))
                    } else {
                        DialogYellowButton(text = strings.testClose, onClick = onDismiss)
                    }
                }
            }
        }
    }
}

@Composable
internal fun DialogYellowButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Brand.Yellow)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp)
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

/** Подписи работы над ошибками. Отдельный класс: в AppStrings лимит полей исчерпан. */
internal class MistakesStrings(val empty: String, val done: String)

private val MistRu = MistakesStrings("Ошибок нет, повторять нечего", "Все ошибки разобраны")
private val MistEn = MistakesStrings("No mistakes to review", "All mistakes are done")
private val MistEs = MistakesStrings("No hay errores que repasar", "Todos los errores repasados")
private val MistDe = MistakesStrings("Keine Fehler zum Wiederholen", "Alle Fehler bearbeitet")
private val MistUk = MistakesStrings("Помилок немає, повторювати нічого", "Усі помилки розібрані")

internal fun mistakesStrings(strings: AppStrings): MistakesStrings = when (strings.botGreeting) {
    EnglishStrings.botGreeting -> MistEn
    SpanishStrings.botGreeting -> MistEs
    GermanStrings.botGreeting -> MistDe
    UkrainianStrings.botGreeting -> MistUk
    else -> MistRu
}

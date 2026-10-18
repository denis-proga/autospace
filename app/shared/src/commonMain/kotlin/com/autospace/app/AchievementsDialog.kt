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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Все достижения: открытые и те, что ещё нужно заработать, с прогрессом. */
@Composable
internal fun AchievementsDialog(onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val ach = achievementStrings(strings)

    var data by remember { mutableStateOf<AchievementsDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            data = ApiClient.getAchievements()
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
                val current = data
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.dashAchievements,
                        fontFamily = interFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    if (current != null) {
                        Text(
                            text = "${current.unlockedCount} / ${current.achievements.size}",
                            fontFamily = interFontFamily(),
                            fontSize = 13.sp,
                            color = Dash.Text2
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                when {
                    error != null -> Text(error.orEmpty(), fontFamily = interFontFamily(), fontSize = 14.sp, color = Dash.Red)

                    current == null -> Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Brand.Yellow) }

                    else -> Column(
                        modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        current.achievements.forEach { item ->
                            AchievementRow(item, ach)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    DialogYellowButton(text = strings.testClose, onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementDto, ach: AchievementStrings) {
    val shape = RoundedCornerShape(16.dp)
    val accent = if (item.unlocked) Brand.Yellow else Dash.Text2
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (item.unlocked) Brand.Yellow.copy(alpha = 0.10f) else Color(0xFF0A0F45).copy(alpha = 0.6f)
            )
            .border(1.dp, if (item.unlocked) Brand.Yellow.copy(alpha = 0.7f) else Dash.PanelBorder, shape)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f))
                .border(1.dp, accent.copy(alpha = 0.7f), CircleShape)
                .alpha(if (item.unlocked) 1f else 0.6f),
            contentAlignment = Alignment.Center
        ) { StarIcon(accent, 20.dp, filled = item.unlocked) }

        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(
                text = ach.title(item.id),
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = if (item.unlocked) Color.White else Color.White.copy(alpha = 0.75f)
            )
            Text(
                text = ach.description(item.id),
                fontFamily = interFontFamily(),
                fontSize = 12.sp,
                color = Dash.Text2
            )
            if (!item.unlocked && item.goal > 1) {
                Box(
                    modifier = Modifier
                        .padding(top = 7.dp)
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.12f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((item.progress.toFloat() / item.goal).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(Brand.Yellow)
                    )
                }
                Text(
                    text = "${item.progress} / ${item.goal}",
                    fontFamily = interFontFamily(),
                    fontSize = 11.sp,
                    color = Dash.Text2,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
        }
    }
}

/** Подписи достижений по id. Отдельный класс: в AppStrings лимит полей исчерпан. */
internal class AchievementStrings(private val titles: Map<String, Pair<String, String>>) {
    fun title(id: String): String = titles[id]?.first ?: id
    fun description(id: String): String = titles[id]?.second.orEmpty()
}

private val AchRu = AchievementStrings(mapOf(
    "first_test" to ("Первый тест" to "Пройти любой тест"),
    "tests_10" to ("Десятка" to "Сдать 10 разных тестов"),
    "tests_all" to ("Все тесты" to "Сдать все 50 тестов"),
    "perfect_exam" to ("Безупречный экзамен" to "Сдать экзамен без единой ошибки"),
    "correct_500" to ("Пятьсот верных" to "Дать 500 правильных ответов"),
    "daily_10" to ("Знаток дня" to "Верно ответить на 10 вопросов дня"),
    "no_mistakes" to ("Чисто" to "Разобрать все свои ошибки")
))

private val AchEn = AchievementStrings(mapOf(
    "first_test" to ("First test" to "Complete any test"),
    "tests_10" to ("Ten down" to "Pass 10 different tests"),
    "tests_all" to ("All tests" to "Pass all 50 tests"),
    "perfect_exam" to ("Perfect exam" to "Pass an exam with no mistakes"),
    "correct_500" to ("Five hundred" to "Give 500 correct answers"),
    "daily_10" to ("Daily expert" to "Answer 10 daily questions correctly"),
    "no_mistakes" to ("All clear" to "Review every mistake you made")
))

private val AchEs = AchievementStrings(mapOf(
    "first_test" to ("Primer test" to "Completa cualquier test"),
    "tests_10" to ("Diez tests" to "Aprueba 10 tests distintos"),
    "tests_all" to ("Todos los tests" to "Aprueba los 50 tests"),
    "perfect_exam" to ("Examen perfecto" to "Aprueba un examen sin fallos"),
    "correct_500" to ("Quinientos aciertos" to "Acierta 500 respuestas"),
    "daily_10" to ("Experto del día" to "Acierta 10 preguntas del día"),
    "no_mistakes" to ("Sin errores" to "Repasa todos tus errores")
))

private val AchDe = AchievementStrings(mapOf(
    "first_test" to ("Erster Test" to "Einen Test abschließen"),
    "tests_10" to ("Zehn Tests" to "10 verschiedene Tests bestehen"),
    "tests_all" to ("Alle Tests" to "Alle 50 Tests bestehen"),
    "perfect_exam" to ("Perfekte Prüfung" to "Eine Prüfung ohne Fehler bestehen"),
    "correct_500" to ("Fünfhundert" to "500 richtige Antworten geben"),
    "daily_10" to ("Tagesexperte" to "10 Tagesfragen richtig beantworten"),
    "no_mistakes" to ("Fehlerfrei" to "Alle eigenen Fehler aufarbeiten")
))

private val AchUk = AchievementStrings(mapOf(
    "first_test" to ("Перший тест" to "Пройти будь-який тест"),
    "tests_10" to ("Десятка" to "Скласти 10 різних тестів"),
    "tests_all" to ("Усі тести" to "Скласти всі 50 тестів"),
    "perfect_exam" to ("Бездоганний іспит" to "Скласти іспит без жодної помилки"),
    "correct_500" to ("Пʼятсот правильних" to "Дати 500 правильних відповідей"),
    "daily_10" to ("Знавець дня" to "Правильно відповісти на 10 питань дня"),
    "no_mistakes" to ("Чисто" to "Розібрати всі свої помилки")
))

internal fun achievementStrings(strings: AppStrings): AchievementStrings = when (strings.botGreeting) {
    EnglishStrings.botGreeting -> AchEn
    SpanishStrings.botGreeting -> AchEs
    GermanStrings.botGreeting -> AchDe
    UkrainianStrings.botGreeting -> AchUk
    else -> AchRu
}

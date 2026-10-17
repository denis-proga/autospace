package com.autospace.app

/**
 * Строки страницы статистики. Лежат вне AppStrings: в нём уже 245 полей, и JVM не допускает
 * конструктор с большим числом параметров. Название страницы, слова «Тест», «Экзамен», «Обучение»,
 * «На главную» и текст пустого состояния берутся из AppStrings как есть.
 */
internal class StatsStrings(
    val subtitle: String,
    val cardTests: String,
    val cardAvg: String,
    val cardCorrect: String,
    val cardExams: String,
    val ofTotal: (String) -> String,
    val attemptsCount: (Int) -> String,
    val chartTitle: String,
    val chartEmpty: String,
    val threshold: String,
    val attemptsTitle: String,
    val showAll: (Int) -> String,
    val collapse: String,
)

private val StatsRu = StatsStrings(
    subtitle = "Ваш прогресс в подготовке к экзамену",
    cardTests = "Тестов пройдено",
    cardAvg = "Средний результат",
    cardCorrect = "Правильных ответов",
    cardExams = "Экзаменов сдано",
    ofTotal = { n -> "из $n" },
    attemptsCount = { n -> "попыток: $n" },
    chartTitle = "Динамика результатов",
    chartEmpty = "Пройдите ещё хотя бы один тест, и здесь появится график",
    threshold = "Порог сдачи 90%",
    attemptsTitle = "Последние попытки",
    showAll = { n -> "Показать все ($n)" },
    collapse = "Свернуть",
)

private val StatsEn = StatsStrings(
    subtitle = "Your progress toward the exam",
    cardTests = "Tests passed",
    cardAvg = "Average score",
    cardCorrect = "Correct answers",
    cardExams = "Exams passed",
    ofTotal = { n -> "of $n" },
    attemptsCount = { n -> "attempts: $n" },
    chartTitle = "Score trend",
    chartEmpty = "Take at least one more test and the chart will appear here",
    threshold = "Pass mark 90%",
    attemptsTitle = "Recent attempts",
    showAll = { n -> "Show all ($n)" },
    collapse = "Show less",
)

private val StatsEs = StatsStrings(
    subtitle = "Tu progreso en la preparación del examen",
    cardTests = "Tests aprobados",
    cardAvg = "Resultado medio",
    cardCorrect = "Respuestas correctas",
    cardExams = "Exámenes aprobados",
    ofTotal = { n -> "de $n" },
    attemptsCount = { n -> "intentos: $n" },
    chartTitle = "Evolución de resultados",
    chartEmpty = "Haz al menos un test más y aquí aparecerá el gráfico",
    threshold = "Aprobado 90%",
    attemptsTitle = "Últimos intentos",
    showAll = { n -> "Ver todos ($n)" },
    collapse = "Ver menos",
)

private val StatsDe = StatsStrings(
    subtitle = "Dein Fortschritt bei der Prüfungsvorbereitung",
    cardTests = "Tests bestanden",
    cardAvg = "Durchschnitt",
    cardCorrect = "Richtige Antworten",
    cardExams = "Prüfungen bestanden",
    ofTotal = { n -> "von $n" },
    attemptsCount = { n -> "Versuche: $n" },
    chartTitle = "Ergebnisverlauf",
    chartEmpty = "Mache mindestens einen weiteren Test, dann erscheint hier das Diagramm",
    threshold = "Bestehensgrenze 90%",
    attemptsTitle = "Letzte Versuche",
    showAll = { n -> "Alle anzeigen ($n)" },
    collapse = "Weniger anzeigen",
)

private val StatsUk = StatsStrings(
    subtitle = "Ваш прогрес у підготовці до іспиту",
    cardTests = "Тестів пройдено",
    cardAvg = "Середній результат",
    cardCorrect = "Правильних відповідей",
    cardExams = "Іспитів складено",
    ofTotal = { n -> "з $n" },
    attemptsCount = { n -> "спроб: $n" },
    chartTitle = "Динаміка результатів",
    chartEmpty = "Пройдіть ще хоча б один тест, і тут з'явиться графік",
    threshold = "Поріг складання 90%",
    attemptsTitle = "Останні спроби",
    showAll = { n -> "Показати всі ($n)" },
    collapse = "Згорнути",
)

/** Язык определяется по самим строкам интерфейса, поэтому не зависит от формата кода языка. */
internal fun statsStrings(strings: AppStrings): StatsStrings = when (strings.botGreeting) {
    EnglishStrings.botGreeting -> StatsEn
    SpanishStrings.botGreeting -> StatsEs
    GermanStrings.botGreeting -> StatsDe
    UkrainianStrings.botGreeting -> StatsUk
    else -> StatsRu
}

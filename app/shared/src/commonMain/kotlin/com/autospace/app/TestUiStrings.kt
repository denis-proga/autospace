package com.autospace.app

/**
 * Подписи экрана теста, которых нет в AppStrings («Назад», «Далее», «Пояснение», «Выйти из теста», «из»).
 * Лежат отдельно: в AppStrings уже 245 полей, и JVM не допускает конструктор с большим числом параметров.
 * Слова «Тест», «Вопрос», «Обучение», «Экзамен», «Завершить тест», «Закрыть» берутся из AppStrings.
 */
internal class TestUiStrings(
    val exit: String,
    val back: String,
    val next: String,
    val explanation: String,
    val ofWord: String,
    val goBack: String,
)

private val TestUiRu = TestUiStrings(exit = "Выйти из теста", back = "Назад", next = "Далее", explanation = "Пояснение", ofWord = "из", goBack = "Вернуться")
private val TestUiEn = TestUiStrings(exit = "Leave test", back = "Back", next = "Next", explanation = "Explanation", ofWord = "of", goBack = "Go back")
private val TestUiEs = TestUiStrings(exit = "Salir del test", back = "Atrás", next = "Siguiente", explanation = "Explicación", ofWord = "de", goBack = "Volver")
private val TestUiDe = TestUiStrings(exit = "Test verlassen", back = "Zurück", next = "Weiter", explanation = "Erklärung", ofWord = "von", goBack = "Zurück")
private val TestUiUk = TestUiStrings(exit = "Вийти з тесту", back = "Назад", next = "Далі", explanation = "Пояснення", ofWord = "з", goBack = "Повернутися")

/** Язык определяется по самим строкам интерфейса, поэтому не зависит от формата кода языка. */
internal fun testUiStrings(strings: AppStrings): TestUiStrings = when (strings.botGreeting) {
    EnglishStrings.botGreeting -> TestUiEn
    SpanishStrings.botGreeting -> TestUiEs
    GermanStrings.botGreeting -> TestUiDe
    UkrainianStrings.botGreeting -> TestUiUk
    else -> TestUiRu
}

/** Подписи нового экрана результата теста. Отдельный класс по той же причине: лимит полей AppStrings. */
internal class ResultUiStrings(
    val statusPassed: String,
    val statusFailed: String,
    val correct: String,
    val wrong: String,
    val percent: String,
    val needToPass: (needed: Int, total: Int) -> String,
)

private val ResultRu = ResultUiStrings("Тест сдан", "Тест не сдан", "Верно", "Ошибок", "Результат") { n, t -> "Для сдачи нужно не менее $n из $t" }
private val ResultEn = ResultUiStrings("Test passed", "Test not passed", "Correct", "Wrong", "Score") { n, t -> "To pass you need at least $n of $t" }
private val ResultEs = ResultUiStrings("Test aprobado", "Test suspendido", "Aciertos", "Errores", "Resultado") { n, t -> "Para aprobar necesitas al menos $n de $t" }
private val ResultDe = ResultUiStrings("Test bestanden", "Test nicht bestanden", "Richtig", "Falsch", "Ergebnis") { n, t -> "Zum Bestehen brauchst du mindestens $n von $t" }
private val ResultUk = ResultUiStrings("Тест складено", "Тест не складено", "Вірно", "Помилок", "Результат") { n, t -> "Щоб скласти, потрібно щонайменше $n з $t" }

internal fun resultUiStrings(strings: AppStrings): ResultUiStrings = when (strings.botGreeting) {
    EnglishStrings.botGreeting -> ResultEn
    SpanishStrings.botGreeting -> ResultEs
    GermanStrings.botGreeting -> ResultDe
    UkrainianStrings.botGreeting -> ResultUk
    else -> ResultRu
}

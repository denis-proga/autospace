package com.autospace.app

/**
 * Помощник на странице поддержки. Работает без сервера и без платных сервисов:
 * темы-кнопки и поиск по ключевым словам. Ответы берут данные доступа пользователя.
 * Если ответ не помог, помощник передаёт вопрос разработчику (форма обращения).
 */
internal enum class BotTopic { NO_CODE, PASSWORD, ACCESS, PAYMENT, PROGRESS, LANGUAGE, QUESTION_ERROR }

internal data class ChatLine(val fromBot: Boolean, val text: String)

internal sealed interface BotAction {
    class Topic(val topic: BotTopic) : BotAction
    object Helped : BotAction
    object NotHelped : BotAction
    object Escalate : BotAction
    object Decline : BotAction
    object Tariffs : BotAction
    object Topics : BotAction
}

internal data class BotChoice(val label: String, val action: BotAction, val highlight: Boolean = false)

internal data class BotAnswer(val text: String, val offerTariffs: Boolean)

internal fun topicLabel(topic: BotTopic, strings: AppStrings): String = when (topic) {
    BotTopic.NO_CODE -> strings.botTopicNoCode
    BotTopic.PASSWORD -> strings.botTopicPassword
    BotTopic.ACCESS -> strings.botTopicAccess
    BotTopic.PAYMENT -> strings.botTopicPayment
    BotTopic.PROGRESS -> strings.botTopicProgress
    BotTopic.LANGUAGE -> strings.botTopicLanguage
    BotTopic.QUESTION_ERROR -> strings.botTopicQuestionError
}

/** Ищет тему по словам пользователя. Ключевые слова на всех пяти языках. */
internal fun matchTopic(text: String): BotTopic? {
    val t = text.lowercase()
    fun has(vararg keys: String) = keys.any { t.contains(it) }
    return when {
        has("парол", "password", "contraseña", "contrasena", "passwort") -> BotTopic.PASSWORD
        has(
            "оплат", "платёж", "платеж", "платил", "заплат", "pago", "pagar", "pagad", "pagué", "pague",
            "payment", "paid", "zahl", "tarif", "precio", "подписк", "subscription"
        ) -> BotTopic.PAYMENT
        has(
            "закрыт", "доступ", "демо", "demo", "заблок", "недоступ", "замок", "lock", "bloquea",
            "candado", "acceso", "zugang", "gesperrt", "не открыва", "no puedo", "cannot open", "can't open"
        ) -> BotTopic.ACCESS
        has(
            "ошибк", "неправильн", "неверн", "mal hecho", "incorrect", "wrong", "error en", "falsch",
            "fehler", "картинк", "imagen", "bild", "опечатк", "typo"
        ) -> BotTopic.QUESTION_ERROR
        has(
            "прогресс", "progress", "progreso", "fortschritt", "результат", "статист", "сброс",
            "reiniciar", "resultado", "reset"
        ) -> BotTopic.PROGRESS
        has("язык", "language", "idioma", "sprache", "мов") -> BotTopic.LANGUAGE
        has("код", "code", "código", "codigo", "письм", "почт", "correo", "mail", "spam", "спам") -> BotTopic.NO_CODE
        else -> null
    }
}

/** Ответ по теме. Для доступа учитывает реальное состояние лицензии. */
internal fun botAnswer(topic: BotTopic, strings: AppStrings, license: LicenseInfoDto?): BotAnswer = when (topic) {
    BotTopic.NO_CODE -> BotAnswer(strings.botAnswerNoCode, false)
    BotTopic.PASSWORD -> BotAnswer(strings.botAnswerPassword, false)
    BotTopic.PAYMENT -> BotAnswer(strings.botAnswerPayment, true)
    BotTopic.PROGRESS -> BotAnswer(strings.botAnswerProgress, false)
    BotTopic.LANGUAGE -> BotAnswer(strings.botAnswerLanguage, false)
    BotTopic.QUESTION_ERROR -> BotAnswer(strings.botAnswerQuestionError, false)
    BotTopic.ACCESS -> {
        val lic = license
        when {
            lic != null && lic.status == "DEMO" ->
                BotAnswer(strings.botAnswerAccessDemo(lic.demoTests.size, lic.demoLimit), true)
            lic != null && lic.status == "EXPIRED" -> BotAnswer(strings.botAnswerAccessExpired, true)
            else -> BotAnswer(strings.botAnswerAccessActive, false)
        }
    }
}

/** Категория обращения, которую подставляем в форму после «Не помогло». */
internal fun ticketCategoryFor(topic: BotTopic?): String = when (topic) {
    BotTopic.PAYMENT -> "PAYMENT"
    BotTopic.NO_CODE -> "NO_CODE"
    BotTopic.QUESTION_ERROR -> "QUESTION_ERROR"
    BotTopic.ACCESS, BotTopic.PASSWORD -> "TECH"
    else -> "OTHER"
}

/** Тема обращения из первых слов вопроса пользователя (до ~40 символов, по границе слова). */
internal fun subjectFromQuestion(question: String): String {
    val q = question.trim().replace(Regex("\\s+"), " ")
    if (q.length <= 40) return q
    val cut = q.take(40)
    val space = cut.lastIndexOf(' ')
    return (if (space >= 15) cut.take(space) else cut).trimEnd('.', ',', ';', ':', '!', '?') + "…"
}

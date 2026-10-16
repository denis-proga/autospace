package com.autospace.app

/**
 * Три строки помощника вне AppStrings: у AppStrings уже 245 полей, и JVM не допускает
 * конструктор с большим числом параметров (лимит 255 слотов с учётом служебных).
 * Новые экраны должны держать свои строки в отдельных классах, а не в AppStrings.
 */
internal class BotDecisionStrings(val yesWrite: String, val noPickTopic: String, val declined: String)

private val DecisionRu = BotDecisionStrings("Да, написать", "Нет, выберу тему", "Хорошо. Выберите тему ниже или напишите вопрос другими словами.")
private val DecisionEn = BotDecisionStrings("Yes, write", "No, pick a topic", "Okay. Pick a topic below or rephrase your question.")
private val DecisionEs = BotDecisionStrings("Sí, escribir", "No, elegir tema", "De acuerdo. Elige un tema abajo o reformula tu pregunta.")
private val DecisionDe = BotDecisionStrings("Ja, schreiben", "Nein, Thema wählen", "Okay. Wähle unten ein Thema oder formuliere die Frage um.")
private val DecisionUk = BotDecisionStrings("Так, написати", "Ні, оберу тему", "Добре. Оберіть тему нижче або сформулюйте питання інакше.")

/** Язык определяется по самим строкам интерфейса, поэтому не зависит от формата кода языка. */
internal fun botDecisionStrings(strings: AppStrings): BotDecisionStrings = when (strings.botGreeting) {
    EnglishStrings.botGreeting -> DecisionEn
    SpanishStrings.botGreeting -> DecisionEs
    GermanStrings.botGreeting -> DecisionDe
    UkrainianStrings.botGreeting -> DecisionUk
    else -> DecisionRu
}

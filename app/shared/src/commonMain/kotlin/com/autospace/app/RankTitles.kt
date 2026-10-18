package com.autospace.app

/**
 * Звания по уровню. Опыт — верные ответы во всех тестах, 150 очков на уровень,
 * поэтому полный проход всех 50 тестов даёт примерно десятый уровень.
 */
internal object RankTitles {
    private val ru = listOf("Новичок", "Стажёр", "Ученик", "Уверенный", "Опытный", "Мастер дороги")
    private val en = listOf("Novice", "Trainee", "Learner", "Confident", "Experienced", "Road master")
    private val es = listOf("Novato", "Aprendiz", "Alumno", "Seguro", "Experto", "Maestro del volante")
    private val de = listOf("Neuling", "Azubi", "Schüler", "Sicher", "Erfahren", "Straßenmeister")
    private val uk = listOf("Новачок", "Стажер", "Учень", "Впевнений", "Досвідчений", "Майстер дороги")

    /** Звание меняется каждые три уровня, последнее остаётся навсегда. */
    fun forLevel(level: Int, strings: AppStrings): String {
        val list = when (strings.botGreeting) {
            EnglishStrings.botGreeting -> en
            SpanishStrings.botGreeting -> es
            GermanStrings.botGreeting -> de
            UkrainianStrings.botGreeting -> uk
            else -> ru
        }
        val index = ((level - 1) / 3).coerceIn(0, list.lastIndex)
        return list[index]
    }
}

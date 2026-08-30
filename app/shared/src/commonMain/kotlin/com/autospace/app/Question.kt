package com.autospace.app

data class AnswerOption(
    val id: Int,
    val text: String
)

data class Question(
    val id: Int,
    val text: String,
    val imageRes: String? = null, // путь/ресурс картинки — подключим позже
    val options: List<AnswerOption>,
    val correctOptionId: Int,
    val explanation: String
)

fun generateMockQuestions(): List<Question> {
    return (1..30).map { index ->
        Question(
            id = index,
            text = "Вопрос номер $index: как правильно поступить в данной ситуации?",
            options = listOf(
                AnswerOption(1, "Вариант A"),
                AnswerOption(2, "Вариант B"),
                AnswerOption(3, "Вариант C"),
                AnswerOption(4, "Вариант D")
            ),
            correctOptionId = 1,
            explanation = "Объяснение: согласно ПДД, в данной ситуации правильным является вариант A, потому что..."
        )
    }
}
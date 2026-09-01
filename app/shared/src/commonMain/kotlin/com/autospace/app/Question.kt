package com.autospace.app

data class AnswerOption(
    val id: Int,
    val text: String
)

data class Question(
    val id: Int,
    val text: String,
    val imageUrl: String,
    val options: List<AnswerOption>,
    val correctOptionId: Int,
    val explanation: String
)

fun QuestionDto.toQuestion(): Question {
    val allOptions = listOf(
        1 to optionA,
        2 to optionB,
        3 to optionC,
        4 to optionD
    )

    val options = allOptions
        .filter { (_, text) -> text != null }
        .map { (id, text) -> AnswerOption(id, text!!) }

    val correctId = when (correctOption.uppercase()) {
        "A" -> 1
        "B" -> 2
        "C" -> 3
        "D" -> 4
        else -> 1
    }

    return Question(
        id = id,
        text = questionText,
        imageUrl = imageUrl,
        options = options,
        correctOptionId = correctId,
        explanation = explanation
    )
}
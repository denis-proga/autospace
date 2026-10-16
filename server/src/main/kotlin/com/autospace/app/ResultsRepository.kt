package com.autospace.app

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

data class ScoreOutcome(val correctCount: Int, val totalQuestions: Int)

object ResultsRepository {
    private val optionLetters = mapOf(1 to "A", 2 to "B", 3 to "C", 4 to "D")

    /** "12:3,15:1" в карту {id вопроса: номер варианта}. Некорректные пары пропускаются. */
    private fun parseAnswers(raw: String): Map<Int, Int> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(",").mapNotNull { pair ->
            val parts = pair.split(":")
            if (parts.size != 2) return@mapNotNull null
            val questionId = parts[0].trim().toIntOrNull()
            val option = parts[1].trim().toIntOrNull()
            if (questionId != null && option != null && option in 1..4) questionId to option else null
        }.toMap()
    }

    /**
     * Считает результат на сервере: сверяет ответы с правильными вариантами вопросов этого теста,
     * обновляет историю ответов по вопросам и сохраняет результат теста.
     * null, если такого теста нет.
     */
    fun saveScoredResult(userId: Int, testNumber: Int, mode: String, answersRaw: String): ScoreOutcome? {
        return transaction<ScoreOutcome?> {
            val questions = Questions.selectAll()
                .where { Questions.testNumber eq testNumber }
                .map { it[Questions.id] to it[Questions.correctOption].uppercase() }

            if (questions.isEmpty()) return@transaction null

            val chosen = parseAnswers(answersRaw)
            val now = System.currentTimeMillis()

            val existing = UserAnswers.selectAll()
                .where { (UserAnswers.userId eq userId) and (UserAnswers.questionId inList questions.map { it.first }) }
                .associateBy { it[UserAnswers.questionId] }

            var correct = 0
            for ((questionId, correctLetter) in questions) {
                val option = chosen[questionId] ?: continue
                val isCorrect = optionLetters[option] == correctLetter
                if (isCorrect) correct++

                val row = existing[questionId]
                if (row == null) {
                    UserAnswers.insert {
                        it[UserAnswers.userId] = userId
                        it[UserAnswers.questionId] = questionId
                        it[UserAnswers.attempts] = 1
                        it[UserAnswers.wrongCount] = if (isCorrect) 0 else 1
                        it[UserAnswers.lastCorrect] = isCorrect
                        it[UserAnswers.updatedAt] = now
                    }
                } else {
                    UserAnswers.update({ (UserAnswers.userId eq userId) and (UserAnswers.questionId eq questionId) }) {
                        it[UserAnswers.attempts] = row[UserAnswers.attempts] + 1
                        it[UserAnswers.wrongCount] = row[UserAnswers.wrongCount] + (if (isCorrect) 0 else 1)
                        it[UserAnswers.lastCorrect] = isCorrect
                        it[UserAnswers.updatedAt] = now
                    }
                }
            }

            TestResults.insert {
                it[TestResults.userId] = userId
                it[TestResults.testNumber] = testNumber
                it[TestResults.mode] = mode
                it[TestResults.correctCount] = correct
                it[TestResults.totalQuestions] = questions.size
                it[TestResults.completedAt] = now
            }

            ScoreOutcome(correct, questions.size)
        }
    }

    fun getResults(username: String): List<TestResultDto> {
        return transaction {
            val user = Users.select(Users.id)
                .where { Users.username eq username }
                .singleOrNull() ?: return@transaction emptyList()

            TestResults.selectAll()
                .where { TestResults.userId eq user[Users.id] }
                .orderBy(TestResults.completedAt, SortOrder.DESC)
                .map { row ->
                    TestResultDto(
                        testNumber = row[TestResults.testNumber],
                        mode = row[TestResults.mode],
                        correctCount = row[TestResults.correctCount],
                        totalQuestions = row[TestResults.totalQuestions],
                        completedAt = row[TestResults.completedAt]
                    )
                }
        }
    }
}

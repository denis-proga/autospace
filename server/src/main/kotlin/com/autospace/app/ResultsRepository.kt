package com.autospace.app

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

object ResultsRepository {
    fun saveResult(
        username: String,
        testNumber: Int,
        mode: String,
        correctCount: Int,
        totalQuestions: Int
    ): Boolean {
        return transaction {
            val user = Users.select(Users.id)
                .where { Users.username eq username }
                .singleOrNull() ?: return@transaction false

            TestResults.insert {
                it[userId] = user[Users.id]
                it[TestResults.testNumber] = testNumber
                it[TestResults.mode] = mode
                it[TestResults.correctCount] = correctCount
                it[TestResults.totalQuestions] = totalQuestions
                it[completedAt] = System.currentTimeMillis()
            }
            true
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
package com.autospace.app

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

data class TicketInfo(
    val id: Int,
    val userId: Int,
    val username: String,
    val subject: String,
    val language: String,
    val replyEmail: String?
)

object TicketRepository {
    val CATEGORIES = setOf("PAYMENT", "NO_CODE", "QUESTION_ERROR", "TECH", "OTHER")
    val LANGUAGES = setOf("ru", "en", "es", "de", "uk")

    /** Сколько обращений может быть открыто одновременно у одного пользователя. */
    const val MAX_ACTIVE = 3

    fun activeCount(userId: Int): Int {
        return transaction {
            SupportTickets.selectAll()
                .where { (SupportTickets.userId eq userId) and (SupportTickets.status neq "CLOSED") }
                .count()
                .toInt()
        }
    }

    fun create(userId: Int, category: String, subject: String, message: String, contactEmail: String?, language: String): Int {
        return transaction {
            val now = System.currentTimeMillis()
            SupportTickets.insert {
                it[SupportTickets.userId] = userId
                it[SupportTickets.category] = category
                it[SupportTickets.subject] = subject
                it[SupportTickets.message] = message
                it[SupportTickets.contactEmail] = contactEmail
                it[SupportTickets.language] = language
                it[SupportTickets.status] = "OPEN"
                it[SupportTickets.createdAt] = now
                it[SupportTickets.updatedAt] = now
            }[SupportTickets.id]
        }
    }

    fun listFor(userId: Int): List<TicketDto> {
        return transaction {
            SupportTickets.selectAll()
                .where { SupportTickets.userId eq userId }
                .orderBy(SupportTickets.createdAt, SortOrder.DESC)
                .map { row ->
                    TicketDto(
                        id = row[SupportTickets.id],
                        category = row[SupportTickets.category],
                        subject = row[SupportTickets.subject],
                        message = row[SupportTickets.message],
                        status = row[SupportTickets.status],
                        createdAt = row[SupportTickets.createdAt],
                        updatedAt = row[SupportTickets.updatedAt],
                        lastReply = row[SupportTickets.lastReply],
                        lastReplyAt = row[SupportTickets.lastReplyAt]
                    )
                }
                .take(20)
        }
    }

    /** Ответ админа из Telegram: сохраняет текст, ставит статус ANSWERED и возвращает данные для письма. */
    fun reply(ticketId: Int, text: String): TicketInfo? {
        return transaction<TicketInfo?> {
            val row = SupportTickets.selectAll().where { SupportTickets.id eq ticketId }.singleOrNull()
                ?: return@transaction null
            val now = System.currentTimeMillis()

            SupportTickets.update({ SupportTickets.id eq ticketId }) {
                it[SupportTickets.status] = "ANSWERED"
                it[SupportTickets.lastReply] = text
                it[SupportTickets.lastReplyAt] = now
                it[SupportTickets.updatedAt] = now
            }

            val user = Users.selectAll().where { Users.id eq row[SupportTickets.userId] }.singleOrNull()
            TicketInfo(
                id = ticketId,
                userId = row[SupportTickets.userId],
                username = user?.get(Users.username) ?: "?",
                subject = row[SupportTickets.subject],
                language = row[SupportTickets.language],
                replyEmail = row[SupportTickets.contactEmail] ?: user?.get(Users.email)
            )
        }
    }

    fun setStatus(ticketId: Int, status: String): Boolean {
        return transaction {
            SupportTickets.update({ SupportTickets.id eq ticketId }) {
                it[SupportTickets.status] = status
                it[SupportTickets.updatedAt] = System.currentTimeMillis()
            } > 0
        }
    }

    /** Строки для списка в Telegram: все обращения, которые ещё не закрыты. */
    fun listOpen(): List<String> {
        return transaction {
            SupportTickets.selectAll()
                .where { SupportTickets.status neq "CLOSED" }
                .orderBy(SupportTickets.createdAt, SortOrder.DESC)
                .map { row ->
                    "#${row[SupportTickets.id]} [${row[SupportTickets.status]}] ${row[SupportTickets.subject]}"
                }
        }
    }
}

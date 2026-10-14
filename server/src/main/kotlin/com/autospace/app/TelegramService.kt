package com.autospace.app

import io.github.cdimascio.dotenv.dotenv
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.call.body

val dotenv = dotenv {
    ignoreIfMissing = true
}
fun readSetting(key: String): String =
    System.getenv(key) ?: dotenv[key] ?: ""

private val botToken: String = readSetting("TELEGRAM_BOT_TOKEN")
private val chatId: String = readSetting("TELEGRAM_CHAT_ID")

private val telegramClient = HttpClient(CIO) {
    install(io.ktor.client.plugins.HttpTimeout) {
        requestTimeoutMillis = 40_000
    }
}

@Serializable
data class InlineKeyboardButton(
    val text: String,
    val callback_data: String
)

@Serializable
data class InlineKeyboardMarkup(
    val inline_keyboard: List<List<InlineKeyboardButton>>
)

@Serializable
data class TelegramMessage(
    val chat_id: String,
    val text: String,
    val reply_markup: InlineKeyboardMarkup? = null
)

@Serializable
data class AnswerCallback(
    val callback_query_id: String,
    val text: String
)

@Serializable
data class TelegramFrom(val id: Long)

@Serializable
data class TelegramCallbackQuery(
    val id: String,
    val from: TelegramFrom,
    val data: String? = null
)

@Serializable
data class TelegramIncomingMessage(
    val text: String? = null,
    val from: TelegramFrom? = null
)

@Serializable
data class TelegramUpdate(
    val update_id: Long,
    val callback_query: TelegramCallbackQuery? = null,
    val message: TelegramIncomingMessage? = null
)

@Serializable
data class TelegramUpdatesResponse(
    val ok: Boolean,
    val result: List<TelegramUpdate>
)

object TelegramService {
    /** Уведомление о новом пользователе. Одобрение больше не нужно: он сразу получает демо. */
    suspend fun notifyNewRegistration(userId: Int, firstName: String, lastName: String, username: String, email: String) {
        if (botToken.isEmpty() || chatId.isEmpty()) {
            println("Telegram not configured, skipping notification")
            return
        }

        val text = "🆕 Новая регистрация (демо)\n\n" +
                "Имя: $firstName $lastName\n" +
                "Username: $username\n" +
                "Email: $email\n" +
                "ID: #$userId"

        val keyboard = InlineKeyboardMarkup(
            inline_keyboard = listOf(
                listOf(
                    InlineKeyboardButton(text = "🚫 Заблокировать", callback_data = "block:$userId")
                )
            )
        )

        val message = TelegramMessage(chat_id = chatId, text = text, reply_markup = keyboard)

        telegramClient.post("https://api.telegram.org/bot$botToken/sendMessage") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(TelegramMessage.serializer(), message))
        }
    }

    suspend fun notifySupportRequest(username: String, message: String, phone: String?, email: String?) {
        if (botToken.isEmpty() || chatId.isEmpty()) {
            println("Telegram not configured, skipping notification")
            return
        }

        val contactInfo = buildString {
            if (!phone.isNullOrBlank()) append("\nPhone: $phone")
            if (!email.isNullOrBlank()) append("\nEmail: $email")
        }

        val text = "📩 Support request\n\n" +
                "From: $username$contactInfo\n\n" +
                "Message:\n$message"

        val telegramMessage = TelegramMessage(chat_id = chatId, text = text)

        telegramClient.post("https://api.telegram.org/bot$botToken/sendMessage") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(TelegramMessage.serializer(), telegramMessage))
        }
    }

    /** Заявка «я оплатил»: админ проверяет, что деньги пришли, и нажимает кнопку. */
    suspend fun notifyPaymentClaim(claimId: Int, userId: Int, username: String, email: String, days: Int, amountEur: Int) {
        if (botToken.isEmpty() || chatId.isEmpty()) {
            println("Telegram not configured, skipping notification")
            return
        }

        val text = "💶 Заявка на оплату #$claimId\n\n" +
                "Пользователь: #$userId $username\n" +
                "Email: $email\n" +
                "Тариф: $days дн. за $amountEur €\n\n" +
                "Проверь, что платёж пришёл, и подтверди."

        val keyboard = InlineKeyboardMarkup(
            inline_keyboard = listOf(
                listOf(
                    InlineKeyboardButton(text = "✅ Выдать $days дн.", callback_data = "pay_ok:$claimId"),
                    InlineKeyboardButton(text = "❌ Отклонить", callback_data = "pay_no:$claimId")
                )
            )
        )

        val message = TelegramMessage(chat_id = chatId, text = text, reply_markup = keyboard)

        telegramClient.post("https://api.telegram.org/bot$botToken/sendMessage") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(TelegramMessage.serializer(), message))
        }
    }

    suspend fun sendPlainMessage(text: String) {
        if (botToken.isEmpty() || chatId.isEmpty()) return

        val telegramMessage = TelegramMessage(chat_id = chatId, text = text)

        telegramClient.post("https://api.telegram.org/bot$botToken/sendMessage") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(TelegramMessage.serializer(), telegramMessage))
        }
    }
}

private const val HELP_TEXT = "Команды:\n" +
        "/users — список пользователей\n" +
        "/grant <id> <дней> — выдать или продлить доступ\n" +
        "/friend <id> — доступ без срока (друзья)\n" +
        "/revoke <id> — снять доступ\n" +
        "/block <id> — заблокировать\n" +
        "/unblock <id> — разблокировать\n" +
        "/demo_reset <id> — вернуть бесплатные тесты\n" +
        "/delete <id> — удалить пользователя"

suspend fun startTelegramListener() {
    if (botToken.isEmpty() || chatId.isEmpty()) {
        println("Telegram not configured, listener not started")
        return
    }

    var offset = 0L

    while (true) {
        try {
            val response: String = telegramClient.get("https://api.telegram.org/bot$botToken/getUpdates") {
                parameter("offset", offset)
                parameter("timeout", 30)
            }.body()

            val json = Json { ignoreUnknownKeys = true }
            val updates = json.decodeFromString(TelegramUpdatesResponse.serializer(), response)

            for (update in updates.result) {
                offset = update.update_id + 1

                val incoming = update.message
                if (incoming != null) {
                    if (incoming.from?.id?.toString() != chatId) continue
                    val text = incoming.text?.trim() ?: continue

                    val parts = text.split(Regex("\\s+"))
                    val command = parts[0].substringBefore("@")
                    val arg1 = parts.getOrNull(1)?.toIntOrNull()
                    val arg2 = parts.getOrNull(2)?.toIntOrNull()

                    when (command) {
                        "/users" -> {
                            val users = UserRepository.listUsers()
                            if (users.isEmpty()) {
                                TelegramService.sendPlainMessage("Пользователей пока нет")
                            } else {
                                // у Telegram лимит на длину сообщения, поэтому шлём частями
                                users.chunked(40).forEachIndexed { index, chunk ->
                                    val header = if (index == 0) "👥 Пользователи:\n\n" else ""
                                    TelegramService.sendPlainMessage(header + chunk.joinToString("\n"))
                                }
                            }
                        }

                        "/grant" -> {
                            if (arg1 == null || arg2 == null || arg2 !in 1..3650) {
                                TelegramService.sendPlainMessage("Формат: /grant <id> <дней>, например /grant 5 30")
                            } else {
                                TelegramService.sendPlainMessage(UserRepository.grantDays(arg1, arg2))
                            }
                        }

                        "/friend" -> {
                            if (arg1 == null) TelegramService.sendPlainMessage("Укажите id: /friend 5")
                            else TelegramService.sendPlainMessage(UserRepository.setFriend(arg1))
                        }

                        "/revoke" -> {
                            if (arg1 == null) TelegramService.sendPlainMessage("Укажите id: /revoke 5")
                            else TelegramService.sendPlainMessage(UserRepository.revokeAccess(arg1))
                        }

                        "/block" -> {
                            if (arg1 == null) {
                                TelegramService.sendPlainMessage("Укажите id: /block 5")
                            } else {
                                val done = UserRepository.blockUser(arg1)
                                TelegramService.sendPlainMessage(
                                    if (done) "🚫 Пользователь #$arg1 заблокирован" else "Пользователь #$arg1 не найден"
                                )
                            }
                        }

                        "/unblock" -> {
                            if (arg1 == null) TelegramService.sendPlainMessage("Укажите id: /unblock 5")
                            else TelegramService.sendPlainMessage(UserRepository.unblockUser(arg1))
                        }

                        "/demo_reset" -> {
                            if (arg1 == null) TelegramService.sendPlainMessage("Укажите id: /demo_reset 5")
                            else TelegramService.sendPlainMessage(UserRepository.resetDemo(arg1))
                        }

                        "/delete" -> {
                            if (arg1 == null) {
                                TelegramService.sendPlainMessage("Укажите id: /delete 5")
                            } else {
                                val deleted = UserRepository.deleteUser(arg1)
                                TelegramService.sendPlainMessage(
                                    if (deleted) "Пользователь #$arg1 удалён" else "Пользователь #$arg1 не найден"
                                )
                            }
                        }

                        "/help", "/start" -> TelegramService.sendPlainMessage(HELP_TEXT)
                    }
                    continue
                }

                val callback = update.callback_query ?: continue
                if (callback.from.id.toString() != chatId) continue

                val data = callback.data ?: continue
                val parts = data.split(":")
                if (parts.size != 2) continue

                val action = parts[0]
                val targetId = parts[1].toIntOrNull() ?: continue

                when (action) {
                    // "reject" оставлен для старых сообщений с кнопками
                    "block", "reject" -> {
                        UserRepository.blockUser(targetId)
                        answerCallback(callback.id, "Пользователь заблокирован 🚫")
                    }
                    "pay_ok" -> {
                        val result = UserRepository.approveClaim(targetId)
                        answerCallback(callback.id, result.take(190))
                        TelegramService.sendPlainMessage(result)
                    }
                    "pay_no" -> {
                        val result = UserRepository.declineClaim(targetId)
                        answerCallback(callback.id, result.take(190))
                    }
                    "approve" -> {
                        answerCallback(callback.id, "Одобрение больше не нужно. Доступ: /grant <id> <дней>")
                    }
                }
            }
        } catch (e: Exception) {
            println("Telegram listener error: ${e.message}")
        }
    }
}

private suspend fun answerCallback(callbackQueryId: String, text: String) {
    telegramClient.post("https://api.telegram.org/bot$botToken/answerCallbackQuery") {
        contentType(ContentType.Application.Json)
        setBody(Json.encodeToString(AnswerCallback.serializer(), AnswerCallback(callback_query_id = callbackQueryId, text = text)))
    }
}

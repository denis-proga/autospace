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
    suspend fun notifyNewRegistration(userId: Int, firstName: String, lastName: String, username: String, email: String) {
        if (botToken.isEmpty() || chatId.isEmpty()) {
            println("Telegram not configured, skipping notification")
            return
        }

        val text = "🆕 New registration\n\n" +
                "Name: $firstName $lastName\n" +
                "Username: $username\n" +
                "Email: $email"

        val keyboard = InlineKeyboardMarkup(
            inline_keyboard = listOf(
                listOf(
                    InlineKeyboardButton(text = "✅ Approve", callback_data = "approve:$userId"),
                    InlineKeyboardButton(text = "❌ Reject", callback_data = "reject:$userId")
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

    suspend fun sendPlainMessage(text: String) {
        if (botToken.isEmpty() || chatId.isEmpty()) return

        val telegramMessage = TelegramMessage(chat_id = chatId, text = text)

        telegramClient.post("https://api.telegram.org/bot$botToken/sendMessage") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(TelegramMessage.serializer(), telegramMessage))
        }
    }
}

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

                    when {
                        text == "/users" -> {
                            val users = UserRepository.listUsers()
                            val reply = if (users.isEmpty()) {
                                "Пользователей пока нет"
                            } else {
                                "👥 Пользователи:\n\n" + users.joinToString("\n")
                            }
                            TelegramService.sendPlainMessage(reply)
                        }

                        text.startsWith("/delete") -> {
                            val id = text.removePrefix("/delete").trim().toIntOrNull()
                            if (id == null) {
                                TelegramService.sendPlainMessage("Укажите id: /delete 5")
                            } else {
                                val deleted = UserRepository.deleteUser(id)
                                TelegramService.sendPlainMessage(
                                    if (deleted) "Пользователь #$id удалён" else "Пользователь #$id не найден"
                                )
                            }
                        }

                        text == "/help" || text == "/start" -> {
                            TelegramService.sendPlainMessage(
                                "Команды:\n/users — список пользователей\n/delete <id> — удалить пользователя"
                            )
                        }
                    }
                    continue
                }

                val callback = update.callback_query ?: continue
                if (callback.from.id.toString() != chatId) continue

                val data = callback.data ?: continue
                val parts = data.split(":")
                if (parts.size != 2) continue

                val action = parts[0]
                val userId = parts[1].toIntOrNull() ?: continue

                when (action) {
                    "approve" -> {
                        UserRepository.approveUser(userId)
                        answerCallback(callback.id, "User approved ✅")
                    }
                    "reject" -> {
                        UserRepository.blockUser(userId)
                        answerCallback(callback.id, "User rejected ❌")
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
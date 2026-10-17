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
    val result: List<TelegramUpdate> = emptyList()
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

    /**
     * Новый заказ на оплату. Человек ещё не заплатил: он видит реквизиты и код платежа.
     * Когда деньги придут, в назначении перевода будет этот код — по нему заказ и подтверждается.
     */
    suspend fun notifyPaymentOrder(orderId: Int, userId: Int, username: String, email: String, days: Int, amountEur: Int, code: String) {
        if (botToken.isEmpty() || chatId.isEmpty()) {
            println("Telegram not configured, skipping notification")
            return
        }

        val text = "🧾 Заказ #$orderId · код $code\n\n" +
                "Пользователь: #$userId $username\n" +
                "Email: $email\n" +
                "Тариф: $days дн. за $amountEur €\n\n" +
                "Когда придёт перевод на $amountEur € с кодом $code в назначении, нажми «Оплата пришла».\n" +
                "Можно и командой: /paid $code"

        val keyboard = InlineKeyboardMarkup(
            inline_keyboard = listOf(
                listOf(
                    InlineKeyboardButton(text = "✅ Оплата пришла ($days дн.)", callback_data = "pay_ok:$orderId"),
                    InlineKeyboardButton(text = "❌ Отклонить", callback_data = "pay_no:$orderId")
                )
            )
        )

        val message = TelegramMessage(chat_id = chatId, text = text, reply_markup = keyboard)

        telegramClient.post("https://api.telegram.org/bot$botToken/sendMessage") {
            contentType(ContentType.Application.Json)
            setBody(Json.encodeToString(TelegramMessage.serializer(), message))
        }
    }

    /** Новое обращение в поддержку: с кнопками «Взять в работу» и «Закрыть». */
    suspend fun notifyTicket(ticketId: Int, userId: Int, username: String, category: String, subject: String, message: String, replyEmail: String) {
        if (botToken.isEmpty() || chatId.isEmpty()) {
            println("Telegram not configured, skipping notification")
            return
        }

        val text = "🎫 Обращение #$ticketId\n\n" +
                "Тема: $subject\n" +
                "Категория: $category\n" +
                "От: #$userId $username\n" +
                "Почта для ответа: $replyEmail\n\n" +
                "$message\n\n" +
                "Ответить: /reply $ticketId текст ответа"

        val keyboard = InlineKeyboardMarkup(
            inline_keyboard = listOf(
                listOf(
                    InlineKeyboardButton(text = "🛠 Взять в работу", callback_data = "tk_work:$ticketId"),
                    InlineKeyboardButton(text = "✔ Закрыть", callback_data = "tk_close:$ticketId")
                )
            )
        )

        val telegramMessage = TelegramMessage(chat_id = chatId, text = text, reply_markup = keyboard)

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

private const val HELP_TEXT = "Команды:\n" +
        "/users — список пользователей\n" +
        "/grant <id> <дней> — выдать или продлить доступ\n" +
        "/friend <id> — доступ без срока (друзья)\n" +
        "/revoke <id> — снять доступ\n" +
        "/block <id> — заблокировать\n" +
        "/unblock <id> — разблокировать\n" +
        "/demo_reset <id> — вернуть бесплатные тесты\n" +
        "/delete <id> — удалить пользователя\n" +
        "/orders — заказы, ждущие оплаты\n" +
        "/away on [дата] — я в отъезде: оплата закрыта, прежним пользователям все тесты бесплатно\n" +
        "/away off — вернулся, всё как обычно\n" +
        "/paid <код или id> — подтвердить, что деньги пришли\n" +
        "/tickets — открытые обращения\n" +
        "/reply <id> <текст> — ответить на обращение\n" +
        "/close <id> — закрыть обращение"

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

            // Telegram отвечает ok=false, например при деплое, когда старый и новый экземпляр сервера
            // одновременно опрашивают бота (409 Conflict). Ждём и пробуем снова, а не крутим цикл без паузы.
            if (!updates.ok) {
                println("Telegram getUpdates not ok: $response")
                kotlinx.coroutines.delay(5000)
                continue
            }

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

                        "/orders" -> TelegramService.sendPlainMessage(PaymentOrders.pendingList())
                        "/away" -> {
                            when (parts.getOrNull(1)?.lowercase()) {
                                "on" -> {
                                    val until = parts.drop(2).joinToString(" ").trim()
                                    AwayMode.turnOn(until)
                                    val tail = if (until.isEmpty()) "без даты возвращения" else "до $until"
                                    TelegramService.sendPlainMessage(
                                        "🌴 Режим отъезда включён ($tail).\n" +
                                            "Оплата закрыта, прежним пользователям открыты все тесты.\n" +
                                            "Выключить: /away off"
                                    )
                                }
                                "off" -> {
                                    AwayMode.turnOff()
                                    TelegramService.sendPlainMessage("✅ Режим отъезда выключен, всё работает как обычно")
                                }
                                else -> {
                                    val state = AwayMode.state()
                                    val now = if (state.enabled) {
                                        val tail = if (state.until.isEmpty()) "без даты" else "до ${state.until}"
                                        "сейчас включён ($tail)"
                                    } else "сейчас выключен"
                                    TelegramService.sendPlainMessage("Режим отъезда $now.\nФормат: /away on 25.10 или /away off")
                                }
                            }
                        }
                        "/paid" -> {
                            val codeArg = parts.getOrNull(1)?.trim().orEmpty()
                            if (codeArg.isEmpty()) {
                                TelegramService.sendPlainMessage("Формат: /paid <код или номер заказа>, например /paid AS-7K3Q")
                            } else {
                                val orderId = codeArg.toIntOrNull() ?: PaymentOrders.findByCode(codeArg)
                                if (orderId == null) {
                                    TelegramService.sendPlainMessage("Заказ с кодом $codeArg не найден")
                                } else {
                                    val result = PaymentOrders.markPaid(orderId)
                                    TelegramService.sendPlainMessage(result.message)
                                    if (result.success) notifyUserPaid(result)
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

                        "/tickets" -> {
                            val open = TicketRepository.listOpen()
                            if (open.isEmpty()) {
                                TelegramService.sendPlainMessage("Открытых обращений нет")
                            } else {
                                open.chunked(40).forEachIndexed { index, chunk ->
                                    val header = if (index == 0) "🎫 Открытые обращения:\n\n" else ""
                                    TelegramService.sendPlainMessage(header + chunk.joinToString("\n"))
                                }
                            }
                        }

                        "/reply" -> {
                            val body = text.substringAfter(" ", "").trim()
                            val ticketId = body.substringBefore(" ").toIntOrNull()
                            val replyText = body.substringAfter(" ", "").trim()
                            if (ticketId == null || replyText.isEmpty()) {
                                TelegramService.sendPlainMessage("Формат: /reply <id> <текст ответа>")
                            } else {
                                val info = TicketRepository.reply(ticketId, replyText)
                                if (info == null) {
                                    TelegramService.sendPlainMessage("Обращение #$ticketId не найдено")
                                } else {
                                    val target = info.replyEmail
                                    val emailed = if (!target.isNullOrBlank()) {
                                        EmailService.sendTicketReply(target, ticketId, info.subject, replyText, info.language)
                                    } else false
                                    TelegramService.sendPlainMessage(
                                        "✅ Ответ на #$ticketId сохранён (${info.username}). Письмо: " + if (emailed) "отправлено" else "не отправлено"
                                    )
                                }
                            }
                        }

                        "/close" -> {
                            if (arg1 == null) {
                                TelegramService.sendPlainMessage("Укажите номер: /close 12")
                            } else {
                                val ok = TicketRepository.setStatus(arg1, "CLOSED")
                                TelegramService.sendPlainMessage(if (ok) "Обращение #$arg1 закрыто" else "Обращение #$arg1 не найдено")
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
                        val result = PaymentOrders.markPaid(targetId)
                        answerCallback(callback.id, result.message.take(190))
                        TelegramService.sendPlainMessage(result.message)
                        if (result.success) notifyUserPaid(result)
                    }
                    "tk_work" -> {
                        val ok = TicketRepository.setStatus(targetId, "IN_PROGRESS")
                        answerCallback(callback.id, if (ok) "Обращение #$targetId в работе" else "Не найдено")
                    }
                    "tk_close" -> {
                        val ok = TicketRepository.setStatus(targetId, "CLOSED")
                        answerCallback(callback.id, if (ok) "Обращение #$targetId закрыто" else "Не найдено")
                    }
                    "pay_no" -> {
                        val result = PaymentOrders.decline(targetId)
                        answerCallback(callback.id, result.take(190))
                    }
                    "approve" -> {
                        answerCallback(callback.id, "Одобрение больше не нужно. Доступ: /grant <id> <дней>")
                    }
                }
            }
        } catch (e: Exception) {
            println("Telegram listener error: ${e.message}")
            kotlinx.coroutines.delay(5000)
        }
    }
}

private suspend fun answerCallback(callbackQueryId: String, text: String) {
    telegramClient.post("https://api.telegram.org/bot$botToken/answerCallbackQuery") {
        contentType(ContentType.Application.Json)
        setBody(Json.encodeToString(AnswerCallback.serializer(), AnswerCallback(callback_query_id = callbackQueryId, text = text)))
    }
}

/** Квитанция пользователю после подтверждения оплаты. Ошибку письма глушим: доступ уже открыт. */
private suspend fun notifyUserPaid(result: PaymentConfirmResult) {
    try {
        val email = PaymentOrders.emailOf(result.userId) ?: return
        EmailService.sendPaymentReceipt(
            toEmail = email,
            days = result.days,
            amountEur = result.amountEur,
            language = "ru"
        )
    } catch (e: Exception) {
        println("Payment receipt email failed: ${e.message}")
    }
}

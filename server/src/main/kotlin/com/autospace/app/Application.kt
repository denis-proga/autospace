package com.autospace.app

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

fun main() {
    initDatabase()

    kotlinx.coroutines.GlobalScope.launch {
        startTelegramListener()
    }

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    embeddedServer(factory = Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/") {
            call.respondText("Hello, Ktor!")
        }

        post("/register") {
            val request = call.receive<RegisterRequest>()

            val exists = transaction {
                Users.select(Users.id)
                    .where { Users.username eq request.username }
                    .any()
            }

            if (exists) {
                call.respond(AuthResponse(success = false, message = "Username already taken"))
                return@post
            }

            val code = PendingRegistrations.create(
                firstName = request.firstName,
                lastName = request.lastName,
                email = request.email,
                username = request.username,
                password = request.password
            )

            val emailSent = EmailService.sendVerificationCode(request.email, code)

            if (!emailSent) {
                call.respond(AuthResponse(success = false, message = "Не удалось отправить письмо с кодом. Попробуйте позже"))
                return@post
            }

            call.respond(AuthResponse(success = true, message = "Verification code sent to email"))
        }

        post("/verify-registration") {
            val request = call.receive<VerifyRegistrationRequest>()

            val pending = PendingRegistrations.verify(request.username, request.code)

            if (pending == null) {
                call.respond(AuthResponse(success = false, message = "Invalid or expired code"))
                return@post
            }

            transaction {
                Users.insert {
                    it[firstName] = pending.firstName
                    it[lastName] = pending.lastName
                    it[email] = pending.email
                    it[username] = pending.username
                    it[passwordHash] = hashPassword(pending.password)
                    it[licenseStatus] = "PENDING"
                    it[licenseExpiresAt] = null
                }[Users.id]
            }.let { newUserId ->
                TelegramService.notifyNewRegistration(
                    userId = newUserId,
                    firstName = pending.firstName,
                    lastName = pending.lastName,
                    username = pending.username,
                    email = pending.email
                )
            }

            call.respond(AuthResponse(success = true, message = "Registration successful, awaiting approval", licenseStatus = "PENDING"))
        }

        post("/resend-code") {
            val request = call.receive<ResendCodeRequest>()

            when (val result = PendingRegistrations.resend(request.username)) {
                is ResendResult.NotFound -> {
                    call.respond(AuthResponse(success = false, message = "Регистрация не найдена или уже завершена"))
                }
                is ResendResult.Cooldown -> {
                    call.respond(AuthResponse(success = false, message = "Подождите ${result.secondsLeft} сек. перед повторной отправкой"))
                }
                is ResendResult.TooManyAttempts -> {
                    call.respond(AuthResponse(success = false, message = "Превышен лимит повторных отправок. Обратитесь в поддержку"))
                }
                is ResendResult.Success -> {
                    val emailSent = EmailService.sendVerificationCode(result.email, result.code)

                    if (!emailSent) {
                        call.respond(AuthResponse(success = false, message = "Не удалось отправить письмо с кодом. Попробуйте позже"))
                        return@post
                    }

                    call.respond(AuthResponse(success = true, message = "Код отправлен повторно"))
                }
            }
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            val user = transaction {
                Users.select(Users.passwordHash, Users.licenseStatus)
                    .where { Users.username eq request.username }
                    .singleOrNull()
            }

            if (user == null || !verifyPassword(request.password, user[Users.passwordHash])) {
                call.respond(AuthResponse(success = false, message = "Invalid username or password"))
                return@post
            }

            call.respond(
                AuthResponse(
                    success = true,
                    message = "Login successful",
                    licenseStatus = user[Users.licenseStatus]
                )
            )
        }

        post("/support") {
            val request = call.receive<SupportRequest>()

            val user = transaction {
                Users.select(Users.id, Users.supportMessagesCount, Users.email)
                    .where { Users.username eq request.username }
                    .singleOrNull()
            }

            if (user != null) {
                val currentCount = user[Users.supportMessagesCount]

                if (currentCount >= 3) {
                    call.respond(AuthResponse(success = false, message = "Вы превысили лимит сообщений в поддержку"))
                    return@post
                }

                transaction {
                    Users.update({ Users.id eq user[Users.id] }) {
                        it[supportMessagesCount] = currentCount + 1
                    }
                }

                TelegramService.notifySupportRequest(
                    username = request.username,
                    message = request.message,
                    phone = request.phone,
                    email = request.email ?: user[Users.email]
                )

                call.respond(AuthResponse(success = true, message = "Support request sent"))
                return@post
            }

            val pending = PendingRegistrations.findByUsername(request.username)

            if (pending != null) {
                if (pending.supportMessagesCount >= 3) {
                    call.respond(AuthResponse(success = false, message = "Вы превысили лимит сообщений в поддержку"))
                    return@post
                }

                pending.supportMessagesCount++

                TelegramService.notifySupportRequest(
                    username = request.username,
                    message = request.message,
                    phone = request.phone,
                    email = request.email ?: pending.email
                )

                call.respond(AuthResponse(success = true, message = "Support request sent"))
                return@post
            }

            call.respond(AuthResponse(success = false, message = "User not found"))
        }

        post("/results") {
            val request = call.receive<SaveResultRequest>()

            val saved = ResultsRepository.saveResult(
                username = request.username,
                testNumber = request.testNumber,
                mode = request.mode,
                correctCount = request.correctCount,
                totalQuestions = request.totalQuestions
            )

            if (saved) {
                call.respond(AuthResponse(success = true, message = "Result saved"))
            } else {
                call.respond(AuthResponse(success = false, message = "User not found"))
            }
        }

        get("/results/{username}") {
            val username = call.parameters["username"]

            if (username == null) {
                call.respond(StatsResponse(results = emptyList()))
                return@get
            }

            call.respond(StatsResponse(results = ResultsRepository.getResults(username)))
        }
    }
}
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
import io.ktor.http.HttpStatusCode
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

fun main() {
    initDatabase()

    kotlinx.coroutines.GlobalScope.launch {
        startTelegramListener()
    }

    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080

    embeddedServer(factory = Netty, port = port, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

private const val IMAGE_BASE_URL = "https://raw.githubusercontent.com/denis-proga/autospace-assets/main/"

private fun localizedText(base: String, localized: String?): String {
    return if (!localized.isNullOrBlank()) localized else base
}

private fun localizedNullableText(base: String?, localized: String?): String? {
    if (base == null) return null
    return if (!localized.isNullOrBlank()) localized else base
}

fun Application.module() {
    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/") {
            call.respondText("Hello, Ktor!")
        }

        get("/questions/{testNumber}") {
            val testNumber = call.parameters["testNumber"]?.toIntOrNull()
            val lang = call.request.queryParameters["lang"] ?: "ru"

            if (testNumber == null) {
                call.respond(HttpStatusCode.BadRequest, AuthResponse(success = false, message = "Invalid test number"))
                return@get
            }

            val questions = transaction {
                Questions.selectAll()
                    .where { Questions.testNumber eq testNumber }
                    .orderBy(Questions.id)
                    .map { row ->
                        val qText: String
                        val oA: String
                        val oB: String
                        val oC: String?
                        val oD: String?
                        val expl: String

                        when (lang) {
                            "en" -> {
                                qText = localizedText(row[Questions.questionText], row[Questions.questionTextEn])
                                oA = localizedText(row[Questions.optionA], row[Questions.optionAEn])
                                oB = localizedText(row[Questions.optionB], row[Questions.optionBEn])
                                oC = localizedNullableText(row[Questions.optionC], row[Questions.optionCEn])
                                oD = localizedNullableText(row[Questions.optionD], row[Questions.optionDEn])
                                expl = localizedText(row[Questions.explanation], row[Questions.explanationEn])
                            }
                            "es" -> {
                                qText = localizedText(row[Questions.questionText], row[Questions.questionTextEs])
                                oA = localizedText(row[Questions.optionA], row[Questions.optionAEs])
                                oB = localizedText(row[Questions.optionB], row[Questions.optionBEs])
                                oC = localizedNullableText(row[Questions.optionC], row[Questions.optionCEs])
                                oD = localizedNullableText(row[Questions.optionD], row[Questions.optionDEs])
                                expl = localizedText(row[Questions.explanation], row[Questions.explanationEs])
                            }
                            "de" -> {
                                qText = localizedText(row[Questions.questionText], row[Questions.questionTextDe])
                                oA = localizedText(row[Questions.optionA], row[Questions.optionADe])
                                oB = localizedText(row[Questions.optionB], row[Questions.optionBDe])
                                oC = localizedNullableText(row[Questions.optionC], row[Questions.optionCDe])
                                oD = localizedNullableText(row[Questions.optionD], row[Questions.optionDDe])
                                expl = localizedText(row[Questions.explanation], row[Questions.explanationDe])
                            }
                            "uk" -> {
                                qText = localizedText(row[Questions.questionText], row[Questions.questionTextUk])
                                oA = localizedText(row[Questions.optionA], row[Questions.optionAUk])
                                oB = localizedText(row[Questions.optionB], row[Questions.optionBUk])
                                oC = localizedNullableText(row[Questions.optionC], row[Questions.optionCUk])
                                oD = localizedNullableText(row[Questions.optionD], row[Questions.optionDUk])
                                expl = localizedText(row[Questions.explanation], row[Questions.explanationUk])
                            }
                            else -> {
                                qText = row[Questions.questionText]
                                oA = row[Questions.optionA]
                                oB = row[Questions.optionB]
                                oC = row[Questions.optionC]
                                oD = row[Questions.optionD]
                                expl = row[Questions.explanation]
                            }
                        }

                        QuestionResponse(
                            id = row[Questions.id],
                            imageUrl = IMAGE_BASE_URL + row[Questions.imageFilename],
                            questionText = qText,
                            optionA = oA,
                            optionB = oB,
                            optionC = oC,
                            optionD = oD,
                            correctOption = row[Questions.correctOption],
                            explanation = expl
                        )
                    }
            }

            if (questions.isEmpty()) {
                call.respond(HttpStatusCode.NotFound, AuthResponse(success = false, message = "Questions not found for this test"))
                return@get
            }

            call.respond(QuestionsListResponse(questions = questions))
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
                password = request.password,
                language = request.language
            )

            val emailSent = EmailService.sendVerificationCode(request.email, code, request.language)

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
                    val emailSent = EmailService.sendVerificationCode(result.email, result.code, result.language)

                    if (!emailSent) {
                        call.respond(AuthResponse(success = false, message = "Не удалось отправить письмо с кодом. Попробуйте позже"))
                        return@post
                    }

                    call.respond(AuthResponse(success = true, message = "Код отправлен повторно"))
                }
            }
        }

        post("/reset-progress") {
            val request = call.receive<ResetProgressRequest>()

            val userId = transaction {
                Users.select(Users.id).where { Users.username eq request.username }.singleOrNull()?.get(Users.id)
            }

            if (userId == null) {
                call.respond(AuthResponse(success = false, message = "User not found"))
                return@post
            }

            transaction {
                TestResults.deleteWhere { TestResults.userId eq userId }
                TestProgress.deleteWhere { TestProgress.userId eq userId }
            }

            call.respond(AuthResponse(success = true, message = "Progress reset"))
        }

        post("/login") {
            val request = call.receive<LoginRequest>()

            val user = transaction {
                Users.select(Users.id, Users.username, Users.passwordHash, Users.licenseStatus, Users.licenseExpiresAt)
                    .where { Users.username eq request.username }
                    .singleOrNull()
            }

            if (user == null || !verifyPassword(request.password, user[Users.passwordHash])) {
                call.respond(LoginResponse(success = false, message = "Invalid username or password"))
                return@post
            }

            var licenseStatus = user[Users.licenseStatus]
            val expiresAt = user[Users.licenseExpiresAt]

            if (licenseStatus == "ACTIVE" && expiresAt != null && expiresAt < System.currentTimeMillis()) {
                licenseStatus = "EXPIRED"
                transaction {
                    Users.update({ Users.id eq user[Users.id] }) {
                        it[Users.licenseStatus] = "EXPIRED"
                    }
                }
            }

            val token = UUID.randomUUID().toString()
            transaction {
                Users.update({ Users.id eq user[Users.id] }) {
                    it[sessionToken] = token
                }
            }

            call.respond(
                LoginResponse(
                    success = true,
                    message = "Login successful",
                    licenseStatus = licenseStatus,
                    username = user[Users.username],
                    token = token
                )
            )
        }

        post("/session") {
            val request = call.receive<SessionRequest>()

            val user = transaction {
                Users.select(Users.id, Users.username, Users.licenseStatus, Users.licenseExpiresAt)
                    .where { Users.sessionToken eq request.token }
                    .singleOrNull()
            }

            if (user == null) {
                call.respond(LoginResponse(success = false, message = "Invalid session"))
                return@post
            }

            var licenseStatus = user[Users.licenseStatus]
            val expiresAt = user[Users.licenseExpiresAt]

            if (licenseStatus == "ACTIVE" && expiresAt != null && expiresAt < System.currentTimeMillis()) {
                licenseStatus = "EXPIRED"
                transaction {
                    Users.update({ Users.id eq user[Users.id] }) {
                        it[Users.licenseStatus] = "EXPIRED"
                    }
                }
            }

            call.respond(
                LoginResponse(
                    success = true,
                    message = "Session valid",
                    licenseStatus = licenseStatus,
                    username = user[Users.username],
                    token = request.token
                )
            )
        }

        post("/save-progress") {
            val request = call.receive<SaveProgressRequest>()

            val userId = transaction {
                Users.select(Users.id).where { Users.username eq request.username }.singleOrNull()?.get(Users.id)
            }

            if (userId == null) {
                call.respond(AuthResponse(success = false, message = "User not found"))
                return@post
            }

            transaction {
                val existing = TestProgress.select(TestProgress.id)
                    .where {
                        (TestProgress.userId eq userId) and
                                (TestProgress.testNumber eq request.testNumber) and
                                (TestProgress.mode eq request.mode)
                    }.singleOrNull()

                if (existing != null) {
                    TestProgress.update({ TestProgress.id eq existing[TestProgress.id] }) {
                        it[answersData] = request.answersData
                        it[currentIndex] = request.currentIndex
                        it[secondsLeft] = request.secondsLeft
                        it[updatedAt] = System.currentTimeMillis()
                    }
                } else {
                    TestProgress.insert {
                        it[TestProgress.userId] = userId
                        it[testNumber] = request.testNumber
                        it[mode] = request.mode
                        it[answersData] = request.answersData
                        it[currentIndex] = request.currentIndex
                        it[secondsLeft] = request.secondsLeft
                        it[updatedAt] = System.currentTimeMillis()
                    }
                }
            }

            call.respond(AuthResponse(success = true, message = "Progress saved"))
        }

        get("/progress/{username}/{testNumber}/{mode}") {
            val username = call.parameters["username"]
            val testNumber = call.parameters["testNumber"]?.toIntOrNull()
            val mode = call.parameters["mode"]

            if (username == null || testNumber == null || mode == null) {
                call.respond(HttpStatusCode.BadRequest, ProgressResponse(found = false))
                return@get
            }

            val userId = transaction {
                Users.select(Users.id).where { Users.username eq username }.singleOrNull()?.get(Users.id)
            }

            if (userId == null) {
                call.respond(ProgressResponse(found = false))
                return@get
            }

            val progress = transaction {
                TestProgress.select(TestProgress.answersData, TestProgress.currentIndex, TestProgress.secondsLeft)
                    .where {
                        (TestProgress.userId eq userId) and
                                (TestProgress.testNumber eq testNumber) and
                                (TestProgress.mode eq mode)
                    }.singleOrNull()
            }

            if (progress == null) {
                call.respond(ProgressResponse(found = false))
            } else {
                call.respond(
                    ProgressResponse(
                        found = true,
                        answersData = progress[TestProgress.answersData],
                        currentIndex = progress[TestProgress.currentIndex],
                        secondsLeft = progress[TestProgress.secondsLeft]
                    )
                )
            }
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
                transaction {
                    val user = Users.select(Users.id).where { Users.username eq request.username }.singleOrNull()
                    if (user != null) {
                        TestProgress.deleteWhere {
                            (TestProgress.userId eq user[Users.id]) and
                                    (TestProgress.testNumber eq request.testNumber) and
                                    (TestProgress.mode eq request.mode)
                        }
                    }
                }
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
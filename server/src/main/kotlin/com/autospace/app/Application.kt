package com.autospace.app

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.*
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
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

/** IP клиента для ограничения частоты запросов (за прокси Render берём первый адрес из X-Forwarded-For). */
private fun ApplicationCall.clientIp(): String {
    val forwarded = request.headers["X-Forwarded-For"]?.split(",")?.firstOrNull()?.trim()
    return if (!forwarded.isNullOrEmpty()) forwarded else request.local.remoteHost
}

/**
 * Проверяет токен из заголовка Authorization: Bearer <token>.
 * При ошибке сама отвечает клиенту и возвращает null: вызывающий просто делает return.
 */
private suspend fun ApplicationCall.requireUser(allowBlocked: Boolean = false): AuthUser? {
    val user = LicenseService.authenticate(request.headers["Authorization"])
    if (user == null) {
        respond(HttpStatusCode.Unauthorized, ApiError(success = false, code = "UNAUTHORIZED", message = "Invalid session"))
        return null
    }
    if (user.status == "BLOCKED" && !allowBlocked) {
        respond(HttpStatusCode.Forbidden, ApiError(success = false, code = "BLOCKED", message = "Account blocked"))
        return null
    }
    return user
}

/** Реквизиты для перевода: задаются переменными окружения на Render, в коде их нет. */
private fun paymentDetails(): PaymentDetailsDto = PaymentDetailsDto(
    bizumPhone = readSetting("PAYMENT_BIZUM_PHONE"),
    cardNumber = readSetting("PAYMENT_CARD_NUMBER"),
    holderName = readSetting("PAYMENT_HOLDER_NAME")
)

private fun buildQuestion(row: ResultRow, lang: String): QuestionResponse {
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

    return QuestionResponse(
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

fun Application.module() {
    // Браузерной версии нужно явное разрешение на запросы к API.
    // Адреса сайта задаются переменной ALLOWED_ORIGINS в Render, через запятую.
    install(CORS) {
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Options)
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)

        readSetting("ALLOWED_ORIGINS")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { origin ->
                val scheme = origin.substringBefore("://", "https")
                val host = origin.substringAfter("://").trimEnd('/')
                allowHost(host, schemes = listOf(scheme))
            }

        // локальная разработка веб-версии: dev-сервер Gradle слушает порт 8080
        allowHost("localhost:8080", schemes = listOf("http"))
    }

    install(ContentNegotiation) {
        json()
    }

    routing {
        get("/") {
            call.respondText("Hello, Ktor!")
        }

        // Текущее состояние лицензии: клиент вызывает после открытия теста, чтобы обновить счётчик демо
        get("/license") {
            val user = call.requireUser(allowBlocked = true) ?: return@get
            call.respond(LicenseService.licenseInfo(user))
        }

        get("/questions/{testNumber}") {
            val testNumber = call.parameters["testNumber"]?.toIntOrNull()
            val lang = call.request.queryParameters["lang"] ?: "ru"

            if (testNumber == null) {
                call.respond(HttpStatusCode.BadRequest, ApiError(success = false, code = "BAD_REQUEST", message = "Invalid test number"))
                return@get
            }

            val user = call.requireUser() ?: return@get

            // несуществующий тест не должен тратить бесплатный слот
            val testExists = transaction {
                Questions.selectAll().where { Questions.testNumber eq testNumber }.any()
            }
            if (!testExists) {
                call.respond(HttpStatusCode.NotFound, ApiError(success = false, code = "NOT_FOUND", message = "Questions not found for this test"))
                return@get
            }

            when (val access = LicenseService.requestTestAccess(user, testNumber)) {
                TestAccess.Granted -> {}
                TestAccess.Blocked -> {
                    call.respond(HttpStatusCode.Forbidden, ApiError(success = false, code = "BLOCKED", message = "Account blocked"))
                    return@get
                }
                is TestAccess.Denied -> {
                    call.respond(HttpStatusCode.Forbidden, ApiError(success = false, code = access.code, message = "License required"))
                    return@get
                }
            }

            val questions = transaction {
                Questions.selectAll()
                    .where { Questions.testNumber eq testNumber }
                    .orderBy(Questions.id)
                    .map { row -> buildQuestion(row, lang) }
            }

            call.respond(QuestionsListResponse(questions = questions))
        }

        post("/register") {
            if (!RateLimiter.allow("register:${call.clientIp()}", 5, 60 * 60_000L)) {
                call.respond(AuthResponse(success = false, message = "Too many registration attempts. Try again later"))
                return@post
            }

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

            // один ящик = один аккаунт, иначе бесплатные тесты можно получать бесконечно
            val requestEmailKey = emailKeyOf(request.email)
            val emailTaken = transaction {
                Users.selectAll().where { Users.emailKey eq requestEmailKey }.any()
            }

            if (emailTaken) {
                call.respond(AuthResponse(success = false, message = "Email already registered"))
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

            val pendingEmailKey = emailKeyOf(pending.email)
            val emailTaken = transaction {
                Users.selectAll().where { Users.emailKey eq pendingEmailKey }.any()
            }

            if (emailTaken) {
                call.respond(AuthResponse(success = false, message = "Email already registered"))
                return@post
            }

            val newUserId = transaction {
                Users.insert {
                    it[Users.firstName] = pending.firstName
                    it[Users.lastName] = pending.lastName
                    it[Users.email] = pending.email
                    it[Users.username] = pending.username
                    it[Users.passwordHash] = hashPassword(pending.password)
                    it[Users.licenseStatus] = "DEMO"
                    it[Users.licenseExpiresAt] = null
                    it[Users.createdAt] = System.currentTimeMillis()
                    it[Users.emailKey] = pendingEmailKey
                }[Users.id]
            }

            // сбой Telegram не должен ломать регистрацию
            try {
                TelegramService.notifyNewRegistration(
                    userId = newUserId,
                    firstName = pending.firstName,
                    lastName = pending.lastName,
                    username = pending.username,
                    email = pending.email
                )
            } catch (e: Exception) {
                println("Telegram notify failed: ${e.message}")
            }

            call.respond(AuthResponse(success = true, message = "Registration successful", licenseStatus = "DEMO"))
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
            val user = call.requireUser() ?: return@post

            transaction {
                TestResults.deleteWhere { TestResults.userId eq user.id }
                TestProgress.deleteWhere { TestProgress.userId eq user.id }
                UserAnswers.deleteWhere { UserAnswers.userId eq user.id }
            }

            call.respond(AuthResponse(success = true, message = "Progress reset"))
        }

        post("/login") {
            if (!RateLimiter.allow("login:${call.clientIp()}", 30, 10 * 60_000L)) {
                call.respond(LoginResponse(success = false, message = "Too many attempts. Try again later"))
                return@post
            }

            val request = call.receive<LoginRequest>()

            // вход возможен и по логину, и по почте
            val resolvedId = PasswordResets.findUser(request.username.trim())?.id
            val user = if (resolvedId == null) null else transaction {
                Users.select(Users.id, Users.username, Users.passwordHash, Users.licenseStatus)
                    .where { Users.id eq resolvedId }
                    .singleOrNull()
            }

            // Регистрация начата, но почта не подтверждена: аккаунта ещё нет, поэтому отправляем человека
            // подтверждать почту (только если пароль совпал с тем, что он указал при регистрации).
            if (user == null) {
                val pending = PendingRegistrations.findByIdentifier(request.username)
                if (pending != null && pending.password == request.password) {
                    call.respond(
                        LoginResponse(
                            success = false,
                            message = "EMAIL_NOT_VERIFIED:${pending.username}:${maskEmail(pending.email)}"
                        )
                    )
                    return@post
                }
            }

            if (user == null || !verifyPassword(request.password, user[Users.passwordHash])) {
                call.respond(LoginResponse(success = false, message = "Invalid username or password"))
                return@post
            }

            val token = UUID.randomUUID().toString()
            transaction {
                Users.update({ Users.id eq user[Users.id] }) {
                    it[sessionToken] = token
                }
            }

            // authenticateToken заодно переводит истёкшую лицензию в EXPIRED
            val authUser = LicenseService.authenticateToken(token)

            call.respond(
                LoginResponse(
                    success = true,
                    message = "Login successful",
                    licenseStatus = authUser?.status ?: user[Users.licenseStatus],
                    username = user[Users.username],
                    token = token,
                    license = authUser?.let { LicenseService.licenseInfo(it) }
                )
            )
        }

        post("/session") {
            val request = call.receive<SessionRequest>()

            val user = LicenseService.authenticateToken(request.token)

            if (user == null) {
                call.respond(LoginResponse(success = false, message = "Invalid session"))
                return@post
            }

            call.respond(
                LoginResponse(
                    success = true,
                    message = "Session valid",
                    licenseStatus = user.status,
                    username = user.username,
                    token = request.token,
                    license = LicenseService.licenseInfo(user)
                )
            )
        }

        post("/save-progress") {
            val user = call.requireUser() ?: return@post
            val request = call.receive<SaveProgressRequest>()

            val validMode = request.mode == "LEARNING" || request.mode == "EXAM"
            if (request.username != user.username || !validMode || request.answersData.length > 2000) {
                call.respond(HttpStatusCode.BadRequest, ApiError(success = false, code = "BAD_REQUEST", message = "Invalid progress data"))
                return@post
            }

            if (!LicenseService.canSaveProgress(user, request.testNumber)) {
                call.respond(HttpStatusCode.Forbidden, ApiError(success = false, code = "NO_ACCESS", message = "No access to this test"))
                return@post
            }

            transaction {
                val existing = TestProgress.select(TestProgress.id)
                    .where {
                        (TestProgress.userId eq user.id) and
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
                        it[TestProgress.userId] = user.id
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

        // Последний незавершённый тест: для блока «Продолжить» на главной.
        // Завершённые тесты из test_progress удаляются, поэтому здесь остаются только начатые.
        get("/progress/latest") {
            val user = call.requireUser() ?: return@get

            val row = transaction {
                TestProgress.selectAll()
                    .where { TestProgress.userId eq user.id }
                    .orderBy(TestProgress.updatedAt, SortOrder.DESC)
                    .limit(1)
                    .singleOrNull()
            }

            if (row == null) {
                call.respond(LatestProgressResponse(found = false))
                return@get
            }

            val answered = row[TestProgress.answersData]
                .split(",")
                .count { it.isNotBlank() }

            call.respond(
                LatestProgressResponse(
                    found = true,
                    testNumber = row[TestProgress.testNumber],
                    mode = row[TestProgress.mode],
                    currentIndex = row[TestProgress.currentIndex],
                    answered = answered,
                    updatedAt = row[TestProgress.updatedAt]
                )
            )
        }

        get("/progress/{username}/{testNumber}/{mode}") {
            val user = call.requireUser() ?: return@get

            val username = call.parameters["username"]
            val testNumber = call.parameters["testNumber"]?.toIntOrNull()
            val mode = call.parameters["mode"]

            if (username == null || testNumber == null || mode == null) {
                call.respond(HttpStatusCode.BadRequest, ProgressResponse(found = false))
                return@get
            }

            if (username != user.username) {
                call.respond(HttpStatusCode.Forbidden, ApiError(success = false, code = "NO_ACCESS", message = "Not your data"))
                return@get
            }

            val progress = transaction {
                TestProgress.select(TestProgress.answersData, TestProgress.currentIndex, TestProgress.secondsLeft)
                    .where {
                        (TestProgress.userId eq user.id) and
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

        // Восстановление пароля, шаг 1. Ответ всегда одинаковый, чтобы нельзя было проверять, зарегистрирована ли почта.
        // Письмо уходит в фоне, чтобы разница во времени ответа тоже ничего не выдавала.
        post("/forgot-password") {
            if (!RateLimiter.allow("forgot:${call.clientIp()}", 10, 60 * 60_000L)) {
                call.respond(AuthResponse(success = false, message = "TOO_MANY_REQUESTS"))
                return@post
            }

            val request = call.receive<ForgotPasswordRequest>()
            val target = PasswordResets.findUser(request.identifier.trim())

            if (target != null) {
                val code = PasswordResets.issueCode(target.id)
                if (code != null) {
                    val email = target.email
                    val language = request.language
                    call.application.launch {
                        EmailService.sendPasswordResetCode(email, code, language)
                    }
                }
            }

            // Если это неподтверждённая регистрация, восстанавливать нечего: подсказываем завершить регистрацию
            if (target == null) {
                val pending = PendingRegistrations.findByIdentifier(request.identifier)
                if (pending != null) {
                    call.respond(
                        AuthResponse(
                            success = false,
                            message = "REGISTRATION_PENDING:${pending.username}:${maskEmail(pending.email)}"
                        )
                    )
                    return@post
                }
            }

            call.respond(AuthResponse(success = true, message = "If the account exists, a code has been sent"))
        }

        // Восстановление пароля, шаг 2: код из письма и новый пароль
        post("/reset-password") {
            if (!RateLimiter.allow("reset:${call.clientIp()}", 20, 10 * 60_000L)) {
                call.respond(AuthResponse(success = false, message = "TOO_MANY_REQUESTS"))
                return@post
            }

            val request = call.receive<ResetPasswordRequest>()

            if (request.newPassword.length < 6 || request.newPassword.length > 100) {
                call.respond(AuthResponse(success = false, message = "PASSWORD_INVALID"))
                return@post
            }

            val target = PasswordResets.findUser(request.identifier.trim())
            if (target == null || !PasswordResets.verify(target.id, request.code)) {
                call.respond(AuthResponse(success = false, message = "INVALID_CODE"))
                return@post
            }

            PasswordResets.setNewPassword(target.id, hashPassword(request.newPassword))
            call.respond(AuthResponse(success = true, message = "Password changed"))
        }

        // Тарифы и реквизиты. Реквизиты берутся из переменной окружения PAYMENT_INSTRUCTIONS (в Render), без обновления приложения
        get("/plans") {
            call.requireUser() ?: return@get
            val instructions = readSetting("PAYMENT_INSTRUCTIONS").replace("\\n", "\n")
            val away = AwayMode.state()
            call.respond(
                PlansResponse(
                    plans = LicenseService.PLANS,
                    paymentInstructions = instructions,
                    awayMode = away.enabled,
                    awayUntil = away.until
                )
            )
        }

        // Профиль: смена имени и аватара
        post("/profile") {
            val user = call.requireUser() ?: return@post
            val request = call.receive<UpdateProfileRequest>()

            val error = UserRepository.updateProfile(user.id, request.username, request.avatar)
            if (error.isNotEmpty()) {
                call.respond(UpdateProfileResponse(success = false, code = error))
                return@post
            }

            // имя могло смениться: отдаём то, что теперь в базе
            call.respond(
                UpdateProfileResponse(
                    success = true,
                    username = request.username?.trim() ?: user.username,
                    avatar = UserRepository.avatarOf(user.id)
                )
            )
        }

        // Создать заказ на оплату (до перевода денег). Повторный вызов возвращает тот же активный заказ.
        post("/payment-order") {
            val user = call.requireUser() ?: return@post
            val request = call.receive<PaymentClaimRequest>()

            val plan = LicenseService.PLANS.firstOrNull { it.days == request.days }
            if (plan == null) {
                call.respond(HttpStatusCode.BadRequest, ApiError(success = false, code = "BAD_REQUEST", message = "Unknown plan"))
                return@post
            }

            // пока админ в отъезде, подтвердить перевод некому: заказ не создаём
            if (AwayMode.isOn()) {
                call.respond(PaymentOrderResponse(success = false, code = "AWAY", awayUntil = AwayMode.state().until))
                return@post
            }

            if (!RateLimiter.allow("order:${user.id}", 10, 60 * 60_000L)) {
                call.respond(PaymentOrderResponse(success = false, code = "TOO_MANY_REQUESTS"))
                return@post
            }

            val order = PaymentOrders.create(user.id, plan.days, plan.priceEur)
            if (order == null) {
                call.respond(PaymentOrderResponse(success = false, code = "ORDER_FAILED"))
                return@post
            }

            try {
                TelegramService.notifyPaymentOrder(
                    orderId = order.orderId,
                    userId = user.id,
                    username = user.username,
                    email = user.email,
                    days = order.days,
                    amountEur = order.amountEur,
                    code = order.code
                )
            } catch (e: Exception) {
                println("Telegram order notify failed: ${e.message}")
            }

            call.respond(PaymentOrderResponse(success = true, order = order, details = paymentDetails()))
        }

        // Статус заказа: приложение опрашивает его, пока ждёт подтверждения оплаты
        get("/payment-order/{id}") {
            val user = call.requireUser() ?: return@get
            val orderId = call.parameters["id"]?.toIntOrNull()
            if (orderId == null) {
                call.respond(HttpStatusCode.BadRequest, ApiError(success = false, code = "BAD_REQUEST", message = "Bad order id"))
                return@get
            }

            val order = PaymentOrders.byIdFor(orderId, user.id)
            if (order == null) {
                call.respond(PaymentOrderResponse(success = false, code = "NOT_FOUND"))
                return@get
            }

            // после оплаты лицензия уже новая: requireUser выше вернул свежие данные пользователя
            call.respond(
                PaymentOrderResponse(
                    success = true,
                    order = order,
                    details = paymentDetails(),
                    license = LicenseService.licenseInfo(user)
                )
            )
        }

        // Активный заказ пользователя, если он уже есть (например, приложение перезапустили)
        get("/payment-order") {
            val user = call.requireUser() ?: return@get
            val order = PaymentOrders.activeFor(user.id)
            if (order == null) {
                call.respond(PaymentOrderResponse(success = false, code = "NO_ACTIVE_ORDER"))
                return@get
            }
            call.respond(PaymentOrderResponse(success = true, order = order, details = paymentDetails()))
        }

        // Отмена заказа пользователем
        post("/payment-order/{id}/cancel") {
            val user = call.requireUser() ?: return@post
            val orderId = call.parameters["id"]?.toIntOrNull()
            if (orderId == null) {
                call.respond(HttpStatusCode.BadRequest, ApiError(success = false, code = "BAD_REQUEST", message = "Bad order id"))
                return@post
            }
            val cancelled = PaymentOrders.cancel(user.id, orderId)
            call.respond(PaymentOrderResponse(success = cancelled, code = if (cancelled) "" else "NOT_FOUND"))
        }

        // Мои обращения и сколько ещё можно открыть
        get("/tickets") {
            val user = call.requireUser() ?: return@get
            val remaining = (TicketRepository.MAX_ACTIVE - TicketRepository.activeCount(user.id)).coerceAtLeast(0)
            call.respond(TicketsResponse(tickets = TicketRepository.listFor(user.id), remaining = remaining))
        }

        // Новое обращение. Уведомление уходит админу в Telegram
        post("/tickets") {
            val user = call.requireUser() ?: return@post
            val request = call.receive<TicketCreateRequest>()

            if (!RateLimiter.allow("ticket:${user.id}", 5, 60 * 60_000L)) {
                call.respond(TicketCreateResponse(success = false, code = "TOO_MANY_REQUESTS"))
                return@post
            }

            val subject = request.subject.trim()
            val message = request.message.trim()
            val email = request.contactEmail?.trim()?.takeIf { it.isNotEmpty() }
            val language = if (request.language in TicketRepository.LANGUAGES) request.language else "ru"

            val valid = request.category in TicketRepository.CATEGORIES &&
                    subject.length in 3..150 &&
                    message.length in 20..1000 &&
                    (email == null || (email.length <= 150 && email.contains("@")))
            if (!valid) {
                call.respond(TicketCreateResponse(success = false, code = "BAD_REQUEST"))
                return@post
            }

            val active = TicketRepository.activeCount(user.id)
            if (active >= TicketRepository.MAX_ACTIVE) {
                call.respond(TicketCreateResponse(success = false, code = "TOO_MANY_TICKETS"))
                return@post
            }

            val ticketId = TicketRepository.create(user.id, request.category, subject, message, email, language)

            try {
                TelegramService.notifyTicket(
                    ticketId = ticketId,
                    userId = user.id,
                    username = user.username,
                    category = request.category,
                    subject = subject,
                    message = message,
                    replyEmail = email ?: user.email
                )
            } catch (e: Exception) {
                println("Telegram ticket notify failed: ${e.message}")
            }

            call.respond(
                TicketCreateResponse(
                    success = true,
                    ticketId = ticketId,
                    remaining = (TicketRepository.MAX_ACTIVE - active - 1).coerceAtLeast(0)
                )
            )
        }

        // Поддержка остаётся без токена: писать должны уметь и те, кто не может войти
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
            val user = call.requireUser() ?: return@post
            val request = call.receive<SaveResultRequest>()

            val validMode = request.mode == "LEARNING" || request.mode == "EXAM"
            if (request.username != user.username || !validMode || request.answers.length > 2000) {
                call.respond(HttpStatusCode.BadRequest, ApiError(success = false, code = "BAD_REQUEST", message = "Invalid result data"))
                return@post
            }

            if (!LicenseService.canSaveProgress(user, request.testNumber)) {
                call.respond(HttpStatusCode.Forbidden, ApiError(success = false, code = "NO_ACCESS", message = "No access to this test"))
                return@post
            }

            // Результат считает сервер по ответам: присланное клиентом число правильных больше не принимается
            val outcome = ResultsRepository.saveScoredResult(user.id, request.testNumber, request.mode, request.answers)
            if (outcome == null) {
                call.respond(HttpStatusCode.NotFound, ApiError(success = false, code = "NOT_FOUND", message = "Test not found"))
                return@post
            }

            transaction {
                TestProgress.deleteWhere {
                    (TestProgress.userId eq user.id) and
                            (TestProgress.testNumber eq request.testNumber) and
                            (TestProgress.mode eq request.mode)
                }
            }

            call.respond(
                SaveResultResponse(
                    success = true,
                    message = "Result saved",
                    correctCount = outcome.correctCount,
                    totalQuestions = outcome.totalQuestions
                )
            )
        }

        get("/results/{username}") {
            val user = call.requireUser() ?: return@get
            val username = call.parameters["username"]

            if (username == null) {
                call.respond(StatsResponse(results = emptyList()))
                return@get
            }

            if (username != user.username) {
                call.respond(HttpStatusCode.Forbidden, ApiError(success = false, code = "NO_ACCESS", message = "Not your data"))
                return@get
            }

            call.respond(StatsResponse(results = ResultsRepository.getResults(username)))
        }
    }
}

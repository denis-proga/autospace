package com.autospace.app

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val username: String,
    val password: String,
    val language: String = "ru"
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class AuthResponse(
    val success: Boolean,
    val message: String,
    val licenseStatus: String? = null
)

@Serializable
data class SupportRequest(
    val username: String,
    val message: String,
    val phone: String? = null,
    val email: String? = null
)

@Serializable
data class VerifyRegistrationRequest(
    val username: String,
    val code: String
)

@Serializable
data class SaveResultRequest(
    val username: String,
    val testNumber: Int,
    val mode: String,
    // "idВопроса:номерВарианта", через запятую; номер варианта 1..4 (A..D). Результат считает сервер
    val answers: String
)

@Serializable
data class SaveResultResponse(
    val success: Boolean,
    val message: String,
    val correctCount: Int = 0,
    val totalQuestions: Int = 0
)

@Serializable
data class TestResultDto(
    val testNumber: Int,
    val mode: String,
    val correctCount: Int,
    val totalQuestions: Int,
    val completedAt: Long
)

@Serializable
data class StatsResponse(
    val results: List<TestResultDto>
)

@Serializable
data class ResendCodeRequest(
    val username: String
)

@Serializable
data class QuestionResponse(
    val id: Int,
    val imageUrl: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String?,
    val optionD: String?,
    val correctOption: String,
    val explanation: String
)

@Serializable
data class QuestionsListResponse(
    val questions: List<QuestionResponse>
)

@Serializable
data class QuestionDto(
    val id: Int,
    val imageUrl: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String?,
    val optionD: String?,
    val correctOption: String,
    val explanation: String
)

@Serializable
data class QuestionsListResponseDto(
    val questions: List<QuestionDto>
)

@Serializable
data class SaveProgressRequest(
    val username: String,
    val testNumber: Int,
    val mode: String,
    val answersData: String,
    val currentIndex: Int,
    val secondsLeft: Int? = null
)

@Serializable
data class ProgressResponse(
    val found: Boolean,
    val answersData: String? = null,
    val currentIndex: Int? = null,
    val secondsLeft: Int? = null
)

@Serializable
data class ResetProgressRequest(
    val username: String
)

@Serializable
data class SessionRequest(
    val token: String
)

/**
 * Состояние лицензии пользователя.
 * status: DEMO, ACTIVE, FRIEND, EXPIRED, BLOCKED
 * expiresAt: конец оплаченного периода (только для ACTIVE), миллисекунды
 * demoLimit: сколько тестов доступно бесплатно
 * demoTests: номера тестов, на которые уже потрачены бесплатные слоты
 */
@Serializable
data class LicenseInfo(
    val status: String,
    // почта нужна экрану профиля; приходит только владельцу токена
    val email: String = "",
    // выбранный аватар («avatar_3»); пустая строка — кружок с буквой имени
    val avatar: String = "",
    // доступ открыт как подарок на время отъезда админа: платить сейчас не нужно
    val giftAccess: Boolean = false,
    val expiresAt: Long? = null,
    val demoLimit: Int,
    val demoTests: List<Int> = emptyList(),
    val daysLeft: Int? = null
)

@Serializable
data class PlanDto(
    val days: Int,
    val priceEur: Int
)

@Serializable
data class PlansResponse(
    val plans: List<PlanDto>,
    val paymentInstructions: String,
    // режим отъезда: оплату принять некому, у прежних пользователей доступ открыт как подарок
    val awayMode: Boolean = false,
    val awayUntil: String = ""
)

@Serializable
data class PaymentClaimRequest(
    val days: Int
)

/** Заказ на оплату: его видит приложение на экране оплаты. */
@Serializable
data class PaymentOrderDto(
    val orderId: Int,
    val days: Int,
    val amountEur: Int,
    val code: String,
    val status: String,
    val createdAt: Long,
    val expiresAt: Long
)

/** Реквизиты для перевода. Значения приходят из переменных окружения, в коде их нет. */
@Serializable
data class PaymentDetailsDto(
    val bizumPhone: String = "",
    val cardNumber: String = "",
    val holderName: String = ""
)

@Serializable
data class PaymentOrderResponse(
    val success: Boolean,
    val code: String = "",
    val awayUntil: String = "",
    val order: PaymentOrderDto? = null,
    val details: PaymentDetailsDto? = null,
    val license: LicenseInfo? = null
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val licenseStatus: String? = null,
    val username: String? = null,
    val token: String? = null,
    val license: LicenseInfo? = null
)

/**
 * Ошибка с машиночитаемым кодом. Коды:
 * UNAUTHORIZED (401), BLOCKED, DEMO_LIMIT, LICENSE_EXPIRED, NO_ACCESS, BAD_REQUEST (403/400)
 */
@Serializable
data class ApiError(
    val success: Boolean,
    val code: String,
    val message: String
)

@Serializable
data class ForgotPasswordRequest(
    val identifier: String,
    val language: String = "ru"
)

@Serializable
data class ResetPasswordRequest(
    val identifier: String,
    val code: String,
    val newPassword: String
)

@Serializable
data class TicketCreateRequest(
    val category: String,
    val subject: String,
    val message: String,
    val contactEmail: String? = null,
    val language: String = "ru"
)

@Serializable
data class TicketDto(
    val id: Int,
    val category: String,
    val subject: String,
    val message: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val lastReply: String? = null,
    val lastReplyAt: Long? = null
)

@Serializable
data class TicketsResponse(
    val tickets: List<TicketDto>,
    val remaining: Int
)

@Serializable
data class TicketCreateResponse(
    val success: Boolean,
    val code: String = "",
    val ticketId: Int = 0,
    val remaining: Int = 0
)

/** Изменение профиля: имя и аватар. Любое поле можно не присылать. */
@Serializable
data class UpdateProfileRequest(
    val username: String? = null,
    val avatar: String? = null
)

@Serializable
data class UpdateProfileResponse(
    val success: Boolean,
    val code: String = "",
    val username: String = "",
    val avatar: String = ""
)

/** Последний незавершённый тест для блока «Продолжить». */
@Serializable
data class LatestProgressResponse(
    val found: Boolean,
    val testNumber: Int = 0,
    val mode: String = "LEARNING",
    val currentIndex: Int = 0,
    val answered: Int = 0,
    val updatedAt: Long = 0L
)

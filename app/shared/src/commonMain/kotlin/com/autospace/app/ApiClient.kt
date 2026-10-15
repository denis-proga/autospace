package com.autospace.app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import io.ktor.client.request.get
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.HttpStatusCode
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin

@Serializable
data class RegisterRequestDto(
    val firstName: String,
    val lastName: String,
    val email: String,
    val username: String,
    val password: String,
    val language: String
)

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String
)

@Serializable
data class AuthResponseDto(
    val success: Boolean,
    val message: String,
    val licenseStatus: String? = null
)

@Serializable
data class SupportRequestDto(
    val username: String,
    val message: String,
    val phone: String? = null,
    val email: String? = null
)

@Serializable
data class VerifyRegistrationRequestDto(
    val username: String,
    val code: String
)

@Serializable
data class SaveResultRequestDto(
    val username: String,
    val testNumber: Int,
    val mode: String,
    val correctCount: Int,
    val totalQuestions: Int
)

@Serializable
data class TestResultItemDto(
    val testNumber: Int,
    val mode: String,
    val correctCount: Int,
    val totalQuestions: Int,
    val completedAt: Long
)

@Serializable
data class StatsResponseDto(
    val results: List<TestResultItemDto>
)

@Serializable
data class ResendCodeRequestDto(
    val username: String
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
data class SaveProgressRequestDto(
    val username: String,
    val testNumber: Int,
    val mode: String,
    val answersData: String,
    val currentIndex: Int,
    val secondsLeft: Int? = null
)

@Serializable
data class ProgressResponseDto(
    val found: Boolean,
    val answersData: String? = null,
    val currentIndex: Int? = null,
    val secondsLeft: Int? = null
)

@Serializable
data class ResetProgressRequestDto(
    val username: String
)

@Serializable
data class SessionRequestDto(
    val token: String
)

@Serializable
data class LoginResponseDto(
    val success: Boolean,
    val message: String,
    val licenseStatus: String? = null,
    val username: String? = null,
    val token: String? = null,
    val license: LicenseInfoDto? = null
)

@Serializable
data class LicenseInfoDto(
    val status: String,
    val expiresAt: Long? = null,
    val demoLimit: Int,
    val demoTests: List<Int> = emptyList(),
    val daysLeft: Int? = null
)

@Serializable
data class ApiErrorDto(
    val success: Boolean = false,
    val code: String = "",
    val message: String = ""
)

@Serializable
data class PlanItemDto(
    val days: Int,
    val priceEur: Int
)

@Serializable
data class PlansResponseDto(
    val plans: List<PlanItemDto>,
    val paymentInstructions: String = ""
)

@Serializable
data class PaymentClaimRequestDto(
    val days: Int
)

@Serializable
data class ForgotPasswordRequestDto(
    val identifier: String,
    val language: String
)

@Serializable
data class ResetPasswordRequestDto(
    val identifier: String,
    val code: String,
    val newPassword: String
)

object ApiClient {
    /** Токен сессии: выставляется после входа, сбрасывается при выходе. Уходит только на наш сервер. */
    var authToken: String? = null

    private fun HttpRequestBuilder.auth() {
        authToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
    }

    // ВАЖНО: для Android-эмулятора localhost сервера — это 10.0.2.2, а не 127.0.0.1
    // Для Desktop — 127.0.0.1 подходит
    private const val BASE_URL = "https://autospace-hv2r.onrender.com"

    private val client = HttpClient {
        followRedirects = true

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
                isLenient = true
            })
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 90_000
            connectTimeoutMillis = 30_000
        }
    }

    init {
        // В браузере сетевой сбой приходит как kotlin.Error, а не Exception, и мимо catch (e: Exception)
        // молча убивает корутину. Превращаем его в обычное исключение, чтобы экраны показали ошибку.
        client.plugin(HttpSend).intercept { request ->
            try {
                execute(request)
            } catch (e: Error) {
                throw NetworkException(e.message ?: "Network error", e)
            }
        }
    }

    suspend fun saveProgress(request: SaveProgressRequestDto): AuthResponseDto {
        return client.post("$BASE_URL/save-progress") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getProgress(username: String, testNumber: Int, mode: String): ProgressResponseDto {
        return client.get("$BASE_URL/progress/$username/$testNumber/$mode") { auth() }.body()
    }

    suspend fun register(request: RegisterRequestDto): AuthResponseDto {
        return client.post("$BASE_URL/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun login(request: LoginRequestDto): LoginResponseDto {
        return client.post("$BASE_URL/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun checkSession(token: String): LoginResponseDto {
        return client.post("$BASE_URL/session") {
            contentType(ContentType.Application.Json)
            setBody(SessionRequestDto(token))
        }.body()
    }

    suspend fun sendSupportRequest(request: SupportRequestDto): AuthResponseDto {
        return client.post("$BASE_URL/support") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun verifyRegistration(request: VerifyRegistrationRequestDto): AuthResponseDto {
        return client.post("$BASE_URL/verify-registration") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun resendCode(request: ResendCodeRequestDto): AuthResponseDto {
        return client.post("$BASE_URL/resend-code") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun resetProgress(request: ResetProgressRequestDto): AuthResponseDto {
        return client.post("$BASE_URL/reset-progress") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun saveResult(request: SaveResultRequestDto): AuthResponseDto {
        return client.post("$BASE_URL/results") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getResults(username: String): StatsResponseDto {
        return client.get("$BASE_URL/results/$username") { auth() }.body()
    }

    suspend fun getQuestions(testNumber: Int, lang: String): QuestionsListResponseDto {
        val response = client.get("$BASE_URL/questions/$testNumber?lang=$lang") { auth() }
        when (response.status) {
            HttpStatusCode.NotFound -> throw QuestionsNotFoundException(testNumber)
            HttpStatusCode.Unauthorized -> throw SessionExpiredException()
            HttpStatusCode.Forbidden -> {
                val error = try {
                    response.body<ApiErrorDto>()
                } catch (_: Exception) {
                    null
                }
                throw LicenseRequiredException(error?.code ?: "NO_ACCESS")
            }
            else -> {}
        }
        return response.body()
    }

    suspend fun forgotPassword(request: ForgotPasswordRequestDto): AuthResponseDto {
        return client.post("$BASE_URL/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun resetPassword(request: ResetPasswordRequestDto): AuthResponseDto {
        return client.post("$BASE_URL/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getLicense(): LicenseInfoDto {
        val response = client.get("$BASE_URL/license") { auth() }
        if (response.status == HttpStatusCode.Unauthorized) throw SessionExpiredException()
        return response.body()
    }

    suspend fun getPlans(): PlansResponseDto {
        val response = client.get("$BASE_URL/plans") { auth() }
        if (response.status == HttpStatusCode.Unauthorized) throw SessionExpiredException()
        return response.body()
    }

    suspend fun sendPaymentClaim(request: PaymentClaimRequestDto): AuthResponseDto {
        val response = client.post("$BASE_URL/payment-claim") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        if (response.status == HttpStatusCode.Unauthorized) throw SessionExpiredException()
        return response.body()
    }

    suspend fun fetchImageBytes(url: String): ByteArray {
        return client.get(url).body()
    }
}
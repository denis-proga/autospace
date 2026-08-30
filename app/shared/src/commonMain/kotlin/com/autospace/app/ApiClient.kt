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

@Serializable
data class RegisterRequestDto(
    val firstName: String,
    val lastName: String,
    val email: String,
    val username: String,
    val password: String
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

object ApiClient {
    // ВАЖНО: для Android-эмулятора localhost сервера — это 10.0.2.2, а не 127.0.0.1
    // Для Desktop — 127.0.0.1 подходит
    private const val BASE_URL = "http://127.0.0.1:8080"

    private val client = HttpClient {
        install(ContentNegotiation) {
            json()
        }
    }

    suspend fun register(request: RegisterRequestDto): AuthResponseDto {
        return client.post("$BASE_URL/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun login(request: LoginRequestDto): AuthResponseDto {
        return client.post("$BASE_URL/login") {
            contentType(ContentType.Application.Json)
            setBody(request)
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

    suspend fun saveResult(request: SaveResultRequestDto): AuthResponseDto {
        return client.post("$BASE_URL/results") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    suspend fun getResults(username: String): StatsResponseDto {
        return client.get("$BASE_URL/results/$username").body()
    }
}
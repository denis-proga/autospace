package com.autospace.app

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable

private val resendApiKey: String = readSetting("RESEND_API_KEY")
private val resendFromAddress: String = readSetting("RESEND_FROM_ADDRESS")

@Serializable
private data class ResendEmailRequest(
    val from: String,
    val to: List<String>,
    val subject: String,
    val text: String
)

object EmailService {
    private val client = HttpClient {
        install(ContentNegotiation) { json() }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 10_000
        }
    }

    suspend fun sendVerificationCode(toEmail: String, code: String): Boolean {
        if (resendApiKey.isEmpty()) {
            println("Resend not configured, skipping email send. Code would have been: $code")
            return false
        }

        return try {
            val response = client.post("https://api.resend.com/emails") {
                header("Authorization", "Bearer $resendApiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    ResendEmailRequest(
                        from = resendFromAddress,
                        to = listOf(toEmail),
                        subject = "AutoSpace — код подтверждения",
                        text = "Ваш код подтверждения регистрации: $code\n\nЕсли вы не регистрировались в AutoSpace, просто проигнорируйте это письмо."
                    )
                )
            }
            println("Resend response: ${response.status}")
            response.status.isSuccess()
        } catch (e: Exception) {
            println("Failed to send email via Resend: ${e.message}")
            false
        }
    }
}
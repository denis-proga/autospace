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

private data class EmailTemplate(val subject: String, val body: String)

private fun emailTemplateFor(language: String, code: String): EmailTemplate {
    return when (language) {
        "en" -> EmailTemplate("AutoSpace — verification code", "Your registration verification code: $code\n\nIf you didn't register with AutoSpace, just ignore this email.")
        "es" -> EmailTemplate("AutoSpace — código de verificación", "Tu código de verificación de registro: $code\n\nSi no te registraste en AutoSpace, ignora este correo.")
        "de" -> EmailTemplate("AutoSpace — Bestätigungscode", "Ihr Bestätigungscode für die Registrierung: $code\n\nWenn Sie sich nicht bei AutoSpace registriert haben, ignorieren Sie diese E-Mail einfach.")
        "uk" -> EmailTemplate("AutoSpace — код підтвердження", "Ваш код підтвердження реєстрації: $code\n\nЯкщо ви не реєструвалися в AutoSpace, просто проігноруйте цей лист.")
        else -> EmailTemplate("AutoSpace — код подтверждения", "Ваш код подтверждения регистрации: $code\n\nЕсли вы не регистрировались в AutoSpace, просто проигнорируйте это письмо.")
    }
}

private fun resetTemplateFor(language: String, code: String): EmailTemplate {
    return when (language) {
        "en" -> EmailTemplate("AutoSpace — password reset code", "Your password reset code: $code\n\nThe code is valid for 15 minutes. If you didn't request a reset, just ignore this email — your password will stay the same.")
        "es" -> EmailTemplate("AutoSpace — código para restablecer la contraseña", "Tu código para restablecer la contraseña: $code\n\nEl código es válido durante 15 minutos. Si no lo has solicitado, ignora este correo: tu contraseña no cambiará.")
        "de" -> EmailTemplate("AutoSpace — Code zum Zurücksetzen des Passworts", "Ihr Code zum Zurücksetzen des Passworts: $code\n\nDer Code ist 15 Minuten gültig. Wenn Sie das nicht angefordert haben, ignorieren Sie diese E-Mail — Ihr Passwort bleibt unverändert.")
        "uk" -> EmailTemplate("AutoSpace — код для скидання пароля", "Ваш код для скидання пароля: $code\n\nКод дійсний 15 хвилин. Якщо ви не робили цей запит, просто проігноруйте лист — пароль не зміниться.")
        else -> EmailTemplate("AutoSpace — код для сброса пароля", "Ваш код для сброса пароля: $code\n\nКод действует 15 минут. Если вы не запрашивали сброс, просто проигнорируйте это письмо — пароль не изменится.")
    }
}

object EmailService {
    private val client = HttpClient {
        install(ContentNegotiation) { json() }
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 10_000
        }
    }

    suspend fun sendVerificationCode(toEmail: String, code: String, language: String = "ru"): Boolean {
        return send(toEmail, emailTemplateFor(language, code), code)
    }

    suspend fun sendPasswordResetCode(toEmail: String, code: String, language: String = "ru"): Boolean {
        return send(toEmail, resetTemplateFor(language, code), code)
    }

    private suspend fun send(toEmail: String, template: EmailTemplate, codeForLog: String): Boolean {
        if (resendApiKey.isEmpty()) {
            println("Resend not configured, skipping email send. Code would have been: $codeForLog")
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
                        subject = template.subject,
                        text = template.body
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
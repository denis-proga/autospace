package com.autospace.app

import jakarta.mail.Authenticator
import jakarta.mail.Message
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import java.util.Properties

private val gmailAddress: String = dotenv["GMAIL_ADDRESS"] ?: ""
private val gmailAppPassword: String = dotenv["GMAIL_APP_PASSWORD"] ?: ""

object EmailService {
    fun sendVerificationCode(toEmail: String, code: String) {
        if (gmailAddress.isEmpty() || gmailAppPassword.isEmpty()) {
            println("Gmail not configured, skipping email send. Code would have been: $code")
            return
        }

        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(gmailAddress, gmailAppPassword)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(gmailAddress))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "AutoSpace — код подтверждения"
                setText("Ваш код подтверждения регистрации: $code\n\nЕсли вы не регистрировались в AutoSpace, просто проигнорируйте это письмо.")
            }
            Transport.send(message)
            println("Verification email sent to $toEmail")
        } catch (e: Exception) {
            println("Failed to send email: ${e.message}")
        }
    }
}
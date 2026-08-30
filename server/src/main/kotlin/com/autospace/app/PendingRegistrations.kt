package com.autospace.app

import java.util.concurrent.ConcurrentHashMap

data class PendingRegistration(
    val firstName: String,
    val lastName: String,
    val email: String,
    val username: String,
    val password: String,
    val code: String,
    val createdAt: Long,
    val lastSentAt: Long = createdAt,
    val resendCount: Int = 0,
    var attempts: Int = 0
)

sealed class ResendResult {
    data class Success(val email: String, val code: String) : ResendResult()
    data class Cooldown(val secondsLeft: Long) : ResendResult()
    object TooManyAttempts : ResendResult()
    object NotFound : ResendResult()
}

object PendingRegistrations {
    private val store = ConcurrentHashMap<String, PendingRegistration>()
    private const val CODE_TTL_MILLIS = 15 * 60 * 1000L // 15 минут
    private const val MAX_ATTEMPTS = 5
    private const val RESEND_COOLDOWN_MILLIS = 60 * 1000L // 60 секунд
    private const val MAX_RESENDS = 3

    fun generateCode(): String {
        return (100000..999999).random().toString()
    }

    fun create(
        firstName: String,
        lastName: String,
        email: String,
        username: String,
        password: String
    ): String {
        val code = generateCode()
        store[username] = PendingRegistration(
            firstName = firstName,
            lastName = lastName,
            email = email,
            username = username,
            password = password,
            code = code,
            createdAt = System.currentTimeMillis()
        )
        return code
    }

    fun verify(username: String, code: String): PendingRegistration? {
        val pending = store[username] ?: return null

        if (System.currentTimeMillis() - pending.createdAt > CODE_TTL_MILLIS) {
            store.remove(username)
            return null
        }

        pending.attempts++
        if (pending.attempts > MAX_ATTEMPTS) {
            store.remove(username)
            return null
        }

        if (pending.code != code) {
            return null
        }

        store.remove(username)
        return pending
    }

    fun resend(username: String): ResendResult {
        val pending = store[username] ?: return ResendResult.NotFound

        val elapsedSinceLastSent = System.currentTimeMillis() - pending.lastSentAt
        if (elapsedSinceLastSent < RESEND_COOLDOWN_MILLIS) {
            val secondsLeft = (RESEND_COOLDOWN_MILLIS - elapsedSinceLastSent) / 1000 + 1
            return ResendResult.Cooldown(secondsLeft)
        }

        if (pending.resendCount >= MAX_RESENDS) {
            return ResendResult.TooManyAttempts
        }

        val newCode = generateCode()
        store[username] = pending.copy(
            code = newCode,
            createdAt = System.currentTimeMillis(),
            lastSentAt = System.currentTimeMillis(),
            resendCount = pending.resendCount + 1,
            attempts = 0
        )

        return ResendResult.Success(pending.email, newCode)
    }
}
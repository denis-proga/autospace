package com.autospace.app

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class PendingRegistration(
    val firstName: String,
    val lastName: String,
    val email: String,
    val username: String,
    val password: String,
    val code: String,
    val createdAt: Long,
    var attempts: Int = 0
)

object PendingRegistrations {
    private val store = ConcurrentHashMap<String, PendingRegistration>()
    private const val CODE_TTL_MILLIS = 15 * 60 * 1000L // 15 минут
    private const val MAX_ATTEMPTS = 5

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
}
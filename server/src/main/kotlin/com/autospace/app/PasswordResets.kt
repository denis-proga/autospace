package com.autospace.app

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

data class ResetTarget(val id: Int, val email: String)

private data class ResetEntry(
    val code: String,
    val createdAt: Long,
    val attempts: Int,
    val sentAt: List<Long>
)

/**
 * Коды сброса пароля. Хранятся в памяти, как и неподтверждённые регистрации:
 * после перезапуска сервера код нужно запросить заново.
 */
object PasswordResets {
    private const val CODE_TTL_MS = 15 * 60 * 1000L
    private const val MAX_ATTEMPTS = 5
    private const val MIN_GAP_MS = 60 * 1000L
    private const val MAX_SENDS_PER_HOUR = 5
    private const val HOUR_MS = 60 * 60 * 1000L

    private val store = ConcurrentHashMap<Int, ResetEntry>()
    private val random = SecureRandom()

    /** Ищет пользователя по логину или по почте. Нужен и для запроса кода, и для смены пароля. */
    fun findUser(identifier: String): ResetTarget? {
        if (identifier.isBlank() || identifier.length > 150) return null

        return transaction<ResetTarget?> {
            var row = Users.selectAll().where { Users.username eq identifier }.firstOrNull()

            if (row == null && identifier.contains("@")) {
                val key = emailKeyOf(identifier)
                row = Users.selectAll().where { Users.emailKey eq key }.firstOrNull()
            }

            row?.let { ResetTarget(it[Users.id], it[Users.email]) }
        }
    }

    /**
     * Выпускает новый код. null, если запрос слишком частый
     * (не чаще раза в минуту и не больше пяти писем в час на одного пользователя).
     * Защищает почту пользователя от спама с чужих запросов.
     */
    @Synchronized
    fun issueCode(userId: Int): String? {
        val now = System.currentTimeMillis()
        val recent = store[userId]?.sentAt?.filter { now - it < HOUR_MS } ?: emptyList()

        if (recent.isNotEmpty() && now - recent.last() < MIN_GAP_MS) return null
        if (recent.size >= MAX_SENDS_PER_HOUR) return null

        val code = (random.nextInt(900000) + 100000).toString()
        store[userId] = ResetEntry(code = code, createdAt = now, attempts = 0, sentAt = recent + now)
        return code
    }

    /** Проверяет код. Пять неверных попыток или 15 минут делают код недействительным. */
    @Synchronized
    fun verify(userId: Int, code: String): Boolean {
        val entry = store[userId] ?: return false
        val now = System.currentTimeMillis()

        if (now - entry.createdAt > CODE_TTL_MS || entry.attempts >= MAX_ATTEMPTS) {
            // история отправок остаётся, чтобы лимит писем нельзя было обойти
            store[userId] = entry.copy(createdAt = 0L)
            return false
        }

        if (entry.code != code.trim()) {
            store[userId] = entry.copy(attempts = entry.attempts + 1)
            return false
        }

        store.remove(userId)
        return true
    }

    /** Меняет пароль и сбрасывает токен: на всех устройствах придётся войти заново. */
    fun setNewPassword(userId: Int, passwordHash: String) {
        transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.passwordHash] = passwordHash
                it[Users.sessionToken] = null
            }
        }
    }
}

package com.autospace.app

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

/**
 * Режим отъезда. Пока он включён, оплату принять некому: новые заказы не создаются, а на экране
 * тарифов человек видит сообщение вместо кнопки. Тем, кто зарегистрировался ДО включения режима,
 * доступ ко всем тестам открыт как подарок — чтобы никто не остался и без оплаты, и без тестов.
 *
 * Состояние лежит в базе, а не в памяти: деплой и перезапуск Render его не сбрасывают.
 * Включается и выключается из Telegram, то есть с телефона и без ноутбука.
 */
object AwayMode {
    private const val KEY = "away"

    data class State(val enabled: Boolean, val since: Long, val until: String)

    private var cached: State? = null

    fun state(): State {
        cached?.let { return it }
        val loaded = transaction {
            val row = AppSettings.selectAll().where { AppSettings.key eq KEY }.singleOrNull()
            if (row == null) {
                State(enabled = false, since = 0L, until = "")
            } else {
                State(
                    enabled = row[AppSettings.value] == "on",
                    since = row[AppSettings.updatedAt],
                    until = row[AppSettings.extra] ?: ""
                )
            }
        }
        cached = loaded
        return loaded
    }

    fun isOn(): Boolean = state().enabled

    /** until — дата возвращения, как её ввёл админ (например «25.10»); пустая строка, если даты нет. */
    fun turnOn(until: String): State = write(enabled = true, until = until)

    fun turnOff(): State = write(enabled = false, until = "")

    private fun write(enabled: Boolean, until: String): State {
        val now = System.currentTimeMillis()
        transaction {
            val exists = AppSettings.selectAll().where { AppSettings.key eq KEY }.any()
            if (exists) {
                AppSettings.update({ AppSettings.key eq KEY }) {
                    it[AppSettings.value] = if (enabled) "on" else "off"
                    it[AppSettings.extra] = until
                    it[AppSettings.updatedAt] = now
                }
            } else {
                AppSettings.insert {
                    it[AppSettings.key] = KEY
                    it[AppSettings.value] = if (enabled) "on" else "off"
                    it[AppSettings.extra] = until
                    it[AppSettings.updatedAt] = now
                }
            }
        }
        // при выключении момент включения больше не нужен, но пусть останется для истории
        val next = State(enabled = enabled, since = now, until = until)
        cached = next
        return next
    }

    /**
     * Подарочный доступ: только тем, кто зарегистрирован до включения режима.
     * Регистрация во время отъезда подарка не даёт, иначе доступ получал бы кто угодно.
     */
    fun giftAccess(userId: Int): Boolean {
        val s = state()
        if (!s.enabled) return false
        return transaction {
            val row = Users.selectAll().where { Users.id eq userId }.singleOrNull() ?: return@transaction false
            val created = row[Users.createdAt]
            created == null || created <= s.since
        }
    }
}

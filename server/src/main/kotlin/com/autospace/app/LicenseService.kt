package com.autospace.app

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Приводит почту к виду, в котором разные написания одного ящика совпадают:
 * регистр, "+метка" и точки в gmail не учитываются.
 */
fun normalizeEmail(email: String): String {
    val e = email.trim().lowercase()
    val at = e.lastIndexOf('@')
    if (at <= 0) return e

    var local = e.substring(0, at)
    var domain = e.substring(at + 1)

    if (domain == "googlemail.com") domain = "gmail.com"

    val plus = local.indexOf('+')
    if (plus > 0) local = local.substring(0, plus)

    if (domain == "gmail.com") local = local.replace(".", "")

    return "$local@$domain"
}

/** SHA-256 нормализованной почты в hex (64 символа). Саму почту в слотах не храним. */
fun emailKeyOf(email: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(normalizeEmail(email).toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

data class AuthUser(
    val id: Int,
    val username: String,
    val email: String,
    val emailKey: String,
    val status: String,
    val expiresAt: Long?
)

sealed class TestAccess {
    object Granted : TestAccess()
    object Blocked : TestAccess()
    data class Denied(val code: String) : TestAccess() // DEMO_LIMIT или LICENSE_EXPIRED
}

object LicenseService {
    const val DEMO_TEST_LIMIT = 2
    private const val DAY_MS = 24L * 60 * 60 * 1000

    /** Тарифы. Цены меняются здесь, клиент получает их с сервера через GET /plans. */
    val PLANS = listOf(
        PlanDto(days = 1, priceEur = 3),
        PlanDto(days = 7, priceEur = 10),
        PlanDto(days = 15, priceEur = 15),
        PlanDto(days = 30, priceEur = 25)
    )

    fun hasFullAccess(status: String): Boolean = status == "ACTIVE" || status == "FRIEND"

    /** Находит пользователя по токену. Истёкшую ACTIVE-лицензию переводит в EXPIRED прямо здесь. */
    fun authenticate(authHeader: String?): AuthUser? {
        val token = authHeader?.removePrefix("Bearer ")?.trim()
        if (token.isNullOrBlank()) return null
        return authenticateToken(token)
    }

    fun authenticateToken(token: String): AuthUser? {
        return transaction<AuthUser?> {
            val row = Users.selectAll()
                .where { Users.sessionToken eq token }
                .singleOrNull() ?: return@transaction null

            var status = row[Users.licenseStatus]
            val expiresAt = row[Users.licenseExpiresAt]

            if (status == "ACTIVE" && expiresAt != null && expiresAt < System.currentTimeMillis()) {
                status = "EXPIRED"
                Users.update({ Users.id eq row[Users.id] }) {
                    it[Users.licenseStatus] = "EXPIRED"
                }
            }

            AuthUser(
                id = row[Users.id],
                username = row[Users.username],
                email = row[Users.email],
                emailKey = row[Users.emailKey] ?: emailKeyOf(row[Users.email]),
                status = status,
                expiresAt = expiresAt
            )
        }
    }

    fun demoTests(emailKey: String): List<Int> {
        return transaction {
            DemoSlots.selectAll()
                .where { DemoSlots.emailKey eq emailKey }
                .orderBy(DemoSlots.claimedAt)
                .map { it[DemoSlots.testNumber] }
        }
    }

    fun hasProgress(userId: Int, testNumber: Int): Boolean {
        return transaction {
            TestProgress.selectAll()
                .where { (TestProgress.userId eq userId) and (TestProgress.testNumber eq testNumber) }
                .any()
        }
    }

    fun licenseInfo(user: AuthUser): LicenseInfo {
        val expiry = user.expiresAt
        val daysLeft: Int? = if (user.status == "ACTIVE" && expiry != null) {
            val ms = expiry - System.currentTimeMillis()
            if (ms <= 0) 0 else ((ms + DAY_MS - 1) / DAY_MS).toInt()
        } else null

        return LicenseInfo(
            status = user.status,
            email = user.email,
            giftAccess = AwayMode.giftAccess(user.id),
            expiresAt = user.expiresAt,
            demoLimit = DEMO_TEST_LIMIT,
            demoTests = demoTests(user.emailKey),
            daysLeft = daysLeft
        )
    }

    /**
     * Открытие теста. Для платных пользователей всегда разрешено.
     * Для остальных: тест уже в бесплатных слотах, либо есть начатый прогресс
     * (можно дописать после окончания лицензии), либо свободный слот занимается сейчас.
     * Слот расходуется при открытии, а не при завершении, иначе его можно не тратить, выходя из теста.
     */
    fun requestTestAccess(user: AuthUser, testNumber: Int): TestAccess {
        if (user.status == "BLOCKED") return TestAccess.Blocked
        if (hasFullAccess(user.status)) return TestAccess.Granted
        // пока админ в отъезде, прежние пользователи получают все тесты как подарок
        if (AwayMode.giftAccess(user.id)) return TestAccess.Granted

        return transaction<TestAccess> {
            // блокируем строку пользователя, чтобы два запроса одновременно не заняли три слота
            Users.selectAll().where { Users.id eq user.id }.forUpdate().singleOrNull()

            val held = DemoSlots.selectAll()
                .where { DemoSlots.emailKey eq user.emailKey }
                .map { it[DemoSlots.testNumber] }

            val progressExists = TestProgress.selectAll()
                .where { (TestProgress.userId eq user.id) and (TestProgress.testNumber eq testNumber) }
                .any()

            when {
                testNumber in held || progressExists -> TestAccess.Granted
                user.status == "EXPIRED" -> TestAccess.Denied("LICENSE_EXPIRED")
                held.size >= DEMO_TEST_LIMIT -> TestAccess.Denied("DEMO_LIMIT")
                else -> {
                    DemoSlots.insert {
                        it[DemoSlots.emailKey] = user.emailKey
                        it[DemoSlots.testNumber] = testNumber
                        it[DemoSlots.claimedAt] = System.currentTimeMillis()
                    }
                    TestAccess.Granted
                }
            }
        }
    }

    /**
     * Сохранять прогресс можно только в тесте, к которому есть доступ.
     * Иначе прогресс можно было бы создать заранее и получить вопросы без слота.
     */
    fun canSaveProgress(user: AuthUser, testNumber: Int): Boolean {
        if (user.status == "BLOCKED") return false
        if (hasFullAccess(user.status)) return true
        if (AwayMode.giftAccess(user.id)) return true
        return testNumber in demoTests(user.emailKey) || hasProgress(user.id, testNumber)
    }
}

/** Простой счётчик запросов в памяти: не больше maxHits за окно windowMs на ключ. */
object RateLimiter {
    private val hits = ConcurrentHashMap<String, MutableList<Long>>()

    fun allow(key: String, maxHits: Int, windowMs: Long): Boolean {
        val now = System.currentTimeMillis()

        if (hits.size > 5000) {
            hits.entries.removeIf { entry ->
                synchronized(entry.value) {
                    entry.value.isEmpty() || now - entry.value.last() > 3_600_000L
                }
            }
        }

        val list = hits.computeIfAbsent(key) { mutableListOf() }
        return synchronized(list) {
            list.removeAll { now - it > windowMs }
            if (list.size >= maxHits) {
                false
            } else {
                list.add(now)
                true
            }
        }
    }
}

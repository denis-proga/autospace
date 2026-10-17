package com.autospace.app

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.lowerCase
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.time.Instant
import java.time.ZoneId

object UserRepository {
    private const val DAY_MS = 24L * 60 * 60 * 1000
    private const val CLAIM_TTL_MS = 48L * 60 * 60 * 1000
    private val adminZone: ZoneId = ZoneId.of("Europe/Madrid")

    private fun formatDate(ms: Long): String =
        Instant.ofEpochMilli(ms).atZone(adminZone).toLocalDate().toString()

    /** Выдать или продлить платный доступ. Если подписка ещё действует, дни добавляются к её концу. */
    /** Выбранный аватар пользователя; пустая строка — кружок с буквой имени. */
    fun avatarOf(userId: Int): String = transaction {
        Users.selectAll().where { Users.id eq userId }.singleOrNull()?.get(Users.avatar).orEmpty()
    }

    /**
     * Меняет имя и аватар. Имя проверяется на длину, допустимые символы и занятость:
     * результаты, прогресс и обращения привязаны к id, поэтому переименование ничего не теряет.
     * Возвращает код ошибки или пустую строку при успехе.
     */
    fun updateProfile(userId: Int, newUsername: String?, newAvatar: String?): String = transaction {
        if (newUsername != null) {
            val name = newUsername.trim()
            if (name.length < 3 || name.length > 20) return@transaction "BAD_USERNAME"
            if (!name.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }) return@transaction "BAD_USERNAME"

            val taken = Users.selectAll().where { Users.username.lowerCase() eq name.lowercase() }
                .any { it[Users.id] != userId }
            if (taken) return@transaction "USERNAME_TAKEN"

            Users.update({ Users.id eq userId }) { it[Users.username] = name }
        }

        if (newAvatar != null) {
            val avatar = newAvatar.trim()
            val allowed = avatar.isEmpty() || avatar in listOf("avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_5", "avatar_6")
            if (!allowed) return@transaction "BAD_AVATAR"
            Users.update({ Users.id eq userId }) { it[Users.avatar] = avatar }
        }

        ""
    }

    fun grantDays(userId: Int, days: Int): String {
        return transaction<String> {
            val row = Users.selectAll().where { Users.id eq userId }.singleOrNull()
                ?: return@transaction "Пользователь #$userId не найден"

            val status = row[Users.licenseStatus]
            if (status == "BLOCKED") return@transaction "Пользователь #$userId заблокирован. Сначала /unblock $userId"
            if (status == "FRIEND") return@transaction "Пользователь #$userId уже FRIEND (доступ без срока)"

            val now = System.currentTimeMillis()
            val currentExpiry = row[Users.licenseExpiresAt]
            val base = if (status == "ACTIVE" && currentExpiry != null && currentExpiry > now) currentExpiry else now
            val newExpiry = base + days.toLong() * DAY_MS

            Users.update({ Users.id eq userId }) {
                it[Users.licenseStatus] = "ACTIVE"
                it[Users.licenseExpiresAt] = newExpiry
            }

            "✅ #$userId ${row[Users.username]}: доступ до ${formatDate(newExpiry)} (+$days дн.)"
        }
    }

    fun setFriend(userId: Int): String {
        return transaction<String> {
            val row = Users.selectAll().where { Users.id eq userId }.singleOrNull()
                ?: return@transaction "Пользователь #$userId не найден"

            Users.update({ Users.id eq userId }) {
                it[Users.licenseStatus] = "FRIEND"
                it[Users.licenseExpiresAt] = null
            }
            "✅ #$userId ${row[Users.username]}: статус FRIEND, доступ без срока"
        }
    }

    /** Снять платный доступ или FRIEND: пользователь видит тарифы, бесплатные слоты не возвращаются. */
    fun revokeAccess(userId: Int): String {
        return transaction<String> {
            val row = Users.selectAll().where { Users.id eq userId }.singleOrNull()
                ?: return@transaction "Пользователь #$userId не найден"

            if (row[Users.licenseStatus] == "BLOCKED") return@transaction "Пользователь #$userId заблокирован"

            Users.update({ Users.id eq userId }) {
                it[Users.licenseStatus] = "EXPIRED"
                it[Users.licenseExpiresAt] = System.currentTimeMillis()
            }
            "🔻 #$userId ${row[Users.username]}: доступ снят (EXPIRED)"
        }
    }

    fun blockUser(userId: Int): Boolean {
        return transaction<Boolean> {
            Users.update({ Users.id eq userId }) {
                it[Users.licenseStatus] = "BLOCKED"
            } > 0
        }
    }

    fun unblockUser(userId: Int): String {
        return transaction<String> {
            val row = Users.selectAll().where { Users.id eq userId }.singleOrNull()
                ?: return@transaction "Пользователь #$userId не найден"

            if (row[Users.licenseStatus] != "BLOCKED") return@transaction "Пользователь #$userId не заблокирован"

            val expiry = row[Users.licenseExpiresAt]
            val newStatus = if (expiry != null && expiry > System.currentTimeMillis()) "ACTIVE" else "DEMO"

            Users.update({ Users.id eq userId }) {
                it[Users.licenseStatus] = newStatus
            }
            "✅ #$userId ${row[Users.username]}: разблокирован, статус $newStatus"
        }
    }

    /** Вернуть пользователю бесплатные тесты (например, если слот потратился по ошибке). */
    fun resetDemo(userId: Int): String {
        return transaction<String> {
            val row = Users.selectAll().where { Users.id eq userId }.singleOrNull()
                ?: return@transaction "Пользователь #$userId не найден"

            val key = row[Users.emailKey] ?: emailKeyOf(row[Users.email])
            val removed = DemoSlots.deleteWhere { DemoSlots.emailKey eq key }
            "♻️ #$userId ${row[Users.username]}: бесплатные тесты сброшены (освобождено слотов: $removed)"
        }
    }

    fun listUsers(): List<String> {
        return transaction<List<String>> {
            val now = System.currentTimeMillis()
            val slotCounts = DemoSlots.selectAll()
                .groupBy { it[DemoSlots.emailKey] }
                .mapValues { it.value.size }

            Users.selectAll().map { row ->
                val status = row[Users.licenseStatus]
                val expires = row[Users.licenseExpiresAt]

                val extra = when {
                    status == "ACTIVE" && expires != null -> {
                        val daysLeft = (expires - now) / DAY_MS
                        if (daysLeft >= 0) ", осталось $daysLeft дн." else ", срок истёк"
                    }
                    status == "DEMO" -> {
                        val key = row[Users.emailKey] ?: emailKeyOf(row[Users.email])
                        ", демо: ${slotCounts[key] ?: 0}/${LicenseService.DEMO_TEST_LIMIT}"
                    }
                    else -> ""
                }
                "#${row[Users.id]} ${row[Users.username]} — $status$extra"
            }
        }
    }

    /** Создаёт заявку «я оплатил». null, если у пользователя уже есть свежая необработанная заявка. */
    fun createPaymentClaim(userId: Int, days: Int, amountEur: Int): Int? {
        return transaction<Int?> {
            val now = System.currentTimeMillis()
            val hasPending = PaymentClaims.selectAll()
                .where { (PaymentClaims.userId eq userId) and (PaymentClaims.status eq "PENDING") }
                .any { it[PaymentClaims.createdAt] > now - CLAIM_TTL_MS }

            if (hasPending) return@transaction null

            PaymentClaims.insert {
                it[PaymentClaims.userId] = userId
                it[PaymentClaims.days] = days
                it[PaymentClaims.amountEur] = amountEur
                it[PaymentClaims.status] = "PENDING"
                it[PaymentClaims.createdAt] = now
            }[PaymentClaims.id]
        }
    }

    /** Подтверждение заявки из Telegram: выдаёт доступ и закрывает заявку. Повторное нажатие ничего не делает. */
    fun approveClaim(claimId: Int): String {
        val claim = transaction<Triple<Int, Int, String>?> {
            PaymentClaims.selectAll().where { PaymentClaims.id eq claimId }.singleOrNull()?.let {
                Triple(it[PaymentClaims.userId], it[PaymentClaims.days], it[PaymentClaims.status])
            }
        } ?: return "Заявка #$claimId не найдена"

        if (claim.third != "PENDING") return "Заявка #$claimId уже обработана (${claim.third})"

        val result = grantDays(claim.first, claim.second)
        if (result.startsWith("✅")) {
            transaction {
                PaymentClaims.update({ PaymentClaims.id eq claimId }) {
                    it[PaymentClaims.status] = "APPROVED"
                }
            }
        }
        return result
    }

    fun declineClaim(claimId: Int): String {
        return transaction<String> {
            val row = PaymentClaims.selectAll().where { PaymentClaims.id eq claimId }.singleOrNull()
                ?: return@transaction "Заявка #$claimId не найдена"

            if (row[PaymentClaims.status] != "PENDING") {
                return@transaction "Заявка #$claimId уже обработана (${row[PaymentClaims.status]})"
            }

            PaymentClaims.update({ PaymentClaims.id eq claimId }) {
                it[PaymentClaims.status] = "DECLINED"
            }
            "❌ Заявка #$claimId отклонена"
        }
    }

    /** Удаляет пользователя вместе с его результатами и прогрессом (иначе мешает внешний ключ). */
    fun deleteUser(userId: Int): Boolean {
        return transaction<Boolean> {
            PaymentClaims.deleteWhere { PaymentClaims.userId eq userId }
            UserAnswers.deleteWhere { UserAnswers.userId eq userId }
            SupportTickets.deleteWhere { SupportTickets.userId eq userId }
            TestResults.deleteWhere { TestResults.userId eq userId }
            TestProgress.deleteWhere { TestProgress.userId eq userId }
            Users.deleteWhere { Users.id eq userId } > 0
        }
    }
}

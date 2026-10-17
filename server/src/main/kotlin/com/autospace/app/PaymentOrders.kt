package com.autospace.app

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import kotlin.random.Random

/**
 * Заказы на оплату. Заказ создаётся ДО оплаты: у него есть код платежа, который человек пишет
 * в назначении перевода, и срок жизни. Приложение опрашивает статус и само открывает доступ,
 * когда админ подтвердил поступление денег.
 *
 * Статусы: PENDING (ждёт оплаты), PAID (подтверждён, дни выданы), EXPIRED (истёк срок),
 * CANCELLED (отменён пользователем), DECLINED (админ отклонил).
 *
 * Та же точка подтверждения (markPaid) потом вызовется из webhook платёжного провайдера,
 * поэтому переходить на него можно будет без переделки экранов.
 */
object PaymentOrders {
    const val TTL_MS = 30 * 60 * 1000L
    private const val MAX_ACTIVE = 1

    /** Буквы и цифры без похожих друг на друга: ноль и O, единица и I. */
    private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

    private fun generateCode(): String {
        val body = (1..4).map { CODE_ALPHABET[Random.nextInt(CODE_ALPHABET.length)] }.joinToString("")
        return "AS-$body"
    }

    /** Переводит просроченные заказы в EXPIRED. Вызывается перед каждым чтением. */
    private fun expireOld() {
        val now = System.currentTimeMillis()
        PaymentClaims.update({ (PaymentClaims.status eq "PENDING") and (PaymentClaims.expiresAt lessEq now) }) {
            it[PaymentClaims.status] = "EXPIRED"
        }
    }

    private fun rowToDto(row: org.jetbrains.exposed.sql.ResultRow): PaymentOrderDto = PaymentOrderDto(
        orderId = row[PaymentClaims.id],
        days = row[PaymentClaims.days],
        amountEur = row[PaymentClaims.amountEur],
        code = row[PaymentClaims.paymentCode] ?: "",
        status = row[PaymentClaims.status],
        createdAt = row[PaymentClaims.createdAt],
        expiresAt = row[PaymentClaims.expiresAt] ?: (row[PaymentClaims.createdAt] + TTL_MS)
    )

    /** Активный (ожидающий оплаты) заказ пользователя, если он есть. */
    fun activeFor(userId: Int): PaymentOrderDto? = transaction {
        expireOld()
        PaymentClaims.selectAll()
            .where { (PaymentClaims.userId eq userId) and (PaymentClaims.status eq "PENDING") }
            .orderBy(PaymentClaims.createdAt, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.let { rowToDto(it) }
    }

    /** Заказ по номеру, только если он принадлежит этому пользователю. */
    fun byIdFor(orderId: Int, userId: Int): PaymentOrderDto? = transaction {
        expireOld()
        PaymentClaims.selectAll()
            .where { (PaymentClaims.id eq orderId) and (PaymentClaims.userId eq userId) }
            .singleOrNull()
            ?.let { rowToDto(it) }
    }

    /**
     * Создаёт заказ. Если у пользователя уже есть неоплаченный заказ, возвращает его же:
     * так человек не плодит заказы, нажимая кнопку несколько раз.
     */
    fun create(userId: Int, days: Int, amountEur: Int): PaymentOrderDto? = transaction {
        expireOld()

        val existing = PaymentClaims.selectAll()
            .where { (PaymentClaims.userId eq userId) and (PaymentClaims.status eq "PENDING") }
            .orderBy(PaymentClaims.createdAt, SortOrder.DESC)
            .limit(MAX_ACTIVE)
            .singleOrNull()
        if (existing != null) return@transaction rowToDto(existing)

        val now = System.currentTimeMillis()
        // код должен быть уникальным среди живых заказов: по нему админ находит платёж
        var code = generateCode()
        var attempts = 0
        while (attempts < 10 && PaymentClaims.selectAll().where {
                (PaymentClaims.paymentCode eq code) and (PaymentClaims.status eq "PENDING")
            }.any()
        ) {
            code = generateCode()
            attempts++
        }

        val id = PaymentClaims.insert {
            it[PaymentClaims.userId] = userId
            it[PaymentClaims.days] = days
            it[PaymentClaims.amountEur] = amountEur
            it[PaymentClaims.status] = "PENDING"
            it[PaymentClaims.createdAt] = now
            it[PaymentClaims.paymentCode] = code
            it[PaymentClaims.expiresAt] = now + TTL_MS
        } get PaymentClaims.id

        PaymentOrderDto(
            orderId = id,
            days = days,
            amountEur = amountEur,
            code = code,
            status = "PENDING",
            createdAt = now,
            expiresAt = now + TTL_MS
        )
    }

    /** Отмена пользователем. Отменить можно только свой и только неоплаченный заказ. */
    fun cancel(userId: Int, orderId: Int): Boolean = transaction {
        PaymentClaims.update({
            (PaymentClaims.id eq orderId) and (PaymentClaims.userId eq userId) and (PaymentClaims.status eq "PENDING")
        }) {
            it[PaymentClaims.status] = "CANCELLED"
        } > 0
    }

    /**
     * Подтверждение оплаты. Дни выдаются ровно один раз: повторное подтверждение ничего не удваивает.
     * Истёкший заказ подтвердить можно (деньги уже пришли, человек просто не успел в 30 минут).
     */
    fun markPaid(orderId: Int): PaymentConfirmResult {
        val order = transaction {
            PaymentClaims.selectAll().where { PaymentClaims.id eq orderId }.singleOrNull()?.let {
                PaymentOrderRow(
                    userId = it[PaymentClaims.userId],
                    days = it[PaymentClaims.days],
                    amountEur = it[PaymentClaims.amountEur],
                    status = it[PaymentClaims.status],
                    code = it[PaymentClaims.paymentCode] ?: ""
                )
            }
        } ?: return PaymentConfirmResult(false, "Заказ #$orderId не найден")

        if (order.status == "PAID") return PaymentConfirmResult(false, "Заказ #$orderId уже оплачен")

        val granted = UserRepository.grantDays(order.userId, order.days)
        if (!granted.startsWith("✅")) return PaymentConfirmResult(false, granted)

        transaction {
            PaymentClaims.update({ PaymentClaims.id eq orderId }) {
                it[PaymentClaims.status] = "PAID"
                it[PaymentClaims.paidAt] = System.currentTimeMillis()
            }
        }
        return PaymentConfirmResult(true, granted, userId = order.userId, days = order.days, amountEur = order.amountEur)
    }

    fun decline(orderId: Int): String = transaction {
        val row = PaymentClaims.selectAll().where { PaymentClaims.id eq orderId }.singleOrNull()
            ?: return@transaction "Заказ #$orderId не найден"
        if (row[PaymentClaims.status] == "PAID") return@transaction "Заказ #$orderId уже оплачен, отклонить нельзя"

        PaymentClaims.update({ PaymentClaims.id eq orderId }) {
            it[PaymentClaims.status] = "DECLINED"
        }
        "❌ Заказ #$orderId отклонён"
    }

    /** Список заказов, ждущих оплаты: для команды /orders. */
    fun pendingList(): String = transaction {
        expireOld()
        val rows = PaymentClaims.selectAll()
            .where { PaymentClaims.status eq "PENDING" }
            .orderBy(PaymentClaims.createdAt, SortOrder.DESC)
            .limit(20)
            .toList()
        if (rows.isEmpty()) return@transaction "Заказов, ждущих оплаты, нет"

        rows.joinToString("\n") { row ->
            val username = Users.selectAll()
                .where { Users.id eq row[PaymentClaims.userId] }
                .singleOrNull()
                ?.get(Users.username) ?: "?"
            "#${row[PaymentClaims.id]} ${row[PaymentClaims.paymentCode] ?: "—"} · " +
                "${row[PaymentClaims.days]} дн. ${row[PaymentClaims.amountEur]} € · $username"
        }
    }

    /** Почта пользователя: нужна для квитанции после оплаты. */
    fun emailOf(userId: Int): String? = transaction {
        Users.selectAll().where { Users.id eq userId }.singleOrNull()?.get(Users.email)
    }

    /** Поиск заказа по коду платежа: админ вводит код из назначения перевода. */
    fun findByCode(code: String): Int? = transaction {
        val normalized = code.trim().uppercase().let { if (it.startsWith("AS-")) it else "AS-$it" }
        PaymentClaims.selectAll()
            .where { PaymentClaims.paymentCode eq normalized }
            .orderBy(PaymentClaims.createdAt, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.get(PaymentClaims.id)
    }
}

private data class PaymentOrderRow(
    val userId: Int,
    val days: Int,
    val amountEur: Int,
    val status: String,
    val code: String
)

data class PaymentConfirmResult(
    val success: Boolean,
    val message: String,
    val userId: Int = 0,
    val days: Int = 0,
    val amountEur: Int = 0
)

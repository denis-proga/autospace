package com.autospace.app

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val email = varchar("email", 150)
    val username = varchar("username", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    // DEMO, ACTIVE, FRIEND, EXPIRED, BLOCKED  (старый PENDING при запуске превращается в DEMO)
    val licenseStatus = varchar("license_status", 20)
    val licenseExpiresAt = long("license_expires_at").nullable() // timestamp в миллисекундах

    val supportMessagesCount = integer("support_messages_count").default(0)
    val sessionToken = varchar("session_token", 64).nullable()

    // SHA-256 от нормализованной почты: один человек = один набор бесплатных тестов
    val emailKey = varchar("email_key", 64).nullable()

    // момент регистрации: нужен режиму отъезда, чтобы подарок получили только прежние пользователи
    val createdAt = long("created_at").nullable()

    // выбранный аватар: «avatar_1».. «avatar_6» или пусто
    val avatar = varchar("avatar", 20).nullable()

    // достижения, которые человек уже видел: идентификаторы через запятую
    val seenAchievements = text("seen_achievements").nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Ответы на «вопрос дня»: один ответ в сутки на пользователя. day — номер дня по UTC. */
object DailyAnswers : Table("daily_answers") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val day = long("day")
    val questionId = integer("question_id")
    val chosenOption = varchar("chosen_option", 1)
    val correct = bool("correct")
    val answeredAt = long("answered_at")

    override val primaryKey = PrimaryKey(id)
    init {
        uniqueIndex(userId, day)
    }
}

/** Настройки, которые меняются на ходу (режим отъезда). Ключ — значение плюс произвольное поле. */
object AppSettings : Table("app_settings") {
    val key = varchar("key", 50).uniqueIndex()
    val value = varchar("value", 100)
    val extra = varchar("extra", 200).nullable()
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(key)
}

/**
 * Потраченные бесплатные тесты. Привязаны к хешу почты, а не к строке в Users,
 * поэтому удаление и повторная регистрация слоты не сбрасывают.
 */
object DemoSlots : Table("demo_slots") {
    val id = integer("id").autoIncrement()
    val emailKey = varchar("email_key", 64)
    val testNumber = integer("test_number")
    val claimedAt = long("claimed_at")

    override val primaryKey = PrimaryKey(id)
    init {
        uniqueIndex(emailKey, testNumber)
    }
}

/**
 * Заказ на оплату. Создаётся ДО оплаты: человек видит код платежа и реквизиты, админ подтверждает
 * поступление денег кнопкой в Telegram, доступ открывается сам.
 * Статусы: PENDING, PAID, EXPIRED, CANCELLED, DECLINED (APPROVED — старое название PAID).
 */
object PaymentClaims : Table("payment_claims") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val days = integer("days")
    val amountEur = integer("amount_eur")
    val status = varchar("status", 20)
    val createdAt = long("created_at")

    // код из назначения платежа (AS-7K3Q), срок жизни заказа и момент подтверждения
    val paymentCode = varchar("payment_code", 16).nullable()
    val expiresAt = long("expires_at").nullable()
    val paidAt = long("paid_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

/**
 * Ответы пользователя по каждому вопросу: основа для режима «работа над ошибками»,
 * статистики по вопросам и наград. Одна строка на пару (пользователь, вопрос).
 */
object UserAnswers : Table("user_answers") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id")
    val questionId = integer("question_id")
    val attempts = integer("attempts")
    val wrongCount = integer("wrong_count")
    val lastCorrect = bool("last_correct")
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
    init {
        uniqueIndex(userId, questionId)
    }
}

/** Обращения в поддержку. Статусы: OPEN, IN_PROGRESS, ANSWERED, CLOSED. */
object SupportTickets : Table("support_tickets") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val category = varchar("category", 30)
    val subject = varchar("subject", 150)
    val message = text("message")
    val contactEmail = varchar("contact_email", 150).nullable()
    val language = varchar("language", 5)
    val status = varchar("status", 20)
    val createdAt = long("created_at")
    val updatedAt = long("updated_at")
    val lastReply = text("last_reply").nullable()
    val lastReplyAt = long("last_reply_at").nullable()

    override val primaryKey = PrimaryKey(id)
}

object TestResults : Table("test_results") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val testNumber = integer("test_number")
    val mode = varchar("mode", 20) // LEARNING или EXAM
    val correctCount = integer("correct_count")
    val totalQuestions = integer("total_questions")
    val completedAt = long("completed_at")

    override val primaryKey = PrimaryKey(id)
}

object Questions : Table("questions") {
    val id = integer("id").autoIncrement()
    val questionKey = varchar("question_key", 255).uniqueIndex()
    val testNumber = integer("test_number")
    val imageFilename = varchar("image_filename", 255)
    val questionText = text("question_text")
    val optionA = text("option_a")
    val optionB = text("option_b")
    val optionC = text("option_c").nullable()
    val optionD = text("option_d").nullable()
    val correctOption = varchar("correct_option", 1)
    val explanation = text("explanation")

    val questionTextEn = text("question_text_en").nullable()
    val optionAEn = text("option_a_en").nullable()
    val optionBEn = text("option_b_en").nullable()
    val optionCEn = text("option_c_en").nullable()
    val optionDEn = text("option_d_en").nullable()
    val explanationEn = text("explanation_en").nullable()

    val questionTextEs = text("question_text_es").nullable()
    val optionAEs = text("option_a_es").nullable()
    val optionBEs = text("option_b_es").nullable()
    val optionCEs = text("option_c_es").nullable()
    val optionDEs = text("option_d_es").nullable()
    val explanationEs = text("explanation_es").nullable()

    val questionTextDe = text("question_text_de").nullable()
    val optionADe = text("option_a_de").nullable()
    val optionBDe = text("option_b_de").nullable()
    val optionCDe = text("option_c_de").nullable()
    val optionDDe = text("option_d_de").nullable()
    val explanationDe = text("explanation_de").nullable()

    val questionTextUk = text("question_text_uk").nullable()
    val optionAUk = text("option_a_uk").nullable()
    val optionBUk = text("option_b_uk").nullable()
    val optionCUk = text("option_c_uk").nullable()
    val optionDUk = text("option_d_uk").nullable()
    val explanationUk = text("explanation_uk").nullable()

    override val primaryKey = PrimaryKey(id)
}

object TestProgress : Table("test_progress") {
    val id = integer("id").autoIncrement()
    val userId = integer("user_id").references(Users.id)
    val testNumber = integer("test_number")
    val mode = varchar("mode", 20)
    val answersData = text("answers_data")
    val currentIndex = integer("current_index")
    val secondsLeft = integer("seconds_left").nullable()
    val updatedAt = long("updated_at")

    override val primaryKey = PrimaryKey(id)
    init {
        uniqueIndex(userId, testNumber, mode)
    }
}

fun initDatabase() {
    val databaseUrl = System.getenv("DATABASE_URL")

    if (databaseUrl.isNullOrBlank()) {
        // локальная разработка
        Database.connect("jdbc:sqlite:autospace.db", driver = "org.sqlite.JDBC")
        println("Database: SQLite (local)")
    } else {
        val uri = java.net.URI(databaseUrl.removePrefix("jdbc:"))
        val userInfo = uri.userInfo?.split(":")
        val cleanUrl = "jdbc:postgresql://${uri.host}${uri.path}?sslmode=require"

        val config = HikariConfig().apply {
            jdbcUrl = cleanUrl
            username = userInfo?.getOrNull(0)
            password = userInfo?.getOrNull(1)
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 5
        }
        Database.connect(HikariDataSource(config))
        println("Database: PostgreSQL")
    }

    transaction {
        SchemaUtils.createMissingTablesAndColumns(Users, TestResults, Questions, TestProgress, DemoSlots, PaymentClaims, UserAnswers, SupportTickets, AppSettings, DailyAnswers)

        // Статуса PENDING больше нет: новый пользователь сразу получает демо
        Users.update({ Users.licenseStatus eq "PENDING" }) {
            it[Users.licenseStatus] = "DEMO"
        }

        // Старым пользователям проставляем дату регистрации: они зарегистрированы до любого будущего отъезда
        val now = System.currentTimeMillis()
        Users.selectAll().filter { it[Users.createdAt] == null }.forEach { row ->
            Users.update({ Users.id eq row[Users.id] }) {
                it[Users.createdAt] = now
            }
        }

        // Старым пользователям дописываем email_key (хеш нормализованной почты)
        val withoutKey = Users.selectAll().filter { it[Users.emailKey] == null }
        withoutKey.forEach { row ->
            Users.update({ Users.id eq row[Users.id] }) {
                it[Users.emailKey] = emailKeyOf(row[Users.email])
            }
        }
    }
}

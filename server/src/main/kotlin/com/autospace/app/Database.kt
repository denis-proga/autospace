package com.autospace.app

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

object Users : Table("users") {
    val id = integer("id").autoIncrement()
    val firstName = varchar("first_name", 100)
    val lastName = varchar("last_name", 100)
    val email = varchar("email", 150)
    val username = varchar("username", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val licenseStatus = varchar("license_status", 20) // PENDING, ACTIVE, EXPIRED, BLOCKED
    val licenseExpiresAt = long("license_expires_at").nullable() // timestamp в миллисекундах

    val supportMessagesCount = integer("support_messages_count").default(0)

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
        SchemaUtils.create(Users, TestResults, Questions, TestProgress)
    }
}
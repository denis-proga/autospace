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

fun initDatabase() {
    val databaseUrl = System.getenv("DATABASE_URL")

    if (databaseUrl.isNullOrBlank()) {
        // локальная разработка
        Database.connect("jdbc:sqlite:autospace.db", driver = "org.sqlite.JDBC")
        println("Database: SQLite (local)")
    } else {
        val config = HikariConfig().apply {
            jdbcUrl = databaseUrl
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 5
        }
        Database.connect(HikariDataSource(config))
        println("Database: PostgreSQL")
    }

    transaction {
        SchemaUtils.create(Users, TestResults)
    }
}
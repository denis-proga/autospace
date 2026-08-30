package com.autospace.app

import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

object UserRepository {
    fun approveUser(userId: Int) {
        transaction {
            Users.update({ Users.id eq userId }) {
                it[licenseStatus] = "ACTIVE"
                it[licenseExpiresAt] = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // +30 дней
                it[supportMessagesCount] = 0
            }
        }
    }

    fun blockUser(userId: Int) {
        transaction {
            Users.update({ Users.id eq userId }) {
                it[licenseStatus] = "BLOCKED"
            }
        }
    }

    fun listUsers(): List<String> {
        return transaction {
            Users.selectAll().map { row ->
                val expires = row[Users.licenseExpiresAt]
                val expiresText = if (expires != null && row[Users.licenseStatus] == "ACTIVE") {
                    val daysLeft = (expires - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
                    if (daysLeft >= 0) ", осталось $daysLeft дн." else ", срок истёк"
                } else ""
                "#${row[Users.id]} ${row[Users.username]} — ${row[Users.licenseStatus]}$expiresText"
            }
        }
    }

    fun deleteUser(userId: Int): Boolean {
        return transaction {
            Users.deleteWhere { Users.id eq userId } > 0
        }
    }
}
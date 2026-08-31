package com.autospace.app

import org.mindrot.jbcrypt.BCrypt

fun hashPassword(password: String): String {
    return BCrypt.hashpw(password, BCrypt.gensalt(12))
}

fun verifyPassword(password: String, hash: String): Boolean {
    return try {
        BCrypt.checkpw(password, hash)
    } catch (e: IllegalArgumentException) {
        false
    }
}
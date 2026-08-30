package com.autospace.app

import java.security.MessageDigest

fun hashPassword(password: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

fun verifyPassword(password: String, hash: String): Boolean {
    return hashPassword(password) == hash
}
package com.autospace.app

data class User(
    val firstName: String,
    val lastName: String,
    val email: String,
    val username: String,
    val password: String
)

enum class LicenseStatus {
    PENDING,   // ждёт одобрения админом
    ACTIVE,    // лицензия активна
    EXPIRED,   // истекла
    BLOCKED    // заблокирован
}
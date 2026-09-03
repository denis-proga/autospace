package com.autospace.app

import java.io.File

private val tokenFile: File by lazy {
    val homeDir = System.getProperty("user.home")
    File(homeDir, ".autospace/session_token")
}

actual fun saveToken(token: String?) {
    try {
        if (token == null) {
            if (tokenFile.exists()) tokenFile.delete()
        } else {
            tokenFile.parentFile?.mkdirs()
            tokenFile.writeText(token)
        }
    } catch (e: Exception) {
        // тихо игнорируем — просто не сохранится, пользователь перелогинится
    }
}

actual fun loadToken(): String? {
    return try {
        if (tokenFile.exists()) tokenFile.readText().trim().ifBlank { null } else null
    } catch (e: Exception) {
        null
    }
}

private val languageFile: File by lazy {
    val homeDir = System.getProperty("user.home")
    File(homeDir, ".autospace/language")
}

actual fun saveLanguageCode(code: String?) {
    try {
        if (code == null) {
            if (languageFile.exists()) languageFile.delete()
        } else {
            languageFile.parentFile?.mkdirs()
            languageFile.writeText(code)
        }
    } catch (e: Exception) {
    }
}

actual fun loadLanguageCode(): String? {
    return try {
        if (languageFile.exists()) languageFile.readText().trim().ifBlank { null } else null
    } catch (e: Exception) {
        null
    }
}
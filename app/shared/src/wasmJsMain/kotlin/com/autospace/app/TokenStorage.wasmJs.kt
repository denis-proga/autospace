package com.autospace.app

import kotlinx.browser.localStorage

private const val TOKEN_KEY = "autospace_token"
private const val LANGUAGE_KEY = "autospace_language"

// localStorage может быть недоступен (например, в приватном режиме Safari), поэтому всё в try/catch
private fun writeValue(key: String, value: String?) {
    try {
        if (value == null) localStorage.removeItem(key) else localStorage.setItem(key, value)
    } catch (_: Throwable) {
    }
}

private fun readValue(key: String): String? {
    return try {
        localStorage.getItem(key)
    } catch (_: Throwable) {
        null
    }
}

actual fun saveToken(token: String?) = writeValue(TOKEN_KEY, token)

actual fun loadToken(): String? = readValue(TOKEN_KEY)

actual fun saveLanguageCode(code: String?) = writeValue(LANGUAGE_KEY, code)

actual fun loadLanguageCode(): String? = readValue(LANGUAGE_KEY)

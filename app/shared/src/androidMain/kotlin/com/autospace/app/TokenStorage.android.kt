package com.autospace.app

import android.content.Context
import android.content.SharedPreferences

private lateinit var prefs: SharedPreferences

fun initTokenStorage(context: Context) {
    prefs = context.getSharedPreferences("autospace_session", Context.MODE_PRIVATE)
}

actual fun saveToken(token: String?) {
    if (!::prefs.isInitialized) return
    prefs.edit().apply {
        if (token == null) remove("session_token") else putString("session_token", token)
    }.apply()
}

actual fun loadToken(): String? {
    if (!::prefs.isInitialized) return null
    return prefs.getString("session_token", null)
}

actual fun saveLanguageCode(code: String?) {
    if (!::prefs.isInitialized) return
    prefs.edit().apply {
        if (code == null) remove("language_code") else putString("language_code", code)
    }.apply()
}

actual fun loadLanguageCode(): String? {
    if (!::prefs.isInitialized) return null
    return prefs.getString("language_code", null)
}
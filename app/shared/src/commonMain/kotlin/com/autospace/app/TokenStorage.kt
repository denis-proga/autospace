package com.autospace.app

expect fun saveToken(token: String?)
expect fun loadToken(): String?

expect fun saveLanguageCode(code: String?)
expect fun loadLanguageCode(): String?
package com.autospace.app

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException

class QuestionsNotFoundException(val testNumber: Int) : Exception("No questions for test $testNumber")

/** Сетевой сбой (нет связи, блокировка CORS и т.п.). */
class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Токен недействителен: сессия истекла или вход выполнен на другом устройстве. */
class SessionExpiredException : Exception("Session expired")

/** Сервер отказал в доступе к тесту. code: DEMO_LIMIT, LICENSE_EXPIRED, BLOCKED, NO_ACCESS. */
class LicenseRequiredException(val code: String) : Exception("License required: $code")

/** Похоже ли исключение на проблему со связью (нет интернета, DNS, обрыв, блокировка в браузере). */
private fun isConnectionProblem(e: Throwable): Boolean {
    var current: Throwable? = e
    var depth = 0
    while (current != null && depth < 5) {
        val t: Throwable = current
        val name = t::class.simpleName ?: ""
        val text = (t.message ?: "").lowercase()
        if (name.contains("UnresolvedAddress") || name.contains("UnknownHost") ||
            name.contains("ConnectException") || name.contains("SocketException") ||
            name.contains("SocketTimeout") || name.contains("NoRouteToHost") ||
            name.contains("IOException") || name.contains("EOFException") ||
            name.contains("ClosedChannel") || name.contains("NetworkException") ||
            name == "JsError" ||
            text.contains("fail to fetch") || text.contains("failed to fetch") ||
            text.contains("network") || text.contains("unable to resolve") ||
            text.contains("connection refused") || text.contains("connection reset")
        ) {
            return true
        }
        current = t.cause
        depth++
    }
    return false
}

/** Текст ошибки для пользователя. Технические подробности наружу не показываем, они идут в консоль. */
fun friendlyServerErrorMessage(e: Exception, strings: AppStrings): String {
    return when (e) {
        is QuestionsNotFoundException -> strings.errorQuestionsNotFound
        is SessionExpiredException -> strings.errorSessionExpired
        is HttpRequestTimeoutException, is ConnectTimeoutException -> strings.errorServerSleeping
        is NetworkException -> strings.errorNoInternet
        else -> {
            println("Request failed: $e")
            if (isConnectionProblem(e)) strings.errorNoInternet else strings.errorGeneric
        }
    }
}

/** Сообщения сервера (на английском) в понятный текст на языке интерфейса. */
fun localizedAuthError(message: String?, strings: AppStrings): String {
    return when (message) {
        null, "" -> strings.errorGeneric
        "Invalid username or password" -> strings.errorInvalidCredentials
        "Username already taken" -> strings.errorUsernameTaken
        "Email already registered" -> strings.errorEmailTaken
        "Too many attempts. Try again later",
        "Too many registration attempts. Try again later",
        "TOO_MANY_REQUESTS" -> strings.forgotTooManyRequests
        "Invalid or expired code" -> strings.forgotInvalidCode
        else -> message
    }
}

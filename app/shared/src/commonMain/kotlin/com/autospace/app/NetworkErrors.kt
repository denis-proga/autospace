package com.autospace.app

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException

class QuestionsNotFoundException(val testNumber: Int) : Exception("No questions for test $testNumber")

/** Токен недействителен: сессия истекла или вход выполнен на другом устройстве. */
class SessionExpiredException : Exception("Session expired")

/** Сервер отказал в доступе к тесту. code: DEMO_LIMIT, LICENSE_EXPIRED, BLOCKED, NO_ACCESS. */
class LicenseRequiredException(val code: String) : Exception("License required: $code")

fun friendlyServerErrorMessage(e: Exception, strings: AppStrings): String {
    return when (e) {
        is QuestionsNotFoundException -> strings.errorQuestionsNotFound
        is SessionExpiredException -> strings.errorSessionExpired
        is HttpRequestTimeoutException, is ConnectTimeoutException -> strings.errorServerSleeping
        else -> "${strings.errorServerUnreachablePrefix}: ${e.message}"
    }
}
package com.autospace.app

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException

class QuestionsNotFoundException(val testNumber: Int) : Exception("No questions for test $testNumber")

fun friendlyServerErrorMessage(e: Exception, strings: AppStrings): String {
    return when (e) {
        is QuestionsNotFoundException -> strings.errorQuestionsNotFound
        is HttpRequestTimeoutException, is ConnectTimeoutException -> strings.errorServerSleeping
        else -> "${strings.errorServerUnreachablePrefix}: ${e.message}"
    }
}
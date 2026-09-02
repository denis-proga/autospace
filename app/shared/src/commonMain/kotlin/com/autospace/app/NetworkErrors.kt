package com.autospace.app

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException

class QuestionsNotFoundException(val testNumber: Int) : Exception("No questions for test $testNumber")

fun friendlyServerErrorMessage(e: Exception): String {
    return when (e) {
        is QuestionsNotFoundException -> "Вопросы для этого теста ещё не добавлены"
        is HttpRequestTimeoutException, is ConnectTimeoutException ->
            "Не удалось подключиться к серверу. Сервер мог «заснуть» — попробуйте ещё раз через минуту."
        else -> "Server unreachable: ${e.message}"
    }
}
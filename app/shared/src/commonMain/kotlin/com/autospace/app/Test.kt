package com.autospace.app

enum class TestMode {
    LEARNING,
    EXAM
}

data class TestInfo(
    val number: Int,
    val title: String
)

fun generateTestList(): List<TestInfo> {
    return (1..50).map { number ->
        TestInfo(number = number, title = "Тест $number")
    }
}
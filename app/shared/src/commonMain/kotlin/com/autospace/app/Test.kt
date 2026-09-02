package com.autospace.app

enum class TestMode {
    LEARNING,
    EXAM
}

data class TestInfo(
    val number: Int
)

fun generateTestList(): List<TestInfo> {
    return (1..50).map { number -> TestInfo(number = number) }
}
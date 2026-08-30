package com.autospace.app

sealed class Screen {
    object LanguageSelection : Screen()
    object Auth : Screen()
    data class VerifyCode(val email: String) : Screen()
    object Pending : Screen()
    object Support : Screen()
    object Blocked : Screen()
    object MainMenu : Screen()

    object Stats : Screen()
    data class Test(val test: TestInfo, val mode: TestMode) : Screen()
}
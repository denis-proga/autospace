package com.autospace.app

sealed class Screen {
    object LanguageSelection : Screen()
    object Auth : Screen()
    object ForgotPassword : Screen()
    object SupportCenter : Screen()
    data class VerifyCode(val email: String) : Screen()
    object Pending : Screen()
    object Support : Screen()
    object Blocked : Screen()
    object MainMenu : Screen()
    data class Paywall(val reason: String) : Screen() // DEMO_LIMIT, LICENSE_EXPIRED или MENU

    object Stats : Screen()
    object Profile : Screen()
    data class Test(val test: TestInfo, val mode: TestMode) : Screen()
}
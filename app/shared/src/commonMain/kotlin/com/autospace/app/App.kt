package com.autospace.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

private fun languageFromCode(code: String?): Language? {
    return Language.entries.find { it.code == code }
}

/** Куда попадает пользователь после входа. Доступ к тестам сервер проверяет сам при открытии теста. */
private fun screenForStatus(status: String?): Screen {
    return if (status == "BLOCKED") Screen.Blocked else Screen.MainMenu
}

@Composable
@Preview
fun App() {
    var appTheme by remember { mutableStateOf(AppTheme.Dark) }

    MaterialTheme(
        colorScheme = autoSpaceColorScheme(appTheme),
        typography = if (getPlatform().name == "Web") autoSpaceTypography() else Typography()
    ) {
        val stack = remember { mutableStateListOf<Screen>(Screen.LanguageSelection) }
        val currentScreen = stack.last()

        var selectedLanguage by remember { mutableStateOf(languageFromCode(loadLanguageCode()) ?: Language.RUSSIAN) }
        var loggedInUsername by remember { mutableStateOf<String?>(null) }
        var sessionToken by remember { mutableStateOf<String?>(null) }
        var isCheckingSession by remember { mutableStateOf(true) }
        var licenseStatus by remember { mutableStateOf<String?>(null) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isChecking by remember { mutableStateOf(false) }
        var isAuthenticating by remember { mutableStateOf(false) }
        var isSendingSupport by remember { mutableStateOf(false) }
        var supportSentSuccessfully by remember { mutableStateOf(false) }
        var supportErrorMessage by remember { mutableStateOf<String?>(null) }
        var isVerifyingCode by remember { mutableStateOf(false) }
        var verifyErrorMessage by remember { mutableStateOf<String?>(null) }
        var pendingUsername by remember { mutableStateOf<String?>(null) }
        var pendingPassword by remember { mutableStateOf<String?>(null) }
        var isLoadingStats by remember { mutableStateOf(false) }
        var statsResults by remember { mutableStateOf<List<TestResultItemDto>>(emptyList()) }
        var resendCooldownSeconds by remember { mutableStateOf(0) }
        var isResendingCode by remember { mutableStateOf(false) }
        var resendExhausted by remember { mutableStateOf(false) }
        var resendCount by remember { mutableStateOf(0) }
        var menuRefreshTrigger by remember { mutableStateOf(0) }

        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            val storedToken = loadToken()
            if (storedToken != null) {
                try {
                    val response = ApiClient.checkSession(storedToken)
                    if (response.success) {
                        ApiClient.authToken = storedToken
                        sessionToken = storedToken
                        loggedInUsername = response.username
                        licenseStatus = response.licenseStatus
                        stack.clear()
                        stack.add(screenForStatus(response.licenseStatus))
                    } else {
                        saveToken(null)
                    }
                } catch (e: Exception) {
                    // тихо игнорируем — просто останемся на экране выбора языка/входа
                }
            }
            isCheckingSession = false
        }

        LaunchedEffect(resendCooldownSeconds) {
            if (resendCooldownSeconds > 0) {
                delay(1000)
                resendCooldownSeconds -= 1
            }
        }

        fun navigateTo(screen: Screen) {
            stack.add(screen)
        }

        fun navigateBack() {
            if (stack.size > 1) stack.removeAt(stack.lastIndex)
        }

        fun navigateReplacingPending(screen: Screen) {
            if (stack.isNotEmpty() && stack.last() == Screen.Pending) {
                stack.removeAt(stack.lastIndex)
            }
            stack.add(screen)
        }

        fun navigateReplacingCurrent(screen: Screen) {
            if (stack.isNotEmpty()) {
                stack.removeAt(stack.lastIndex)
            }
            stack.add(screen)
        }

        fun resetToStart() {
            saveToken(null)
            ApiClient.authToken = null
            stack.clear()
            stack.add(Screen.LanguageSelection)
            loggedInUsername = null
            sessionToken = null
            licenseStatus = null
            errorMessage = null
            isSendingSupport = false
            supportSentSuccessfully = false
            isVerifyingCode = false
            verifyErrorMessage = null
            pendingUsername = null
            pendingPassword = null
            supportErrorMessage = null
            isLoadingStats = false
            statsResults = emptyList()
            resendCooldownSeconds = 0
            isResendingCode = false
            resendExhausted = false
            resendCount = 0
            isAuthenticating = false
            menuRefreshTrigger = 0
        }

        val showBackButton = stack.size > 1 && currentScreen != Screen.Pending

        BoxWithConstraints {
            val windowSizeClass = windowSizeClassFor(maxWidth)

            CompositionLocalProvider(
                LocalWindowSizeClass provides windowSizeClass,
                LocalAppTheme provides appTheme,
                LocalAppColors provides appColorsFor(appTheme),
                LocalSemanticColors provides semanticColorsFor(appTheme),
                LocalStrings provides stringsFor(selectedLanguage)
            ) {
                AppBackground {
                    if (isCheckingSession) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        val strings = LocalStrings.current
                        val colors = LocalAppColors.current
                        val interFamily = interFontFamily()

                        Column {
                            // на экране выбора языка верхней панели нет: там полноэкранная сцена
                            if (currentScreen != Screen.LanguageSelection && currentScreen != Screen.Auth &&
                                currentScreen !is Screen.VerifyCode && currentScreen != Screen.ForgotPassword &&
                                currentScreen != Screen.MainMenu && currentScreen != Screen.SupportCenter &&
                                currentScreen != Screen.Stats && currentScreen !is Screen.Test) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (showBackButton) {
                                    Text(
                                        text = strings.commonBack,
                                        fontFamily = interFamily,
                                        color = colors.textSecondary,
                                        modifier = Modifier.clickable { navigateBack() }
                                    )
                                }

                                if (loggedInUsername != null) {
                                    Text(
                                        text = strings.commonLogout,
                                        fontFamily = interFamily,
                                        color = colors.textSecondary,
                                        modifier = Modifier
                                            .padding(start = if (showBackButton) 16.dp else 0.dp)
                                            .clickable { resetToStart() }
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                Text(
                                    text = if (appTheme == AppTheme.Dark) strings.themeSwitchToLight else strings.themeSwitchToDark,
                                    fontFamily = interFamily,
                                    color = colors.textSecondary,
                                    modifier = Modifier.clickable {
                                        appTheme = if (appTheme == AppTheme.Dark) AppTheme.Light else AppTheme.Dark
                                    }
                                )
                            }
                            }

                            Box(modifier = Modifier.padding(0.dp)) {
                                when (val screen = currentScreen) {
                                    is Screen.LanguageSelection -> {
                                        LanguageSelectionScreen(
                                            onLanguageSelected = { language ->
                                                selectedLanguage = language
                                                saveLanguageCode(language.code)
                                                navigateTo(Screen.Auth)
                                            }
                                        )
                                    }

                                    is Screen.Auth -> {
                                        AuthScreen(
                                            errorMessage = errorMessage,
                                            isLoading = isAuthenticating,
                                            onModeChanged = { errorMessage = null },
                                            onForgotPassword = { navigateTo(Screen.ForgotPassword) },
                                            language = selectedLanguage,
                                            onChangeLanguage = { navigateBack() },
                                            onLogin = { username, password ->
                                                scope.launch {
                                                    isAuthenticating = true
                                                    errorMessage = null
                                                    try {
                                                        val response = ApiClient.login(LoginRequestDto(username, password))
                                                        if (response.success) {
                                                            loggedInUsername = response.username ?: username
                                                            sessionToken = response.token
                                                            saveToken(response.token)
                                                            ApiClient.authToken = response.token
                                                            licenseStatus = response.licenseStatus
                                                            navigateTo(screenForStatus(response.licenseStatus))
                                                        } else {
                                                            if (response.message.startsWith("EMAIL_NOT_VERIFIED:")) {
                                                                val parts = response.message.split(":", limit = 3)
                                                                pendingUsername = parts.getOrNull(1) ?: username
                                                                pendingPassword = password
                                                                resendCooldownSeconds = 0
                                                                isResendingCode = false
                                                                resendExhausted = false
                                                                resendCount = 0
                                                                navigateTo(Screen.VerifyCode(parts.getOrNull(2) ?: ""))
                                                            } else {
                                                                errorMessage = localizedAuthError(response.message, strings)
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        errorMessage = friendlyServerErrorMessage(e, strings)
                                                    }
                                                    isAuthenticating = false
                                                }
                                            },
                                            onRegister = { user ->
                                                scope.launch {
                                                    isAuthenticating = true
                                                    errorMessage = null
                                                    try {
                                                        val response = ApiClient.register(
                                                            RegisterRequestDto(
                                                                firstName = user.firstName,
                                                                lastName = user.lastName,
                                                                email = user.email,
                                                                username = user.username,
                                                                password = user.password,
                                                                language = selectedLanguage.code,
                                                            )
                                                        )
                                                        if (response.success) {
                                                            pendingUsername = user.username
                                                            pendingPassword = user.password
                                                            resendCooldownSeconds = 0
                                                            isResendingCode = false
                                                            resendExhausted = false
                                                            resendCount = 0
                                                            navigateTo(Screen.VerifyCode(user.email))
                                                        } else {
                                                            errorMessage = localizedAuthError(response.message, strings)
                                                        }
                                                    } catch (e: Exception) {
                                                        errorMessage = friendlyServerErrorMessage(e, strings)
                                                    }
                                                    isAuthenticating = false
                                                }
                                            }
                                        )
                                    }

                                    is Screen.ForgotPassword -> {
                                        ForgotPasswordScreen(
                                            language = selectedLanguage.code,
                                            onBackToLogin = { navigateBack() },
                                            onContinueRegistration = { username, maskedEmail ->
                                                pendingUsername = username
                                                pendingPassword = null
                                                resendCooldownSeconds = 0
                                                isResendingCode = false
                                                resendExhausted = false
                                                resendCount = 0
                                                navigateReplacingCurrent(Screen.VerifyCode(maskedEmail))
                                            }
                                        )
                                    }

                                    is Screen.SupportCenter -> {
                                        SupportCenterScreen(
                                            language = selectedLanguage.code,
                                            onBack = { navigateBack() },
                                            onOpenPaywall = { navigateTo(Screen.Paywall("MENU")) },
                                            onSessionExpired = { resetToStart() }
                                        )
                                    }

                                    is Screen.Pending -> {
                                        PendingScreen(
                                            isChecking = isChecking,
                                            onCheckStatus = {
                                                scope.launch {
                                                    isChecking = true
                                                    val token = sessionToken
                                                    if (token != null) {
                                                        try {
                                                            val response = ApiClient.checkSession(token)
                                                            if (response.success) {
                                                                licenseStatus = response.licenseStatus
                                                                when (response.licenseStatus) {
                                                                    "ACTIVE" -> navigateReplacingPending(Screen.MainMenu)
                                                                    "BLOCKED", "EXPIRED" -> navigateReplacingPending(Screen.Blocked)
                                                                    else -> { /* остаёмся ждать */ }
                                                                }
                                                            }
                                                        } catch (_: Exception) {
                                                        }
                                                    }
                                                    isChecking = false
                                                }
                                            },
                                            onGoToSupport = { navigateTo(Screen.Support) }
                                        )
                                    }

                                    is Screen.VerifyCode -> {
                                        VerifyCodeScreen(
                                            email = screen.email,
                                            errorMessage = verifyErrorMessage,
                                            isVerifying = isVerifyingCode,
                                            onVerify = { code ->
                                                scope.launch {
                                                    isVerifyingCode = true
                                                    verifyErrorMessage = null
                                                    try {
                                                        val response = ApiClient.verifyRegistration(
                                                            VerifyRegistrationRequestDto(
                                                                username = pendingUsername!!,
                                                                code = code
                                                            )
                                                        )
                                                        if (response.success) {
                                                            loggedInUsername = pendingUsername
                                                            licenseStatus = response.licenseStatus

                                                            var loginOk = false
                                                            try {
                                                                val loginResponse = ApiClient.login(
                                                                    LoginRequestDto(pendingUsername!!, pendingPassword!!)
                                                                )
                                                                if (loginResponse.success) {
                                                                    sessionToken = loginResponse.token
                                                                    saveToken(loginResponse.token)
                                                                    ApiClient.authToken = loginResponse.token
                                                                    loginOk = true
                                                                }
                                                            } catch (e: Exception) {
                                                                // тихо игнорируем
                                                            }
                                                            pendingPassword = null

                                                            if (!loginOk) loggedInUsername = null
                                                            navigateReplacingCurrent(if (loginOk) Screen.MainMenu else Screen.Auth)
                                                        } else {
                                                            verifyErrorMessage = localizedAuthError(response.message, strings)
                                                        }
                                                    } catch (e: Exception) {
                                                        verifyErrorMessage = friendlyServerErrorMessage(e, strings)
                                                    }
                                                    isVerifyingCode = false
                                                }
                                            },
                                            resendCooldownSeconds = resendCooldownSeconds,
                                            isResending = isResendingCode,
                                            resendExhausted = resendExhausted,
                                            onResendCode = {
                                                scope.launch {
                                                    isResendingCode = true
                                                    verifyErrorMessage = null
                                                    try {
                                                        val response = ApiClient.resendCode(ResendCodeRequestDto(username = pendingUsername!!))
                                                        if (response.success) {
                                                            resendCount += 1
                                                            if (resendCount >= 3) {
                                                                resendExhausted = true
                                                            } else {
                                                                resendCooldownSeconds = 60
                                                            }
                                                        } else {
                                                            verifyErrorMessage = response.message
                                                            val cooldownMatch = Regex("\\d+").find(response.message ?: "")
                                                            if (cooldownMatch != null) {
                                                                resendCooldownSeconds = cooldownMatch.value.toInt()
                                                            } else {
                                                                resendExhausted = true
                                                            }
                                                        }
                                                    } catch (e: Exception) {
                                                        verifyErrorMessage = friendlyServerErrorMessage(e, strings)
                                                    }
                                                    isResendingCode = false
                                                }
                                            },
                                            onContactSupport = { navigateTo(Screen.Support) },
                                            onBack = { navigateBack() }
                                        )
                                    }

                                    is Screen.Support -> {
                                        SupportScreen(
                                            isSending = isSendingSupport,
                                            sentSuccessfully = supportSentSuccessfully,
                                            errorMessage = supportErrorMessage,
                                            onSend = { message, phone, email ->
                                                scope.launch {
                                                    isSendingSupport = true
                                                    try {
                                                        val response = ApiClient.sendSupportRequest(
                                                            SupportRequestDto(
                                                                username = loggedInUsername ?: pendingUsername ?: "unknown",
                                                                message = message,
                                                                phone = phone.ifBlank { null },
                                                                email = email.ifBlank { null }
                                                            )
                                                        )
                                                        if (response.success) {
                                                            supportSentSuccessfully = true
                                                        } else {
                                                            supportErrorMessage = response.message
                                                        }
                                                    } catch (_: Exception) {
                                                    }
                                                    isSendingSupport = false
                                                }
                                            },
                                            onBackToRegistration = { resetToStart() },
                                            onKeepWaiting = {
                                                supportSentSuccessfully = false
                                                navigateBack()
                                            }
                                        )
                                    }

                                    is Screen.Blocked -> {
                                        BlockedScreen(
                                            isSending = isSendingSupport,
                                            sentSuccessfully = supportSentSuccessfully,
                                            errorMessage = supportErrorMessage,
                                            onSend = { message, phone, email ->
                                                scope.launch {
                                                    isSendingSupport = true
                                                    try {
                                                        val response = ApiClient.sendSupportRequest(
                                                            SupportRequestDto(
                                                                username = loggedInUsername ?: "unknown",
                                                                message = message,
                                                                phone = phone.ifBlank { null },
                                                                email = email.ifBlank { null }
                                                            )
                                                        )
                                                        if (response.success) {
                                                            supportSentSuccessfully = true
                                                        } else {
                                                            supportErrorMessage = response.message
                                                        }
                                                    } catch (_: Exception) {
                                                    }
                                                    isSendingSupport = false
                                                }
                                            },
                                            onBackToRegistration = { resetToStart() }
                                        )
                                    }

                                    is Screen.MainMenu -> {
                                        MainMenuScreen(
                                            username = loggedInUsername ?: "",
                                            refreshKey = menuRefreshTrigger,
                                            onOpenPaywall = { navigateTo(Screen.Paywall("MENU")) },
                                            language = selectedLanguage,
                                            onLanguageChange = { newLanguage ->
                                                selectedLanguage = newLanguage
                                                saveLanguageCode(newLanguage.code)
                                            },
                                            onLogout = { resetToStart() },
                                            onOpenSupport = { navigateTo(Screen.SupportCenter) },
                                            onTestSelected = { test, mode ->
                                                navigateTo(Screen.Test(test, mode))
                                            },
                                            onOpenStats = {
                                                navigateTo(Screen.Stats)
                                                scope.launch {
                                                    isLoadingStats = true
                                                    try {
                                                        val response = ApiClient.getResults(loggedInUsername ?: "")
                                                        statsResults = response.results
                                                    } catch (_: Exception) {
                                                        statsResults = emptyList()
                                                    }
                                                    isLoadingStats = false
                                                }
                                            }
                                        )
                                    }

                                    is Screen.Paywall -> {
                                        PaywallScreen(
                                            reason = screen.reason,
                                            onAccessGranted = { navigateBack() },
                                            onSessionExpired = { resetToStart() }
                                        )
                                    }

                                    is Screen.Stats -> {
                                        StatsScreen(
                                            isLoading = isLoadingStats,
                                            results = statsResults,
                                            onBack = { navigateBack() }
                                        )
                                    }

                                    is Screen.Test -> {
                                        TestScreen(
                                            test = screen.test,
                                            mode = screen.mode,
                                            language = selectedLanguage.code,
                                            username = loggedInUsername ?: "",
                                            scope = scope,
                                            onFinish = { navigateBack() },
                                            onLicenseRequired = { code ->
                                                if (code == "BLOCKED") navigateReplacingCurrent(Screen.Blocked)
                                                else navigateReplacingCurrent(Screen.Paywall(code))
                                            },
                                            onSessionExpired = { resetToStart() },
                                            onSaveResult = { _, _, answersPayload ->
                                                scope.launch {
                                                    try {
                                                        ApiClient.saveResult(
                                                            SaveResultRequestDto(
                                                                username = loggedInUsername ?: "",
                                                                testNumber = screen.test.number,
                                                                mode = screen.mode.name,
                                                                answers = answersPayload
                                                            )
                                                        )
                                                        menuRefreshTrigger++
                                                    } catch (_: Exception) {
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
package com.autospace.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Восстановление пароля, элементы лежат прямо на странице:
 * 1) логин или почта, сервер шлёт код на почту;
 * 2) «Проверьте почту» (без раскрытия, существует ли аккаунт);
 * 3) код из письма и новый пароль;
 * 4) готово.
 * language: язык интерфейса, на нём уходит письмо.
 */
@Composable
fun ForgotPasswordScreen(
    language: String,
    onBackToLogin: () -> Unit,
    onContinueRegistration: (username: String, maskedEmail: String) -> Unit
) {
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(1) }
    var identifier by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableStateOf(0) }
    // неподтверждённая регистрация с таким логином или почтой: (логин, скрытая почта)
    var pendingRegistration by remember { mutableStateOf<Pair<String, String>?>(null) }

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds--
        }
    }

    // goToCheck: после первой отправки переходим на «Проверьте почту», после повторной остаёмся
    fun sendCode(goToCheck: Boolean) {
        if (identifier.isBlank() || isLoading) return
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.forgotPassword(ForgotPasswordRequestDto(identifier.trim(), language))
                if (response.success) {
                    if (goToCheck) step = 2
                    cooldownSeconds = 60
                } else if (response.message.startsWith("REGISTRATION_PENDING:")) {
                    val parts = response.message.split(":", limit = 3)
                    pendingRegistration = Pair(parts.getOrNull(1) ?: identifier.trim(), parts.getOrNull(2) ?: "")
                } else {
                    errorMessage = if (response.message == "TOO_MANY_REQUESTS") {
                        strings.forgotTooManyRequests
                    } else {
                        response.message
                    }
                }
            } catch (e: Exception) {
                errorMessage = friendlyServerErrorMessage(e, strings)
            }
            isLoading = false
        }
    }

    fun changePassword() {
        if (code.length < 6 || isLoading) return
        if (newPassword.length < 6) {
            errorMessage = strings.forgotPasswordTooShort
            return
        }
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.resetPassword(
                    ResetPasswordRequestDto(identifier.trim(), code.trim(), newPassword)
                )
                if (response.success) {
                    done = true
                } else {
                    errorMessage = when (response.message) {
                        "INVALID_CODE" -> strings.forgotInvalidCode
                        "PASSWORD_INVALID" -> strings.forgotPasswordTooShort
                        "TOO_MANY_REQUESTS" -> strings.forgotTooManyRequests
                        else -> response.message
                    }
                }
            } catch (e: Exception) {
                errorMessage = friendlyServerErrorMessage(e, strings)
            }
            isLoading = false
        }
    }

    // стрелка «назад»: на шаг раньше, с первого шага ко входу
    val goBack: () -> Unit = {
        errorMessage = null
        when {
            done -> onBackToLogin()
            step == 3 -> {
                step = 2
                code = ""
                newPassword = ""
            }
            step == 2 -> step = 1
            else -> onBackToLogin()
        }
    }

    FormScaffold(onBack = if (step == 2) null else goBack, framed = false) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                done -> {
                    ShieldHeroIcon()
                    Text(
                        text = strings.forgotSuccess,
                        fontFamily = interFamily,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 24.dp)
                    )
                    PrimaryButton(text = strings.forgotBackToLogin, onClick = onBackToLogin)
                }

                step == 1 -> {
                    EmailHeroIcon()
                    AccentTitle(
                        text = strings.forgotTitle,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                    )
                    Text(
                        text = strings.forgotDescription,
                        fontFamily = interFamily,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 26.dp)
                    )
                    GlassTextField(
                        value = identifier,
                        onValueChange = {
                            identifier = it
                            pendingRegistration = null
                        },
                        placeholder = strings.forgotIdentifierLabel,
                        leading = { UserIcon() },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { sendCode(goToCheck = true) })
                    )
                    if (errorMessage != null) {
                        GentleError(message = errorMessage ?: "", modifier = Modifier.padding(top = 12.dp))
                    }
                    val pendingInfo = pendingRegistration
                    if (pendingInfo != null) {
                        GentleError(message = strings.forgotRegistrationPending, modifier = Modifier.padding(top = 12.dp))
                    }
                    PrimaryButton(
                        text = if (pendingInfo != null) strings.forgotContinueRegistration else strings.forgotSendCode,
                        onClick = {
                            if (pendingInfo != null) onContinueRegistration(pendingInfo.first, pendingInfo.second)
                            else sendCode(goToCheck = true)
                        },
                        enabled = identifier.isNotBlank(),
                        isLoading = isLoading,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                    SecondaryButton(
                        text = strings.authBackToLogin,
                        onClick = onBackToLogin,
                        withChevron = false,
                        leadingChevron = true,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                step == 2 -> {
                    EmailHeroIcon()
                    AccentTitle(
                        text = strings.checkEmailTitle,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                    )
                    Text(
                        text = strings.forgotCodeSent,
                        fontFamily = interFamily,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 26.dp)
                    )
                    EmailInfoCard(
                        email = identifier.trim(),
                        noCodeText = strings.checkEmailNoCode,
                        resendText = strings.checkEmailResend,
                        onResend = { sendCode(goToCheck = false) },
                        resendEnabled = !isLoading,
                        waitText = if (cooldownSeconds > 0) strings.forgotResendIn(cooldownSeconds) else null
                    )
                    if (errorMessage != null) {
                        GentleError(message = errorMessage ?: "", modifier = Modifier.padding(top = 12.dp))
                    }
                    PrimaryButton(
                        text = strings.checkEmailContinue,
                        onClick = { step = 3 },
                        modifier = Modifier.padding(top = 22.dp)
                    )
                    LinkText(
                        text = strings.authBackToLogin,
                        onClick = onBackToLogin,
                        color = Color(0xFFC9C5FF),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                else -> {
                    ShieldHeroIcon()
                    AccentTitle(
                        text = strings.verifyEnterTitle,
                        fontSize = 32.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                    )
                    Text(
                        text = strings.verifyEnterDescription,
                        fontFamily = interFamily,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 26.dp)
                    )
                    CodeBoxes(
                        value = code,
                        onValueChange = { code = it },
                        isError = errorMessage == strings.forgotInvalidCode
                    )
                    GlassTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        placeholder = strings.forgotNewPasswordLabel,
                        leading = { LockIcon() },
                        trailing = {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { isPasswordVisible = !isPasswordVisible },
                                contentAlignment = Alignment.Center
                            ) {
                                EyeIcon(open = isPasswordVisible)
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { changePassword() }),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    if (errorMessage != null) {
                        GentleError(message = errorMessage ?: "", modifier = Modifier.padding(top = 12.dp))
                    }
                    PrimaryButton(
                        text = strings.forgotChangePassword,
                        onClick = { changePassword() },
                        enabled = code.length == 6 && newPassword.isNotBlank(),
                        isLoading = isLoading,
                        modifier = Modifier.padding(top = 20.dp)
                    )
                    if (cooldownSeconds > 0) {
                        Text(
                            text = strings.forgotResendIn(cooldownSeconds),
                            fontFamily = interFamily,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                        )
                    } else {
                        LinkText(
                            text = strings.forgotResend,
                            onClick = { sendCode(goToCheck = false) },
                            enabled = !isLoading,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

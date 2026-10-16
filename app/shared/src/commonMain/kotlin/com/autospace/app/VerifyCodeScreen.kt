package com.autospace.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/** Срок жизни кода на сервере (PendingRegistrations): 15 минут. */
private const val CODE_LIFETIME_SECONDS = 15 * 60

/**
 * Подтверждение почты после регистрации, два экрана подряд:
 * 1) «Проверьте почту»: письмо, адрес и повторная отправка;
 * 2) «Введите код подтверждения»: шесть ячеек, таймер действия кода.
 * onBack возвращает ко входу (неподтверждённая регистрация остаётся на сервере до истечения срока).
 */
@Composable
fun VerifyCodeScreen(
    email: String,
    errorMessage: String?,
    isVerifying: Boolean,
    onVerify: (code: String) -> Unit,
    resendCooldownSeconds: Int,
    isResending: Boolean,
    resendExhausted: Boolean,
    onResendCode: () -> Unit,
    onContactSupport: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val interFamily = interFontFamily()

    var step by remember { mutableStateOf(1) }
    var code by remember { mutableStateOf("") }

    // таймер действия кода: после повторной отправки (кулдаун снова ~60 с) начинается заново
    var secondsLeft by remember { mutableStateOf(CODE_LIFETIME_SECONDS) }
    LaunchedEffect(secondsLeft) {
        if (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }
    LaunchedEffect(resendCooldownSeconds) {
        if (resendCooldownSeconds >= 59) secondsLeft = CODE_LIFETIME_SECONDS
    }

    val timeText = "${(secondsLeft / 60).toString().padStart(2, '0')}:${(secondsLeft % 60).toString().padStart(2, '0')}"
    val canVerify = code.length == 6 && secondsLeft > 0

    FormScaffold(
        onBack = if (step == 2) ({ step = 1 }) else null,
        framed = false
    ) {
        Column(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (step == 1) {
                EmailHeroIcon()
                AccentTitle(
                    text = strings.checkEmailTitle,
                    fontSize = 32.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                )
                Text(
                    text = strings.checkEmailDescription,
                    fontFamily = interFamily,
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color.White.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 26.dp)
                )

                EmailInfoCard(
                    email = email,
                    noCodeText = strings.checkEmailNoCode,
                    resendText = strings.checkEmailResend,
                    onResend = onResendCode,
                    resendEnabled = !isResending,
                    waitText = if (resendCooldownSeconds > 0) strings.verifyResendInText(resendCooldownSeconds) else null
                )

                if (resendExhausted) {
                    Text(
                        text = strings.verifyCodeProblem,
                        fontFamily = interFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                    )
                    LinkText(text = strings.commonSupport, onClick = onContactSupport)
                }

                PrimaryButton(
                    text = strings.checkEmailContinue,
                    onClick = { step = 2 },
                    modifier = Modifier.padding(top = 22.dp)
                )
                LinkText(
                    text = strings.authBackToLogin,
                    onClick = onBack,
                    color = Color(0xFFC9C5FF),
                    modifier = Modifier.padding(top = 8.dp)
                )
            } else {
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
                    isError = errorMessage != null,
                    keyboardActions = KeyboardActions(onDone = { if (canVerify && !isVerifying) onVerify(code) })
                )

                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ClockIcon(color = if (secondsLeft in 1..60) Brand.Error else Brand.Yellow)
                    Text(
                        text = if (secondsLeft > 0) strings.verifyCodeExpires(timeText) else strings.verifyCodeExpired,
                        fontFamily = interFamily,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }

                if (errorMessage != null) {
                    GentleError(message = errorMessage, modifier = Modifier.padding(top = 14.dp))
                }

                PrimaryButton(
                    text = strings.verifyConfirm,
                    onClick = { onVerify(code) },
                    enabled = canVerify,
                    isLoading = isVerifying,
                    modifier = Modifier.padding(top = 20.dp)
                )

                when {
                    resendExhausted -> {
                        Text(
                            text = strings.verifyCodeProblem,
                            fontFamily = interFamily,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                        )
                        LinkText(text = strings.commonSupport, onClick = onContactSupport)
                    }
                    resendCooldownSeconds > 0 -> {
                        Text(
                            text = strings.verifyResendInText(resendCooldownSeconds),
                            fontFamily = interFamily,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
                        )
                    }
                    else -> {
                        LinkText(
                            text = strings.forgotResend,
                            onClick = onResendCode,
                            enabled = !isResending,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

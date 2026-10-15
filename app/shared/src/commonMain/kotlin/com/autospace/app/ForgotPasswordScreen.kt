package com.autospace.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Восстановление пароля в два шага на одном экране:
 * 1) логин или почта, сервер шлёт код на почту;
 * 2) код из письма и новый пароль.
 * language: язык интерфейса, на нём уходит письмо.
 */
@Composable
fun ForgotPasswordScreen(
    language: String,
    onBackToLogin: () -> Unit
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    var step by remember { mutableStateOf(1) }
    var identifier by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var infoMessage by remember { mutableStateOf<String?>(null) }
    var done by remember { mutableStateOf(false) }
    var cooldownSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000)
            cooldownSeconds--
        }
    }

    val windowSizeClass = LocalWindowSizeClass.current
    val formModifier = if (windowSizeClass == WindowSizeClass.Compact) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().widthIn(max = 400.dp)
    }

    fun sendCode() {
        if (identifier.isBlank() || isLoading) return
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = ApiClient.forgotPassword(ForgotPasswordRequestDto(identifier.trim(), language))
                if (response.success) {
                    step = 2
                    infoMessage = strings.forgotCodeSent
                    cooldownSeconds = 60
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
        if (code.isBlank() || isLoading) return
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
                    infoMessage = null
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = strings.forgotTitle,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )

        Column(
            modifier = formModifier.padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                done -> {
                    Text(
                        text = strings.forgotSuccess,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LoadingButton(
                        text = strings.forgotBackToLogin,
                        isLoading = false,
                        enabled = true,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onBackToLogin
                    )
                }

                step == 1 -> {
                    Text(
                        text = strings.forgotDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = identifier,
                        onValueChange = { identifier = it },
                        label = { Text(strings.forgotIdentifierLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { sendCode() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Text(text = errorMessage ?: "", color = Color(0xFFF44336))
                    }
                    LoadingButton(
                        text = strings.forgotSendCode,
                        isLoading = isLoading,
                        enabled = identifier.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { sendCode() }
                    )
                }

                else -> {
                    if (infoMessage != null) {
                        Text(
                            text = infoMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text(strings.forgotCodeLabel) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text(strings.forgotNewPasswordLabel) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Text(if (isPasswordVisible) strings.authHidePassword else strings.authShowPassword)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { changePassword() }),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMessage != null) {
                        Text(text = errorMessage ?: "", color = Color(0xFFF44336))
                    }
                    LoadingButton(
                        text = strings.forgotChangePassword,
                        isLoading = isLoading,
                        enabled = code.isNotBlank() && newPassword.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { changePassword() }
                    )

                    if (cooldownSeconds > 0) {
                        Text(
                            text = strings.forgotResendIn(cooldownSeconds),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        TextButton(
                            onClick = { sendCode() },
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(strings.forgotResend)
                        }
                    }
                }
            }
        }
    }
}

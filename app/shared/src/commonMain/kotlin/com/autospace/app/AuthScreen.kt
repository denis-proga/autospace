package com.autospace.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay

@Composable
fun AuthScreen(
    errorMessage: String?,
    isLoading: Boolean,
    onModeChanged: () -> Unit,
    onLogin: (username: String, password: String) -> Unit,
    onRegister: (User) -> Unit
) {
    var isLoginMode by remember { mutableStateOf(true) }

    var showWakingHint by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(5000)
            showWakingHint = true
        } else {
            showWakingHint = false
        }
    }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLoginMode) "Вход" else "Регистрация",
            style = MaterialTheme.typography.headlineSmall
        )

        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isLoginMode) {
                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("Имя") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Фамилия") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Почта") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Логин") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Пароль") },
                modifier = Modifier.fillMaxWidth()
            )

            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFF44336),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            val canSubmit = if (isLoginMode) {
                username.isNotBlank() && password.isNotBlank()
            } else {
                firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() &&
                        username.isNotBlank() && password.isNotBlank()
            }

            LoadingButton(
                text = if (isLoginMode) "Войти" else "Зарегистрироваться",
                isLoading = isLoading,
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (isLoginMode) {
                        onLogin(username, password)
                    } else {
                        onRegister(
                            User(
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                username = username,
                                password = password
                            )
                        )
                    }
                }
            )

            if (showWakingHint) {
                Text(
                    text = "Подключение к серверу… Обычно это занимает до минуты",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            }

            TextButton(
                onClick = {
                    isLoginMode = !isLoginMode
                    onModeChanged()
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (isLoginMode) "Нет аккаунта? Зарегистрироваться"
                    else "Уже есть аккаунт? Войти"
                )
            }
        }
    }
}
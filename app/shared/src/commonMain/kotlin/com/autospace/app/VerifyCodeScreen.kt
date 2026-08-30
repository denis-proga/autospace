package com.autospace.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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
    onContactSupport: () -> Unit
) {
    var code by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Подтверждение почты",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Мы отправили код подтверждения на $email",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
        )

        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            label = { Text("Код из письма") },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color(0xFFF44336),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        LoadingButton(
            text = "Подтвердить",
            isLoading = isVerifying,
            enabled = code.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            onClick = { onVerify(code) }
        )

        when {
            resendExhausted -> {
                Text(
                    text = "Код не приходит? Похоже, что-то пошло не так с доставкой письма",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )
                TextButton(onClick = onContactSupport) {
                    Text("Написать в поддержку")
                }
            }
            resendCooldownSeconds > 0 -> {
                Text(
                    text = "Отправить код повторно можно через $resendCooldownSeconds сек.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
            else -> {
                TextButton(
                    onClick = onResendCode,
                    enabled = !isResending
                ) {
                    Text(if (isResending) "Отправка..." else "Не пришёл код? Отправить снова")
                }
            }
        }
    }
}
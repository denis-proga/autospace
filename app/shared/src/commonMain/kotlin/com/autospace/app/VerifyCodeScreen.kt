package com.autospace.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    val strings = LocalStrings.current
    var code by remember { mutableStateOf("") }

    val windowSizeClass = LocalWindowSizeClass.current
    val contentModifier = if (windowSizeClass == WindowSizeClass.Compact) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().widthIn(max = 400.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = contentModifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.verifyTitle,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = strings.verifyDescription(email),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp)
            )

            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text(strings.verifyCodeLabel) },
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
                text = strings.verifyConfirm,
                isLoading = isVerifying,
                enabled = code.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                onClick = { onVerify(code) }
            )

            when {
                resendExhausted -> {
                    Text(
                        text = strings.verifyCodeProblem,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                    TextButton(onClick = onContactSupport) {
                        Text(strings.commonSupport)
                    }
                }
                resendCooldownSeconds > 0 -> {
                    Text(
                        text = strings.verifyResendInText(resendCooldownSeconds),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    )
                }
                else -> {
                    TextButton(
                        onClick = onResendCode,
                        enabled = !isResending
                    ) {
                        Text(if (isResending) strings.verifyResendSending else strings.verifyResendLink)
                    }
                }
            }
        }
    }
}
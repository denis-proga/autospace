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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun BlockedScreen(
    isSending: Boolean,
    sentSuccessfully: Boolean,
    errorMessage: String?,
    onSend: (message: String, phone: String, email: String) -> Unit,
    onBackToRegistration: () -> Unit
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val contentModifier = if (windowSizeClass == WindowSizeClass.Compact) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().widthIn(max = 500.dp)
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
                text = "Доступ отклонён",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "К сожалению, ваша заявка была отклонена. Если считаете, что это ошибка, напишите нам.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp)
            )

            SupportForm(
                isSending = isSending,
                sentSuccessfully = sentSuccessfully,
                errorMessage = errorMessage,
                onSend = onSend
            )

            OutlinedButton(
                onClick = onBackToRegistration,
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
            ) {
                Text("Зарегистрироваться заново")
            }
        }
    }
}
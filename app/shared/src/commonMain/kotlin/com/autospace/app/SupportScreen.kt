package com.autospace.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun SupportScreen(
    isSending: Boolean,
    sentSuccessfully: Boolean,
    errorMessage: String?,
    onSend: (message: String, phone: String, email: String) -> Unit,
    onBackToRegistration: () -> Unit,
    onKeepWaiting: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Поддержка",
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Если вы допустили ошибку при регистрации, вы можете начать заново. Либо продолжайте ожидать решение по вашей заявке.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp)
        )

        SupportForm(
            isSending = isSending,
            sentSuccessfully = sentSuccessfully,
            errorMessage = errorMessage,
            onSend = onSend
        )

        Button(
            onClick = onKeepWaiting,
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
        ) {
            Text("Продолжить ожидание")
        }

        OutlinedButton(
            onClick = onBackToRegistration,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text("Начать регистрацию заново")
        }
    }
}
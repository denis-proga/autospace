package com.autospace.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun SupportForm(
    isSending: Boolean,
    sentSuccessfully: Boolean,
    errorMessage: String?,
    onSend: (message: String, phone: String, email: String) -> Unit
) {
    val strings = LocalStrings.current

    var message by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = strings.commonSupport,
            style = MaterialTheme.typography.titleMedium
        )

        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text(strings.supportFormMessage) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text(strings.supportFormPhone) },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(strings.supportFormEmail) },
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage != null) {
            Text(
                text = errorMessage,
                color = Color(0xFFF44336)
            )
        } else if (sentSuccessfully) {
            Text(
                text = strings.supportFormSentSuccess,
                color = Color(0xFF4CAF50)
            )
        } else {
            LoadingButton(
                text = strings.supportFormSend,
                isLoading = isSending,
                enabled = message.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = { onSend(message, phone, email) }
            )
        }
    }
}
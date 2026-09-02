package com.autospace.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun PendingScreen(
    isChecking: Boolean,
    onCheckStatus: () -> Unit,
    onGoToSupport: () -> Unit
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val contentModifier = if (windowSizeClass == WindowSizeClass.Compact) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().widthIn(max = 400.dp)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = contentModifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ваша заявка обрабатывается",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "В течение 12 часов вы получите ответ",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            LoadingButton(
                text = "Проверить статус",
                isLoading = isChecking,
                modifier = Modifier.padding(top = 24.dp),
                onClick = onCheckStatus
            )

            TextButton(
                onClick = onGoToSupport,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                Text("Нужна помощь?")
            }
        }
    }
}
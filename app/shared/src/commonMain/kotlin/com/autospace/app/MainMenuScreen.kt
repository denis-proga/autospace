package com.autospace.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun MainMenuScreen(
    username: String,
    refreshKey: Int,
    onTestSelected: (TestInfo, TestMode) -> Unit,
    onOpenStats: () -> Unit
) {
    val strings = LocalStrings.current

    var selectedMode by remember { mutableStateOf(TestMode.LEARNING) }
    val tests = remember { generateTestList() }
    val scope = rememberCoroutineScope()

    var results by remember { mutableStateOf<List<TestResultItemDto>>(emptyList()) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var alreadyCompletedTestNumber by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(username, refreshTrigger, refreshKey) {
        try {
            val response = ApiClient.getResults(username)
            results = response.results
        } catch (e: Exception) {
            // тихо игнорируем — карточки просто останутся без цвета
        }
    }

    val latestByTestMode = remember(results) {
        results.groupBy { it.testNumber to it.mode }
            .mapValues { (_, list) -> list.maxByOrNull { it.completedAt } }
    }

    val windowSizeClass = LocalWindowSizeClass.current
    val contentModifier = if (windowSizeClass == WindowSizeClass.Compact) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().widthIn(max = 900.dp)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = contentModifier.fillMaxHeight().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.mainMenuAppName,
                    style = MaterialTheme.typography.headlineMedium
                )
                TextButton(onClick = onOpenStats) {
                    Text(strings.mainMenuStats)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedMode == TestMode.LEARNING,
                    onClick = { selectedMode = TestMode.LEARNING },
                    label = { Text(strings.commonLearning) }
                )
                FilterChip(
                    selected = selectedMode == TestMode.EXAM,
                    onClick = { selectedMode = TestMode.EXAM },
                    label = { Text(strings.commonExam) }
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tests) { test ->
                    val latestResult = latestByTestMode[test.number to selectedMode.name]
                    val isCompleted = latestResult != null

                    val cardColor = when {
                        latestResult == null -> null
                        selectedMode == TestMode.EXAM -> Color(0xFF2196F3)
                        latestResult.correctCount >= 27 -> Color(0xFF4CAF50)
                        else -> Color(0xFFF44336)
                    }

                    Card(
                        modifier = Modifier
                            .height(80.dp)
                            .fillMaxWidth(),
                        colors = if (cardColor != null)
                            CardDefaults.cardColors(containerColor = cardColor)
                        else
                            CardDefaults.cardColors(),
                        onClick = {
                            if (isCompleted) {
                                alreadyCompletedTestNumber = test.number
                            } else {
                                onTestSelected(test, selectedMode)
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("${strings.commonTestWord} ${test.number}")
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = { showResetConfirmation = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.mainMenuResetAll)
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isResetting) showResetConfirmation = false },
            title = { Text(strings.mainMenuResetConfirmTitle) },
            text = { Text(strings.mainMenuResetConfirmText) },
            confirmButton = {
                Button(
                    onClick = {
                        isResetting = true
                        scope.launch {
                            try {
                                ApiClient.resetProgress(ResetProgressRequestDto(username = username))
                                refreshTrigger++
                            } catch (e: Exception) {
                                // тихо игнорируем — можно повторить попытку
                            }
                            isResetting = false
                            showResetConfirmation = false
                        }
                    }
                ) {
                    Text(if (isResetting) strings.mainMenuResetting else strings.mainMenuResetConfirmButton)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetConfirmation = false },
                    enabled = !isResetting
                ) {
                    Text(strings.mainMenuCancel)
                }
            }
        )
    }

    if (alreadyCompletedTestNumber != null) {
        AlertDialog(
            onDismissRequest = { alreadyCompletedTestNumber = null },
            title = { Text(strings.mainMenuAlreadyCompletedTitle) },
            text = { Text(strings.mainMenuAlreadyCompletedText("${strings.commonTestWord} $alreadyCompletedTestNumber")) },
            confirmButton = {
                Button(onClick = { alreadyCompletedTestNumber = null }) {
                    Text(strings.mainMenuOk)
                }
            }
        )
    }
}